package de.intranda.goobi.plugins.Messages;


import lombok.Getter;
import lombok.Setter;

public class UrnCreationSuccessfulMessage {
    @Getter
	private String urn;
    @Getter
	private String created;
}
