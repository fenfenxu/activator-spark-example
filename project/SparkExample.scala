
import sbt._
import sbt.Classpaths.publishTask
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
// For Sonatype publishing
//import com.jsuereth.pgp.sbtplugin.PgpKeys._

object SparExamplekBuild extends Build {

  // HBase version; set as appropriate.
  val HBASE_VERSION = "0.94.6"

  // Target JVM version
  val SCALAC_JVM_VERSION = "jvm-1.5"
  val JAVAC_JVM_VERSION = "1.5"

  lazy val examples = Project("spark-examples", file("."), settings = examplesSettings)

  
  def sharedSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.apache.spark",
    version := "0.0.1",
    scalaVersion := "2.9.3",
    scalacOptions := Seq("-unchecked", "-optimize", "-deprecation", 
      "-target:" + SCALAC_JVM_VERSION),
    javacOptions := Seq("-target", JAVAC_JVM_VERSION, "-source", JAVAC_JVM_VERSION),
    unmanagedJars in Compile <<= baseDirectory map { base => (base / "lib" ** "*.jar").classpath },
    retrieveManaged := true,
    retrievePattern := "[type]s/[artifact](-[revision])(-[classifier]).[ext]",
    transitiveClassifiers in Scope.GlobalScope := Seq("sources"),
 
    // Fork new JVMs for tests and set Java options for those
    fork := true,
    javaOptions += "-Xmx3g",

    // Only allow one test at a time, even across projects, since they run in the same JVM
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),

    // Shared between both core and streaming.
    resolvers ++= Seq("Akka Repository" at "http://repo.akka.io/releases/"),

    // For Sonatype publishing
    resolvers ++= Seq("sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "sonatype-staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"),

    publishMavenStyle := true,

    libraryDependencies ++= Seq(

      // "org.eclipse.jetty" % "jetty-server" % "7.6.8.v20121106",
      "org.scalatest" %% "scalatest" % "1.9.1" % "test",
      "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
      "com.novocode" % "junit-interface" % "0.9" % "test",
      "org.easymock" % "easymock" % "3.1" % "test"
    )
    
   
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings

  val slf4jVersion = "1.7.2"

  // val excludeJackson = ExclusionRule(organization = "org.codehaus.jackson")
  val excludeNetty = ExclusionRule(organization = "org.jboss.netty")
  val excludeAsm = ExclusionRule(organization = "asm")
  val excludeSnappy = ExclusionRule(organization = "org.xerial.snappy")

  def examplesSettings = sharedSettings ++ Seq(
    
    libraryDependencies ++= Seq(

      "org.apache.spark" % "spark-core_2.9.3" % "0.8.0-incubating",
      "org.apache.spark" % "spark-streaming_2.9.3" % "0.8.0-incubating",
      "org.apache.spark" % "spark-bagel_2.9.3" % "0.8.0-incubating",
      "org.apache.spark" % "spark-mllib_2.9.3" % "0.8.0-incubating",
      
      "org.apache.kafka" % "kafka_2.9.2" % "0.8.0-beta1"
        exclude("com.sun.jmx", "jmxri")
        exclude("com.sun.jdmk", "jmxtools"),
    

      "com.twitter" % "algebird-core_2.9.2" % "0.1.11",

      "org.apache.hbase" % "hbase" % HBASE_VERSION excludeAll(excludeNetty, excludeAsm),

      "org.apache.cassandra" % "cassandra-all" % "1.2.5"
        exclude("com.google.guava", "guava")
        exclude("com.googlecode.concurrentlinkedhashmap", "concurrentlinkedhashmap-lru")
        exclude("com.ning","compress-lzf")
        exclude("io.netty", "netty")
        exclude("jline","jline")
        exclude("log4j","log4j")
        exclude("org.apache.cassandra.deps", "avro")
        excludeAll(excludeSnappy)
    )
  ) ++ assemblySettings ++ extraAssemblySettings

  def extraAssemblySettings() = Seq(
    test in assembly := {},
    mergeStrategy in assembly := {
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case m if m.toLowerCase.matches("meta-inf.*\\.sf$") => MergeStrategy.discard
      case "META-INF/services/org.apache.hadoop.fs.FileSystem" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    }
  )
}
