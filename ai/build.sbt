import Dependencies._

ThisBuild / scalaVersion     := "2.12.18"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.socialpulse"
ThisBuild / organizationName := "socialpulse"

lazy val sparkVersion = "3.5.1"

lazy val root = (project in file("."))
  .settings(
    name := "ai-ranking",
    libraryDependencies ++= Seq(
      munit % Test,

      "com.github.luben" % "zstd-jni" % "1.5.7-4",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.19.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.0",

      // Spark
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.spark" %% "spark-mllib" % sparkVersion,

      // XGBoost (Spark integration)
      "ml.dmlc" %% "xgboost4j-spark" % "1.7.6",

      // Configuration
      "com.github.pureconfig" %% "pureconfig" % "0.17.6"
    )
  )
