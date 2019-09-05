package uk.gov.hmrc.mobilemessages.controllers

import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.mobilemessages.controllers.model.RenderMessageRequest
import uk.gov.hmrc.mobilemessages.mocks.AuthMock._
import uk.gov.hmrc.mobilemessages.mocks.MessageMock._
import uk.gov.hmrc.mobilemessages.support.BaseISpec

class MobileMessagesControllerISpec extends BaseISpec {

  def request(url: String, journeyId: String): WSRequest = {
    wsUrl(s"$url?journeyId=$journeyId").addHttpHeaders(acceptJsonHeader)
  }

  def requestWithoutAcceptHeader(url: String, journeyId: String): WSRequest = wsUrl(s"$url?journeyId=$journeyId")


  "GET /messages" should {
    val url = "/messages"

    "return a valid response with a journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(request(url, "journeyId").get()).status shouldBe 200
    }

    "return a 400 without a journeyId" in {
      authRecordExists()
      messagesAreFound()

      await(wsUrl(url).get()).status shouldBe 400
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, "journeyId").get()).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, "journeyId").get()).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, "journeyId").get()).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFoundException()

      await(request(url, "journeyId").get()).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceUnavailableException()

      await(request(url, "journeyId").get()).status shouldBe 500
    }

    "return 401 when authority call fails" in {
      unauthorised()
      await(request(url, "journeyId").get()).status shouldBe 401
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, "journeyId").get()).status shouldBe 401
    }
  }

  "POST /messages/read" should {
    val url        = "/messages/read"
    val messageUrl = RenderMessageRequest("L2U5NkwzTCtUdmQvSS9VVyt0MGh6UT09")
    val authHeader = ("Authorization", "auth1")

    def messagesRequest(journeyId: String) = request(url, journeyId).addHttpHeaders(authHeader)

    "return a valid response with a journeyId with empty render payload from message service" in {
      authRecordExists()
      messageFound("url1", "message")
      messageIsRenderedSuccessfully()

      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from sa-message-renderer" in {
      authRecordExists()
      messageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from ats-message-renderer" in {
      authRecordExists()
      messageFound("url1", "ats-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload from secure-message-renderer" in {
      authRecordExists()
      messageFound("url1", "secure-message-renderer")
      messageIsRenderedSuccessfully()

      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 200
    }

    "return a 400 without a journeyId" in {
      authRecordExists()
      messageFound("url1", "sa-message-renderer")
      messageIsRenderedSuccessfully()

      await(wsUrl(url).addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 400
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, "journeyId").post(toJson(messageUrl))).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, "journeyId").post(toJson(messageUrl))).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, "journeyId").post(toJson(messageUrl))).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFound("url1")

      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceIsUnavailable("url1")

      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 500
    }

    "return 401 when authority call fails" in {
      unauthorised()
      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 401
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, "journeyId").addHttpHeaders(authHeader).post(toJson(messageUrl))).status shouldBe 401
    }
  }
}
