/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;

import java.util.*;


/**
 * Listener that can be used to listen for change events in config files.
 * <p>
 * Use the {@link ConfigFile#addListener(ConfigFileListener)} method to register listeners.
 */
public class ConfigFileListener {

	/**
	 * Gets called immediately after a config file has been loaded.
	 *
	 * @param cf The config file being loaded.
	 */
	public void onLoad(ConfigFile cf) {}

	/**
	 * Gets called immediately after a config file has been saved.
	 *
	 * @param cf The config file being saved.
	 */
	public void onSave(ConfigFile cf) {}

	/**
	 * Signifies that the specified values have changed.
	 *
	 * @param cf The config file being modified.
	 * @param changes The full keys (e.g. <js>"Section/key"</js>) of entries that have changed in the config file.
	 */
	public void onChange(ConfigFile cf, Set<String> changes) {}
}
