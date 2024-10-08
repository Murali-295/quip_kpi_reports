package com.quiptrial.quiptrial.service;

import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quiptrial.quiptrial.helper.Utility;

@Service
public class ComponentExtractJson {
	@Autowired
	ActivityTracking activityTracking;
	private ObjectMapper mapper = new ObjectMapper();
	Utility utility = new Utility();

	public JsonNode componentExtractJson(String userName, String apiUrl, String clientName) throws Exception {
		String auth = "Authorization: Basic cXVpcC1zZXJ2aWNlLXVzZXI6cXVpcFNlcnZpY2VVc2Vy=\n";
		//		+ "Cookie: 383bd0ee17ff6f31\n" + "cq-authoring-mode: TOUCH";
		HttpResponse<?> cmp = utility.utilityMethod(apiUrl, auth);
		if(cmp.statusCode() == 200)
			activityTracking.addActivity(userName, apiUrl, "aem_data_extract", clientName);
		String cmp1 = (String) cmp.body();
		JsonNode json = (JsonNode) mapper.readTree(cmp1);
		return json;
	}
}