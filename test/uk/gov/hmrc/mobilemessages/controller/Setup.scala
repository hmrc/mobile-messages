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

package uk.gov.hmrc.mobilemessages.controller

import java.util.UUID

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.mobilemessages.config.MicroserviceAuditConnector
import uk.gov.hmrc.mobilemessages.connector.{AuthConnector, Authority, EntityResolverConnector, MessageConnector}
import uk.gov.hmrc.mobilemessages.controllers.MobileMessagesController
import uk.gov.hmrc.mobilemessages.controllers.action.{AccountAccessControl, AccountAccessControlCheckAccessOff, AccountAccessControlWithHeaderCheck}
import uk.gov.hmrc.mobilemessages.domain.{Accounts, MessageHeader, ReadTimeUrl}
import uk.gov.hmrc.mobilemessages.services.{LiveMobileMessagesService, MobileMessagesService, SandboxMobileMessagesService}
import uk.gov.hmrc.time.DateTimeUtils
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}


class TestAuthConnector(nino: Option[Nino], saUtr:Option[SaUtr]) extends AuthConnector {
  override val serviceUrl: String = "someUrl"

  override def serviceConfidenceLevel: ConfidenceLevel = ???

  override def http: HttpGet = ???

  override def accounts()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Accounts] = Future(Accounts(nino, saUtr))

  override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future(Authority(nino.getOrElse(throw new Exception("Invalid nino")), ConfidenceLevel.L200, "some-auth-id"))
}

class TestMessageConnector(result:Seq[MessageHeader], html:Html) extends MessageConnector {

  override def http: HttpGet with HttpPost = ???

  override def now: DateTime = ???

  override val messageBaseUrl: String = "someUrl"

  override def readMessageContent(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, auth: Option[uk.gov.hmrc.mobilemessages.connector.Authority]): Future[Html] = Future.successful(html)

  override val provider: String = "provider"
  override val token: String = "token"
  override val id: String = "id"
}

class TestEntityResolverConnector(result:Seq[MessageHeader]) extends EntityResolverConnector {

  override def http: HttpGet with HttpPost = ???

  override val entityResolverBaseUrl: String = "someUrl"

  private var lastUtr = SaUtr("utr not established")
  def lastUtrPassed = lastUtr

  override def messages(utr: SaUtr)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MessageHeader]] = {
    lastUtr = utr
    Future.successful(result)
  }
}

class TestMobileMessagesService(testAuthConnector:TestAuthConnector,
                                mobileMessageConnector:MessageConnector,
                                mobileEntityResolverConnector:EntityResolverConnector)
  extends LiveMobileMessagesService {
  var saveDetails:Map[String, String]=Map.empty

  override def audit(service: String, details: Map[String, String])
                    (implicit hc: HeaderCarrier, ec : ExecutionContext) = {
    saveDetails=details
    Future.successful(AuditResult.Success)
  }

  override val authConnector = testAuthConnector
  override val messageConnector = mobileMessageConnector
  override val entityResolverConnector = mobileEntityResolverConnector
  override val auditConnector: AuditConnector = MicroserviceAuditConnector
}

class TestAccessCheck(testAuthConnector: TestAuthConnector) extends AccountAccessControl {
  override val authConnector: AuthConnector = testAuthConnector
}

class TestAccountAccessControlWithAccept(testAccessCheck:AccountAccessControl) extends AccountAccessControlWithHeaderCheck {
  override val accessControl: AccountAccessControl = testAccessCheck
}


trait Setup {
  implicit val hc = HeaderCarrier()

  val journeyId = Option(UUID.randomUUID().toString)

  val nino = Nino("CS700100A")
  val saUtrVal = SaUtr("1234567890")

  val now = DateTimeUtils.now
  val messages =
    s"""[{"id":"543e8c6001000001003e4a9e","subject":"You have a new tax statement","validFrom":"${now.minusDays(3).toLocalDate}","readTime":${now.minusDays(1).getMillis},"readTimeUrl":"/message/sa/${saUtrVal.value}/543e8c6001000001003e4a9e/read-time","sentInError":false},
       |{"id":"643e8c5f01000001003e4a8f","subject":"Stopping Self Assessment","validFrom":"${now.toLocalDate}","readTimeUrl":"/message/sa/${saUtrVal.value}/643e8c5f01000001003e4a8f/read-time","sentInError":false}]""".stripMargin

  lazy val html = Html.apply("<div>some snippet</div>")


  val messageHeader = MessageHeader("someId",
    subject="someSubject",
    validFrom =  LocalDate.now(),
    readTime= None,
    readTimeUrl="someUrl",
    sentInError=false)
  val messageHeaderList = Seq(messageHeader, messageHeader.copy(id="id2"), messageHeader.copy(id="id3"))

  val acceptHeader = "Accept" -> "application/vnd.hmrc.1.0+json"
  val emptyRequest = FakeRequest()

  def fakeRequest(body:JsValue) = FakeRequest(POST, "url").withBody(body)
    .withHeaders("Content-Type" -> "application/json")

  val emptyRequestWithAcceptHeader = FakeRequest().withHeaders(acceptHeader)

  lazy val readTimeRequest = fakeRequest(Json.toJson(ReadTimeUrl("/somewhere/543e8c6001000001003e4a9e"))).withHeaders(acceptHeader)
  lazy val readTimeRequestNoHeaders = fakeRequest(Json.toJson(ReadTimeUrl("/somewhere/543e8c6001000001003e4a9e")))

  val authConnector = new TestAuthConnector(Some(nino), Some(saUtrVal))
  val messageConnector = new TestMessageConnector(Seq.empty[MessageHeader], html)
  val entityResolverConnector = new TestEntityResolverConnector(Seq.empty[MessageHeader])
  val testAccess = new TestAccessCheck(authConnector)
  val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)
  val testMMService = new TestMobileMessagesService(authConnector, messageConnector, entityResolverConnector)

  object sandbox extends SandboxMobileMessagesService {
      implicit val dateTime = now
      val saUtr = saUtrVal
  }
  val testSandboxPersonalIncomeService = sandbox


  val sandboxCompositeAction = AccountAccessControlCheckAccessOff
}

trait Success extends Setup {

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}

trait SuccessWithMessages extends Setup {

  val testEntityResolverConnector = new TestEntityResolverConnector(messageHeaderList)
  override val testMMService = new TestMobileMessagesService(
    authConnector,
    new TestMessageConnector(Seq.empty, html),
    testEntityResolverConnector
  )

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}


trait AuthWithoutNino extends Setup {

  override val authConnector =  new TestAuthConnector(None, None) {
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future.failed(new Upstream4xxResponse("Error", 401, 401))
  }

  override val testAccess = new TestAccessCheck(authConnector)
  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }

}


trait AuthWithLowCL extends Setup {

  override val authConnector =  new TestAuthConnector(None, None) {
    override def grantAccess()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Authority] = Future.failed(new ForbiddenException("Error"))
  }

  override val testAccess = new TestAccessCheck(authConnector)
  override val testCompositeAction = new TestAccountAccessControlWithAccept(testAccess)

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testMMService
    override val accessControl: AccountAccessControlWithHeaderCheck = testCompositeAction
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }

}


trait SandboxSuccess extends Setup {

  val controller = new MobileMessagesController {
    override val service: MobileMessagesService = testSandboxPersonalIncomeService
    override val accessControl: AccountAccessControlWithHeaderCheck = sandboxCompositeAction
    override implicit val ec: ExecutionContext = ExecutionContext.global
  }
}
