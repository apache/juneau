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

import com.ibm.juno.core.utils.*;


/**
 * Listener that can be used to listen for change events for a specific section in a config file.
 * <p>
 * Use the {@link ConfigFile#addListener(ConfigFileListener)} method to register listeners.
 */
public class SectionListener extends ConfigFileListener {

	private boolean isDefault;
	private String prefix;

	/**
	 * Constructor.
	 *
	 * @param section The name of the section in the config file to listen to.
	 */
	public SectionListener(String section) {
		isDefault = StringUtils.isEmpty(section);
		prefix = isDefault ? null : (section + '/');
	}

	@Override /* ConfigFileListener */
	public void onChange(ConfigFile cf, Set<String> changes) {
		for (String c : changes) {
			if (isDefault) {
				if (c.indexOf('/') == -1) {
					onChange(cf);
					return;
				}
			} else {
				if (c.startsWith(prefix)) {
					onChange(cf);
					return;
				}
			}
		}
	}

	/**
	 * Signifies that the config file entry changed.
	 *
	 * @param cf The config file being modified.
	 */
	public void onChange(ConfigFile cf) {}
}
