/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import org.apache.commons.codec.binary.Base64.encodeBase64String
import play.api.libs.json._
import uk.gov.hmrc.crypto.{Encrypter, PlainText}
import uk.gov.hmrc.mobilemessages.domain.MessageHeader

trait WriteDatesAsLongs {

  implicit val dateTimeWrites: Writes[LocalDateTime] = new Writes[LocalDateTime] {

    override def writes(o: LocalDateTime): JsValue =
      JsNumber(o.toInstant(ZoneOffset.UTC).toEpochMilli)
  }
}

final case class MessageHeaderResponseBody(
  id:          String,
  subject:     String,
  validFrom:   LocalDate,
  readTime:    Option[LocalDateTime],
  readTimeUrl: String,
  sentInError: Boolean)

object MessageHeaderResponseBody extends WriteDatesAsLongs {

  import play.api.libs.json._

  implicit val writes: Writes[MessageHeaderResponseBody] = Json.writes[MessageHeaderResponseBody]

  def fromAll(messageHeaders: Seq[MessageHeader])(encrypter: Encrypter): Seq[MessageHeaderResponseBody] =
    messageHeaders.map(from(_)(encrypter))

  def from(messageHeader: MessageHeader)(encrypter: Encrypter): MessageHeaderResponseBody =
    MessageHeaderResponseBody(
      messageHeader.id.value,
      messageHeader.subject,
      messageHeader.validFrom,
      messageHeader.readTime,
      encodeBase64String(encrypter.encrypt(PlainText(messageHeader.id.value)).value.getBytes),
      messageHeader.sentInError
    )
}
