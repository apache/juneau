// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.bean.jsonschema;

/**
 * Represents possible JSON types in the JSON-Schema core specification.
 *
 * <p>
 * Implements custom <c>toString()</c> and <c>fromString(String)</c> methods that override the default
 * serialization/parsing behavior of <c>Enum</c> types so that they are represented in lowercase form (as per the
 * specification).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Produces 'number', not 'NUMBER'.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(JsonType.<jsf>NUMBER</jsf>);
 * </p>
 */
public enum JsonType {

	/** JSON type {@code array}. */
	ARRAY("array"),

	/** JSON type {@code boolean}. */
	BOOLEAN("boolean"),

	/** JSON type {@code integer}. */
	INTEGER("integer"),

	/** JSON type {@code null}. */
	NULL("null"),

	/** JSON type {@code number}. */
	NUMBER("number"),

	/** JSON type {@code object}. */
	OBJECT("object"),

	/** JSON type {@code string}. */
	STRING("string"),

	/** JSON type {@code any}. */
	ANY("any");

	private final String value;	// The serialized format of the enum.

	JsonType(String value) {
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
	 * Converts the specified lowercase form of the enum back into an <c>Enum</c>.
	 *
	 * @param value The lowercase form of the enum (e.g. <js>"array"</js>).
	 * @return The matching <c>Enum</c>, or <jk>null</jk> if no match found.
	 */
	public static JsonType fromString(String value) {
		if (value == null || value.length() < 4)
			return null;
		var c = value.charAt(0);
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