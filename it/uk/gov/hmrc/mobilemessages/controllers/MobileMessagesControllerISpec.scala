package uk.gov.hmrc.mobilemessages.controllers

import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.mobilemessages.controllers.model.RenderMessageRequest
import uk.gov.hmrc.mobilemessages.domain.{MessageCountResponse, Shuttering}
import uk.gov.hmrc.mobilemessages.mocks.AuthMock._
import uk.gov.hmrc.mobilemessages.mocks.MessageMock._
import uk.gov.hmrc.mobilemessages.mocks.ShutteringMock._
import uk.gov.hmrc.mobilemessages.support.BaseISpec

class MobileMessagesControllerISpec extends BaseISpec {
  val authorisationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer 123"

  def request(
    url:       String,
    journeyId: String
  ): WSRequest =
    wsUrl(s"$url?journeyId=$journeyId").addHttpHeaders(acceptJsonHeader)

  def requestWithoutAcceptHeader(
    url:       String,
    journeyId: String
  ): WSRequest = wsUrl(s"$url?journeyId=$journeyId")

  "GET /messages" should {
    val url = "/messages"

    "return a valid response with a journeyId" in {
      authRecordExists()
      messagesAreFound()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 200
    }

    "return a 400 without a journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(wsUrl(url).get()).status shouldBe 400
    }

    "return a 400 with invalid journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(request(url, "ThisIsAnInvalidJourneyId").get()).status shouldBe 400
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, journeyId).get()).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFoundException()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 429" in {
      authRecordExists()
      messagesTooManyRequestsException()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 429
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceUnavailableException()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 500
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, journeyId).get()).status shouldBe 401
    }

    "return shuttered when shuttered" in {
      authRecordExists()
      stubForShutteringEnabled

      val response = await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get())

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Messages are currently not available")
    }
  }

  "GET /messages/count" should {
    val url = "/messages/count"

    "return a valid response with a journeyId" in {
      authRecordExists()
      messageCountFound()
      stubForShutteringDisabled

      val response = await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get())
      response.status shouldBe 200
      val parsedResponse = Json.parse(response.body).as[MessageCountResponse]
      parsedResponse.count.unread shouldBe 1
      parsedResponse.count.total  shouldBe 2
    }

    "return a 400 without a journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(wsUrl(url).get()).status shouldBe 400
    }

    "return a 400 with invalid journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(request(url, "ThisIsAnInvalidJourneyId").get()).status shouldBe 400
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, journeyId).get()).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesCountNotFoundException()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesCountServiceUnavailableException()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 500
    }

    "return a valid response when MessageConnector returns 429" in {
      authRecordExists()
      messagesCountTooManyRequestsException()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get()).status shouldBe 429
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, journeyId).get()).status shouldBe 401
    }

    "return shuttered when shuttered" in {
      authRecordExists()
      stubForShutteringEnabled

      val response = await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).get())

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Messages are currently not available")
    }
  }

  "POST /messages/read" should {
    val url        = "/messages/read"
    val messageUrl = RenderMessageRequest("L2U5NkwzTCtUdmQvSS9VVyt0MGh6UT09")
    val authHeader = ("Authorization", "auth1")

    "return a valid response with a journeyId with empty render payload and headers from message service" in {
      authRecordExists()
      messageFound("url1", "secure-message")
      messageIsRenderedSuccessfully()
      stubForShutteringDisabled

      val result: WSResponse = await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl)))

      result.status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload and no headers from message service" in {
      authRecordExists()
      messageFound("url1", "secure-message", false)
      messageIsRenderedSuccessfully()
      stubForShutteringDisabled

      val result:  WSResponse               = await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl)))
      val headers: Map[String, Seq[String]] = result.headers

      result.status                    shouldBe 200
      headers.find(_._1 == "type")     shouldBe None
      headers.find(_._1 == "threadId") shouldBe None
    }

    "return a valid response with a journeyId with empty render payload from sa-message-renderer" in {
      authRecordExists()
      messageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from ats-message-renderer" in {
      authRecordExists()
      messageFound("url1", "ats-message-renderer")
      messageIsRenderedSuccessfully()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from secure-message-renderer" in {
      authRecordExists()
      messageFound("url1", "secure-message-renderer")
      messageIsRenderedSuccessfully()
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a 400 without a journeyId" in {
      authRecordExists()
      messageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()

      await(wsUrl(url).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 400
    }

    "return a 400 with an invalid journeyId" in {
      authRecordExists()
      messageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, "ThisIsAnInvalidJourneyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 400
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).post(toJson(messageUrl))).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, journeyId).addHttpHeaders(authorisationJsonHeader).post(toJson(messageUrl))).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, journeyId).post(toJson(messageUrl))).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFound("url1")
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceIsUnavailable("url1")
      stubForShutteringDisabled

      await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 500
    }

    "return 401 with authorise call fails" in {
      authFailure()
      val response = await(request(url, journeyId).addHttpHeaders(authHeader).post(toJson(messageUrl)))
      print("RESPONSE" + response.body)
      response.status shouldBe 401
    }

    "return shuttered when shuttered" in {
      authRecordExists()
      stubForShutteringEnabled

      val response =
        await(request(url, journeyId).addHttpHeaders(authHeader, authorisationJsonHeader).post(toJson(messageUrl)))

      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("Messages are currently not available")
    }
  }
}
