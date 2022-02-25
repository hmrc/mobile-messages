/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.http.SecretConfiguration
import play.api.libs.crypto.{CookieSigner, DefaultCookieSigner}
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.syntax.retrieved._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.controllers.auth.Authority
import uk.gov.hmrc.mobilemessages.controllers.model.MessageHeaderResponseBody
import uk.gov.hmrc.mobilemessages.domain._
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs._
import uk.gov.hmrc.mobilemessages.services.MessageWithHeader
import uk.gov.hmrc.mobilemessages.utils.Setup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class MobileMessagesControllerSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with Setup {

  val cookieSigner: CookieSigner = new DefaultCookieSigner(SecretConfiguration("hwdODU8hulPkolIryPRkVW=="))

  def readAndUnreadMessagesMock(response: Seq[MessageHeader]): Unit =
    (mockMobileMessagesService
      .readAndUnreadMessages()(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *)
      .returns(Future successful response)

  def readMessageContentMock(response: Html): Unit =
    (mockMobileMessagesService
      .readMessageContent(_: MessageId)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returns(
        Future successful MessageWithHeader(response,
                                            Some("2wsm-advisor"),
                                            Some("9794f96d-f595-4b03-84dc-1861408918fb"))
      )

  val userId: Option[String] = Some("userId123")

  val liveController =
    new MobileMessagesController(
      mockMobileMessagesService,
      mockAuthConnector,
      Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")),
      stubControllerComponents(),
      cookieSigner,
      mockShutteringConnector
    )

  "getMessages() Live" should {

    "return an empty list of messages successfully" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readAndUnreadMessagesMock(Seq.empty)
      stubShutteringResponse(notShuttered)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result)                                           shouldBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe Seq.empty[MessageHeaderResponseBody]
    }

    "return an empty list of messages successfully when journeyId is supplied" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readAndUnreadMessagesMock(Seq.empty)
      stubShutteringResponse(notShuttered)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result)                                           shouldBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe Seq.empty[MessageHeaderResponseBody]
    }

    "return a list of messages successfully" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readAndUnreadMessagesMock(messageServiceHeadersResponse)
      stubShutteringResponse(notShuttered)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result)                                           shouldBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe getMessageResponseItemList
    }

    "return forbidden when authority record does not contain a NINO" in {
      stubAuthorisationGrantAccess(None and userId)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) shouldBe 403
    }

    "return unauthorized when auth call fails" in {
      stubAuthorisationUnauthorised()

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = liveController.getMessages(journeyId)(FakeRequest())

      status(result) shouldBe 406
    }

    "return unauthorized when unable to retrieve authority record uri" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and None)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) shouldBe 401
    }

    "return 521 when shuttered" in {
      stubShutteringResponse(shuttered)
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)

      val result = liveController.getMessages(journeyId)(emptyRequestWithAcceptHeader)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message").as[String]    shouldBe "Messages are currently not available"
    }
  }

  "read() Live" should {

    "read a valid html response and header from the read service" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)
      readMessageContentMock(html)
      stubShutteringResponse(notShuttered)

      val result = liveController.read(journeyId)(readTimeRequest)

      val `type`   = Await.result(result.map[String](h => h.header.headers("type")), 1 second)
      val threadId = Await.result(result.map[String](h => h.header.headers("threadId")), 1 second)

      status(result)          shouldBe 200
      contentAsString(result) shouldBe html.toString()
      `type`                  shouldBe "2wsm-advisor"
      threadId                shouldBe "9794f96d-f595-4b03-84dc-1861408918fb"
    }

    "return forbidden when authority record does not contain a NINO" in {
      stubAuthorisationGrantAccess(None and userId)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) shouldBe 403
    }

    "return unauthorized when auth call fails" in {
      stubAuthorisationUnauthorised()

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in {
      val result = liveController.read(journeyId)(readTimeRequestNoAcceptHeader)

      status(result) shouldBe 406
    }

    "return unauthorized when unable to retrieve authority record uri" in {
      stubAuthorisationGrantAccess(Some(nino.nino) and None)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) shouldBe 401
    }

    "return 521 when shuttered" in {
      stubShutteringResponse(shuttered)
      stubAuthorisationGrantAccess(Some(nino.nino) and userId)

      val result = liveController.read(journeyId)(readTimeRequest)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message").as[String]    shouldBe "Messages are currently not available"
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

      status(result) shouldBe 200

      val jsonResponse: JsValue = contentAsJson(result)
      val restTime:     Long    = (jsonResponse \ 0 \ "readTime").as[Long]
      jsonResponse shouldBe Json.parse(messages(restTime))
    }
  }

  "read() Sandbox" should {

    "return messages" in {
      val result = sandboxController.read(journeyId)(readTimeRequest)

      status(result) shouldBe 200

      contentAsString(result) shouldEqual newTaxStatement.toString()
    }
  }
}
