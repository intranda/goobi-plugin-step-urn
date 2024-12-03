package de.intranda.goobi.plugins.responsehandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonSyntaxException;

import de.intranda.goobi.plugins.messages.UrnSuggestionMessage;

public class SuggestionResponseHandler extends UrnResponseHandler {

    @Override
    public String handleResponse(HttpResponse response) throws IOException, JsonSyntaxException {
        initialize(response);

        if (status >= 200 && status < 300) {
            if (entity == null) {
                throw new ClientProtocolException(status + ": reason-> " + "no response provided");
            } else {
                //TODO parse JSON
                String jsonString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
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
