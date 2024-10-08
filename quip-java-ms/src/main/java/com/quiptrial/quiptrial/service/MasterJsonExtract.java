package com.quiptrial.quiptrial.service;

import java.util.Map;

public interface MasterJsonExtract {
	public Map<String, Object> extractSubMenuLink(String _id, String clientName);
	public Map<String, Object> extractSubMenuSection(String _id, String clientName);
	public Map<String, Object> extractPrimaryNavigation(String _id, String clientName);
	public Map<String, Object> extractQuipMainJson(String _id, String clientName);
}
