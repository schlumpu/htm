package nl.malienkolders.htm.admin.lib.exporter

import java.io.OutputStream
import nl.malienkolders.htm.lib.model._
import nl.malienkolders.htm.admin.lib.Utils.Constants._

object DetailsExporter extends ExcelExporter {

  def doExport(out: OutputStream): Unit = {
    val workbook = loadWorkbook("details")
    val sheet = workbook.getSheetAt(0)

    var i = 0

    for (t <- Tournament.findAll) {
      sheet.getOrCreateRow(i).getOrCreateCell(0).setCellValue(t.name.get)
      i += 1
      for (r <- t.rounds.filter(r => t.identifier.get == "wrestling" || r.name.get == ROUND_NAME_FINAL || r.name.get == ROUND_NAME_THIRD_PLACE)) {
        sheet.getOrCreateRow(i).getOrCreateCell(0).setCellValue(r.name.get)
        for (p <- r.pools.flatMap(_.fights).flatMap(f => List(f.fighterA.obj.get, f.fighterB.obj.get))) {
          val row = sheet.getOrCreateRow(i)
          i += 1
          val rowName = sheet.getOrCreateRow(i)
          rowName.getOrCreateCell(0).setCellValue("Name")
          rowName.getOrCreateCell(1).setCellValue(p.name.get)
          i += 1
          val rowClub = sheet.getOrCreateRow(i)
          rowClub.getOrCreateCell(0).setCellValue("Club")
          rowClub.getOrCreateCell(1).setCellValue(p.club.get)
          i += 1
          val rowCountry = sheet.getOrCreateRow(i)
          rowCountry.getOrCreateCell(0).setCellValue("Country")
          rowCountry.getOrCreateCell(1).setCellValue(p.country.obj.get.name.get)
          i += 1
          val rowAge = sheet.getOrCreateRow(i)
          rowAge.getOrCreateCell(0).setCellValue("Age")
          rowAge.getOrCreateCell(1).setCellValue(p.age.get)
          i += 1
          val rowHeight = sheet.getOrCreateRow(i)
          rowHeight.getOrCreateCell(0).setCellValue("Height (cm)")
          rowHeight.getOrCreateCell(1).setCellValue(p.height.get)
          i += 1
          val rowWeight = sheet.getOrCreateRow(i)
          rowWeight.getOrCreateCell(0).setCellValue("Weight (kg)")
          rowWeight.getOrCreateCell(1).setCellValue(p.weight.get)
          i += 1
          val winsRow = sheet.getOrCreateRow(i)
          winsRow.getOrCreateCell(0).setCellValue("Previous Wins")
          for (win <- p.previousWins.get.split("""(\r|\n)+""")) {
            sheet.getOrCreateRow(i).getOrCreateCell(1).setCellValue(win)
            i += 1
          }

        }

        i += 1
      }

      i += 1
    }

    workbook.write(out)
  }

}