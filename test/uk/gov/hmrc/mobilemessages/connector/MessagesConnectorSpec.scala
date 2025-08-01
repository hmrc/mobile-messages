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

package uk.gov.hmrc.mobilemessages.connector

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.SecretConfiguration
import play.api.i18n.Lang.logger
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.SERVICE_UNAVAILABLE
import uk.gov.hmrc.http.*
import uk.gov.hmrc.mobilemessages.connector.model.{ResourceActionLocation, UpstreamMessageHeadersResponse, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.domain.*
import uk.gov.hmrc.mobilemessages.mocks.ShutteringStub
import uk.gov.hmrc.mobilemessages.utils.Setup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MessagesConnectorSpec extends Setup with ShutteringStub {

  implicit val formats: OFormat[RenderMessageLocation] = Json.format[RenderMessageLocation]

  val responseRenderer = RenderMessageLocation("sa-message-renderer", "http://somelocation.com/")
  val renderPath       = "/some/render/path"

  val messageBodyToRender: UpstreamMessageResponse =
    message.bodyWith(id = "id1", renderUrl = ResourceActionLocation("test-renderer-service", renderPath))
  val messageToBeMarkedAsReadBody: UpstreamMessageResponse = message.bodyToBeMarkedAsReadWith(id = "id48")

  val messageToBeMarkedAsRead: UnreadMessage =
    UnreadMessage(
      id            = MessageId(messageToBeMarkedAsReadBody.id),
      renderUrl     = messageToBeMarkedAsReadBody.renderUrl.url,
      markAsReadUrl = "http://markasreadurl.com/"
    )

  lazy val PostSuccessResult: Future[AnyRef with HttpResponse] =
    Future.successful(HttpResponse(200, toJson(html.body), headers))

  lazy val PostSuccessResultCy: Future[AnyRef with HttpResponse] =
    Future.successful(HttpResponse(200, toJson(htmlCy.body), headers))

  lazy val PostSuccessRendererResult: Future[AnyRef with HttpResponse] =
    Future.successful(HttpResponse(200, toJson(responseRenderer), headers))

  lazy val connector: MessageConnector =
    new MessageConnector(
      message.fullUrlFor("secure-message", ""),
      message.fullUrlFor("sa-message-renderer", ""),
      message.fullUrlFor("ats-message-renderer", ""),
      message.fullUrlFor("secure-message-renderer", ""),
      message.fullUrlFor("two-way-message", ""),
      Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")),
      new DefaultCookieSigner(SecretConfiguration("hwdODU8hulPkolIryPRkVW==")),
      mockHttpClient
    )

  private val upstream5xxResponse     = UpstreamErrorResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)
  private val badRequestException     = new BadRequestException("")
  private val tooManyRequestException = new TooManyRequestException("")

  "messages()" should {

    "return a list of items when a 200 response is received with a payload" in {
      val messagesHeaders =
        Seq(message.headerWith(id = "someId1"), message.headerWith(id = "someId2"), message.headerWith(id = "someId3"))
      when(requestBuilderExecute[UpstreamMessageHeadersResponse])
        .thenReturn(Future.successful(UpstreamMessageHeadersResponse(messagesHeaders)))

      connector.messages(Some("en")) onComplete {
        case Success(_) => messagesHeaders
        case Failure(_) =>
      }

    }

    "throw BadRequestException when a 400 response is returned" in {

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.failed(badRequestException))
      connector.messages(Some("en")) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.failed(upstream5xxResponse))

      connector.messages(Some("en")) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }

    "throw TooManyRequestException when a 429 is returned" in {

      when(requestBuilderExecute[HttpResponse])
        .thenReturn(Future.failed(tooManyRequestException))

      connector.messages(Some("en")) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }

    "return empty response when a 200 response is received with an empty payload" in {

      when(requestBuilderExecute[UpstreamMessageHeadersResponse])
        .thenReturn(Future.successful(UpstreamMessageHeadersResponse(Seq.empty)))

      connector.messages(Some("en")) onComplete {
        case Success(_) => Seq.empty
        case Failure(_) =>
      }
    }
  }

  "getMessageBy(messageId)" should {

    "return a message when a 200 response is received with a payload" in {
      val messageBodyToRender: UpstreamMessageResponse =
        message.bodyWith(id = "id123", renderUrl = ResourceActionLocation("test-renderer-service", renderPath))
      when(requestBuilderExecute[UpstreamMessageResponse]).thenReturn(Future.successful(messageBodyToRender))

      connector.getMessageBy(messageId, Some("en")) onComplete {
        case Success(_) =>
          message.convertedFrom(
            message.bodyWith(id = messageId.value)
          )
        case Failure(_) =>
      }

    }

    "throw BadRequestException when a 400 response is returned" in {

      when(requestBuilderExecute[UpstreamMessageResponse]).thenReturn(Future.failed(badRequestException))

      connector.getMessageBy(messageId, Some("en")) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }

    "throw Upstream5xxResponse when a 500 response is returned" in {

      when(requestBuilderExecute[UpstreamMessageResponse]).thenReturn(Future.failed(upstream5xxResponse))

      connector.getMessageBy(messageId, Some("en")) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }
    }
  }

  "render()" should {

    "return empty response when a 200 response is received with an empty payload" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(ReadSuccessEmptyResult)

      connector.render(message.convertedFrom(messageBodyToRender), Some("en"), hc) onComplete {
        case Success(result) => result.body mustBe ""
        case Failure(_)      =>
      }

      connector.render(message.convertedFrom(messageBodyToRender), Some("cy"), hc) onComplete {
        case Success(result) => result.body mustBe ""
        case Failure(_) =>
      }

    }

    "return a rendered message when a 200 response is received with a payload" in {
      when(requestBuilderExecute[HttpResponse]).thenReturn(PostSuccessResult)

      connector.render(message.convertedFrom(messageBodyToRender), Some("en"), hc) onComplete {
        case Success(result) => result.body must include(s"${html.body}")
        case Failure(_)      =>
      }
    }

    "return a rendered message when a 200 response is received with a payload with welsh language" in {
      when(requestBuilderExecute[HttpResponse]).thenReturn(PostSuccessResult)

      connector.render(message.convertedFrom(messageBodyToRender), Some("cy"), hc) onComplete {
        case Success(result) => result.body must include(s"${htmlCy.body}")
        case Failure(_) =>
      }
    }

    "throw BadRequestException when a 400 response is returned" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(badRequestException))

      connector.render(message.convertedFrom(messageBodyToRender), Some("en"), hc) onComplete {
        case Success(result) => fail()
        case Failure(_)      =>
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(upstream5xxResponse))

      connector.render(message.convertedFrom(messageBodyToRender), Some("en"), hc) onComplete {
        case Success(result) => fail()
        case Failure(_)      =>
      }
    }
  }

  "markAsRead()" should {

    "return a message when a 200 response is received with a payload" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(PostSuccessRendererResult)

      connector.markAsRead(messageToBeMarkedAsRead) onComplete {
        case Success(result: HttpResponse) => result.status mustBe (200)
        case Failure(_) =>
      }

    }

    "throw BadRequestException when a 400 response is returned" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(badRequestException))

      connector.markAsRead(messageToBeMarkedAsRead) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }

    "throw Upstream5xxResponse when a 500 response is returned" in {

      when(requestBuilderExecute[HttpResponse]).thenReturn(Future.failed(upstream5xxResponse))

      connector.markAsRead(messageToBeMarkedAsRead) onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }
    }
  }

  "messageCount()" should {

    "return a message count when a 200 response is received with a payload" in {
      val messageCountResponse = MessageCountResponse(MessageCount(total = 2, unread = 1))

      when(requestBuilderExecute[MessageCountResponse]).thenReturn(Future.successful(messageCountResponse))

      connector.messageCount() onComplete {
        case Success(_) => messageCountResponse
        case Failure(_) =>
      }

    }

    "throw BadRequestException when a 400 response is returned" in {

      when(requestBuilderExecute[MessageCountResponse]).thenReturn(Future.failed(badRequestException))

      connector.messageCount() onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {

      when(requestBuilderExecute[MessageCountResponse]).thenReturn(Future.failed(upstream5xxResponse))

      connector.messageCount() onComplete {
        case Success(_) => fail()
        case Failure(_) =>
      }

    }
  }
}
