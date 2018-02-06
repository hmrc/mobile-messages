package utils

import org.scalatest.{Matchers, OptionValues, WordSpec}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps


class BaseISpec  extends WordSpec with Matchers with OptionValues with WsScalaTestClient with GuiceOneServerPerSuite with WireMockSupport {
  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "auditing.enabled" -> false,
        "microservice.services.service-locator.enabled" -> false,
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.citizen-details.port" -> wireMockPort,
        "microservice.services.entity-resolver.port" -> wireMockPort,
        "microservice.services.service-locator.port" -> wireMockPort,
        "microservice.services.preferences.port" -> wireMockPort
      )

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  implicit val defaultTimeout: FiniteDuration = 5 seconds

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)
}