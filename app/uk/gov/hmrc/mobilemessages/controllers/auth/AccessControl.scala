/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.mobilemessages.controllers.auth

import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, OFormat, Reads}
import play.api.mvc._
import uk.gov.hmrc.api.controllers.{ErrorAcceptHeaderInvalid, HeaderValidator}
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{confidenceLevel, nino, saUtr}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Request => _, _}
import uk.gov.hmrc.mobilemessages.controllers._
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

final case class Authority(nino: Nino, cl: ConfidenceLevel, authId: String)

final case class AuthenticatedRequest[A](authority: Option[Authority], request: Request[A]) extends WrappedRequest(request)

case class AuthorityRecord(uri: String)

object AuthorityRecord {
  implicit val format: OFormat[AuthorityRecord] = Json.format[AuthorityRecord]
  implicit val reads: Reads[AuthorityRecord] = Json.reads[AuthorityRecord]
}

trait Authorisation extends Results with AuthorisedFunctions {

  import AuthorityRecord._

  val confLevel: Int
  val http: CoreGet
  val authUrl: String

  lazy val requiresAuth: Boolean = true
  lazy val ninoNotFoundOnAccount = new NinoNotFoundOnAccount
  lazy val lowConfidenceLevel = new AccountWithLowCL

  def grantAccess()(implicit hc: HeaderCarrier): Future[Authority] = {
    getAuthorityRecord.flatMap { authRecord: AuthorityRecord =>
      authorised().retrieve(nino and confidenceLevel and saUtr) {
        case Some(foundNino) ~ foundConfidenceLevel ~ foundSAUtr =>
          Logger.info(s"mobile messages for user with utr: ${foundSAUtr.getOrElse("not found")}" )

          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
          Future successful Authority(Nino(foundNino), foundConfidenceLevel, authRecord.uri)
        case None ~ _ ~ _ =>
          throw ninoNotFoundOnAccount
      }
    }
  }

  def invokeAuthBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)

    grantAccess().flatMap { authority =>
      block(AuthenticatedRequest(Some(authority), request))
    }.recover {
      case _: uk.gov.hmrc.http.Upstream4xxResponse =>
        Logger.info("Unauthorized! Failed to grant access since 4xx response!")
        Unauthorized(toJson(ErrorUnauthorizedMicroService))

      case _: NinoNotFoundOnAccount =>
        Logger.info("Unauthorized! NINO not found on account!")
        Forbidden(toJson(ErrorForbidden))

      case _: AccountWithLowCL =>
        Logger.info("Unauthorized! Account with low CL!")
        Forbidden(toJson(ErrorForbidden))
    }
  }

  def getAuthorityRecord(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthorityRecord] =
    http.GET[AuthorityRecord](s"$authUrl/auth/authority")
}

trait AccessControl extends HeaderValidator with Authorisation {

  def validateAcceptWithAuth(rules: Option[String] => Boolean): ActionBuilder[AuthenticatedRequest] = new ActionBuilder[AuthenticatedRequest] {

    def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
      Logger.info("invokeBlock")

      if (rules(request.headers.get("Accept"))) {
        if (requiresAuth) invokeAuthBlock(request, block)
        else block(AuthenticatedRequest(None, request))
      }
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
  }
}