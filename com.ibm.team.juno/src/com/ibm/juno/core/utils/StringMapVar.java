/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.util.*;

/**
 * A simple extension of {@link StringVarWithDefault} that simply pulls
 * 	values from a {@link Map}.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("rawtypes")
public class StringMapVar extends StringVarWithDefault {

	private final Map m;

	/**
	 * Constructor.
	 *
	 * @param m The map to pull values from.
	 */
	public StringMapVar(Map m) {
		this.m = m;
	}

	@Override /* StringVar */
	public String resolve(String varVal) {
		if (m == null)
			return null;
		Object o = m.get(varVal);
		return (o == null ? null : o.toString());
	}
}
