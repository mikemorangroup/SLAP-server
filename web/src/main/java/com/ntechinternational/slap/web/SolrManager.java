package com.ntechinternational.slap.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.IOUtils;

public class SolrManager {
	private static final String DEFAULT_CONFIG_LOCATION = "Map.xml";
	
	XMLConfiguration config = null;
	public SolrManager() throws ConfigurationException{
		String configLocation = System.getenv("SLAP_WEB_CONFIG_XML");
		configLocation = configLocation == null ? DEFAULT_CONFIG_LOCATION : configLocation;
		
		loadConfiguration(configLocation);
	}
	

	public void loadConfiguration(String configLocation) throws ConfigurationException{
		config = new XMLConfiguration(configLocation);
		config.setExpressionEngine(new XPathExpressionEngine());
	}
	
	public String executeQuery(String queryName, InputStream dataToSendStream) throws IOException{
		StringBuilder queryURL = new StringBuilder();
		
		HierarchicalConfiguration queryNode = config.configurationAt("queries/query[@name='" + queryName + "']");
		
		//set host name
		queryURL.append(config.getString("/url"));
		
		//set request path
		String path = queryNode.getString("//@path");
		path = path == null ? config.getString("queries/@defaultPath") : path;
		queryURL.append(path);
		
		//add query params
		List<HierarchicalConfiguration> queryParamNodes = queryNode.configurationsAt("queryParams/queryParam");
		if(queryParamNodes.size() > 0){
			queryURL.append("?");
			for(HierarchicalConfiguration queryParamNode : queryParamNodes){
				queryURL.append(queryParamNode.getString("//@name"));
				queryURL.append("=");
				queryURL.append(URLEncoder.encode(queryParamNode.getString("."), "UTF-8"));
				queryURL.append("&");
			}
		}
		
		//start connecting
		URL requestUrl = new URL(queryURL.toString());
		HttpURLConnection connection = (HttpURLConnection)requestUrl.openConnection(); 
		
		//set the request method
		String method = queryNode.getString("//@method");
		method = method == null ? "GET" : method;
		connection.setRequestMethod(method);
		connection.setDoInput(true);
		
		//set the content type
		String contentType = queryNode.getString("//@contentType");
		if(contentType != null){
			connection.setRequestProperty("Content-Type", contentType);
		}
		
		if(dataToSendStream != null){
			connection.setDoOutput(true);
			
			OutputStream outputStream = connection.getOutputStream();
			IOUtils.copy(dataToSendStream, outputStream);
			outputStream.flush();
		}
		
		StringWriter stringWriter = new StringWriter();
		IOUtils.copy(connection.getInputStream(), stringWriter, connection.getContentEncoding());
		
		return stringWriter.toString();
	}
	
	public String clearSolr() throws IOException{
		
		return executeQuery("reset", null);
	}
	
	public String uploadQuestionToSolr(InputStream inputStream) throws IOException{
		
		return executeQuery("insertQuestion", inputStream);
	}
}
