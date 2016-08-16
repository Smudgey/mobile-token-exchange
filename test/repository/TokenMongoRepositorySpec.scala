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

package repository

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, LoneElement}
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mobiletokenexchange.domain.{UpdateRefreshToken, TokenRegistration}
import uk.gov.hmrc.mobiletokenexchange.repository.{TokenPersist, TokenMongoRepository}
import uk.gov.hmrc.mongo.{DatabaseUpdate, MongoSpecSupport, Saved}
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global

class TokenMongoRepositorySpec extends UnitSpec with
                                                 MongoSpecSupport with
                                                 BeforeAndAfterEach with
                                                 ScalaFutures with
                                                 LoneElement with
                                                 Eventually {

  private val repository: TokenMongoRepository = new TokenMongoRepository

  trait Setup {
    val deviceId = "some-auth-id"
    val testToken = "token"
    val testUpdateToken = "updateToken"
    val now = System.currentTimeMillis()
    val registration = TokenRegistration(deviceId, testToken, now)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Validating index's " should {

    "not able to insert duplicate data entries for deviceId" in new Setup {
      val resp: DatabaseUpdate[TokenPersist] = await(repository.save(registration))

      a[DatabaseException] should be thrownBy await(repository.insert(resp.updateType.savedValue))
      a[DatabaseException] should be thrownBy await(repository.insert(resp.updateType.savedValue.copy(id = BSONObjectID.generate)))
    }

    "insert unique records with different deviceId" in new Setup {
      val resp: DatabaseUpdate[TokenPersist] = await(repository.save(registration))

      await(repository.insert(resp.updateType.savedValue.copy(id = BSONObjectID.generate, deviceId = "another deviceId")))
      await(repository.insert(resp.updateType.savedValue.copy(id = BSONObjectID.generate, deviceId = "and another deviceId")))
    }
  }

  "repository" should {

    "create a new record" in new Setup {
      val result = await(repository.save(registration))
      result.updateType shouldBe an[Saved[_]]
      result.updateType.savedValue.deviceId shouldBe deviceId
      result.updateType.savedValue.refreshToken shouldBe testToken
      result.updateType.savedValue.timestamp shouldBe now
    }

    "create multiple records if the deviceId is different" in new Setup {
      val result = await(repository.save(registration))

      result.updateType.savedValue.deviceId shouldBe deviceId
      result.updateType.savedValue.refreshToken shouldBe testToken
      result.updateType.savedValue.timestamp shouldBe now

      val result2 = await(repository.save(registration.copy(deviceId = "another deviceId")))
      result2.updateType shouldBe an[Saved[_]]

      result2.updateType.savedValue.deviceId shouldBe "another deviceId"
      result2.updateType.savedValue.refreshToken shouldBe testToken
      result2.updateType.savedValue.timestamp shouldBe now
    }

    "find an existing record" in new Setup {
      val result = await(repository.save(registration))

      result.updateType.savedValue.deviceId shouldBe deviceId
      result.updateType.savedValue.refreshToken shouldBe testToken
      result.updateType.savedValue.timestamp shouldBe now

      val findResult: Option[TokenPersist] = await(repository.findByDeviceId(deviceId))
      findResult.get.deviceId shouldBe deviceId
      findResult.get.refreshToken shouldBe testToken
      findResult.get.timestamp shouldBe now
    }

    "not find an existing record" in new Setup {
      val result = await(repository.save(registration))

      val findResult = await(repository.findByDeviceId("unknown"))
      findResult shouldBe None
    }

    "update the timestamp of an existing record" in new Setup {
      await(repository.save(registration))

      val update = UpdateRefreshToken(deviceId, testUpdateToken)
      val result = await(repository.updateTimestampAndRefreshToken(update))
      result.get.updateType.savedValue.deviceId shouldBe deviceId
      result.get.updateType.savedValue.refreshToken shouldBe testUpdateToken
      result.get.updateType.savedValue.timestamp should be > now
    }

    "fail to update timestamp of an existing record when the wrong deviceId is supplied" in new Setup {
      await(repository.save(registration))

      val update = UpdateRefreshToken("unknown", testUpdateToken)
      val result = await(repository.updateTimestampAndRefreshToken(update))
      result shouldBe None
    }
  }
}
