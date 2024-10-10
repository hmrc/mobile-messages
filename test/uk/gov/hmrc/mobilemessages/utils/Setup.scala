/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.mobilemessages.utils

import java.time.LocalDateTime
import eu.timepit.refined.auto._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.POST
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse}
import uk.gov.hmrc.mobilemessages.connector.MessageConnector
import uk.gov.hmrc.mobilemessages.controllers.auth.Authority
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
import uk.gov.hmrc.mobilemessages.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilemessages.domain.{MessageCount, MessageCountResponse, MessageHeader, MessageId, Shuttering}
import uk.gov.hmrc.mobilemessages.mocks.{AuditStub, AuthorisationStub, MessagesStub, ShutteringStub, StubApplicationConfiguration}
import uk.gov.hmrc.mobilemessages.services.MobileMessagesService
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted

import scala.concurrent.Future

trait Setup extends PlaySpec with AuthorisationStub with StubApplicationConfiguration with AuditStub with MockitoSugar {

  lazy val html = Html.apply("<div>some snippet</div>")

  lazy val emptyRequestWithAcceptHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)

  lazy val readTimeRequest: FakeRequest[JsValue] =
    fakeRequest(Json.toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))
      .withHeaders(acceptHeader)

  lazy val readTimeRequestNoAcceptHeader: FakeRequest[JsValue] = fakeRequest(
    Json.toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e")))
  )

  lazy val ReadSuccessEmptyResult: Future[AnyRef with HttpResponse] =
    Future.successful(HttpResponse(200, ""))

  implicit val reads:                Reads[MessageHeaderResponseBody] = Json.reads[MessageHeaderResponseBody]
  implicit val hc:                   HeaderCarrier                    = HeaderCarrier(Some(Authorization("authToken")))
  implicit val mockAuthConnector:    AuthConnector                    = mock[AuthConnector]
  implicit val mockMessageConnector: MessageConnector                 = mock[MessageConnector]
  implicit val authUser:             Option[Authority]                = Some(Authority(Nino("CS700100A"), Some("someId")))

  val shuttered =
    Shuttering(shuttered = true, Some("Shuttered"), Some("Messages are currently not available"))
  val notShuttered = Shuttering.shutteringDisabled

  val configuration: Configuration = Configuration("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")

  val nino:         Nino                     = Nino("CS700100A")
  val journeyId:    JourneyId                = "87144372-6bda-4cc9-87db-1d52fd96498f"
  val acceptHeader: (String, String)         = "Accept" -> "application/vnd.hmrc.1.0+json"
  val headers:      Map[String, Seq[String]] = Map("Accept" -> Seq("application/vnd.hmrc.1.0+json"))

  val encrypter: Encrypter with Decrypter =
    SymmetricCryptoFactory.aesCryptoFromConfig(baseConfigKey = "cookie.encryption", configuration.underlying)

  val message = new MessageServiceMock("authToken")

  val messageId = MessageId("id123")

  val messageServiceHeadersResponse: Seq[MessageHeader] = Seq(
    message.headerWith(id = "id1"),
    message.headerWith(id = "id2"),
    message.headerWith(id = "id3")
  )

  val messageCountResponse = MessageCountResponse(MessageCount(total = 2, unread = 1))

  val getMessageResponseItemList: Seq[MessageHeaderResponseBody] =
    MessageHeaderResponseBody.fromAll(messageHeaders = messageServiceHeadersResponse)(encrypter)

  val mockMobileMessagesService: MobileMessagesService = mock[MobileMessagesService]

  def fakeRequest(body: JsValue): FakeRequest[JsValue] =
    FakeRequest(POST, "url").withBody(body).withHeaders("Content-Type" -> "application/json")

  val timeNow: LocalDateTime = LocalDateTime.now
  val msgId1 = "543e8c6001000001003e4a9e"
  val msgId2 = "643e8c5f01000001003e4a8f"

  def messages(readTime: Long): String =
    s"""[{"id":"$msgId1","subject":"You have a new tax statement","validFrom":"${timeNow
         .minusDays(3)
         .toLocalDate}","readTime":$readTime,"readTimeUrl":"${encrypted(msgId1)}","sentInError":false},
       |{"id":"$msgId2","subject":"Stopping Self Assessment","validFrom":"${timeNow.toLocalDate}","readTimeUrl":"${encrypted(
         msgId2
       )}","sentInError":false}]""".stripMargin
}
