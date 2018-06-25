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

package uk.gov.hmrc.mobilemessages.connector

import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, OFormat}
import play.api.test.FakeApplication
import play.api.test.Helpers.SERVICE_UNAVAILABLE
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilemessages.config.WSHttp
import uk.gov.hmrc.mobilemessages.connector.model.{ResourceActionLocation, UpstreamMessageHeadersResponse, UpstreamMessageResponse}
import uk.gov.hmrc.mobilemessages.controllers.Setup
import uk.gov.hmrc.mobilemessages.stubs.StubApplicationConfiguration
import uk.gov.hmrc.mobilemessages.controllers.auth.{Authority, AuthorityRecord}
import uk.gov.hmrc.mobilemessages.domain._
import uk.gov.hmrc.mobilemessages.utils.MessageServiceMock
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MessagesConnectorSpec
  extends UnitSpec
    with Setup
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfterEach {


//  override def beforeAll(): Unit = {
//    super.beforeAll()
//    startMockServer()
//  }
//
//  override def afterAll(): Unit = {
//    super.afterAll()
//    stopMockServer()
//  }
//
//
//  override protected def afterEach(): Unit = {
//    super.afterEach()
//    WireMock.reset()
//  }

//  override lazy val fakeApplication = FakeApplication(additionalConfiguration = config)

//  private trait Setup extends MockitoSugar {
//    private val authToken = "authToken"
//    implicit lazy val hc: HeaderCarrier = HeaderCarrier(Some(Authorization(authToken)))
//    implicit val formats: OFormat[RenderMessageLocation] = Json.format[RenderMessageLocation]
//
//    lazy val html = Html.apply("<div>some snippet</div>")
//    val responseRenderer = RenderMessageLocation("sa-message-renderer", "http://somelocation")
//    val mockWsHttp: WSHttp = mock[WSHttp]
//
//    val message = new MessageServiceMock(authToken)
//    val messageId = MessageId("id123")
//
//    val renderPath = "/some/render/path"
//    val messageBodyToRender: UpstreamMessageResponse =
//      message.bodyWith(id = "id1", renderUrl = ResourceActionLocation("test-renderer-service", renderPath))
//    val messageToBeMarkedAsReadBody: UpstreamMessageResponse = message.bodyToBeMarkedAsReadWith(id = "id48")
//    val messageToBeMarkedAsRead: UnreadMessage = message.convertedFrom(messageToBeMarkedAsReadBody).asInstanceOf[UnreadMessage]
//    lazy val ReadSuccessEmptyResult = Future.successful(HttpResponse(200, None, Map.empty, None))
//    lazy val PostSuccessResult = Future.successful(HttpResponse(200, Some(toJson(html.body))))
//    lazy val PostSuccessRendererResult = Future.successful(HttpResponse(200, Some(toJson(responseRenderer))))
//    implicit val authUser: Option[Authority] = Some(Authority(Nino("CS700100A"), L200, "someId"))
//
//    def testBaseUrl(serviceName: String): String = "http://localhost:8089"
//
//    def mockBaseUrl: String => String = testBaseUrl
//
//    val connector = new MessageConnector("messagesBaseUrl", mockWsHttp, mockBaseUrl)
//
//  }

//  (http.GET(_: String)(_: HttpReads[UpstreamMessageHeadersResponse], _: HeaderCarrier, _: ExecutionContext))
//    .expects(*, *, *, *).returns(Future successful UpstreamMessageHeadersResponse(messageServiceHeadersResponse))

  def testBaseUrl(serviceName: String): String = "http://localhost:8089"
  def mockBaseUrl: String => String = testBaseUrl

  val connector: MessageConnector = new MessageConnector("messagesBaseUrl", http, mockBaseUrl)

  "messagesConnector messages" should {

    "throw BadRequestException when a 400 response is returned" in {
//      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(new BadRequestException("")))

      (http.GET(_: String)(_: HttpReads[UpstreamMessageHeadersResponse], _: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *).returns(Future failed new BadRequestException(""))

      intercept[BadRequestException] {
        await(connector.messages())
      }
    }

//    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
//      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
//
//      intercept[Upstream5xxResponse] {
//        await(connector.messages())
//      }
//    }
//
//    "return empty response when a 200 response is received with an empty payload" in new Setup {
//      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future successful UpstreamMessageHeadersResponse(Seq.empty))
//
//      await(connector.messages()) shouldBe Seq.empty
//    }
//
//    "return a list of items when a 200 response is received with a payload" in new Setup {
//      val messagesHeaders = Seq(
//        message.headerWith(id = "someId1"),
//        message.headerWith(id = "someId2"),
//        message.headerWith(id = "someId3"))
//
//      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future successful UpstreamMessageHeadersResponse(messagesHeaders))
//
//
//      await(connector.messages()) shouldBe Seq(
//        message.headerWith(id = "someId1"),
//        message.headerWith(id = "someId2"),
//        message.headerWith(id = "someId3")
//      )
//    }

  }

  "messagesConnector render message" should {
//
//    "throw BadRequestException when a 400 response is returned" in new Setup {
//      when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(new BadRequestException("")))
//
//      intercept[BadRequestException] {
//        await(connector.render(message.convertedFrom(messageBodyToRender), hc))
//      }
//    }
//
//    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
//      when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
//
//      intercept[Upstream5xxResponse] {
//        await(connector.render(message.convertedFrom(messageBodyToRender), hc))
//      }
//    }
//
//    "return empty response when a 200 response is received with an empty payload" in new Setup {
//      when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any())).thenReturn(ReadSuccessEmptyResult)
//
//      await(connector.render(message.convertedFrom(messageBodyToRender), hc)).body shouldBe ""
//    }
//
//    "return a rendered message when a 200 response is received with a payload" in new Setup {
//      when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any())).thenReturn(PostSuccessResult)
//
//      await(connector.render(message.convertedFrom(messageBodyToRender), hc)).body should include(s"${html.body}")
//    }
  }

  "messagesConnector get message by id" should {

//    "throw BadRequestException when a 400 response is returned" in new Setup {
//      when(mockWsHttp.GET[UpstreamMessageResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(new BadRequestException("")))
//
//      intercept[BadRequestException] {
//        await(connector.getMessageBy(messageId))
//      }
//    }
//
//    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
//      when(mockWsHttp.GET[UpstreamMessageResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
//
//      intercept[Upstream5xxResponse] {
//        await(connector.getMessageBy(messageId))
//      }
//    }
//
//    "return a message when a 200 response is received with a payload" in new Setup {
//      when(mockWsHttp.GET[UpstreamMessageResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future successful message.bodyWith(id = messageId.value))
//
//      await(connector.getMessageBy(messageId)) shouldBe message.convertedFrom(
//        message.bodyWith(id = messageId.value)
//      )
//    }
  }

  "messagesConnector mark message as read" should {

//    "throw BadRequestException when a 400 response is returned" in new Setup {
//      when(mockWsHttp.POSTEmpty[HttpResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(new BadRequestException("")))
//
//      intercept[BadRequestException] {
//        await(connector.markAsRead(messageToBeMarkedAsRead))
//      }
//    }
//
//    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
//      when(mockWsHttp.POSTEmpty[HttpResponse](any[String])(any(), any(), any()))
//        .thenReturn(Future.failed(Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))
//
//      intercept[Upstream5xxResponse] {
//        await(connector.markAsRead(messageToBeMarkedAsRead))
//      }
//    }
//
//    "return a message when a 200 response is received with a payload" in new Setup {
//      when(mockWsHttp.POSTEmpty[HttpResponse](any[String])(any(), any(), any())).thenReturn(PostSuccessRendererResult)
//
//      connector.markAsRead(messageToBeMarkedAsRead).futureValue.status shouldBe 200
//    }
  }
}
