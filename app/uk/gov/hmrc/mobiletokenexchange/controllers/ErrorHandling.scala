/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletokenexchange.controllers

import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

trait ErrorHandling {
  self:BaseController =>

  import play.api.{Logger, mvc}
  import uk.gov.hmrc.play.http.HeaderCarrier

  implicit val ec : ExecutionContext

  def errorWrapper(func: => Future[mvc.Result])(implicit hc:HeaderCarrier) = {
    func.recover {

      case e: Throwable =>
        Logger.error(s"Internal server error: ${e.getMessage}", e)
        InternalServerError
    } 
  }
}
