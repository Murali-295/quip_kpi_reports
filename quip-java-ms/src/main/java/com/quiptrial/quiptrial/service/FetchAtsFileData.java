package com.quiptrial.quiptrial.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import com.mongodb.client.MongoCursor;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import com.quiptrial.quiptrial.jsonexcel.Readjsonfile;

@Service
public class FetchAtsFileData {
	@Autowired
	private Readjsonfile readjsonfile;
	private ObjectMapper mapper = new ObjectMapper();

	public String jsonToExcel(String clientName) throws Exception {
		String json = null;
		Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig")
				.find().first();

		if (tenantConfigDoc != null) {
			json = tenantConfigDoc.toJson();
		}
		Map<String, Object> jsonMap = (Map<String, Object>) mapper.readValue(json, Map.class);
		Map<String, String> columnsMap = (Map<String, String>) jsonMap.get("columnsMap");
		Set<String> columnsKey = new LinkedHashSet<>(columnsMap.keySet());
		Map<String, Map<String, Object>> components = (Map<String, Map<String, Object>>) jsonMap.get("components");
		MongoCursor<Document> cursor = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component")
				.find().iterator();
		List<Map<String, String>> excelDataList = new ArrayList<>();
		while (cursor.hasNext()) {
			Document resultDocument = cursor.next();
			if (resultDocument.get("componentName") != null) {
				Map<String, Object> componentPropertiesList = readjsonfile.retrieveComponentProperties(components,
						resultDocument);
				Map<String, String> excelDataRow = new HashMap<>();
				for (String key : columnsKey) {
					String cellValue = "";
					String k = "";
					if (componentPropertiesList.get(key) != null) {
						k = componentPropertiesList.get(key).toString();
						if (resultDocument.get(k) != null) {
							cellValue = resultDocument.get(k).toString();
						}
					}
					excelDataRow.put(key, cellValue);
				}
				excelDataList.add(excelDataRow);
			}
		}
		String jsonData = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(excelDataList);
		return jsonData;
	}
}