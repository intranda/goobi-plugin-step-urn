package de.intranda.goobi.plugins.messages;

import lombok.Getter;

public class UrnCreationSuccessfulMessage {
    @Getter
    private String urn;
    @Getter
    private String created;
}
