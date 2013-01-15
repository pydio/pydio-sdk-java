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

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLDocEntity extends BasicHttpEntity {
	
	private Document doc;
	private SAXException currentE;

	public XMLDocEntity(HttpEntity notConsumedEntity) throws ParserConfigurationException, IOException, SAXException
	{
		ErrorHandler myErrorHandler = new ErrorHandler()
		{
		    public void fatalError(SAXParseException exception)
		    throws SAXException
		    {
		        System.err.println("fatalError: " + exception);
		    }
		    
		    public void error(SAXParseException exception)
		    throws SAXException
		    {
		        System.err.println("error: " + exception);
		    }

		    public void warning(SAXParseException exception)
		    throws SAXException
		    {
		        System.err.println("warning: " + exception);
		    }

		};

		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();			
		DocumentBuilder db = dbf.newDocumentBuilder();
		db.setErrorHandler(myErrorHandler);
		try{
			doc = db.parse(notConsumedEntity.getContent());
		}catch(SAXException e){
			this.currentE = e;
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public Document getDoc() throws SAXException {
		if(this.currentE != null) {
			throw this.currentE;
		}
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
			return;
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
			System.out.println(xmlString);
		}catch(Exception e){
		}
		*/
	}
	
}
