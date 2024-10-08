package com.quiptrial.quiptrial.service;

import java.util.Map;

import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
//import com.quiptrial.quiptrial.dbhelper.MongoUtils;

@Service
public class TenantConfigServiceImpl {
	//@Autowired
	//private MongoUtils mongoUtils;
	private ObjectMapper mapper = new ObjectMapper();

	public UpdateResult updateTenantConfig(Map<String, Object> configdoc, String clientName){
		String refNum = configdoc.get("refNum").toString();
		Document query = new Document();
		query.put("refNum", refNum);

		Document update = mapper.convertValue(configdoc, Document.class);
		Document setDoc = new Document();
		setDoc.put("$set", update);

		UpdateResult result = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").updateOne(query, setDoc);
		return result;
	}

	public Document getTenantConfig(String refNum,String clientName) {
		Document tenantConfig = null;
		Document query = new Document();
		query.put("refNum", refNum);
		tenantConfig = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("tenantConfig").find(query).first();
		if(tenantConfig == null)
			tenantConfig = new Document();
		return tenantConfig;
	}

}
