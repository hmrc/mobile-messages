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

package uk.gov.hmrc.mobilemessages.acceptance.microservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json.obj
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.domain.Nino

class AuthServiceMock {
  def token = "authToken9349872"

  def authRecordExists(nino: Nino = Nino("BC233445B"), confidenceLevel: ConfidenceLevel = L200, uri: String = "uri"): Unit = {
    stubFor(post(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(
      """{ "authorise": [], "retrieve": ["nino","confidenceLevel","userDetailsUri"] }""".stripMargin, true, false)).willReturn(
        aResponse().withStatus(200).withBody(obj(
          "confidenceLevel" -> confidenceLevel.level, "nino" -> nino.nino, "userDetailsUri" -> uri).toString)))
  }
}
