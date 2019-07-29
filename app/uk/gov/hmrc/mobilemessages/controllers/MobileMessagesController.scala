/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.mobilemessages.controllers

import com.google.inject.Singleton
import javax.inject.{Inject, Named}
import play.api.libs.crypto.CookieSigner
import play.api.libs.json._
import play.api.mvc._
import play.api.{Configuration, Logger}
import play.twirl.api.Html
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.crypto.{CryptoWithKeysFromConfig, Decrypter, Encrypter}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.mobilemessages.controllers.auth.{AccessControl, Authority}
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.mobilemessages.sandbox.DomainGenerator.{nextSaUtr, readMessageHeader, unreadMessageHeader}
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs._
import uk.gov.hmrc.mobilemessages.services.MobileMessagesService
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MobileMessagesController @Inject()(
  val service:                                                  MobileMessagesService,
  override val authConnector:                                   AuthConnector,
  override val http:                                            CoreGet,
  configuration:                                                Configuration,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  @Named("auth") val authUrl:                                   String,
  val controllerComponents:                                     ControllerComponents,
  cookieSigner:                                                 CookieSigner
)(
  implicit val executionContext: ExecutionContext
) extends BackendBaseController
    with HeaderValidator
    with ErrorHandling
    with AccessControl {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  val crypto: Encrypter with Decrypter =
    new CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption", configuration.underlying)

  def getMessages(journeyId: String): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules).async { implicit authenticated =>
      errorWrapper(
        service
          .readAndUnreadMessages()
          .map(
            (messageHeaders: Seq[MessageHeader]) => Ok(Json.toJson(MessageHeaderResponseBody.fromAll(messageHeaders)(crypto)))
          ))
    }

  def read(journeyId: String): Action[JsValue] =
    validateAcceptWithAuth(acceptHeaderValidationRules).async(controllerComponents.parsers.json) { implicit authenticated =>
      authenticated.request.body
        .validate[RenderMessageRequest]
        .fold(
          errors => {
            Logger.warn("Received JSON error with read endpoint: " + errors)
            Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
          },
          renderMessageRequest => {
            implicit val auth: Option[Authority] = authenticated.authority
            errorWrapper {
              service
                .readMessageContent(renderMessageRequest.toMessageIdUsing(crypto))
                .map((as: Html) => Ok(as))
            }
          }
        )
    }

}

@Singleton
class SandboxMobileMessagesController @Inject()(
  config:                   Configuration,
  val controllerComponents: ControllerComponents
)(
  implicit val executionContext: ExecutionContext
) extends BackendBaseController
    with HeaderValidator {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  val crypto: Encrypter with Decrypter =
    new CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption", config.underlying)

  val saUtr: SaUtr = nextSaUtr

  def getMessages(journeyId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      Future successful (request.headers.get("SANDBOX-CONTROL") match {
        case Some("ERROR-401") => Unauthorized
        case Some("ERROR-403") => Forbidden
        case Some("ERROR-500") => InternalServerError
        case _                 => Ok(Json.toJson(MessageHeaderResponseBody.fromAll(Seq(readMessageHeader(saUtr), unreadMessageHeader(saUtr)))(crypto)))
      })
    }

  def read(journeyId: String): Action[JsValue] =
    validateAccept(acceptHeaderValidationRules).async(controllerComponents.parsers.json) { implicit request =>
      Future successful (request.headers.get("SANDBOX-CONTROL") match {
        case Some("ANNUAL-TAX-SUMMARY") => Ok(annualTaxSummary)
        case Some("STOPPING-SA")        => Ok(stoppingSA)
        case Some("OVERDUE-PAYMENT")    => Ok(overduePayment)
        case Some("ERROR-401")          => Unauthorized
        case Some("ERROR-403")          => Forbidden
        case Some("ERROR-500")          => InternalServerError
        case _                          => Ok(newTaxStatement)
      })
    }
}
