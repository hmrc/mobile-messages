package uk.gov.hmrc.mobilemessages.controllers

import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import uk.gov.hmrc.mobilemessages.controllers.model.RenderMessageRequest
import uk.gov.hmrc.mobilemessages.sandbox.MessageContentPartialStubs._
import uk.gov.hmrc.mobilemessages.support.BaseISpec

class SandboxMobileMessagesControllerISpec extends BaseISpec {

  def request(url: String, journeyId: Option[String], sandboxControl: String = "NEW-TAX-STATEMENT"): WSRequest = {
    val urlMaybeId = journeyId.fold(url) { id => s"$url?journeyId=$id" }
    wsUrl(urlMaybeId).withHeaders(acceptJsonHeader, "SANDBOX-CONTROL" -> s"$sandboxControl", "X-MOBILE-USER-ID" -> "208606423740")
  }

  def requestWithoutAcceptHeader(url: String, journeyId: Option[String]): WSRequest = {
    val urlMaybeId = journeyId.fold(url) { id => s"$url?journeyId=$id" }
    wsUrl(urlMaybeId)
  }

  "GET /sandbox/messages" should {
    val url = "/messages"

    "return a valid response without a journeyId" in {
      await(request(url, None).get()).status shouldBe 200
    }

    "return a valid response with a journeyId" in {
      await(request(url, Some("journeyId")).get()).status shouldBe 200
    }

    "return a valid response for ERROR-401" in {
      await(request(url, None, "ERROR-401").get()).status shouldBe 401
    }

    "return a valid response for ERROR-403" in {
      await(request(url, None, "ERROR-403").get()).status shouldBe 403
    }

    "return a valid response for ERROR-500" in {
      await(request(url, None, "ERROR-500").get()).status shouldBe 500
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, None).get()).status shouldBe 406
    }

  }

  "POST /sandbox/messages/read" should {
    val url = "/messages/read"
    val messageUrl = RenderMessageRequest("L2U5NkwzTCtUdmQvSS9VVyt0MGh6UT09")

    "return a valid response without a journeyId with empty render payload" in {
      val response: WSResponse = await(request(url, None).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
      response.body shouldEqual newTaxStatement.toString()
    }

    "return a valid response with a journeyId with empty render payload" in {
      val response = await(request(url, Some("journeyId")).withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
      response.body shouldEqual newTaxStatement.toString()
    }

    "return a 406 when request does not contain an Accept header" in {
      await(requestWithoutAcceptHeader(url, None).post(Json.toJson(messageUrl))).status shouldBe 406
    }

    "return a valid response for NEW-TAX-STATEMENT" in {
      val response: WSResponse = await(request(url, None, "NET-TAX-STATEMENT").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
      response.body shouldEqual newTaxStatement.toString()
    }

    "return a valid response for ANNUAL-TAX-SUMMARY" in {
      val response: WSResponse = await(request(url, None, "ANNUAL-TAX-SUMMARY").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
      response.body shouldEqual annualTaxSummary.toString()
    }

    "return a valid response for STOPPING-SA" in {
      val response: WSResponse = await(request(url, None, "STOPPING-SA").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
      response.body shouldEqual stoppingSA.toString()
    }

    "return a valid response for OVERDUE-PAYMENT" in {
      val response: WSResponse = await(request(url, None, "OVERDUE-PAYMENT").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl)))

      response.status shouldBe 200
      response.body shouldEqual overduePayment.toString()
    }

    "return a valid response for ERROR-401" in {
      await(request(url, None, "ERROR-401").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 401
    }

    "return a valid response for ERROR-403" in {
      await(request(url, None, "ERROR-403").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 403
    }

    "return a valid response for ERROR-500" in {
      await(request(url, None, "ERROR-500").withHeaders(("Authorization", "auth1")).post(Json.toJson(messageUrl))).status shouldBe 500
    }
  }
}