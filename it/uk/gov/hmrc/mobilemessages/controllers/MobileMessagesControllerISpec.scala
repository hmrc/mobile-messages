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

      val response = await(request(url, None).get())

      response.status shouldBe 200
    }

    "return a valid response with a journeyId" in {
      authRecordExists()
      messagesAreFound()

      val response = await(request(url, Some("journeyId")).get())

      response.status shouldBe 200
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      val response = await(request(url, None).get())

      response.status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      val response = await(request(url, None).get())

      response.status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, None).get())

      response.status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesNotFoundException()

      val response = await(request(url, None).get())

      response.status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesServiceUnavailableException()

      val response = await(request(url, None).get())

      response.status shouldBe 500
    }
  }

  "POST /messages/read" should {
    val url = "/messages/read"
    val messageUrl = RenderMessageRequest("L2U5NkwzTCtUdmQvSS9VVyt0MGh6UT09")

    "return a valid response without a journeyId with empty render payload" in {
      authRecordExists()
      messagesBySuccess("url1")
      renderCallSuccess()

      val response = await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
    }

    "return a valid response with a journeyId with empty render payload" in {
      authRecordExists()
      messagesBySuccess("url1")
      renderCallSuccess()

      val response = await(request(url, Some("journeyId")).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
    }

    "return 403 when authority record does not have a high enough confidence level" in {
      authRecordExistsWithLowCL()

      val response = await(request(url, None).post(Json.toJson(messageUrl)))

      response.status shouldBe 403
    }

    "return 403 when authority record does not contain a NINO" in {
      authRecordExistsWithoutNino()

      val response = await(request(url, None).post(Json.toJson(messageUrl)))

      response.status shouldBe 403
    }

    "return a 406 when request does not contain an Accept header" in {
      val response = await(requestWithoutAcceptHeader(url, None).post(Json.toJson(messageUrl)))

      response.status shouldBe 406
    }

    "return a valid response when MessageConnector returns 404" in {
      authRecordExists()
      messagesByNotFoundException("url1")

      val response = await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 404
    }

    "return a valid response when MessageConnector returns 500" in {
      authRecordExists()
      messagesByServiceUnavailableException("url1")

      val response = await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 500
    }
  }
}