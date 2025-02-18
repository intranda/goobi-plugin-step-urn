package de.intranda.goobi.plugins.responsehandler;

import java.io.IOException;

import jakarta.json.JsonException;

import org.apache.http.HttpResponse;

public class PatchResponseHandler extends UrnResponseHandler {

    @Override
    public String handleResponse(HttpResponse response) throws IOException, JsonException {
        initialize(response);
        if (status == 204) {
            return "success";
        }

        handleErrorStates();

        //unreacheable
        return "failed";
    }

}
