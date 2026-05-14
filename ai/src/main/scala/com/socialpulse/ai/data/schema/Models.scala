package com.socialpulse.ai.data.schema

// Case classes representing the raw JSON data extracted from Pushshift
// Since we only use a subset, we only map the fields we care about.

case class Submission(
  id: String,
  author: String,
  created_utc: Long,
  title: String,
  selftext: String,
  score: Long,
  num_comments: Long,
  url: String
)

case class Comment(
  id: String,
  author: String,
  link_id: String, // The submission this comment belongs to (e.g. t3_xxxx)
  parent_id: String,
  created_utc: Long,
  score: Long
)
