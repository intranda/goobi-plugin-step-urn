package de.intranda.goobi.plugins.Messages;

import lombok.experimental.Accessors;
import lombok.Getter;
import lombok.Setter;
@Accessors (chain =true)
public class ErrorMessage {
    @Getter
	private String code;
	@Getter
	private String developerMessage;
	@Getter
	private int status;

}
