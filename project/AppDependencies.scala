import sbt._

private object AppDependencies {

  import play.core.PlayVersion

  private val play28Bootstrap     = "5.24.0"
  private val playHmrcApiVersion  = "7.0.0-play-28"
  private val domainVersion       = "8.1.0-play-28"
  private val emailAddressVersion = "3.6.0"

  private val scalaMockVersion     = "4.4.0"
  private val scalaTestVersion     = "3.0.8"
  private val wireMockVersion      = "2.21.0"
  private val pegdownVersion       = "1.6.0"
  private val scalaTestPlusVersion = "4.0.3"
  private val refinedVersion       = "0.9.4"

  val compile = Seq(
    "uk.gov.hmrc" %% "play-hmrc-api" % playHmrcApiVersion,
    "uk.gov.hmrc" %% "domain"        % domainVersion,
    "uk.gov.hmrc" %% "emailaddress"  % emailAddressVersion,
    "eu.timepit"  %% "refined"       % refinedVersion
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
