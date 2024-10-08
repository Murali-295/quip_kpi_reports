package com.quiptrial.quiptrial.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import com.quiptrial.quiptrial.helper.Utility;
import com.quiptrial.quiptrial.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/component")
public class ComponentController {

	@Autowired
	private ComponentService componentService;
	@Autowired
	private ExportComponentsToExcel eComponentsToExcel;
	@Autowired
	private ImportExcelToDB importExcelToDB;
	@Autowired
	private KpiReportGenerator kpiReportGenerator;
	@Autowired
	private TenantConfigServiceImpl tenantConfigService;
	@Autowired
	private MasterJsonExtractImpl extract;
	@Autowired
	private AEMDataConsumer aemDataConsumer;
	@Autowired
	private ComponentExtractJson componentExtractJson;
	@Autowired
	private FileService fileService;
	@Autowired
	private ActivityTracking activityTracking;
	@Autowired
	private ConvertMappingFileToJson convertMappingFileToJson;
	@Autowired
	private FetchAtsFileData fetchAtsFileData;
	@Autowired
	private Utility utility;
	@Autowired
	private ExportComponentsToJSON exportComponentsToJSON;
	@Autowired
    private UpdateComponentService updateComponentService;
	@Autowired
	private MasterJSONComponentData masterJSONComponentData;
	@Autowired
	private KpiReportsV2 kpiReportsV2;
	private static final ObjectMapper mapper = new ObjectMapper();

	// Obsolete Api
	@PostMapping("/exportATS")
	public ResponseEntity<Map<String, Integer>> exportATS(@RequestBody JsonNode requestJson, @RequestParam String clientUrl) {

		String clientName=utility.getClientName(clientUrl);
		Map<String, Integer> insertionId = null;

		Map<String, Integer> response = null;

		try {
			Map<String, Object> request = mapper.treeToValue(requestJson, HashMap.class);
			response = componentService.exportATS(request,clientName);
		} catch (JsonProcessingException ex) {
			ex.printStackTrace();
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/generateATS")
	public void generateATS(@RequestParam String userName, @RequestParam String domainUrl) {
		Map<String, Integer> report = null;
		try {
			eComponentsToExcel.exportToExcel(userName, domainUrl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@GetMapping
	public Map fetchDoc(@RequestParam String id,@RequestParam String clientUrl) {
		String clientName=utility.getClientName(clientUrl);
		return extract.extractQuipMainJson(id,clientName);
	}

	@PostMapping("/uploadATS")
	public void uploadOptimisedATS(@RequestParam("optimsedATSFile") MultipartFile file, @RequestParam String clientUrl) throws IOException {
		String clientName=utility.getClientName(clientUrl);
		importExcelToDB.readComponentsFromExcel(file.getInputStream(), "components",clientName);
	}

	//Obsolete
	@GetMapping("/optimisedAEMJson/{siteName}")
	public Map<String, Object> fetchOptimizedAEMJson(@PathVariable String siteName, @RequestParam String clientUrl) {
		String clientName=utility.getClientName(clientUrl);
		return componentService.processOptmisedAEMJson(siteName,clientName);
	}

	@GetMapping("/generateKPI")
	public Map<String, Integer> generateKPIReport() {
		try {
			return kpiReportGenerator.generateKPIReport();
		} catch (Exception e) {
			return Map.of("status", 500);
		}

	}

	@PutMapping("/updateTenantConfig")
	public ResponseEntity<JsonNode> updateTenantConfig(@RequestBody JsonNode requestJson, @RequestParam String clientUrl) {
		Map<String, Object> responseMap = new HashMap<>();

		String clientName=utility.getClientName(clientUrl);
		JsonNode response = null;
		try {
			Map<String, Object> request = mapper.treeToValue(requestJson, Map.class);
			UpdateResult result = tenantConfigService.updateTenantConfig(request,clientName);
			responseMap.put("result", result);
		} catch (Exception ex) {
			responseMap.put("error", ex.getMessage());
			response = mapper.convertValue(responseMap, JsonNode.class);
			return ResponseEntity.internalServerError().body(response);
		}
		response = mapper.convertValue(responseMap, JsonNode.class);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/getTenantConfig/{refNum}")
	public ResponseEntity<JsonNode> getTenantConfig(@PathVariable String refNum,@RequestParam String clientUrl) {

		String clientName=utility.getClientName(clientUrl);

		Map<String, Object> responseMap = new HashMap<>();
		JsonNode response = null;
		try {
			Document tenantConfigDoc = tenantConfigService.getTenantConfig(refNum,clientName);
			response = mapper.convertValue(tenantConfigDoc, JsonNode.class);
		} catch (Exception ex) {
			responseMap.put("error", ex.getMessage());
			response = mapper.convertValue(responseMap, JsonNode.class);
			return ResponseEntity.internalServerError().body(response);
		}
		return ResponseEntity.ok(response);
	}

	@GetMapping("/testDataLoad")
	public Map<String, Object> testDataLoad(@RequestBody JsonNode requestJson,@RequestParam String clientUrl){
		String clientName=utility.getClientName(clientUrl);

		Map<String, Object> result = new HashMap<>();
		Map<String, Object> aemData = null;
		try {
			aemData = aemDataConsumer.testProcessAEMData(requestJson,clientName);
		}catch(Exception ex) {

		}

		result.put("message", "data ingested in QUIP");
		result.put("result", aemData);
		return result;
	}

	@GetMapping("/ingestAemData")
	public Map<String, Object> ingestAemData(@RequestParam String userName, @RequestParam String clientUrl) {

		String clientName=utility.getClientName(clientUrl);

		Map<String, Object> result = new HashMap<>();
		Map<String, Object> activityStatusMap = activityTracking.initAemExtract(userName, clientUrl,clientName);
		try {
			boolean initAem = (boolean) activityStatusMap.get("initAEM");
			if (!initAem) {
				result.put("message", activityStatusMap.get("message").toString());
				result.put("result", Collections.EMPTY_MAP);
				return result;
			} else {
				Map<String, Object> aemData = aemDataConsumer.processAEMData(clientUrl, userName, clientName);
				Document masterJson = mapper.convertValue(aemData, Document.class);
				MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson").insertOne(masterJson);
				result.put("message", "data ingested in QUIP");
				result.put("result", aemData);
				activityTracking.updateActivity(userName, clientUrl, "aem_data_ingestion",clientName);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			result.put("message", "Exception: " + ex.toString());
			result.put("result", Collections.emptyMap());
			return result;
		}
		return result;
	}

	@GetMapping("/getdata")
	public ResponseEntity<JsonNode> getDataFromApi(@RequestParam String apiUrl,@RequestParam String userName) {

		String clientName=utility.getClientName(apiUrl);

		new HashMap<>();
		JsonNode response = null;
		ResponseEntity<?> entity = null;
		try {
			response = (JsonNode) componentExtractJson.componentExtractJson(userName, apiUrl,clientName);
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(response);
		}
		return entity.ok(response);
	}

	//pass parameters String userName, String domain
	@GetMapping("/downloadATS")
	public ResponseEntity<Resource> downloadAtsFile(@RequestParam String userName, @RequestParam String domainUrl) {
		Resource resource = fileService.getFileContent(userName, domainUrl);

		if (resource != null) {
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} else {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	// Obsolete Api
	@PostMapping("/uploadMappingFile")
	public void uploadMappingFile(@RequestParam("file") MultipartFile file, @RequestParam String clientUrl) throws IOException {
		String clientName=utility.getClientName(clientUrl);
		importExcelToDB.readComponentsFromExcel(file.getInputStream(), "components",clientName);
	}

	@GetMapping("/fetchAtsData")
	public String fetchAtsData(@RequestParam String clientUrl) {
		try {
			String clientName=utility.getClientName(clientUrl);
			return fetchAtsFileData.jsonToExcel(clientName);
		} catch (Exception e) {
			e.printStackTrace();
			return "Error occurred during export: " + e.getMessage();
		}
	}

	@PostMapping(path = "/convertExcelToJson", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public Map convertMappingFileToJson(@RequestParam("file") MultipartFile file, @RequestParam String domain) {
		try {

			Document jsonData=mapper.convertValue(convertMappingFileToJson.transformMappingFileToJson(file.getInputStream(), domain), Document.class);
			MongoClientSingleton.getClient().getDatabase(domain).getCollection("tenantConfig").insertOne(jsonData);
			return convertMappingFileToJson.transformMappingFileToJson(file.getInputStream(), domain);
		} catch (Exception e) {
			return new HashMap<>();
		}
	}

	@GetMapping("/exportComponentsToJSON")
	public JsonNode returnComponentsToJSON(@RequestParam String userName, @RequestParam String clientUrl) throws Exception {
		return mapper.convertValue(exportComponentsToJSON.exportToJSON(userName,clientUrl),JsonNode.class);
	}

	@PutMapping("/updateComponent")
	public ResponseEntity<Map<String, String>> updateComponent(@RequestBody JsonNode document) {
		Map<String, String> response = new LinkedHashMap<>();
		JsonNode updateDocumentNode = document.get("updateDocument");

		Map<String, String> updateDocument = mapper.convertValue(updateDocumentNode, Map.class);

		String clientName = document.get("clientname").asText();
		String id = updateDocument.get("id");

		if (id == null || id.isEmpty()) {
			response.put("status", "failed");
			response.put("message", "Document id should not be empty or null, please provide a valid id");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
		try {
			response = updateComponentService.updateComponent(clientName, updateDocument);
		} catch (Exception e) {
			if (e.getMessage().equalsIgnoreCase("state should be: hexString has 24 characters")) {
				response.put("status", "failed");
				response.put("message", "document id should be valid ");
			} else {
				response.put("status", "failed");
				response.put("message", "exception occurred:" + e.getMessage());
			}
		}
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/updateDataToAEM")
	public JsonNode masterJsonComponentData(@RequestParam String clientUrl) throws Exception {
		String clientName=utility.getClientName(clientUrl);
		return mapper.convertValue(masterJSONComponentData.convertMasterJsonData(clientName), JsonNode.class);
	}

	@GetMapping("/kpiReportsV2")
	public Map<String, String> getDocumentResult(@RequestParam String clientName, @RequestParam String fieldName){
		return kpiReportsV2.getDocumentResult(clientName,fieldName);
	}

	/*
	@PostMapping(path = "/modaltest")
	public void updateModalTest() {
		Set<String> modalReferencePaths = new HashSet<>();
		modalReferencePaths.add("/content/psoriasis/en-us/site-modals/warn-on-leave-ps-skyrisi-info");
		//aemDataConsumer.processModalComponents(modalReferencePaths);
	}
	*/

}
