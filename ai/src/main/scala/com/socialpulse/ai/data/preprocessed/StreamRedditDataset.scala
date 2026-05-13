package com.socialpulse.ai.data.preprocessed

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.luben.zstd.ZstdInputStreamNoFinalizer

import java.io._
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters._

object StreamRedditDataset {

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def streamLines(path: String): Iterator[String] = {
    val in = new ZstdInputStreamNoFinalizer(new BufferedInputStream(new FileInputStream(path), 65536))
    scala.io.Source.fromInputStream(in)(StandardCharsets.UTF_8).getLines()
  }

  def cleanText(text: String): String = {
    if (text == null) ""
    else text.filter(c => c == '\n' || c == '\t' || (c >= 32 && c < 127)).replaceAll("\\s+", " ").trim
  }

  def parseComment(line: String): Option[Map[String, Any]] = {
    try {
      val node = mapper.readTree(line)
      val body = node.get("body").asText()
      if (body == "[deleted]" || body == "[removed]" || body.length < 10) None
      else Some(node.fields().asScala.map { e => e.getKey -> e.getValue.asText() }.toMap)
    } catch { case _: Exception => None }
  }

  def filterComment(obj: Map[String, Any], minScore: Int, subreddits: Option[Seq[String]]): Boolean = {
    val score = obj("score").toString.toInt
    if (score < minScore) return false
    subreddits.foreach { list =>
      val sr = obj("subreddit").toString.toLowerCase
      if (!list.exists(sr == _.toLowerCase)) return false
    }
    true
  }

  def transformComment(obj: Map[String, Any]): String = {
    val json = Map(
      "id" -> obj("id"),
      "created_utc" -> obj("created_utc"),
      "subreddit" -> obj("subreddit"),
      "author" -> obj("author"),
      "body" -> cleanText(obj("body").toString),
      "link_id" -> obj.getOrElse("link_id", ""),
      "parent_id" -> obj.getOrElse("parent_id", ""),
      "score" -> obj("score")
    )
    mapper.writeValueAsString(json)
  }

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      println("""
        |Usage: StreamRedditDataset <zstPath> <outputJsonl> [minScore] [subreddit1,subreddit2,...]
        |
        |Example:
        |  comments    raw/RC_2019-04.zst  output/comments.jsonl  3  python,movies
        |  submissions raw/RS_2019-04.zst  output/posts.jsonl     5  learnscala
      """.stripMargin)
      return
    }

    val zstPath = args(0)
    val outputPath = args(1)
    val minScore = if (args.length > 2) args(2).toInt else 1
    val subreddits = if (args.length > 3) Some(args(3).split(",").toSeq) else None

    println(s"Processing: $zstPath -> $outputPath")
    val writer = new PrintWriter(new File(outputPath))
    var count = 0
    try {
      streamLines(zstPath).foreach { line =>
        parseComment(line).foreach { obj =>
          if (filterComment(obj, minScore, subreddits)) {
            writer.println(transformComment(obj))
            count += 1
            if (count % 100000 == 0) println(s"  $count records...")
          }
        }
      }
    } finally {
      writer.close()
    }
    println(s"Done: $count records -> $outputPath")
  }
}
