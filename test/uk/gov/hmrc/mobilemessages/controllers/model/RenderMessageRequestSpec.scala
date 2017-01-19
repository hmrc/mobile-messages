/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.mobilemessages.acceptance.microservices.MessageServiceMock
import uk.gov.hmrc.mobilemessages.domain.MessageId
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.mobilemessages.utils.UnitTestCrypto
import uk.gov.hmrc.play.test.UnitSpec

class RenderMessageRequestSpec extends UnitSpec {

  val message = new MessageServiceMock("authToken")

  val crypto = new UnitTestCrypto

  "messageId should be" should {
    "be correctly decrypted from encrypted version" in {

      val messageId = "messageId43573947"

      RenderMessageRequest(encrypted(messageId, crypto)).toMessageIdUsing(crypto) shouldBe MessageId(messageId)
    }
  }

  "invalid data" should {
    "cause security exception" in {
      intercept[SecurityException] {
        RenderMessageRequest("invalidDataThatIsNeitherMessageIdNorEncrypted").toMessageIdUsing(crypto)
      }
    }
  }
}
