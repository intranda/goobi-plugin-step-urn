package de.intranda.goobi.plugins.Messages;

import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.Setter;

@Accessors(chain = true)
public class UrlListMessage {
    @Getter @Setter
    private String url;
    //private int priority -  maybe later
}
