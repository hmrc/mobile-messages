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

package uk.gov.hmrc.mobilemessages.acceptance

import org.apache.http.HttpStatus
import play.api.libs.json.Json.parse
import uk.gov.hmrc.mobilemessages.connector.model.{ResourceActionLocation, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted

class RenderMessageAcceptanceSpec extends AcceptanceSpec {

  "microservice render message" should {

    "return a rendered message after calling get message and sa renderer" in new Setup {
      private val messageBody = message.bodyWith(id = messageId1)

      message.getByIdReturns(messageBody)
      saMessageRenderer.successfullyRenders(messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(messageBody)
    }

    "return a rendered message after calling get message and ats renderer" in new Setup {
      private val messageBody = message.bodyWith(
        id = messageId1,
        renderUrl = ResourceActionLocation("ats-message-renderer", "/ats/render/url/path")
      )

      message.getByIdReturns(messageBody)
      atsMessageRenderer.successfullyRenders(messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe atsMessageRenderer.rendered(messageBody)
    }

    "return a rendered message after calling get message and secure message renderer" in new Setup {
      private val messageBody = message.bodyWith(
        id = messageId1,
        renderUrl = ResourceActionLocation("secure-message-renderer", "/secure/render/url/path")
      )

      message.getByIdReturns(messageBody)
      secureMessageRenderer.successfullyRenders(messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe secureMessageRenderer.rendered(messageBody)
    }

    "mark message as read if the markAsRead url is present in the message body" in new Setup {
      private val messageBody = successfulSetupFor(message.bodyToBeMarkedAsReadWith(id = messageId1))
      message.markAsReadSucceedsFor(messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(messageBody)

      eventually {
        message.assertMarkAsReadHasBeenCalledFor(messageBody)
      }
    }

    "do not mark message as read if the markAsRead url is not present in the message body" in new Setup {
      private val messageBody = successfulSetupFor(message.bodyWith(id = messageId1))

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(messageBody)

      waitUntilMarkAsReadIsCalled()
      message.assertMarkAsReadHasNeverBeenCalled()
    }

    "still return successful response even if mark as read fails" in new Setup {
      private val messageBody = successfulSetupFor(message.bodyToBeMarkedAsReadWith(id = messageId1))
      message.markAsReadFailsWith(status = 500, messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      bodyOf(readMessageResponse) shouldBe saMessageRenderer.rendered(messageBody)

      eventually {
        message.assertMarkAsReadHasBeenCalledFor(messageBody)
      }
    }

    "does not mark message as read when the rendering fails" in new Setup {
      private val messageBody = message.bodyToBeMarkedAsReadWith(id = messageId1)
      message.getByIdReturns(messageBody)
      saMessageRenderer.failsWith(status = 500, path = messageBody.renderUrl.url)
      message.markAsReadSucceedsFor(messageBody)

      // when
      val readMessageResponse = messageController.read(None)(
        mobileMessagesGetRequest.withBody(parse(s""" { "url": "${encrypted(messageBody.id, configBasedCrypto)}" } """))
      ).futureValue

      status(readMessageResponse) shouldBe HttpStatus.SC_INTERNAL_SERVER_ERROR

      waitUntilMarkAsReadIsCalled()
      message.assertMarkAsReadHasNeverBeenCalledFor(messageBody)
    }
  }

  trait Setup {
    val messageId1 = "messageId90342"

    auth.authRecordExists()

    def successfulSetupFor(messageBody: UpstreamMessageResponse): UpstreamMessageResponse = {
      message.getByIdReturns(messageBody)
      saMessageRenderer.successfullyRenders(messageBody)
      messageBody
    }

    def waitUntilMarkAsReadIsCalled(): Unit = {
      Thread.sleep(100)
    }
  }
}
