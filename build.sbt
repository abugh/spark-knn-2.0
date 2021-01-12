import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbtassembly.AssemblyPlugin.autoImport._
import _root_.sbt.complete.DefaultParsers.spaceDelimited

import scala.sys.process.{Process, ProcessLogger}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF")             => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf/.*\\.sf$") => MergeStrategy.discard
  case "reference.conf"                                => MergeStrategy.concat
  case "application.conf"                              => MergeStrategy.concat
  case x                                               => MergeStrategy.first
}

name := "spark-nearest-neighbor"
fork in Test := true
javaOptions in Test ++= Seq(
  "-Xmx2048m",
  "-XX:ReservedCodeCacheSize=1024m",
  "-XX:MaxPermSize=2048m"
)

scalaVersion := "2.12.12"
logLevel in assembly := sbt.Level.Info

resolvers ++= Seq(
  "Spark Package Main Repo" at "https://dl.bintray.com/spark-packages/maven",
  Resolver.jcenterRepo
)

val sparkVersion = "3.0.0"
val configFiles = Seq("application.conf")
val sparkHome = sys.env.getOrElse("SPARK_HOME", "/opt/spark")

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
    excludeAll ExclusionRule(organization = "org.apache.hadoop"),
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-hive" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
  "commons-logging" % "commons-logging" % "1.2",
  "org.slf4j" % "slf4j-log4j12" % "1.7.25",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "com.holdenkarau" %% "spark-testing-base" % "2.4.5_0.14.0" % "test",
  "org.scala-lang" % "scala-reflect" % "2.12.8" % "provided",
)

unmanagedJars in Compile ++= (file("src/main/resources/") ** "*.jar").classpath
unmanagedJars in Test ++= (unmanagedJars in Compile).value
unmanagedResources in Compile ++= Seq(
  baseDirectory.value / "../application.conf"
)

assemblyOption in assembly := (assemblyOption in assembly).value
  .copy(includeScala = false, includeDependency = true)
test in assembly := {} // disable tests for sbt-assembly so they don't trigger twice during sbt-release
// show duration of each test case
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
//assemblyExcludedJars in assembly := (fullClasspath in assembly).value.filter(_.data.getName == "RedshiftJDBC.jar")

assemblyJarName in assembly := s"${name.value}-assembly-${dynver.value}.jar"

releaseIgnoreUntrackedFiles := true
