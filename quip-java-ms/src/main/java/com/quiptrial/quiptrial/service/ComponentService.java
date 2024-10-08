package com.quiptrial.quiptrial.service;

import java.util.Map;

public interface ComponentService {
	public Map<String, Integer> exportATS(Map<String, Object> request,String clientName);
	public Map<String, Object> processOptmisedAEMJson(String siteName,String clientName);

}
