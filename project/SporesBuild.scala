package scala.spores

import sbt._
import Keys._

object SporesBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "org.scala-lang.modules",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := "2.11.2"
  )

  override lazy val settings =
    super.settings ++
    buildSettings

  lazy val defaultSettings = buildSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
  )

}
