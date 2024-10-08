package com.quiptrial.quiptrial.poi;

import javax.swing.GroupLayout.Alignment;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PoiUtilService {
	public static Font getGlobalFont(Workbook wb) {
		Font font = wb.createFont();
		font.setFontHeightInPoints((short) 10);
		font.setFontName("Arial");
		font.setColor(IndexedColors.BLACK.getIndex());
		return font;
	}
	public static Font getHeaderFont(Workbook wb) {
		Font font = wb.createFont();
		font.setFontHeightInPoints((short) 10);
		font.setFontName("Arial");
		font.setColor(IndexedColors.BLACK.getIndex());
		font.setBold(true);
		return font;
	}
	public static CellStyle getRowStyle(Workbook wb, Row row) {
		CellStyle rowStyle = row.getRowStyle();
		rowStyle.setAlignment(HorizontalAlignment.CENTER);
		rowStyle.setFont(getGlobalFont(wb));
		return rowStyle;
	}
	public static CellStyle getHeaderStyle(Workbook wb, Row row) {
		CellStyle rowStyle = row.getRowStyle();
		rowStyle.setAlignment(HorizontalAlignment.CENTER);
		rowStyle.setFont(getHeaderFont(wb));
		rowStyle.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		//rowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return rowStyle;
	}
	
	public static void hideColumn(Sheet sheet , int columnIndex) {
		sheet.setColumnHidden(columnIndex, true);
	}
	
	public static void exposeColumn(Sheet sheet , int columnIndex) {
		sheet.setColumnHidden(columnIndex, false);
	}
	public static void setCellBackgroundColor(Cell cell, IndexedColors color) {
        Workbook workbook = cell.getSheet().getWorkbook();
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);
    }
    public static void setCellTextColor(Cell cell, IndexedColors color) {
        Workbook workbook = cell.getSheet().getWorkbook();
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(color.getIndex());
        style.setFont(font);
        cell.setCellStyle(style);
    }

}
