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


import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{Crypted, Decrypter}
import uk.gov.hmrc.mobilemessages.domain.MessageId

final case class RenderMessageRequest(url: String) {
  def toMessageIdUsing(decrypter: Decrypter): MessageId = {
    MessageId(decrypter.decrypt(Crypted.fromBase64(url)).value)
  }

  private def readTimePathIsStoredIn(url: String): Boolean = {
    url.endsWith("read-time")
  }

  def toMessageIdOrUrlUsing(decrypter: Decrypter): Either[String, MessageId] = {
    if (readTimePathIsStoredIn(url)) {
      Left(url)
    } else {
      Right(MessageId(decrypter.decrypt(Crypted.fromBase64(url)).value))
    }
  }
}

object RenderMessageRequest {
  implicit val formats = Json.format[RenderMessageRequest]
}
