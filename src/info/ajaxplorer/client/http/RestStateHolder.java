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
package info.ajaxplorer.client.http;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;

import java.util.ArrayList;
import java.util.Iterator;

public class RestStateHolder {

	public static final int FILE_UPLOAD_CHUNK_1K = 1024;
	public static final int FILE_UPLOAD_CHUNK_8K = 1024 * 8;
	public static final int FILE_UPLOAD_CHUNK_16K = 1024 * 16;

	private static RestStateHolder instance;
	
	private Server server;
	private Node repository;
	private Node directory;
	private String SECURE_TOKEN;

	// upload file chunk size
	private int fileUploadChunkSize = FILE_UPLOAD_CHUNK_1K;

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
	public interface ServerStateResolutionListener extends StateListener{
		public void onServerChangeResolution(Server server);
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
   
	public void setServer(Server serverToSet) {
		
		if(serverToSet == null){
			return;
		}
		
		Server oldServer = null;
		if(this.server != null && this.server != serverToSet){
			oldServer = this.server;
		}
		this.server = serverToSet;
		Iterator<StateListener> it = listeners.iterator();
		while(it.hasNext()){
			StateListener l = it.next();
			if(l instanceof ServerStateListener){
				((ServerStateListener)l).onServerChange(serverToSet, oldServer);
			}
		}
	}

	public void notifyServerChanged(Server server) {
		
		Iterator<StateListener> it = listeners.iterator();
		while(it.hasNext()){
			StateListener l = it.next();
			if(l instanceof ServerStateResolutionListener){
				((ServerStateResolutionListener)l).onServerChangeResolution(server);
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
	
	/**
	 * Sets chunk size for AjxpFileBody during upload
	 * 
	 * @param fileUploadChunkSize
	 */
	public void setFileUploadChunkSize(int fileUploadChunkSize) {
		this.fileUploadChunkSize = fileUploadChunkSize;
	}
	
	public int getFileUploadChunkSize() {
		return fileUploadChunkSize;
	}

}
