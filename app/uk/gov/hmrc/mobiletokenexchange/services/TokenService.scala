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

package uk.gov.hmrc.mobiletokenexchange.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobiletokenexchange.config.MicroserviceAuditConnector
import uk.gov.hmrc.mobiletokenexchange.domain._
import uk.gov.hmrc.mobiletokenexchange.repository.{TokenPersist, TokenRepository}
import uk.gov.hmrc.mongo.{DatabaseUpdate, Saved, Updated}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TokenService {
  def register(registration: TokenRegistration)(implicit hc: HeaderCarrier): Future[(Boolean,String)]
  def find(deviceId:String)(implicit hc: HeaderCarrier): Future[Option[FoundToken]]
  def updateToken(update: UpdateRefreshToken)(implicit hc: HeaderCarrier): Future[Option[FoundToken]]
}

trait LiveTokenService extends TokenService with Auditor {

  def repository: TokenRepository

  override def register(registration:TokenRegistration)(implicit hc: HeaderCarrier): Future[(Boolean,String)] = {

    withAudit("register", TokenRegistration.audit(registration)) {
      repository.save(registration).map { result =>
        result.updateType match {
          case Saved(res) => (true,res.id.stringify)
          case Updated(_,savedValue) => (false,savedValue.id.stringify)
        }
      }
    }
  }

  override def find(deviceId:String)(implicit hc: HeaderCarrier): Future[Option[FoundToken]] = {
    withAudit("find", Map("deviceId" -> deviceId)) {
      repository.findByDeviceId(deviceId).map { item => item.map(row => FoundToken(row.id.stringify, row.deviceId, row.refreshToken, row.timestamp)) }
    }
  }

  override def updateToken(update: UpdateRefreshToken)(implicit hc: HeaderCarrier): Future[Option[FoundToken]] = {
    def buildResponse(token:TokenPersist) = FoundToken(token.id.stringify, token.deviceId, token.refreshToken, token.timestamp)

    withAudit("updateTimestamp", Map("deviceId" -> update.deviceId)) {
      repository.updateTimestampAndRefreshToken(update).map {
        case Some(update:DatabaseUpdate[TokenPersist]) => Some(buildResponse(update.updateType.savedValue))
        case _ => None
      }
    }
  }

}

object LiveTokenService extends LiveTokenService {
  override val auditConnector: AuditConnector = MicroserviceAuditConnector

  override val repository:TokenRepository = TokenRepository()
}
