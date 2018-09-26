package uk.gov.hmrc.mobilemessages.mocks

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json.obj
import uk.gov.hmrc.auth.core.AuthenticateHeaderParser.{ENROLMENT, WWW_AUTHENTICATE}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L100, L200}
import uk.gov.hmrc.domain.Nino

object AuthMock {
  private val accountsRequestJson: String = """{ "authorise": [], "retrieve": ["nino","confidenceLevel"] }""".stripMargin
  private val authorityUrl = "/auth/authority"
  private val authUrl = "/auth/authorise"

  def authRecordExists(nino: Nino = Nino("BC233445B"), confidenceLevel: ConfidenceLevel = L200): Unit = {
    stubFor(get(urlEqualTo(authorityUrl)).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo(authUrl)).withRequestBody(equalToJson(
      accountsRequestJson, true, false)).willReturn(
      aResponse().withStatus(200).withBody(obj("confidenceLevel" -> confidenceLevel.level, "nino" -> nino.nino).toString)))
  }

  def authRecordExistsWithLowCL(): Unit = {
    stubFor(get(urlEqualTo(authorityUrl)).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo(authUrl)).withRequestBody(equalToJson(
      accountsRequestJson, true, false)).willReturn(
      aResponse().withStatus(200).withBody(obj("confidenceLevel" -> L100.level).toString)))
  }

  def authRecordExistsWithoutNino(): Unit = {
    stubFor(get(urlEqualTo(authorityUrl)).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo(authUrl)).withRequestBody(equalToJson(
      accountsRequestJson, true, false)).willReturn(
        aResponse().withStatus(200).withBody(obj("confidenceLevel" -> L200.level).toString)))
  }

  def authFailure(): Unit = {
    stubFor(get(urlEqualTo(authorityUrl)).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo(authUrl)).withRequestBody(equalToJson(
      accountsRequestJson, true, false)).willReturn(
      aResponse().withStatus(401).withHeader(
        WWW_AUTHENTICATE,"""MDTP detail="BearerTokenExpired"""").withHeader(ENROLMENT, "")))
  }

  def unauthorised(): Unit = {
    stubFor(get(urlEqualTo(authorityUrl)).willReturn(aResponse().withStatus(401)))
  }
}
