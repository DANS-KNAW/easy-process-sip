/*
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.multideposit

import better.files.File
import org.scalatest.matchers.{ MatchResult, Matcher }

import scala.language.postfixOps
import scala.xml._

/** Does not dump the full file but just the searched content if it is not found.
 *
 * See also <a href="http://www.scalatest.org/user_guide/using_matchers#usingCustomMatchers">CustomMatchers</a> */
trait CustomMatchers {
  class ContentMatcher(content: String) extends Matcher[File] {
    def apply(left: File): MatchResult = {
      def trimLines(s: String): String = s.split("\n").map(_.trim).mkString("\n")

      MatchResult(
        trimLines(left.contentAsString).contains(trimLines(content)),
        s"$left did not contain: $content",
        s"$left contains $content"
      )
    }
  }
  def containTrimmed(content: String) = new ContentMatcher(content)

  class EqualTrimmedMatcher(right: Iterable[Node]) extends Matcher[Iterable[Node]] {
    private val pp = new PrettyPrinter(160, 2)

    private def prepForTest(n: Node): Node = XML.loadString(pp.format(n))

    override def apply(left: Iterable[Node]): MatchResult = {
      val prettyLeft = left.map(prepForTest)
      val prettyRight = right.map(prepForTest)

      lazy val prettyLeftString = prettyLeft.mkString("\n")
      lazy val prettyRightString = prettyRight.mkString("\n")

      MatchResult(
        prettyLeft == prettyRight,
        s"$prettyLeftString was not equal to $prettyRightString",
        s"$prettyLeftString was equal to $prettyRightString"
      )
    }
  }
  def equalTrimmed(right: Iterable[Node]) = new EqualTrimmedMatcher(right)
}
