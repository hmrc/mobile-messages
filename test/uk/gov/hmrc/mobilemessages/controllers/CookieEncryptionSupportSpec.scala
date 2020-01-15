/*
 * Copyright 2020 HM Revenue & Customs
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

import com.typesafe.config.Config
import org.scalatest.{Matchers, WordSpecLike}
import play.api.Configuration
import play.api.http.SecretConfiguration
import play.api.libs.crypto.{CookieSigner, DefaultCookieSigner}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.mobilemessages.connector.SessionCookieEncryptionSupport
import uk.gov.hmrc.mobilemessages.mocks.StubApplicationConfiguration

class CookieEncryptionSupportSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with StubApplicationConfiguration {

  "encrypt/decrypt cookie" should {

    "successfully encrypt and decrypt" in {
      val crypto: SessionCookieEncryptionSupport = new SessionCookieEncryptionSupport {
        override def config: Config =
          Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")).underlying
        override def cookieSigner: CookieSigner =
          new DefaultCookieSigner(SecretConfiguration("hwdODU8hulPkolIryPRkVW=="))
      }

      val data = Map(("some key1", "some value1"), ("some key2", "some value2"))
      val result:          (String, String) = crypto.withSession(data.toList: _*)
      val len:             Int              = s"${crypto.mtdpSessionCookie}=".length
      val encryptedResult: String           = result._2.substring(len + 1, result._2.length - 1)

      val decryptedMap: Map[String, String] = crypto.sessionOf(encryptedResult)
      decryptedMap shouldBe data
    }

    "fail to decrypt when an invalid payload is supplied for decryption" in {

      val crypto: SessionCookieEncryptionSupport = new SessionCookieEncryptionSupport {
        override def config: Config =
          Configuration.from(Map("cookie.encryption.key" -> "hwdODU8hulPkolIryPRkVW==")).underlying
        override def cookieSigner: CookieSigner =
          new DefaultCookieSigner(SecretConfiguration("hwdODU8hulPkolIryPRkVW=="))
      }

      val data = Map(("some key1", "some value1"), ("some key2", "some value2"))
      val result: (String, String) = crypto.withSession(data.toList: _*)

      intercept[SecurityException] {
        crypto.sessionOf(result._2)
      }
    }

  }
}
