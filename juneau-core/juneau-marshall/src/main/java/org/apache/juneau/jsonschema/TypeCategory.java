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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import java.util.*;

/**
 * Represents possible values for JSONSCHEMA_addExamplesTo and JSONSCHEMA_addDescriptionsTo.
 *
 * <ul class='javatree'>
 * 	<li class='jm'>{@link JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}
 * 	<li class='jm'>{@link JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonSchemaDetails">JSON-Schema Support</a>
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
		Set<TypeCategory> set = set();
		split(s, x -> set.add(valueOf(x.toUpperCase())));
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
		List<TypeCategory> list = list();
		split(s, x -> list.add(valueOf(x.toUpperCase())));
		return list.toArray(new TypeCategory[list.size()]);
	}
}
