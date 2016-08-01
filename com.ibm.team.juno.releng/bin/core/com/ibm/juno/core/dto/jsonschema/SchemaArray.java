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
 * Represents a list of {@link Schema} objects.
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.jsonschema} for usage information.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SchemaArray extends LinkedList<Schema> {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public SchemaArray() {}

	/**
	 * Constructor with predefined types to add to this list.
	 *
	 * @param schemas The list of schemas in this array.
	 */
	public SchemaArray(Schema...schemas) {
		addAll(schemas);
	}

	/**
	 * Convenience method for adding one or more {@link Schema} objects to
	 * 	this array.
	 *
	 * @param schemas The {@link Schema} objects to add to this array.
	 * @return This object (for method chaining).
	 */
	public SchemaArray addAll(Schema...schemas) {
		for (Schema s : schemas)
			add(s);
		return this;
	}
}
