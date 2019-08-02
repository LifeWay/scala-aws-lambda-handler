name := "scala-lambda-handler"
organization := "com.lifeway.aws"

scalaVersion := "2.12.8"

val circeVersion = "0.11.1"

libraryDependencies ++= Seq(
  "io.circe"      %% "circe-core"          % circeVersion,
  "io.circe"      %% "circe-parser"        % circeVersion,
  "org.slf4j"     % "slf4j-api"            % "1.7.26",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.lihaoyi"   %% "requests"            % "0.2.0",
  "com.lihaoyi"   %% "utest"               % "0.6.3" % "test"
)

testFrameworks += new TestFramework("utest.runner.Framework")

addCommandAlias("testcoverage", "; reload; clean; compile; coverage; test; coverageReport; reload")
