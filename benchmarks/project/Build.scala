import sbt.Keys._
import sbt._
import Keys._
import io.gatling.sbt.GatlingPlugin

object ReactiveDataFederationPoCBenchmarksBuild extends Build {
  lazy val benchmarksProject = Project(
    id = "benchmarks",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
      organization := "com.github.akovari",
      version := "0.1",
      scalaVersion := "2.11.7",
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      resolvers ++= Seq(
        "Spray repo" at "http://repo.spray.io/",
        Resolver.sonatypeRepo("public"),
        "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
        "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      ),
      libraryDependencies ++= {
        val scalaTestV = "2.2.1"
        val gatlingV = "2.1.7"
        val sprayV = "1.3.1"
        Seq(
          "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV,
          "io.spray" % "spray-can" % sprayV,
          "io.spray" % "spray-caching" % sprayV,
          "io.spray" % "spray-routing" % sprayV,
          "io.spray" % "spray-client" % sprayV,
          "org.scalatest" %% "scalatest" % scalaTestV % "test",
          "io.gatling" % "gatling-test-framework" % gatlingV % "test"
        )
      }
    )
  ).enablePlugins(GatlingPlugin)
}
