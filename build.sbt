name := "scala-aws-lambda-handler"
organization := "com.lifeway.aws"

scalaVersion := "2.12.4"

val circeVersion = "0.9.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.slf4j"     % "slf4j-api"            % "1.7.25",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.lihaoyi"   %% "utest"               % "0.6.3" % "test"
)

testFrameworks += new TestFramework("utest.runner.Framework")

addCommandAlias("testcoverage", "; reload; clean; compile; coverage; test; coverageReport; reload")