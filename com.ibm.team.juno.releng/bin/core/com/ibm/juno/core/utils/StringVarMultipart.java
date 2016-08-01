/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;


/**
 * Interface for the resolution of string vars that consist of a comma-delimited list.
 * <p>
 * (e.g. <js>"$X{foo, bar, baz}"</js>)
 */
public abstract class StringVarMultipart extends StringVar {

	/**
	 * The interface that needs to be implemented for this interface.
	 *
	 * @param args The arguments inside the variable.
	 * @return The resolved variable.
	 */
	public abstract String resolve(String[] args);

	@Override /* StringVar*/
	public String resolve(String s) {
		String[] s2 = s.indexOf(',') == -1 ? new String[]{s} : StringUtils.split(s, ',');
		return resolve(s2);
	}
}
