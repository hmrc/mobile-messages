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

package uk.gov.hmrc.mobilemessages.domain

import org.joda.time.{DateTime, LocalDate}

case class MessageHeader(id: String,
                         subject: String,
                         validFrom: LocalDate,
                         readTime: Option[DateTime],
                         readTimeUrl: String,
                         sentInError: Boolean)

object MessageHeader {

  import play.api.libs.functional.syntax._
  import play.api.libs.json.{Json, Reads, _}
  import uk.gov.hmrc.play.controllers.RestFormats

  private val reads = {
    implicit val dateTimeForm = RestFormats.dateTimeFormats
    val read = Json.reads[MessageHeader]
    val legacyRead: Reads[MessageHeader] = (
      (__ \ "id").read[String] and
        (__ \ "subject").read[String] and
        (__ \ "validFrom").read[LocalDate] and
        (__ \ "readTime").readNullable[DateTime] and
        (__ \ "readTimeUrl").read[String] and
        (__ \ "detail" \ "linkType").read[String] and
        (__ \ "sentInError").read[Boolean]
      ) (convertFromLegacy _)
    read orElse legacyRead
  }

  private val writes = Json.writes[MessageHeader]

  implicit val formats = Format(reads, writes)

  private def convertFromLegacy(id: String,
                                subject: String,
                                validFrom: LocalDate,
                                readTime: Option[DateTime],
                                readTimeUrl: String,
                                linkType: String,
                                sentInError: Boolean) = new MessageHeader(id, subject, validFrom, readTime, readTimeUrl, sentInError)
}
