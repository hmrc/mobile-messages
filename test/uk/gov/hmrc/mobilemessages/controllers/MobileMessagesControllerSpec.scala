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

import play.api.libs.json._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L100, L200}
import uk.gov.hmrc.auth.core.syntax.retrieved._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.controllers.auth.{Authority, AuthorityRecord}
import uk.gov.hmrc.mobilemessages.controllers.model.MessageHeaderResponseBody
import uk.gov.hmrc.mobilemessages.domain._
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs._
import uk.gov.hmrc.mobilemessages.utils.Setup
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class MobileMessagesControllerSpec extends UnitSpec with Setup {

  def readAndUnreadMessagesMock(response: Seq[MessageHeader]): Unit =
    (mockMobileMessagesService.readAndUnreadMessages()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(Future successful response)

  def readMessageContentMock(response: Html): Unit =
    (mockMobileMessagesService.readMessageContent(_: MessageId)(_: HeaderCarrier, _: ExecutionContext, _: Option[Authority]))
      .expects(*, *, *, *).returns(Future successful response)

  running(fakeApplication) {
    val controller = new MobileMessagesController(mockMobileMessagesService, mockAuthConnector, mockHttp, L200.level, "authUrl")

    "getMessages() Live" should {

      "return an empty list of messages successfully" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))
        readAndUnreadMessagesMock(Seq.empty)

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 200
        contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe Seq.empty[MessageHeaderResponseBody]
      }


      "return an empty list of messages successfully when journeyId is supplied" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))
        readAndUnreadMessagesMock(Seq.empty)

        val result: Result = await(controller.getMessages(journeyId)(emptyRequestWithAcceptHeader))

        status(result) shouldBe 200
        contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe Seq.empty[MessageHeaderResponseBody]
      }

      "return a list of messages successfully" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))
        readAndUnreadMessagesMock(messageServiceHeadersResponse)

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 200
        contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe getMessageResponseItemList
      }

      "return forbidden when authority record does not have correct confidence level" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L100 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 403
      }

      "return forbidden when authority record does not contain a NINO" in {
        stubAuthorisationGrantAccess(None and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 403
      }

      "return unauthorized when auth call fails" in {
        stubAuthoritySuccess(AuthorityRecord("uri"))
        stubAuthorisationUnauthorised()

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 401
      }

      "return status code 406 when the headers are invalid" in {
        val result: Result = await(controller.getMessages()(FakeRequest()))

        status(result) shouldBe 406
      }

      "return unauthorized when unable to retrieve authority record uri" in {
        stubAuthorityFailure()

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 401
      }
    }

    "read() Live" should {

      "read a valid html response from the read service" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))
        readMessageContentMock(html)

        val result: Result = await(controller.read()(readTimeRequest))

        status(result) shouldBe 200
        contentAsString(result) shouldBe html.toString()
      }

      "read a valid html response from the read service when a journeyId is supplied" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))
        readMessageContentMock(html)

        val result: Result = await(controller.read(journeyId)(readTimeRequest))

        status(result) shouldBe 200
        contentAsString(result) shouldBe html.toString()
      }

      "return forbidden when authority record does not have correct confidence level" in {
        stubAuthorisationGrantAccess(Some(nino.nino) and L100 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))

        val result: Result = await(controller.read(journeyId)(readTimeRequest))

        status(result) shouldBe 403
      }

      "return forbidden when authority record does not contain a NINO" in {
        stubAuthorisationGrantAccess(None and L200 and None)
        stubAuthoritySuccess(AuthorityRecord("uri"))

        val result: Result = await(controller.read(journeyId)(readTimeRequest))

        status(result) shouldBe 403
      }

      "return unauthorized when auth call fails" in {
        stubAuthorisationUnauthorised()
        stubAuthoritySuccess(AuthorityRecord("uri"))

        val result: Result = await(controller.read(journeyId)(readTimeRequest))

        status(result) shouldBe 401
      }

      "return status code 406 when the headers are invalid" in {
        val result: Result = await(controller.read(journeyId)(readTimeRequestNoAcceptHeader))

        status(result) shouldBe 406
      }

      "return unauthorized when unable to retrieve authority record uri" in {
        stubAuthorityFailure()

        val result: Result = await(controller.read(journeyId)(readTimeRequest))

        status(result) shouldBe 401
      }
    }
  }

  running(fakeApplication) {
    val controller = new SandboxMobileMessagesController()

    "getMessages() Sandbox" should {

      "return messages" in {

        import scala.language.postfixOps

        val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

        status(result) shouldBe 200

        val jsonResponse: JsValue = contentAsJson(result)
        val restTime: Long = (jsonResponse \ 0 \ "readTime").as[Long]
        jsonResponse shouldBe Json.parse(messages(restTime))

      }
    }

    "read() Sandbox" should {

      "return messages" in {
        val result: Result = await(controller.read()(readTimeRequest))

        status(result) shouldBe 200

        contentAsString(result) shouldEqual newTaxStatement.toString()
      }
    }
  }
}
