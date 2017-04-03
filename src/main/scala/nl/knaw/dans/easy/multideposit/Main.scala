/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy.multideposit

import nl.knaw.dans.easy.multideposit.actions._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.language.postfixOps
import scala.util.{ Failure, Try }

object Main extends DebugEnhancedLogging {

  def main(args: Array[String]): Unit = {
    debug("Starting application")
    implicit val settings = CommandLineOptions.parse(args)

    run
      .ifFailure { case e =>
        logger.error(e.getMessage)
        logger.debug(e.getMessage, e)
      }
      .ifSuccess(_ => logger.info("Finished successfully!"))

    logger.debug("closing ldap")
    settings.ldap.close()
  }

  def run(implicit settings: Settings): Try[Unit] = {
    MultiDepositParser.parse(multiDepositInstructionsFile)
      .flatMap(getActions(_).map(_.run(())).getOrElse(Failure(new Exception)))
  }

  def getActions(datasets: Datasets)(implicit settings: Settings): Option[Action[Unit, Unit]] = {
    logger.info("Compiling list of actions to perform ...")

    val retrieveDatamanagerAction = RetrieveDatamanagerAction()
    val datasetActions = datasets.map {
      case entry@(datasetID, dataset) =>
        val row = dataset.getRowNumber
        logger.debug(s"Getting actions for dataset $datasetID ...")

        CreateStagingDir(row, datasetID)
          .combine(AddBagToDeposit(row, entry))
          .combine(AddDatasetMetadataToDeposit(row, entry))
          .combine(AddFileMetadataToDeposit(row, entry))
          .combine(retrieveDatamanagerAction)
          .combine(AddPropertiesToDeposit(row, entry))
          .combine(SetDepositPermissions(row, datasetID))
    }.reduceOption(_ combine _)
    val moveActions = datasets.map {
      case (datasetID, dataset) => MoveDepositToOutputDir(dataset.getRowNumber, datasetID): Action[Unit, Unit]
    }.reduceOption(_ combine _)

    for {
      dsAct <- datasetActions
      mvAct <- moveActions
    } yield dsAct combine mvAct
  }
}
