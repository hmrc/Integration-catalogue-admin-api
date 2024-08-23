import sbt._

object AppDependencies {

  lazy val enumeratumVersion = "1.8.0"
  lazy val bootstrapVersion = "9.3.0"
  lazy val jacksonVersion = "2.17.1"

  val compile = Seq(
    "uk.gov.hmrc"                     %% "bootstrap-backend-play-30" % bootstrapVersion,
    "com.beachape"                    %% "enumeratum-play-json"       % enumeratumVersion,
    "com.fasterxml.jackson.core"       % "jackson-core"               % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-annotations"        % jacksonVersion,
    "com.fasterxml.jackson.core"       % "jackson-databind"           % jacksonVersion,
    "com.fasterxml.jackson.module"     %% "jackson-module-scala"      % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml"    % jacksonVersion,
    "org.typelevel"                   %% "cats-core"                  % "2.10.0",
    "io.circe"                        %% "circe-yaml"                 % "1.15.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % bootstrapVersion % Test,
    "org.pegdown"             % "pegdown"                  % "1.6.0"          % Test,
    "org.jsoup"               % "jsoup"                    % "1.17.2"         % Test,
    "org.scalatestplus"      %% "mockito-4-11"             % "3.2.17.0"       % Test,
  )

  val it = Seq.empty

}
