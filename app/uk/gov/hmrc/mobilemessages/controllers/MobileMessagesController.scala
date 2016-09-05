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

package uk.gov.hmrc.mobilemessages.controllers

import play.api.Logger
import play.api.libs.json._
import play.api.mvc.BodyParsers
import play.twirl.api.Html
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.crypto.{CryptoWithKeysFromConfig, Decrypter, Encrypter}
import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControlCheckAccessOff, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, MessageIdHiddenInUrl}
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}


trait MobileMessagesController extends BaseController with HeaderValidator with ErrorHandling {

  val service: MobileMessagesService
  val accessControl: AccountAccessControlWithHeaderCheck
  val crypto: Encrypter with Decrypter

  final def getMessages(journeyId: Option[String] = None) = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit authenticated =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(authenticated.request.headers, None)
      errorWrapper(service.readAndUnreadMessages().map((messageHeaders: Seq[MessageHeader]) =>
        Ok(Json.toJson(MessageHeaderResponseBody.fromAll(messageHeaders)(crypto)))
      ))
  }

  final def read(journeyId: Option[String] = None) = accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {
    implicit authenticated =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(authenticated.request.headers, None)

      val body: JsValue = authenticated.request.body
      body.validate[MessageIdHiddenInUrl].fold (
        errors => {
          Logger.warn("Received JSON error with read endpoint: " + errors)
          Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
        },
        messageIdHiddenInUrl => {
          implicit val auth = authenticated.authority
          errorWrapper(service.readMessageContent(messageIdHiddenInUrl.toMessageIdUsing(crypto)).
            map((as: Html) => Ok(as)))
        }
      )
  }
}

object SandboxMobileMessagesController extends MobileMessagesController {
  override val service = SandboxMobileMessagesService
  override val accessControl = AccountAccessControlCheckAccessOff
  override implicit val ec: ExecutionContext = ExecutionContext.global
  override val crypto: Encrypter with Decrypter = CryptoWithKeysFromConfig(
    baseConfigKey = "queryParameter.encryption"
  )
}

object LiveMobileMessagesController extends MobileMessagesController {
  override val service = LiveMobileMessagesService
  override val accessControl = AccountAccessControlWithHeaderCheck
  override implicit val ec: ExecutionContext = ExecutionContext.global
  override val crypto: Encrypter with Decrypter = CryptoWithKeysFromConfig(
    baseConfigKey = "queryParameter.encryption"
  )
}
