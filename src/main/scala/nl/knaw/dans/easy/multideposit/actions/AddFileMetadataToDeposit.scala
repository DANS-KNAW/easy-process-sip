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
import java.net.URLConnection

import nl.knaw.dans.easy.multideposit.actions.AddFileMetadataToDeposit._
import nl.knaw.dans.easy.multideposit.{Action, Settings, _}
import org.apache.commons.logging.LogFactory

import scala.util.{Failure, Try}

case class AddFileMetadataToDeposit(row: Int, datasetID: DatasetID)(implicit settings: Settings) extends Action {

  val log = LogFactory.getLog(getClass)

  def run() = {
    log.debug(s"Running $this")

    writeFileMetadataXml(row, datasetID)
  }
}
object AddFileMetadataToDeposit {

  def writeFileMetadataXml(row: Int, datasetID: DatasetID)(implicit settings: Settings): Try[Unit] = {
    Try {
      outputFileMetadataFile(settings, datasetID).writeXml(datasetToFileXml(datasetID))
    } recoverWith {
      case e => Failure(ActionException(row, s"Could not write file meta data: $e", e))
    }
  }

  def datasetToFileXml(datasetID: DatasetID)(implicit settings: Settings) = {
    val inputDir = multiDepositDir(settings, datasetID)

    <files xmlns:dcterms="http://purl.org/dc/terms/">{
      if (inputDir.exists && inputDir.isDirectory)
        inputDir.listRecursively.map(xmlPerPath(datasetID))
    }</files>
  }

  // TODO other fields need to be added here later
  def xmlPerPath(datasetID: DatasetID)(file: File) = {
    <file filepath={s"data${file.getAbsolutePath.split(datasetID).last}"}>{
      <dcterms:format>{URLConnection.getFileNameMap.getContentTypeFor(file.getPath)}</dcterms:format>
    }</file>
  }
}
