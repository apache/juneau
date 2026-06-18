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
package org.apache.juneau.commons.settings;

import java.util.*;

/**
 * A {@link PropertySource} decorator that adds <b>relaxed binding</b> over a delegate source.
 *
 * <p>
 * When a property lookup misses against the delegate, this source retries with a small set of
 * <i>canonical name variants</i> derived from the requested name, so a single logical key can be supplied in any of
 * the common environment conventions and still resolve.  This is the mechanism that lets a Kubernetes-style
 * environment variable such as {@code MY_SECTION_MY_KEY} satisfy a lookup for {@code MySection/myKey}, or
 * {@code MY_PROP} satisfy {@code my.prop} / {@code myProp}.
 *
 * <h5 class='section'>Candidate order</h5>
 * <p>
 * The requested name is always tried <b>verbatim first</b>, so exact-match behavior is preserved unchanged (an
 * already-canonical key resolves with no transformation, and a source that defines the literal name wins).  Only on a
 * miss are the relaxed variants tried, in this order:
 * <ol>
 * 	<li>verbatim (the requested name, unchanged);
 * 	<li><b>upper underscore</b> &mdash; every run of non-alphanumeric characters (and every lower&rarr;upper camel-case
 * 		boundary) becomes a single {@code _}, then the whole thing is upper-cased.  This is the dominant env-var form:
 * 		{@code my.prop}/{@code my-prop}/{@code myProp}/{@code MySection/myKey} &rarr; {@code MY_PROP} /
 * 		{@code MY_SECTION_MY_KEY};
 * 	<li><b>lower dotted</b> &mdash; the same separator collapse but joined with {@code .} and lower-cased
 * 		({@code MY_PROP} &rarr; {@code my.prop}), so a dotted-property lookup can be satisfied by an env-style key and
 * 		vice-versa.
 * </ol>
 *
 * <p>
 * Because the boundary between "section" and "key" in a flat relaxed name (e.g. the underscores in
 * {@code MY_SECTION_MY_KEY}) is inherently ambiguous, this decorator deliberately works in the
 * <b>lookup-key &rarr; candidate-name</b> direction only (the well-defined direction): the caller asks for a known
 * logical key and the decorator generates the environment-convention spellings to probe.  It never tries to reverse a
 * flat env name back into a {@code section/key} pair.
 *
 * <h5 class='section'>Usage</h5>
 * <p class='bjava'>
 * 	<jc>// Wrap the system-env source so env vars bind relaxedly.</jc>
 * 	<jv>settings</jv> = Settings.<jsm>create</jsm>().addSource(<jk>new</jk> RelaxedPropertySource(Settings.<jsf>SYSTEM_ENV_SOURCE</jsf>)).build();
 * </p>
 *
 * @since 10.0.0
 */
public class RelaxedPropertySource implements PropertySource {

	private final PropertySource delegate;

	/**
	 * Constructor.
	 *
	 * @param delegate The wrapped source that the generated candidate names are probed against.  Must not be <jk>null</jk>.
	 */
	public RelaxedPropertySource(PropertySource delegate) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public PropertyLookupResult get(String name) {
		if (name == null)
			return PropertyLookupResult.missing();
		for (var candidate : candidates(name)) {
			var r = delegate.get(candidate);
			if (r.isPresent())
				return r;
		}
		return PropertyLookupResult.missing();
	}

	/**
	 * Generates the ordered, de-duplicated list of candidate names for a requested name (verbatim first).
	 *
	 * @param name The requested property name.
	 * @return The candidate names to probe, in priority order.
	 */
	static List<String> candidates(String name) {
		var out = new ArrayList<String>(3);
		out.add(name);
		var tokens = tokenize(name);
		if (! tokens.isEmpty()) {
			addIfNew(out, String.join("_", tokens).toUpperCase(Locale.ROOT));
			addIfNew(out, String.join(".", tokens).toLowerCase(Locale.ROOT));
		}
		return out;
	}

	/**
	 * Splits a name into lowercase-normalized word tokens, breaking on any non-alphanumeric run and on each
	 * lower&rarr;upper camel-case boundary.  E.g. {@code "MySection/myKey"} &rarr; {@code [my, section, my, key]}.
	 */
	private static List<String> tokenize(String name) {
		var tokens = new ArrayList<String>();
		var sb = new StringBuilder();
		char prev = 0;
		for (var i = 0; i < name.length(); i++) {
			var c = name.charAt(i);
			if (! Character.isLetterOrDigit(c)) {
				// Separator (., -, /, _, space, …): flush the current token.
				if (sb.length() > 0) {
					tokens.add(sb.toString());
					sb.setLength(0);
				}
			} else {
				// camelCase boundary: lower/digit followed by upper starts a new token.
				if (sb.length() > 0 && Character.isUpperCase(c) && (Character.isLowerCase(prev) || Character.isDigit(prev))) {
					tokens.add(sb.toString());
					sb.setLength(0);
				}
				sb.append(Character.toLowerCase(c));
			}
			prev = c;
		}
		if (sb.length() > 0)
			tokens.add(sb.toString());
		return tokens;
	}

	private static void addIfNew(List<String> out, String candidate) {
		if (! out.contains(candidate))
			out.add(candidate);
	}
}
