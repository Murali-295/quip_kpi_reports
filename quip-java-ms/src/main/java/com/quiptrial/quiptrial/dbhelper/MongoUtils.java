package com.quiptrial.quiptrial.dbhelper;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;

/*
public class MongoUtils {
	@Autowired
	private MongoClientSingleton mongoClient;
	
	public MongoDatabase getDB(String dbname) {
		return mongoClient.getClient().getDatabase(dbname);
	}
	
	public MongoCollection<Document> getCollection(String dbname, String collname){
		return getDB(dbname).getCollection(collname);
	}
	
	public Document findOneAndUpdate(String dbname, String collname, Document query, Document update) {
		Document document = null;
		try {
			MongoCollection<Document> collection = getCollection(dbname, collname);
			document = collection.findOneAndUpdate(query, update);
		}catch(Exception ex) {
			
		}
		return document;
	}
	
	public List<Document> find(String dbname, String collname, Document query){
		MongoCursor<Document> cursor;
		List<Document> documents = new ArrayList<>();
		try {
			cursor = getCollection(dbname, collname).find(query).iterator();
			while(cursor.hasNext()) {
				Document document = cursor.next();
				documents.add(document);
			}
		}catch(Exception ex) {
			
		}
		return documents;
	}
	
	public InsertOneResult insert(String dbname, String collname, Document insertDoc) {
		InsertOneResult result = null;
		try {
			result = getCollection(dbname, collname).insertOne(insertDoc);
		}catch(Exception ex) {
			
		}
		return result;
	}

}

*/
