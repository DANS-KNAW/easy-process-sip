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
package nl.knaw.dans.easy.multideposit.actions

import better.files.{ ManagedResource, _ }
import nl.knaw.dans.easy.multideposit.PathExplorer.OutputPathExplorer
import nl.knaw.dans.easy.multideposit.actions.ReportDatasets._
import nl.knaw.dans.easy.multideposit.encoding
import nl.knaw.dans.easy.multideposit.model.Deposit
import org.apache.commons.csv.{ CSVFormat, CSVPrinter }

import scala.util.Try

class ReportDatasets {

  def report(deposits: Seq[Deposit])(implicit output: OutputPathExplorer): Try[Unit] = Try {
    for(printer <- csvPrinter(output.reportFile);
        deposit <- deposits)
      printRecord(deposit, printer)
  }

  private def csvPrinter(file: File): ManagedResource[CSVPrinter] = {
    file.bufferedWriter(charset = encoding)
      .flatMap[CSVPrinter, ManagedResource](writer => new ManagedResource(csvFormat.print(writer)))
  }

  private def printRecord(deposit: Deposit, printer: CSVPrinter): Unit = {
    printer.printRecord(
      deposit.depositId,
      deposit.bagId,
      deposit.baseUUID.getOrElse(deposit.bagId)
    )
  }
}

object ReportDatasets {
  private val csvFormat = CSVFormat.RFC4180.withHeader("DATASET", "UUID", "BASE-REVISION")
}
