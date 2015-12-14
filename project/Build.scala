import sbt.Keys._
import sbt._

import spray.revolver.RevolverPlugin.Revolver
import com.typesafe.sbt.SbtAspectj._


object UnifiedDataServicesBuild extends Build {
  private val allResolvers = Seq(
    "Local Maven Repository" at "file:///" + Path.userHome + "/.m2/repository",
    "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases",
    "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    "theatr.us" at "http://repo.theatr.us",
    "JBoss Releases" at "https://repository.jboss.org/nexus/content/repositories/releases",
    "JBoss Snapshots" at "https://repository.jboss.org/nexus/content/repositories/snapshots",
    "JBoss Public" at "http://repository.jboss.org/nexus/content/groups/public/",
    "JBoss Developer" at "http://repository.jboss.org/nexus/content/groups/developer/",
    "JBoss Early Access" at "http://maven.repository.redhat.com/earlyaccess/all/",
    "Red Hat Tech Preview" at "http://maven.repository.redhat.com/techpreview/all/",
    Resolver.sonatypeRepo("public"),
    "bintray-flossware-maven" at "http://dl.bintray.com/flossware/maven",
    "bintray-solenopsis-maven" at "http://dl.bintray.com/solenopsis/maven",
    "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven"
  )

  val scalaV = "2.11.7"
  val akkaV = "2.4.0"
  val akkaHttpV = "2.0-M2"
  val jsr181V = "1.0-MR1"
  val javaxActivationV = "1.1-rev-1"
  val javaxValidationV = "1.1.0.Final"
  val javaxPersistenceV = "1.0.2"
  val json4sV = "3.2.11"
  val jacksonModuleV = "2.4.3"
  val jodaTimeV = "2.7"
  val jodaConvertV = "1.7"
  val scalamockV = "3.2.1"
  val scalatestV = "2.2.1"
  //  val slickJodaMapper = "1.2.0"
  val logbackV = "1.1.3"
  val slf4jV = "1.7.12"
  val janinoV = "2.7.8"
  val scalaLoggingV = "2.1.2"
  val scalaZV = "7.0.6"
  val resteasyV = "3.0.11.Final"
  val jaxrsV = "2.0"
  val parboiledV = "1.1.6"
  val jooqV = "3.7.0"
  val postgresV = "9.4-1205-jdbc41"
  val aspectjV = "1.8.5"
  val asyncV = "0.9.2"
  val hikariCPV = "2.3.6"
  val slickV = "2.1.0"
  val lasiusV = "3.0.5"
  val shapelessV = "2.2.3"
  val jsonRpcV = "1.1"
  val typesafeSalesforceV = "1.0-SNAPSHOT"
  val saajV = "1.3.25"
  val groovyV = "2.4.3"
  val akkaJson4sV = "1.3.0"

  lazy val mainProject = Project(
    id = "main",
    base = file("."),
    settings = Defaults.coreDefaultSettings ++ Revolver.settings ++ aspectjSettings ++ Seq(
      organization := "com.github.akovari",
      name := "Reactive-Data-Federation-Platform-PoC",
      //      javaOptions <++= AspectjKeys.weaverOptions in Aspectj,
      javaOptions ++= Seq("-Xmx8G" /*, "-Djavax.net.debug=all"*/),
      //      fork := true,
      version := "0.1",
      scalaVersion := scalaV,
      javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8"),
      resolvers ++= allResolvers,
      scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions",
        "-language:existentials", "-language:postfixOps", "-language:reflectiveCalls", "-Xfatal-warnings", "-Xlint"),

      libraryDependencies ++= Seq(
        "com.github.akovari" % "ws" % typesafeSalesforceV,
        "com.github.akovari" % "query" % typesafeSalesforceV,
        "com.github.akovari" % "util" % typesafeSalesforceV,

        "org.scala-lang" % "scalap" % scalaV,
        "org.scala-lang" % "scala-reflect" % scalaV,
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe.akka" %% "akka-slf4j" % akkaV,
        "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-core-experimental" % akkaHttpV,
        "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpV,
        "de.heikoseeberger" %% "akka-http-json4s" % akkaJson4sV,

        "javax.jws" % "jsr181-api" % jsr181V,
        "javax.ws.rs" % "javax.ws.rs-api" % jaxrsV,
        "javax.activation" % "activation" % javaxActivationV,
        "javax.validation" % "validation-api" % javaxValidationV,
        "javax.persistence" % "persistence-api" % javaxPersistenceV,
        "org.json4s" %% "json4s-jackson" % json4sV,
        "org.json4s" %% "json4s-ext" % json4sV,
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonModuleV,
        "joda-time" % "joda-time" % jodaTimeV,
        "org.joda" % "joda-convert" % jodaConvertV,
        "ch.qos.logback" % "logback-classic" % logbackV,
        "ch.qos.logback" % "logback-core" % logbackV,
        "org.slf4j" % "log4j-over-slf4j" % slf4jV,
        "org.slf4j" % "jcl-over-slf4j" % slf4jV,
        "org.codehaus.janino" % "janino" % janinoV,
        "com.typesafe.scala-logging" %% "scala-logging-slf4j" % scalaLoggingV,
        "org.scalaz" %% "scalaz-core" % scalaZV,
        "org.parboiled" %% "parboiled-scala" % parboiledV,
        "org.jooq" % "jooq-scala" % jooqV,
        "org.jooq" % "jooq" % jooqV,
        "org.jooq" % "jooq-meta" % jooqV,
        "org.jooq" % "jooq-codegen" % jooqV,
        "com.zaxxer" % "HikariCP" % hikariCPV,
        "org.scala-lang.modules" %% "scala-async" % asyncV,
        "com.chuusai" %% "shapeless" % shapelessV,
        "com.sun.xml.messaging.saaj" % "saaj-impl" % saajV,
        "org.codehaus.groovy" % "groovy" % groovyV,

        "org.postgresql" % "postgresql" % postgresV,

        "org.solenopsis.lasius" % "wsutils-common" % lasiusV,
        "org.solenopsis.lasius" % "common" % lasiusV,

        "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
        "org.scalatest" %% "scalatest" % scalatestV % "test",
        "org.scalamock" %% "scalamock-scalatest-support" % scalamockV % "test"
      ).map(_.exclude("commons-logging", "commons-logging")),
      unmanagedClasspath in Compile += baseDirectory.value / "jooq",
      //      slicRdfp <<= slickCodeGenTaskRdfp,
      jooqCodegenRdfp <<= jooqCodegenRdfpTask
    )
  )

  // code generation tasks
  //  lazy val slicRdfp = TaskKey[Seq[File]]("gen-tables-rdfp")
  //  lazy val slickCodeGenTaskRdfp = (sourceDirectory, dependencyClasspath in Compile, runner in Compile, streams) map {
  //    (dir, cp, r, s) =>
  //      val outputDir = (dir / "main" / "scala").getPath // place generated files in sbt's managed sources folder
  //    val url = "jdbc:postgresql://localhost:5432/rdfp"
  //      val jdbcDriver = "org.postgresql.Driver"
  //      val slickDriver = "scala.slick.driver.PostgresDriver"
  //      val pkg = "com.github.akovari.rdfp.data"
  //      val user = "rdfp"
  //      val password = "rdfp"
  //      toError(r.run("scala.slick.codegen.SourceCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, url, outputDir, pkg, user, password), s.log))
  //      val fname = outputDir + "/com/github/akovari/rdfp/data/Tables.scala"
  //      Seq(file(fname))
  //  }
  lazy val jooqCodegenRdfp = TaskKey[Seq[File]]("gen-tables-jooq-rdfp")
  lazy val jooqCodegenRdfpTask = (sourceDirectory, fullClasspath in Compile, runner in Compile, streams) map {
    (dir, cp, r, s) =>
      val outputDir = (dir / "main" / "java").getPath // place generated files in sbt's managed sources folder
      toError(r.run("org.jooq.util.GenerationTool", cp.files, Seq("jooq-config-rdfp.xml"), s.log))
      val fname = outputDir + "/com/github/akovari/rdfp/data/schema"
      Seq(file(fname))
  }
  //  val sharedSlickSettings = Defaults.coreDefaultSettings ++ Seq(
  //    scalaVersion := scalaV,
  //    javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8"),
  //    resolvers ++= allResolvers,
  //    libraryDependencies ++= Seq(
  //      "com.typesafe.slick" %% "slick" % slickV,
  //      "com.typesafe.slick" %% "slick-codegen" % slickV,
  //      "org.postgresql" % "postgresql" % postgresV,
  //      "org.scala-lang" % "scala-reflect" % scalaV
  //    )
  //  )
}
