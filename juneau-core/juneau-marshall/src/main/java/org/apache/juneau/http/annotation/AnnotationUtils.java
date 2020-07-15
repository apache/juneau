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
			allEmpty(a.description(), a.d(), a._default(), a.df(), a.example(), a.ex(), a.api(), a._enum(), a.e())
			&& allEmpty(a.name(), a.n(), a.value(), a.type(), a.t(), a.format(), a.f(), a.pattern(), a.p(), a.collectionFormat(), a.cf(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo())
			&& allFalse(a.multi(), a.allowEmptyValue(), a.aev(), a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.required(), a.r(), a.uniqueItems(), a.ui(), a.skipIfEmpty(), a.sie())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
			&& a.parser() == HttpPartParser.Null.class
			&& a.serializer() == HttpPartSerializer.Null.class
		;
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
			allEmpty(a.description(), a.d(), a._default(), a.df(), a._enum(), a.e(), a.example(), a.ex(), a.api())
			&& allEmpty(a.name(), a.n(), a.value(), a.type(), a.t(), a.format(), a.f(), a.pattern(), a.p(), a.collectionFormat(), a.cf(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo())
			&& allFalse(a.multi(), a.allowEmptyValue(), a.aev(), a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.required(), a.r(), a.uniqueItems(), a.ui(), a.skipIfEmpty(), a.sie())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
			&& a.parser() == HttpPartParser.Null.class
			&& a.serializer() == HttpPartSerializer.Null.class
		;
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
			allEmpty(a.description(), a.d(), a._default(), a.df(), a._enum(), a.e(), a.example(), a.ex(), a.api())
			&& allEmpty(a.name(), a.value(), a.n(), a.type(), a.t(), a.format(), a.f(), a.pattern(), a.p(), a.collectionFormat(), a.cf(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo())
			&& allFalse(a.multi(), a.allowEmptyValue(), a.aev(), a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.required(), a.r(), a.uniqueItems(), a.ui(), a.skipIfEmpty(), a.sie())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
			&& a.parser() == HttpPartParser.Null.class
			&& a.serializer() == HttpPartSerializer.Null.class
		;
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
			allEmpty(a.description(), a.d(), a.example(), a.ex(), a.examples(), a.exs(), a.api())
			&& a.code().length == 0
			&& a.value().length == 0
			&& a.headers().length == 0
			&& empty(a.schema())
			&& a.parser() == HttpPartParser.Null.class
			&& a.serializer() == HttpPartSerializer.Null.class
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
			allEmpty(a.description(), a.d(), a._default(), a.df(), a._enum(), a.e(), a.example(), a.ex(), a.api())
			&& allEmpty(a.name(), a.n(), a.value(), a.type(), a.t(), a.format(), a.f(), a.collectionFormat(), a.cf(), a.$ref(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo(), a.pattern(), a.p())
			&& allFalse(a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.uniqueItems(), a.ui())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
			&& a.code().length == 0
			&& a.serializer() == HttpPartSerializer.Null.class
		;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.jsonschema.annotation.Schema a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a.description(), a.d(), a._default(), a.df(), a._enum(), a.e(), a.allOf(), a.properties(), a.additionalProperties(), a.xml(), a.example(), a.ex(), a.examples(), a.exs())
			&& allEmpty(a.$ref(), a.format(), a.f(), a.title(), a.multipleOf(), a.mo(), a.maximum(), a.max(), a.minimum(), a.min(), a.pattern(), a.p(), a.type(), a.t(), a.discriminator(), a.collectionFormat(), a.cf(), a.on())
			&& allMinusOne(a.maxProperties(), a.maxp(), a.minProperties(), a.minp())
			&& allFalse(a.ignore(), a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.readOnly(), a.ro(), a.required(), a.r(), a.uniqueItems(), a.ui())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
			&& empty(a.externalDocs())
		;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.jsonschema.annotation.ExternalDocs a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a.description())
			&& allEmpty(a.url())
		;
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
			allEmpty(a.description(), a.d(), a.example(), a.ex(), a.examples(), a.exs(), a.api(), a.value())
			&& allFalse(a.required(), a.r())
			&& empty(a.schema())
		;
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
			&& allEmpty(a.name(), a.url(), a.email())
		;
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
			&& allEmpty(a.name(), a.url())
		;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.jsonschema.annotation.Items a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a._default(), a.df(), a._enum(), a.e())
			&& allEmpty(a.type(), a.t(), a.format(), a.f(), a.collectionFormat(), a.cf(), a.pattern(), a.p(), a.$ref(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo())
			&& allFalse(a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.uniqueItems(), a.ui())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
		;
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(org.apache.juneau.jsonschema.annotation.SubItems a) {
		if (a == null)
			return true;
		return
			allEmpty(a.value(), a._default(), a.df(), a._enum(), a.e(), a.items())
			&& allEmpty(a.type(), a.t(), a.format(), a.f(), a.collectionFormat(), a.cf(), a.pattern(), a.p(), a.$ref(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo())
			&& allFalse(a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.uniqueItems(), a.ui())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
		;
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
			allEmpty(a.description(), a.d(), a._enum(), a.e(), a.example(), a.ex(), a.api())
			&& allEmpty(a.name(), a.value(), a.n(), a.type(), a.t(), a.format(), a.f(), a.pattern(), a.p(), a.maximum(), a.max(), a.minimum(), a.min(), a.multipleOf(), a.mo(), a.collectionFormat(), a.cf())
			&& allFalse(a.exclusiveMaximum(), a.emax(), a.exclusiveMinimum(), a.emin(), a.uniqueItems(), a.ui(), a.allowEmptyValue(), a.aev())
			&& allTrue(a.required(), a.r())
			&& allMinusOne(a.maxLength(), a.maxl(), a.minLength(), a.minl(), a.maxItems(), a.maxi(), a.minItems(), a.mini())
			&& empty(a.items())
			&& a.parser() == HttpPartParser.Null.class
			&& a.serializer() == HttpPartSerializer.Null.class
		;
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
	 * Returns <jk>true</jk> if all the specified string arrays are empty.
	 *
	 * @param strings The strings to test.
	 * @return <jk>true</jk> if all the specified string arrays are empty.
	 */
	protected static boolean allEmpty(String[]...strings) {
		for (String[] s : strings)
			if (s != null && s.length > 0)
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
	 * Returns <jk>true</jk> if all the specified booleans are true.
	 *
	 * @param booleans The booleans to test.
	 * @return <jk>true</jk> if all the specified booleans are true.
	 */
	protected static boolean allTrue(boolean...booleans) {
		for (boolean b : booleans)
			if (! b)
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
}
