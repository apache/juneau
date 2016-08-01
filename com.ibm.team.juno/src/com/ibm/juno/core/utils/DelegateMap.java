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

import com.ibm.juno.core.*;

/**
 * Represents a wrapped {@link Map} where entries in the map can be removed without
 * 	affecting the underlying map.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type of the wrapped bean.
 */
public class DelegateMap<T> extends ObjectMap implements Delegate<T> {
	private static final long serialVersionUID = 1L;

	private transient ClassMeta<T> classMeta;

	DelegateMap(ClassMeta<T> classMeta) {
		this.classMeta = classMeta;
	}

	@Override /* Delegate */
	public ClassMeta<T> getClassMeta() {
		return classMeta;
	}

	/**
	 * Remove all but the specified keys from this map.
	 * <p>
	 * This does not affect the underlying map.
	 *
	 * @param keys The remaining keys in the map (in the specified order).
	 */
	public void filterKeys(List<String> keys) {
		ObjectMap m2 = new ObjectMap();
		for (String k : keys)
			m2.put(k, get(k));
		this.clear();
		this.putAll(m2);
	}
}
