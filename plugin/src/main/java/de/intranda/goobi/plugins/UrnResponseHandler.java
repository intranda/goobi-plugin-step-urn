package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public abstract class UrnResponseHandler implements ResponseHandler<String> {
	protected int status;
	protected HttpEntity entity;
	
	protected void initialize(HttpResponse response) {
		status = response.getStatusLine().getStatusCode();
	    entity = response.getEntity();
	}

	@Override
	public abstract String handleResponse(HttpResponse response)throws ClientProtocolException, IOException;
	
	protected void handleErrorStates() throws ClientProtocolException, IOException {
		if (status == 400) {
			JsonObject jsonObject = null;
			if (entity != null) {
				String jsonString = EntityUtils.toString(entity, Charset.forName("utf-8"));
				JsonReader reader = Json.createReader(new StringReader(jsonString));
				jsonObject= reader.readObject();				
			}
			throw new ClientProtocolException(status + ": reason-> " + jsonObject == null ? "no response body received"
					: "Errorcode: " + jsonObject.getString("code") + ": " + jsonObject.getString("developerMessage"));
		} else
			throw new ClientProtocolException(status + ": reason-> " + " unhandled error");
	}
}
