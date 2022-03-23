package de.intranda.goobi.plugins.Messages;

public class UrnCreationSuccessfulMessage {

	private String urn;
	private String created;
	// the API Answer provides way more Details
	public String getUrn() {
		return urn;
	}
	public String getCreated() {
		return created;
	}
}
