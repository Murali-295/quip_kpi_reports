package com.quiptrial.quiptrial.service;

import com.mongodb.client.MongoCollection;
import com.quiptrial.quiptrial.dbhelper.MongoClientSingleton;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UpdateComponentService {

    public Map<String, String> updateComponent(String clientName, Map<String, String> document) throws Exception {
        Map<String, String> response = new LinkedHashMap<>();

        MongoCollection<Document> clientCollection = MongoClientSingleton.getClient().getDatabase(clientName).getCollection("component");

        Document updateDocument = new Document(document);
        ObjectId id = new ObjectId(document.get("id"));

        Document queryDoc = new Document("_id", id);
        Document updateDoc = new Document("$set", updateDocument);

        long result = clientCollection.updateOne(queryDoc, updateDoc).getModifiedCount();
        if (result > 0) {
            response.put("status", "success");
            response.put("message", "Document updated successfully with the id: " + id);
        } else {
            response.put("status", "failed");
            response.put("message", "No field got updated or no document matched with the id: " + id);
        }
        return response;
    }
}