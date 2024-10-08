package com.quiptrial.quiptrial.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;

@Service
public class ImportExcelToDB {
	private ObjectMapper mapper = new ObjectMapper();
	public void readComponentsFromExcel(InputStream is, String sheetname, String clientName) throws IOException {

		JsonNode excelColMapperJson = jsonParser(ResourceUtils.getFile("classpath:mapper/excel-mapper.txt"));
		Map<String, String> excelColMapper = mapper.treeToValue(excelColMapperJson, HashMap.class);

		Workbook wb = new XSSFWorkbook(is);
		Sheet sheet = wb.getSheet(sheetname);
		for (int rownum = 1; rownum <= sheet.getLastRowNum(); rownum++) {
			Row row = sheet.getRow(rownum);
			String componentname = row.getCell(row.getLastCellNum()-2).getStringCellValue();
			JsonNode compMapperJson = jsonParser(ResourceUtils.getFile("classpath:mapper/" + componentname + ".txt"));
			Map<String, String> compMapper = mapper.treeToValue(compMapperJson, Map.class);

			String id = row.getCell(row.getLastCellNum()-1).getStringCellValue();
			Document querydoc = new Document();
			querydoc.put("_id", new ObjectId(id));

			Document updatedoc = new Document();
			for (int cellnum = 0; cellnum <= row.getLastCellNum()-3; cellnum++) {
				if(null != row.getCell(cellnum) && null != compMapper.get(excelColMapper.get(Integer.toString(cellnum)))) {
					updatedoc.put(compMapper.get(excelColMapper.get(Integer.toString(cellnum))), row.getCell(cellnum).getStringCellValue());
				}
			}
			Document setdoc = new Document();
			setdoc.put("$set", updatedoc);
			MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").findOneAndUpdate(querydoc, setdoc);
		}
	}

	private JsonNode jsonParser(File filePath) {
		JsonNode mapperJNode= null;
		try {
			InputStream isCompMapper = new FileInputStream(filePath);
			String jsonMapper = IOUtils.toString(isCompMapper, StandardCharsets.UTF_8);
			mapperJNode = mapper.readTree(jsonMapper);
		}catch(Exception e) {
			e.printStackTrace();
		}

		return mapperJNode;
	}

}
