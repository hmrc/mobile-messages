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

package uk.gov.hmrc.mobilemessages.controllers.model

import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.mobilemessages.acceptance.microservices.MessageServiceMock
import uk.gov.hmrc.mobilemessages.domain.MessageHeader
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.mobilemessages.utils.UnitTestCrypto
import uk.gov.hmrc.play.test.UnitSpec

class MessageHeaderResponseBodySpec extends UnitSpec {

  val message = new MessageServiceMock("authToken")

  "get messages response" should {
    "be correctly converted from message headers" in {
      val messageHeader1 = message.headerWith(id = "id1")
      val messageHeader2 = message.headerWith(id = "id2", readTime = Some(DateTime.now(DateTimeZone.UTC)))

      MessageHeaderResponseBody.fromAll(Seq(messageHeader1, messageHeader2))(new UnitTestCrypto) shouldBe Seq(
        expectedMessageResponseItemFor(messageHeader1),
        expectedMessageResponseItemFor(messageHeader2)
      )
    }
  }

  def expectedMessageResponseItemFor(messageHeader: MessageHeader): MessageHeaderResponseBody = {
    MessageHeaderResponseBody(
      messageHeader.id.value,
      messageHeader.subject,
      messageHeader.validFrom,
      messageHeader.readTime,
      encrypted(messageHeader.id.value),
      messageHeader.sentInError
    )
  }
}
