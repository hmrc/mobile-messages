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

import play.api.libs.json._
import play.api.{Logger, mvc}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControlForSandbox, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
import uk.gov.hmrc.play.http.{ForbiddenException, HeaderCarrier, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

trait ErrorHandling {
  self:BaseController =>

  import scala.concurrent.ExecutionContext.Implicits.global

  def errorWrapper(func: => Future[mvc.Result])(implicit hc:HeaderCarrier) = {
    func.recover {
      case ex:NotFoundException => Status(ErrorNotFound.httpStatusCode)(Json.toJson(ErrorNotFound))

      case ex:UnauthorizedException => Unauthorized(Json.toJson(ErrorUnauthorizedNoNino))

      case ex:ForbiddenException => Unauthorized(Json.toJson(ErrorUnauthorizedLowCL))

      case e: Throwable =>
        Logger.error(s"Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(Json.toJson(ErrorInternalServerError))
    }
  }
}

trait MobileMessagesController extends BaseController with HeaderValidator with ErrorHandling {

  import scala.concurrent.ExecutionContext.Implicits.global

  val service: MobileMessagesService
  val accessControl:AccountAccessControlWithHeaderCheck

  final def ping() = accessControl.validateAccept(acceptHeaderValidationRules).async {
  implicit request =>
    implicit val hc = HeaderCarrier.fromHeadersAndSession(request.headers, None)
    errorWrapper(service.ping().map(as => Ok(Json.toJson(as))))
  }
}

object SandboxMobileMessagesController extends MobileMessagesController {
  override val service = SandboxMobileMessagesService
  override val accessControl = AccountAccessControlForSandbox
}

object LiveMobileMessagesController extends MobileMessagesController {
  override val service = LiveMobileMessagesService
  override val accessControl = AccountAccessControlWithHeaderCheck
}
