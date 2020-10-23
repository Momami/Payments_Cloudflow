import sbt._
import sbt.Keys._

lazy val root =
  Project(id = "root", base = file("."))
    .enablePlugins(ScalafmtPlugin)
    .settings(
      name := "root",
      scalafmtOnCompile := true,
      skip in publish := true,
    )
    .withId("root")
    .settings(commonSettings)
    .aggregate(
      myPaymentsPipeline,
      datamodel,
      akkaStreamlets
    )

def appModule(moduleID: String): Project = {
  Project(id = moduleID, base = file(moduleID))
    .settings(
      name := moduleID
    )
    .withId(moduleID)
    .settings(commonSettings)
}

lazy val akkaStreamlets = appModule("akka-streamlets")
  .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.lightbend.akka"        %% "akka-stream-alpakka-file"  % "1.1.2",
      "com.typesafe.akka"         %% "akka-http-spray-json"   % "10.1.12",
      "ch.qos.logback"            %  "logback-classic"        % "1.2.3",
      "org.scalatest"             %% "scalatest"              % "3.0.8"    % "test"
    )
  )
  .dependsOn(datamodel)

lazy val flinkStreamlets = appModule("flink-streamlets")
  .enablePlugins(CloudflowApplicationPlugin, CloudflowFlinkPlugin)
  .settings(commonSettings)
  .dependsOn(datamodel)

lazy val myPaymentsPipeline = appModule("my-payments-pipeline")
  .enablePlugins(CloudflowApplicationPlugin)
  .settings(commonSettings)

lazy val datamodel = (project in file("./datamodel"))
  .enablePlugins(CloudflowLibraryPlugin)

lazy val paymentData = (project in file("."))
  .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin)
  .settings(
    runLocalConfigFile := Some("src/main/resources/local.conf"),
    name := "PaymentsCloudflow",
    scalaVersion := "2.12.11",
    version := "0.1",
  )

lazy val commonSettings = Seq(
  organization := "com.lightbend.cloudflow",
  headerLicense := Some(HeaderLicense.ALv2("(C) 2016-2020", "Lightbend Inc. <https://www.lightbend.com>")),
  scalaVersion := "2.12.11",
  javacOptions += "-Xlint:deprecation",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls",
    "-Xlint",
    "-Ywarn-unused",
    "-Ywarn-unused-import",
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked"
  ),

  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

)

dynverSeparator in ThisBuild := "-"