package uk.gov.hmrc.mobilemessages.mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.api.domain.Registration

object ServiceLocatorMock {

  def registrationWillSucceed(): Unit = {
    stubFor(post(urlEqualTo("/registration"))
      .willReturn(aResponse()
        .withStatus(204)))
  }

  def registrationWillFail(): Unit = {
    stubFor(post(urlEqualTo("/registration"))
      .willReturn(aResponse()
        .withStatus(500)))
  }

  private def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  private val registrationPattern: RequestPatternBuilder = postRequestedFor(urlPathEqualTo("/registration"))
    .withHeader("content-type", equalTo("application/json"))
    .withRequestBody(equalTo(regPayloadStringFor("mobile-messages", "https://mobile-messages.protected.mdtp")))

  def registerShouldHaveBeenCalled(): Unit = verify(1, registrationPattern)

  def registerShouldNotHaveBeenCalled(): Unit = verify(0, registrationPattern)
}