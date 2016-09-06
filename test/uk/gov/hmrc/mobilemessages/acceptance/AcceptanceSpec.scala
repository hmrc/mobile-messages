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

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.test.{FakeApplication, FakeRequest}
import play.api.{GlobalSettings, Play}
import uk.gov.hmrc.crypto.CryptoWithKeysFromConfig
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.mobilemessages.acceptance.microservices.{AuthServiceMock, MessageRendererServiceMock, MessageServiceMock}
import uk.gov.hmrc.mobilemessages.acceptance.utils.WiremockServiceLocatorSugar
import uk.gov.hmrc.mobilemessages.controllers.{LiveMobileMessagesController, MobileMessagesController}
import uk.gov.hmrc.mobilemessages.utils.ConfigHelper.microserviceConfigPathFor
import uk.gov.hmrc.play.test.UnitSpec

trait AcceptanceSpec extends UnitSpec
  with MockitoSugar
  with ScalaFutures
  with WiremockServiceLocatorSugar
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with IntegrationPatience
  with Eventually {

  override def beforeAll() = {
    super.beforeAll()
    Play.start(app)
    startMockServer()
    saMessageRenderer.start()
    atsMessageRenderer.start()
    secureMessageRenderer.start()
  }

  override def afterAll() = {
    super.afterAll()
    Play.stop()
    stopMockServer()
    saMessageRenderer.stop()
    atsMessageRenderer.stop()
    secureMessageRenderer.stop()
  }

  override protected def afterEach() = {
    super.afterEach()
    saMessageRenderer.reset()
    atsMessageRenderer.reset()
    secureMessageRenderer.reset()
    WireMock.reset()
  }

  lazy val messageController: MobileMessagesController = LiveMobileMessagesController

  val utr = SaUtr("109238")

  val auth = new AuthServiceMock
  val message = new MessageServiceMock(auth.token)
  val saMessageRenderer = new MessageRendererServiceMock(auth.token, servicePort = 8089, "sa-message-renderer")
  val atsMessageRenderer = new MessageRendererServiceMock(auth.token, servicePort = 8093, "ats-message-renderer")
  val secureMessageRenderer = new MessageRendererServiceMock(auth.token, servicePort = 9847, "secure-message-renderer")

  lazy val configBasedCrypto = CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")

  val mobileMessagesGetRequest = FakeRequest("GET", "/").
    withHeaders(
      ("Accept", "application/vnd.hmrc.1.0+json"),
      ("Authorization", auth.token)
    )

  object TestGlobal extends GlobalSettings

  implicit val app = FakeApplication(
    withGlobal = Some(TestGlobal),
    additionalConfiguration = Map(
      "appName" -> "application-name",
      "appUrl" -> "http://microservice-name.service",
      s"${microserviceConfigPathFor("auth")}.host" -> stubHost,
      s"${microserviceConfigPathFor("auth")}.port" -> stubPort,
      s"${microserviceConfigPathFor("message")}.host" -> stubHost,
      s"${microserviceConfigPathFor("message")}.port" -> stubPort,
      "auditing.enabled" -> "false"
    )
  )
}
