/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

/**
 * Interface for the resolution of string variables using the {@link StringVarResolver} API.
 *
 * @author jbognar
 */
public abstract class StringVar {

	/**
	 * The method called from {@link StringVarResolver}.
	 * Can be overridden to intercept the request and do special handling.
	 * Default implementation simply calls resolve(String).
	 *
	 * @param arg The inside argument of the variable.
	 * @return The resolved value.
	 */
	protected String doResolve(String arg) {
		return resolve(arg);
	}

	/**
	 * The interface that needs to be implemented for string vars.
	 *
	 * @param arg The inside argument of the variable.
	 * @return The resolved value.
	 */
	public abstract String resolve(String arg);
}
