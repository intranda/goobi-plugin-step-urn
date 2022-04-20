package de.intranda.goobi.plugins;

import java.io.IOException;
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
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
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
    private String metadataType;
    private String uri;
    private String namespace;
    private String apiUser;
    private String apiPassword;
    private String returnPath;
    private String publicationUrl;
    private String infix;
    private String[] structElements;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);

        metadataType = myconfig.getString("metadataType", "_urn");
        uri = myconfig.getString("uri", "https://api.nbn-resolving.org/v2/");
        namespace = myconfig.getString("namespace", "urn:nbn:de:gbv:NN");
        apiUser = myconfig.getString("apiUser", "user");
        apiPassword = myconfig.getString("apiPassword", "password");

        // Liste mit Stukturelementen einlesen
        structElements = myconfig.getStringArray("structElements/structElement");
        publicationUrl = myconfig.getString("publicationUrl", "https://viewer.example.org/{meta.CatalogIDDigital}");
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

    private boolean replaceUrlsOrAddUrn(ArrayList<String> urls, DocStruct logical, Prefs prefs, UrnRestClient urnClient, Fileformat ff)
            throws JsonSyntaxException, ClientProtocolException, IllegalArgumentException, MetadataTypeNotAllowedException, IOException, InterruptedException, WriteException, PreferencesException, SwapException, DAOException {

        boolean foundExistingUrn = false;
        boolean successful = false;
        for (Metadata md : logical.getAllMetadata()) {
            if (md.getType().getName().equals(metadataType)) {
                foundExistingUrn = true;
                String existingUrn = md.getValue();
                successful = urnClient.replaceUrls(existingUrn, urls);

                if (!successful)
                    Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, "URN: " + existingUrn + " could not be updated!");
                else {
                    Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO, "URN: " + existingUrn + " was updated successfully!");
                }
            }
        }
        //if no URNs found yet register a new one
        if (!foundExistingUrn) {
            Metadata md = new Metadata(prefs.getMetadataTypeByName(metadataType));
            String myNewUrn = urnClient.createUrn(urls);
            md.setValue(myNewUrn);
            logical.addMetadata(md);
            Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO, "URN: " + myNewUrn + " was created successfully!");
            // save the mets file 
            // only once or risk loosing a URN?
            step.getProzess().writeMetadataFile(ff);
            successful = true;
        }
        return successful;
    }

    @Override
    public PluginReturnValue run() {
        boolean successful = false;
        boolean foundExistingUrn = false;

        try {
            UrnRestClient urnClient = new UrnRestClient(uri, namespace, infix, apiUser, apiPassword);
            // read mets file
            Fileformat ff = step.getProzess().readMetadataFile();
            Prefs prefs = step.getProzess().getRegelsatz().getPreferences();
            DocStruct logical = ff.getDigitalDocument().getLogicalDocStruct();
            // initialize VariableReplacer
            VariableReplacer replacer = new VariableReplacer(ff.getDigitalDocument(), prefs, step.getProzess(), step);
            // create URL-List and add Value from Configuration
            ArrayList<String> urls = new ArrayList<String>();
            urls.add(replacer.replace(publicationUrl));

            //TODO prüfen ob Elemente vorgeben sind oder nicht

            if (structElements.length > 0) {
                // new algorithm
                List<DocStruct> dsList = logical.getAllChildrenAsFlatList();

                for (DocStruct ds : dsList) {
                    String typeName = ds.getType().getName();
                    //TODO remove
                    int i=0;
                    for (String structName : structElements) {
                        if (typeName.equals(structName)) {
                            Helper.addMessageToProcessLog(step.getProcessId(), LogType.INFO, "Found Element: " + structName);
                            //TODO remove
                            urls.set(0, urls.get(0) +"/"+ ++i);
                            successful = replaceUrlsOrAddUrn(urls, ds, prefs, urnClient,ff);
                        }
                    }
                }

            } else {

                if (logical.getType().isAnchor()) {
                    logical = logical.getAllChildren().get(0);
                }
                successful = replaceUrlsOrAddUrn(urls, logical, prefs, urnClient, ff);
            }
        } catch (ReadException | JsonException | PreferencesException | WriteException | IOException | IllegalArgumentException | InterruptedException
                | SwapException | DAOException | MetadataTypeNotAllowedException e) {
            log.error(e);
            Helper.addMessageToProcessLog(step.getProcessId(), LogType.ERROR, e.getMessage());
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
