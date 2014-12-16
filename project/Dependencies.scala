package scala.spores

import sbt._

object Dependencies {

  val scalaVersion = "2.10.4"

  val scalaReflect = "org.scala-lang" % "scala-reflect" % scalaVersion % "provided"
  val scalaCompiler = "org.scala-lang" % "scala-compiler" % scalaVersion % "test"

  val junit = "junit" % "junit" % "4.10" % "test"
  val junitIntf = "com.novocode" % "junit-interface" % "0.8" % "test"

  val scalaPickling = "org.scala-lang" %% "scala-pickling" % "0.9.1"

  val quasiquotes = "org.scalamacros" %% "quasiquotes" % "2.0.1"

  val core = Seq(scalaReflect, scalaCompiler, quasiquotes, junit, junitIntf)

  val pickling = core ++ Seq(scalaPickling)

}
