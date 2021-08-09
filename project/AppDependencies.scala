import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val enumeratumVersion = "1.6.3"
  lazy val bootstrapVersion = "5.3.0"
  lazy val jacksonVersion = "2.12.2"

  val compile = Seq(
    "uk.gov.hmrc"                     %% "bootstrap-backend-play-27" % bootstrapVersion,
    "com.beachape"                    %% "enumeratum-play-json"       % enumeratumVersion,
    "com.fasterxml.jackson.core"       % "jackson-core"               % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-annotations"        % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-databind"           % jacksonVersion,
    "com.fasterxml.jackson.module"     %% "jackson-module-scala"           % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml"    % jacksonVersion,
    "com.typesafe.play"               %% "play-json-joda"             % "2.9.0",
    "org.typelevel"                   %% "cats-core"                  % "2.4.2",
    "io.circe"                        %% "circe-yaml"                 % "0.12.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-27"   % bootstrapVersion % Test,
    "org.pegdown"             % "pegdown"                  % "1.6.0"  % "test, it",
    "org.jsoup"               % "jsoup"                    % "1.13.1" % Test,
    "com.typesafe.play"      %% "play-test"                % current  % Test,
    "com.vladsch.flexmark"    % "flexmark-all"             % "0.36.8" % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"       % "4.0.3"  % "test, it",
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.14.4" % "test, it",
    "com.github.tomakehurst"  % "wiremock"                 % "2.25.1" % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8-standalone" % "2.27.1" % "test, it"
  )
}
