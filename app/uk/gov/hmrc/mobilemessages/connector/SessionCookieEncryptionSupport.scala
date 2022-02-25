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

import java.net.URLDecoder

import com.typesafe.config.Config
import org.apache.commons.codec.CharEncoding
import org.apache.commons.lang3.StringUtils
import play.api.http.HeaderNames
import play.api.libs.crypto.CookieSigner
import uk.gov.hmrc.crypto.{Crypted, CryptoGCMWithKeysFromConfig, PlainText}

trait SessionCookieEncryptionSupport {
  val SignSeparator     = "-"
  val mtdpSessionCookie = "mdtp"

  def config:       Config
  def cookieSigner: CookieSigner

  lazy val cipher = new CryptoGCMWithKeysFromConfig("cookie.encryption", config)

  private def createPopulatedSessionCookie(payload: String): String = {
    val signedPayload = cookieSigner.sign(payload) + SignSeparator + payload
    val encryptedSignedPayload: String = cipher.encrypt(PlainText(signedPayload)).value

    s"""$mtdpSessionCookie="$encryptedSignedPayload""""
  }

  def sessionOf(session: String): Map[String, String] = {
    val decrypted:    PlainText = cipher.decrypt(Crypted(session))
    val hashAndValue: String    = URLDecoder.decode(decrypted.value, CharEncoding.UTF_8)
    val justTheValue = StringUtils.substringAfter(hashAndValue, "-")
    val pairs = justTheValue.split("&") map { keyValuePair =>
        val pair = keyValuePair.split("=")
        (pair(0), pair(1))
      }
    pairs.toMap
  }

  def withSession(pair: (String, String)*): (String, String) = {
    val payload = pair.toSeq
      .map {
        case (k, v) => s"$k=$v"
      }
      .mkString("&")
    (HeaderNames.COOKIE, createPopulatedSessionCookie(payload))
  }
}
