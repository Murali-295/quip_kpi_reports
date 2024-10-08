package com.quiptrial.quiptrial.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.json.JSONParser;
import org.bson.Document;
import org.bson.json.JsonObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.util.StringUtil;
import com.quiptrial.quiptrial.client.QuipClient;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import com.quiptrial.quiptrial.helper.FetchCompPropFromTenantConfig;
import com.quiptrial.quiptrial.jsonexcel.Readjsonfile;
import com.quiptrial.quiptrial.poi.PoiUtilService;

import nonapi.io.github.classgraph.json.JSONSerializer;

@Service
public class ExportComponentsToExcel {
	@Autowired
	private QuipClient quipClient;
	@Autowired
	private FetchCompPropFromTenantConfig fetchCompPropFromTenantConfig;
	@Autowired
	private Readjsonfile readjsonfile;
	@Autowired
	private ActivityTracking activityTracking;

	private ObjectMapper mapper = new ObjectMapper();

	public void exportToExcel(String userName, String domain) throws Exception {
		String urlsecondPart = domain.substring(8);
		String[] urlArr = urlsecondPart.split("\\.");
		String client_name = urlArr[0];
		Map<String, Object> tenantConfigMap = null;
		Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(client_name).getCollection("tenantConfig")
				.find().first();
		if (tenantConfigDoc != null) {
			tenantConfigMap = mapper.convertValue(tenantConfigDoc, Map.class);
		} else {

		}

		// Read the JSON file and convert it to a Map
		// Access the "columnsMap" from the JSON data
		Map<String, String> columnsMap = (Map<String, String>) tenantConfigMap.get("columnsMap");
		// Read the JSON data into a list of maps. Create a new LinkedHashSet to store the keys from columnsMap in serial order
		Set<String> columnsKey = new LinkedHashSet<>(columnsMap.keySet());
		//remove Authorable set and Filter columns
		columnsKey.remove("authorableSet");
		columnsKey.remove("filter");
		// Create a new Excel workbook
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Interactions");
		int rowNum = 0;
		Row row = sheet.createRow(rowNum);

		// Create and apply a bold font style to the header row
		Font headerFont = PoiUtilService.getHeaderFont(workbook);

		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);

		// Create headers from the HashSet of keys and apply the bold style to the
		// header row
		int cellNum = 0;
		for (String key : columnsKey) {
			Cell cell = row.createCell(cellNum++);
			cell.setCellValue(columnsMap.get(key));
			cell.setCellStyle(headerCellStyle);
		}
		// Access the "components" from the JSON data
		Map<String, Map<String, Object>> components = (Map<String, Map<String, Object>>) tenantConfigMap.get("components");
		// Iterate through each map (object) in the list
		// Provide the path to your component path
		MongoCursor<Document> cursor = MongoClientSingleton.getClient().getDatabase(client_name).getCollection("component")
				.find().iterator();

		// Use StringBuilder to concatenate JSON strings
		while (cursor.hasNext()) {
			Document resultDocument = cursor.next();
			if (resultDocument.get("componentName") != null) {
				Map<String, Object> componentPropertiesList = readjsonfile.retrieveComponentProperties(components,
						resultDocument);
				// Check if the map contains the key "componentname"
				// Get the value associated with the "componentname"
				rowNum++;
				Row componentRow = sheet.createRow(rowNum);
				// Iterate through the keys in keysSet
				cellNum = 0;
				for (String key : columnsKey) {
					String cellValue = "Property is missing";
					String k = "";
					if (null != componentPropertiesList.get(key) && StringUtils.isNotBlank(componentPropertiesList.get(key).toString())) {
						k = componentPropertiesList.get(key).toString();
						String resultDocKey = getResultdocKey(resultDocument, k);
						if (resultDocKey != null && resultDocument.get(resultDocKey) != null) {
							cellValue = resultDocument.get(resultDocKey).toString();
						}
					}
					if(StringUtils.equalsIgnoreCase(key, "componentName")) {
						cellValue = null != resultDocument.get(key)? resultDocument.get(key).toString(): StringUtils.EMPTY;
					}
					Cell cell = componentRow.createCell(cellNum++);
					cell.setCellValue(cellValue);
				}
			}
		}

		Workbook updatedWorkbook = addRecordType(workbook, columnsKey);

		FileOutputStream fileOut = new FileOutputStream("/tmp" + File.separator + userName + "_" + client_name + "_atsExtract.xlsx");
		updatedWorkbook.write(fileOut);
		fileOut.close();
		updatedWorkbook.close();
		workbook.close();
		activityTracking.updateActivity(userName, domain, "ats_export_done",client_name);

	}

	private Workbook addRecordType(Workbook workbook, Set<String> columnsKey) {
		Sheet interactionsSheet = workbook.getSheet("Interactions");
		int lastrowIndex = interactionsSheet.getLastRowNum();
		int rownum = 1;
		while(rownum <= lastrowIndex) {
			Row row = interactionsSheet.getRow(rownum);
			int cellnum = 0;
			int lastCellIndex = (row.getLastCellNum()) -1;
			boolean allCellsEmpty = true;
			for(String column : columnsKey) {
				if((!StringUtils.equalsIgnoreCase(column, "recordType")) && (!StringUtils.equalsIgnoreCase(column, "filter")) && (!StringUtils.equalsIgnoreCase(column, "authorableSet")) && (!StringUtils.equalsIgnoreCase(column, "componentName")) && (!StringUtils.equalsIgnoreCase(column, "path")) && (!StringUtils.equalsIgnoreCase(column, "componentPath")) && (!StringUtils.equalsIgnoreCase(column, "inventoryUrl")) && (!StringUtils.equalsIgnoreCase(column, "analyticsInteractionId"))) {

					String cellValue = row.getCell(cellnum).getStringCellValue();
					if(StringUtils.isBlank(cellValue)) {
						cellnum ++;
					}else if (StringUtils.equals("Property is missing", cellValue)) {
						cellnum ++;
					}else {
						allCellsEmpty = false;
						break;
					}
				}else {
					cellnum ++;
				}
			}


			if(allCellsEmpty) {
				Cell lastCell = row.getCell(lastCellIndex);
				row.removeCell(lastCell);
				//Cell recordTypeCell = row.createCell(lastCellIndex+1, CellType.STRING);
				Cell recordTypeCell = row.createCell(lastCellIndex, CellType.STRING);
				recordTypeCell.setCellValue("Redundant");
			}

			rownum ++;
		}

		return workbook;
	}


	private String getResultdocKey(Document resultDocument, String key){
		String matchKey = null;
		for (String resultdocKey : resultDocument.keySet()) {
			//if(resultdocKey.contains(key)) {
			if(StringUtils.containsIgnoreCase(resultdocKey, key)) {
				matchKey = resultdocKey;
				break;
			}
		}
		return matchKey;
	}


	/*
	 * public Map<String, Integer> exportToExcel(String transactionId) throws
	 * IOException { Map<String, Object> tenantConfigMap = null; Document
	 * tenantConfigDoc =
	 * MongoClientSingleton.getClient().getDatabase("demodb").getCollection(
	 * "tenantConfig").find().first(); if (tenantConfigDoc != null) {
	 * tenantConfigMap = mapper.convertValue(tenantConfigDoc, Map.class); }else {
	 * return new HashMap<>(); } MongoCursor<Document> cursor =
	 * MongoClientSingleton.getClient().getDatabase("demodb").getCollection(
	 * "component") .find().cursor(); Workbook workbook = new XSSFWorkbook(); Sheet
	 * sheet = workbook.createSheet("components"); Document document;
	 *
	 * Map<String, String> columnsMap = (Map<String, String>)
	 * tenantConfigMap.get("columnsMap");
	 *
	 * // Read the JSON data into a list of maps // Create a new LinkedHashSet to
	 * store the keys from columnsMap in serial order Set<String> columnNamesSet =
	 * new LinkedHashSet<>(columnsMap.keySet());
	 *
	 *
	 * int rowCount = 0;
	 *
	 * if (rowCount == 0) { Row row = sheet.createRow(rowCount); int cellNum = 0;
	 * for (String key : columnNamesSet) { Cell cell = row.createCell(cellNum ++);
	 * cell.setCellValue(columnsMap.get(key)); } rowCount++; }
	 *
	 * while (cursor.hasNext()) { Row row = sheet.createRow(rowCount);
	 *
	 *
	 *
	 * document = cursor.next(); String componentname =
	 * document.get("componentname").toString();
	 *
	 * JsonNode compMapper = jsonParser(ResourceUtils.getFile("classpath:mapper/" +
	 * componentname + ".txt")); Map<String, String> cMapper =
	 * mapper.treeToValue(compMapper, Map.class);
	 *
	 *
	 * for(String key : colMapper.keySet()) { String columnName =
	 * colMapper.get(key); if(cMapper.get(columnName) == null) { Cell cell =
	 * row.createCell(Integer.parseInt(key)); cell.setCellValue(""); }else { String
	 * colFieldInDB = cMapper.get(columnName); Cell cell =
	 * row.createCell(Integer.parseInt(key)); Object dbValue =
	 * document.get(colFieldInDB); if(Objects.nonNull(dbValue)) {
	 * cell.setCellValue(document.get(colFieldInDB).toString()); }else {
	 * cell.setCellValue(""); } }
	 *
	 * } rowCount++; }
	 *
	 *
	 * String path = "C:/Users/vishnu bandari/Desktop/excelRepo/%s"; String
	 * fileLocation = String.format(path, "temp.xlsx");
	 *
	 * FileOutputStream outputStream = new FileOutputStream(fileLocation);
	 * workbook.write(outputStream); outputStream.close(); workbook.close(); return
	 * quipClient.generateKPIReport(); }
	 *
	 * private JsonNode jsonParser(File filePath) { JsonNode mapperJNode= null; try
	 * { InputStream isCompMapper = new FileInputStream(filePath); String jsonMapper
	 * = IOUtils.toString(isCompMapper, StandardCharsets.UTF_8); mapperJNode =
	 * mapper.readTree(jsonMapper); }catch(Exception e) { e.printStackTrace(); }
	 *
	 * return mapperJNode; }
	 */

}
