package info.ajaxplorer.client.http;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XMLDocEntity extends BasicHttpEntity {
	
	private Document doc;

	public XMLDocEntity(HttpEntity notConsumedEntity) throws ParserConfigurationException, IOException, SAXException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();			
		DocumentBuilder db = dbf.newDocumentBuilder();
		doc = db.parse(notConsumedEntity.getContent());		
	}
	
	public Document getDoc() {
		return doc;
	}

	public void setDoc(Document doc) {
		this.doc = doc;
	}

	@Override
	public boolean isRepeatable(){
		return true;
	}
	
	@Override
	public boolean isStreaming(){
		return false;
	}
	
	public void toLogger(){
		if(true){
			//return;
		}
		/*
		try{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
	
			String xmlString = result.getWriter().toString();
		}catch(Exception e){
		}
		*/
	}
	
}
