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

package controllers

import java.util.UUID

import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mobiletokenexchange.domain.{UpdateRefreshToken, TokenRegistration}
import uk.gov.hmrc.mobiletokenexchange.repository.{TokenPersist, TokenRepository}
import uk.gov.hmrc.mobiletokenexchange.services.{TokenService, LiveTokenService}
import uk.gov.hmrc.mongo.{DatabaseUpdate, Saved, Updated}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.mobiletokenexchange.config.MicroserviceAuditConnector
import uk.gov.hmrc.mobiletokenexchange.controllers.MobileTokenExchangeController

import scala.concurrent.{ExecutionContext, Future}

trait TestData {
  val deviceId="deviceId123"
  val refreshToken="refreshToken456"
  val time = 10
}

class TestRepository(bSONObjectID: BSONObjectID, save:Boolean, updateTimestamp:Boolean, find:Boolean) extends TokenRepository with TestData {
  val token = TokenPersist(bSONObjectID, deviceId, refreshToken, 100)
  val saveResponse = if (save) Saved(token) else Updated(token,token)

  override def save(expectation: TokenRegistration): Future[DatabaseUpdate[TokenPersist]] = Future.successful(DatabaseUpdate(null, saveResponse))
  override def findByDeviceId(deviceId: String): Future[Option[TokenPersist]] = Future.successful(if (find) Some(token) else None)
  override def updateTimestampAndRefreshToken(update: UpdateRefreshToken): Future[Option[DatabaseUpdate[TokenPersist]]] = Future.successful(
      if (updateTimestamp) Some(DatabaseUpdate(null, Saved(token))) else None)

}

class TestTokenService(testRepository:TestRepository, testAuditConnector: AuditConnector) extends LiveTokenService {
  var saveDetails:Map[String, String]=Map.empty

  override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec : ExecutionContext) = {
    saveDetails=details
    Future.successful(AuditResult.Success)
  }

  override val auditConnector = testAuditConnector
  override val repository = testRepository
}

trait Setup extends TestData {
  implicit val hc = HeaderCarrier()
  val journeyId = Option(UUID.randomUUID().toString)

  val emptyRequest = FakeRequest()
  val registration = TokenRegistration(deviceId, refreshToken, time)
  val tokenJsonBody: JsValue = Json.toJson(registration)
  val invalidTokenJsonBody: JsValue = Json.parse("""{"invalid":"value"}""")
  val updateJsonBody: JsValue = Json.parse(s"""{"deviceId":"someId", "refreshToken":"$refreshToken"}""")

  def fakeRequest(body:JsValue) = FakeRequest(POST, "url").withBody(body)
    .withHeaders("Content-Type" -> "application/json")

  val saveMode=true
  val updateTimestamp=true
  val find=true

  val bSONObjectID: BSONObjectID = BSONObjectID.generate
  val testRepository = new TestRepository(bSONObjectID, saveMode, updateTimestamp, find)
  val testTaskService = new TestTokenService(testRepository , MicroserviceAuditConnector)
}

trait Success extends Setup {
  val controller = new MobileTokenExchangeController {
    override val service: TokenService = testTaskService
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}

trait SuccessUpdate extends Setup {
  override val saveMode = false
  val controller = new MobileTokenExchangeController {
    override val service: TokenService = testTaskService
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}

trait UpdateTimestampNotFound extends Setup {
  override val updateTimestamp = false
  val controller = new MobileTokenExchangeController {
    override val service: TokenService = testTaskService
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}

trait FindNotFound extends Setup {
  override val find=false

  val controller = new MobileTokenExchangeController {
    override val service: TokenService = testTaskService
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}


