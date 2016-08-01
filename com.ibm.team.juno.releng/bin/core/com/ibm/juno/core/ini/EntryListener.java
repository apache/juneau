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
 * Listener that can be used to listen for change events for a specific entry in a config file.
 * <p>
 * Use the {@link ConfigFile#addListener(ConfigFileListener)} method to register listeners.
 */
public class EntryListener extends ConfigFileListener {

	private String fullKey;

	/**
	 * Constructor.
	 *
	 * @param fullKey The key in the config file to listen for changes on.
	 */
	public EntryListener(String fullKey) {
		this.fullKey = fullKey;
	}

	@Override /* ConfigFileListener */
	public void onChange(ConfigFile cf, Set<String> changes) {
		if (changes.contains(fullKey))
			onChange(cf);
	}

	/**
	 * Signifies that the config file entry changed.
	 *
	 * @param cf The config file being changed.
	 */
	public void onChange(ConfigFile cf) {}
}
