val scala3Version = "3.6.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Career Anthology",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "com.openai" % "openai-java-client-okhttp" % "1.3.0",
      "dev.zio" %% "zio" % "2.1.17",
      "dev.zio" %% "zio-json" % "0.7.42"
    )
  )
