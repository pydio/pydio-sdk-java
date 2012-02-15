package info.ajaxplorer.client.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName="b")
public class Property {

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
