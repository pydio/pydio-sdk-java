package info.ajaxplorer.client.http;

import java.util.ArrayList;
import java.util.Iterator;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;

public class RestStateHolder {

	private static RestStateHolder instance;
	
	private Server server;
	private Node repository;
	private Node directory;
	private String SECURE_TOKEN;

	private ArrayList<StateListener> listeners = new ArrayList<RestStateHolder.StateListener>();
	
	private RestStateHolder(){
		
	}
	
	public static RestStateHolder getInstance(){
		if(instance == null){
			instance = new RestStateHolder();
		}
		return instance;
	}
	
	public interface StateListener{
	}
	public interface ServerStateListener extends StateListener{
		public void onServerChange(Server newServer, Server oldServer);
	}
	public interface RepositoryStateListener extends StateListener{
		public void onRepositoryChange(Node newRepository, Node oldRepository);
	}
	public interface DirectoryStateListener extends StateListener{
		public void onDirectoryChange(Node newDirectory, Node oldDirectory);
	}

	public void registerStateListener(StateListener listener){
		this.listeners.add(listener);
	}

	public void unRegisterStateListener(StateListener listener){
		this.listeners.remove(listener);
	}

	public boolean isServerSet(){
		return server != null;
	}
	
	public Server getServer() {
		return server;
	}
	
	public String getSECURE_TOKEN() {
		return SECURE_TOKEN;
	}

	public void setSECURE_TOKEN(String token) {
		SECURE_TOKEN = token;
	}	

	public void setServer(Server currentServer) {
			
		Server oldServer = null;
		if(this.server != null && this.server != currentServer){
			oldServer = this.server;
		}
		this.server = currentServer;
		Iterator<StateListener> it = listeners.iterator();
		while(it.hasNext()){
			StateListener l = it.next();
			if(l instanceof ServerStateListener){
				((ServerStateListener)l).onServerChange(currentServer, oldServer);
			}
		}
	}

	public boolean isRepositorySet(){
		return repository != null;
	}
	
	public Node getRepository() {
		return repository;
	}

	public void setRepository(Node currentRepository) {
		Node oldRepo = null;
		if(this.repository != null && this.repository != currentRepository){
			oldRepo = this.repository;
		}
		this.repository = currentRepository;
		Iterator<StateListener> it = listeners.iterator();
		while(it.hasNext()){
			StateListener l = it.next();
			if(l instanceof RepositoryStateListener){
				((RepositoryStateListener)l).onRepositoryChange(currentRepository, oldRepo);
			}
		}
	}
	
	public boolean isDirectorySet(){
		return directory != null;
	}

	public Node getDirectory() {
		return directory;
	}

	public void setDirectory(Node currentDirectory) {
		Node oldDirectory = null;
		if(this.directory != null && this.directory != currentDirectory){
			oldDirectory = this.directory;
		}
		this.directory = currentDirectory;
		Iterator<StateListener> it = listeners.iterator();
		while(it.hasNext()){
			StateListener l = it.next();
			if(l instanceof DirectoryStateListener){
				((DirectoryStateListener)l).onDirectoryChange(currentDirectory, oldDirectory);
			}
		}
		
	}
	
	
}
