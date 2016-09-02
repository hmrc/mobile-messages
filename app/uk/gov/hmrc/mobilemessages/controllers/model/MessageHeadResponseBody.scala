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

import com.ning.http.util.Base64
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.crypto.{Encrypter, PlainText}
import uk.gov.hmrc.mobilemessages.domain.MessageHeader

case class MessageHeadResponseBody(id: String,
                                   subject: String,
                                   validFrom: LocalDate,
                                   readTime: Option[DateTime],
                                   readTimeUrl: String,
                                   sentInError: Boolean)

object MessageHeadResponseBody {

  import play.api.libs.json.{Json, _}

  implicit val writes: Writes[MessageHeadResponseBody] = Json.writes[MessageHeadResponseBody]

  def fromAll(messageHeaders: Seq[MessageHeader])(encrypter: Encrypter): Seq[MessageHeadResponseBody] = {
    messageHeaders.map(from(_)(encrypter))
  }

  def from(message: MessageHeader)(encrypter: Encrypter) = {
    MessageHeadResponseBody(
      message.id,
      message.subject,
      message.validFrom,
      message.readTime,
      Base64.encode(encrypter.encrypt(PlainText(message.id)).value.getBytes),
      message.sentInError
    )
  }
}
