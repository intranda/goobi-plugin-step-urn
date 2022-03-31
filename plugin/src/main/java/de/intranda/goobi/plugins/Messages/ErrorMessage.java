package de.intranda.goobi.plugins.Messages;

import lombok.Getter;
import lombok.Setter;
public class ErrorMessage {
    @Getter
	private String code;
	@Getter
	private String developerMessage;
	@Getter
	private int status;

}
