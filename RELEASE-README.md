## Release steps
Builds occur on Travis, but SNAPSHOTS are not automatically published.

To publish a snapshot, run `sbt publishSigned` against a clean master.

Production releases are managed with the sbt release plugin. Run a `sbt release` command to release the build and queue it for Sonotype to push to maven central.

*Note: In the above steps, you will need a PGP key for an authorized user against the `com.lifeway` groupId in Sonotype.*
