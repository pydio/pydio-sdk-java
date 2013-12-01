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

import java.io.Serializable;

import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="b")
@IgnoreSizeOf
public class Property implements Serializable {

	@DatabaseField(generatedId=true)
	int id;
	@DatabaseField
	String name;
	@DatabaseField
	String value;
	
	@DatabaseField(foreign=true,index=true)
	Node node;
	
	Property() {
		// For DAO
	}
	
	public Property(String name, String value, Node parentNode){
		this.name = name;
		this.value = value;
		this.node = parentNode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
