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
package org.apache.juneau.jsonschema;

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Represents possible values for JSONSCHEMA_addExamplesTo and JSONSCHEMA_addDescriptionsTo.
 *
 * <ul class='javatree'>
 * 	<li class='jm'>{@link JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}
 * 	<li class='jm'>{@link JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public enum TypeCategory {

	/** Beans */
	BEAN,

	/** Map */
	MAP,

	/** List/Set */
	COLLECTION,

	/** Array */
	ARRAY,

	/** Boolean (including primitives) */
	BOOLEAN,

	/** Short/Integer/Long/Float/Double (including primitives) */
	NUMBER,

	/** String/CharSequence/Character */
	STRING,

	/** Enums */
	ENUM,

	/** Anything else */
	OTHER,

	/** Anything */
	ANY;

	/**
	 * Parses a comma-delimited list of values into a set of {@link TypeCategory} values.
	 *
	 * @param s The comma-delimited string.
	 * @return A comma-delimited list of values into a set of {@link TypeCategory} values.
	 */
	public static Set<TypeCategory> parse(String s) {
		if (s == null || s.isEmpty())
			return Collections.emptySet();
		Set<TypeCategory> set = new LinkedHashSet<>();
		for (String ss : StringUtils.split(s))
			set.add(valueOf(ss.toUpperCase()));
		return set;
	}

	/**
	 * Parses a comma-delimited list of values into an array of {@link TypeCategory} values.
	 *
	 * @param s The comma-delimited string.
	 * @return A comma-delimited list of values into an array of {@link TypeCategory} values.
	 */
	public static TypeCategory[] parseArray(String s) {
		if (s == null || s.isEmpty())
			return new TypeCategory[0];
		List<TypeCategory> x = new ArrayList<>();
		for (String ss : StringUtils.split(s))
			x.add(valueOf(ss.toUpperCase()));
		return x.toArray(new TypeCategory[x.size()]);
	}
}
