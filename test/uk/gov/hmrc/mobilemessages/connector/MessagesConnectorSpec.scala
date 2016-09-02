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

package uk.gov.hmrc.mobilemessages.connector

import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, Writes}
import play.api.test.FakeApplication
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilemessages.acceptance.microservices.MessageService
import uk.gov.hmrc.mobilemessages.controller.StubApplicationConfiguration
import uk.gov.hmrc.mobilemessages.domain.{MessageId, RenderMessageLocation}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessagesConnectorSpec
  extends UnitSpec
    with WithFakeApplication with ScalaFutures with StubApplicationConfiguration {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

  private trait Setup {
    implicit lazy val hc = HeaderCarrier(Some(Authorization("some value")))

    lazy val html = Html.apply("<div>some snippet</div>")
    val saUtr = SaUtr("1234567890")
    val nino = Nino("CS700100A")
    val responseRenderer = RenderMessageLocation("sa-message-renderer", "http://somelocation")


    val message = new MessageService("authToken")

    lazy val http500Response = Future.failed(new Upstream5xxResponse("Error", 500, 500))
    lazy val http400Response = Future.failed(new BadRequestException("bad request"))

    lazy val successfulEmptyResponse = Future.successful(
      HttpResponse(200, responseString = Some(""))
    )

    lazy val successfulEmptyMessageHeadersResposne = Future.successful(
      HttpResponse(200, Some(Json.parse(message.jsonRepresentationOf(Seq.empty))))
    )

    lazy val successfulMessageHeadersResponse = Future.successful(
      HttpResponse(
        200,
        Some(Json.parse(
          message.jsonRepresentationOf(
            Seq(
              message.headerWith(id = "someId1"),
              message.headerWith(id = "someId2"),
              message.headerWith(id = "someId3")
            )
          ))))
    )

    val messageId = MessageId("id123")
    lazy val successfulSingleMessageResponse = Future.successful(
      HttpResponse(
        200,
        Some(Json.parse(
          message.jsonRepresentationOf(
            message.bodyWith(id = "id123")
          ))))
    )

    val sampleMessage = message.convertedFrom(message.bodyWith("id1"))

    lazy val ReadSuccessResult = Future.successful(HttpResponse(200, None, Map.empty, Some(html.toString())))
    lazy val PostSuccessResult = Future.successful(HttpResponse(200, Some(Json.toJson(responseRenderer))))
    lazy val PostConflictResult = Future.successful(HttpResponse(409, Some(Json.toJson(responseRenderer))))

    lazy val responseGet: Future[HttpResponse] = http400Response
    lazy val responsePost: Future[HttpResponse] = PostSuccessResult

    implicit val authUser: Option[Authority] = Some(Authority(nino, ConfidenceLevel.L200, "someId"))

    val connector = new MessageConnector {

      override def http = new HttpGet with HttpPost {
        override val hooks: Seq[HttpHook] = NoneRequired

        override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = responseGet

        override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = responsePost

        override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = responsePost

        override protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = responsePost

        override protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = responsePost

      }

      val hc = new HeaderCarrier()

      override def now: DateTime = DateTime.now()

      override val messageBaseUrl: String = "somebase-url"
      override val provider: String = "some provider"
      override val token: String = "token"
      override val id: String = "id"
    }

  }

  "messagesConnector messages" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      override lazy val responseGet = http400Response
      intercept[BadRequestException] {
        await(connector.messages(saUtr))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      override lazy val responseGet = http500Response
      intercept[Upstream5xxResponse] {
        await(connector.messages(saUtr))
      }
    }

    "return empty response when a 200 response is received with an empty payload" in new Setup {
      override lazy val responseGet = successfulEmptyMessageHeadersResposne
      await(connector.messages(saUtr)) shouldBe Seq.empty
    }

    "return a list of items when a 200 response is received with a payload" in new Setup {
      override lazy val responseGet = successfulMessageHeadersResponse
      await(connector.messages(saUtr)) shouldBe Seq(
        message.headerWith(id = "someId1"),
        message.headerWith(id = "someId2"),
        message.headerWith(id = "someId3")
      )
    }

  }

  "messagesConnector render message" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      override lazy val responseGet = http400Response
      intercept[BadRequestException] {
        await(connector.render(sampleMessage, hc))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      override lazy val responseGet = http500Response
      intercept[Upstream5xxResponse] {
        await(connector.render(sampleMessage, hc))
      }
    }

    s"return empty response when a 200 response is received with an empty payload" in new Setup {
      override lazy val responseGet = successfulEmptyResponse
      await(connector.render(sampleMessage, hc)).body shouldBe ""
    }

    "return a rendered message when a 200 response is received with a payload" in new Setup {
      override lazy val responseGet = ReadSuccessResult
      await(connector.render(sampleMessage, hc)).body shouldBe html.body
    }
  }

  "messagesConnector get message by id" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      override lazy val responseGet = http400Response
      intercept[BadRequestException] {
        await(connector.getMessageBy(messageId))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      override lazy val responseGet = http500Response
      intercept[Upstream5xxResponse] {
        await(connector.getMessageBy(messageId))
      }
    }

    "throw NullPointerException when a 200 response is received with an empty payload" in new Setup {
      override lazy val responseGet = successfulEmptyResponse

      intercept[NullPointerException] {
        await(connector.getMessageBy(messageId))
      }
    }

    "return a message when a 200 response is received with a payload" in new Setup {
      override lazy val responseGet = successfulSingleMessageResponse
      await(connector.getMessageBy(messageId)) shouldBe message.convertedFrom(
        message.bodyWith(id = messageId.value)
      )
    }
  }
}
