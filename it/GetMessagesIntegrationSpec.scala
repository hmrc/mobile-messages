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

import com.github.tomakehurst.wiremock.client.WireMock._
import it.utils.WiremockServiceLocatorSugar
import org.joda.time.{DateTime, LocalDate}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.{FakeApplication, FakeRequest}
import play.api.{GlobalSettings, Play}
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mobilemessages.controllers.{LiveMobileMessagesController, MobileMessagesController}
import uk.gov.hmrc.play.test.UnitSpec

/**
  * Testcase to verify the capability of integration with the Messages on the Digital Platform
  *
  * 1. To get the list of messages for a given utr this service needs to call entity-resolver, parse and return its output
  *
  */
class GetMessagesIntegrationSpec extends UnitSpec with MockitoSugar with ScalaFutures with WiremockServiceLocatorSugar with BeforeAndAfter {

  "microservice get messages" should {

    "return a list of messages returned by entity-resolver" in new Setup {
      authContainsUserWith(utr)
      entityResolverGetMessagesReturns(messageHeadersBody)

      val messageHeadersResponse = messageController.getMessages(None)(request).futureValue
      jsonBodyOf(messageHeadersResponse) shouldBe Json.parse(expectedGetMessagesResponse)
    }
  }

  before {
    startMockServer()
    stubRegisterEndpoint(204)
    Play.start(app)
  }

  after {
    Play.stop()
    stopMockServer()
  }

  val additionalConfiguration: Map[String, Any] = Map(
    "appName" -> "application-name",
    "appUrl" -> "http://microservice-name.service",
    "microservice.services.entity-resolver.host" -> stubHost,
    "microservice.services.entity-resolver.port" -> stubPort,
    "microservice.services.auth.host" -> stubHost,
    "microservice.services.auth.port" -> stubPort,
    "microservice.services.message.host" -> stubHost,
    "microservice.services.message.port" -> stubPort,
    "auditing.enabled" -> "false"
  )

  object TestGlobal extends GlobalSettings
  implicit val app = FakeApplication(
    withGlobal = Some(TestGlobal),
    additionalConfiguration = additionalConfiguration
  )


  trait Setup {
    val validFromDate = new LocalDate(29348L)
    val readTime = new DateTime(82347L)
    val messageId = "messageId90342"
    val utr = SaUtr("109238")

    val messageHeadersBody =
      s"""
         |[
         |  {
         |    "id": "$messageId",
         |    "subject": "message subject",
         |    "validFrom": "${validFromDate.toString()}",
         |    "readTime": "${readTime.toString()}",
         |    "readTimeUrl": "readTimeUrl",
         |    "sentInError": false
         |  }
         |]
             """.stripMargin

    val expectedGetMessagesResponse =
      s"""
         |[
         |  {
         |    "id": "$messageId",
         |    "subject": "message subject",
         |    "validFrom": "${validFromDate.toString()}",
         |    "readTime": ${readTime.getMillis()},
         |    "readTimeUrl": "readTimeUrl",
         |    "sentInError": false
         |  }
         |]
             """.stripMargin

    val messageController: MobileMessagesController = LiveMobileMessagesController
    val request = FakeRequest("GET", "/").
      withHeaders(("Accept", "application/vnd.hmrc.1.0+json"))

    def entityResolverGetMessagesReturns(messageResponseBody: String) {
      givenThat(get(urlPathMatching(s"/messages")).
        withQueryParam("read", equalTo("Both")).
        willReturn(aResponse().
          withBody(
            messageResponseBody
          )))
    }

    def authContainsUserWith(utr: SaUtr) = {
      givenThat(get(urlMatching("/auth/authority*"))
        .willReturn(aResponse().withStatus(200).withBody(
          Json.parse(
            s"""
               | {
               |    "confidenceLevel": 500,
               |    "uri": "testUri",
               |    "accounts": {
               |        "sa": {
               |            "utr": "$utr"
               |         },
               |         "paye": {
               |            "nino": "BC233445B"
               |         }
               |     }
               | }""".
              stripMargin
          )
            .toString())))
    }
  }

}
