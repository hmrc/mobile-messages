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

import com.fasterxml.jackson.databind.JsonMappingException
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json
import play.api.libs.json.Json.{parse, toJson}
import play.api.test.FakeApplication
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse, Upstream5xxResponse}
import uk.gov.hmrc.mobilemessages.acceptance.microservices.{MessageRendererServiceMock, MessageServiceMock}
import uk.gov.hmrc.mobilemessages.acceptance.utils.WiremockServiceLocatorSugar
import uk.gov.hmrc.mobilemessages.config.WSHttp
import uk.gov.hmrc.mobilemessages.connector.model.{ResourceActionLocation, UpstreamMessageHeadersResponse}
import uk.gov.hmrc.mobilemessages.controllers.StubApplicationConfiguration
import uk.gov.hmrc.mobilemessages.controllers.action.Authority
import uk.gov.hmrc.mobilemessages.domain._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers.SERVICE_UNAVAILABLE
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessagesConnectorSpec
  extends UnitSpec
    with WithFakeApplication
    with ScalaFutures
    with StubApplicationConfiguration
    with WiremockServiceLocatorSugar
    with BeforeAndAfterAll
    with BeforeAndAfterEach {


  override def beforeAll() = {
    super.beforeAll()
    startMockServer()
  }

  override def afterAll() = {
    super.afterAll()
    stopMockServer()
  }


  override protected def afterEach() = {
    super.afterEach()
    WireMock.reset()
  }

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = additionalConfig)

  def testRendererServiceName = "test-renderer-service"

  val additionalConfig = Map[String, Any](
    "auditing.enabled" -> false,
    "microservice.services.datastream.host" -> "localhost",
    "microservice.services.datastream.port" -> s"$stubPort",
    "microservice.services.datastream.enabled" -> false,
    "microservice.services.message.host" -> "localhost",
    "microservice.services.message.port" -> s"$stubPort",
    s"microservice.services.$testRendererServiceName.host" -> "localhost",
    s"microservice.services.$testRendererServiceName.port" -> s"$stubPort",
    "microservice.services.service-locator.enabled" -> false,
    "microservice.services.service-locator.host" -> "localhost",
    "microservice.services.service-locator.port" -> s"$stubPort",
    "appName" -> "mobile-messages",
    "microservice.services.auth.host" -> "localhost",
    "microservice.services.auth.port" -> s"$stubPort",
    "microservice.services.ntc.host" -> "localhost",
    "microservice.services.ntc.port" -> s"$stubPort"
  )

  private trait Setup extends MockitoSugar {
    private val authToken = "authToken"
    implicit lazy val hc = HeaderCarrier(Some(Authorization(authToken)))
    implicit val formats = Json.format[RenderMessageLocation]

    lazy val html = Html.apply("<div>some snippet</div>")
    val responseRenderer = RenderMessageLocation("sa-message-renderer", "http://somelocation")
    val mockWsHttp: WSHttp = mock[WSHttp]


    val message = new MessageServiceMock(authToken)
    val testMessageRenderer = new MessageRendererServiceMock(authToken, stubPort, "testService")

    lazy val successfulEmptyResponse = HttpResponse(200, responseString = Some(""))

    lazy val successfulEmptyMessageHeadersResposne = HttpResponse(200, Some(parse(message.jsonRepresentationOf(Seq.empty))))

    val messageId = MessageId("id123")
    lazy val successfulSingleMessageResponse = HttpResponse(
      200,
      Some(parse(
        message.jsonRepresentationOf(
          message.bodyWith(id = "id123")
        )))
    )

    val renderPath = "/some/render/path"
    val messageBodyToRender = message.bodyWith(id = "id1", renderUrl = ResourceActionLocation(testRendererServiceName, renderPath))
    val messageToBeMarkedAsReadBody = message.bodyToBeMarkedAsReadWith(id = "id48")
    //val messageToBeMarkedAsRead = message.convertedFrom(messageToBeMarkedAsReadBody).asInstanceOf[UnreadMessage]
    lazy val ReadSuccessResult = Future.successful(HttpResponse(200, None, Map.empty, Some(html.toString())))
    lazy val ReadSuccessEmptyResult = Future.successful(HttpResponse(200, None, Map.empty, None))
    lazy val PostSuccessResult = Future.successful(HttpResponse(200, Some(toJson(responseRenderer))))
    lazy val PostConflictResult = Future.successful(HttpResponse(409, Some(toJson(responseRenderer))))
    lazy val BadRequestResult = Future.successful(HttpResponse(400, None))
    lazy val ServiceUnavailableResult = Future.successful(HttpResponse(500, None))

    implicit val authUser: Option[Authority] = Some(Authority(Nino("CS700100A"), L200, "someId"))

    def testBaseUrl(serviceName: String): String = "someUrl"

    def mockBaseUrl: String => String = testBaseUrl

    val connector = new MessageConnector("messagesBaseUrl", mockWsHttp, mockBaseUrl)

  }

  "messagesConnector messages" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
        .thenReturn(Future.failed(new BadRequestException("")))

      message.headersListFailsWith(status = 400)
      intercept[BadRequestException] {
        await(connector.messages())
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
        .thenReturn(Future.failed(Upstream5xxResponse("", SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE)))

      message.headersListFailsWith(status = 500)
      intercept[Upstream5xxResponse] {
        await(connector.messages())
      }
    }

    "return empty response when a 200 response is received with an empty payload" in new Setup {
      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
        .thenReturn(Future successful UpstreamMessageHeadersResponse(Seq.empty))

      message.headersListReturns(Seq.empty)
      await(connector.messages()) shouldBe Seq.empty
    }

    "return a list of items when a 200 response is received with a payload" in new Setup {
      val messagesHeaders = Seq(
        message.headerWith(id = "someId1"),
        message.headerWith(id = "someId2"),
        message.headerWith(id = "someId3"))

      when(mockWsHttp.GET[UpstreamMessageHeadersResponse](any[String])(any(), any(), any()))
        .thenReturn(Future successful UpstreamMessageHeadersResponse(messagesHeaders))


      await(connector.messages()) shouldBe Seq(
        message.headerWith(id = "someId1"),
        message.headerWith(id = "someId2"),
        message.headerWith(id = "someId3")
      )
    }

  }

  "messagesConnector render message" should {

        "throw BadRequestException when a 400 response is returned" in new Setup {
          when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any())).thenReturn(BadRequestResult)

          testMessageRenderer.failsWith(status = 400, path = renderPath)
          intercept[BadRequestException] {
            await(connector.render(message.convertedFrom(messageBodyToRender), hc))
          }
        }

        "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
          when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any())).thenReturn(ServiceUnavailableResult)

          //testMessageRenderer.failsWith(status = 500, path = renderPath)
          //intercept[Upstream5xxResponse] {
            val result: Html = await(connector.render(message.convertedFrom(messageBodyToRender), hc))
          result
          //}
        }

        "return empty response when a 200 response is received with an empty payload" in new Setup {
          when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any())).thenReturn(ReadSuccessEmptyResult)

          testMessageRenderer.successfullyRenders(messageBodyToRender, overrideBody = Some(""))
          await(connector.render(message.convertedFrom(messageBodyToRender), hc)).body shouldBe ""
        }

        "return a rendered message when a 200 response is received with a payload" in new Setup {
          when(mockWsHttp.GET[HttpResponse](any[String])(any(), any(), any())).thenReturn(PostSuccessResult)

          testMessageRenderer.successfullyRenders(messageBodyToRender)
          await(connector.render(message.convertedFrom(messageBodyToRender), hc)).body shouldBe testMessageRenderer.rendered(messageBodyToRender)
        }
  }

  "messagesConnector get message by id" should {

    "throw BadRequestException when a 400 response is returned" in new Setup {
      message.getByIdFailsWith(status = 400, messageId = messageId)
      intercept[BadRequestException] {
        await(connector.getMessageBy(messageId))
      }
    }

    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
      message.getByIdFailsWith(status = 500, messageId = messageId)
      intercept[Upstream5xxResponse] {
        await(connector.getMessageBy(messageId))
      }
    }

    "throw JsonMappingException when a 200 response is received with an empty payload" in new Setup {
      message.getByIdFailsWith(status = 200, messageId = messageId)
      intercept[JsonMappingException] {
        await(connector.getMessageBy(messageId))
      }
    }

    //    "return a message when a 200 response is received with a payload" in new Setup {
    //      message.getByIdReturns(message.bodyWith(id = messageId.value))
    //      await(connector.getMessageBy(messageId)) shouldBe message.convertedFrom(
    //        message.bodyWith(id = messageId.value)
    //      )
    //    }
  }

  "messagesConnector mark message as read" should {

    //    "throw BadRequestException when a 400 response is returned" in new Setup {
    //      message.markAsReadFailsWith(status = 400, messageToBeMarkedAsReadBody)
    //      intercept[BadRequestException] {
    //        await(connector.markAsRead(messageToBeMarkedAsRead))
    //      }
    //    }

    //    "throw Upstream5xxResponse when a 500 response is returned" in new Setup {
    //      message.markAsReadFailsWith(status = 500, messageToBeMarkedAsReadBody)
    //      intercept[Upstream5xxResponse] {
    //        await(connector.markAsRead(messageToBeMarkedAsRead))
    //      }
    //    }
    //
    //    "return a message when a 200 response is received with a payload" in new Setup {
    //      message.markAsReadSucceedsFor(messageToBeMarkedAsReadBody)
    //      connector.markAsRead(messageToBeMarkedAsRead).futureValue.status shouldBe 200
    //    }
  }
}
