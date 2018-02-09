name := "scala-aws-lambda-handler"
organization := "com.lifeway.aws"


scalaVersion := "2.12.4"

val circeVersion = "0.9.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "io.symphonia" % "lambda-logging" % "1.0.1" % "provided",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0"
)
