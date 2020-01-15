package uk.gov.hmrc.mobilemessages.mocks

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object ShutteringMock {

  def stubForShutteringDisabled: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/mobile-messages/shuttered-status?journeyId=87144372-6bda-4cc9-87db-1d52fd96498f"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": false,
                       |  "title":     "",
                       |  "message":    ""
                       |}
          """.stripMargin)
      )
    )

  def stubForShutteringEnabled: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/mobile-messages/shuttered-status?journeyId=87144372-6bda-4cc9-87db-1d52fd96498f"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": true,
                       |  "title":     "Shuttered",
                       |  "message":   "Messages are currently not available"
                       |}
          """.stripMargin)
      )
    )


}
