package info.ajaxplorer.client.http;

import info.ajaxplorer.client.model.Node;
import info.ajaxplorer.client.model.Server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


public class EndPointResolverApi {
	
	String auth;
	public static String STATUS_ERROR;
	public static String STATUS_ERROR_LABEL;
	
	public static String SERVER_URL_RESOLUTION 	= "RequestResolution";	
	
	public static String STATUS_OFFLINE = "Endpoint_server_offline";
	public static String STATUS_TMP_OFFLINE = "Endpoint_server_temporary_offline";
	public static String STATUS_UNKOWN = "Endpoint_error_unkown";
	
	public static String STATUS_ERROR_AUTH = "Endpoint_login_failed";
	public static String STATUS_ERROR_METHOD="Endpoint_Method_not_implemented";
	public static String STATUS_ERROR_WRONG_ARG="Endpoint_wrong_method_arguments";
	public static String STATUS_ERROR_INT="Endpoint_internal_error";
	public static String STATUS_ERROR_NOT_FIND="Endpoint_cannot_find_specified_endpoint";
	
	
     public EndPointResolverApi(){
		
		
	    }
     
     
     public static boolean checkResolutionRequired(URI uri)
 		{
     	if(uri.toString().contains(SERVER_URL_RESOLUTION)){
 			return true; }
     	else { return false; }
 		}
     
     
	
    public org.w3c.dom.Node getListEndpointURL(){
			
			return null;
			
			
		}
		
	
    public void initFromXmlNode(Node server,Document regDoc) throws Exception 
		{
    	final NodeList entries_error = regDoc.getElementsByTagName("error");
		final NodeList entries_endpoint = regDoc.getElementsByTagName("endpoint");
		
		if(entries_error != null && entries_error.getLength() > 0){
			
			for(int i=0; i< entries_error.getLength(); i++){
						org.w3c.dom.Node xmlNodeServerError = entries_error.item(i);
						final NodeList entriesError =  xmlNodeServerError.getChildNodes();
						String valueErrorid="";
						for(int k=0; k< entriesError.getLength(); k++){
							
							
							org.w3c.dom.Node xmlNodeError = entriesError.item(k);
		
							String nameError = xmlNodeError.getNodeName();
							String valueError=xmlNodeError.getTextContent();
							
							
							
							if(!nameError.equals("#text")){
								if(nameError.equals("id")) {
									//nameError=nameError.concat("Error");
									valueErrorid = valueError;
								}
								else if(nameError.equals("label")) {
									nameError=nameError.concat("Error");
					        		 STATUS_ERROR = errorHandler(Integer.parseInt(valueErrorid));
						        	 STATUS_ERROR_LABEL = valueError;
						        	 throw new Exception("End_Point_Error");
									 
								}
								 
							 }
							 
						}
				
				
				
				
			    }
			
		}
		else if(entries_endpoint != null && entries_endpoint.getLength() > 0){
			final NodeList entries_status = regDoc.getElementsByTagName("status");	
			NamedNodeMap map_status = entries_status.item(0).getAttributes();			
				String id_status = map_status.item(0).getTextContent();
				//In case server is not online
				if(Integer.parseInt(id_status)!=2)
				{
					if(Integer.parseInt(id_status)!=-1)
					{
						STATUS_ERROR = errorHandler(Integer.parseInt(id_status));
						throw new Exception("End_Point_Error");
						
					}
					else{
						org.w3c.dom.Node xmlNode_status = entries_status.item(0);
						final NodeList entries_ =  xmlNode_status.getChildNodes();
						for(int l=0; l< entries_.getLength(); l++){
						org.w3c.dom.Node xmlNode_status_ = entries_.item(l);
						String name_status = xmlNode_status_.getNodeName();
						String value_status=xmlNode_status_.getTextContent();
						if(!name_status.equals("#text")){
							STATUS_ERROR = "-1";
				        	 STATUS_ERROR_LABEL = value_status;
				        	 throw new Exception("End_Point_Error");
						}
					}
						
					}
				}
				else
				{

							org.w3c.dom.Node xmlNode_status = entries_status.item(0);
							final NodeList entries_ =  xmlNode_status.getChildNodes();
							for(int l=0; l< entries_.getLength(); l++){
							org.w3c.dom.Node xmlNode_status_ = entries_.item(l);
							String name_status = xmlNode_status_.getNodeName();
							String value_status=xmlNode_status_.getTextContent();
							if(!name_status.equals("#text")){
								server.setProperty("label_status", value_status);
							}
						}
			
			//getAttributes of endpoint
			NamedNodeMap map2 = entries_endpoint.item(0).getAttributes();
			for(int i=0;i<map2.getLength();i++){
				String name_endpoint = map2.item(i).getNodeName();
				String value_endpoint = map2.item(i).getTextContent();
				if(name_endpoint.equals("expire"))
				{
					if(value_endpoint.equals("a day"))
					{
						server.setProperty(name_endpoint, "date:["+String.valueOf(System .currentTimeMillis() + 86400000)+"]");
					}
					else if (value_endpoint.equals("a week")){
						server.setProperty(name_endpoint, "date:["+String.valueOf(System .currentTimeMillis() + 604800000)+"]");			
					}
					else if (value_endpoint.equals("a month"))
					{
						server.setProperty(name_endpoint, "date:["+String.valueOf(System .currentTimeMillis() + 2678400000L)+"]");
					}
					else{
						server.setProperty(name_endpoint, value_endpoint);
						
					}
				}
				else{
					server.setProperty(name_endpoint, value_endpoint);
				}
				

			}

			for(int i=0; i< entries_endpoint.getLength(); i++){
				org.w3c.dom.Node xmlNode_server = entries_endpoint.item(i);
				final NodeList entries2 =  xmlNode_server.getChildNodes();

				for(int k=1; k< entries2.getLength(); k++){


					org.w3c.dom.Node xmlNode = entries2.item(k);

					String name = xmlNode.getNodeName();
					String value=xmlNode.getTextContent();
					if(!name.equals("#text") && !name.equals("url") && !name.equals("status")){

						server.setProperty(name, value);
					}
					else if(name.equals("url")){


						final NodeList entries_url = xmlNode.getChildNodes();
						for(int l=1; l< entries_url.getLength(); l++){
							org.w3c.dom.Node xmlNode_url = entries_url.item(l);
							String name_url = xmlNode_url.getNodeName();
							String value_url=xmlNode_url.getTextContent();
							if(!name_url.equals("#text")){
								//in case 1 blue, putting a false alias doesn't return error
                                if(name_url.equals("host")&&value_url.equals(""))
                                {
            						STATUS_ERROR = errorHandler(32);
            						throw new Exception("End_Point_Error");
                                }
                                else
                                {
                                	server.setProperty(name_url, value_url);
                                }
							}

						}

					}




				}


			}
			}
		}
		}
		
    public String constructAuth(Node serverNode)
		{
   	 String uuid = UUID.randomUUID().toString();
   	 String seed = uuid.substring(0,5);
  
			String auth = serverNode.getPropertyValue("apikey")+":"+seed+":"+RestRequest.md5(seed+serverNode.getPropertyValue("apisecret")) ;

    	return auth;
		}
    
    public String contructURL(Node NodeServer){
	
	String url = NodeServer.getPropertyValue("protocol").concat("://").concat(NodeServer.getPropertyValue("host"))
			.concat(NodeServer.getPropertyValue("path"));
	return url;
	
    }

    public URI getEndPointInfoURL(Node serverNode,String endPointAlias) throws URISyntaxException{
       	String api = serverNode.getPropertyValue("server_url");
    	String url = api.concat("server.xml?auth="+constructAuth(serverNode)).
				concat("&method="+serverNode.getPropertyValue("parameter_name1")+"&"+serverNode.getPropertyValue("parameter_name2")+"="+endPointAlias);
    	
    	//String url = api.concat("?auth="+constructAuth(serverNode)).
			//concat("&method="+serverNode.getPropertyValue("parameter_name1")+"&"+serverNode.getPropertyValue("parameter_name2")+"="+endPointAlias);
    	URI uri = new URI(url);
			return uri;		
		}
    
    public String resolveServer(Server server, RestRequest restRequest, URI uri_server) throws Exception
    {

    	String uriServer=null;
    	URI uri = null;

   		uri = getEndPointInfoURL(server.getServerNode(),server.getServerNode().getPropertyValue("Resolution_Alias"));


    	Document regDoc = null;

    	regDoc = restRequest.getDocumentContent(uri);

    	if(uri_server.toString().contains("get_xml_registry")){
    		  
    		     initFromXmlNode(server.getServerNode(),regDoc); 

    	}

         if(server.getServerNode().getStatus() != Node.NODE_STATUS_ERROR)
         {
    			uriServer = contructURL(server.getServerNode());
         }
         else {
        	 
         }

    	return uriServer;

    }
  
    public static String errorHandler(int idError){
    	
	    switch (idError) {
	    case 2:
	    	return STATUS_ERROR_AUTH;
	    case 4:
	    	return STATUS_ERROR_METHOD;
	    case 8:
	    	return STATUS_ERROR_WRONG_ARG;
	    case 16:
	    	return STATUS_ERROR_INT;
	    	
    default:
        return STATUS_ERROR_NOT_FIND;
    }
    }
    


    
    public static String statusHandler(Server server,int idStatus){
    	
	    switch (idStatus) {
	    case 4:
	    	return STATUS_OFFLINE;
	    case 8:
	    	return STATUS_TMP_OFFLINE;
	    	
    default:
        return STATUS_UNKOWN;
    }
    	
    }


}

