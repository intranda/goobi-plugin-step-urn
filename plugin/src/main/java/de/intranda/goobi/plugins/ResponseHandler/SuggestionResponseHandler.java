package de.intranda.goobi.plugins.ResponseHandler;

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

import com.google.gson.JsonSyntaxException;

import de.intranda.goobi.plugins.Messages.UrnCreationSuccessfulMessage;
import de.intranda.goobi.plugins.Messages.UrnSuggestionMessage;


public class SuggestionResponseHandler extends UrnResponseHandler {

	@Override
	public  String handleResponse(HttpResponse response) throws ClientProtocolException, IOException, JsonSyntaxException  {
		initialize(response);
		
		if (status >= 200 && status < 300) {
			if ( entity == null) {
				throw new ClientProtocolException(status + ": reason-> " + "no response provided");
			} else {
				//TODO parse JSON
				String jsonString = EntityUtils.toString(entity, Charset.forName("utf-8"));
				UrnSuggestionMessage successful = gson.fromJson(jsonString, UrnSuggestionMessage.class);
				return successful.getSuggestedUrn();
			}
		} else {
			handleErrorStates();
			//not reachable
			return null;
		}
	}
}

