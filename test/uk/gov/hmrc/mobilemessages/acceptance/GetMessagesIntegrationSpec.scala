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

package uk.gov.hmrc.mobilemessages.acceptance

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.{DateTime, LocalDate}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.{FakeApplication, FakeRequest}
import play.api.{GlobalSettings, Play}
import uk.gov.hmrc.crypto.CryptoWithKeysFromConfig
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mobilemessages.acceptance.microservices.MessageService
import uk.gov.hmrc.mobilemessages.acceptance.utils.WiremockServiceLocatorSugar
import uk.gov.hmrc.mobilemessages.controllers.{LiveMobileMessagesController, MobileMessagesController}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.play.test.UnitSpec

class GetMessagesIntegrationSpec extends UnitSpec
  with MockitoSugar
  with ScalaFutures
  with WiremockServiceLocatorSugar
  with BeforeAndAfter {

  "microservice get messages" should {

    "return a list of message heads converted from message service response" in new Setup {
      authContainsUserWith(utr)
      message.headersListReturns(
        Seq(
          message.headerWith(id = messageId1),
          message.headerWith(id = messageId2, readTime = Some(readTime))
        )
      )

      val getMessagesResponse = messageController.getMessages(None)(mobileMessagesGetRequest).futureValue
      jsonBodyOf(getMessagesResponse) shouldBe expectedGetMessagesResponse
    }
  }

  before {
    startMockServer()
    Play.start(app)
  }

  after {
    Play.stop()
    stopMockServer()
  }

  object TestGlobal extends GlobalSettings

  implicit val app = FakeApplication(
    withGlobal = Some(TestGlobal),
    additionalConfiguration = Map(
      "appName" -> "application-name",
      "appUrl" -> "http://microservice-name.service",
      "microservice.services.entity-resolver.host" -> stubHost,
      "microservice.services.entity-resolver.port" -> stubPort,
      "microservice.services.auth.host" -> stubHost,
      "microservice.services.auth.port" -> stubPort,
      "microservice.services.message.host" -> stubHost,
      "microservice.services.message.port" -> stubPort,
      "auditing.enabled" -> "false",
      "queryParameter.encryption.key" -> "kepODU8hulPkolIryPOrTY=="
    )
  )


  trait Setup {
    val validFromDate = new LocalDate(29348L)
    val readTime = new DateTime(82347L)
    val messageId1 = "messageId90342"
    val messageId2 = "messageId932847"
    val utr = SaUtr("109238")

    val messageController: MobileMessagesController = LiveMobileMessagesController

    val message = new MessageService(authToken)

    val configBasedCryptor = CryptoWithKeysFromConfig(baseConfigKey = "queryParameter.encryption")

    val expectedGetMessagesResponse =
      Json.parse(s"""
         |[
         |  {
         |    "id": "$messageId1",
         |    "subject": "message subject",
         |    "validFrom": "${validFromDate.toString()}",
         |    "readTimeUrl": "${encrypted(messageId1, configBasedCryptor)}",
         |    "sentInError": false
         |  },
         |  {
         |    "id": "$messageId2",
         |    "subject": "message subject",
         |    "validFrom": "${validFromDate.toString()}",
         |    "readTimeUrl": "${encrypted(messageId2, configBasedCryptor)}",
         |    "readTime": ${readTime.getMillis()},
         |    "sentInError": false
         |  }
         |]
             """.stripMargin)

    def authToken = "authToken9349872"

    val mobileMessagesGetRequest = FakeRequest("GET", "/").
      withHeaders(
        ("Accept", "application/vnd.hmrc.1.0+json"),
        ("Authorization", authToken)
      )

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
