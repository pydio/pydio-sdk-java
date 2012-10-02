/**
 *  Copyright 2012 Charles du Jeu
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 *  This file is part of the AjaXplorer Java Client
 *  More info on http://ajaxplorer.info/
 */
package info.ajaxplorer.client.model;

import info.ajaxplorer.client.http.AjxpAPI;
import info.ajaxplorer.client.http.RestRequest;
import info.ajaxplorer.client.util.PassManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;


public class Server {

	String id;
	String label;
	String url;
	String user;
	String password;
	URI uri;
	Node serverNode;	
	boolean trustSSL;
	boolean legacyServer;
	Map<String, String> remoteCapacities;
	
	public boolean passNeedsEncryption = false;
	public static String capacity_UPLOAD_LIMIT = "//property[@name='UPLOAD_MAX_SIZE']";
	
	public Node getServerNode() {
		return serverNode;
	}
	

	public void setServerNode(Node serverNode) {
		this.serverNode = serverNode;
	}

	public ArrayList<Node> getRepositories(){
		return new ArrayList<Node>(this.serverNode.children);
	}

	public Server(Node serverNode) throws URISyntaxException{
		this.serverNode = serverNode;
		this.label = serverNode.getLabel();
		this.url = serverNode.getPropertyValue("url");
		this.user = serverNode.getPropertyValue("user");
		//this.password = serverNode.getPropertyValue("password");
		try {
			String p = serverNode.getPropertyValue("password");
			this.password = PassManager.decrypt(p);
			if(p.equals(this.password)){passNeedsEncryption = true;}
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(serverNode.getPropertyValue("trust_ssl") != null){
			this.trustSSL = Boolean.parseBoolean(serverNode.getPropertyValue("trust_ssl"));
		}
		if(serverNode.getPropertyValue("legacy_server") != null){
			this.legacyServer = Boolean.parseBoolean(serverNode.getPropertyValue("legacy_server"));
		}
		this.id = Server.slugifyId(user, url);		
		this.uri = Server.uriFromString(url);
	}

	public Node createDbNode(Dao<Node, String> nodeDao) throws SQLException{
		Node n = new Node(Node.NODE_TYPE_SERVER, this.getLabel(), null);
		nodeDao.create(n);
		n.properties = nodeDao.getEmptyForeignCollection("properties");
		n.addProperty("url", this.getUrl());
		n.addProperty("user", this.getUser());
		n.addProperty("trust_ssl", Boolean.toString(trustSSL));
		n.addProperty("legacy_server", Boolean.toString(legacyServer));
		try {
			n.addProperty("password", PassManager.encrypt(this.getPassword()));
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
		this.setServerNode(n);
		return n;		
	}
	
	public Node createDbNode(RuntimeExceptionDao<Node, Integer> nodeDao){
		Node n = new Node(Node.NODE_TYPE_SERVER, this.getLabel(), null);
		nodeDao.create(n);
		n.properties = nodeDao.getEmptyForeignCollection("properties");
		n.addProperty("url", this.getUrl());
		n.addProperty("user", this.getUser());
		n.addProperty("trust_ssl", Boolean.toString(trustSSL));
		n.addProperty("legacy_server", Boolean.toString(legacyServer));
		try {
			n.addProperty("password", PassManager.encrypt(this.getPassword()));
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
		this.setServerNode(n);
		return n;		
	}
	
	public void updateDbNode(Dao<Node, String> nodeDao, Dao<Property, String> propertyDao) throws SQLException{
		Node n = this.getServerNode();
		for(Property p:n.properties){
			if(p.getName().equals("url")) p.setValue(this.getUrl());
			else if(p.getName().equals("user")) p.setValue(this.getUser());
			else if(p.getName().equals("password")){
				try {
					p.setValue(PassManager.encrypt(this.getPassword()));
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
			}
			else if(p.getName().equals("trust_ssl")) p.setValue(Boolean.toString(trustSSL));
			else if(p.getName().equals("legacy_server")) p.setValue(Boolean.toString(legacyServer));	
			propertyDao.update(p);
		}
		if(!n.getLabel().equals(this.label)){
			n.setLabel(this.label);
		}
		nodeDao.update(n);
	}	
	public void updateDbNode(RuntimeExceptionDao<Node, Integer> nodeDao, RuntimeExceptionDao<Property, Integer> propertyDao){
		Node n = this.getServerNode();
		for(Property p:n.properties){
			if(p.getName().equals("url")) p.setValue(this.getUrl());
			else if(p.getName().equals("user")) p.setValue(this.getUser());
			else if(p.getName().equals("password")) {
				try {
					p.setValue(PassManager.encrypt(this.getPassword()));
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
			}
			else if(p.getName().equals("trust_ssl")) p.setValue(Boolean.toString(trustSSL));
			else if(p.getName().equals("legacy_server")) p.setValue(Boolean.toString(legacyServer));			
			propertyDao.update(p);
		}
		if(!n.getLabel().equals(this.label)){
			n.setLabel(this.label);
			nodeDao.update(n);
		}
	}
	public void upgradePassword(RuntimeExceptionDao<Property, Integer> propertyDao){
		for(Property p:this.getServerNode().properties){
			if(!p.getName().equals("password")) continue;
			try {
				p.setValue(PassManager.encrypt(this.getPassword()));
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
			propertyDao.update(p);
		}		
	}

	public Server(String label, String url, String user, String password, boolean trustSSL, boolean legacyServer) throws URISyntaxException{
		this.label = label;
		this.url = url;
		this.user = user;
		this.password = password;
		this.id = Server.slugifyId(user, url);
		this.trustSSL = trustSSL;
		this.legacyServer = legacyServer;
		this.uri = uriFromString(url);
	}
	
	public Map<String,String> getRemoteCapacities(RestRequest rest){
		if(this.remoteCapacities != null) return this.remoteCapacities;
		// Load XML Registry and get values
		remoteCapacities = new HashMap<String, String>();
		try {
			Document doc = rest.getDocumentContent(AjxpAPI.getInstance().getXmlPluginsRegistryUri());
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			XPathExpression expr = xpath.compile(capacity_UPLOAD_LIMIT);
			org.w3c.dom.Node result = (org.w3c.dom.Node)expr.evaluate(doc, XPathConstants.NODE);
			remoteCapacities.put(capacity_UPLOAD_LIMIT, result.getFirstChild().getNodeValue());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return this.remoteCapacities;
	}

	private static String slugifyId(String user, String url) throws URISyntaxException{
		// check if URL is parsable :
		URI uri = uriFromString(url);
		if (uri == null)
			return null;

		return user + "@" + uri.getHost();
	}

	static private URI uriFromString(String url){
		URI uri = null;
		try {
			uri = new URI(url);
			return uri;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return uri;
	}

	
	public String getHost() {
		return uri.getHost();
	}

	public String getProtocol() {
		return uri.getScheme();
	}

	public void setTrustSSL(boolean trust){
		this.trustSSL = trust;
	}
	
	public boolean shouldTrustSSL(){
		return trustSSL;
	}
	
	public void setLegacyServer(boolean legacy){
		this.legacyServer = legacy;		
	}
	
	public boolean isLegacyServer(){
		return this.legacyServer;
	}
	
	public String getUrl() {
		if(!url.endsWith("/")) return url.concat("/");
		return url;
	}

	public void setUrl(String url) {
		try {
			this.uri = new URI(url);
			this.url = url;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public URI getUri() {
		return this.uri;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getID() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public String getIcon() {
		return "mime_empty.png";
	}


	public void setLabel(String srvLabel) {
		this.label = srvLabel;
		
	}

}
