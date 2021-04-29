/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.mobilemessages.controllers._
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

final case class Authority(
  nino:   Nino,
  userID: Option[String])

final case class AuthenticatedRequest[A](
  authority: Option[Authority],
  request:   Request[A])
    extends WrappedRequest(request)

case class AuthorityRecord(uri: String)

object AuthorityRecord {
  implicit val format: OFormat[AuthorityRecord] = Json.format[AuthorityRecord]
  implicit val reads:  Reads[AuthorityRecord]   = Json.reads[AuthorityRecord]
}

trait Authorisation extends Results with AuthorisedFunctions {

  lazy val requiresAuth: Boolean = true
  lazy val ninoNotFoundOnAccount = new NinoNotFoundOnAccount
  lazy val lowConfidenceLevel    = new AccountWithLowCL
  lazy val upstreamException     = new Upstream4xxResponse(("userId not found"), 401, 401)

  def grantAccess()(implicit hc: HeaderCarrier): Future[Authority] =
    authorised(ConfidenceLevel.L200)
      .retrieve(nino and internalId) {
        case None ~ _ => throw ninoNotFoundOnAccount
        case _ ~ None => throw upstreamException
        case Some(foundNino) ~ foundInternalId =>
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount else Future(Authority(Nino(foundNino), foundInternalId))
      }

  def invokeAuthBlock[A](
    request: Request[A],
    block:   AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = fromRequest(request)

    grantAccess()
      .flatMap { authority =>
        block(AuthenticatedRequest(Some(authority), request))
      }
      .recover {
        case _: uk.gov.hmrc.http.Upstream4xxResponse =>
          Logger.info("Unauthorized! Failed to grant access since 4xx response!")
          Unauthorized(toJson(ErrorUnauthorizedMicroService))

        case _: NinoNotFoundOnAccount =>
          Logger.info("Unauthorized! NINO not found on account!")
          Forbidden(toJson(ErrorForbidden))
      }
  }

}

trait AccessControl extends HeaderValidator with Authorisation {
  outer =>

  def validateAcceptWithAuth(rules: Option[String] => Boolean): ActionBuilder[AuthenticatedRequest, AnyContent] =
    new ActionBuilder[AuthenticatedRequest, AnyContent] {

      override def parser:                     BodyParser[AnyContent] = outer.parser
      override protected def executionContext: ExecutionContext       = outer.executionContext

      def invokeBlock[A](
        request: Request[A],
        block:   AuthenticatedRequest[A] => Future[Result]
      ): Future[Result] =
        if (rules(request.headers.get("Accept"))) {
          if (requiresAuth) invokeAuthBlock(request, block)
          else block(AuthenticatedRequest(None, request))
        } else Future.successful(Status(ErrorAcceptHeaderInvalid.httpStatusCode)(toJson(ErrorAcceptHeaderInvalid)))
    }
}
