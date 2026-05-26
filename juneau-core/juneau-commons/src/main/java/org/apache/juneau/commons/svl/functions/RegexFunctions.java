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
package org.apache.juneau.commons.svl.functions;

import java.util.regex.*;

import org.apache.juneau.commons.svl.*;

/**
 * Regex functions for the {@code #{...}} script catalog.
 *
 * <p>
 * Replacements for {@code PatternMatchVar} ({@code $PM}), {@code PatternExtractVar}
 * ({@code $PE}), and {@code PatternReplaceVar} ({@code $PR}) per the migration inventory.
 *
 * <p>
 * Note that {@link StringFunctions.Replace} is the literal-string replace; {@link ReplaceRegex}
 * is the regex form.
 */
public final class RegexFunctions {

	private RegexFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Match.class, Extract.class, ReplaceRegex.class
	};

	/** {@code #{match(s, regex)}} — replacement for the legacy {@code $PM{...}}. Returns {@code "true"} or {@code "false"}. */
	public static class Match extends TypedFunction {
		@Override public String name() { return "match"; }
		public String invoke(String s, String regex) {
			if (s == null || regex == null) return "false";
			return String.valueOf(Pattern.compile(regex).matcher(s).find());
		}
	}

	/**
	 * {@code #{extract(s, regex)}} / {@code #{extract(s, regex, group)}} — replacement for the
	 * legacy {@code $PE{...}}. Returns the first match; if {@code group} is supplied, returns
	 * that capture group from the first match.
	 */
	public static class Extract extends TypedFunction {
		@Override public String name() { return "extract"; }
		public String invoke(String s, String regex) { return invoke(s, regex, 0); }
		public String invoke(String s, String regex, int group) {
			if (s == null || regex == null) return "";
			var m = Pattern.compile(regex).matcher(s);
			if (!m.find()) return "";
			if (group < 0 || group > m.groupCount()) return "";
			var v = m.group(group);
			return v == null ? "" : v;
		}
	}

	/** {@code #{replaceRegex(s, regex, replacement)}} — replacement for the legacy {@code $PR{...}}. */
	public static class ReplaceRegex extends TypedFunction {
		@Override public String name() { return "replaceRegex"; }
		public String invoke(String s, String regex, String replacement) {
			if (s == null) return "";
			if (regex == null) return s;
			return Pattern.compile(regex).matcher(s).replaceAll(replacement == null ? "" : replacement);
		}
	}
}
