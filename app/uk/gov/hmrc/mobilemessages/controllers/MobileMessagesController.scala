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
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControlCheckAccessOff, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.mobilemessages.domain.ReadTimeUrl
import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}


trait MobileMessagesController extends BaseController with HeaderValidator with ErrorHandling {

  val service: MobileMessagesService
  val accessControl: AccountAccessControlWithHeaderCheck

  final def getMessages = accessControl.validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)
      errorWrapper(service.readAndUnreadMessages().map(as => Ok(Json.toJson(as))))
  }

  final def read = accessControl.validateAccept(acceptHeaderValidationRules).async(BodyParsers.parse.json) {
    implicit request =>
      implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)

      request.body.validate[ReadTimeUrl].fold (
        errors => {
          Logger.warn("Received error with read endpoint: " + errors)
          Future.successful(BadRequest(Json.toJson(ErrorGenericBadRequest(errors))))
        },
        readMessage => {
          errorWrapper(service.readMessageContent(readMessage.url).map(as => Ok(as)))
        }
      )
  }
}

object SandboxMobileMessagesController extends MobileMessagesController {
  override val service = SandboxMobileMessagesService
  override val accessControl = AccountAccessControlCheckAccessOff
  override implicit val ec: ExecutionContext = ExecutionContext.global
}

object LiveMobileMessagesController extends MobileMessagesController {
  override val service = LiveMobileMessagesService
  override val accessControl = AccountAccessControlWithHeaderCheck
  override implicit val ec: ExecutionContext = ExecutionContext.global
}
