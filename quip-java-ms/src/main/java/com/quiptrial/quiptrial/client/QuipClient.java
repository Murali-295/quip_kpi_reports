package com.quiptrial.quiptrial.client;

import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface QuipClient {
	
	@GetExchange("/component/generateKPI")
	public Map<String, Integer> generateKPIReport();
	
	@GetExchange("/component/generateATS/{transactionId}")
	public Map<String, Integer> exportExcel(@PathVariable String transactionId);
}
