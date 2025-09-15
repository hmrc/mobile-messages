import sbt.*

private object AppDependencies {

  private val playBootstrapVersion = "10.1.0"
  private val playHmrcApiVersion = "8.3.0"
  private val domainVersion = "13.0.0"

  private val scalaMockVersion = "7.5.0"
  private val refinedVersion = "0.11.3"
  private val commonsCodecVersion = "1.19.0"

  val compile = Seq(
    "uk.gov.hmrc"  %% "bootstrap-backend-play-30" % playBootstrapVersion,
    "uk.gov.hmrc"  %% "play-hmrc-api-play-30"     % playHmrcApiVersion,
    "uk.gov.hmrc"  %% "domain-play-30"            % domainVersion,
    "eu.timepit"   %% "refined"                   % refinedVersion,
    "commons-codec" % "commons-codec"             % commonsCodecVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
          "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
          "uk.gov.hmrc" %% "domain-test-play-30" % domainVersion % scope
        )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope)
      }.test
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % playBootstrapVersion % scope,
    "uk.gov.hmrc" %% "domain-test-play-30" % domainVersion % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
