package de.intranda.goobi.plugins;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import javax.json.JsonException;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.http.client.ClientProtocolException;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import com.google.gson.JsonSyntaxException;

import de.intranda.ugh.extension.util.DocstructConfigurationItem;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class UrnStepPlugin implements IStepPluginVersion2 {

	@Getter
	private String title = "intranda_step_urn";
	@Getter
	private Step step;
	private Prefs prefs;
	private String ppn;
	private MetadataType ppntype;

	private String metsUrnType;
	private String modsUrnType;
	private boolean setmodsUrn;

	private boolean setWorkUrn;
	private boolean setAnchorUrn;
	private boolean successful = true;

	private String metadataType;
	private String uri;
	private String namespace;
	private String apiUser;
	private String apiPassword;
	private String returnPath;
	private String publicationUrl;
	private String infix;
	private String[] allowedTypes;
	private UrnRestClient urnClient;
	private UrnGenerator urnGenerator;
	private Fileformat ff;
	private ArrayList<String> urls;

	@Override
	public void initialize(Step step, String returnPath) {
		this.returnPath = returnPath;
		this.step = step;

		// read parameters from correct block in configuration file
		SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
		uri = myconfig.getString("apiUri", "https://api.nbn-resolving.org/v2/");
		namespace = myconfig.getString("namespace", "urn:nbn:de:gbv:NN");
		apiUser = myconfig.getString("apiUser", "user");
		apiPassword = myconfig.getString("apiPassword", "password");

		metsUrnType = myconfig.getString("typeNameMets", "_urn");
		modsUrnType = myconfig.getString("typeNameMods", "URN");

		setmodsUrn = myconfig.getBoolean("createModsUrns", false);
		setWorkUrn = myconfig.getBoolean("work", true);
		setAnchorUrn = myconfig.getBoolean("anchor", false);

		// read Array with allowed elements from configuration
		allowedTypes = myconfig.getStringArray("allowed/type");
		publicationUrl = myconfig.getString("url", "https://viewer.example.org/viewer/resolver?urn={pi.urn}");
		infix = myconfig.getString("infix");

		log.info("Urn step plugin initialized");
	}

	@Override
	public PluginGuiType getPluginGuiType() {
		return PluginGuiType.NONE;
	}

	@Override
	public String getPagePath() {
		return "/uii/plugin_step_urn.xhtml";
	}

	@Override
	public PluginType getType() {
		return PluginType.Step;
	}

	@Override
	public String cancel() {
		return "/uii" + returnPath;
	}

	@Override
	public String finish() {
		return "/uii" + returnPath;
	}

	@Override
	public int getInterfaceVersion() {
		return 0;
	}

	@Override
	public HashMap<String, StepReturnValue> validate() {
		return null;
	}

	@Override
	public boolean execute() {
		PluginReturnValue ret = run();
		return ret != PluginReturnValue.ERROR;
	}

	private void setUrn(DocStruct ds) throws JsonSyntaxException, ClientProtocolException, IllegalArgumentException,
			MetadataTypeNotAllowedException, WriteException, PreferencesException, IOException, InterruptedException,
			SwapException, DAOException, SQLException, UrnDatabaseException {

		// always look for a ppn
		if (ds.getAllMetadata() != null && ds.getAllMetadata().size() > 0) {
			for (Metadata m : ds.getAllMetadata()) {
				if (m.getType().equals(ppntype)) {
					ppn = m.getValue();
				}
			}
		}

		if (!isAllowedElement(ds.getType()) || ds.getType().getName().equals("boundbook")) {
			// do nothing, maybe we need other types without urn too?
		} else {
			if (!replaceUrlsOrAddUrn(ds))
				successful = false;
		}

		// if there are elements on the whitelist work the whole tree
		// if the element is an anchor iterate at least to the next child to maybe set
		// the work urn
		if (allowedTypes.length > 0 || ds.getType().isAnchor()) {
			List<DocStruct> dsList = ds.getAllChildren();
			if (dsList != null && dsList.size() > 0) {
				for (DocStruct s : dsList) {
					setUrn(s);
				}
			}
		}
	}

	/**
	 * checks if the DocStructType was whitelisted in the configuration file it will
	 * also whitelist the anchorelement and the topmost element if the plugin was
	 * configured accordingly
	 * 
	 * @param type the DocStructType provided
	 * @return true if the Element is whitelisted, false if not
	 */
	private boolean isAllowedElement(DocStructType type) {
		// whitelist topmost and anchor-element
		if ((type.isAnchor() && setAnchorUrn) || (type.isTopmost() && setWorkUrn))
			return true;

		for (String structName : allowedTypes) {
			if (type.getName().equals(structName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the DocStruct-element allows to write modsUrns
	 * 
	 * @param ds
	 * @return
	 */
	private boolean isAllowedUrn(DocStruct ds, String urnType) {
		for (MetadataType metadataType : ds.getType().getAllMetadataTypes()) {
			if (metadataType.getName().equals(urnType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * tries to find  existing URN of the given Element
	 * @param logical DocStruct element that will be searched
	 * @param urnType Typname of the URN
	 * @return
	 */
	private String findExistingUrn(DocStruct logical, String urnType) {
		if (logical.getAllMetadata() != null) {
			for (Metadata md : logical.getAllMetadata()) {
				if (md.getType().getName().equals(urnType)) {
					return md.getValue();
				}
			}
		}
		return null;
	}

	private boolean replaceUrlsOrAddUrn(DocStruct logical) throws JsonSyntaxException, ClientProtocolException,
			IllegalArgumentException, MetadataTypeNotAllowedException, IOException, InterruptedException,
			WriteException, PreferencesException, SwapException, DAOException, SQLException, UrnDatabaseException {
		boolean foundExistingUrn = false;
		boolean successful = false;
		String metsUrn = findExistingUrn(logical, metsUrnType);
		String modsUrn = findExistingUrn(logical, modsUrnType);

		if (modsUrn != null && metsUrn == null) {
			foundExistingUrn = true;
			Metadata md = new Metadata(prefs.getMetadataTypeByName(metsUrnType));
			md.setValue(modsUrn);
			logical.addMetadata(md);
			successful = true;
			Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO,
					"Notice: Found MODs-URN, copy value to METs");
		}

		if (metsUrn != null) {
			foundExistingUrn = true;
			if (modsUrn != null && !metsUrn.equals(modsUrn)) {
				Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO,
						"Hinweis: unterschiedliche MEDs und MODs URN für das gleiche Element gefunden!");
			}
			if (metsUrn.startsWith(namespace)) {
				 successful = urnClient.replaceUrls(metsUrn, urls);
				if (!successful)
					Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR,
							"URN: " + metsUrn + " could not be updated!");
				else {
					Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO,
							"URN: " + metsUrn + " was updated successfully!");
				}
			} else {
				successful = true;
				Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO,
						"Note: URN: " + metsUrn + "is not part of the namespace and will not be updated!");
			}

		}

		// if no URN found yet register a new one
		if (!foundExistingUrn) {
			boolean modsUrnAllowed = isAllowedUrn(logical, modsUrnType);
			boolean metsUrnAllowed = isAllowedUrn(logical, metsUrnType);

			if (metsUrnAllowed || (modsUrnAllowed && setmodsUrn)) {
				Metadata md = new Metadata(prefs.getMetadataTypeByName(metsUrnType));
				int urnid = urnGenerator.getUrnId(ppn, logical.getType());
				String myNewUrn = UrnGenerator.generateUrn(namespace, infix, urnid);

				try {
					successful  = myNewUrn.equals(urnClient.registerUrn(myNewUrn, urls));
					
				} catch (Exception ex) {
					// if registering the urn fails for any reason delete the database entry
					// but if we can change the urls, continue...
					if (!urnClient.replaceUrls(myNewUrn, urls)) {
						urnGenerator.removeUrnId(urnid);
						successful = false;
						Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR,
								"Couldn't register urn, urnid " + urnid + " was removed from database!");
						throw ex;
					} else {
						successful = true;
					}
				}
				if (metsUrnAllowed) {
					md.setValue(myNewUrn);
					logical.addMetadata(md);
				}

				if (setmodsUrn && modsUrnAllowed) {
					Metadata md2 = new Metadata(prefs.getMetadataTypeByName(modsUrnType));
					md2.setValue(myNewUrn);
					logical.addMetadata(md2);
				}

				Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO,
						"URN: " + myNewUrn + " was created successfully!");

				// maybe better to save the mets file
				// only once but risk loosing a URN?
				step.getProzess().writeMetadataFile(ff);
				successful = true;
			} else {
				Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO, "No URN was created for Element ");
				successful = false;
			}
		}
		return successful;
	}

	@Override
	public PluginReturnValue run() {
		boolean foundExistingUrn = false;

		try {
			urnGenerator = new UrnGenerator();
			urnClient = new UrnRestClient(uri, namespace, apiUser, apiPassword);

			// read mets file
			ff = step.getProzess().readMetadataFile();
			prefs = step.getProzess().getRegelsatz().getPreferences();
			ppntype = prefs.getMetadataTypeByName("CatalogIDDigital");
			DocStruct ds = ff.getDigitalDocument().getLogicalDocStruct();

			// initialize VariableReplacer
			VariableReplacer replacer = new VariableReplacer(ff.getDigitalDocument(), prefs, step.getProzess(), step);

			// create URL and add Value from Configuration
			urls = new ArrayList<String>();
			urls.add(replacer.replace(publicationUrl));

			setUrn(ds);

		} catch (ReadException | JsonException | PreferencesException | WriteException | IOException
				| IllegalArgumentException | InterruptedException | SwapException | DAOException
				| MetadataTypeNotAllowedException | SQLException | JsonSyntaxException | UrnDatabaseException e) {
			log.error(e);
			Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, e.getMessage());
			successful = false;
		}

		log.info("URN step plugin executed");
		Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO,
				successful ? "URN step plugin executed successfully" : "URN step plugin executed with Errors");
		if (!successful) {
			return PluginReturnValue.ERROR;
		}
		return PluginReturnValue.FINISH;
	}
}
