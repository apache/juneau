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
 * Interface for the resolution of string vars with a default value if the <code>resolve(String)</code> method returns <jk>null</jk>.
 * <p>
 * For example, to resolve the system property <js>"myProperty"</js> but resolve to <js>"not found"</js> if the property doesn't exist:
 * <js>"$S{myProperty,not found}"</js>
 */
public abstract class StringVarWithDefault extends StringVar {

	@Override /* StringVar*/
	public String doResolve(String s) {
		int i = s.indexOf(',');
		if (i == -1)
			return resolve(s);
		String[] s2 = StringUtils.split(s, ',');
		String v = resolve(s2[0]);
		if (v == null)
			v = s2[1];
		return v;
	}
}
