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

import org.joda.time.DateTime
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilemessages.acceptance.microservices.MessageServiceMock
import uk.gov.hmrc.mobilemessages.config.WSHttp
import uk.gov.hmrc.mobilemessages.connector.MessageConnector
import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControl, AccountAccessControlCheckAccessOff, AccountAccessControlWithHeaderCheck, Authority}
import uk.gov.hmrc.mobilemessages.controllers.model.{MessageHeaderResponseBody, RenderMessageRequest}
import uk.gov.hmrc.mobilemessages.domain.{Message, MessageHeader, MessageId, ReadMessage}
import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
import uk.gov.hmrc.mobilemessages.utils.EncryptionUtils.encrypted
import uk.gov.hmrc.mobilemessages.utils.UnitTestCrypto
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

class TestAccessControl(nino: Option[Nino], saUtr: Option[SaUtr]) extends AccountAccessControl with MockitoSugar {
  override val authConnector = mock[AuthConnector]
  override val http: WSHttp = mock[WSHttp]
  override val authUrl = "authUrl"

  override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] =
    Future(Authority(nino.getOrElse(throw new Exception("Invalid nino")), L200, "some-auth-id"))
}

class TestMessageConnector(result: Seq[MessageHeader], html: Html, message: Message, http: WSHttp, messageBaseUrl: String)
  extends MessageConnector(messageBaseUrl, http) with MockitoSugar with TimeSetup {

  override val now: DateTime = timeNow

  override def messages()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = Future.successful(result)

  override def getMessageBy(id: MessageId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Message] = Future.successful(message)

  override def render(message: Message, hc: HeaderCarrier)(implicit ec: ExecutionContext, auth: Option[Authority]): Future[Html] = Future.successful(html)
}

class TestMobileMessagesService(override val appNameConfiguration: Configuration, testAccessControl: TestAccessControl,
                                mobileMessageConnector: MessageConnector, testAuditConnector: AuditConnector)
  extends LiveMobileMessagesService(mobileMessageConnector, testAuditConnector, testAccessControl, appNameConfiguration) {
  var saveDetails: Map[String, String] = Map.empty

  override def audit(service: String, details: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    saveDetails = details
    Future.successful(AuditResult.Success)
  }

  override val accountAccessControl = testAccessControl
  override val messageConnector = mobileMessageConnector
  override val auditConnector: AuditConnector = testAuditConnector
}

class TestSandboxMobileMessagesService(val appNameConfiguration: Configuration, testAccessControl: TestAccessControl,
                                mobileMessageConnector: MessageConnector, testAuditConnector: AuditConnector) extends SandboxMobileMessagesService {
  override val saUtr = SaUtr("1234567890")
}

class TestAccountAccessControlWithAccept(testAccessCheck: AccountAccessControl) extends AccountAccessControlWithHeaderCheck(testAccessCheck) {
  override val accessControl: AccountAccessControl = testAccessCheck
}

trait TimeSetup {
  val timeNow: DateTime = DateTimeUtils.now
}

trait Setup extends MockitoSugar with TimeSetup {
  implicit val hc = HeaderCarrier()

  val journeyId = Option(randomUUID().toString)

  val nino = Nino("CS700100A")
  val saUtrVal = SaUtr("1234567890")

  lazy val html = Html.apply("<div>some snippet</div>")

  val message = new MessageServiceMock("authToken")

  val msgId1 = "543e8c6001000001003e4a9e"
  val msgId2 = "643e8c5f01000001003e4a8f"
  val messages =
    s"""[{"id":"$msgId1","subject":"You have a new tax statement","validFrom":"${timeNow.minusDays(3).toLocalDate}","readTimeUrl":"${encrypted(msgId1)}","sentInError":false},
       |{"id":"$msgId2","subject":"Stopping Self Assessment","validFrom":"${timeNow.toLocalDate}","readTimeUrl":"${encrypted(msgId2)}","sentInError":false}]""".stripMargin

  def messageHeaderResponseBodyFrom(messageHeader: MessageHeader) = MessageHeaderResponseBody(
    messageHeader.id.value,
    messageHeader.subject,
    messageHeader.validFrom,
    messageHeader.readTime,
    readTimeUrl = encrypted(messageHeader.id.value),
    messageHeader.sentInError
  )


  val mockAuditConnector = mock[AuditConnector]

  val messageServiceHeadersResponse = Seq(
    message.headerWith(id = "id1"),
    message.headerWith(id = "id2"),
    message.headerWith(id = "id3")
  )
  val getMessagesResponseItemsList = messageServiceHeadersResponse.
    map(messageHeaderResponseBodyFrom)

//  val sampleMessage = message.convertedFrom(message.bodyWith(id = "id1"))

  val acceptHeader = "Accept" -> "application/vnd.hmrc.1.0+json"
  val emptyRequest = FakeRequest()

  def fakeRequest(body: JsValue) = FakeRequest(POST, "url").
    withBody(body).
    withHeaders("Content-Type" -> "application/json")

  val emptyRequestWithAcceptHeader = FakeRequest().withHeaders(acceptHeader)

  lazy val readTimeRequest = fakeRequest(toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e")))).withHeaders(acceptHeader)
  lazy val readTimeRequestNoHeaders = fakeRequest(toJson(RenderMessageRequest(encrypted("543e8c6001000001003e4a9e"))))

  val testAccess = new TestAccessControl(Some(nino), Some(saUtrVal))
  val messageConnector = new TestMessageConnector(Seq.empty[MessageHeader], html, ReadMessage(id = MessageId("id1"), "someUrl"), mock[WSHttp], "someUrl")
  val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
  val testMMService = new TestMobileMessagesService(mock[Configuration], testAccess, messageConnector, mockAuditConnector)

  val testSandboxMMService = new TestSandboxMobileMessagesService(mock[Configuration], testAccess, messageConnector, mockAuditConnector)

  val sandboxCompositeAction = new AccountAccessControlCheckAccessOff(testAccess)
}

trait Success extends Setup {

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
  }
}

trait SuccessWithMessages extends Setup {

  override val testMMService =
    new TestMobileMessagesService(mock[Configuration], testAccess,
      new TestMessageConnector(messageServiceHeadersResponse, html, ReadMessage(id = MessageId("id1"), "someUrl"), mock[WSHttp], "someUrl"), mockAuditConnector)

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
  }
}

trait AuthWithoutNino extends Setup {

  override val testAccess = new TestAccessControl(None, None) {
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future.failed(new Upstream4xxResponse("Error", 401, 401))
  }

  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
  }

}

trait AuthWithLowCL extends Setup {

  override val testAccess = new TestAccessControl(None, None) {
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future.failed(new ForbiddenException("Error"))
  }

  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
  }

}

trait SandboxSuccess extends Setup {

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testSandboxMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = sandboxCompositeAction
    override val crypto: Encrypter with Decrypter = new UnitTestCrypto
  }
}