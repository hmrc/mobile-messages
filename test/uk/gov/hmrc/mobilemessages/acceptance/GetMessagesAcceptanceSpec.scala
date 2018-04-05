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

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeApplication
import uk.gov.hmrc.mobilemessages.controllers.{LiveMobileMessagesController, MobileMessagesController, StubApplicationConfiguration}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.play.test.WithFakeApplication
import play.api.test.Helpers._

class GetMessagesAcceptanceSpec extends AcceptanceSpec with WithFakeApplication with StubApplicationConfiguration {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

  "microservice get messages" should {

    "return a list of message heads converted from message service response" in new Setup {
      running(fakeApplication) {
        auth.authRecordExists()

        messageMock.headersListReturns(
          Seq(
            messageMock.headerWith(id = messageId1),
            messageMock.headerWith(id = messageId2, readTime = Some(readTime))
          )
        )

        val getMessagesResponse: Result = messageController.getMessages(None)(mobileMessagesGetRequest).futureValue
        jsonBodyOf(getMessagesResponse) shouldBe expectedGetMessagesResponse
      }
    }
  }

  trait Setup {
    val validFromDate = new LocalDate(29348L)
    val readTime = new DateTime(82347L)
    val messageId1 = "messageId90342"
    val messageId2 = "messageId932847"

    val expectedGetMessagesResponse: JsValue =
      Json.parse(
        s"""
           |[
           |  {
           |    "id": "$messageId1",
           |    "subject": "message subject",
           |    "validFrom": "${validFromDate.toString()}",
           |    "readTimeUrl": "${encrypted(messageId1, configBasedCrypto)}",
           |    "sentInError": false
           |  },
           |  {
           |    "id": "$messageId2",
           |    "subject": "message subject",
           |    "validFrom": "${validFromDate.toString()}",
           |    "readTimeUrl": "${encrypted(messageId2, configBasedCrypto)}",
           |    "readTime": ${readTime.getMillis()},
           |    "sentInError": false
           |  }
           |]
             """.stripMargin)

    val messageController: MobileMessagesController = new LiveMobileMessagesController(testMobileMessagesService,
      testAccountAccessControlWithHeaderCheck)
  }
}
