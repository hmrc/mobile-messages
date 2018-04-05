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

package uk.gov.hmrc.mobilemessages.domain

import javax.inject.Inject

import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.Mode.Mode

final case class RenderMessageLocation(service : String, url : String)

class RenderMessageLocationImpl @Inject()(val mode: Mode, val runModeConfiguration: Configuration) extends ServicesConfig {
  import play.api.libs.json.Json

  implicit def toUrl(renderMessageLocation: RenderMessageLocation) : String = {
    val url = s"${baseUrl(renderMessageLocation.service)}${renderMessageLocation.url}"
    Logger.info(s"Sending request to $url")
    url
  }

  implicit val formats = Json.format[RenderMessageLocation]
}