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

package uk.gov.hmrc.mobilemessages.controllers.model

import com.ning.http.util.Base64
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.crypto.{Encrypter, PlainText}
import uk.gov.hmrc.mobilemessages.domain.MessageHeader

final case class MessageHeaderResponseBody(id: String,
                                           subject: String,
                                           validFrom: LocalDate,
                                           readTime: Option[DateTime],
                                           readTimeUrl: String,
                                           sentInError: Boolean)

object MessageHeaderResponseBody {

  import play.api.libs.json.{Json, _}

  implicit val writes: Writes[MessageHeaderResponseBody] = Json.writes[MessageHeaderResponseBody]

  def fromAll(messageHeaders: Seq[MessageHeader])(encrypter: Encrypter): Seq[MessageHeaderResponseBody] = {
    messageHeaders.map(from(_)(encrypter))
  }

  def from(messageHeader: MessageHeader)(encrypter: Encrypter): MessageHeaderResponseBody = {
    MessageHeaderResponseBody(
      messageHeader.id.value,
      messageHeader.subject,
      messageHeader.validFrom,
      messageHeader.readTime,
      Base64.encode(encrypter.encrypt(PlainText(messageHeader.id.value)).value.getBytes),
      messageHeader.sentInError
    )
  }
}