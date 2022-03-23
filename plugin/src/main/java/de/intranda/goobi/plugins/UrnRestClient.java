
package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Base64;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

public class UrnRestClient {

	private String uri;
	private String auth;
	private String namespaceName;

	/**
	 * @param Uri       URL of the URN servcce
	 * @param namespace namespace in which URNs are created
	 * @param User      Username of the API User
	 * @param Password  Password of the User
	 */
	public UrnRestClient(String Uri, String Namespace, String User, String Password) {
		this.auth = Base64.getEncoder().encodeToString((User.trim() + ":" + Password.trim()).getBytes());
		if (!Uri.startsWith("https"))
			throw new IllegalArgumentException("Bad URL - only https is permitted");
		uri = (!Uri.endsWith("/")) ? Uri + "/" : Uri;
		this.namespaceName = Namespace.trim();
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
	private String getUrnSuggestion() throws ClientProtocolException, IOException, IllegalArgumentException {

		Request request = Request.Post(uri + "/v2/namespaces/name/" + namespaceName + "/urn-suggestion");
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
	public String createUrn(ArrayList<String> urls)
			throws ClientProtocolException, IOException, IllegalArgumentException {
		String urn = getUrnSuggestion();
		Request request = Request.Post(uri + "urns");
		request = addHeaders(request);
		String response = request.addHeader("Content-Type", "application/json")
				.bodyString(createUrnBodyString(urn, urls), ContentType.APPLICATION_JSON).execute()
				.handleResponse(new CreateResponseHandler());

		// TODO new Response Handling
		return response;
	}

	/**
	 * Replaces the urls of the provided URN
	 * 
	 * @param Urn  Name of the entry that shall be updated
	 * @param urls Array of String with Metadata
	 * @return true if operation was successful
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public boolean replaceUrls(String Urn, ArrayList<String> urls)
			throws ClientProtocolException, IOException, IllegalArgumentException {

		Request request = Request.Patch(uri + "urns/urn/" + Urn + "/" + "my-urls");
		request = addHeaders(request);
		String response = request.addHeader("Content-Type", "application/json")
				.bodyString(replaceUrlsBodyString(urls), ContentType.APPLICATION_JSON).execute()
				.handleResponse(new PatchResponseHandler());
		return response.equals("success");
	}

	/**
	 * Helper method of replaceUrls. It creates a JSONArray String of Objects with
	 * the Paraneter url
	 * 
	 * @param urls urls that shall be added to the JSONArray
	 * @return String with JSONarray
	 */
	private String replaceUrlsBodyString(ArrayList<String> urls) {
		JsonArrayBuilder arrayBuilder = createUrlArray(urls);
		Writer writer = new StringWriter();
		Json.createWriter(writer).write(arrayBuilder.build());
		return writer.toString();
	}

	/**
	 * Helper method of replaceUrlsBodyString and createUrnBodyString
	 * 
	 * @param urls urls to put in the array
	 * @return arrayBuilder object
	 */
	private JsonArrayBuilder createUrlArray(ArrayList<String> urls) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (String url : urls) {
			arrayBuilder.add(Json.createObjectBuilder().add("url", url));
		}
		return arrayBuilder;
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
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = createUrlArray(urls);
		// add Values to JsonObject
		objectBuilder.add("urn", Urn);
		objectBuilder.add("urls", arrayBuilder);

		Writer writer = new StringWriter();
		Json.createWriter(writer).write(objectBuilder.build());

		return writer.toString();
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
