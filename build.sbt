name := "spark-engine"
version := "0.0.1"
scalaVersion := "2.12.11"

val sparkVersion = "3.1.1"
libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion ,
    "com.github.scopt" %% "scopt" % "4.0.0-RC2",
    "org.scala-lang" % "scala-reflect" % "2.12.3"
)

// mark the main function for assembly so that it know where to invoke when the jar is being provided in class path
mainClass in assembly := Some("Driver.MainApp")
// We do not want to include any Scala artifacts or dependencies because the spark env already contains all those jars ahead of the time
// the aim is to reduce the size of the artifacts so that we don't save unncessary binary files in the artifact repos
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

// when we are generating the fat jar or final release, we want to make sure the naming convention is there for the assembly to follow
assemblyJarName in assembly := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
        case x => MergeStrategy.first
}
