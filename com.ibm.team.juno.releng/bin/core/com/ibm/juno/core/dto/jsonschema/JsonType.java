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
 * Represents possible JSON types in the JSON-Schema core specification.
 * <p>
 * 	Implements custom <code>toString()</code> and <code>fromString(String)</code> methods
 * 		that override the default serialization/parsing behavior of <code>Enum</code> types
 * 		so that they are represented in lowercase form (as per the specification).
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	// Produces 'number', not 'NUMBER'.
 * 	String json = JsonSerializer.DEFAULT.serialize(JsonType.NUMBER);
 * </p>
 *
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.jsonschema} for usage information.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public enum JsonType {

	/** array */
	ARRAY("array"),

	/** boolean */
	BOOLEAN("boolean"),

	/** integer */
	INTEGER("integer"),

	/** null */
	NULL("null"),

	/** number */
	NUMBER("number"),

	/** object */
	OBJECT("object"),

	/** string */
	STRING("string"),

	/** any */
	ANY("any");

	private final String value;	// The serialized format of the enum.

	private JsonType(String value) {
		this.value = value;
	}

	/**
	 * Returns the lowercase form of this enum that's compatible with the JSON-Schema specification.
	 */
	@Override /* Object */
	public String toString() {
		return value;
	}

	/**
	 * Converts the specified lowercase form of the enum back into an <code>Enum</code>.
	 *
	 * @param value The lowercase form of the enum (e.g. <js>"array"</js>).
	 * @return The matching <code>Enum</code>, or <jk>null</jk> if no match found.
	 */
	public static JsonType fromString(String value) {
		if (value == null || value.length() < 4)
			return null;
		char c = value.charAt(0);
		if (c == 'a') {
			if (value.equals("array"))
			return ARRAY;
			if (value.equals("any"))
				return ANY;
		}
		if (c == 'b' && value.equals("boolean"))
			return BOOLEAN;
		if (c == 'i' && value.equals("integer"))
			return INTEGER;
		if (c == 'n') {
			c = value.charAt(2);
			if (c == 'l' && value.equals("null"))
				return NULL;
			if (c == 'm' && value.equals("number"))
				return NUMBER;
			return null;
		}
		if (c == 'o' && value.equals("object"))
			return OBJECT;
		if (c == 's' && value.equals("string"))
			return STRING;
		return null;
	}
}
