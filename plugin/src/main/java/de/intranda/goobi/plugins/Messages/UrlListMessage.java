package de.intranda.goobi.plugins.Messages;

public class UrlListMessage {
	private String url;
	//private int priority -  maybe later

	public String getUrl() {
		return url;
	}

	public UrlListMessage setUrl(String url) {
		this.url = url;
		return this;
	}
}
