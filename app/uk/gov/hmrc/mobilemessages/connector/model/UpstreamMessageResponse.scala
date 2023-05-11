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

package uk.gov.hmrc.mobilemessages.connector.model

import play.api.libs.json.{Format, Json, Reads}
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageId, ReadMessage, UnreadMessage}

final case class ResourceActionLocation(
  service: String,
  url:     String) {
  def toUrlUsing(baseUrl: String) = s"${baseUrl.stripSuffix("/").trim}/${url.stripPrefix("/").trim}"
}

final case class UpstreamMessageResponse(
  id:            String,
  renderUrl:     ResourceActionLocation,
  markAsReadUrl: Option[ResourceActionLocation],
  body:          Option[Details]) {

  def toMessageUsing(servicesToUrl: Map[String, String]): Message = {
    val `type`:   Option[String] = body.flatMap(details => details.`type`)
    val threadId: Option[String] = body.flatMap(details => details.threadId)

    markAsReadUrl.fold[Message](
      ReadMessage(
        MessageId(id),
        renderUrl.toUrlUsing(servicesToUrl(renderUrl.service)),
        `type`,
        threadId
      )
    )(res =>
      UnreadMessage(
        MessageId(id),
        renderUrl.toUrlUsing(servicesToUrl(renderUrl.service)),
        res.toUrlUsing(servicesToUrl(res.service)),
        `type`,
        threadId
      )
    )
  }
}

case class Details(
  `type`:   Option[String],
  threadId: Option[String])

object Details {
  implicit val format: Format[Details] = Json.format[Details]
}

object ResourceActionLocation {

  import play.api.libs.json.Json

  implicit val reads: Reads[ResourceActionLocation] = Json.reads[ResourceActionLocation]
}

object UpstreamMessageResponse {

  import play.api.libs.json.Json

  implicit val reads: Reads[UpstreamMessageResponse] = Json.reads[UpstreamMessageResponse]
}
