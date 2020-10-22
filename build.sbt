
lazy val paymentData =  (project in file("."))
  .enablePlugins(CloudflowApplicationPlugin, CloudflowAkkaPlugin)
  .settings(
    runLocalConfigFile := Some("src/main/resources/local.conf"),
    name := "PaymentsCloudflow",
    scalaVersion := "2.12.11",
    version := "0.1",

    libraryDependencies ++= Seq(
      "com.lightbend.akka"     %% "akka-stream-alpakka-file"  % "1.1.2",
      "ch.qos.logback"         %  "logback-classic"           % "1.2.3"
    )
  )