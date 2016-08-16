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

package uk.gov.hmrc.mobiletokenexchange.controllers

import play.api.mvc.{Action, BodyParsers}
import play.api.libs.json.{JsError, Json}
import uk.gov.hmrc.mobiletokenexchange.domain.{UpdateRefreshToken, FoundToken, TokenRegistration}
import uk.gov.hmrc.mobiletokenexchange.services._
import play.api.Logger
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}


trait MobileTokenExchangeController extends BaseController with ErrorHandling {
  val service: TokenService

  final def register(journeyId: Option[String] = None) = Action.async(BodyParsers.parse.json) {
    implicit request =>

    request.body.validate[TokenRegistration].fold(
      errors => {
        Logger.warn("Received error with parsing service request: " + errors)
        Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(errors))))
      },
      tokenRegistration => {
        def buildResponse(id:String) = s"""{"id":"$id"}"""
        errorWrapper(
          service.register(tokenRegistration).map {
            case (true, value) => Created(buildResponse(value))
            case (false, value) => Ok(buildResponse(value))
          }
        )
      })
  }

  def updateRefreshToken(journeyId: Option[String] = None) = Action.async(BodyParsers.parse.json) {
    implicit request =>

      request.body.validate[UpdateRefreshToken].fold(
        errors => {
          Logger.warn("Received error with parsing service register: " + errors)
          Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(errors))))
        },
        update => {
            errorWrapper(service.updateToken(update).map {
              case Some(response) => Ok(Json.toJson(response))
              case _ => NotFound
            })
        })
  }

  def find(id:String, journeyId: Option[String] = None) = Action.async {
    implicit request =>
      errorWrapper(service.find(id).map {
        case Some(response:FoundToken) => Ok(Json.toJson(response))
        case None => NotFound
      })
  }
}

object MobileTokenExchangeController extends MobileTokenExchangeController {
  override val service = LiveTokenService
  override implicit val ec: ExecutionContext = ExecutionContext.global
}


