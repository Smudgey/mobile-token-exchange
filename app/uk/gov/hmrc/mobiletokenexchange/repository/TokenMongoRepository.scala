/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobiletokenexchange.repository

import play.api.libs.json._
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import uk.gov.hmrc.mobiletokenexchange.domain.{UpdateRefreshToken, TokenRegistration}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, BSONBuilderHelpers, DatabaseUpdate, ReactiveRepository}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class TokenPersist(id: BSONObjectID, deviceId:String, refreshToken: String, timestamp:Long)

object TokenPersist {

  val mongoFormats: Format[TokenPersist] = ReactiveMongoFormats.mongoEntity(
  {
    implicit val oidFormat = ReactiveMongoFormats.objectIdFormats
    Format(Json.reads[TokenPersist], Json.writes[TokenPersist])
  })
}

object TokenRepository extends MongoDbConnection {
  lazy val mongo = new TokenMongoRepository
  def apply(): TokenRepository = mongo
}

class TokenMongoRepository(implicit mongo: () => DB)
  extends ReactiveRepository[TokenPersist, BSONObjectID]("tokens", mongo, TokenPersist.mongoFormats, ReactiveMongoFormats.objectIdFormats)
          with AtomicUpdate[TokenPersist]
          with TokenRepository
          with BSONBuilderHelpers {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[scala.Seq[Boolean]] = {
    Future.sequence(
      Seq(
        collection.indexesManager.ensure(
          Index(Seq("refreshToken" -> IndexType.Ascending), name = Some("refreshTokenNotUnique"), unique = false)),
        collection.indexesManager.ensure(
          Index(Seq("deviceId" -> IndexType.Ascending), name = Some("deviceIdUnique"), unique = true))
      )
    )
  }

  override def isInsertion(suppliedId: BSONObjectID, returned: TokenPersist): Boolean =
    suppliedId.equals(returned.id)

  private def modifierForInsert(registration: TokenRegistration): BSONDocument = {
    BSONDocument(
      "$set" -> BSONDocument("timestamp" -> registration.timestamp),
      "$set" -> BSONDocument("refreshToken" -> registration.refreshToken),
      "$setOnInsert" -> BSONDocument("deviceId" -> registration.deviceId),
      "$setOnInsert" -> BSONDocument("created" -> BSONDateTime(DateTimeUtils.now.getMillis)),
      "$set" -> BSONDocument("updated" -> BSONDateTime(DateTimeUtils.now.getMillis))
    )
  }

  private def modifierForTimestampAndRefreshToken(update: UpdateRefreshToken): BSONDocument = {
    val now = DateTimeUtils.now.getMillis
    BSONDocument(
      "$set" -> BSONDocument("timestamp" -> now),
      "$set" -> BSONDocument("refreshToken" -> update.refreshToken),
      "$set" -> BSONDocument("updated" -> BSONDateTime(now))
    )
  }

  private def findRecordByDeviceId(deviceId: String) = BSONDocument("deviceId" -> BSONString(deviceId))

  def updateTimestampAndRefreshToken(update: UpdateRefreshToken): Future[Option[DatabaseUpdate[TokenPersist]]] = {
    atomicUpdate(findRecordByDeviceId(update.deviceId), modifierForTimestampAndRefreshToken(update))
  }

  def findByDeviceId(deviceId: String): Future[Option[TokenPersist]] = {
    collection.find(Json.obj("deviceId" -> Json.toJson(deviceId))).one[TokenPersist]
  }

  override def save(token: TokenRegistration): Future[DatabaseUpdate[TokenPersist]] = {
    atomicUpsert(findRecordByDeviceId(token.deviceId), modifierForInsert(token))
  }

}

trait TokenRepository {
  def save(expectation: TokenRegistration): Future[DatabaseUpdate[TokenPersist]]
  def findByDeviceId(deviceId: String): Future[Option[TokenPersist]]
  def updateTimestampAndRefreshToken(update: UpdateRefreshToken): Future[Option[DatabaseUpdate[TokenPersist]]]
}
