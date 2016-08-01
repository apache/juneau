/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import java.util.*;

import com.ibm.juno.core.filter.*;

/**
 * Transforms {@link Enumeration Enumerations} to {@code List<Object>} objects.
 * <p>
 * 	This is a one-way filter, since {@code Enumerations} cannot be reconstituted.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class EnumerationFilter extends PojoFilter<Enumeration,List> {

	/**
	 * Converts the specified {@link Enumeration} to a {@link List}.
	 */
	@Override /* PojoFilter */
	public List filter(Enumeration o) {
		List l = new LinkedList();
		while (o.hasMoreElements())
			l.add(o.nextElement());
		return l;
	}
}
