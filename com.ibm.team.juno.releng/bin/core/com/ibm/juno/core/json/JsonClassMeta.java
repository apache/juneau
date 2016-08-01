/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import com.ibm.juno.core.json.annotation.*;
import com.ibm.juno.core.utils.*;

/**
 * Metadata on classes specific to the JSON serializers and parsers pulled from the {@link Json @Json} annotation on the class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class JsonClassMeta {

	private final Json json;
	private final String wrapperAttr;

	/**
	 * Constructor.
	 *
	 * @param c The class that this annotation is defined on.
	 */
	public JsonClassMeta(Class<?> c) {
		this.json = ReflectionUtils.getAnnotation(Json.class, c);
		if (json != null) {
			wrapperAttr = StringUtils.nullIfEmpty(json.wrapperAttr());
		} else {
			wrapperAttr = null;
		}
	}

	/**
	 * Returns the {@link Json} annotation defined on the class.
	 *
	 * @return The value of the {@link Json} annotation, or <jk>null</jk> if not specified.
	 */
	protected Json getAnnotation() {
		return json;
	}

	/**
	 * Returns the {@link Json#wrapperAttr()} annotation defined on the class.
	 *
	 * @return The value of the {@link Json#wrapperAttr()} annotation, or <jk>null</jk> if not specified.
	 */
	protected String getWrapperAttr() {
		return wrapperAttr;
	}
}
