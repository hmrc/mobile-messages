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

package uk.gov.hmrc.mobilemessages.acceptance

import play.api.libs.json.Json
import uk.gov.hmrc.mobilemessages.connector.model.{GetMessageResponseBody, ResourceActionLocation}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted

class RenderMessageAcceptanceSpec extends AcceptanceSpec {

  "microservice render message" should {

    "return a rendered message after calling get message and sa renderer" in new Setup {
      private val messageBody = message.bodyWith(id = messageId1)

      message.getByIdReturns(messageBody)
      saMessageRenderer.successfullyRenders(
        message.convertedFrom(messageBody),
        messageBody.renderUrl.url
      )

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(Json.parse(s""" { "url": "${encrypted(messageBody.id, configBasedCryptor)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(message.convertedFrom(messageBody))
    }

    "return a rendered message after calling get message and ats renderer" in new Setup {
      private val messageBody = message.bodyWith(
        id = messageId1,
        renderUrl = ResourceActionLocation("ats-message-renderer", "/ats/render/url/path")
      )

      message.getByIdReturns(messageBody)
      atsMessageRenderer.successfullyRenders(
        message.convertedFrom(messageBody),
        messageBody.renderUrl.url
      )

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(Json.parse(s""" { "url": "${encrypted(messageBody.id, configBasedCryptor)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe atsMessageRenderer.rendered(message.convertedFrom(messageBody))
    }

    "return a rendered message after calling get message and secure message renderer" in new Setup {
      private val messageBody = message.bodyWith(
        id = messageId1,
        renderUrl = ResourceActionLocation("secure-message-renderer", "/secure/render/url/path")
      )

      message.getByIdReturns(messageBody)
      secureMessageRenderer.successfullyRenders(
        message.convertedFrom(messageBody),
        messageBody.renderUrl.url
      )

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(Json.parse(s""" { "url": "${encrypted(messageBody.id, configBasedCryptor)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe secureMessageRenderer.rendered(message.convertedFrom(messageBody))
    }

    "mark message as read if the markAsRead url is present in the message body" in new Setup {
      private val messageBody = successfulSetupFor(message.bodyToBeMarkedAsReadWith(id = messageId1))
      message.markAsReadSucceedsFor(messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(Json.parse(s""" { "url": "${encrypted(messageBody.id, configBasedCryptor)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(message.convertedFrom(messageBody))

      message.assertMarkAsReadHasBeenCalledFor(messageBody)
    }

    "do not mark message as read if the markAsRead url is not present in the message body" in new Setup {
      private val messageBody = successfulSetupFor(message.bodyWith(id = messageId1))

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(Json.parse(s""" { "url": "${encrypted(messageBody.id, configBasedCryptor)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(message.convertedFrom(messageBody))

      message.assertMarkAsReadHasNeverBeenCalled()
    }

    "still return successful response even if mark as read fails" in new Setup {
      private val messageBody = successfulSetupFor(message.bodyToBeMarkedAsReadWith(id = messageId1))
      message.markAsReaFailsWith(status = 500, messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(Json.parse(s""" { "url": "${encrypted(messageBody.id, configBasedCryptor)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(message.convertedFrom(messageBody))

      message.assertMarkAsReadHasBeenCalledFor(messageBody)
    }
  }

  trait Setup {
    val messageId1 = "messageId90342"
    auth.containsUserWith(utr)
    def successfulSetupFor(messageBody: GetMessageResponseBody): GetMessageResponseBody = {
      message.getByIdReturns(messageBody)
      saMessageRenderer.successfullyRenders(
        message.convertedFrom(messageBody),
        messageBody.renderUrl.url
      )
      messageBody
    }
  }
}
