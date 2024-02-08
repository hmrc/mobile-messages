import sbt._

private object AppDependencies {

  import play.core.PlayVersion

  private val play28Bootstrap     = "7.19.0"
  private val playHmrcApiVersion  = "7.2.0-play-28"
  private val domainVersion       = "8.1.0-play-28"
  private val emailAddressVersion = "4.0.0"

  private val scalaMockVersion     = "5.1.0"
  private val scalaTestVersion     = "3.2.9"
  private val wireMockVersion      = "2.21.0"
  private val pegdownVersion       = "1.6.0"
  private val scalaTestPlusVersion = "5.1.0"
  private val refinedVersion        = "0.9.26"
  private val commonsCodecVersion  = "1.16.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % play28Bootstrap,
    "uk.gov.hmrc" %% "play-hmrc-api"             % playHmrcApiVersion,
    "uk.gov.hmrc" %% "domain"                    % domainVersion,
    "uk.gov.hmrc" %% "emailaddress-play-28"      % emailAddressVersion,
    "eu.timepit"  %% "refined"                    % refinedVersion,
    "commons-codec" % "commons-codec"            % commonsCodecVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
            "org.scalatest" %% "scalatest" % scalaTestVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
          )
      }.test
  }

  private def testCommon(scope: String) = Seq(
    "org.pegdown"            % "pegdown"                 % pegdownVersion       % scope,
    "com.typesafe.play"      %% "play-test"              % PlayVersion.current  % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"     % scalaTestPlusVersion % scope,
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % play28Bootstrap      % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()

}
