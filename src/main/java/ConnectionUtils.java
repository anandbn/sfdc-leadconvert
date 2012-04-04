

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
public class ConnectionUtils {
	
	public static PartnerConnection getPartnerConnection(String username,String password,String loginUrl) throws Exception  {
		PartnerConnection connection=null;
		try { 
		   // login to the Force.com Platform
		   ConnectorConfig config = new ConnectorConfig();
		   config.setTraceMessage(true);
		   config.setUsername(username);
		   config.setPassword(password);
		   config.setAuthEndpoint(loginUrl);
		   connection = com.sforce.soap.partner.Connector.newConnection(config);
		} catch ( ConnectionException ce) {
			ce.printStackTrace();
		}
	   return connection;
		
	}
	public static PartnerConnection getPartnerConnection(ConnectorConfig connCfg) throws Exception  {
		return com.sforce.soap.partner.Connector.newConnection(connCfg);
	}
	public static MetadataConnection getMetaConnection(String username,String password,String loginUrl) throws Exception  {
		PartnerConnection connection=getPartnerConnection(username,password,loginUrl);
		MetadataConnection metaConnection;
		
		try { 
		   ConnectorConfig metaConfig = new ConnectorConfig();
		   metaConfig.setSessionId(connection.getConfig().getSessionId());
		   metaConfig.setServiceEndpoint(getMetaDataServiceEndpoint(connection.getConfig().getServiceEndpoint()));
		   metaConnection = com.sforce.soap.metadata.Connector.newConnection(metaConfig);
		}catch(Exception ex){
			throw ex;
		}
		return metaConnection;
	}
	public static MetadataConnection getMetaConnection(ConnectorConfig connCfg) throws Exception  {
		   ConnectorConfig metaConfig = new ConnectorConfig();
		   metaConfig.setSessionId(connCfg.getSessionId());
		   metaConfig.setServiceEndpoint(getMetaDataServiceEndpoint(connCfg.getServiceEndpoint()));
		return com.sforce.soap.metadata.Connector.newConnection(metaConfig);
	}

	private static String getMetaDataServiceEndpoint(String apiEndpoint){
		String metaAPIEndpoint="";
		String[] tokens = apiEndpoint.split("/");
		metaAPIEndpoint+=tokens[0];
		metaAPIEndpoint+="//";
		
		for(Integer idx=2;idx<tokens.length-1;idx++){
			if(tokens[idx].equals("u")){
				metaAPIEndpoint+="m";
			}else{
				metaAPIEndpoint+=tokens[idx];
			}
			metaAPIEndpoint +="/";
		}
		return metaAPIEndpoint+tokens[tokens.length-1];
	}
	

}
