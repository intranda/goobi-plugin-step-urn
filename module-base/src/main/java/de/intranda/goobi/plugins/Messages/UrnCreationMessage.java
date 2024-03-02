package de.intranda.goobi.plugins.Messages;

import java.util.ArrayList;
import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.Setter;

@Accessors(chain = true)
public class UrnCreationMessage {
    @Getter @Setter
    private String urn;
    @Getter @Setter
    private ArrayList<UrlListMessage> urls;

}
