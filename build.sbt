import sbt._
import Keys._

name := "Spores"

lazy val core = Project(
  id = "spores-core",
  base = file("core")
)

lazy val pickling = Project(
  id = "spores-pickling",
  base = file("spores-pickling")
) dependsOn(core)

lazy val root = Project(
  id = "spores",
  base = file(".")
).aggregate(core, pickling)
