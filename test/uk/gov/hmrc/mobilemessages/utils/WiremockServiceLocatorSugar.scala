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

package uk.gov.hmrc.mobilemessages.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.apache.http.HttpHeaders
import play.api.libs.json.Json
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.mobilemessages.connector.model.UpstreamMessageResponse
import uk.gov.hmrc.play.it.Port.randomAvailable

trait WiremockServiceLocatorSugar {
  val stubPort: Int = randomAvailable
  val stubHost: String = "localhost"
  lazy val wireMockUrl = s"http://$stubHost:$stubPort"
  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))
  private val service = new WireMock(stubPort)

  def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    Json.toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  def startMockServer(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
  }

  def stopMockServer(): Unit = {
    wireMockServer.stop()
    // A cleaner solution to reset the mappings, but only works with wiremock "1.57" (at the moment version 1.48 is pulled)
    //wireMockServer.resetMappings()
  }

  def stubRegisterEndpoint(status: Int): StubMapping = stubFor(post(urlMatching("/registration")).willReturn(aResponse().withStatus(status)))

  def successfullyRenders(messageBody: UpstreamMessageResponse, overrideBody: Option[String] = None, serviceName: String, authToken: String): Unit = {
    service.register(get(urlEqualTo(messageBody.renderUrl.url)).
      withHeader(HttpHeaders.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().
        withBody(if (overrideBody.isDefined) overrideBody.get else rendered(messageBody, serviceName))))
  }

  def failsWith(status: Int, body: String = "", path: String, authToken: String): Unit = {
    service.register(get(urlEqualTo(path)).
      withHeader(HttpHeaders.AUTHORIZATION, equalTo(authToken)).
      willReturn(aResponse().
        withStatus(status).
        withBody(body)))
  }

  def rendered(messageBody: UpstreamMessageResponse, serviceName: String): String = {
    s"""
       |<div>This is a message with id: ${messageBody.id} rendered by $serviceName</div>
     """.stripMargin
  }
}
