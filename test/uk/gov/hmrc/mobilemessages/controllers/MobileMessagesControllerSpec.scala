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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.Result
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilemessages.controllers.action.Authority
import uk.gov.hmrc.mobilemessages.controllers.model.MessageHeaderResponseBody
import uk.gov.hmrc.mobilemessages.domain.MessageId
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.{ExecutionContext, Future}

class MobileMessagesControllerSpec extends UnitSpec with WithFakeApplication with StubApplicationConfiguration {

  implicit val reads: Reads[MessageHeaderResponseBody] = Json.reads[MessageHeaderResponseBody]

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

  "read messages Live" should {

    "read a valid html response from the read service" in new Success {
      when(testMMService.readMessageContent(any[MessageId])(any[HeaderCarrier], any[ExecutionContext], any[Option[Authority]]))
        .thenReturn(Future successful html)

      val result: Result = await(controller.read()(readTimeRequest))

      status(result) shouldBe 200
      contentAsString(result) shouldBe html.toString()
    }

    "read a valid html response from the read service when a journeyId is supplied" in new Success {
      when(testMMService.readMessageContent(any[MessageId])(any[HeaderCarrier], any[ExecutionContext], any[Option[Authority]]))
        .thenReturn(Future successful html)

      val result: Result = await(controller.read(journeyId)(readTimeRequest))

      status(result) shouldBe 200
      contentAsString(result) shouldBe html.toString()
    }

    "return unauthorized when authority record does not contain a NINO" in new AuthWithoutNino {
      val result: Result = await(controller.read()(readTimeRequest))

      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in new Success {
      val result: Result = await(controller.read()(readTimeRequestNoHeaders))

      status(result) shouldBe 406
    }
  }

  "read messages Sandbox" should {

    "return the messages" in new SandboxSuccess {
      val result: Result = await(controller.read()(readTimeRequest))

      status(result) shouldBe 200

      contentAsString(result) shouldEqual MessageContentPartialStubs.newTaxStatement.toString()
    }
  }

  "getMessages Live" should {

    "return an empty list of messages successfully" in new Success {
      when(testMMService.readAndUnreadMessages()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future successful Seq.empty)

      val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

      status(result) shouldBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe Seq.empty[MessageHeaderResponseBody]
    }


    "return an empty list of messages successfully when journeyId is supplied" in new Success {
      when(testMMService.readAndUnreadMessages()(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future successful Seq.empty)

      val result: Result = await(controller.getMessages(journeyId)(emptyRequestWithAcceptHeader))

      status(result) shouldBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe Seq.empty[MessageHeaderResponseBody]
    }

    "return a list of messages successfully" in new SuccessWithMessages {

      val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

      status(result) shouldBe 200
      contentAsJson(result).as[Seq[MessageHeaderResponseBody]] shouldBe getMessagesResponseItemsList
    }

    "return forbidden when authority record does not have correct confidence level" in new AuthWithLowCL {
      val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

      status(result) shouldBe 403
    }

    "return unauthorized when authority record does not contain a NINO" in new AuthWithoutNino {
      val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

      status(result) shouldBe 401
    }

    "return status code 406 when the headers are invalid" in new Success {
      val result: Result = await(controller.getMessages()(emptyRequest))

      status(result) shouldBe 406
    }
  }

  "getMessages Sandbox" should {

    "return the messages" in new SandboxSuccess {

      import scala.language.postfixOps

      val result: Result = await(controller.getMessages()(emptyRequestWithAcceptHeader))

      status(result) shouldBe 200

      val jsonResponse: JsValue = contentAsJson(result)
      val restTime: Long = (jsonResponse \ 0 \ "readTime").as[Long]
      jsonResponse shouldBe Json.parse(messages(restTime))

    }
  }
}
