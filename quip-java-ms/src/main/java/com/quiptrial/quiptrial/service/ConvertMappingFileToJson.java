package com.quiptrial.quiptrial.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ConvertMappingFileToJson {
	public Map<String, Object> transformMappingFileToJson(InputStream inputfile, String domain) throws IOException{
		Map<String, Object> mappingJson = new HashMap<>();
		mappingJson.put("domain", domain);
		Map<String, String> colsATSFile = new LinkedHashMap<>();
		Map<String, Object> components = new HashMap<>();
		XSSFWorkbook wbook = new XSSFWorkbook(inputfile);
		XSSFSheet sheet = wbook.getSheet("QUIPMasterMapping");
		Iterator<Row> rowsItr = sheet.rowIterator();
		int rowIndex = 0;
		String parentCompName = "";
		String[] colNamesList = null;
		int compNameColIndex = 0;

		while(rowsItr.hasNext()) {
			Boolean rowContainsDot = false;
			Row row = rowsItr.next();
			if(rowIndex == 0) {
				Iterator<Cell> cellItr = row.cellIterator();
				while(cellItr.hasNext()) {
					String colName = "";
					Cell cell = cellItr.next();
					CellType cellType = cell.getCellType();
					if(cellType != CellType.BLANK) {
						colName = cell.getStringCellValue();
						colsATSFile.put(CaseUtils.toCamelCase(colName, false, ' '), colName);
					}
				}
				mappingJson.put("columnsMap", colsATSFile);

				colNamesList = new String[colsATSFile.keySet().size()];
				colsATSFile.keySet().toArray(colNamesList);

				rowIndex ++;
				continue;
			}


			Map<String, Object> componentMap = new HashMap<>();
			List<Map<String, String>> mappingsList = new ArrayList<>();
			List<String> childList = new ArrayList<>();

			Map<String, String> mapping = new HashMap<>();

			for(int cn=0; cn<row.getLastCellNum(); cn++) {
				if(cn == 0 || cn == 1)
					continue;
				Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
				String colValue = cell.getStringCellValue();
				if(StringUtils.isNotEmpty(colValue) && colValue.contains(".")) {
					String[] dotSplit = colValue.split("\\.");
					mapping.put(colNamesList[cn], dotSplit[dotSplit.length-1]);
					if(Boolean.FALSE.equals(rowContainsDot))
						rowContainsDot = true;
				}else {
					mapping.put(colNamesList[cn], colValue);
				}
			}


			if(Boolean.TRUE.equals(rowContainsDot)) {
				String nonBlankCol = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
				if(nonBlankCol.contains(".")) {
					String[] splitArr = nonBlankCol.split("\\.");
					if(splitArr.length == 2) {
						String compName = splitArr[splitArr.length-2].trim();
						if(components.containsKey(compName)) {
							Map<String, Object> component = (Map<String, Object>)components.get(compName);
							List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
							mappings.add(mapping);
						}else {
							mappingsList.add(mapping);
							componentMap.put("mappings", mappingsList);
							componentMap.put("child", childList);
							components.put(compName, componentMap);
						}
						String parent = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue();
						if(StringUtils.isNotEmpty(parent)) {
							Map<String, Object> component = (Map<String, Object>)components.get(parent);
							List<String> child = (List<String>) component.get("child");
							child.add(compName);
						}
					}
					if(splitArr.length >= 3) {
						String compName = splitArr[splitArr.length-2].trim();
						if(components.containsKey(compName)) {
							Map<String, Object> component = (Map<String, Object>)components.get(compName);
							List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
							mappings.add(mapping);
						}else {
							mappingsList.add(mapping);
							componentMap.put("mappings", mappingsList);
							componentMap.put("child", childList);
							components.put(compName, componentMap);
						}
						String parent = splitArr[splitArr.length-3].trim();
						if(StringUtils.isNotEmpty(parent)) {
							Map<String, Object> component = (Map<String, Object>)components.get(parent);
							List<String> child = (List<String>) component.get("child");
							child.add(compName);
						}
					}
				}
			}else {
				parentCompName = row.getCell(compNameColIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() != CellType.BLANK?row.getCell(compNameColIndex).getStringCellValue(): parentCompName;
				if(components.containsKey(parentCompName)) {
					Map<String, Object> component = (Map<String, Object>)components.get(parentCompName);
					List<Map<String, String>> mappings = (List<Map<String, String>>) component.get("mappings");
					mappings.add(mapping);
				}else {
					mappingsList.add(mapping);
					componentMap.put("mappings", mappingsList);
					componentMap.put("child", childList);
					components.put(parentCompName, componentMap);
				}
			}
			rowIndex ++;
		}
		mappingJson.put("components", components);
		wbook.close();
		return mappingJson;
	}
}