
package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.intranda.goobi.plugins.Messages.UrlListMessage;
import de.intranda.goobi.plugins.Messages.UrnCreationMessage;
import de.intranda.goobi.plugins.ResponseHandler.CreateResponseHandler;
import de.intranda.goobi.plugins.ResponseHandler.PatchResponseHandler;
import de.intranda.goobi.plugins.ResponseHandler.SuggestionResponseHandler;

public class UrnRestClient {

    private String uri;
    private String auth;
    private String namespaceName;
    private String infix;
    private Gson gson;

    /**
     * @param Uri URL of the URN servcce
     * @param namespace namespace in which URNs are created
     * @param User Username of the API User
     * @param Password Password of the User
     */
    public UrnRestClient(String Uri, String Namespace, String Infix, String User, String Password) {
        this.auth = Base64.getEncoder().encodeToString((User.trim() + ":" + Password.trim()).getBytes());
        if (!Uri.startsWith("https"))
            throw new IllegalArgumentException("Bad URL - only https is permitted");
        uri = (!Uri.endsWith("/")) ? Uri + "/" : Uri;
        this.namespaceName = Namespace.trim();
        this.infix = (Infix != null && Infix.trim().isEmpty()) ? null : Infix;
        gson = new Gson();
    }

    /**
     * asks for an urn-suggestion
     * 
     * @param namespaceName namespace in which the suggested URN ist located
     * @return String URN-suggestion
     * @throws ClientProtocolException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    private String getUrnSuggestion() throws ClientProtocolException, IOException, IllegalArgumentException, JsonSyntaxException {

        Request request = Request.Get(uri + "namespaces/name/" + namespaceName + "/urn-suggestion");
        request = addHeaders(request);

        String response = request.execute().handleResponse(new SuggestionResponseHandler());

        return response;
    }

    /**
     * Creates new URN
     * 
     * @param urls
     * @return String with new URN
     * @throws ClientProtocolException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public String createUrn(ArrayList<String> urls) throws ClientProtocolException, IOException, IllegalArgumentException, JsonSyntaxException {
        String urn = infix == null ? getUrnSuggestion() : UrnGenerator.generateUrn(namespaceName, infix);
        Request request = Request.Post(uri + "urns");
        request = addHeaders(request);
        String response = request.addHeader("Content-Type", "application/json")
                .bodyString(createUrnBodyString(urn, urls), ContentType.APPLICATION_JSON)
                .execute()
                .handleResponse(new CreateResponseHandler());
        return response;
    }

    /**
     * Replaces the urls of the provided URN
     * 
     * @param Urn Name of the entry that shall be updated
     * @param urls Array of String with Metadata
     * @return true if operation was successful
     * @throws ClientProtocolException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public boolean replaceUrls(String Urn, ArrayList<String> urls)
            throws ClientProtocolException, IOException, IllegalArgumentException, JsonSyntaxException {

        Request request = Request.Patch(uri + "urns/urn/" + Urn + "/" + "my-urls");
        request = addHeaders(request);
        String response = request.addHeader("Content-Type", "application/json")
                .bodyString(replaceUrlsBodyString(urls), ContentType.APPLICATION_JSON)
                .execute()
                .handleResponse(new PatchResponseHandler());
        return response.equals("success");
    }

    /**
     * Helper method of replaceUrls. It creates a JSONArray String of Objects with the Parameter url
     * 
     * @param urls urls that shall be added to the JSONArray
     * @return String with JSONarray
     */
    private String replaceUrlsBodyString(ArrayList<String> urls) {
        ArrayList<UrlListMessage> ulm = createUrlList(urls);
        return gson.toJson(ulm);
    }

    /**
     * Helper method of replaceUrlsBodyString and createUrnBodyString
     * 
     * @param urls urls to put in the array
     * @return arrayBuilder object
     */
    private ArrayList<UrlListMessage> createUrlList(ArrayList<String> urls) {
        ArrayList<UrlListMessage> ulm = new ArrayList<UrlListMessage>();
        for (String url : urls) {
            // Method Chaining
            ulm.add(new UrlListMessage().setUrl(url));
        }
        return ulm;
    }

    /**
     * Helper method of createUrn. Creates JSON-String needed to create an URN
     *
     * @param Urn
     * @param urls
     * @return
     * @throws IOException
     */
    private String createUrnBodyString(String Urn, ArrayList<String> urls) throws IOException {
        UrnCreationMessage createArk = new UrnCreationMessage();
        ArrayList<UrlListMessage> ulm = new ArrayList<UrlListMessage>();
        createArk.setUrn(Urn);
        createArk.setUrls(createUrlList(urls));
        return gson.toJson(createArk);
    }

    /**
     * Helper method that adds Authorization- and Accept- Header to a given Request
     * 
     * @param request Request Object which needs Authorization and Accept Headers
     * @return returns the Request with Authorization and Accept Header
     */
    private Request addHeaders(Request request) {
        return request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth).addHeader("Accept", "application/json");
    }

}
