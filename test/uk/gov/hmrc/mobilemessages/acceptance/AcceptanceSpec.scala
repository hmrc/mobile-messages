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

///*
// * Copyright 2018 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.mobilemessages.acceptance
//
//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
//import com.github.tomakehurst.wiremock.client.WireMock
//import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
//import org.scalatest.mockito.MockitoSugar
//import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
//import play.api.test.{FakeApplication, FakeRequest}
//import play.api.{Configuration, GlobalSettings, Play}
//import play.twirl.api.Html
//import uk.gov.hmrc.crypto.CryptoWithKeysFromConfig
//import uk.gov.hmrc.domain.{Nino, SaUtr}
//import uk.gov.hmrc.mobilemessages.acceptance.microservices.{AuthServiceMock, MessageRendererServiceMock, MessageServiceMock}
//import uk.gov.hmrc.mobilemessages.acceptance.utils.WiremockServiceLocatorSugar
//import uk.gov.hmrc.mobilemessages.config.WSHttp
//import uk.gov.hmrc.mobilemessages.controllers._
//import uk.gov.hmrc.mobilemessages.controllers.action.AccountAccessControlWithHeaderCheck
//import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId, ReadMessage}
//import uk.gov.hmrc.mobilemessages.services.LiveMobileMessagesService
//import uk.gov.hmrc.mobilemessages.utils.ConfigHelper.microserviceConfigPathFor
//import uk.gov.hmrc.play.audit.http.connector.AuditConnector
//import uk.gov.hmrc.play.test.UnitSpec
//
//trait AcceptanceSpec extends UnitSpec
//  with MockitoSugar
//  with ScalaFutures
//  with WiremockServiceLocatorSugar
//  with BeforeAndAfterAll
//  with BeforeAndAfterEach
//  with IntegrationPatience
//  with Eventually {
//
//
//  implicit val system = ActorSystem()
//  implicit val materializer = ActorMaterializer()
//
//  override def beforeAll() = {
//    super.beforeAll()
//    Play.start(app)
//    startMockServer()
//    saMessageRenderer.start()
//    atsMessageRenderer.start()
//    secureMessageRenderer.start()
//  }
//
//  override def afterAll() = {
//    super.afterAll()
//    Play.stop(app)
//    stopMockServer()
//    saMessageRenderer.stop()
//    atsMessageRenderer.stop()
//    secureMessageRenderer.stop()
//  }
//
//  override protected def afterEach() = {
//    super.afterEach()
//    saMessageRenderer.reset()
//    atsMessageRenderer.reset()
//    secureMessageRenderer.reset()
//    WireMock.reset()
//  }
//
//  val auth = new AuthServiceMock
//
//  val nino = Nino("CS700100A")
//  val saUtrVal = SaUtr("1234567890")
//  val testAccess = new TestAccessControl(Some(nino), Some(saUtrVal))
//  lazy val html = Html.apply("<div>some snippet</div>")
//  val message = new MessageServiceMock(auth.token)
//  val mockAuditConnector: AuditConnector = mock[AuditConnector]
//  //val sampleMessage: Message = message.convertedFrom(message.bodyWith(id = "id1"))
//  val messageConnector = new TestMessageConnector(Seq.empty[MessageHeader], html, ReadMessage(id = MessageId("id1"), "someUrl"), mock[WSHttp], "someUrl")
//  val testMobileMessagesService: LiveMobileMessagesService = new TestMobileMessagesService(mock[Configuration], testAccess, messageConnector, mockAuditConnector)
//  val testAccountAccessControlWithHeaderCheck: AccountAccessControlWithHeaderCheck = mock[AccountAccessControlWithHeaderCheck]
//
//  val utr = SaUtr("109238")
//
//  val messageMock = new MessageServiceMock(auth.token)
//  val saMessageRenderer = new MessageRendererServiceMock(auth.token, servicePort = 8089, "sa-message-renderer")
//  val atsMessageRenderer = new MessageRendererServiceMock(auth.token, servicePort = 8093, "ats-message-renderer")
//  val secureMessageRenderer = new MessageRendererServiceMock(auth.token, servicePort = 9847, "secure-message-renderer")
//
//  lazy val configBasedCrypto = CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")
//
//  val mobileMessagesGetRequest = FakeRequest("GET", "/").
//    withHeaders(
//      ("Accept", "application/vnd.hmrc.1.0+json"),
//      ("Authorization", auth.token)
//    )
//
//  object TestGlobal extends GlobalSettings
//
//  implicit val app = FakeApplication(
//    withGlobal = Some(TestGlobal),
//    additionalConfiguration = Map(
//      "appName" -> "application-name",
//      "appUrl" -> "http://microservice-name.service",
//      s"${microserviceConfigPathFor("auth")}.host" -> stubHost,
//      s"${microserviceConfigPathFor("auth")}.port" -> stubPort,
//      s"${microserviceConfigPathFor("message")}.host" -> stubHost,
//      s"${microserviceConfigPathFor("message")}.port" -> stubPort,
//      "auditing.enabled" -> "false"
//    )
//  )
//}
