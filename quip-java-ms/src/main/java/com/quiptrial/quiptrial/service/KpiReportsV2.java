package com.quiptrial.quiptrial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCollection;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KpiReportsV2 {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> getDocumentResult(String clientName, String fieldName) {

        Map<String, String> response = new LinkedHashMap<>();
        Map<String, Integer> mappingDataMissingComponents = new HashMap<>();
        Map<String, Integer> mappingDataComponents = new HashMap<>();
        Map<String, Integer> noFieldComponents = new HashMap<>();

        int validCount = 0;
        int invalidCount = 0;
        int missingFieldCount = 0;

        MongoCollection<Document> componentsCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

        Document tenantConfigData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").find().first();

        response.put("totalCount", String.valueOf(componentsCollectionData.countDocuments()));

        for (Document componentDocument : componentsCollectionData.find()) {
            String componentName = componentDocument.get("componentName").toString();
            JsonNode tenantConfigComponent = objectMapper.convertValue(tenantConfigData.get("components"), JsonNode.class);
            String UpdatedComponentName = componentName.contains("|") ? componentName.substring(0, componentName.indexOf("|")) : componentName;
            JsonNode tenantConfigComponentMapping = objectMapper.convertValue(tenantConfigComponent.get(UpdatedComponentName), JsonNode.class);

            if (tenantConfigComponentMapping.isNull() || tenantConfigComponentMapping.isEmpty()) {
                invalidCount++;
                mappingDataMissingComponents.put(componentName, mappingDataMissingComponents.getOrDefault(componentName, 0) + 1);
                continue;
            }

            ArrayNode tenantConfigComponentMappingData = objectMapper.convertValue(tenantConfigComponentMapping.get("mappings"), ArrayNode.class);
            JsonNode mappingData = null;

            for (int i = 0; i < tenantConfigComponentMappingData.size(); i++) {
                mappingData = tenantConfigComponentMappingData.get(i);
                Iterator<String> mappingDataKeys = mappingData.fieldNames();

                while (mappingDataKeys.hasNext()) {
                    if (!componentDocument.containsKey(mappingDataKeys.next())) {
                        break;
                    }
                }
            }

            if (mappingData != null) {
                Document mappingDocumentData = objectMapper.convertValue(mappingData, Document.class);
                String checkField = getComponentDocumentKey(mappingDocumentData, fieldName);

                if (checkField != null) {
                    validCount++;
                    mappingDataComponents.put(componentName, mappingDataComponents.getOrDefault(componentName, 0) + 1);
                } else {
                    missingFieldCount++;
                    noFieldComponents.put(componentName, noFieldComponents.getOrDefault(componentName, 0) + 1);

                }
            }
        }

        response.put("message", "Search field Valid Count in components is: " + validCount);
        for (String compName : mappingDataComponents.keySet()) {
            response.put(compName, String.valueOf(mappingDataComponents.get(compName)));
        }

        response.put("errorMessage", "Search field Invalid Count in components is: " + invalidCount);
        for (String compName : mappingDataMissingComponents.keySet()) {
            response.put("no mapping for the component: " + compName, String.valueOf(mappingDataMissingComponents.get(compName)));
        }

        response.put("errorResponse", "Search field is missing: " + missingFieldCount);
        for (String compName : noFieldComponents.keySet()) {
            response.put("search field not found for the component: " + compName, String.valueOf(noFieldComponents.get(compName)));
        }

        return response;
    }

    private String getComponentDocumentKey(Document resultDocument, String key) {
        for (String componentDocumentKey : resultDocument.keySet()) {
            if (StringUtils.containsAnyIgnoreCase(componentDocumentKey, key)) {
                return componentDocumentKey;
            }
        }
        return null;
    }
}
