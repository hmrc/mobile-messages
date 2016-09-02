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

package uk.gov.hmrc.mobilemessages.connector.model


import play.api.libs.json.Reads
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageId}
import uk.gov.hmrc.play.config.ServicesConfig

case class ResourceActionLocation(service: String, url: String) {
  def toUrlUsing(servicesConfig: ServicesConfig) = {
    val baseUrl: String = servicesConfig.baseUrl(service)

    s"${baseUrl.stripSuffix("/").trim}/${url.stripPrefix("/").trim}"
  }
}

case class GetMessageResponseBody(id: String,
                                  renderUrl: ResourceActionLocation,
                                  markAsReadUrl: Option[ResourceActionLocation]) {
  def toMessageUsing(servicesConfig: ServicesConfig): Message = {
    Message(MessageId(id), renderUrl.toUrlUsing(servicesConfig), None)
  }
}

object ResourceActionLocation {
  import play.api.libs.json.Json

  implicit val reads: Reads[ResourceActionLocation] = Json.reads[ResourceActionLocation]
}

object GetMessageResponseBody {
  import play.api.libs.json.Json

  implicit val reads: Reads[GetMessageResponseBody] = Json.reads[GetMessageResponseBody]
}
