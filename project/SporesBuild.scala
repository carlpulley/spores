package scala.spores

import sbt._
import Keys._

object SporesBuild extends Build {

  lazy val buildSettings = Seq(
    organization := "org.scala-lang.modules",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := "2.10.4",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  )

  override lazy val settings =
    super.settings ++
    buildSettings

  lazy val defaultSettings = buildSettings ++ Seq(
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
  )

}
