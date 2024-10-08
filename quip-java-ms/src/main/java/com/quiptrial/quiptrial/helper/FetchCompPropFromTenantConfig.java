package com.quiptrial.quiptrial.helper;
import java.util.*;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class FetchCompPropFromTenantConfig {
	public static Map<String, Object> retrieveComponentProperties(Map<String, Map<String, Object>> components,Document resultDocument) {
        List<Map<String, Object>> componentPropertiesList = new ArrayList<>();
        Map<String,Object>mergedMap = new HashMap<>();
        Map<String, Object> componentProperties = components.get(resultDocument.get("componentName"));

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
                    if (!resultDocument.containsValue(key)) {
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
	}