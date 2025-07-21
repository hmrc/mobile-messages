/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.SecretConfiguration
import play.api.libs.crypto.{CookieSigner, DefaultCookieSigner}
import play.api.libs.json.*
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.syntax.retrieved.*
import uk.gov.hmrc.mobilemessages.connector.ShutteringConnector
import uk.gov.hmrc.mobilemessages.controllers.model.MessageHeaderResponseBody
import uk.gov.hmrc.mobilemessages.domain.*
import uk.gov.hmrc.mobilemessages.mocks.{MessagesStub, ShutteringStub}
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs.*
import uk.gov.hmrc.mobilemessages.services.RenderedMessage
import uk.gov.hmrc.mobilemessages.utils.Setup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MobileMessagesControllerSpec extends Setup with ShutteringStub {

  implicit val shutteringConnectorMock: ShutteringConnector =
    new ShutteringConnector(http = mockHttpClient, serviceUrl = s"http://baseUrl")

  val cookieSigner: CookieSigner = new DefaultCookieSigner(SecretConfiguration("hwdODU8hulPkolIryPRkVW=="))

  def readAndUnreadMessagesMock(response: Seq[MessageHeader]): Unit =
    when(mockMobileMessagesService.readAndUnreadMessages()(any(), any())).thenReturn(Future successful response)

  def readMessageCountMock(response: MessageCountResponse): Unit =
    when(mockMobileMessagesService.countOnlyMessages()(any(), any())).thenReturn(Future successful response)

  def readMessageContentMock(response: Html): Unit =
    when(mockMobileMessagesService.readMessageContent(any())(any(), any()))
      .thenReturn(Future successful RenderedMessage(response))

  val userId: Option[String] = Some("userId123")

  val liveController =
    new MobileMessagesController(
      mockMobileMessagesService,
      mockAuthConnector,
      Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")),
      stubControllerComponents(),
      shutteringConnectorMock
    )

  "getMessages() Live" should {

    "return an empty list of messages successfully" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      stubShutteringResponse(notShuttered)
      readAndUnreadMessagesMock(Seq.empty)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] mustBe (Seq.empty[MessageHeaderResponseBody])
    }

    "return an empty list of messages successfully when journeyId is supplied" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readAndUnreadMessagesMock(Seq.empty)
      stubShutteringResponse(notShuttered)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] mustBe (Seq.empty[MessageHeaderResponseBody])
    }

    "return a list of messages successfully" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readAndUnreadMessagesMock(messageServiceHeadersResponse)
      stubShutteringResponse(notShuttered)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] mustBe getMessageResponseItemList
    }

    "return forbidden when authority record does not contain a NINO" in {
      stubAuthorisationGrantAccess(None and userId)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 403
    }

    "return unauthorized when auth call fails" in {
      stubAuthorisationUnauthorised()

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = liveController.getMessages(journeyId)(FakeRequest())

      status(result) mustBe 406
    }

    "return unauthorized when unable to retrieve authority record uri" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and None)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 401
    }

    "return 521 when shuttered" in {
      stubShutteringResponse(shuttered)
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message").as[String] mustBe "Messages are currently not available"
    }
  }

  "getMessageCount()" should {

    "return the message count successfully" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readMessageCountMock(messageCountResponse)
      stubShutteringResponse(notShuttered)

      val result = liveController.getMessageCount(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 200
      val response = contentAsJson(result).as[MessageCountResponse]
      response.count.total mustBe 2
      response.count.unread mustBe 1
    }

    "return forbidden when authority record does not contain a NINO" in {
      stubAuthorisationGrantAccess(None and userId)

      val result = liveController.getMessageCount(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 403
    }

    "return unauthorized when auth call fails" in {
      stubAuthorisationUnauthorised()

      val result = liveController.getMessageCount(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = liveController.getMessageCount(journeyId)(FakeRequest())

      status(result) mustBe 406
    }

    "return 521 when shuttered" in {
      stubShutteringResponse(shuttered)
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)

      val result = liveController.getMessageCount(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message").as[String] mustBe "Messages are currently not available"
    }
  }

  "read() Live" should {

    "read a valid html response and header from the read service" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readMessageContentMock(html)
      stubShutteringResponse(notShuttered)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) mustBe 200
      contentAsString(result) mustBe html.toString()
    }

    "return forbidden when authority record does not contain a NINO" in {
      stubAuthorisationGrantAccess(None and userId)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) mustBe 403
    }

    "return unauthorized when auth call fails" in {
      stubAuthorisationUnauthorised()

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) mustBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = liveController.read(journeyId)(readTimeRequestNoAcceptHeader)

      status(result) mustBe 406
    }

    "return unauthorized when unable to retrieve authority record uri" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and None)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) mustBe 401
    }

    "return 521 when shuttered" in {
      stubShutteringResponse(shuttered)
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) mustBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] mustBe true
      (jsonBody \ "title").as[String] mustBe "Shuttered"
      (jsonBody \ "message").as[String] mustBe "Messages are currently not available"
    }
  }

  val sandboxController =
    new SandboxMobileMessagesController(
      Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")),
      stubControllerComponents()
    )

  "getMessages() Sandbox" should {
    "return messages" in {
      val result = sandboxController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 200

      val jsonResponse: JsValue = contentAsJson(result)
      val restTime: Long = (jsonResponse \ 0 \ "readTime").as[Long]
      jsonResponse mustBe Json.parse(messages(restTime))
    }
  }
  "getMessageCount() Sandbox" should {
    "return message count" in {
      val result = sandboxController.getMessageCount(journeyId)(emptyRequestWithAcceptHeader)

      status(result) mustBe 200

      val jsonResponse: MessageCountResponse = contentAsJson(result).as[MessageCountResponse]
      jsonResponse.count.total mustBe 2
      jsonResponse.count.unread mustBe 1
    }
  }

  "read() Sandbox" should {

    "return messages" in {
      val result = sandboxController.read(journeyId)(readTimeRequest)

      status(result) mustBe 200

      contentAsString(result) mustEqual newTaxStatement.toString()
    }
  }
}
