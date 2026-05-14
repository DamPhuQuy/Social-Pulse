package com.socialpulse.ai.config

import pureconfig._
import pureconfig.generic.auto._

case class SparkConfig(master: String, appName: String)
case class DataConfig(submissionsPath: String, commentsPath: String, outputFeaturesPath: String)
case class ModelConfig(numRounds: Int, maxDepth: Int, modelExportPath: String)

case class AppConfig(
  spark: SparkConfig,
  data: DataConfig,
  model: ModelConfig
)

object AppConfig {
  def load(): AppConfig = {
    // For local dev, we could fallback to some default if application.conf is missing
    ConfigSource.default.loadOrThrow[AppConfig]
  }
}
