package com.svcmesh;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import io.swagger.annotations.ApiOperation;

@RestController
@RefreshScope
public class BackendController {
	@Autowired
	private DiscoveryClient discoveryClient; 
	
	@Autowired
	private LoadBalancerClient lbClient;
	
	@Value("${server.port}")
	private String port;
	
	@Value("${call.divisor}")
	private int divisor;
	private int calls = 0;
	
	@Value("${greeting.message}")
	private String greeting;
	
	@GetMapping("/port")
	@ApiOperation(value = "Return Port number")
	public String port() {
		return "PORT: " + port;
	}
	
	@GetMapping("/weather")
	@ApiOperation(value="Return weather")
	public ResponseEntity<String> weather() {
		System.out.println("port: "+port+", divisor: "+divisor);
		
		System.out.println("Today's weather: ");
		if(isUnavailable()) {
			return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
		}
		System.out.println("Clean");
		return new ResponseEntity<String>("Clean", HttpStatus.OK);
	}

	private boolean isUnavailable() {
		if( divisor==0 ) 
			return false;
		else
			return ++calls % divisor != 0;
	}
	
	@GetMapping("/provider/{message}")
	@ApiOperation(value="Call 'provider' API of backend-provider-service")
	public String provider(@PathVariable String message) {
		/*
		 * Disconvery Client 사용 예제 : L/B를 수동으로 해야  
		  
		List <ServiceInstance> instances = discoveryClient.getInstances("backend-provider");
		if(instances.size()==0) {
			System.out.println("*** NO Provider service!!!");
			return "NO DATA";
		}
		System.out.println("Service count: "+ instances.size());
		int idx = (int)((Math.random()*10000)%(instances.size()-1));
		ServiceInstance instance = instances.get(idx);
		System.out.println("Url("+idx+"): "+ baseUrl);
		*/
		String baseUrl = "";
		try {
			ServiceInstance instance = lbClient.choose("backend-provider");
			baseUrl = instance.getUri().toString()+"/provider/"+message;
			System.out.println("Url: "+ baseUrl);
		} catch(Exception e) {
			System.out.println("*** NO Provider service!!!");
			return "NO DATA";
		}		
		
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = null;
		try {
			response = restTemplate.exchange(baseUrl,  HttpMethod.GET, getHeaders(), String.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return "["+port+"] => ["+baseUrl+"] " + response.getBody();
	}
	
	@GetMapping("/greeting")
	@ApiOperation(value = "Return greeting value in configuration using config server")
	public String message() {
		return "Greeting: " + greeting;
	}
	
	private static HttpEntity<?> getHeaders() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}	
	
	
}
