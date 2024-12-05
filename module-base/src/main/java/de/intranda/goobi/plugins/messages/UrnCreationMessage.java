package de.intranda.goobi.plugins.messages;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class UrnCreationMessage {
    @Getter
    @Setter
    private String urn;
    @Getter
    @Setter
    private List<UrlListMessage> urls;

}
