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
 * Represents a wrapped {@link Collection} where entries in the list can be removed or reordered without
 * 	affecting the underlying list.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 * @param <T> The class type of the wrapped bean.
 */
public class DelegateList<T extends Collection<?>> extends ObjectList implements Delegate<T> {
	private static final long serialVersionUID = 1L;

	private transient ClassMeta<T> classMeta;

	DelegateList(ClassMeta<T> classMeta) {
		this.classMeta = classMeta;
	}

	@Override /* Delegate */
	public ClassMeta<T> getClassMeta() {
		return classMeta;
	}
}
