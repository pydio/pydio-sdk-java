package info.ajaxplorer.client.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
		this.password = serverNode.getPropertyValue("password");
		if(serverNode.getPropertyValue("trust_ssl") != null){
			this.trustSSL = Boolean.parseBoolean(serverNode.getPropertyValue("trust_ssl"));
		}
		if(serverNode.getPropertyValue("legacy_server") != null){
			this.legacyServer = Boolean.parseBoolean(serverNode.getPropertyValue("legacy_server"));
		}
		this.id = Server.slugifyId(user, url);		
		this.uri = Server.uriFromString(url);
	}
	
	public Node createDbNode(RuntimeExceptionDao<Node, Integer> nodeDao){
		Node n = new Node(Node.NODE_TYPE_SERVER, this.getLabel(), null);
		nodeDao.create(n);
		n.properties = nodeDao.getEmptyForeignCollection("properties");
		n.addProperty("url", this.getUrl());
		n.addProperty("user", this.getUser());
		n.addProperty("password", this.getPassword());
		n.addProperty("trust_ssl", Boolean.toString(trustSSL));
		n.addProperty("legacy_server", Boolean.toString(legacyServer));
		this.setServerNode(n);
		return n;		
	}
	
	public void updateDbNode(RuntimeExceptionDao<Node, Integer> nodeDao, RuntimeExceptionDao<Property, Integer> propertyDao){
		Node n = this.getServerNode();
		for(Property p:n.properties){
			if(p.getName().equals("url")) p.setValue(this.getUrl());
			else if(p.getName().equals("user")) p.setValue(this.getUser());
			else if(p.getName().equals("password")) p.setValue(this.getPassword());
			else if(p.getName().equals("trust_ssl")) p.setValue(Boolean.toString(trustSSL));
			else if(p.getName().equals("legacy_server")) p.setValue(Boolean.toString(legacyServer));			
			propertyDao.update(p);
		}
		if(!n.getLabel().equals(this.label)){
			n.setLabel(this.label);
			nodeDao.update(n);
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
