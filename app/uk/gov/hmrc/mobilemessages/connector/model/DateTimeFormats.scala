/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.mobilemessages.connector.model

import play.api.libs.json.{Reads, Writes}

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object DateTimeFormats {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  implicit val offsetDateTimeReads: Reads[OffsetDateTime] =
    Reads.offsetDateTimeReads(formatter)

  implicit val offsetDateTimeWrites: Writes[OffsetDateTime] =
    Writes.of[String].contramap(formatter.format)
}
