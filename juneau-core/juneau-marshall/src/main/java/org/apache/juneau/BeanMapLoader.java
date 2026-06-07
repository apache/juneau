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
package org.apache.juneau;

import java.io.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.parser.*;

/**
 * Marshalling-side helper for loading {@link BeanMap} contents from JSON text or a reader/parser pair.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Populate a bean map from a JSON string.</jc>
 * 	BeanMap&lt;Person&gt; <jv>m</jv> = <jv>bc</jv>.newBeanMap(Person.<jk>class</jk>);
 * 	BeanMapLoader.<jsm>load</jsm>(<jv>m</jv>, <js>"{name:'John',age:21}"</js>);
 * </p>
 */
public final class BeanMapLoader {

	private BeanMapLoader() {}

	/**
	 * Populates the supplied {@link BeanMap} with the contents of the JSON text in {@code input}.
	 *
	 * <p>
	 * Equivalent to the legacy {@code BeanMap.load(String)} method.
	 *
	 * @param <T> The bean type.
	 * @param m The bean map to populate.
	 * @param input The text that will get parsed into a map and then added to {@code m}.
	 * @return The supplied bean map for fluent chaining.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> BeanMap<T> load(BeanMap<T> m, String input) throws ParseException {
		m.putAll(Json5Map.ofString(input));
		return m;
	}

	/**
	 * Populates the supplied {@link BeanMap} with the contents of the reader using the specified {@link ReaderParser}.
	 *
	 * <p>
	 * Equivalent to the legacy {@code BeanMap.load(Reader, ReaderParser)} method.
	 *
	 * @param <T> The bean type.
	 * @param m The bean map to populate.
	 * @param r The reader containing serialized text.
	 * @param p The parser to use to parse the text.
	 * @return The supplied bean map for fluent chaining.
	 * @throws ParseException Malformed input encountered.
	 */
	public static <T> BeanMap<T> load(BeanMap<T> m, Reader r, ReaderParser p) throws ParseException {
		m.putAll(JsonMap.ofString(r, p));
		return m;
	}
}
