package uk.gov.hmrc.mobilemessages.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.mobilemessages.controllers.model.RenderMessageRequest
import uk.gov.hmrc.mobilemessages.stubs.AuthStub._
import uk.gov.hmrc.mobilemessages.stubs.MessageStub._
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

    "return a valid response without a journeyId with empty render payload" in {
      authRecordExists()
      messagesBySuccess("url1")
      renderCallSuccess()

      await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload" in {
      authRecordExists()
      messagesBySuccess("url1")
      renderCallSuccess()

      await(request(url, Some("journeyId")).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 200
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      await(request(url, None).post(Json.toJson(messageUrl))).status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      await(request(url, None).post(Json.toJson(messageUrl))).status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, None).post(Json.toJson(messageUrl))).status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesByNotFoundException("url1")

      await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesByServiceUnavailableException("url1")

      await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 500
    }

    "return 401 when authority call fails" in {
      unauthorised()
      await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 401
    }

    "return 401 with authorise call fails" in {
      authFailure()
      await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 401
    }
  }
}