package org.cardanofoundation.job.util.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import org.cardanofoundation.job.util.DataUtil;
import org.cardanofoundation.job.util.ReflectorUtil;

@Log4j2
@Component
public class ExcelHelper {

  private static final String DATE_TIME_PATTERN = "MM/dd/yyyy HH:mm:ss";

  @Value("${jobs.limit-content}")
  private int limitSize;

  public ByteArrayInputStream writeContent(
      List<ExportContent> exportContents, Long timezoneOffset, String timePattern) {
    if (timezoneOffset == null) {
      timezoneOffset = 0L;
    }

    if (timePattern == null) {
      timePattern = DATE_TIME_PATTERN;
    }

    var currentTime = System.currentTimeMillis();
    try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
      CellStyle cellStyleHeader = createStyleHeader(workbook);
      for (ExportContent exportContent : exportContents) {
        SXSSFSheet sheet = workbook.createSheet(exportContent.getHeaderTitle());
        List<ExportColumn> lstColumn = exportContent.getLstColumn();
        List<?> lstData = exportContent.getLstData();
        int startRow = 0;

        // write header
        Row rowHeader = sheet.createRow(startRow);
        for (int i = 0; i < lstColumn.size(); i++) {
          Cell cell = rowHeader.createCell(i);
          cell.setCellStyle(cellStyleHeader);
          cell.setCellValue(richTextString(lstColumn.get(i).getColumnTitle().getValue()));
          sheet.trackColumnForAutoSizing(i);
          sheet.autoSizeColumn(i);
        }
        writeDataReport(
            workbook, exportContent, sheet, lstColumn, lstData, timezoneOffset, timePattern);
      }
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        workbook.write(out);
        log.info("Export excel time taken: {}", System.currentTimeMillis() - currentTime);
        return new ByteArrayInputStream(out.toByteArray());
      }
    } catch (final IOException | IllegalAccessException e) {
      throw new RuntimeException("Excel writing error");
    }
  }

  private void writeDataReport(
      SXSSFWorkbook workbook,
      ExportContent exportContent,
      SXSSFSheet sheet,
      List<ExportColumn> lstColumn,
      List<?> lstData,
      Long timezoneOffset,
      String timePattern)
      throws IllegalAccessException {
    List<Field> fields = ReflectorUtil.getAllFields(exportContent.getClazz());
    Map<String, Field> mapField = new HashMap<>();

    exportContent
        .getLstColumn()
        .forEach(
            exportColumn ->
                fields.stream()
                    .peek(f -> f.setAccessible(true))
                    .filter(f -> f.getName().equals(exportColumn.getColumnField().getValue()))
                    .forEach(f -> mapField.put(exportColumn.getColumnField().getValue(), f)));

    if (DataUtil.isNullOrEmpty(lstData)) {
      String message =
          exportContent.getSimpleMessage() == null
              ? "No records"
              : exportContent.getSimpleMessage();
      writeNoRecordContents(workbook, sheet, lstColumn, message);
      return;
    }

    CellStyle cellStyleLeft = createCellStyle(workbook, HorizontalAlignment.LEFT);
    CellStyle cellStyleCenter = createCellStyle(workbook, HorizontalAlignment.CENTER);
    CellStyle cellStyleRight = createCellStyle(workbook, HorizontalAlignment.RIGHT);

    for (int i = 0; i < lstData.size(); i++) {
      Object obj = lstData.get(i);
      Row row = sheet.createRow(i + 1);
      for (int j = 0; j < lstColumn.size(); j++) {
        Cell cell = row.createCell(j);
        ExportColumn exportColumn = lstColumn.get(j);
        Field field = mapField.get(exportColumn.getColumnField().getValue());
        if (field == null) {
          throw new RuntimeException(
              "Field not found: " + exportColumn.getColumnField().getValue());
        }
        Object value = field.get(obj);
        String text;
        if (value instanceof Double) {
          text = DataUtil.doubleToString((Double) value);
        } else if (value instanceof Instant) {
          text = DataUtil.instantToString((Instant) value, timezoneOffset, timePattern);
        } else if (value instanceof Date || value instanceof Timestamp) {
          text = DataUtil.dateToString(((Date) value), timezoneOffset, timePattern);
        } else if (value instanceof LocalDateTime) {
          text =
              DataUtil.localDateTimeToString(((LocalDateTime) value), timezoneOffset, timePattern);
        } else if (value instanceof Enum<?>) {
          text = DataUtil.enumToString((Enum<?>) value);
        } else {
          text = DataUtil.objectToString(value);
        }
        switch (exportColumn.getAlign()) {
          case LEFT:
            cell.setCellStyle(cellStyleLeft);
            break;
          case RIGHT:
            cell.setCellStyle(cellStyleRight);
            break;
          case CENTER:
            cell.setCellStyle(cellStyleCenter);
            break;
        }
        cell.setCellValue(richTextString(text));
        int numberOfLines = text.split("\n").length;
        row.setHeightInPoints(numberOfLines * sheet.getDefaultRowHeightInPoints());
      }
    }

    for (int i = 0; i < lstColumn.size(); i++) {
      sheet.trackColumnForAutoSizing(i);
      sheet.autoSizeColumn(i);
      sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 100);
    }

    if (lstData.size() == limitSize) {
      writeOutOfLimitContents(workbook, sheet, lstColumn);
    }
  }

  private void writeOutOfLimitContents(
      SXSSFWorkbook workbook, SXSSFSheet sheet, List<ExportColumn> lstColumn) {
    Row row = sheet.createRow(limitSize + 2);
    Cell cell = row.createCell(0);
    CellStyle cellStyle = workbook.getXSSFWorkbook().createCellStyle();
    cellStyle.setAlignment(HorizontalAlignment.CENTER);
    Font font = workbook.createFont();
    font.setFontName(HSSFFont.FONT_ARIAL);
    font.setFontHeightInPoints((short) 11);
    font.setColor(IndexedColors.RED.getIndex());
    font.setItalic(true);
    cellStyle.setFont(font);
    cell.setCellStyle(cellStyle);
    cell.setCellValue(
        richTextString("***This sheet only exports the earliest " + limitSize + " records.***"));
    sheet.addMergedRegion(
        new CellRangeAddress(limitSize + 2, limitSize + 2, 0, lstColumn.size() - 1));
  }

  private void writeNoRecordContents(
      SXSSFWorkbook workbook, SXSSFSheet sheet, List<ExportColumn> lstColumn, String message) {
    Row row = sheet.createRow(1);
    Cell cell = row.createCell(0);
    CellStyle cellStyle = workbook.getXSSFWorkbook().createCellStyle();
    cellStyle.setAlignment(HorizontalAlignment.CENTER);
    Font font = workbook.createFont();
    font.setFontName(HSSFFont.FONT_ARIAL);
    font.setFontHeightInPoints((short) 11);
    cellStyle.setFont(font);
    cell.setCellStyle(cellStyle);
    cell.setCellValue(richTextString(message));
    sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lstColumn.size() - 1));
  }

  private CellStyle createStyleHeader(SXSSFWorkbook workbook) {
    CellStyle cellStyleHeader = createCellStyleHeader(workbook);
    Font fontHeader = workbook.createFont();
    fontHeader.setFontName(HSSFFont.FONT_ARIAL);
    fontHeader.setBold(true);
    fontHeader.setFontHeightInPoints((short) 11);
    cellStyleHeader.setFont(fontHeader);
    return cellStyleHeader;
  }

  private CellStyle createCellStyleHeader(SXSSFWorkbook workbook) {
    CellStyle cellStyleHeader = workbook.getXSSFWorkbook().createCellStyle();
    cellStyleHeader.setAlignment(HorizontalAlignment.CENTER);
    cellStyleHeader.setVerticalAlignment(VerticalAlignment.CENTER);
    cellStyleHeader.setBorderLeft(BorderStyle.THIN);
    cellStyleHeader.setBorderBottom(BorderStyle.THIN);
    cellStyleHeader.setBorderRight(BorderStyle.THIN);
    cellStyleHeader.setBorderTop(BorderStyle.THIN);
    cellStyleHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
    cellStyleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    cellStyleHeader.setWrapText(true);
    return cellStyleHeader;
  }

  private CellStyle createCellStyle(
      SXSSFWorkbook workbook, HorizontalAlignment horizontalAlignment) {
    CellStyle cellStyle = workbook.getXSSFWorkbook().createCellStyle();
    cellStyle.setAlignment(horizontalAlignment);
    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    cellStyle.setBorderLeft(BorderStyle.THIN);
    cellStyle.setBorderBottom(BorderStyle.THIN);
    cellStyle.setBorderRight(BorderStyle.THIN);
    cellStyle.setBorderTop(BorderStyle.THIN);
    cellStyle.setWrapText(true);
    cellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("@"));

    XSSFFont font = workbook.getXSSFWorkbook().createFont();
    font.setFontName(HSSFFont.FONT_ARIAL);
    font.setFontHeightInPoints((short) 11);
    cellStyle.setFont(font);
    return cellStyle;
  }

  public XSSFRichTextString richTextString(Object object) {
    return new XSSFRichTextString(object.toString());
  }
}
