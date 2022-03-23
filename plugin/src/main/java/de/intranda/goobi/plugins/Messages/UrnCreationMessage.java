package de.intranda.goobi.plugins.Messages;

import java.util.ArrayList;

public class UrnCreationMessage {
	private String urn;
	private ArrayList<UrlListMessage> urls;
	public ArrayList<UrlListMessage> getUrls() {
		return urls;
	}
	public UrnCreationMessage setUrls(ArrayList<UrlListMessage> urls) {
		this.urls = urls;
		return this;
	}
	public String getUrn() {
		return urn;
	}
	public UrnCreationMessage setUrn(String urn) {
		this.urn = urn;
		return this;
	}
}
