package uk.gov.hmrc.mobilemessages.controllers

import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.mobilemessages.controllers.model.RenderMessageRequest
import uk.gov.hmrc.mobilemessages.mocks.AuthMock._
import uk.gov.hmrc.mobilemessages.mocks.MessageMock._
import uk.gov.hmrc.mobilemessages.support.BaseISpec

class MobileMessagesControllerISpec extends BaseISpec {

  def request(url: String, journeyId: Option[String]): WSRequest = {
    val urlMaybeId = journeyId.fold(url) { id => s"$url?journeyId=$id" }
    wsUrl(urlMaybeId).withHeaders(acceptJsonHeader)
  }

  def requestWithoutAcceptHeader(url: String, journeyId: Option[String]): WSRequest = {
    val urlMaybeId = journeyId.fold(url) { id => s"$url?journeyId=$id" }
    wsUrl(urlMaybeId)
  }

  "GET /messages" should {
    val url = "/messages"

    "return a valid response without a journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(request(url, None).get()).status shouldBe 200
    }

    "return a valid response with a journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(request(url, Some("journeyId")).get()).status shouldBe 200
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, None).get()).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, None).get()).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, None).get()).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFoundException()

      await(request(url, None).get()).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceUnavailableException()

      await(request(url, None).get()).status shouldBe 500
    }

    "return 401 when authority call fails" in {
      unauthorised()
      await(request(url, None).get()).status shouldBe 401
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, None).get()).status shouldBe 401
    }
  }

  "POST /messages/read" should {
    val url = "/messages/read"
    val messageUrl = RenderMessageRequest("L2U5NkwzTCtUdmQvSS9VVyt0MGh6UT09")
    val authHeader = ("Authorization", "auth1")

    def messagesRequest(journeyId: Option[String]) = request(url, journeyId).withHeaders(authHeader)

    "return a valid response without a journeyId with empty render payload from sa-message-renderer" in {
      authRecordExists()
      mmessageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, None).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from message service" in {
      authRecordExists()
      mmessageFound("url1", "message")
      messageIsRenderedSuccessfully()

      await(request(url, Some("journeyId")).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from sa-message-renderer" in {
      authRecordExists()
      mmessageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, Some("journeyId")).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from ats-message-renderer" in {
      authRecordExists()
      mmessageFound("url1", "ats-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, Some("journeyId")).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from secure-message-renderer" in {
      authRecordExists()
      mmessageFound("url1", "secure-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, Some("journeyId")).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, None).post(toJson(messageUrl))).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, None).post(toJson(messageUrl))).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, None).post(toJson(messageUrl))).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFound("url1")

      await(request(url, None).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceIsUnavailable("url1")

      await(request(url, None).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 500
    }

    "return 401 when authority call fails" in {
      unauthorised()
      await(request(url, None).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 401
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, None).withHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 401
    }
  }
}