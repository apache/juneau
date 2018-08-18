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
package org.apache.juneau.http.annotation;

import org.apache.juneau.httppart.*;

/**
 * Various reusable utility methods when working with annotations.
 */
public class AnnotationUtils {

	//=================================================================================================================
	// Methods for checking if annotations are empty.
	//=================================================================================================================

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Query a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Header a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(FormData a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.collectionFormat(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.allowEmptyValue(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Response a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a.example(), a.examples(), a.api())
			&& a.headers().length == 0
			&& empty(a.schema())
		;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ResponseHeader a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._default(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.collectionFormat(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Schema a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a.description(), a._default(), a._enum(), a.allOf(), a.properties(), a.additionalProperties(), a.xml(), a.example(), a.examples())
			&& allEmpty(a.$ref(), a.format(), a.title(), a.multipleOf(), a.maximum(), a.minimum(), a.pattern(), a.type(), a.discriminator())
			&& allMinusOne(a.maxProperties(), a.minProperties())
			&& allFalse(a.ignore(), a.exclusiveMaximum(), a.exclusiveMinimum(), a.readOnly(), a.required(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items())
			&& empty(a.externalDocs());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(ExternalDocs a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a.description())
			&& allEmpty(a.url());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Body a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a.example(), a.examples(), a.api(), a.value())
			&& allFalse(a.required())
			&& empty(a.schema());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Contact a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value())
			&& allEmpty(a.name(), a.url(), a.email());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(License a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value())
			&& allEmpty(a.name(), a.url());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Items a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a._default(), a._enum())
			&& allEmpty(a.type(), a.format(), a.collectionFormat(), a.pattern(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(SubItems a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a._default(), a._enum(), a.items())
			&& allEmpty(a.type(), a.format(), a.collectionFormat(), a.pattern(), a.$ref(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum(), a.uniqueItems())
			&& allMinusOne(a.maxLength(), a.minLength(), a.maxItems(), a.minItems());
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Path a) {
		if (a == null)
			return true;
		return
			allEmpty(a.description(), a._enum(), a.example(), a.api())
			&& allEmpty(a.name(), a.value(), a.type(), a.format(), a.pattern(), a.maximum(), a.minimum(), a.multipleOf())
			&& allFalse(a.exclusiveMaximum(), a.exclusiveMinimum())
			&& allMinusOne(a.maxLength(), a.minLength())
			&& empty(a.items());
	}

	/**
	 * Returns <jk>true</jk> if all the specified strings are empty or null.
	 *
	 * @param strings The strings to test.
	 * @return <jk>true</jk> if all the specified strings are empty or null.
	 */
	protected static boolean allEmpty(String...strings) {
		if (strings != null)
			for (String s : strings)
				if (s != null && ! s.isEmpty())
					return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all the specified strings are empty or null.
	 *
	 * @param strings The strings to test.
	 * @return <jk>true</jk> if all the specified strings are empty or null.
	 */
	protected static boolean allEmpty(String[]...strings) {
		for (String[] s : strings)
			if (s != null && s.length > 0 && ! allEmpty(s))
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all the specified booleans are false.
	 *
	 * @param booleans The booleans to test.
	 * @return <jk>true</jk> if all the specified booleans are false.
	 */
	protected static boolean allFalse(boolean...booleans) {
		for (boolean b : booleans)
			if (b)
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all the specified longs are -1.
	 *
	 * @param longs The booleans to test.
	 * @return <jk>true</jk> if all the specified longs are -1.
	 */
	protected static boolean allMinusOne(long...longs) {
		for (long i : longs)
			if (i != -1)
				return false;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if the part parser should be used on the specified part.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the part parser should be used on the specified part.
	 */
	public static boolean usePartParser(Body a) {
		return
			a.usePartParser()
			|| a.partParser() != HttpPartParser.Null.class
			|| ! empty(a.schema());
	}

	/**
	 * Returns <jk>true</jk> if the part parser should be used on the specified part.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the part parser should be used on the specified part.
	 */
	public static boolean usePartParser(Response a) {
		return
			a.usePartParser()
			|| a.partParser() != HttpPartParser.Null.class
			|| ! empty(a.schema());
	}

	/**
	 * Returns <jk>true</jk> if the part serializer should be used on the specified part.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the part serializer should be used on the specified part.
	 */
	public static boolean usePartSerializer(Body a) {
		return
			a.usePartSerializer()
			|| a.partSerializer() != HttpPartSerializer.Null.class
			|| ! empty(a.schema());
	}

	/**
	 * Returns <jk>true</jk> if the part serializer should be used on the specified part.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the part serializer should be used on the specified part.
	 */
	public static boolean usePartSerializer(Response a) {
		return
			a.usePartSerializer()
			|| a.partSerializer() != HttpPartSerializer.Null.class
			|| ! empty(a.schema());
	}
}
