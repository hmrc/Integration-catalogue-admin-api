import uk.gov.hmrc.DefaultBuildSettings

val appName = "integration-catalogue-admin-api"

//ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

inThisBuild(
  List(
    majorVersion := 0,
    scalaVersion := "2.13.12"
//    semanticdbEnabled := true,
//    semanticdbVersion := scalafixSemanticdb.revision
  )
)

//val silencerVersion = "1.17.13"
//
//val jettyVersion = "9.2.24.v20180105"
//
//val jettyOverrides = Seq(
//  "org.eclipse.jetty" % "jetty-server" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-servlet" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-security" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-servlets" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-xml" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-client" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-http" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-io" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty" % "jetty-util" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty.websocket" % "websocket-api" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty.websocket" % "websocket-common" % jettyVersion % IntegrationTest,
//  "org.eclipse.jetty.websocket" % "websocket-client" % jettyVersion % IntegrationTest
//)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .settings(
    routesImport                     += "uk.gov.hmrc.integrationcatalogueadmin.controllers.binders._",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    Test / unmanagedSourceDirectories += baseDirectory(_ / "test-common").value
//    dependencyOverrides ++= jettyOverrides
  )
  .settings(scoverageSettings)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(scalacOptions ++= Seq("-deprecation", "-feature"))
  .settings(scalacOptions += "-Wconf:src=routes/.*:s")

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := ";.*\\.domain\\.models\\..*;uk\\.gov\\.hmrc\\.BuildInfo;.*\\.Routes;.*\\.RoutesPrefix;;Module;GraphiteStartUp;.*\\.Reverse[^.]*",
    ScoverageKeys.coverageMinimumStmtTotal := 96,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution  := false
  )
}

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
