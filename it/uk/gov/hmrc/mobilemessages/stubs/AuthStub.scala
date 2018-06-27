package uk.gov.hmrc.mobilemessages.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json.obj
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L200, L100}
import uk.gov.hmrc.domain.Nino

object AuthStub {
  def authRecordExists(nino: Nino = Nino("BC233445B"), confidenceLevel: ConfidenceLevel = L200): Unit = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(
      """{ "authorise": [], "retrieve": ["nino","confidenceLevel"] }""".stripMargin, true, false)).willReturn(
      aResponse().withStatus(200).withBody(obj("confidenceLevel" -> confidenceLevel.level, "nino" -> nino.nino).toString)))
  }

  def authRecordExistsWithLowCL(): Unit = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(
      """{ "authorise": [], "retrieve": ["nino","confidenceLevel"] }""".stripMargin, true, false)).willReturn(
      aResponse().withStatus(200).withBody(obj("confidenceLevel" -> L100.level).toString)))
  }

  def authRecordExistsWithoutNino(): Unit = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(200).withBody(obj("uri" -> "uri").toString())))

    stubFor(post(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(
      """{ "authorise": [], "retrieve": ["nino","confidenceLevel"] }""".stripMargin, true, false)).willReturn(
        aResponse().withStatus(200).withBody(obj("confidenceLevel" -> L200.level).toString)))
  }

  def unauthorised(): Unit = {
    stubFor(get(urlEqualTo("/auth/authority")).willReturn(aResponse().withStatus(401)))
  }
}
