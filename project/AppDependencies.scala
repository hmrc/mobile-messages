import sbt._

private object AppDependencies {

  private val playBootstrapVersion = "9.11.0"
  private val playHmrcApiVersion   = "8.0.0"
  private val domainVersion        = "10.0.0"
  private val emailAddressVersion  = "4.1.0"

  private val scalaMockVersion    = "5.2.0"
  private val refinedVersion      = "0.11.3"
  private val commonsCodecVersion = "1.16.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % playBootstrapVersion,
    "uk.gov.hmrc"   %% "play-hmrc-api-play-30"     % playHmrcApiVersion,
    "uk.gov.hmrc"   %% "domain-play-30"            % domainVersion,
    "eu.timepit"    %% "refined"                   % refinedVersion,
    "commons-codec" % "commons-codec"              % commonsCodecVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock" % scalaMockVersion % scope
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
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % playBootstrapVersion % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
