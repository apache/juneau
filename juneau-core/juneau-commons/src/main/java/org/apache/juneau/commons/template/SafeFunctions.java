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
package org.apache.juneau.commons.template;

import java.util.*;

/**
 * The default function allowlist of a {@link TemplateResolver}.
 *
 * <p>
 * Pure, bounded string and collection helpers, the structure functions and the operators.  Deliberately left out:
 * <ul>
 * 	<li>Functions that read arbitrary data (<c>jsonPath</c>, <c>get</c>, <c>extract</c>, <c>keys</c>, <c>values</c>).
 * 	<li>Regex functions (<c>match</c>, <c>replaceRegex</c>), which can backtrack catastrophically on author-chosen patterns.
 * 	<li>Non-deterministic functions (<c>rand*</c>, <c>uuid</c>, <c>now</c>) and date parsing.
 * 	<li>Functions that can allocate large results from small input (<c>repeat</c>) or that reach the platform
 * 		(<c>switch</c>, the encoding functions).
 * </ul>
 * {@link TemplateResolver.Builder#allowFunctions(String...)} replaces this list.
 */
public final class SafeFunctions {

	private static final List<String> PATTERNS = List.of(
		"upper", "lower", "trim", "len", "size", "substring", "split", "join", "concat", "replace",
		"startsWith", "endsWith", "contains", "if", "coalesce", "notEmpty",
		"ul", "ol", "block", "table", "list", "take",
		"$op.*"
	);

	private SafeFunctions() {}

	/**
	 * Returns the default allowlist patterns.
	 *
	 * @return An unmodifiable list of glob patterns, as accepted by
	 * 	{@link org.apache.juneau.commons.svl.VarResolver.Builder#allowFunctions(String...)}.
	 */
	public static List<String> patterns() { return PATTERNS; }
}
