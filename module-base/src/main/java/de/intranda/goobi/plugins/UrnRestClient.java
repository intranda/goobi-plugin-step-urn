
package de.intranda.goobi.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import de.intranda.goobi.plugins.messages.UrlListMessage;
import de.intranda.goobi.plugins.messages.UrnCreationMessage;
import de.intranda.goobi.plugins.responsehandler.CreateResponseHandler;
import de.intranda.goobi.plugins.responsehandler.PatchResponseHandler;
import de.sub.goobi.config.ConfigurationHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UrnRestClient {

    private String uri;
    private String auth;
    private Gson gson;
    private boolean useProxy = false;
    private HttpHost proxy;

    /**
     * @param Uri URL of the URN service
     * @param namespace namespace in which URNs are created
     * @param user Username of the API User
     * @param password Password of the User
     */
    public UrnRestClient(String uri, String namespace, String user, String password) {
        this.auth = Base64.getEncoder().encodeToString((user.trim() + ":" + password.trim()).getBytes());
        if (!uri.startsWith("https")) {
            throw new IllegalArgumentException("Bad URL - only https is permitted");
        }
        this.uri = (!uri.endsWith("/")) ? uri + "/" : uri;
        gson = new Gson();
        ConfigurationHelper cHelper = ConfigurationHelper.getInstance();
        if (cHelper.isUseProxy()) {
            try {
                URL url = new URL(this.uri);
                if (!cHelper.isProxyWhitelisted(url)) {
                    this.useProxy = cHelper.isUseProxy();
                    this.proxy = new HttpHost(cHelper.getProxyUrl(), cHelper.getProxyPort());
                } else {
                    log.debug("URN PLUGIN: url was on proxy whitelist, no proxy used: " + this.uri);
                }
            } catch (MalformedURLException e) {
                log.debug("URN PLUGIN: could not convert into URL: {} ", this.uri);
            }
        }
    }

    /**
     * registers new URN
     * 
     * @param urls
     * @return String with new URN
     * @throws ClientProtocolException
     * @throws IOException
     * @throws InterruptedException
     */
    public String registerUrn(String urn, List<String> urls) throws IOException, JsonSyntaxException {

        Request request = Request.Post(uri + "urns");
        request = addHeadersAndProxy(request);

        return request.addHeader("Content-Type", "application/json")
                .bodyString(createUrnBodyString(urn, urls), ContentType.APPLICATION_JSON)
                .execute()
                .handleResponse(new CreateResponseHandler());

    }

    /**
     * Replaces the urls of the provided URN
     * 
     * @param urn Name of the entry that shall be updated
     * @param urls Array of String with Metadata
     * @return true if operation was successful
     * @throws ClientProtocolException
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public boolean replaceUrls(String urn, List<String> urls)
            throws IOException, IllegalArgumentException, JsonSyntaxException {

        Request request = Request.Patch(uri + "urns/urn/" + urn + "/" + "my-urls");
        request = addHeadersAndProxy(request);
        String response = request.addHeader("Content-Type", "application/json")
                .bodyString(replaceUrlsBodyString(urn, urls), ContentType.APPLICATION_JSON)
                .execute()
                .handleResponse(new PatchResponseHandler());
        return "success".equals(response);
    }

    /**
     * Helper method of replaceUrls. It creates a JSONArray String of Objects with the Parameter url
     * 
     * @param urls urls that shall be added to the JSONArray
     * @return String with JSONarray
     */
    private String replaceUrlsBodyString(String urn, List<String> urls) {
        List<UrlListMessage> ulm = createUrlList(urn, urls);
        return gson.toJson(ulm);
    }

    /**
     * Helper method of replaceUrlsBodyString and createUrnBodyString
     * 
     * @param urls urls to put in the array
     * @return arrayBuilder object
     */
    private List<UrlListMessage> createUrlList(String urn, List<String> urls) {
        List<UrlListMessage> ulm = new ArrayList<>();
        for (String url : urls) {
            ulm.add(new UrlListMessage().setUrl(url.replace("{pi.urn}", urn)));
        }
        return ulm;
    }

    /**
     * Helper method of createUrn. Creates JSON-String needed to create an URN
     *
     * @param urn
     * @param urls
     * @return
     */
    private String createUrnBodyString(String urn, List<String> urls) {
        UrnCreationMessage createArk = new UrnCreationMessage();
        createArk.setUrn(urn);
        createArk.setUrls(createUrlList(urn, urls));
        return gson.toJson(createArk);
    }

    /**
     * Helper method that adds Authorization- and Accept- Header to a given Request
     * 
     * @param request Request Object which needs Authorization and Accept Headers
     * @return returns the Request with Authorization and Accept Header
     */
    private Request addHeadersAndProxy(Request request) {
        if (this.useProxy) {
            return request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth).addHeader("Accept", "application/json").viaProxy(this.proxy);
        } else {
            return request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth).addHeader("Accept", "application/json");
        }
    }

}
