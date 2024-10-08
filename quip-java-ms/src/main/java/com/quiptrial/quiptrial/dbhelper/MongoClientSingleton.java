package com.quiptrial.quiptrial.dbhelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;


public class MongoClientSingleton {
	private static MongoClient clientSync;
	//@Value("${spring.data.mongodb.host}")
	private String hosts = "http://100.24.248.226";
	//@Value("${spring.data.mongodb.username}")
	private String username="admin";
	//@Value("${spring.data.mongodb.password}")
	private String password="password";
	//@Value("${spring.data.mongodb.database}")
	private String db="admin";
	private MongoClientSingleton() {

		/*
		 * List<String> hostArr = Arrays.asList(hosts.split(",")); List<ServerAddress>
		 * seeds = new ArrayList<>(); for(String host : hostArr) { seeds.add(new
		 * ServerAddress(host)); }
		 */
		//MongoCredential mongoCredential = MongoCredential.createCredential(username, db, password.toCharArray());
		//MongoClientSettings settings = MongoClientSettings.builder().credential(mongoCredential).readPreference(ReadPreference.secondaryPreferred()).build();
		MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(new ConnectionString("mongodb://admin:nextrow%232024@100.24.248.226:27017/?retryWrites=true&loadBalanced=false&serverSelectionTimeoutMS=5000&connectTimeoutMS=10000&authSource=admin&authMechanism=SCRAM-SHA-256&3t.uriVersion=3&3t.connection.name=quip_seo_db&3t.alwaysShowAuthDB=true&3t.alwaysShowDBFromUserRole=true")).build();

		clientSync = MongoClients.create(settings);
	}

	public static synchronized MongoClient getClient() {
		if(clientSync == null) {
			new MongoClientSingleton();
		}
		return clientSync;
	}

}
