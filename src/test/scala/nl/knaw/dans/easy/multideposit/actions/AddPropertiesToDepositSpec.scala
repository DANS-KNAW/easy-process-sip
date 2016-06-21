/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.multideposit.actions

import java.io.File
import javax.naming.directory.Attributes

import nl.knaw.dans.easy.multideposit.{Settings, UnitSpec, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import rx.lang.scala.Observable

import scala.collection.mutable
import scala.util.{Failure, Success}

class AddPropertiesToDepositSpec extends UnitSpec with BeforeAndAfter with BeforeAndAfterAll with MockFactory {

  val ldapMock = mock[Ldap]
  implicit val settings = Settings(
    multidepositDir = new File(testDir, "md"),
    outputDepositDir = new File(testDir, "dd"),
    ldap = ldapMock
  )
  val datasetID = "ds1"
  val dataset = mutable.HashMap(
    "DEPOSITOR_ID" -> List("dp1", "", "", "")
  )

  before {
    new File(settings.outputDepositDir, s"md-$datasetID").mkdirs
  }

  override def afterAll = testDir.getParentFile.deleteDirectory()

  "checkPreconditions" should "succeed if the depositorID is in the dataset and has one value" in {
    (ldapMock.query(_: String)(_: Attributes => Boolean)) expects ("dp1", *) returning Observable.just(true)

    AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions shouldBe a[Success[_]]
  }

  it should "succeed if the depositorID column contains multiple but equal values" in {
    val dataset = mutable.HashMap(
      "DEPOSITOR_ID" -> List("dp1", "dp1", "dp1", "dp1")
    )
    (ldapMock.query(_: String)(_: Attributes => Boolean)) expects ("dp1", *) returning Observable.just(true)

    AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions shouldBe a[Success[_]]
  }

  it should "fail when the depositorID column is not in the dataset" in {
    val dataset = mutable.HashMap(
      "TEST_COLUMN" -> List("abc", "def")
    )

    val result = AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions

    result shouldBe a[Failure[_]]
    (the[ActionException] thrownBy result.get).row shouldBe 1
    (the[ActionException] thrownBy result.get).message contains "is not present"
  }

  it should "fail when the depositorID column contains multiple different values" in {
    val dataset = mutable.HashMap(
      "DEPOSITOR_ID" -> List("dp1", "dp1", "dp2", "dp1")
    )

    val result = AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions

    result shouldBe a[Failure[_]]
    (the[ActionException] thrownBy result.get).row shouldBe 1
    (the[ActionException] thrownBy result.get).message contains "multiple distinct"
  }

  it should "fail if ldap identifies the depositorID as not active" in {
    (ldapMock.query(_: String)(_: Attributes => Boolean)) expects ("dp1", *) returning Observable.just(false)

    val result = AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions

    result shouldBe a[Failure[_]]
    (the [ActionException] thrownBy result.get).row shouldBe 1
    (the [ActionException] thrownBy result.get).message contains "depositor dp1 is not an active user"
  }

  it should "fail if ldap does not return anything" in {
    (ldapMock.query(_: String)(_: Attributes => Boolean)) expects ("dp1", *) returning Observable.empty

    val result = AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions

    result shouldBe a[Failure[_]]
    (the [ActionException] thrownBy result.get).row shouldBe 1
    (the [ActionException] thrownBy result.get).message contains "dp1 is unknown"
    (the [ActionException] thrownBy result.get).cause.getClass shouldBe classOf[NoSuchElementException]
  }

  it should "fail if ldap returns multiple values" in {
    (ldapMock.query(_: String)(_: Attributes => Boolean)) expects ("dp1", *) returning Observable.just(true, true)

    val result = AddPropertiesToDeposit(1, (datasetID, dataset)).checkPreconditions

    result shouldBe a[Failure[_]]
    (the [ActionException] thrownBy result.get).row shouldBe 1
    (the [ActionException] thrownBy result.get).message contains "multiple users with id dp1"
    (the [ActionException] thrownBy result.get).cause.getClass shouldBe classOf[IllegalArgumentException]
  }

  "run" should "generate the properties file" in {
    AddPropertiesToDeposit(1, (datasetID, dataset)).run() shouldBe a[Success[_]]

    new File(outputDepositDir(settings, datasetID), "deposit.properties") should exist
  }

  "writeProperties" should "generate the properties file and write the properties in it" in {
    AddPropertiesToDeposit(1, (datasetID, dataset)).run() shouldBe a[Success[_]]

    val props = outputPropertiesFile(settings, datasetID)
    val content = props.read()
    content should include ("state.label")
    content should include ("state.description")
    content should include ("depositor.userId=dp1")
  }
}
