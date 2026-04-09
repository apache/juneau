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
package org.apache.juneau.ng.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArg;

/**
 * Specifies how collection-valued query/form parameters are serialized to a single string.
 *
 * <p>
 * When a parameter value is an {@link Iterable} or array, the elements are joined using the delimiter
 * corresponding to the selected format.  {@link #REPEATED} instead emits one name=value pair per element.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
public enum CollectionFormat {

	/** Elements joined with {@code ,} — e.g. {@code colors=red,green,blue}. */
	COMMA(","),

	/** Elements joined with {@code |} — e.g. {@code colors=red|green|blue}. */
	PIPE("|"),

	/** Elements joined with a space — e.g. {@code colors=red green blue}. */
	SPACE(" "),

	/** Elements joined with {@code \t} (tab) — e.g. {@code colors=red\tgreen\tblue}. */
	TAB("\t"),

	/** One parameter per element — e.g. {@code colors=red&colors=green&colors=blue}. Handled specially by the request builder. */
	REPEATED(null);

	private final String delimiter;

	CollectionFormat(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Returns the delimiter string, or {@code null} for {@link #REPEATED}.
	 *
	 * @return The delimiter, possibly <jk>null</jk>.
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * Serializes the given values to a single string using this format's delimiter.
	 *
	 * <p>
	 * Only valid for non-{@link #REPEATED} formats.
	 *
	 * @param values The values to join. Must not be <jk>null</jk>.
	 * @return The joined string. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If called on {@link #REPEATED}.
	 */
	public String join(Iterable<?> values) {
		assertArg(delimiter != null, "join() is not supported for CollectionFormat.REPEATED");
		var sb = new StringBuilder();
		var first = true;
		for (var v : values) {
			if (!first)
				sb.append(delimiter);
			sb.append(v);
			first = false;
		}
		return sb.toString();
	}
}
