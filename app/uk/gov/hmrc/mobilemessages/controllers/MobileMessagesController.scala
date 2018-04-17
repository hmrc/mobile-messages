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

package uk.gov.hmrc.mobilemessages.controllers

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, BodyParsers}
import play.twirl.api.Html
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.crypto.{CryptoWithKeysFromConfig, Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControlCheckAccessOff, AccountAccessControlWithHeaderCheck, Authority}
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
import uk.gov.hmrc.play.HeaderCarrierConverter._
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent.Future

trait MobileMessagesController extends BaseController with HeaderValidator with ErrorHandling {

  val service: MobileMessagesService
  val accessControl: AccountAccessControlWithHeaderCheck
  val crypto: Encrypter with Decrypter

  final def getMessages(journeyId: Option[String] = None): Action[AnyContent] =
    accessControl.validateAccept(acceptHeaderValidationRules).async {
      implicit authenticated =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(authenticated.request.headers, None)
        errorWrapper(service.readAndUnreadMessages().map(
          (messageHeaders: Seq[MessageHeader]) =>
            Ok(Json.toJson(MessageHeaderResponseBody.fromAll(messageHeaders)(crypto)))
        ))
    }

  final def read(journeyId: Option[String] = None): Action[JsValue] =
    accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {
      implicit authenticated =>
        implicit val hc: HeaderCarrier = fromHeadersAndSession(authenticated.request.headers, None)

        authenticated.request.body.validate[RenderMessageRequest].fold(
          errors => {
            Logger.warn("Received JSON error with read endpoint: " + errors)
            Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
          },
          renderMessageRequest => {
            implicit val auth: Option[Authority] = authenticated.authority
            errorWrapper {
              service.readMessageContent(renderMessageRequest.toMessageIdUsing(crypto))
                .map((as: Html) => Ok(as))
            }
          }
        )
    }
}

class SandboxMobileMessagesController @Inject()(val service: SandboxMobileMessagesService,
                                                val accessControl: AccountAccessControlCheckAccessOff)
  extends MobileMessagesController {
  val crypto: Encrypter with Decrypter =
    CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")
}

class LiveMobileMessagesController @Inject()(val service: LiveMobileMessagesService,
                                             val accessControl: AccountAccessControlWithHeaderCheck)
  extends MobileMessagesController {
  val crypto: Encrypter with Decrypter =
    CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")
}