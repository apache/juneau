/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.xml.*;

/**
 * Sames as {@link BeanMeta}, except the list of bean properties are limited
 * by a {@link BeanProperty#properties()} annotation.
 *
 * @param <T> The class type that this metadata applies to.
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class BeanMetaFiltered<T> extends BeanMeta<T> {

	private final BeanMeta<T> innerMeta;

	/**
	 * Wrapper constructor.
	 *
	 * @param innerMeta The unfiltered bean meta of the bean property.
	 * @param pNames The list of filtered property names.
	 */
	public BeanMetaFiltered(BeanMeta<T> innerMeta, String[] pNames) {
		this.innerMeta = innerMeta;
		this.properties = new LinkedHashMap<String,BeanPropertyMeta<T>>();
		for (String p : pNames)
			properties.put(p, innerMeta.getPropertyMeta(p));
		this.xmlMeta = new XmlBeanMeta<T>(innerMeta, pNames);
	}

	/**
	 * Wrapper constructor.
	 *
	 * @param innerMeta The unfiltered bean meta of the bean property.
	 * @param pNames The list of filtered property names.
	 */
	public BeanMetaFiltered(BeanMeta<T> innerMeta, Collection<String> pNames) {
		this(innerMeta, pNames.toArray(new String[pNames.size()]));
	}

	@Override /* Delagate */
	public ClassMeta<T> getClassMeta() {
		return innerMeta.classMeta;
	}

	@Override /* BeanMeta */
	public Collection<BeanPropertyMeta<T>> getPropertyMetas() {
		return properties.values();
	}

	@Override /* BeanMeta */
	public BeanPropertyMeta<T> getPropertyMeta(String name) {
		return properties.get(name);
	}

	@Override /* Object */
	public String toString() {
		return innerMeta.c.getName();
	}
}
