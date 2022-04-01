package de.intranda.goobi.plugins.ResponseHandler;

import java.io.IOException;
import java.nio.charset.Charset;

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

    /**
     * Helper method of handleResponse that can be used by the Children of an UrnResponseHandler to set the status and the message entity of the
     * UrnResponsehandler. It also intitializes gson.
     * 
     * @param response HttpResponse Object
     */
    protected void initialize(HttpResponse response) {
        status = response.getStatusLine().getStatusCode();
        entity = response.getEntity();
        gson = new Gson();
    }

    @Override
    public abstract String handleResponse(HttpResponse response) throws ClientProtocolException, IOException, JsonSyntaxException;

    /**
     * Helper Method of handleResponse() that can be used by the Children of an UrnResponseHandler
     * 
     * @throws ClientProtocolException
     * @throws IOException
     * @throws JsonSyntaxException
     */
    protected void handleErrorStates() throws ClientProtocolException, IOException, JsonSyntaxException {
        if (status > 400 && status <= 429) {

            ErrorMessage error = null;
            if (entity != null) {
                String jsonString = EntityUtils.toString(entity, Charset.forName("utf-8"));
                error = gson.fromJson(jsonString, ErrorMessage.class);
            }
            throw new ClientProtocolException(error == null ? ("Error: "+ status + " -> no response body received")
                    : "Errorcode: " + error.getCode() + ": " + error.getDeveloperMessage());
        } else
            throw new ClientProtocolException("Error: "+ status + ": reason-> " + " unhandled error");
    }
}
