package uk.gov.hmrc.mobilemessages.mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json.obj
import uk.gov.hmrc.auth.core.AuthenticateHeaderParser.{ENROLMENT, WWW_AUTHENTICATE}
import uk.gov.hmrc.domain.Nino

object AuthMock {
  private val accountsRequestJson: String = """{ "authorise": [ { "confidenceLevel" : 200 } ], "retrieve": ["nino","internalId"] }""".stripMargin
  private val authUrl      = "/auth/authorise"

  def authRecordExists(nino: Nino = Nino("BC233445B"), internalId: String = "userId123"): Unit =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(aResponse().withStatus(200).withBody(obj("internalId" -> internalId, "nino" -> nino.nino).toString)))

  def authRecordExistsWithLowCL(): Unit =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(aResponse().withStatus(200).withBody(obj().toString)))

  def authRecordExistsWithoutNino(): Unit =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(aResponse().withStatus(200).withBody(obj("internalId" -> "userId123").toString)))

  def authFailure(): Unit =
    stubFor(
      post(urlEqualTo(authUrl))
        .withRequestBody(equalToJson(accountsRequestJson, true, false))
        .willReturn(aResponse().withStatus(401).withHeader(WWW_AUTHENTICATE, """MDTP detail="BearerTokenExpired"""").withHeader(ENROLMENT, "")))
}
