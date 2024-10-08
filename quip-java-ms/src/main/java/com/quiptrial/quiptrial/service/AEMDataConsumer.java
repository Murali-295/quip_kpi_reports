package com.quiptrial.quiptrial.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.InsertOneResult;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import com.quiptrial.quiptrial.helper.Utility;
import com.quiptrial.quiptrial.jsonexcel.Readjsonfile;

@Service
public class AEMDataConsumer {
	@Autowired
	private Utility utility;
	@Autowired
	private ComponentExtractJson componentExtractJson;
	@Autowired
	private ActivityTracking activityTracking;
	@Autowired
	private Readjsonfile readjsonfile;

	private ObjectMapper mapper = new ObjectMapper();

	public Map<String, Object> processAEMData(String clientUrl, String userName, String clientName) throws Exception {
		Map<String, Object> aemData = null;
		Set<String> interactionIdsSet = new LinkedHashSet<>();
		Set<String> modalReferencePaths = new HashSet<>();
		JsonNode aemDataJson = componentExtractJson.componentExtractJson(userName, clientUrl,clientName);
		//Path filePath = Path.of("C:\\Users\\VishnubharathBandari\\Documents\\QUIP_2.O\\COmponent Extract Quilipta.json");
		//String fileContent = Files.readString(filePath);
		//JsonNode aemDataJson = mapper.convertValue(fileContent, JsonNode.class);
		aemData = mapper.convertValue(aemDataJson, Map.class);
		Map<String, Object> componentsDataCopy = new HashMap<>();
		Map<String, Object> componentsData = (Map<String, Object>) aemData.get("components");
		for (Map.Entry<String, Object> compEntry : componentsData.entrySet()) {
			String compKey = compEntry.getKey();
			List<Map<String, Object>> compData = (List<Map<String, Object>>) compEntry.getValue();
			List<String> compDataCopy = processComponent(compKey, compData, interactionIdsSet, clientUrl,
					modalReferencePaths, null,clientName);
			componentsDataCopy.put(compKey, compDataCopy);
		}
		aemData.put("components", componentsDataCopy);
		processModalComponents(modalReferencePaths,clientName);
		// System.out.println(modalReferencePaths);
		return aemData;
	}

	public Map<String, Object> testProcessAEMData(JsonNode aemDataJson,String clientName) throws Exception {
		Map<String, Object> aemData = null;
		Set<String> interactionIdsSet = new LinkedHashSet<>();
		Set<String> modalReferencePaths = new HashSet<>();
		//JsonNode aemDataJson = componentExtractJson.componentExtractJson(userName, clientUrl);
		aemData = mapper.convertValue(aemDataJson, Map.class);
		Map<String, Object> componentsDataCopy = new HashMap<>();
		Map<String, Object> componentsData = (Map<String, Object>) aemData.get("components");
		for (Map.Entry<String, Object> compEntry : componentsData.entrySet()) {
			String compKey = compEntry.getKey();
			List<Map<String, Object>> compData = (List<Map<String, Object>>) compEntry.getValue();
			List<String> compDataCopy = processComponent(compKey, compData, interactionIdsSet, "https://psoriasis.p.cwcm-admp.com",
					modalReferencePaths, null,clientName);
			componentsDataCopy.put(compKey, compDataCopy);
		}
		aemData.put("components", componentsDataCopy);
		processModalComponents(modalReferencePaths,clientName);
		// System.out.println(modalReferencePaths);
		return aemData;
	}

	private void processModalComponents(Set<String> modalReferencePaths, String clientName) {
		// Document queryDoc = new Document();
		// queryDoc.put("_id", new ObjectId("6593f51d1e9d28611c3f9e38"));
		MongoCursor<Document> cursor = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component")
				.find().iterator();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String referencePath = null != doc.get("path|Path") ? doc.get("path|Path").toString() : null;
			if (modalReferencePaths.contains(referencePath)) {
				Document updateFields = new Document();
				updateFields.put("componentName", (doc.get("componentName").toString() + " - Modal"));

				Document updateQuery = new Document();
				updateQuery.put("$set", updateFields);

				String id = doc.getObjectId("_id").toString();
				Document searchQuery = new Document();
				searchQuery.put("_id", new ObjectId(id));

				MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").updateOne(searchQuery,
						updateQuery);
			}
		}
		// System.out.println();
	}

	private List<String> processComponent(String compKey, List<Map<String, Object>> compData,
										  Set<String> interactionIdsSet, String clientUrl, Set<String> modalReferencePaths, Map<String, Object> parentComp, String clientName) throws Exception {
		List<String> compDataCopy = new ArrayList<>();
		int counter = 0;
		for (Map<String, Object> comp : compData) {

			if(parentComp != null) {
				if(!comp.containsKey("path|Path") && null != parentComp.get("path|Path")) {
					comp.put("path|Path", (parentComp.get("path|Path").toString()) + "_" + counter);
				}
				if(!comp.containsKey("componentPath|Component Path") && null != parentComp.get("componentPath|Component Path")) {
					comp.put("componentPath|Component Path", (parentComp.get("componentPath|Component Path").toString()) + "_" + counter);
				}
			}

			String compkeyAemName = compKey.contains("|")? compKey.substring(0, compKey.indexOf("|")):compKey;

			List<String> childCompList = getCompChildList(compkeyAemName,clientName);
			if (childCompList != null && !childCompList.isEmpty()) {
				for (String childCompAemName : childCompList) {
					String childCompName = getResultdocKey(mapper.convertValue(comp, Document.class), childCompAemName);
					if (comp.containsKey(childCompName)) {
						List<String> compChildFieldCopy = processComponent(childCompName,
								(List<Map<String, Object>>) comp.get(childCompName), interactionIdsSet, clientUrl,
								modalReferencePaths, comp,clientName);
						comp.put(childCompName, compChildFieldCopy);
					}
				}
			}
			comp.put("componentName", compKey);

			if ("modal|identifier-NA".equalsIgnoreCase(compKey)) {
				if (null != comp.get("referencePath|Link URL")) {
					modalReferencePaths.add(comp.get("referencePath|Link URL").toString());
				}
			}
			comp = addRecordType(comp,clientName);
			HashMap<String, Object> validInteractionMap = validateInteractionId(comp, interactionIdsSet);
			boolean validInteraction = Boolean.parseBoolean(validInteractionMap.get("validInteraction").toString());
			Document doc = mapper.convertValue(comp, Document.class);
			InsertOneResult result = null;
			if (validInteraction) {
				result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component")
						.insertOne(doc);
				compDataCopy.add(result.getInsertedId().asObjectId().getValue().toString());
			} else {
				String dulpicateMessage = validInteractionMap.get("validInteraction").toString();
				String ClientName = utility.extractClientName(clientUrl);
				if ("duplicate_interaction_id".equals(dulpicateMessage)) {
					doc.put("isDuplicate", true);
					doc.put("clientName", ClientName);
				}
				result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("invalid_component")
						.insertOne(doc);
				compDataCopy.add(result.getInsertedId().asObjectId().getValue().toString());
			}
			counter++;
		}
		return compDataCopy;
	}

	private Map<String, Object> addRecordType(Map<String, Object> comp, String clientName) throws Exception {
		String recordType = "Interactive";
		Document document = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").find()
				.first();
		Map<String, Object> docMap = mapper.convertValue(document, Map.class);
		Map<String, String> columnsMap = docMap.get("columnsMap") != null
				? (Map<String, String>) docMap.get("columnsMap")
				: Collections.EMPTY_MAP;
		Map<String, Map<String, Object>> components = (Map<String, Map<String, Object>>) docMap.get("components");
		Set<String> columnsKey = new LinkedHashSet<>(columnsMap.keySet());
		Map<String, Object> componentPropertiesList = readjsonfile.retrieveComponentProperties(components,
				mapper.convertValue(comp, Document.class));

		if (null != componentPropertiesList.get("filter")
				&& StringUtils.isNotEmpty(componentPropertiesList.get("filter").toString())) {
			String compKey = componentPropertiesList.get("filter").toString();
			String compDocKey = getResultdocKey(mapper.convertValue(comp, Document.class), compKey);
			String compKeyValue = (null != compDocKey && null != comp.get(compDocKey)) ? comp.get(compDocKey).toString()
					: "";
			if (StringUtils.isBlank(compKeyValue))
				recordType = "Non Interactive";
		}

		comp.put("recordType", recordType);
		return comp;
	}

	public List<String> getCompChildList(String compName, String clientName) {
		List<String> compChildList = null;
		Document document = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").find()
				.first();
		Map<String, Object> docMap = mapper.convertValue(document, Map.class);
		Map<String, Object> components = null != docMap.get("components")
				? (Map<String, Object>) docMap.get("components")
				: Collections.EMPTY_MAP;
		Map<String, Object> componentMap = null != components.get(compName)
				? (Map<String, Object>) components.get(compName)
				: Collections.EMPTY_MAP;
		compChildList = null != componentMap.get("child") ? (List<String>) componentMap.get("child")
				: Collections.EMPTY_LIST;
		return compChildList;
	}

	private boolean obsolete_validateInteractionId(Map<String, Object> comp, Set<String> interactionIdsSet) {
		boolean validInteraction = true;
		String interactionId = getInteractionIdKey(comp);
		if (interactionId != null) {
			String interactionIdVal = comp.get(interactionId).toString();
			if (StringUtils.isEmpty(interactionIdVal) || interactionIdsSet.contains(interactionIdVal)) {
				validInteraction = false;
			}
		} else {
			validInteraction = false;
		}
		return validInteraction;
	}

	private String getInteractionIdKey(Map<String, Object> comp) {
		Set<String> compKeySet = comp.keySet();
		for (String key : compKeySet) {
			if (StringUtils.containsIgnoreCase(key, "interactionid")) {
				return key;
			}
		}
		return null;
	}

	private HashMap<String, Object> validateInteractionId(Map<String, Object> comp, Set<String> interactionIdsSet) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("validInteraction", true);
		result.put("message", "valid interaction id");
		String interactionId = getInteractionIdKey(comp);
		if (interactionId != null) {
			String interactionIdVal = comp.get(interactionId).toString();
			if (StringUtils.isEmpty(interactionIdVal) || interactionIdsSet.contains(interactionIdVal)) {
				result.put("validInteraction", false);
				result.put("message", "interaction id is either empty or duplicated");
				if (interactionIdsSet.contains(interactionIdVal)) {
					result.put("message", "duplicate_interaction_id");
				}
			}
		} else {
			result.put("validInteraction", false);
			result.put("message", "interaction id is null");
		}
		return result;
	}

	private String getResultdocKey(Document resultDocument, String key) {
		String matchKey = null;
		if (null != key && StringUtils.isNotEmpty(key)) {
			for (String resultdocKey : resultDocument.keySet()) {
				// if(resultdocKey.contains(key)) {
				if (StringUtils.containsIgnoreCase(resultdocKey, key)) {
					matchKey = resultdocKey;
					break;
				}
			}
		}
		return matchKey;
	}

}