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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.apache.http.util.EncodingUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="a")
public class Node {

	public static int NODE_STATUS_FRESH = 1; 
	public static int NODE_STATUS_LOADING = 2;
	public static int NODE_STATUS_LOADED = 3;
	public static int NODE_STATUS_ERROR = 4;
	
	public static String NODE_TYPE_ROOT = "root";
	public static String NODE_TYPE_SERVER = "server";
	public static String NODE_TYPE_REPOSITORY = "repository";	
	public static String NODE_TYPE_ENTRY = "entry";
	public static String NODE_TYPE_SPECIAL="search";
	
	// id is generated by the database and set on the object automatically
	@DatabaseField(generatedId = true)
	public int id;
	@DatabaseField
	String path;
	@DatabaseField
	String label;
	@DatabaseField
	Date lastModified;
	@DatabaseField
	String resourceType = NODE_TYPE_ENTRY;
	@DatabaseField
	int status = NODE_STATUS_FRESH;
	@DatabaseField
	boolean leaf = false;
	@DatabaseField
	UUID uuid;

	@ForeignCollectionField(eager = false)
	public ForeignCollection<Node> children;
	@ForeignCollectionField(eager = true)
	public ForeignCollection<Property> properties;
	@DatabaseField(foreign = true, index=true)//, columnDefinition="INTEGER REFERENCES `node`(`id`) ON DELETE CASCADE")
	Node parent;
	
	public Node(){
		
	}
	
	public void initFromXmlNode(org.w3c.dom.Node xmlNode){
		if(this.resourceType.equals(NODE_TYPE_REPOSITORY)){
			this.addProperty("icon","repository.png");
			NodeList children = xmlNode.getChildNodes();
			for(int k=0;k<children.getLength();k++){
				if(children.item(k).getNodeName().equals("label")){
					this.label = children.item(k).getTextContent(); 
				}/*else if(children.item(k).getNodeName().equals("client_settings")){
					this.addProperty("icon", children.item(k).getAttributes().getNamedItem("icon").getNodeValue());
				}*/
			}
		}
		NamedNodeMap map = xmlNode.getAttributes();
		for(int i=0;i<map.getLength();i++){
			String name = map.item(i).getNodeName();
			String value = map.item(i).getTextContent();
			if(name == "icon" || name == "openicon"){
				value = value.replace("-",	"_");
			}
			if(this.resourceType.equals(NODE_TYPE_REPOSITORY)){
				if(name.equals("id")){
					this.addProperty("repository_id", value);
				}else if(name.equals("repositorySlug")){
					this.addProperty("slug", value);
				}else {
					this.addProperty(name, value);
				}
			}else{
				if(name.equals("text")){
					this.label = value;
				}else if(name.equals("is_file")){
					if(value.equalsIgnoreCase("true")) {
						this.setLeaf();
					}
				}else if(name.equals("ajxp_modiftime")){
					this.lastModified = new Date(Long.parseLong(value)*1000);
				}else if(name.equals("filename")){
					this.path = value;
				}else{
					this.addProperty(name, value);
				}
			}
		}
		if(getPropertyValue("ajxp_mime") != null  && getPropertyValue("ajxp_mime").equalsIgnoreCase("ajxp_browsable_archive")){
			this.leaf = false;
		}			
		this.uuid = UUID.randomUUID();
		this.setStatus(NODE_STATUS_FRESH);
	}
	
	public Node(String resourceType, String label, Node parentNode){
		this.resourceType = resourceType;
		this.label = label;
		if(parentNode != null){
			this.path = parentNode.getPath().concat("/").concat(label);		
			parent = parentNode;
		}else{
			this.path = "";
		}
	}
	
	public String getPropertyValue(String name){
		if(properties == null) return null;
		try{
			CloseableIterator<Property> it = properties.closeableIterator();
			while(it.hasNext()){
				Property current = it.next();
				if(current.getName().equals(name)) {
					it.close();
					return current.getValue(); 
				}
			}
			it.close();
		}catch(SQLException e){
			
		}
		return null;
	}
	
	public void addProperty(String name, String value){
		Property p = new Property(name, value, this);
		properties.add(p);
	}
	
	public void setProperty(String name, String value){
		if(properties == null) return;
		boolean found = false;
		try{
			CloseableIterator<Property> it = properties.closeableIterator();
			while(it.hasNext()){
				Property current = it.next();
				if(current.getName().equals(name)) {
					current.setValue(value);
					found = true;
					break;
				}
			}
			it.close();
		}catch(SQLException e){
			
		}
		if(!found){
			this.addProperty(name, value);
		}		
	}
	
	
	
	public synchronized void setProperty(String name, String value, RuntimeExceptionDao<Property, Integer>dao){
		if(properties == null) return;
		boolean found = false;
		try{
			CloseableIterator<Property> it = properties.closeableIterator();
			while(it.hasNext()){
				Property current = it.next();
				if(current.getName().equals(name)) {
					current.setValue(value);
					dao.update(current);
					found = true;
					break;
				}
			}
			it.close();
			if(!found){
				this.addProperty(name, value, dao);
			}		
		}catch(SQLException e){
			
		}
	}
	public synchronized void setProperty(String name, String value, Dao<Property, Integer>dao) throws SQLException{
		if(properties == null) return;
		boolean found = false;
		try{
			CloseableIterator<Property> it = properties.closeableIterator();
			while(it.hasNext()){
				Property current = it.next();
				if(current.getName().equalsIgnoreCase(name)) {
					current.setValue(value);
					dao.update(current);
					found = true;
					break;
				}
			}
			it.close();
			if(!found){
				this.addProperty(name, value, dao);
			}		
		}catch(SQLException e){
			//e.printStackTrace();
			throw e;
		}
	}
	public int deleteProperty(String name, RuntimeExceptionDao<Property, Integer> propDao){
		if(properties == null) return 0;
		int count = 0;
		try{
			ArrayList<Property> removed = new ArrayList<Property>();
			CloseableIterator<Property> it = properties.closeableIterator();
			while(it.hasNext()){
				Property current = it.next();
				if(current.getName().equals(name)) {
					propDao.delete(current);
					removed.add(current);
					count++;
				}
			}
			it.close();
			Iterator<Property> i = removed.iterator();
			while(i.hasNext()) properties.remove(i.next());
		}catch(SQLException e){
			e.printStackTrace();
		}		
		return count;
	}
	
	public void recursiveDeleteChildren(RuntimeExceptionDao<Node, Integer> dao){
		for(Node child:children){
			child.recursiveDeleteChildren(dao);
		}
		dao.delete(children);
	}
	
	public synchronized void addProperty(String name, String value, RuntimeExceptionDao<Property, Integer>dao){
		Property p = new Property(name, value, this);
		dao.create(p);
		properties.add(p);
	}
	
	public synchronized void addProperty(String name, String value, Dao<Property, Integer>dao){
		Property p = new Property(name, value, this);
		try {
			dao.create(p);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		properties.add(p);
	}
	
	public boolean isRoot(){
		return (parent == null); 
	}
	
	public boolean isLeaf(){
		return leaf;
	}
	
	public void setLeaf(){
		this.leaf = true;
	}
	
	public String getPath() {
		if(path == null) return "";
		byte bytes[]=EncodingUtils.getBytes(path,"utf-8");
		return new String(bytes);
	}

	public String getPath(boolean skipEncoding) {
		if(skipEncoding) return path;
		if(path == null) return "";
		byte bytes[]=EncodingUtils.getBytes(path,"utf-8");
		return new String(bytes);
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public ForeignCollection<Node> getChildren() {
		return children;
	}

	public void setChildren(ForeignCollection<Node> children) {
		this.children = children;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public boolean isStatus(int compareStatus) {
		return status == compareStatus;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getIsoLabel(){
		byte bytes[]=EncodingUtils.getBytes(label,"iso-8859-1");
		return new String(bytes);
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public Date getLastModified(){
		return lastModified;
	}
	
	public void setLastModified(Date d){
		lastModified = d;
	}
	
	public String toString(){
		StringBuffer bf = new StringBuffer();
		bf.append("Node id : " + id + "\n");
		bf.append("Node path : " + path + "\n");
		if(parent != null) bf.append("Parent Node Id: " + parent.id + "\n");
		if(children != null && children.size()>0){
			bf.append("Children:\n");
			for(Node child : children){
				bf.append("\t" + child + "\n");
			}
		}
		return bf.toString();
	}
	
	public String getUuidPath(){
		return uuid.toString() +(getExtension()!=""?".":"") + getExtension();
	}
	
	public String getIcon() {
		String i = this.getPropertyValue("icon");
		if(i != null) return i;
		else return "mime_empty.png";
	}
		
	public String getExtension() {
	    int dot = getLabel().lastIndexOf(".");
	    if(dot == -1) return "";
	    return getLabel().substring(dot + 1).toLowerCase();
	}
	
	public boolean extensionIn(String ... exts){
		String ext = getExtension();
		for(int i=0;i<exts.length;i++){
			if(ext.equalsIgnoreCase(exts[i])) return true;
		}
		return false;
	}
}
