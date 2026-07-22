/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.jsonschema;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;

/**
 * Utilities for working with the schema annotations.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
public class SchemaUtils {

	/**
	 * Prevents instantiation.
	 */
	private SchemaUtils() {}

	/**
	 * Joins an array of strings with newlines.
	 *
	 * @param s The arrays to join.  Must not be <jk>null</jk>.
	 * @return A new joined string.
	 */
	public static String joinnl(String[]...s) {
		for (var ss : s) {
			if (ss.length != 0)
				return StringUtils.joinnl(ss).trim();
		}
		return "";
	}

	/**
	 * Parses a generic object as JSON and converts it to an {@link JsonMap}.
	 *
	 * @param o The object to convert.  Can be <jk>null</jk> (returns a new empty {@link JsonMap}).
	 * @return
	 * 	The parsed contents (a new empty {@link JsonMap} if {@code o} is <jk>null</jk> or an empty string),
	 * 	never <jk>null</jk>.
	 * @throws ParseException Invalid JSON encountered.
	 */
	public static JsonMap parseMap(Object o) throws ParseException {
		if (o == null)
			return new JsonMap();
		if (o instanceof String[] o2)
			o = joinnl(o2);
		if (o instanceof String o2) {
			var s = o2;
			if (s.isEmpty())
				return new JsonMap();
			if ("IGNORE".equalsIgnoreCase(s))
				return JsonMap.of("ignore", true);
			if (! isProbablyJsonObject(s, true))
				s = "{" + s + "}";
			return new JsonMap(Json5Map.ofString(s));
		}
		if (o instanceof JsonMap o2)
			return o2;
		if (o instanceof MarshalledMap o2)
			return new JsonMap(o2);
		throw new ParseException("Unexpected data type '%s'.  Expected Map or String.", cn(o));
	}

	/**
	 * Concatenates and parses a string array as a JSON object.
	 *
	 * @param ss The array to concatenate and parse.  Must not be <jk>null</jk>.
	 * @return
	 * 	The parsed contents (a new empty {@link JsonMap} if the array is empty or joins to an empty string),
	 * 	never <jk>null</jk>.
	 * @throws ParseException Invalid JSON encountered.
	 */
	public static JsonMap parseMap(String[] ss) throws ParseException {
		if (ss.length == 0)
			return new JsonMap();
		String s = joinnl(ss);
		if (s.isEmpty())
			return new JsonMap();
		if (! isProbablyJsonObject(s, true))
			s = "{" + s + "}";
		return new JsonMap(Json5Map.ofString(s));
	}

	/**
	 * Concatenates and parses a string array as JSON array or comma-delimited list.
	 *
	 * @param ss The array to concatenate and parse.  Must not be <jk>null</jk>.
	 * @return
	 * 	The parsed contents (a new empty mutable {@link Set} if the array is empty or joins to an empty string),
	 * 	never <jk>null</jk>.
	 * @throws ParseException Invalid JSON encountered.
	 */
	public static Set<String> parseSet(String[] ss) throws ParseException {
		Set<String> set = set();
		if (ss.length == 0)
			return set;
		String s = joinnl(ss);
		if (s.isEmpty())
			return set;
		Json5List.ofJson5OrCdl(s).forEach(x -> set.add(x.toString()));
		return set;
	}
}
