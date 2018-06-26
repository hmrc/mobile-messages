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

package uk.gov.hmrc.mobilemessages.controllers

import java.util.UUID.randomUUID

import play.api.Configuration
import play.api.libs.json.{JsValue, Json, Reads}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.POST
import play.api.test.{FakeApplication, FakeRequest}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.crypto.CryptoWithKeysFromConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.mobilemessages.config.WSHttpImpl
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
import uk.gov.hmrc.mobilemessages.domain.{MessageHeader, MessageId}
import uk.gov.hmrc.mobilemessages.services.LiveMobileMessagesService
import uk.gov.hmrc.mobilemessages.stubs.{AuthorisationStub, StubApplicationConfiguration}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.mobilemessages.utils.MessageServiceMock
import uk.gov.hmrc.play.test.WithFakeApplication

trait Setup extends AuthorisationStub with StubApplicationConfiguration with WithFakeApplication {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)
  lazy val html = Html.apply("<div>some snippet</div>")
  lazy val emptyRequestWithAcceptHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)
  lazy val readTimeRequest: FakeRequest[JsValue] = fakeRequest(Json.toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))
    .withHeaders(acceptHeader)
  lazy val readTimeRequestNoAcceptHeader: FakeRequest[JsValue] = fakeRequest(Json.toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))

  implicit val reads: Reads[MessageHeaderResponseBody] = Json.reads[MessageHeaderResponseBody]
  implicit val hc: HeaderCarrier = HeaderCarrier(Some(Authorization("authToken")))
  implicit val http: WSHttpImpl = mock[WSHttpImpl]
  implicit val authConnector: AuthConnector = mock[AuthConnector]

  val nino = Nino("CS700100A")
  val journeyId = Option(randomUUID().toString)
  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  val encrypter: CryptoWithKeysFromConfig = {
    val configuration = fakeApplication.injector.instanceOf[Configuration]
    CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption", configuration)
  }

  val message = new MessageServiceMock("authToken")

  val messageId = MessageId("id123")

  val messageServiceHeadersResponse: Seq[MessageHeader] = Seq(
    message.headerWith(id = "id1"),
    message.headerWith(id = "id2"),
    message.headerWith(id = "id3")
  )

  val getMessageResponseItemList: Seq[MessageHeaderResponseBody] =
    MessageHeaderResponseBody.fromAll(messageHeaders = messageServiceHeadersResponse)(encrypter)

  val service: LiveMobileMessagesService = mock[LiveMobileMessagesService]

  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(POST, "url").
    withBody(body).
    withHeaders("Content-Type" -> "application/json")
}

//
//package uk.gov.hmrc.mobilemessages.controllers
//
//import java.util.UUID.randomUUID
//
//import org.joda.time.DateTime
//import org.scalamock.scalatest.MockFactory
//import org.scalatest.mockito.MockitoSugar
//import play.api.Configuration
//import play.api.libs.json.JsValue
//import play.api.libs.json.Json.toJson
//import play.api.test.FakeRequest
//import play.api.test.Helpers._
//import play.twirl.api.Html
//import uk.gov.hmrc.auth.core.AuthConnector
//import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
//import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
//import uk.gov.hmrc.domain.{Nino, SaUtr}
//import uk.gov.hmrc.http._
//import uk.gov.hmrc.mobilemessages.config.WSHttp
//import uk.gov.hmrc.mobilemessages.connector.MessageConnector
//import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControl, AccountAccessControlCheckAccessOff, AccountAccessControlWithHeaderCheck, Authority}
//import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
//import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId, ReadMessage}
//import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
//import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
//import uk.gov.hmrc.mobilemessages.utils.{MessageServiceMock, UnitTestCrypto}
//import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
//import uk.gov.hmrc.time.DateTimeUtils
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class TestAccessControl(nino: Option[Nino], saUtr: Option[SaUtr],
//                        override val authConnector: AuthConnector,
//                        override val http: WSHttp,
//                        override val authUrl: String,
//                        override val serviceConfidenceLevel: Int)
//  extends AccountAccessControl(authConnector, http, authUrl, serviceConfidenceLevel) with MockitoSugar {
//  override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] =
//    Future(Authority(nino.getOrElse(throw new Exception("Invalid nino")), L200, "some-auth-id"))
//}
//
//class TestMessageConnector(result: Seq[MessageHeader], html: Html, message: Message, http: WSHttp, messageBaseUrl: String, baseUrl: String => String)
//  extends MessageConnector(messageBaseUrl, http, baseUrl) with MockitoSugar with TimeSetup {
//
//  override val now: DateTime = timeNow
//
//  override def messages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = Future.successful(result)
//
//  override def getMessageBy(id: MessageId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Message] = Future.successful(message)
//
//  override def render(message: Message, hc: HeaderCarrier)(implicit ec: ExecutionContext, auth: Option[Authority]): Future[Html] = Future.successful(html)
//}
//
//class TestMobileMessagesService(override val appNameConfiguration: Configuration, testAccessControl: TestAccessControl,
//                                mobileMessageConnector: MessageConnector, testAuditConnector: AuditConnector)
//  extends LiveMobileMessagesService(mobileMessageConnector, testAuditConnector, testAccessControl, appNameConfiguration) {
//  var saveDetails: Map[String, String] = Map.empty
//
//  override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
//    saveDetails = details
//    Future.successful(AuditResult.Success)
//  }
//
//  override val accountAccessControl: TestAccessControl = testAccessControl
//  override val messageConnector: MessageConnector = mobileMessageConnector
//  override val auditConnector: AuditConnector = testAuditConnector
//}
//
//class TestSandboxMobileMessagesService(val appNameConfiguration: Configuration, testAccessControl: TestAccessControl,
//                                       mobileMessageConnector: MessageConnector, testAuditConnector: AuditConnector) extends SandboxMobileMessagesService {
//  override val saUtr = SaUtr("1234567890")
//}
//
//class TestAccountAccessControlWithAccept(testAccessCheck: AccountAccessControl) extends AccountAccessControlWithHeaderCheck(testAccessCheck) {
//  override val accessControl: AccountAccessControl = testAccessCheck
//}
//
//trait TimeSetup {
//  val timeNow: DateTime = DateTimeUtils.now
//}
//
//trait Setup extends MockFactory with TimeSetup {
//  implicit val hc: HeaderCarrier = HeaderCarrier()
//
//  val journeyId = Option(randomUUID().toString)
//
//  val nino = Nino("CS700100A")
//  val saUtrVal = SaUtr("1234567890")
//
//  lazy val html = Html.apply("<div>some snippet</div>")
//
//  val message = new MessageServiceMock("authToken")
//
//  val msgId1 = "543e8c6001000001003e4a9e"
//  val msgId2 = "643e8c5f01000001003e4a8f"
//
//  def messages(readTime: Long): String =
//    s"""[{"id":"$msgId1","subject":"You have a new tax statement","validFrom":"${timeNow.minusDays(3).toLocalDate}","readTime":$readTime,"readTimeUrl":"${encrypted(msgId1)}","sentInError":false},
//       |{"id":"$msgId2","subject":"Stopping Self Assessment","validFrom":"${timeNow.toLocalDate}","readTimeUrl":"${encrypted(msgId2)}","sentInError":false}]""".stripMargin
//
//  val testMessage1: MessageHeader = MessageHeader(MessageId(msgId1),
//    "You have a new tax statement",
//    timeNow.minusDays(3).toLocalDate,
//    None, sentInError = false)
//
//
//
//  val mockAuditConnector: AuditConnector = mock[AuditConnector]
//  val mockAuthConnector: AuthConnector = mock[AuthConnector]
//  val mockWsHttp: WSHttp = mock[WSHttp]
//  val testAuthUrl: String = "authTest"
//  val testServiceConfidenceLevel: Int = 200
//
//  def testBaseUrl(serviceName: String): String = "someUrl"
//
//  def mockBaseUrl: String => String = testBaseUrl
//
//
//  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"
//  val emptyRequest = FakeRequest()
//
//  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(POST, "url").
//    withBody(body).
//    withHeaders("Content-Type" -> "application/json")
//
//  val emptyRequestWithAcceptHeader = FakeRequest().withHeaders(acceptHeader)
//
//  lazy val readTimeRequest = fakeRequest(toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e")))).withHeaders(acceptHeader)
//  lazy val readTimeRequestNoHeaders = fakeRequest(toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))
//
//  val testAccess = new TestAccessControl(Some(nino), Some(saUtrVal), mockAuthConnector, mockWsHttp, testAuthUrl, testServiceConfidenceLevel)
//  val messageConnector = new TestMessageConnector(Seq.empty[MessageHeader], html, ReadMessage(id = MessageId("id1"), "someUrl"), mock[WSHttp], "someUrl", mockBaseUrl)
//  val testSandboxMMService = new TestSandboxMobileMessagesService(mock[Configuration], testAccess, messageConnector, mockAuditConnector)
//  val sandboxCompositeAction = new AccountAccessControlCheckAccessOff(testAccess)
//  val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
//  val testMMService: MobileMessagesService = mock[MobileMessagesService]
//}
//
//trait Success extends Setup {
//
//  val controller = new MobileMessagesController {
//    override val service: MobileMessagesService = testMMService
//    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
//    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
//  }
//}
//
//trait SuccessWithMessages extends Setup {
//
//  override val testMMService =
//    new TestMobileMessagesService(mock[Configuration], testAccess,
//      new TestMessageConnector(messageServiceHeadersResponse, html, ReadMessage(id = MessageId("id1"), "someUrl"), mock[WSHttp], "someUrl", mockBaseUrl), mockAuditConnector)
//
//  val controller = new MobileMessagesController {
//    override val service: MobileMessagesService = testMMService
//    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
//    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
//  }
//}
//
//trait AuthWithoutNino extends Setup {
//
//  override val testAccess = new TestAccessControl(None, None, mockAuthConnector, mockWsHttp, testAuthUrl, testServiceConfidenceLevel) {
//    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future.failed(Upstream4xxResponse("Error", 401, 401))
//  }
//
//  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
//
//  val controller = new MobileMessagesController {
//    override val service: MobileMessagesService = testMMService
//    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
//    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
//  }
//
//}
//
//trait AuthWithLowCL extends Setup {
//
//  override val testAccess = new TestAccessControl(None, None, mockAuthConnector, mockWsHttp, testAuthUrl, testServiceConfidenceLevel) {
//    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future.failed(new ForbiddenException("Error"))
//  }
//
//  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
//
//  val controller = new MobileMessagesController {
//    override val service: MobileMessagesService = testMMService
//    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
//    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
//  }
//
//}
//
//trait SandboxSuccess extends Setup {
//
//  val controller = new MobileMessagesController {
//    override val service: MobileMessagesService = testSandboxMMService
//    override val accessControl: AccountAccessControlWithHeaderCheck = sandboxCompositeAction
//    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
//  }
//}