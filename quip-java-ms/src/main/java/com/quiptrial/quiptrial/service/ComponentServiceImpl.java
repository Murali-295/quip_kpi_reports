package com.quiptrial.quiptrial.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.InsertOneResult;
import com.quiptrial.quiptrial.client.QuipClient;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;

@Service
public class ComponentServiceImpl implements ComponentService{

	@Autowired
	private QuipClient quipClient;

	//create bean for this instance
	private ObjectMapper mapper = new ObjectMapper();

	@Override
	public Map<String, Integer> exportATS(Map<String, Object> request, String clientName) {
		Map<String, Integer> report = null;
		String siteName = null;
		if(null != request.get("metadata")) {
			Map<String, Object> metadata = mapper.convertValue(request.get("metadata"), Map.class);
			siteName = metadata.get("siteName").toString();
		}
		if(null != request.get("components") && !CollectionUtils.isEmpty(request)) {
			Map<String,Object> components = mapper.convertValue(request.get("components"), Map.class);
			request.put("components", processComponents(components, siteName,clientName));
		}
		InsertOneResult result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson").insertOne(mapper.convertValue(request, Document.class));
		String insertionId = result.getInsertedId().asObjectId().getValue().toString();

		if(null != insertionId)
			report = quipClient.exportExcel("test");
		if(report == null)
			report = new HashMap<>();

		return report;
	}

	private Map<String, Object> processComponents(Map<String, Object> components, String siteName, String clientName) {
		List<Map<String,Object>> primaryNavigationList = Objects.nonNull(components.get("primary-navigation")) ? (List<Map<String,Object>>)components.get("primary-navigation"): Collections.EMPTY_LIST;
		List<Object> primaryNavigationListCopy = new ArrayList<>();
		for(Map<String, Object> pNavigation : primaryNavigationList) {
			primaryNavigationListCopy.add(processPrimaryNavigation(pNavigation,clientName));
		}
		components.put("primary-navigation", primaryNavigationListCopy);
		return components;
	}

	private String processPrimaryNavigation(Map<String, Object> pNavigation, String clientName) {
		List<Map<String,Object>> subMenuSectionsetList = pNavigation.get("subMenuSectionset") != null ? (List<Map<String, Object>>) pNavigation.get("subMenuSectionset") : Collections.EMPTY_LIST;
		List<Object> subMenuSectionsetListCopy = new ArrayList<>();
		for(Map<String,Object> subMenuSS : subMenuSectionsetList) {
			subMenuSectionsetListCopy.add(processSubMenuSS(subMenuSS,clientName));
		}
		pNavigation.put("subMenuSectionset", subMenuSectionsetListCopy);
		pNavigation.put("componentname", "primary-navigation");
		InsertOneResult result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").insertOne(mapper.convertValue(pNavigation, Document.class));
		return result.getInsertedId().asObjectId().getValue().toString();
	}

	private String processSubMenuSS(Map<String, Object> subMenuSS, String clientName) {
		List<Map<String,Object>> subMenuLinksFieldsetList = subMenuSS.get("subMenuLinksFieldset") != null ? (List<Map<String, Object>>) subMenuSS.get("subMenuLinksFieldset") : Collections.EMPTY_LIST;
		List<Object> subMenuLinksFieldsetListCopy = new ArrayList<>();
		for(Map<String,Object> subMenuLinkFS : subMenuLinksFieldsetList) {
			subMenuLinksFieldsetListCopy.add(processSubMenuLinkFS(subMenuLinkFS,clientName));
		}
		subMenuSS.put("subMenuLinksFieldset", subMenuLinksFieldsetListCopy);
		subMenuSS.put("componentname", "subMenuSectionset");
		InsertOneResult result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").insertOne(mapper.convertValue(subMenuSS, Document.class));
		return result.getInsertedId().asObjectId().getValue().toString();
	}

	private String processSubMenuLinkFS(Map<String, Object> subMenuLinkFS, String clientName) {
		subMenuLinkFS.put("componentname", "subMenuLinksFieldset");
		InsertOneResult result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").insertOne(mapper.convertValue(subMenuLinkFS, Document.class));
		return result.getInsertedId().asObjectId().getValue().toString();
	}

	public Map<String, Object> processOptmisedAEMJson(String siteName, String clientName) {
		Document query = new Document();
		query.put("siteName", "rinvoq");
		Document masterjson = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson").find().first();
		Map<String, Object> masterdoc = mapper.convertValue(masterjson, Map.class);
		if(null != masterdoc.get("components")) {
			Map<String, Object> components = (Map<String, Object>) masterdoc.get("components");
			if(null != components.get("primary-navigation")) {
				List<String> primaryNavigation =  (List<String>) components.get("primary-navigation");
				List<Map<String, Object>> primaryNavigation_copy = processOptPrimarynavigation(primaryNavigation,clientName);
				components.put("primary-navigation", primaryNavigation_copy);
			}
		}
		return masterdoc;
	}

	private List<Map<String, Object>> processOptPrimarynavigation(List<String> primaryNavigationList, String clientName) {
		List<Map<String, Object>> primaryNavigationList_copy = new ArrayList<>();
		for(String prItem : primaryNavigationList) {
			Document querydoc = new Document();
			querydoc.put("_id", new ObjectId(prItem));
			Document primarynavigationdoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find(querydoc).first();
			Map<String, Object> primarynavigationMap = mapper.convertValue(primarynavigationdoc, Map.class);
			if(null != primarynavigationMap.get("subMenuSectionset")) {
				List<String> subMenuSectionsetList = mapper.convertValue(primarynavigationMap.get("subMenuSectionset"), List.class);
				List<Map<String, Object>> subMenuSectionsetList_copy = processOptSubMenuSectionset(subMenuSectionsetList,clientName);
				primarynavigationMap.put("subMenuSectionset", subMenuSectionsetList_copy);
			}
			primaryNavigationList_copy.add(primarynavigationMap);
		}
		return primaryNavigationList_copy;
	}

	private List<Map<String, Object>> processOptSubMenuSectionset(List<String> subMenuSectionsetList, String clientName) {
		List<Map<String, Object>> subMenuSectionsetList_copy = new ArrayList<>();
		for(String id : subMenuSectionsetList) {
			Document querydoc = new Document();
			querydoc.put("_id", new ObjectId(id));
			Document subMenuSectionsetdoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find(querydoc).first();
			Map<String, Object> subMenuSectionsetMap = mapper.convertValue(subMenuSectionsetdoc, Map.class);
			if(null != subMenuSectionsetMap.get("subMenuLinksFieldset")) {
				List<String> subMenuLinksFieldsetList = mapper.convertValue(subMenuSectionsetMap.get("subMenuLinksFieldset"), List.class);
				List<Map<String, Object>> subMenuLinksFieldsetList_copy = processOptSubMenuLinksFieldset(subMenuLinksFieldsetList,clientName);
				subMenuSectionsetMap.put("subMenuLinksFieldset", subMenuLinksFieldsetList_copy);
			}
			subMenuSectionsetList_copy.add(subMenuSectionsetMap);
		}
		return subMenuSectionsetList_copy;
	}

	private List<Map<String, Object>> processOptSubMenuLinksFieldset(List<String> subMenuLinksFieldsetList, String clientName) {
		List<Map<String, Object>> subMenuLinksFieldsetList_copy = new ArrayList<>();
		for(String id : subMenuLinksFieldsetList) {
			Document querydoc = new Document();
			querydoc.put("_id", new ObjectId(id));
			Document subMenuLinksFieldsetdoc = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find(querydoc).first();
			Map<String, Object> subMenuLinksFieldsetMap = mapper.convertValue(subMenuLinksFieldsetdoc, Map.class);
			subMenuLinksFieldsetList_copy.add(subMenuLinksFieldsetMap);
		}
		return subMenuLinksFieldsetList_copy;
	}

}
