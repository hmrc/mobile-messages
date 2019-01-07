/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.libs.json.OFormat

import scala.language.implicitConversions

final case class RenderMessageLocation(service: String, url: String)

class RenderMessageLocationImpl @Inject()(@Named("baseUrl") _baseUrl: String => String) {

  import play.api.libs.json.Json

  implicit def toUrl(renderMessageLocation: RenderMessageLocation): String = {
    val url = s"${_baseUrl(renderMessageLocation.service)}${renderMessageLocation.url}"
    Logger.info(s"Sending request to $url")
    url
  }

  implicit val formats: OFormat[RenderMessageLocation] = Json.format[RenderMessageLocation]
}