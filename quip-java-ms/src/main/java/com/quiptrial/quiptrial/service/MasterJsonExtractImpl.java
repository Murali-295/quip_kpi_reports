package com.quiptrial.quiptrial.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;

@Service
public class MasterJsonExtractImpl implements MasterJsonExtract{
	ObjectMapper mapper = new ObjectMapper();

	@Override
	public Map<String, Object> extractSubMenuLink(String _id, String clientName) {
		Document queryDoc = new Document();
		queryDoc.put("_id", new ObjectId(_id));
		Document projDoc = new Document();
		projDoc.put("_id", 0);
		projDoc.put("type", 0);
		Document document = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find(queryDoc).projection(projDoc).first();
		Map<String, Object> docMap = mapper.convertValue(document, Map.class);
		System.out.println("SubMenuLink Doc: " + docMap);
		return docMap;
	}

	@Override
	public Map<String, Object> extractSubMenuSection(String _id, String clientName) {
		Document queryDoc = new Document();
		queryDoc.put("_id", new ObjectId(_id));
		Document projDoc = new Document();
		projDoc.put("_id", 0);
		projDoc.put("type", 0);
		Document document = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find(queryDoc).projection(projDoc).first();
		Map<String, Object> docMap = mapper.convertValue(document, Map.class);
		if(docMap.get("subMenuLinksFieldset") != null && (!CollectionUtils.isEmpty((List<String>)docMap.get("subMenuLinksFieldset")))) {
			List<Map<String, Object>> subMenuLinksFieldset = new ArrayList<>();
			for(String _idIter : (List<String>)docMap.get("subMenuLinksFieldset")) {
				Map<String, Object> subMenuLinkField = extractSubMenuLink(_idIter,clientName);
				subMenuLinksFieldset.add(subMenuLinkField);
			}
			docMap.put("subMenuLinksFieldset", subMenuLinksFieldset);
		}
		return docMap;
	}

	@Override
	public Map<String, Object> extractPrimaryNavigation(String _id, String clientName) {
		Document queryDoc = new Document();
		queryDoc.put("_id", new ObjectId(_id));
		Document projDoc = new Document();
		projDoc.put("_id", 0);
		projDoc.put("type", 0);
		Document document = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component").find(queryDoc).projection(projDoc).first();
		Map<String, Object> docMap = mapper.convertValue(document, Map.class);
		if(docMap.get("subMenuSectionset") != null && (!CollectionUtils.isEmpty((List<String>)docMap.get("subMenuSectionset")))) {
			List<Map<String, Object>> subMenuSectionset = new ArrayList<>();
			for(String _idIter : (List<String>)docMap.get("subMenuSectionset")) {
				Map<String, Object> subMenuSection = extractSubMenuSection(_idIter,clientName);
				subMenuSectionset.add(subMenuSection);
			}
			docMap.put("subMenuSectionset", subMenuSectionset);
		}
		return docMap;
	}

	@Override
	public Map<String, Object> extractQuipMainJson(String _id, String clientName) {
		Document queryDoc = new Document();
		queryDoc.put("_id", new ObjectId(_id));
		Document projDoc = new Document();
		projDoc.put("_id", 0);
		Document document = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson").find(queryDoc).projection(projDoc).first();
		Map<String, Object> docMap = mapper.convertValue(document, Map.class);
		if(docMap.get("components") != null && (!CollectionUtils.isEmpty((Map<String, Object>)docMap.get("components")))) {
			Map<String, Object> components = (Map<String, Object>)docMap.get("components");
			components = processComponents(components,clientName);
			docMap.put("components", components);
		}
		return docMap;
	}
	private Map<String, Object> processComponents(Map<String, Object> components, String clientName) {
		if(components.get("primary-navigation") != null && (!CollectionUtils.isEmpty((List<String>)components.get("primary-navigation")))) {
			List<Map<String, Object>> primaryNavigationList = new ArrayList<>();
			for(String _idIter : (List<String>)components.get("primary-navigation")) {
				Map<String, Object> primaryNavigation = extractPrimaryNavigation(_idIter,clientName);
				primaryNavigationList.add(primaryNavigation);
			}
			components.put("primary-navigation", primaryNavigationList);
		}
		return components;
	}

}
