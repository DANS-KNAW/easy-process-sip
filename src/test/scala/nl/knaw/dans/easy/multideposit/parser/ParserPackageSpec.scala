/**
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
package nl.knaw.dans.easy.multideposit.parser

import nl.knaw.dans.easy.multideposit.UnitSpec

class ParserPackageSpec extends UnitSpec {

  "NonEmptyList transform" should "succeed when the input list is not empty" in {
    listToNEL(List(1, 2, 3)) shouldBe ::(1, ::(2, ::(3, Nil)))
  }

  it should "fail when the input list is empty" in {
    the[IllegalArgumentException] thrownBy listToNEL(List.empty) should have message "requirement failed: the list can't be empty"
  }

  "defaultIfEmpty" should "return the original list if it isn't empty" in {
    val list = (1 to 4).toList
    list.defaultIfEmpty(-1) shouldBe list
  }

  it should "return a list with the default value if the input list is empty" in {
    List.empty.defaultIfEmpty(-1) shouldBe List(-1)
  }

  "find" should "return the value corresponding to the key in the row" in {
    val row = Map("foo" -> "abc", "bar" -> "def")
    row.find("foo").value shouldBe "abc"
  }

  it should "return None if the key is not present in the row" in {
    val row = Map("foo" -> "abc", "bar" -> "def")
    row.find("baz") shouldBe empty
  }

  it should "return None if the value corresponding to the key in the row is empty" in {
    val row = Map("foo" -> "abc", "bar" -> "")
    row.find("bar") shouldBe empty
  }

  it should "return None if the value corresponding to the key in the row is blank" in {
    val row = Map("foo" -> "abc", "bar" -> "  \t  ")
    row.find("bar") shouldBe empty
  }
}
