/**
  * User: michaelk
  * Date: 24/12/2017
  * Time: 17:34
  */

import sbt.Keys.{unmanagedJars, _}
import sbt.{file, _}

object Commons {
  val jacksonVer = "2.9.2"
  val sparkVer = "2.2.0"
  val springVer = "1.5.9.RELEASE"
  val scalaVer = "2.11"
  val twitterVer = "7.1.0"
  val finatraVersion = "17.10.0"
  val kafkaVer = "10"
  val guavaVer = "11.0.2"
  val fullScalaVersion = s"$scalaVer.8"

  val settings = Seq(
    version := "1.0",
    scalaVersion := fullScalaVersion
  )

  val testSettings = Seq(
    libraryDependencies += "org.springframework.boot" % "spring-boot-starter-test" % springVer % "test",
    libraryDependencies += "junit" % "junit" % "4.12" % "test",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1"

  )

  val overrides = Seq(
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % jacksonVer,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVer,
    dependencyOverrides += "com.fasterxml.jackson.module" % s"jackson-module-scala_$scalaVer" % jacksonVer
  )

  val excludes = Seq(
    excludeDependencies += "slf4j-log4j12" % "log4j",
    excludeDependencies += "log4j" % "log4j",
    excludeDependencies += "log4j" % "apache-log4j-extras"
  )

  val externalJars = Seq(
    unmanagedJars in Compile += file("common/lib/sql-builder.jar"),
    unmanagedJars in Compile += file("common/lib/vertica-jdbc-8.1.1-7.jar"),
    unmanagedJars in Compile += file("common/lib/vertica9.0.0_spark2.1_scala2.11.jar")
  )
}