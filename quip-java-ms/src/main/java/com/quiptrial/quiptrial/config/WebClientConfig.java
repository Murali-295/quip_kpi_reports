package com.quiptrial.quiptrial.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import com.quiptrial.quiptrial.client.QuipClient;

@Configuration
public class WebClientConfig {
	//@Autowired(required = true)
	//private ExchangeFilterFunction filterFunction; 
	
	@Bean
	public WebClient quipWebClient() {
		return WebClient.builder().baseUrl("http://localhost:8080").build();
	}
	
	@Bean
	public QuipClient employeeClient() {
		HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(quipWebClient())).build();
		return httpServiceProxyFactory.createClient(QuipClient.class);
	}

}
