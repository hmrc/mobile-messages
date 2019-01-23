package uk.gov.hmrc.mobilemessages.tasks

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlMatching, verify}
import play.api.libs.json.Json
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.mobilemessages.mocks.ServiceLocatorMock._
import uk.gov.hmrc.mobilemessages.support.BaseISpec

class ServiceLocatorRegistrationTaskISpec extends BaseISpec {
  def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    Json.toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  "ServiceLocatorRegistrationTask" should {
    val task = app.injector.instanceOf[ServiceLocatorRegistrationTask]

    "register with the api platform" in {
      registrationWillSucceed()
      await(task.register) shouldBe true
      verify(
        1,
        postRequestedFor(urlMatching("/registration"))
          .withHeader("content-type", equalTo("application/json"))
          .withRequestBody(equalTo(regPayloadStringFor("mobile-messages", "https://mobile-messages.protected.mdtp")))
      )
    }

    "handle errors" in {
      registrationWillFail()
      await(task.register) shouldBe false
    }
  }
}
