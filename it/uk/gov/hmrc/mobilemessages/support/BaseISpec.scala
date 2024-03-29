package uk.gov.hmrc.mobilemessages.support

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

class BaseISpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with FutureAwaits
    with DefaultAwaitTimeout
    with WireMockSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"
  protected val journeyId = "87144372-6bda-4cc9-87db-1d52fd96498f"

  def config: Map[String, Any] = Map(
    "auditing.enabled"                                   -> false,
    "microservice.services.auth.port"                    -> wireMockPort,
    "microservice.services.citizen-details.port"         -> wireMockPort,
    "microservice.services.entity-resolver.port"         -> wireMockPort,
    "microservice.services.secure-message.host"          -> "localhost",
    "microservice.services.secure-message.port"          -> wireMockPort,
    "microservice.services.sa-message-renderer.host"     -> "localhost",
    "microservice.services.sa-message-renderer.port"     -> wireMockPort,
    "microservice.services.ats-message-renderer.host"    -> "localhost",
    "microservice.services.ats-message-renderer.port"    -> wireMockPort,
    "microservice.services.secure-message-renderer.host" -> "localhost",
    "microservice.services.secure-message-renderer.port" -> wireMockPort,
    "cookie.encryption.key"                              -> "gvBoGdgzqG1AarzF1LY0zQ==",
    "microservice.services.mobile-shuttering.port"       -> wireMockPort
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
