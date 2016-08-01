/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.util.*;

/**
 * Combination of a {@link LinkedList} and <code>IdentitySet</code>.
 * <ul>
 * 	<li>Duplicate objects (by identity) will be skipped during insertion.
 * 	<li>Order of insertion maintained.
 * </ul>
 * <p>
 * 	Note:  This class is NOT thread safe, and is intended for use on small lists.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> Entry type.
 */
public class IdentityList<T> extends LinkedList<T> {

	private static final long serialVersionUID = 1L;

	@Override /* List */
	public boolean add(T t) {
		for (T t2 : this)
			if (t2 == t)
				return false;
		super.add(t);
		return true;
	}

	@Override /* List */
	public boolean contains(Object t) {
		for (T t2 : this)
			if (t2 == t)
				return true;
		return false;
	}
}
