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
 *  
 */
package info.ajaxplorer.client.http;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class PydioClient {
	
	
	
	String SECURE_TOKEN;
	String server_url;
	String content_php = "index.php?";
	Document xml_registry;
	JSONObject dictionary;
	private PydioClient() {
		RestStateHolder stateHolder = RestStateHolder.getInstance();
		if(stateHolder.isServerSet()){
			this.setServer(stateHolder.getServer());
		}
		stateHolder.registerStateListener(new RestStateHolder.ServerStateListener() {			
			public void onServerChange(Server newServer, Server oldServer) {
				PydioClient.this.setServer(newServer);
			}
		});
	}
	
	private static PydioClient instance;
	public static PydioClient getInstance(){
		if(instance == null) instance = new PydioClient();
		return instance;
	}
			
	public static URI returnUriFromString(String url) throws URISyntaxException{
		URI uri = null;
		try {
			uri = new URI(url);
		} catch (Exception e) {
		}
		return uri;
	}

	private String getAPIUrl() {
		String url = server_url.concat(content_php);
		return url;
	}

	public URI getAPIUri() throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getAPIUrl().concat("&get_action=ping"));
	}
	
	/**
	 * Ping the server to make sure secure token is up-to-date
	 * Then append secure_token and Cookie value as ajxp_sessid parameter
	 * @param uri
	 * @param rest RestRequest to perform ping operation
	 * @return RESTFul Url that can be used by an external client (no need to cookie)
	 */
	public String pingAndRestifyUri(URI uri, boolean skipPing, RestRequest rest) throws URISyntaxException{
		if(!skipPing && rest != null) {
			rest.getStatusCodeForRequest(getAPIUri());
		}
		String url = uri.toString().concat("&secure_token="+RestStateHolder.getInstance().getSECURE_TOKEN());		
		Iterator<Cookie> it = AjxpHttpClient.getCookies(uri).iterator();
		while(it.hasNext()){
			Cookie cook = it.next();
			if(cook.getName().equals("AjaXplorer")){
				url = url.concat("&ajxp_sessid=" + cook.getValue());
			}else{
				url = url.concat("&AJXP_COOK_"+cook.getName() + "=" + cook.getValue());
			}
		}
		return url;
	}
	
	public void enrichConnexionWithCookies(URLConnection connexion){
		try{
			URI uri = new URI(connexion.getURL().toString());
	        List<Cookie> cookies = AjxpHttpClient.getCookies(uri);
	        String cookieString = "";
	        for (int i=0;i<cookies.size();i++) {
	        	String cs = "";
	        	if (i>0) {
	        		cs += "; ";
	        	}
	        	cs += cookies.get(i).getName();
	        	cs += "=";
	        	cs += cookies.get(i).getValue();
	        	cookieString+= cs;
	        }
	    	connexion.setRequestProperty("Cookie", cookieString);				
		}catch(Exception e){
			
		}
	}	

	private String getGetActionUrl(String action) {
		return this.getGetActionUrl(action, true);
	}

	private String getGetActionUrl(String action, boolean appendRepositoryId) {
		if(!appendRepositoryId){
			return this.getAPIUrl().concat("get_action=").concat(action).concat("&");
		}else{
			return this.getAPIUrl().concat("get_action=").concat(action).concat("&tmp_repository_id="+RestStateHolder.getInstance().getRepository().getPropertyValue("repository_id")+"&");			
		}
	}
	
	
	
	public URI getGetActionUri(String action) throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getGetActionUrl(action));
	}

	public URI getGetSecureTokenUri() throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getGetActionUrl("get_boot_conf", false));
	}

	public URI getGetLoginSeedUri() throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getGetActionUrl("get_seed", false));
	}
	
	public URI makeLoginUri() throws URISyntaxException{
		String base = this.getGetActionUrl("login", false);
		return AjxpAPI.returnUriFromString(base);
	}
	
	public URI getGetXmlRegistryUri() throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getGetActionUrl("get_xml_registry", false).concat("xPath=user/repositories"));
	}
	
	public URI getXmlPluginsRegistryUri() throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getGetActionUrl("get_xml_registry", false).concat("xPath=plugins"));
	}

	private String getLsRepositoryUrl() throws URISyntaxException{
		String ret = getGetActionUrl("ls");
		return ret.concat("dir=%2F");
	}

	public URI getLsRepositoryUri() throws URISyntaxException{
		return returnUriFromString(getLsRepositoryUrl());
	}

	public URI getRecursiveLsDirectoryUri(Node directory) throws URISyntaxException{
		return returnUriFromString(getDirectoryUrl(directory, true));
	}

	public URI getLsDirectoryUri(Node directory) throws URISyntaxException{
		return returnUriFromString(getDirectoryUrl(directory));
	}

	private String getDirectoryUrl(Node directory) {
		return getDirectoryUrl(directory, false);
	}
	
	public URI getPingUri() throws URISyntaxException{
		return returnUriFromString(getGetActionUrl("ping"));
	}
	
	private String getDirectoryUrl(Node directory, boolean recursive) {
		if (directory == null)
			return "";

		String url ="";
		String path = directory.getPath();
		if(directory.getResourceType().equals(Node.NODE_TYPE_REPOSITORY)){
			path = "/";
		}
		try {
			if(recursive){
				url = getGetActionUrl("ls") + "options=a&dir="
						+ java.net.URLEncoder.encode(path, "UTF-8") + "&recursive=true";
			}else{
				url = getGetActionUrl("ls") + "options=al&dir="
						+ java.net.URLEncoder.encode(path, "UTF-8");				
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if(directory.getPropertyValue("pagination:target") != null){
			try {
				url = url.concat(java.net.URLEncoder.encode("%23"+directory.getPropertyValue("pagination:target"), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return url;
	}

	public Document getXmlRegistry() {
		return xml_registry;
	}

	public void setXmlRegistry(Document xmlRegistry) {
		xml_registry = xmlRegistry;
	}

	public URI getSwitchRepositoryUri(String id) throws URISyntaxException{
		return returnUriFromString(getSwitchRepositoryUrl(id));
	}

	private String getSwitchRepositoryUrl(String id) {
		return getGetActionUrl("switch_repository", false).concat(
				"repository_id=" + id);
	}
	
	public void setServer (Server s) {
		if (s!=null) {
			this.server_url = s.getUrl();
			this.content_php = s.isLegacyServer()?"content.php?":"index.php?";
		}
	}
	
	public URI getRawContentUri(String file) throws URISyntaxException{
		String ret=getGetActionUrl("get_content");
		try{
			String url = ret+"file="+java.net.URLEncoder.encode(file, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}	
	}
	
	public URI getSetRawContentPostUri(String file) throws URISyntaxException{
		String ret=getGetActionUrl("put_content");
		try{
			String url = ret+"file="+java.net.URLEncoder.encode(file, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}				
	}
	
	public URI getDeleteUri(String path) throws URISyntaxException{
		String url=getGetActionUrl("delete");
		try{
			url = url.concat("dir="+java.net.URLEncoder.encode(RestStateHolder.getInstance().getDirectory().getPath(true), "UTF-8"));
			url = url.concat("&file="+java.net.URLEncoder.encode(path, "UTF-8"));
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}

	public URI getUploadUri(String targetFolder) throws URISyntaxException{
		String url=getGetActionUrl("upload");
		try{
			url = url.concat("dir="+java.net.URLEncoder.encode(targetFolder, "UTF-8")+"&xhr_uploader=true");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}

	public URI getDownloadUri(String file) throws URISyntaxException{
		String ret=getGetActionUrl("download");
		try{
			String url = ret+"file="+java.net.URLEncoder.encode(file, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getMediaStreamUri(String file) throws URISyntaxException{
		String ret=getGetActionUrl("get_content");
		try{
			String url = ret+"file="+java.net.URLEncoder.encode(file, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getImageDataUri(String file, boolean getThumb, int thumbDimension) throws URISyntaxException{
		String ret=getGetActionUrl("preview_data_proxy");
		try{
			String url = ret+"file="+java.net.URLEncoder.encode(file, "UTF-8");
			if(getThumb){
				url = url.concat("&get_thumb=true");
				if(thumbDimension != -1){
					url = url.concat("&dimension="+thumbDimension);
				}
			}			
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getStatUri(String path) throws URISyntaxException{
		String url=getGetActionUrl("stat");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(path, "UTF-8"));
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
		
	public URI getMkdirUri(String file) throws URISyntaxException{
		String url=getGetActionUrl("mkdir");
		try{
			url = url.concat("dir="+java.net.URLEncoder.encode(RestStateHolder.getInstance().getDirectory().getPath(true), "UTF-8"));
			url = url+"&dirname="+java.net.URLEncoder.encode(file, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getMkfileUri(String file) throws URISyntaxException{
		String url=getGetActionUrl("mkfile");
		try{
			url = url.concat("dir="+java.net.URLEncoder.encode(RestStateHolder.getInstance().getDirectory().getPath(true), "UTF-8"));
			url = url+"&filename="+java.net.URLEncoder.encode(file, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getRenameUri(Node targetNode, String newName) throws URISyntaxException{
		String url=getGetActionUrl("rename");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(targetNode.getPath(true), "UTF-8"));
			url = url+"&filename_new="+java.net.URLEncoder.encode(newName, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getRenameUri(Node targetNode, Node destNode) throws URISyntaxException{
		String url=getGetActionUrl("rename");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(targetNode.getPath(true), "UTF-8"));
			url = url+"&dest="+java.net.URLEncoder.encode(destNode.getPath(true), "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getShareLinkUri(Node targetNode, int expiration, String password) throws URISyntaxException{
		String url=getGetActionUrl("share");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(targetNode.getPath(true), "UTF-8"));
			if(expiration != 0){
				url = url.concat("&expiration=" + expiration);
			}
			if(!password.equals("")){
				url = url.concat("&password=" + password);
			}
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			return null;
		}
	}
	
	public URI getShareLinkUri(Node targetNode, int expiration, String password, int downloadlimit) throws URISyntaxException{
		String url=getGetActionUrl("share");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(targetNode.getPath(true), "UTF-8"));
			if(expiration != 0){
				url = url.concat("&expiration=" + expiration);
			}
			if(!password.equals("")){
				url = url.concat("&password=" + password);
			}
			
			if(downloadlimit != 0){
				url = url.concat("&downloadlimit=" + downloadlimit);
			}			
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getMoveUri( Node from, Node to) throws URISyntaxException{

		try{
			String ret=getGetActionUrl("move");
			ret += "file="+java.net.URLEncoder.encode(from.getPath(true), "UTF-8");
			ret += "&dest="+java.net.URLEncoder.encode(to.getPath(true), "UTF-8");
			return returnUriFromString(ret);
		}catch(UnsupportedEncodingException e){
			return null;
		}
	}
	
	public URI getCopyUri( Node from, Node to) throws URISyntaxException{

		try{
			String ret=getGetActionUrl("copy");
			ret += "file="+java.net.URLEncoder.encode(from.getPath(true), "UTF-8");
			ret += "&dest="+java.net.URLEncoder.encode(to.getPath(true), "UTF-8");
			return returnUriFromString(ret);
		}catch(UnsupportedEncodingException e){
			return null;
		}
	}
	public URI getSearchUri( String name) throws URISyntaxException{

		try{
			String ret=getGetActionUrl("search");
			ret += "query="+java.net.URLEncoder.encode(name, "UTF-8");
			return returnUriFromString(ret);
		}catch(UnsupportedEncodingException e){
			return null;
		}
	}
	public URI getFilehashDeltaUri(Node node)throws URISyntaxException {
		
		String url=getGetActionUrl("filehasher_delta");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(node.getPath(true), "UTF-8"));
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
		
		
	}
	public URI getFilehashSignatureUri(Node node)throws URISyntaxException {
		
		String url=getGetActionUrl("filehasher_signature");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(node.getPath(true), "UTF-8"));
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
		
		
	}	
	public URI getFilehashPatchUri(Node node)throws URISyntaxException {
		
		String url=getGetActionUrl("filehasher_patch");
		try{
			url = url.concat("file="+java.net.URLEncoder.encode(node.getPath(true), "UTF-8"));
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}	
		
	}
	
	
	public Object applyAction(String method, String[] names, String[] types, Object[] values) throws Exception{	
		
		String rules = "{\"listNodes\": {\"action\": \"ls\",\"params\": {\"dir\": {\"name\": \"dir\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"},\"file\": {\"name\": \"file\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"},\"nodes\": {\"name\": \"nodes\",\"type\": \"AJXP_NODE[]\",\"mandatory\": false,\"default\": \"\"},\"options\": {\"name\": \"options\",\"type\": \"string\",\"mandatory\": false,\"default\": \"al\"},\"recursive\": {\"name\": \"recursive\",\"type\": \"boolean\",\"mandatory\": false,\"default\": \"false\"},\"remote_order\": {\"name\": \"remote_order\",\"type\": \"boolean\",\"mandatory\": false,\"default\": \"false\"},\"order_column\": {\"name\": \"order_column\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"},\"order_direction\": {\"name\": \"order_direction\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"}},\"output\": \"AJXP_NODE[]\"},\"upload\": {\"action\": \"upload\",\"params\": {		\"node\": {\"name\": \"node\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"}},\"output\": \"xml\"},\"download\": {\"action\": \"download\",\"params\": {\"nodes\": {\"name\": \"nodes\",\"type\": \"AJXP_NODE[]\",\"mandatory\": true,\"default\": \"\"}},\"output\": \"stream\"},\"mkdir\": {\"action\": \"mkdir\",\"params\": {\"dir\": {\"name\": \"dir\",\"type\": \"AJXP_NODE\",\"mandatory\": true,\"default\": \"\"},\"dirname\": {\"name\": \"dirname\",\"type\": \"string\",\"mandatory\": true,\"default\": \"\"}},\"output\": \"xml\"},\"mkfile\": {\"action\": \"mkfile\",\"params\": {\"node\": {\"name\": \"node\",\"type\": \"AJXP_NODE\",\"mandatory\": false,\"default\": \"\"}},\"output\": \"xml\"},\"search\": {\"action\": \"search\",\"params\": {\"query\": {\"name\": \"query\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"}},\"output\": \"xml\"},\"searchByKeyword\": {\"action\": \"search_by_keyword\",\"params\": {\"query\": {\"name\": \"query\",\"type\": \"string\",\"mandatory\": false,\"default\": \"\"}},\"output\": \"xml\"}}";

		if(dictionary == null){
	        dictionary = new JSONObject(rules.replaceAll("\n", "\\n"));
		}
		if(!dictionary.has(method)){
			return null;
		}
		
		JSONObject m = dictionary.getJSONObject(method);
		String action = (String) m.get("action");
		JSONObject action_params = m.getJSONObject("params");
		String output = (String) m.get("output");
		
		int i = 0;
		Iterator it = action_params.keys();
		try{
			while(it.hasNext()){
				JSONObject param = action_params.getJSONObject((String)it.next());
				if((Boolean)param.get("mandatory")){
					i++;
				}
			}
		}catch(Exception e){
			
		}
		
		if(i > 0 && (values == null || types == null) || values.length < i || types.length < i) return null;
		
		
		Map<String, String> post_params = new HashMap<String, String>();
		post_params.put("action", action);
		
		
		i = 0;
		it = action_params.keys();
		while(it.hasNext()){			
			JSONObject param = action_params.getJSONObject((String)it.next());
			String name = (String) param.get("name");
			String type = (String) param.get("type");
			
			
			
			if(types[i].equals(type) && names[i].equals(name)){	
				if(type.equals("AJXP_NODE")){
					post_params.put(name, ((Node) values[i]).getPath());
				}if(type.equals("AJXP_NODE[]")){
					//
				}else{
					if(((String)values[i]).equals("")){
						post_params.put("action", "get_xml_registry");
						post_params.put("xPath", "user/repositories");
					}else{				
						post_params.put(name, (String)values[i]);
					}
				}
				i++;
				if(names.length - 1 < i) break;
			}else{
				
			}
			
		}	
		
		RestRequest rest = new RestRequest();
		URI uri = AjxpAPI.returnUriFromString(getAPIUrl());
		
		
		if(output.equals("xml")){
			return rest.getDocumentContent(uri, post_params);	
			
		}else if(output.equals("AJXP_NODE[]")){
			
			Document doc = rest.getDocumentContent(uri, post_params);
			NodeList children = doc.getDocumentElement().getChildNodes();
			
			
			ArrayList<Node> nodes = new ArrayList<Node>();
			
			if(children.getLength() != 0){				
				NodeList repos = doc.getDocumentElement().getElementsByTagName("repositories");				
				if(repos.getLength() != 0){	
					//get repositoriy list
					repos = repos.item(0).getChildNodes();					
					for (int j = 0; j < repos.getLength(); j++) {
						org.w3c.dom.Node xmlNode = repos.item(j);
						Node repository = new Node(Node.NODE_TYPE_REPOSITORY, "", null);
						repository.initFromXmlNode(xmlNode);
							nodes.add(repository);
					}
				}
				
				if(doc.getDocumentElement().getNodeName().equals("tree")){
					//dir list					
					NodeList entries = doc.getChildNodes();
					for(int j=0; j< entries.getLength(); j++){
						org.w3c.dom.Node xmlNode = entries.item(j);
						Node ajxpNode = new Node(Node.NODE_TYPE_ENTRY, "", null);
						ajxpNode.initFromXmlNode(xmlNode);
						nodes.add(ajxpNode);
					}
				}
			}
				
			return nodes;
			
		}else if(output.equals("json")){
			return rest.getJSonContent(uri, post_params);
		}else if(output.equals("stream")){
			
		}
		
		return null;
	}
	
}