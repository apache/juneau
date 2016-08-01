/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.jsonschema;

import java.util.*;

/**
 * Represents a list of {@link JsonType} objects.
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.jsonschema} for usage information.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class JsonTypeArray extends LinkedList<JsonType> {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public JsonTypeArray() {}

	/**
	 * Constructor with predefined types to add to this list.
	 *
	 * @param types The list of types to add to the list.
	 */
	public JsonTypeArray(JsonType...types) {
		addAll(types);
	}

	/**
	 * Convenience method for adding one or more {@link JsonType} objects to
	 * 	this array.
	 *
	 * @param types The {@link JsonType} objects to add to this array.
	 * @return This object (for method chaining).
	 */
	public JsonTypeArray addAll(JsonType...types) {
		for (JsonType t : types)
			add(t);
		return this;
	}
}
