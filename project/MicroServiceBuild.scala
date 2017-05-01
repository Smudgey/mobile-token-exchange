import sbt._


object MicroServiceBuild extends Build with MicroService {
  import play.sbt.routes.RoutesKeys._

  val appName = "mobile-token-exchange"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val wireMockVersion = "1.57"
  private val scalaJVersion = "1.1.5"
  private val scalaTestVersion = "3.0.1"

  private val microserviceBootstrapVersion = "5.15.0"
  private val playHealthVersion = "2.1.0"
  private val playJsonLoggerVersion = "3.1.0"
  private val playConfigVersion = "4.3.0"
  private val hmrcTestVersion = "2.3.0"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "play-json-logger" % playJsonLoggerVersion,
    "uk.gov.hmrc" %% "play-reactivemongo" % "5.2.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % "test,it",
        "org.scalaj" %% "scalaj-http" % scalaJVersion % "test,it",
        "org.scalatest" %% "scalatest" % scalaTestVersion % "test,it",
        "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
        "uk.gov.hmrc" %% "reactivemongo-test" % "1.6.0" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
