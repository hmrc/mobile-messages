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

package uk.gov.hmrc.mobilemessages.connector

import play.api.http.HeaderNames
import play.api.libs.Crypto
import uk.gov.hmrc.crypto.{CryptoGCMWithKeysFromConfig, PlainText}

trait SessionCookieEncryptionSupport {
  val SignSeparator = "-"
  val mtdpSessionCookie = "mdtp"

  lazy val cipher = CryptoGCMWithKeysFromConfig("cookie.encryption")

  private def createPopulatedSessionCookie(payload: String): String = {
    val signedPayload = Crypto.sign(payload) + SignSeparator + payload
    val encryptedSignedPayload: String = cipher.encrypt(PlainText(signedPayload)).value

    s"""$mtdpSessionCookie="$encryptedSignedPayload""""
  }

  def withSession(pair: (String, String)*): (String, String) = {
    val payload = pair.toSeq.map {
      case (k, v) => s"$k=$v"
    }.mkString("&")
    (HeaderNames.COOKIE, createPopulatedSessionCookie(payload))
  }
}