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

package uk.gov.hmrc.mobilemessages.controllers

import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.mobilemessages.connector.SessionCookieEncryptionSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class CookieEncryptionSupportSpec extends UnitSpec with ScalaFutures with StubApplicationConfiguration with WithFakeApplication {

  "encrypt/decrypt cookie" should {

    "successfully encrypt and decrypt" in new Success {

      val crypto = new SessionCookieEncryptionSupport {}

      val data = Map(("some key1", "some value1"), ("some key2", "some value2"))
      val result: (String, String) = crypto.withSession(data.toList: _ *)
      val len = s"${crypto.mtdpSessionCookie}=".length
      val encryptedResult = result._2.substring(len + 1, result._2.length - 1)

      val decryptedMap: Map[String, String] = crypto.sessionOf(encryptedResult)
      decryptedMap shouldBe data
    }

    "fail to decrypt when an invalid payload is supplied for decryption" in new Success {

      val crypto = new SessionCookieEncryptionSupport {}

      val data = Map(("some key1", "some value1"), ("some key2", "some value2"))
      val result: (String, String) = crypto.withSession(data.toList: _ *)

      intercept[SecurityException] {
        crypto.sessionOf(result._2)
      }
    }

  }
}