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

package uk.gov.hmrc.mobilemessages.connector

import org.joda.time.{DateTime, LocalDate}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.{Json, Writes}
import play.api.test.FakeApplication
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.mobilemessages.controller.StubApplicationConfiguration
import uk.gov.hmrc.mobilemessages.domain.{MessageHeader, RenderMessageLocation}
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EntityResolverConnectorSpec extends UnitSpec
  with WithFakeApplication
  with ScalaFutures
  with StubApplicationConfiguration {

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

  private trait Setup {
    implicit lazy val hc = HeaderCarrier(Some(Authorization("some value")))

    lazy val html = Html.apply("<div>some snippet</div>")
    val saUtr = SaUtr("1234567890")
    val nino = Nino("CS700100A")
    val responseRenderer = RenderMessageLocation("sa-message-renderer","http://somelocation")
    val messageHeader = MessageHeader("someId",
                          subject="someSubject",
                         validFrom =  LocalDate.now(),
                         readTime= None,
                         readTimeUrl="someUrl",
                         sentInError=false)

    lazy val http500Response = Future.failed(new Upstream5xxResponse("Error", 500, 500))
    lazy val http400Response = Future.failed(new BadRequestException("bad request"))
    lazy val http200ResponseEmpty = Future.successful(HttpResponse(200, Some(Json.toJson(Seq.empty[MessageHeader]))))
    lazy val http200Response = Future.successful(HttpResponse(200, Some(Json.toJson(Seq(messageHeader,messageHeader,messageHeader)))))

    lazy val ReadSuccessResult = Future.successful(HttpResponse(200, None, Map.empty, Some(html.toString())))
    lazy val PostSuccessResult = Future.successful(HttpResponse(200, Some(Json.toJson(responseRenderer))))
    lazy val PostConflictResult = Future.successful(HttpResponse(409, Some(Json.toJson(responseRenderer))))

    lazy val responseGet: Future[HttpResponse] = http400Response
    lazy val responsePost: Future[HttpResponse] = PostSuccessResult

    implicit val authUser : Option[Authority] = Some(Authority(nino, ConfidenceLevel.L200, "someId"))

    val httpStub = new HttpGet with HttpPost {
      override val hooks: Seq[HttpHook] = NoneRequired
      private var lastUrl: String = "never called"
      def lastUrlCalled = lastUrl

      override protected def doGet(url: String)
                                  (implicit hc: HeaderCarrier): Future[HttpResponse] = {
        lastUrl = url
        responseGet
      }

      override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])
                                      (implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        lastUrl = url
        responsePost
      }

      override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])
                                         (implicit hc: HeaderCarrier): Future[HttpResponse] = {
        lastUrl = url
        responsePost
      }

      override protected def doFormPost(url: String, body: Map[String, Seq[String]])
                                       (implicit hc: HeaderCarrier): Future[HttpResponse] = {
        lastUrl = url
        responsePost
      }

      override protected def doEmptyPost[A](url: String)
                                           (implicit hc: HeaderCarrier): Future[HttpResponse] = {
        lastUrl = url
        responsePost
      }
    }

    val entityResolverUrl = "somebase-url"

    val connector = new EntityResolverConnector {
      override def http = httpStub

      override val entityResolverBaseUrl: String = entityResolverUrl
    }
  }

  "entityResolverConnector messages" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      override lazy val responseGet = http400Response
        intercept[BadRequestException] {
          await(connector.messages)
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      override lazy val responseGet = http500Response
      intercept[Upstream5xxResponse] {
        await(connector.messages)
      }
    }

    "return empty response when a 200 response is received with an empty payload" in new Setup {
      override lazy val responseGet = http200ResponseEmpty
      connector.messages.futureValue shouldBe Seq.empty
    }

    "return a list of items when a 200 response is received with a payload" in new Setup {
      override lazy val responseGet = http200Response
      connector.messages.futureValue shouldBe Seq(messageHeader,messageHeader,messageHeader)
    }

    "be calling entity resolver with the correct url" in new Setup {
      override lazy val responseGet = http200Response
      await(connector.messages)
      httpStub.lastUrlCalled shouldBe s"$entityResolverUrl/messages?read=Both"
    }
  }
}
