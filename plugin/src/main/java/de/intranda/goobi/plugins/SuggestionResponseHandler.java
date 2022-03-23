package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;


public class SuggestionResponseHandler extends UrnResponseHandler {

	@Override
	public  String handleResponse(HttpResponse response) throws ClientProtocolException, IOException, JsonException  {
		initialize(response);
		
		if (status >= 200 && status < 300) {
			if ( entity == null) {
				throw new ClientProtocolException(status + ": reason-> " + "no response provided");
			} else {
				//TODO parse JSON
				String jsonString = EntityUtils.toString(entity, Charset.forName("utf-8"));
				JsonReader reader = Json.createReader(new StringReader(jsonString));
				JsonObject jsonObject= reader.readObject();
				return jsonObject.getString("suggestedUrn");
			}
		} else {
			handleErrorStates();
			//not reachable
			return null;
		}
	}
}

