/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.jsonschema;

/**
 * Represents a JSON property in the JSON-Schema core specification.
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.jsonschema} for usage information.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SchemaProperty extends Schema {

	/**
	 * Default constructor.
	 */
	public SchemaProperty() {}

	/**
	 * Convenience constructor.
	 *
	 * @param name The name of this property.
	 */
	public SchemaProperty(String name) {
		setName(name);
	}

	/**
	 * Convenience constructor.
	 *
	 * @param name The name of this property.
	 * @param type The JSON type of this property.
	 */
	public SchemaProperty(String name, JsonType type) {
		setName(name);
		setType(type);
	}
}