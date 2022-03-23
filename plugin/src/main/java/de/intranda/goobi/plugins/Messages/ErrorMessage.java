package de.intranda.goobi.plugins.Messages;

public class ErrorMessage {
	private String code;
	
	private String developerMessage;
	
	private int status;

	public String getDeveloperMessage() {
		return developerMessage;
	}

	public int getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

}
