package info.ajaxplorer.client.http;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.w3c.dom.Document;

public class AjxpAPI {

	String SECURE_TOKEN;
	String server_url;
	String content_php = "?";
	Document xml_registry;

	private AjxpAPI() {
		RestStateHolder stateHolder = RestStateHolder.getInstance();
		if(stateHolder.isServerSet()){
			this.setServer(stateHolder.getServer());
		}
		stateHolder.registerStateListener(new RestStateHolder.ServerStateListener() {			
			public void onServerChange(Server newServer, Server oldServer) {
				AjxpAPI.this.setServer(newServer);
			}
		});
	}
	private static AjxpAPI instance;
	public static AjxpAPI getInstance(){
		if(instance == null) instance = new AjxpAPI();
		return instance;
	}
			
	public static URI returnUriFromString(String url) throws URISyntaxException{
		URI uri = null;
		try {
			uri = new URI(url);
			return uri;
		} catch (Exception e) {
			e.printStackTrace();
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
			return this.getAPIUrl().concat("get_action=").concat(action)
				.concat("&");
		}else{
			return this.getAPIUrl().concat("get_action=").concat(action)
					.concat("&tmp_repository_id="+RestStateHolder.getInstance().getRepository().getPropertyValue("repository_id")+"&");			
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
	
	public URI makeLoginUri(String userid, String password, String seed) throws URISyntaxException{
		String base = this.getGetActionUrl("login", false).concat("userid="+userid+"&password="+password+"&login_seed="+seed);
		return AjxpAPI.returnUriFromString(base);
	}
	
	public URI getGetXmlRegistryUri() throws URISyntaxException{
		return AjxpAPI.returnUriFromString(this.getGetActionUrl("get_xml_registry", false).concat("xPath=user/repositories"));
	}

	private String getLsRepositoryUrl() throws URISyntaxException{
		String ret = getGetActionUrl("ls");
		return ret.concat("dir=%2F");
	}

	public URI getLsRepositoryUri() throws URISyntaxException{
		return returnUriFromString(getLsRepositoryUrl());
	}

	public URI getLsDirectoryUri(Node directory) throws URISyntaxException{
		return returnUriFromString(getDirectoryUrl(directory));
	}

	private String getDirectoryUrl(Node directory) {
		if (directory == null)
			return "";

		String url ="";
		String path = directory.getPath();
		if(directory.getResourceType().equals(Node.NODE_TYPE_REPOSITORY)){
			path = "/";
		}
		try {
			url = getGetActionUrl("ls") + "options=al&dir="
					+ java.net.URLEncoder.encode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
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
	
	protected void setServer (Server s) {
		if (s!=null) {
			this.server_url = s.getUrl();
			this.content_php = s.isLegacyServer()?"content.php?":"?";
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
			url = url.concat("dir="+java.net.URLEncoder.encode(RestStateHolder.getInstance().getDirectory().getPath(), "UTF-8"));
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
			url = url.concat("dir="+java.net.URLEncoder.encode(targetFolder, "UTF-8"));
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
	
	public URI getImageDataUri(String file, boolean getThumb) throws URISyntaxException{
		String ret=getGetActionUrl("preview_data_proxy");
		try{
			String url = ret+"file="+java.net.URLEncoder.encode(file, "UTF-8");
			if(getThumb){
				url = url.concat("&get_thumb=true");
			}
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	
	public URI getMkdirUri(String file) throws URISyntaxException{
		String url=getGetActionUrl("mkdir");
		try{
			url = url.concat("dir="+java.net.URLEncoder.encode(RestStateHolder.getInstance().getDirectory().getPath(), "UTF-8"));
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
			url = url.concat("dir="+java.net.URLEncoder.encode(RestStateHolder.getInstance().getDirectory().getPath(), "UTF-8"));
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
			url = url.concat("file="+java.net.URLEncoder.encode(targetNode.getPath(), "UTF-8"));
			url = url+"&filename_new="+java.net.URLEncoder.encode(newName, "UTF-8");
			return returnUriFromString(url);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public URI getMoveUri( Node from, Node to) throws URISyntaxException{

		try{
			String ret=getGetActionUrl("move");
			ret += "file="+java.net.URLEncoder.encode(from.getPath(), "UTF-8");
			ret += "&dir="+java.net.URLEncoder.encode(from.getParent().getPath(), "UTF-8");
			ret += "&dest_node="+java.net.URLEncoder.encode(to.getPath(), "UTF-8");
			ret += "&dest="+java.net.URLEncoder.encode(to.getPath(), "UTF-8");
			ret += "&dest_repository_id="+RestStateHolder.getInstance().getRepository().getPropertyValue("repository_id");
			return returnUriFromString(ret);
		}catch(UnsupportedEncodingException e){
			return null;
		}
	}
}