/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

/**
 * Resuable common StringVar resolvers.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class StringVars {

	/**
	 * Reusable string variable for system properties.
	 */
	public static final StringVar SYSTEM_PROPERTIES_VAR = new StringMapVar(System.getProperties());

	/**
	 * Reusable string variable for environment variables.
	 */
	public static final StringVar ENV_VARIABLES_VAR = new StringVarWithDefault() {
		@Override /* StringVar */
		public String resolve(String varVal) {
			// Note that lookup is case-insensitive on windows.
			return System.getenv(varVal);
		}
	};
}
