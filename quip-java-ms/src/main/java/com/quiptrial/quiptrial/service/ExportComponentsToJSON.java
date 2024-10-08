package com.quiptrial.quiptrial.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCursor;
import com.quiptrial.quiptrial.client.QuipClient;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import com.quiptrial.quiptrial.helper.FetchCompPropFromTenantConfig;
import com.quiptrial.quiptrial.helper.Utility;
import com.quiptrial.quiptrial.jsonexcel.Readjsonfile;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExportComponentsToJSON {

	@Autowired
	private Utility utility;
	@Autowired
	private QuipClient quipClient;
	@Autowired
	private FetchCompPropFromTenantConfig fetchCompPropFromTenantConfig;
	@Autowired
	private Readjsonfile readjsonfile;

	private static final ObjectMapper mapper = new ObjectMapper();

	public Map<String, Object> exportToJSON(String userName, String domainUrl) throws Exception {

		// extract clientName from URL
		String clientName = utility.getClientName(domainUrl);

		Document tenantConfigDoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig")
				.find().first();

		Map<String, Object> outerData = new LinkedHashMap<>();
		List<Map<String, Object>> innerList = new ArrayList<>();

		if (tenantConfigDoc != null) {
			JsonNode tenantConfigCollectionData = mapper.convertValue(tenantConfigDoc, JsonNode.class);
			JsonNode tenantConfigColumnMapData = tenantConfigCollectionData.get("columnsMap");

			Iterator<String> tenantConfigColumnMapDataKeys = tenantConfigColumnMapData.fieldNames();
			Set<String> columnKeys = new HashSet<>();

			while (tenantConfigColumnMapDataKeys.hasNext()) {
				columnKeys.add(tenantConfigColumnMapDataKeys.next());
			}

			columnKeys.remove("authorableSet");
			columnKeys.remove("filter");

			JsonNode tenantConfigComponentsData = tenantConfigCollectionData.get("components");

			Map<String, Map<String, Object>> components = mapper.convertValue(tenantConfigComponentsData, new TypeReference<Map<String, Map<String, Object>>>() {
            });

			MongoCursor<Document> componentCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find().iterator();

			while (componentCollectionData.hasNext()) {
				Document componentDocument = componentCollectionData.next();
				String documentId=componentDocument.getObjectId("_id").toHexString();

				if (componentDocument.get("componentName") != null) {
					Map<String, Object> componentPropertiesList = readjsonfile.retrieveComponentProperties(components, componentDocument);
					Map<String, Object> innerData = new LinkedHashMap<>();
					innerData.put("documentId",documentId);

					for (String key : columnKeys) {
						String cellValue = "Property is missing";
						String k = "";
						if (componentPropertiesList.get(key) != null && StringUtils.isNotBlank(componentPropertiesList.get(key).toString())) {
							k = componentPropertiesList.get(key).toString();
							String resultDocKey = getComponentDocumentKey(componentDocument, k);
							if (resultDocKey != null && componentDocument.get(resultDocKey) != null) {
								cellValue = componentDocument.get(resultDocKey).toString();
							}
						}
						if (StringUtils.equalsIgnoreCase(key, "componentName")) {
							cellValue = componentDocument.get(key) != null ? componentDocument.get(key).toString() : StringUtils.EMPTY;
						}
						innerData.put(key, cellValue);
					}
					innerList.add(innerData);
				}
			}

		}
		outerData.put("componentsData", innerList);
		return outerData;
	}

	private String getComponentDocumentKey(Document resultDocument, String key) {
		for (String componentDocumentKey : resultDocument.keySet()) {
			if (StringUtils.containsIgnoreCase(componentDocumentKey, key)) {
				return componentDocumentKey;
			}
		}
		return null;
	}
}