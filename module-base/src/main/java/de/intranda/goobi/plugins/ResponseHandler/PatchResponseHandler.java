package de.intranda.goobi.plugins.ResponseHandler;

import java.io.IOException;

import javax.json.JsonException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

public class PatchResponseHandler extends UrnResponseHandler {

	@Override
	public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException, JsonException {
		initialize(response);
		if (status == 204)
			return "success";
		
		handleErrorStates();
		
		//unreacheable
		return "failed";
	}

}
