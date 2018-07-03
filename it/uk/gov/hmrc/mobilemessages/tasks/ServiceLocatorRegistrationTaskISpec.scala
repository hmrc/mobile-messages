package uk.gov.hmrc.mobilemessages.tasks

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlMatching, verify}
import uk.gov.hmrc.mobilemessages.stubs.ServiceLocatorStub._
import uk.gov.hmrc.mobilemessages.support.{BaseISpec, WiremockServiceLocatorSugar}

class ServiceLocatorRegistrationTaskISpec extends BaseISpec with WiremockServiceLocatorSugar {
  "ServiceLocatorRegistrationTask" should {
    val task = app.injector.instanceOf[ServiceLocatorRegistrationTask]

    "register with the api platform" in {
      registrationWillSucceed()
      await(task.register) shouldBe true
      verify(1,
        postRequestedFor(urlMatching("/registration")).withHeader("content-type", equalTo("application/json")).
          withRequestBody(equalTo(regPayloadStringFor("mobile-messages", "https://mobile-messages.protected.mdtp")))
      )
    }

    "handle errors" in {
      registrationWillFail()
      await(task.register) shouldBe false
    }
  }
}