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

import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeApplication
import uk.gov.hmrc.mobiletokenexchange.domain.FoundToken
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


class MobileTokenExchangeControllerSpec extends UnitSpec with WithFakeApplication with ScalaFutures with StubApplicationConfiguration {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

  "token register service" should {

    "return successfully with a 201 response when the record is first created" in new Success {

      val result: Result = await(controller.register(journeyId)(fakeRequest(tokenJsonBody)))

      status(result) shouldBe 201
      jsonBodyOf(result) shouldBe Json.parse( s"""{"id":"${bSONObjectID.stringify}"}""")

      testTaskService.saveDetails shouldBe Map(
        "deviceId" -> deviceId,
        "refreshToken" -> refreshToken,
        "timestamp" -> time.toString)
    }

    "return successfully with a 200 response when updating an existing records with a new refresh token" in new SuccessUpdate {
      val result: Result = await(controller.register(journeyId)(fakeRequest(tokenJsonBody)))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.parse( s"""{"id":"${bSONObjectID.stringify}"}""")

      testTaskService.saveDetails shouldBe Map(
        "deviceId" -> deviceId,
        "refreshToken" -> refreshToken,
        "timestamp" -> time.toString)
    }

    "return bad request if the request is invalid" in new Success {
      val result: Result = await(controller.register(journeyId)(fakeRequest(invalidTokenJsonBody)))

      status(result) shouldBe 400
      testTaskService.saveDetails shouldBe Map.empty
    }

  }

  "updateRefreshToken service" should {

    "return successfully with a 200 response" in new Success {
      val result: Result = await(controller.updateRefreshToken(journeyId)(fakeRequest(updateJsonBody)))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(FoundToken(bSONObjectID.stringify, deviceId, refreshToken, 100))

      testTaskService.saveDetails shouldBe Map("deviceId" -> "someId")
    }

    "return 404 when id cannot be resolved" in new UpdateTimestampNotFound {
      val result: Result = await(controller.updateRefreshToken(journeyId)(fakeRequest(updateJsonBody)))

      status(result) shouldBe 404
      testTaskService.saveDetails shouldBe Map("deviceId" -> "someId")
    }

  }

  "find service" should {

    "return successfully with a 200 response and record found" in new Success {

      val result: Result = await(controller.find("someId", journeyId)(emptyRequest))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(FoundToken(bSONObjectID.stringify, deviceId, refreshToken, 100))

      testTaskService.saveDetails shouldBe Map("deviceId" -> "someId")
    }

    "return successfully with a 404 response when record not found" in new FindNotFound {

      val result: Result = await(controller.find("someId", journeyId)(emptyRequest))

      status(result) shouldBe 404
      testTaskService.saveDetails shouldBe Map("deviceId" -> "someId")
    }
  }
}
