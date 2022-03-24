package de.intranda.goobi.plugins.ResponseHandler;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.intranda.goobi.plugins.Messages.ErrorMessage;

public abstract class UrnResponseHandler implements ResponseHandler<String> {
	protected int status;
	protected HttpEntity entity;
	protected Gson gson;

	protected void initialize(HttpResponse response) {
		status = response.getStatusLine().getStatusCode();
		entity = response.getEntity();
		gson = new Gson();
	}

	@Override
	public abstract String handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException, JsonSyntaxException;

	protected void handleErrorStates() throws ClientProtocolException, IOException {
		if (status > 400 && status <= 429) {

			ErrorMessage error = null;
			if (entity != null) {
				String jsonString = EntityUtils.toString(entity, Charset.forName("utf-8"));
				error = gson.fromJson(jsonString, ErrorMessage.class);
			}
			throw new ClientProtocolException(status + ": reason-> " + error == null ? "no response body received"
					: "Errorcode: " + error.getCode() + ": " + error.getDeveloperMessage());
		} else
			throw new ClientProtocolException(status + ": reason-> " + " unhandled error");
	}
}
