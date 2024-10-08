package com.quiptrial.quiptrial.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCollection;
import com.quiptrial.quiptrial.client.QuipClient;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import com.quiptrial.quiptrial.helper.FetchCompPropFromTenantConfig;
import com.quiptrial.quiptrial.helper.Utility;
import com.quiptrial.quiptrial.jsonexcel.Readjsonfile;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MasterJSONComponentData {

    @Autowired
    private AEMDataConsumer aemDataConsumer;
    @Autowired
    private Utility utility;
    @Autowired
    private QuipClient quipClient;
    @Autowired
    private FetchCompPropFromTenantConfig fetchCompPropFromTenantConfig;
    @Autowired
    private Readjsonfile readjsonfile;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Map<String, List<Document>>> convertMasterJsonData(String clientName) {
        Document masterJsonData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJson")
                .find().first();

        MongoCollection<Document> componentsCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

        MongoCollection<Document> invalidComponentsCollectionData = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("invalid_component");

        Map<String, Map<String, List<Document>>> totalData = new HashMap<>();
        Map<String, List<Document>> componentsData = new HashMap<>();

        if (masterJsonData != null) {
            masterJsonData.remove("_id");

            JsonNode components = objectMapper.convertValue(masterJsonData.get("components"), JsonNode.class);
            Iterator<String> componentKeys = components.fieldNames();

            while (componentKeys.hasNext()) {
                String componentKey = componentKeys.next();
                ArrayNode componentData = (ArrayNode) components.get(componentKey);
                List<Document> componentsList = new ArrayList<>();

                for (int i = 0; i < componentData.size(); i++) {
                    String objectId = componentData.get(i).asText();
                    Document filterById = new Document("_id", new ObjectId(objectId));
                    Document component = componentsCollectionData.find().filter(filterById).first();

                    if (component != null) {
                        component.remove("_id");
                        List<Document> processedComponentData = processComponent(component, clientName,componentsCollectionData);
                        componentsList.addAll(processedComponentData);
                    } else {
                    Document invalidComponent = invalidComponentsCollectionData.find().filter(filterById).first();
                    if (invalidComponent != null) {
                        invalidComponent.remove("_id");
                        List<Document> processedComponentData = processComponent(invalidComponent, clientName,componentsCollectionData);
                        componentsList.addAll(processedComponentData);
                    }
                    }
                }
                componentsData.put(componentKey, componentsList);
            }
        }

        totalData.put("components", componentsData);

        // if in case want to store the response to database for backup
        //MongoClientSingleton.getClient().getDatabase(clientName).getCollection("masterJsonData").insertOne(new Document("components", componentsData));
        return totalData;
    }


    private List<Document> processComponent(Document component, String clientName, MongoCollection<Document> componentsCollectionData) {
        // to store the processed component
        List<Document> processedComponents = new ArrayList<>();

        // processed component
        Document processedComponent = new Document(component);

        // Check for child component
        List<String> childComponentsList = childComponentsList(component, clientName);

        // if child exists
        if (!childComponentsList.isEmpty()) {
            // to store child documents
            List<Document> childDocuments = new ArrayList<>();

            // iterate over each child component key
            for (String childComponentKey : childComponentsList) {

                // get the key that will match with key in component
                String childComponentMatchKey = getResultdocKey(component, childComponentKey);

                // check if component has that key
                if (component.containsKey(childComponentMatchKey)) {

                    // get doc array of child components
                    Object componentDataObject = component.get(childComponentMatchKey);
                    ArrayNode childComponentDocumentIds = objectMapper.convertValue(componentDataObject, ArrayNode.class);

                    // iterate over each doc id from child components docs
                    for (int i = 0; i < childComponentDocumentIds.size(); i++) {
                        String childComponentDocumentId = childComponentDocumentIds.get(i).asText();
                        Document filterById = new Document("_id", new ObjectId(childComponentDocumentId));

                        // retrieve the child component
                        Document childComponentData = componentsCollectionData.find(filterById).first();

                        if (childComponentData != null) {
                            // processing the doc id for understanding
                            childComponentData.remove("_id");

                            // recursive method to check and fetch if component has children
                            List<Document> processedChildDocuments = processComponent(childComponentData, clientName, componentsCollectionData);
                            // adding if sub children are present
                            childDocuments.addAll(processedChildDocuments);
                        }
                    }

                    // add the processed child documents to the parent component
                    if (!childDocuments.isEmpty()) {
                        processedComponent.put(childComponentMatchKey, childDocuments);
                    }
                }
            }
        }
        // add the processed component to the list
        processedComponents.add(processedComponent);
        return processedComponents;
    }

    public List<String> childComponentsList(Document component,String clientName){
        String compName=component.getString("componentName");

        String compkeyAemName = compName.contains("|")? compName.substring(0, compName.indexOf("|")):compName;
        List<String> childCompList = aemDataConsumer.getCompChildList(compkeyAemName,clientName);
        return childCompList;
    }


    public String getResultdocKey(Document resultDocument, String key) {
        String matchKey = null;
        if (null != key && StringUtils.isNotEmpty(key)) {
            for (String resultdocKey : resultDocument.keySet()) {
                if (StringUtils.containsIgnoreCase(resultdocKey, key)) {
                    matchKey = resultdocKey;
                    break;
                }
            }
        }
        return matchKey;
    }
}
