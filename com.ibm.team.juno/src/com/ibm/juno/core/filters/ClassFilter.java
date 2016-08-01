/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;

/**
 * Transforms {@link Class Classes} to {@link String Strings}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class ClassFilter extends PojoFilter<Class<?>,String> {

	/**
	 * Converts the specified {@link Class} to a {@link String}.
	 */
	@Override /* PojoFilter */
	public String filter(Class<?> o) {
		return o.getName();
	}

	/**
	 * Converts the specified {@link String} to a {@link Class}.
	 */
	@Override /* PojoFilter */
	public Class<?> unfilter(String o, ClassMeta<?> hint) throws ParseException {
		try {
			return Class.forName(o);
		} catch (ClassNotFoundException e) {
			throw new ParseException(e);
		}
	}
}
