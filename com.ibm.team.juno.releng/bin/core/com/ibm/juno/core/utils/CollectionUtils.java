/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.util.*;

/**
 * Utility methods for collections.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class CollectionUtils {

	/**
	 * Reverses the order of a {@link LinkedHashMap}.
	 *
	 * @param in The map to reverse the order on.
	 * @return A new {@link LinkedHashMap} with keys in reverse order.
	 */
	public static <K,V> LinkedHashMap<K,V> reverse(LinkedHashMap<K,V> in) {
		if (in == null)
			return null;
		LinkedHashMap<K,V> m = new LinkedHashMap<K,V>();

		// Note:  Entry objects are reusable in an entry set, so we simply can't
		// create a reversed iteration of that set.
		List<K> keys = new ArrayList<K>(in.keySet());
		List<V> values = new ArrayList<V>(in.values());
		for (int i = in.size()-1; i >= 0; i--)
			m.put(keys.get(i), values.get(i));

		return m;
	}

	/**
	 * Add a value to a list if the value is not null.
	 *
	 * @param l The list to add to.
	 * @param o The element to add.
	 * @return The same list.
	 */
	public static <T> List<T> addIfNotNull(List<T> l, T o) {
		if (o != null)
			l.add(o);
		return l;
	}
}
