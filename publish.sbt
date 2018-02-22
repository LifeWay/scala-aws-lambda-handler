publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := {
  <scm>
    <url>git@github.com:LifeWay/scala-aws-lambda-handler.git</url>
    <connection>scm:git:git@github.com:LifeWay/scala-aws-lambda-handler.git</connection>
  </scm>
    <developers>
      <developer>
        <id>lifeway</id>
        <name>LifeWay Christian Resources</name>
        <url>https://www.lifeway.com</url>
      </developer>
    </developers>
}
pomIncludeRepository := { _ => false }
homepage := Some(url(s"https://github.com/LifeWay/scala-aws-lambda-handler"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

sonatypeProfileName := "com.lifeway"

releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseTagName := s"${(version in ThisBuild).value}"
releaseCrossBuild := true

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
