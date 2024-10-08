package com.quiptrial.quiptrial.jsonexcel;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class Readjsonfile {
	private ObjectMapper mapper = new ObjectMapper();
	public Map<String, Object> retrieveComponentProperties(Map<String, Map<String, Object>> components,
			Document resultDocument) {
		List<Map<String, Object>> componentPropertiesList = new ArrayList<>();
		Map<String, Object> mergedMap = new HashMap<>();
		String compNameWithSlash = resultDocument.get("componentName").toString();
		String[] compNameArr = compNameWithSlash.split("\\|");
		String compNameKey = compNameArr[0].trim();
		//System.out.println("compNameKey: " + compNameKey);
		Map<String, Object> componentProperties = components.get(compNameKey);

		if (componentProperties != null) {
			String propertyKeyToRetrieve = "mappings";
			// Replace with the key you want to retrieve
			Object propertyValue = componentProperties.get(propertyKeyToRetrieve);
			if (propertyValue instanceof List) {
				componentPropertiesList = (List<Map<String, Object>>) propertyValue;
			}
		}
		int propertyListSize = componentPropertiesList.size();
		if (propertyListSize > 1) {
			// Iterate through the list of maps
			for (Map<String, Object> map : componentPropertiesList) {
				boolean allKeysMatch = true;
				// Check if all keys in the 'item' map match the values in the 'map'
				for (String key : map.keySet()) {
					String resultDocKey = getResultdocKey(resultDocument, map.get(key).toString());
					if (!resultDocument.containsKey(resultDocKey)) {
						allKeysMatch = false;
						break;
					}
				}
				if (allKeysMatch) {
					mergedMap.putAll(map);
					break;
				}
			}
		} else {
			for (Map<String, Object> map : componentPropertiesList) {
				mergedMap.putAll(map);
			}
		}
		return mergedMap;
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
	
}
