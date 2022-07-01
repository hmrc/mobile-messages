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

package uk.gov.hmrc.mobilemessages.connector

import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.http.SecretConfiguration
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers.SERVICE_UNAVAILABLE
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilemessages.connector.model.{ResourceActionLocation, UpstreamMessageHeadersResponse, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.domain._
import uk.gov.hmrc.mobilemessages.utils.Setup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessagesConnectorSpec extends WordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with Setup {

  def testBaseUrl(serviceName: String): String = "http://localhost:8089"

  def mockBaseUrl: String => String = testBaseUrl

  implicit val formats: OFormat[RenderMessageLocation] = Json.format[RenderMessageLocation]

  val responseRenderer = RenderMessageLocation("sa-message-renderer", "http://somelocation")
  val renderPath       = "/some/render/path"

  val messageBodyToRender: UpstreamMessageResponse =
    message.bodyWith(id = "id1", renderUrl = ResourceActionLocation("test-renderer-service", renderPath))
  val messageToBeMarkedAsReadBody: UpstreamMessageResponse = message.bodyToBeMarkedAsReadWith(id = "id48")

  val messageToBeMarkedAsRead: UnreadMessage =
    UnreadMessage(
      MessageId(messageToBeMarkedAsReadBody.id),
      messageToBeMarkedAsReadBody.renderUrl.url,
      "markAsReadUrl",
      Some("2wsm-advisor"),
      Some("9794f96d-f595-4b03-84dc-1861408918fb")
    )

  lazy val PostSuccessResult: Future[AnyRef with HttpResponse] =
    Future.successful(HttpResponse(200, toJson(html.body), headers))

  lazy val PostSuccessRendererResult: Future[AnyRef with HttpResponse] =
    Future.successful(HttpResponse(200, toJson(responseRenderer), headers))

  lazy val connector: MessageConnector =
    new MessageConnector(
      message.fullUrlFor("message", ""),
      message.fullUrlFor("sa-message-renderer", ""),
      message.fullUrlFor("ats-message-renderer", ""),
      message.fullUrlFor("secure-message-renderer", ""),
      message.fullUrlFor("two-way-message", ""),
      Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")),
      new DefaultCookieSigner(SecretConfiguration("hwdODU8hulPkolIryPRkVW==")),
      mockHttp
    )

  private val upstream5xxResponse = UpstreamErrorResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)
  private val badRequestException = new BadRequestException("")

  "messages()" should {

    "return a list of items when a 200 response is received with a payload" in {
      val messagesHeaders =
        Seq(message.headerWith(id = "someId1"), message.headerWith(id = "someId2"), message.headerWith(id = "someId3"))

      messagesGetSuccess(UpstreamMessageHeadersResponse(messagesHeaders))

      await(connector.messages()) shouldBe messagesHeaders
    }

    "throw BadRequestException when a 400 response is returned" in {
      messagesGetFailure(badRequestException)

      intercept[BadRequestException] {
        await(connector.messages())
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      messagesGetFailure(upstream5xxResponse)

      intercept[UpstreamErrorResponse] {
        await(connector.messages())
      }
    }

    "return empty response when a 200 response is received with an empty payload" in {
      messagesGetSuccess(UpstreamMessageHeadersResponse(Seq.empty))

      await(connector.messages()) shouldBe Seq.empty
    }
  }

  "getMessageBy(messageId)" should {

    "return a message when a 200 response is received with a payload" in {
      messageByGetSuccess(message.bodyWith(id = messageId.value))

      await(connector.getMessageBy(messageId)) shouldBe message.convertedFrom(
        message.bodyWith(id = messageId.value)
      )
    }

    "throw BadRequestException when a 400 response is returned" in {
      messageByGetFailure(badRequestException)

      intercept[BadRequestException] {
        await(connector.getMessageBy(messageId))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      messageByGetFailure(upstream5xxResponse)

      intercept[UpstreamErrorResponse] {
        await(connector.getMessageBy(messageId))
      }
    }
  }

  "render()" should {

    "return empty response when a 200 response is received with an empty payload" in {
      renderGetSuccess(ReadSuccessEmptyResult)

      await(connector.render(message.convertedFrom(messageBodyToRender), hc)).body shouldBe ""
    }

    "return a rendered message when a 200 response is received with a payload" in {
      renderGetSuccess(PostSuccessResult)

      await(connector.render(message.convertedFrom(messageBodyToRender), hc)).body should include(s"${html.body}")
    }

    "throw BadRequestException when a 400 response is returned" in {
      renderGetFailure(badRequestException)

      intercept[BadRequestException] {
        await(connector.render(message.convertedFrom(messageBodyToRender), hc))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      renderGetFailure(upstream5xxResponse)

      intercept[UpstreamErrorResponse] {
        await(connector.render(message.convertedFrom(messageBodyToRender), hc))
      }
    }
  }

  "markAsRead()" should {

    "return a message when a 200 response is received with a payload" in {
      markAsReadPostSuccess(PostSuccessRendererResult)

      await(connector.markAsRead(messageToBeMarkedAsRead)).status shouldBe 200
    }

    "throw BadRequestException when a 400 response is returned" in {
      markAsReadPostFailure(badRequestException)

      intercept[BadRequestException] {
        await(connector.markAsRead(messageToBeMarkedAsRead))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      markAsReadPostFailure(upstream5xxResponse)

      intercept[UpstreamErrorResponse] {
        await(connector.markAsRead(messageToBeMarkedAsRead))
      }
    }
  }

  "messageCount()" should {

    "return a message count when a 200 response is received with a payload" in {
      val messageCountResponse = MessageCountResponse(MessageCount(total = 2, unread = 1))

      messageCountGetSuccess(messageCountResponse)

      await(connector.messageCount()) shouldBe messageCountResponse
    }

    "throw BadRequestException when a 400 response is returned" in {
      messagesGetFailure(badRequestException)

      intercept[BadRequestException] {
        await(connector.messageCount())
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in {
      messagesGetFailure(upstream5xxResponse)

      intercept[UpstreamErrorResponse] {
        await(connector.messageCount())
      }
    }
  }
}
