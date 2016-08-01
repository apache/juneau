/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.ini;

/**
 * Internal utility methods.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ConfigUtils {

	static final String getSectionName(String key) {
		int i = key.indexOf('/');
		if (i == -1)
			return "default";
		return key.substring(0, i);
	}

	static final String getSectionKey(String key) {
		int i = key.indexOf('/');
		if (i == -1)
			return key;
		return key.substring(i+1);
	}

	static final String getFullKey(String section, String key) {
		if (section.equals("default"))
			return key;
		return section + '/' + key;
	}
}
