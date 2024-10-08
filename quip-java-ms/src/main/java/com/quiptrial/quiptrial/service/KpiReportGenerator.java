package com.quiptrial.quiptrial.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.mongodb.util.Base64.OutputStream;

@Service
public class KpiReportGenerator {
	public Map<String, Integer> generateKPIReport() throws IOException{
		Map<String, Integer> kpiSummary = new HashMap<>();
		int analyticsTitleCount = 0;
		int mvaCount = 0;
		int mvaTitleForAnalyticsCount = 0;
		InputStream is = new FileInputStream(new File("C:/Users/vishnu bandari/Desktop/excelRepo/temp.xlsx"));
		Workbook wb = new XSSFWorkbook(is);
		Sheet componentsSheet = wb.getSheet("components");
		Sheet summarySheet = wb.createSheet("ATS Summary");
		Iterator<Row> rows = componentsSheet.iterator();
		for(int num = 1; num < componentsSheet.getLastRowNum(); num++) {
			Row currRow = componentsSheet.getRow(num);
			if(currRow.getCell(2).getStringCellValue().equals("")) analyticsTitleCount++;
			if(currRow.getCell(3).getStringCellValue().equals("")) mvaCount++;
			if(currRow.getCell(4).getStringCellValue().equals("")) mvaTitleForAnalyticsCount++;	
		}
		kpiSummary.put("analyticsTitleBlanks", analyticsTitleCount);
		kpiSummary.put("mvaBlanks", mvaCount);
		kpiSummary.put("mvaTitleForAnalyticsBlanks", mvaTitleForAnalyticsCount);
		is.close();
		//wb.close();
		//FileOutputStream os = new FileOutputStream(new File("C:/Users/vishnu bandari/Desktop/excelRepo/temp.xlsx"));
		//wb = new XSSFWorkbook(os);
		//int rowcount = 0;
		//for(Map.Entry<String, Integer> entry : kpiSummary.entrySet()) {
		//	Row row = summarySheet.createRow(rowcount++);
		//	row.createCell(0, CellType.STRING).setCellValue(entry.getKey());
		//	row.createCell(1, CellType.NUMERIC).setCellValue(entry.getValue());
		//}
		//wb.write(os);
		wb.close();
		//os.close();
		return kpiSummary;
	}

}
