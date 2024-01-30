import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  lazy val enumeratumVersion = "1.8.0"
  lazy val bootstrapVersion = "8.4.0"
  lazy val jacksonVersion = "2.16.1"

  val compile = Seq(
    "uk.gov.hmrc"                     %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.beachape"                    %% "enumeratum-play-json"       % enumeratumVersion,
    "com.fasterxml.jackson.core"       % "jackson-core"               % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-annotations"        % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-databind"           % jacksonVersion,
    "com.fasterxml.jackson.module"     %% "jackson-module-scala"      % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml"    % jacksonVersion,
    "com.typesafe.play"               %% "play-json-joda"             % "2.10.4",
    "org.typelevel"                   %% "cats-core"                  % "2.10.0",
    "io.circe"                        %% "circe-yaml"                 % "1.15.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % bootstrapVersion % Test,
    "org.pegdown"             % "pegdown"                  % "1.6.0"  % Test,
    "org.jsoup"               % "jsoup"                    % "1.17.2" % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.30" % Test
  )

  val it = Seq.empty

}
