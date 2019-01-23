package uk.gov.hmrc.mobilemessages.support

import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.language.postfixOps

class BaseISpec
    extends WordSpecLike
    with Matchers
    with OptionValues
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with FutureAwaits
    with DefaultAwaitTimeout
    with WireMockSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  def config: Map[String, Any] = Map(
    "auditing.enabled"                                   -> false,
    "microservice.services.service-locator.enabled"      -> false,
    "microservice.services.auth.port"                    -> wireMockPort,
    "microservice.services.citizen-details.port"         -> wireMockPort,
    "microservice.services.entity-resolver.port"         -> wireMockPort,
    "microservice.services.service-locator.port"         -> wireMockPort,
    "microservice.services.preferences.port"             -> wireMockPort,
    "microservice.services.message.host"                 -> "localhost",
    "microservice.services.message.port"                 -> wireMockPort,
    "microservice.services.sa-message-renderer.host"     -> "localhost",
    "microservice.services.sa-message-renderer.port"     -> wireMockPort,
    "microservice.services.ats-message-renderer.host"    -> "localhost",
    "microservice.services.ats-message-renderer.port"    -> wireMockPort,
    "microservice.services.secure-message-renderer.host" -> "localhost",
    "microservice.services.secure-message-renderer.port" -> wireMockPort,
    "cookie.encryption.key"                              -> "gvBoGdgzqG1AarzF1LY0zQ=="
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
