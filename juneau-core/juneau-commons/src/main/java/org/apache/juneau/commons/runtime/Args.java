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
package org.apache.juneau.commons.runtime;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.commons.utils.Utils;

/**
 * Lean parser for command-line arguments passed in through a {@code main(String[] args)} method.
 *
 * <p>
 * Supports the following grammars (all enabled by default; toggle via {@link Builder}):
 * <ul>
 * 	<li>Positional arguments — any token that does not start with a recognized flag prefix.
 * 	<li>Short flags — <c>-key value</c> (and repeated <c>-key v1 -key v2</c> to collect multiple values).
 * 	<li>Long flags — <c>--key value</c>.
 * 	<li>Equals form — <c>--key=value</c> / <c>-key=value</c>.
 * 	<li>System-property style — <c>-Dkey=value</c> (the leading {@code D} is stripped).
 * </ul>
 *
 * <p>
 * Positional arguments are only collected before the first flag is seen; once a flag appears, every subsequent
 * non-flag token becomes a value of the most recently seen flag.  This matches the legacy parsing rule and is
 * preserved for backwards compatibility.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Wrap raw command-line args.</jc>
 * 	Args <jv>args</jv> = <jk>new</jk> Args(<jv>argv</jv>);
 *
 * 	<jc>// Positional access.</jc>
 * 	<jv>args</jv>.get(0).ifPresent(System.<jsf>out</jsf>::println);
 *
 * 	<jc>// Named lookup with conversion via Optional.map(...).</jc>
 * 	<jk>int</jk> <jv>port</jv> = <jv>args</jv>.get(<js>"port"</js>).map(Integer::parseInt).orElse(8080);
 *
 * 	<jc>// Multi-value flag.</jc>
 * 	List&lt;String&gt; <jv>tags</jv> = <jv>args</jv>.getAll(<js>"tag"</js>);
 *
 * 	<jc>// Custom grammar.</jc>
 * 	Args <jv>strict</jv> = Args.<jsm>create</jsm>().allowEquals(<jk>false</jk>).caseSensitive(<jk>false</jk>).build(<jv>argv</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is immutable and thread-safe once constructed.
 * 	<li class='note'>{@link #get(String)} returns the <i>first</i> value when a flag is repeated; use {@link #getAll(String)} for the full list.
 * </ul>
 */
public class Args {

	private final List<String> positional;
	private final Map<String,List<String>> options;
	private final boolean caseSensitive;

	/**
	 * Constructor that parses a raw command-line string using the default grammar.
	 *
	 * <p>
	 * The string is split on whitespace honouring single- and double-quoted segments via
	 * {@link org.apache.juneau.commons.utils.StringUtils#splitQuoted(String)}.
	 *
	 * @param line The raw command line.  Can be <jk>null</jk> (treated as an empty argv).
	 */
	public Args(String line) {
		this(create().build(line));
	}

	/**
	 * Constructor that parses an argv array using the default grammar.
	 *
	 * @param argv The arguments passed in through a <c>main(String[] args)</c> method.  Can be <jk>null</jk> (treated as an empty argv).
	 */
	public Args(String[] argv) {
		this(create().build(argv));
	}

	private Args(Args copyFrom) {
		this.positional = copyFrom.positional;
		this.options = copyFrom.options;
		this.caseSensitive = copyFrom.caseSensitive;
	}

	private Args(List<String> positional, Map<String,List<String>> options, boolean caseSensitive) {
		this.positional = Collections.unmodifiableList(positional);
		var copy = new LinkedHashMap<String,List<String>>();
		options.forEach((k,v) -> copy.put(k, Collections.unmodifiableList(v)));
		this.options = Collections.unmodifiableMap(copy);
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Creates a new builder for parsing arguments with non-default grammar.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns the positional argument at the specified zero-based index.
	 *
	 * @param index The position.
	 * @return The argument value, or empty if {@code index} is out of range.
	 */
	public Optional<String> get(int index) {
		if (index < 0 || index >= positional.size())
			return opte();
		return opt(positional.get(index));
	}

	/**
	 * Returns the first value of the named option, or empty if the option was not set.
	 *
	 * <p>
	 * If the option was specified multiple times (e.g. <c>-tag a -tag b</c>), this returns only the first value.
	 * Use {@link #getAll(String)} to get the full list.
	 *
	 * @param key The option name (without the flag prefix).
	 * @return The first value, or empty if the key is unset or has no values.
	 */
	public Optional<String> get(String key) {
		var v = options.get(normalize(key));
		if (e(v))
			return opte();
		return opt(v.get(0));
	}

	/**
	 * Returns all values for the named option in the order they appeared on the command line.
	 *
	 * @param key The option name (without the flag prefix).
	 * @return All values, or an empty list if the key is unset.
	 */
	public List<String> getAll(String key) {
		var v = options.get(normalize(key));
		return v == null ? Collections.emptyList() : v;
	}

	/**
	 * Returns the positional arguments in the order they appeared on the command line.
	 *
	 * @return An unmodifiable list of positional arguments.
	 */
	public List<String> positional() {
		return positional;
	}

	/**
	 * Returns the number of positional arguments.
	 *
	 * @return The count.
	 */
	public int argCount() {
		return positional.size();
	}

	/**
	 * Returns the number of distinct named options.
	 *
	 * @return The count.
	 */
	public int optionCount() {
		return options.size();
	}

	/**
	 * Returns <jk>true</jk> if no positional arguments and no named options were captured.
	 *
	 * @return <jk>true</jk> if both axes are empty.
	 */
	public boolean isEmpty() {
		return positional.isEmpty() && options.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if a positional argument exists at the given index.
	 *
	 * @param index The zero-based index.
	 * @return <jk>true</jk> if an argument exists at that index.
	 */
	public boolean has(int index) {
		return index >= 0 && index < positional.size();
	}

	/**
	 * Returns <jk>true</jk> if the named option appears in the parsed args (even if no values were supplied).
	 *
	 * @param key The option name.
	 * @return <jk>true</jk> if the option was set.
	 */
	public boolean has(String key) {
		return options.containsKey(normalize(key));
	}

	/**
	 * Returns the named-option contents as an unmodifiable map.
	 *
	 * <p>
	 * Positional arguments are <i>not</i> included — use {@link #positional()} for those.
	 *
	 * @return An unmodifiable map of named options to their value lists.
	 */
	public Map<String,List<String>> asMap() {
		return options;
	}

	private String normalize(String key) {
		return caseSensitive ? key : key.toLowerCase(Locale.ROOT);
	}

	/**
	 * Builder for {@link Args} with configurable grammar hooks.
	 *
	 * <p>
	 * Defaults match the legacy {@code -key value [value...]} parsing plus the natural extensions
	 * (<c>--key</c>, <c>--key=value</c>, <c>-Dkey=value</c>).  Toggle individual options off to restrict the grammar.
	 */
	public static class Builder {

		private boolean allowEquals = true;
		private boolean allowShortFlags = true;
		private boolean allowLongFlags = true;
		private boolean allowSystemPropStyle = true;
		private boolean caseSensitive = true;
		private String[] customPrefix;

		/**
		 * Enables or disables recognition of the <c>--key=value</c> / <c>-key=value</c> form.
		 *
		 * @param value <jk>true</jk> to recognize the equals form.  Default <jk>true</jk>.
		 * @return This object.
		 */
		public Builder allowEquals(boolean value) {
			allowEquals = value;
			return this;
		}

		/**
		 * Enables or disables recognition of short-flag prefix <c>-</c>.
		 *
		 * @param value <jk>true</jk> to recognize <c>-key</c>-style flags.  Default <jk>true</jk>.
		 * @return This object.
		 */
		public Builder allowShortFlags(boolean value) {
			allowShortFlags = value;
			return this;
		}

		/**
		 * Enables or disables recognition of long-flag prefix <c>--</c>.
		 *
		 * @param value <jk>true</jk> to recognize <c>--key</c>-style flags.  Default <jk>true</jk>.
		 * @return This object.
		 */
		public Builder allowLongFlags(boolean value) {
			allowLongFlags = value;
			return this;
		}

		/**
		 * Enables or disables Java system-property style <c>-Dkey=value</c> parsing (strips the leading {@code D}).
		 *
		 * @param value <jk>true</jk> to recognize <c>-Dkey=value</c>.  Default <jk>true</jk>.
		 * @return This object.
		 */
		public Builder allowSystemPropStyle(boolean value) {
			allowSystemPropStyle = value;
			return this;
		}

		/**
		 * Controls whether named-option lookups are case-sensitive.
		 *
		 * @param value <jk>true</jk> for case-sensitive lookups.  Default <jk>true</jk>.
		 * @return This object.
		 */
		public Builder caseSensitive(boolean value) {
			caseSensitive = value;
			return this;
		}

		/**
		 * Overrides the default flag prefixes ({@code "-"} and {@code "--"}).
		 *
		 * <p>
		 * Pass an empty array to disable flag parsing entirely (every token becomes positional).  When set, this
		 * supersedes {@link #allowShortFlags(boolean)} and {@link #allowLongFlags(boolean)}.
		 *
		 * @param prefixes The replacement prefix list, or <jk>null</jk> to keep the default behavior.
		 * @return This object.
		 */
		public Builder customPrefix(String...prefixes) {
			customPrefix = prefixes;
			return this;
		}

		/**
		 * Parses the given command-line string into an {@link Args} instance.
		 *
		 * @param line The raw command line.  Can be <jk>null</jk> (treated as an empty argv).
		 * @return A new immutable {@link Args} instance.
		 */
		public Args build(String line) {
			return build(line == null ? new String[0] : splitQuoted(line));
		}

		/**
		 * Parses the given argv array into an {@link Args} instance.
		 *
		 * @param argv The arguments.  Can be <jk>null</jk> (treated as an empty argv).
		 * @return A new immutable {@link Args} instance.
		 */
		@SuppressWarnings({
			"java:S3776", // Cognitive complexity acceptable for argument prefix resolution and parsing logic
			"java:S135"   // Argv tokenizer dispatch loop; per-token continue guards are clearer than restructuring the parse flow.
		})
		public Args build(String[] argv) {
			var prefixes = resolvePrefixes();
			var positional = new ArrayList<String>();
			var options = new LinkedHashMap<String,List<String>>();
			String currentKey = null;

			if (argv != null) {
				for (var token : argv) {
					if (token == null)
						continue;
					var matched = matchPrefix(token, prefixes);
					if (matched == null) {
						if (currentKey != null)
							options.get(currentKey).add(token);
						else
							positional.add(token);
						continue;
					}
					var body = token.substring(matched.length());
					if (allowSystemPropStyle && body.length() > 1 && body.charAt(0) == 'D' && body.indexOf('=') >= 0)
						body = body.substring(1);
					if (allowEquals && body.indexOf('=') >= 0) {
						var eq = body.indexOf('=');
						var key = normalizeKey(body.substring(0, eq));
						var value = body.substring(eq + 1);
						options.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
						currentKey = null;
					} else {
						currentKey = normalizeKey(body);
						options.computeIfAbsent(currentKey, k -> new ArrayList<>());
					}
				}
			}
			return new Args(positional, options, caseSensitive);
		}

		private List<String> resolvePrefixes() {
			List<String> p;
			if (customPrefix != null) {
				p = list(customPrefix);
			} else {
				p = new ArrayList<>();
				if (allowLongFlags)
					p.add("--");
				if (allowShortFlags)
					p.add("-");
			}
			p.removeIf(Utils::e);
			p.sort((a,b) -> Integer.compare(b.length(), a.length()));
			return p;
		}

		private static String matchPrefix(String token, List<String> prefixes) {
			for (var prefix : prefixes) {
				if (token.length() > prefix.length() && token.startsWith(prefix))
					return prefix;
			}
			return null;
		}

		private String normalizeKey(String key) {
			if (ne(key) && ! caseSensitive)
				return key.toLowerCase(Locale.ROOT);
			return key;
		}
	}
}
