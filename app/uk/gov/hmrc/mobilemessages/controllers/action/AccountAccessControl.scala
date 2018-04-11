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

package uk.gov.hmrc.mobilemessages.controllers.action

import javax.inject.{Inject, Named}

import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, OFormat, Reads}
import play.api.mvc._
import uk.gov.hmrc.api.controllers.{ErrorAcceptHeaderInvalid, ErrorUnauthorizedLowCL, HeaderValidator}
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{confidenceLevel, nino}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{Request => _, _}
import uk.gov.hmrc.mobilemessages.controllers.{ErrorUnauthorizedNoNino, ForbiddenAccess}
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

final case class AuthenticatedRequest[A](authority: Option[Authority], request: Request[A]) extends WrappedRequest(request)

final case class Authority(nino: Nino, cl: ConfidenceLevel, authId: String)

class NinoNotFoundOnAccount(message: String) extends HttpException(message, 401)

class AccountWithLowCL(message: String) extends HttpException(message, 401)

class AccountAccessControl @Inject()(val authConnector: AuthConnector,
                                     val http: CoreGet,
                                     @Named("auth") val authUrl: String,
                                     @Named("controllers.confidenceLevel") val serviceConfidenceLevel: Int) extends ActionBuilder[AuthenticatedRequest] with Results with AuthorisedFunctions {

  val missingNinoException = new UnauthorizedException("The user must have a National Insurance Number to access this service")

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None)

    grantAccess().flatMap {
      authority => {
        block(AuthenticatedRequest(Some(authority), request))
      }
    }.recover {
      case _: Upstream4xxResponse => Unauthorized(toJson(ErrorUnauthorizedNoNino))

      case _: ForbiddenException =>
        Logger.info("Unauthorized! ForbiddenException caught and returning 403 status!")
        Forbidden(toJson(ForbiddenAccess))

      case _: NinoNotFoundOnAccount =>
        Logger.info("Unauthorized! NINO not found on account!")
        Unauthorized(toJson(ErrorUnauthorizedNoNino))

      case _: AccountWithLowCL =>
        Logger.info("Unauthorized! Account with low CL!")
        Unauthorized(toJson(ErrorUnauthorizedLowCL))
    }
  }

  def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = {
    getAuthorityRecord.flatMap { authRecord: AuthorityRecord =>
      authorised().retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel ⇒
          if (foundNino.isEmpty) throw missingNinoException
          else if (serviceConfidenceLevel > foundConfidenceLevel.level)
            throw new ForbiddenException("The user does not have sufficient permissions to access this service")
          else Future successful Authority(Nino(foundNino), foundConfidenceLevel, authRecord.uri)
        case None ~ _ ⇒
          throw missingNinoException
      }
    }
  }

  implicit val reads: Reads[AuthorityRecord] = Json.reads[AuthorityRecord]

  def getAuthorityRecord(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuthorityRecord] = {
    http.GET[AuthorityRecord](s"$authUrl/auth/authority")
  }

  case class AuthorityRecord(uri: String)

  object AuthorityRecord {
    implicit val format: OFormat[AuthorityRecord] = Json.format[AuthorityRecord]
  }

}

class AccountAccessControlWithHeaderCheck @Inject()(val accessControl: AccountAccessControl) extends HeaderValidator {
  val checkAccess = true

  override def validateAccept(rules: Option[String] => Boolean) = new ActionBuilder[AuthenticatedRequest] {

    def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
      if (rules(request.headers.get("Accept"))) {
        if (checkAccess) accessControl.invokeBlock(request, block)
        else block(AuthenticatedRequest(None, request))
      }
      else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
  }
}

class AccountAccessControlCheckAccessOff @Inject()(override val accessControl: AccountAccessControl)
  extends AccountAccessControlWithHeaderCheck(accessControl) {
  override val checkAccess = false
}

class AccountAccessControlSandbox @Inject()(@Named("auth") override val authUrl: String,
                                            override val http: HttpGet,
                                            override val authConnector: AuthConnector,
                                            override val serviceConfidenceLevel: Int = 0)
  extends AccountAccessControl(authConnector, http, authUrl, serviceConfidenceLevel) {
  override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] =
    Future.failed(new IllegalArgumentException("Sandbox mode!"))
}