package info.ajaxplorer.client.http;

import info.ajaxplorer.client.model.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class RestRequest {

	String httpUser;
	String httpPassword;
	Boolean serversNeedsCredentials = false;
	String authStep = "";
	Boolean loginStateChanged = false;
	MessageListener handler;
	
	public static String STATUS_REFRESHING_AUTH 	= "refreshing_auth";
	public static String STATUS_LOADING_DATA 		= "loading_data";
	public static String STATUS_PARSING_RESPONSE	= "parsing_response";

	public static String AUTH_ERROR_NOSERVER		= "nocurrent_server";
	public static String AUTH_ERROR_LOCKEDOUT		= "locked_out";
	public static String AUTH_ERROR_LOGIN_FAILED	= "login_failed";
	
	public void setHandler(MessageListener handler) {
		this.handler = handler;
	}
	
	public void clearHandler() {
		this.handler = null;
	}

	protected AjxpHttpClient httpClient;

	public RestRequest() {		
		RestStateHolder state = RestStateHolder.getInstance();
		state.registerStateListener(new RestStateHolder.ServerStateListener() {			
			public void onServerChange(Server newServer, Server oldServer) {
				initWithServer(newServer);
				if(oldServer != null && AjxpHttpClient.cookieStore != null){
					String currentHost = oldServer.getHost();
					String currentUser = oldServer.getUser();
					if(newServer.getHost().equals(currentHost) && !newServer.getUser().equals(currentUser)){
						AjxpHttpClient.cookieStore.clear();
					}
				}
			}
		}) ;		
		this.initWithServer(state.getServer());
	}
	
	private void initWithServer(Server server){
		boolean trustSSL  = (server != null && server.shouldTrustSSL());
		if(httpClient != null){
			httpClient.destroy();
		}
		httpClient = new AjxpHttpClient(trustSSL);
		if(server != null){
			setHttpUser(server.getUser());
			setHttpPassword(server.getPassword());
			refreshCredentials();			
		}		
	}

	private CountingMultipartRequestEntity.ProgressListener uploadListener;
	public void setUploadProgressListener(CountingMultipartRequestEntity.ProgressListener uploadList){
		this.uploadListener = uploadList;
	}
	
	private HttpResponse issueRequest(URI uri) throws Exception{
		return this.issueRequest(uri, null, null, null);
	}
	
	@SuppressWarnings("unused")
	private HttpResponse issueRequest(URI uri, Map<String,String> postParameters) throws Exception{
		return this.issueRequest(uri, postParameters, null, null);
	}
	
	private class CustomFileBody extends FileBody{

		private String customFileName;
		public CustomFileBody(File file, String fileName) {
			super(file);
			customFileName = fileName;
		}
		@Override
		public String getFilename(){
			return customFileName;
		}
				
	}

	private HttpResponse issueRequest(URI uri, Map<String,String> postParameters, File file, String fileName) throws Exception {
		URI originalUri = new URI(uri.toString());
		if(RestStateHolder.getInstance().getSECURE_TOKEN() != null){
			uri = new URI(uri.toString().concat("&secure_token=" + RestStateHolder.getInstance().getSECURE_TOKEN()));
		}
		//Log.d("RestRequest", "Issuing request : " + uri.toString());
		HttpResponse response = null;
		try {
			HttpRequestBase request;

			if(postParameters != null || file != null){
				request = new HttpPost();
				if(file != null){
					FileBody bin;
					if(fileName != null) bin = new CustomFileBody(file, fileName);
					else bin = new FileBody(file);
					MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
					reqEntity.addPart("userfile_0", bin);
					if(fileName != null && !EncodingUtils.getAsciiString(EncodingUtils.getBytes(fileName, "US-ASCII")).equals(fileName)){
						reqEntity.addPart("urlencoded_filename", new StringBody(java.net.URLEncoder.encode(fileName, "UTF-8")));
					}
					if(postParameters != null){						
						Iterator<Map.Entry<String, String>> it = postParameters.entrySet().iterator();
						while(it.hasNext()){
							Map.Entry<String, String> entry = it.next();
							reqEntity.addPart(entry.getKey(), new StringBody(entry.getValue()));
						}						
					}
					if(uploadListener != null){
						CountingMultipartRequestEntity countingEntity = new CountingMultipartRequestEntity(reqEntity, uploadListener);
						((HttpPost)request).setEntity(countingEntity);
					}else{
						((HttpPost)request).setEntity(reqEntity);
					}
				}else{
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(postParameters.size());
					Iterator<Map.Entry<String, String>> it = postParameters.entrySet().iterator();
					while(it.hasNext()){
						Map.Entry<String, String> entry = it.next();
						nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
					}
					((HttpPost)request).setEntity(new UrlEncodedFormEntity(nameValuePairs));
				}
			}else{
				request = new HttpGet();
			}
			
			request.setURI(uri);
			if(this.httpUser.length()> 0 && this.httpPassword.length()> 0 ){
				request.addHeader("Ajxp-Force-Login", "true");
			}
			response = httpClient.executeInContext(request);
			if(isAuthenticationRequested(response)){
				sendMessageToHandler(MessageListener.MESSAGE_WHAT_STATE, STATUS_REFRESHING_AUTH);
				this.discardResponse(response);
				this.authenticate();
				if(loginStateChanged){
					// RELOAD
					loginStateChanged = false;
					sendMessageToHandler(MessageListener.MESSAGE_WHAT_STATE, STATUS_LOADING_DATA);					
					return this.issueRequest(originalUri, postParameters, file, fileName);
				}
			}
		} catch (ClientProtocolException e) {
			sendMessageToHandler(MessageListener.MESSAGE_WHAT_ERROR, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			sendMessageToHandler(MessageListener.MESSAGE_WHAT_ERROR, e.getMessage());
			e.printStackTrace();
		} catch (AuthenticationException e){
			sendMessageToHandler(MessageListener.MESSAGE_WHAT_ERROR, e.getMessage());
			throw e;
		} catch (Exception e) {
			sendMessageToHandler(MessageListener.MESSAGE_WHAT_ERROR, e.getMessage());
			e.printStackTrace();
		}finally{
			uploadListener = null;
		}
		return response;
	}

	private void authenticate() throws AuthenticationException{
		loginStateChanged = false;
		AjxpAPI API = AjxpAPI.getInstance();
		try{
			if(authStep.equals("RENEW-TOKEN")){
				
				JSONObject jObject = this.getJSonContent(API.getGetSecureTokenUri());
				RestStateHolder.getInstance().setSECURE_TOKEN(jObject.getString("SECURE_TOKEN"));
				loginStateChanged = true;
				
			}else{
				
				String seed = this.getStringContent(API.getGetLoginSeedUri());
				if(seed != null) seed = seed.trim();
				if(seed.indexOf("captcha") > -1){
					throw new AuthenticationException(AUTH_ERROR_LOCKEDOUT);
				}
				if(!RestStateHolder.getInstance().isServerSet()){
					throw new AuthenticationException(AUTH_ERROR_NOSERVER);
				}
				String user = RestStateHolder.getInstance().getServer().getUser();
				String password = RestStateHolder.getInstance().getServer().getPassword();				
				if(!seed.trim().equals("-1")){
					password = RestRequest.md5(password) + seed;
					password = RestRequest.md5(password);
				}
				Document doc = this.getDocumentContent(API.makeLoginUri(user, password, seed));
				if(doc.getElementsByTagName("logging_result").getLength() > 0){
					String result = doc.getElementsByTagName("logging_result").item(0).getAttributes().getNamedItem("value").getNodeValue();
					if(result.equals("1")){
						//Log.d("RestRequest Authentication", "LOGGING SUCCEED! REFRESHING TOKEN");
						String newToken = doc.getElementsByTagName("logging_result").item(0).getAttributes().getNamedItem("secure_token").getNodeValue();
						RestStateHolder.getInstance().setSECURE_TOKEN(newToken);				
						loginStateChanged = true;
					}else{
						//Log.d("RestRequest Authentication", "Login Failed");
						throw new AuthenticationException(AUTH_ERROR_LOGIN_FAILED);
					}
				}
			
			}
		}catch(AuthenticationException e){
			throw e;
		}catch(Exception e){
			throw new AuthenticationException(e.getMessage());
		}			
	}
	
	private boolean isAuthenticationRequested(HttpResponse response) {
		
		Header[] heads = response.getHeaders("Content-type");
		boolean xml = false;
		for(int i=0;i<heads.length;i++){
			if(heads[i].getValue().contains("text/xml")) xml = true;
		}
		if(!xml || loginStateChanged) return false;
		try{
			HttpEntity ent = response.getEntity();
			Document doc;
			if(ent.getClass() == XMLDocEntity.class){
				doc = ((XMLDocEntity)ent).getDoc();
				((XMLDocEntity)ent).toLogger();
			}else{
				XMLDocEntity docEntity = new XMLDocEntity(ent);
				ent.consumeContent();// Make sure to clear resources
				doc = docEntity.getDoc();
				response.setEntity(docEntity);
				docEntity.toLogger();
			}
			
			if(doc.getElementsByTagName("ajxp_registry_part").getLength() > 0 && doc.getElementsByTagName("repositories").getLength() == 0){
				//Log.d("RestRequest Authentication", "EMPTY REGISTRY : AUTH IS REQUIRED");
				this.authStep = "LOG-USER";
				return true;
			}
			if(doc.getElementsByTagName("require_auth").getLength() > 0){
				//Log.d("RestRequest Authentication", "REQUIRE_AUTH TAG : AUTH IS REQUIRED");
				this.authStep = "LOG-USER";
				return true;
			}
			if(doc.getElementsByTagName("message").getLength() > 0){
				if(doc.getElementsByTagName("message").item(0).getFirstChild().getNodeValue().trim().contains("You are not allowed to access this resource.")){
					//Log.d("RestRequest Authentication", "REQUIRE_AUTH TAG : TOKEN IS REQUIRED");
					this.authStep = "RENEW-TOKEN";
					return true;
				}
			}
		}catch (Exception e) {
			sendMessageToHandler(MessageListener.MESSAGE_WHAT_ERROR, e.getMessage());
			e.printStackTrace();
		}
		return false;
	}	
	
	public String getStringContent(URI uri) throws Exception {
		return this.getStringContent(uri, null, null, null);
	}

	public String getStringContent(URI uri, Map<String, String> parameters) throws Exception {
		return this.getStringContent(uri, parameters, null, null);
	}
	
	public String getStringContent(URI uri, Map<String, String> parameters, File file, String fileName) throws Exception {
		BufferedReader in = null;
		try {
			HttpResponse response = this.issueRequest(uri, parameters, file, fileName);
			if(response == null){
				throw new Exception("Empty Http response");
			}

			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");

			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}

			in.close();

			String page = sb.toString();

			return page;

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public HttpEntity getNotConsumedResponseEntity(URI uri, Map<String, String> params) throws Exception{
		HttpResponse response = this.issueRequest(uri, params, null, null);
		StatusLine status = response.getStatusLine();
		if(status.getStatusCode() != 200){
			throw new Exception("Status code :" + status.getStatusCode());
		}
		return response.getEntity();
	}

	public Document getDocumentContent(URI uri) throws Exception {

		HttpResponse response = this.issueRequest(uri);
		
		HttpEntity ent = response.getEntity();
		Document doc;
		sendMessageToHandler(MessageListener.MESSAGE_WHAT_STATE, STATUS_PARSING_RESPONSE);
		if(ent.getClass() == XMLDocEntity.class){
			doc = ((XMLDocEntity)ent).getDoc();
		}else{
			XMLDocEntity docEntity = new XMLDocEntity(ent);
			ent.consumeContent();
			doc = docEntity.getDoc();
			response.setEntity(docEntity);
		}			
		return doc;
	}

	public JSONObject getJSonContent(URI uri) throws Exception {
		return new JSONObject(this.getStringContent(uri));
	}

	/**
	 * @param uri
	 *            Target uri that might require http basic auth
	 * @return true or false depending if the target uri requires http basic
	 *         auth
	 */
	public Boolean credentialsRequired(URI uri) {

		Boolean credentialsRequired = false;
		try {
			int status = getStatusCodeForRequest(uri);
			if (status == 401) {
				credentialsRequired = true;
				serversNeedsCredentials = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return credentialsRequired;
	}

	public Boolean credentialsRequired() {
		return serversNeedsCredentials;
	}

	public void parseDocumentMessage(Document doc){
		try{
			parseDocumentMessage(doc, false);
		}catch(Exception e){}
	}
	
	public void parseDocumentMessage(Document doc, boolean throwOnError) throws Exception{
		if(doc.getElementsByTagName("message").getLength() > 0){
			Node node = doc.getElementsByTagName("message").item(0);
			String type = node.getAttributes().getNamedItem("type").getNodeValue();
			String message = node.getTextContent();
			if(type.equalsIgnoreCase("error")){
				if(throwOnError){
					throw new Exception(message);
				}
				sendMessageToHandler(MessageListener.MESSAGE_WHAT_ERROR, message);
			}else{
				sendMessageToHandler(MessageListener.MESSAGE_WHAT_STATE, message);
			}
		}		
	}
	
	
	public String getHttpUser() {
		return httpUser;
	}

	public void setHttpUser(String httpUser) {
		this.httpUser = httpUser;
		//setAuthenticator();
	}

	public String getHttpPassword() {
		return httpPassword;
	}

	public void setHttpPassword(String httpPassword) {
		this.httpPassword = httpPassword;
		//setAuthenticator();
	}

	public void refreshCredentials(){
		this.getHttpClient().refreshCredentials(httpUser, httpPassword);
	}
	
	/*
	public void setAuthenticator() {
		if (httpPassword != null && httpUser != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(httpUser, httpPassword.toCharArray());
				}
			});
		}
	}
*/

	public int getStatusCodeForRequest(URI uri) {
		HttpResponse response;
		try {
			response = issueRequest(uri);
			if (response != null) {
				int status = response.getStatusLine().getStatusCode();

				// we need to read the whole response-body stream if we want to
				// avoid problems with the SingleConnexionManager
				discardResponse(response);

				return status;
			} else {
				return -1;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}

	}

	public void discardResponse(HttpResponse response) {
		try {
			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer sb = new StringBuffer("");
			String line = "";
			String NL = System.getProperty("line.separator");
			while ((line = in.readLine()) != null) {
				sb.append(line + NL);
			}
			in.close();
		} catch (IllegalStateException e){
			// Silent, was already consumed
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized AjxpHttpClient getHttpClient() {
		return httpClient;
	}
	
	public static final String md5(final String s) {
	    try {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest
	                .getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	 
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i = 0; i < messageDigest.length; i++) {
	            String h = Integer.toHexString(0xFF & messageDigest[i]);
	            while (h.length() < 2)
	                h = "0" + h;
	            hexString.append(h);
	        }
	        return hexString.toString();
	 
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return "";
	}
	
	protected void sendMessageToHandler(int messageType, Object obj){
		if(handler == null) return;
		handler.sendMessage(messageType, obj);
	}
	
}