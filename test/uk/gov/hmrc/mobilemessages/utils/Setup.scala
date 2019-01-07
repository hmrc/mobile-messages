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

package uk.gov.hmrc.mobilemessages.utils

import java.util.UUID.randomUUID

import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.POST
import play.api.test.{FakeApplication, FakeRequest}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.crypto.CryptoWithKeysFromConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.mobilemessages.config.WSHttpImpl
import uk.gov.hmrc.mobilemessages.connector.MessageConnector
import uk.gov.hmrc.mobilemessages.controllers.auth.Authority
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
import uk.gov.hmrc.mobilemessages.domain.{MessageHeader, MessageId}
import uk.gov.hmrc.mobilemessages.services.MobileMessagesService
import uk.gov.hmrc.mobilemessages.mocks.{AuthorisationStub, MessagesStub, StubApplicationConfiguration}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.Future

trait Setup extends AuthorisationStub with MessagesStub with WithFakeApplication with StubApplicationConfiguration {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)
  lazy val html = Html.apply("<div>some snippet</div>")
  lazy val emptyRequestWithAcceptHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)
  lazy val readTimeRequest: FakeRequest[JsValue] = fakeRequest(Json.toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))
    .withHeaders(acceptHeader)
  lazy val readTimeRequestNoAcceptHeader: FakeRequest[JsValue] = fakeRequest(Json.toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))
  lazy val ReadSuccessEmptyResult: Future[AnyRef with HttpResponse] = Future.successful(HttpResponse(200, None, Map.empty, None))

  implicit val reads: Reads[MessageHeaderResponseBody] = Json.reads[MessageHeaderResponseBody]
  implicit val hc: HeaderCarrier = HeaderCarrier(Some(Authorization("authToken")))
  implicit val mockHttp: WSHttpImpl = mock[WSHttpImpl]
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAuditConnector: AuditConnector = mock[AuditConnector]
  implicit val mockMessageConnector: MessageConnector = mock[MessageConnector]
  implicit val authUser: Option[Authority] = Some(Authority(Nino("CS700100A"), L200, "someId"))

  val configuration: Configuration = fakeApplication.injector.instanceOf[Configuration]

  val nino = Nino("CS700100A")
  val journeyId = Option(randomUUID().toString)
  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  val encrypter: CryptoWithKeysFromConfig =
    new CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption", configuration.underlying)

  val message = new MessageServiceMock("authToken")

  val messageId = MessageId("id123")

  val messageServiceHeadersResponse: Seq[MessageHeader] = Seq(
    message.headerWith(id = "id1"),
    message.headerWith(id = "id2"),
    message.headerWith(id = "id3")
  )

  val getMessageResponseItemList: Seq[MessageHeaderResponseBody] =
    MessageHeaderResponseBody.fromAll(messageHeaders = messageServiceHeadersResponse)(encrypter)

  val mockMobileMessagesService: MobileMessagesService = mock[MobileMessagesService]

  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(POST, "url").
    withBody(body).
    withHeaders("Content-Type" -> "application/json")

  val timeNow: DateTime = DateTimeUtils.now
  val msgId1 = "543e8c6001000001003e4a9e"
  val msgId2 = "643e8c5f01000001003e4a8f"

  def messages(readTime: Long): String =
    s"""[{"id":"$msgId1","subject":"You have a new tax statement","validFrom":"${timeNow.minusDays(3).toLocalDate}","readTime":$readTime,"readTimeUrl":"${encrypted(msgId1)}","sentInError":false},
       |{"id":"$msgId2","subject":"Stopping Self Assessment","validFrom":"${timeNow.toLocalDate}","readTimeUrl":"${encrypted(msgId2)}","sentInError":false}]""".stripMargin
}