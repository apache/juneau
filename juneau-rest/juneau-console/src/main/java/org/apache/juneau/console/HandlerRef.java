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
package org.apache.juneau.console;

import java.util.*;
import java.util.regex.*;

/**
 * A reference to a client-side handler: a registered name or an opt-in inline body.
 *
 * <p>
 * Names use the {@code ns.name} grammar and are registered on the page with {@code JuneauViews.handlers.register}.
 * The {@code juneau.*} namespace is reserved for the built-ins (see {@link #builtin(String)}).
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 *   HandlerRef <jv>a</jv> = HandlerRef.<jsm>named</jsm>(<js>"orders.refresh"</js>);
 *   HandlerRef <jv>b</jv> = HandlerRef.<jsm>builtin</jsm>(<js>"refresh"</js>);   <jc>// juneau.refresh</jc>
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845", // Instance field name and the NAME pattern constant are distinct, conventionally named members.
	"java:S5998" // Handler names are short, so the regex repetition depth is bounded.
})
public final class HandlerRef {

	private static final Pattern NAME = Pattern.compile("^[a-z][A-Za-z0-9]*(\\.[a-z][A-Za-z0-9-]*)+$");

	/** The built-in handler names, without the {@code juneau.} prefix. */
	static final Set<String> BUILTINS = Set.of("refresh", "refreshClosest", "expand", "collapse", "activate", "publish", "countLabel", "countBadge");

	private final String name;      // null when inline
	private final String inline;    // null when named

	private HandlerRef(String name, String inline) {
		this.name = name;
		this.inline = inline;
	}

	/**
	 * A registered handler name.
	 *
	 * @param name The name, e.g. {@code orders.refresh}.  A {@code juneau.}-prefixed name is accepted only when it names a built-in.
	 * @return A new reference.
	 * @throws IllegalArgumentException If the name is malformed or uses the reserved namespace.
	 */
	public static HandlerRef named(String name) {
		if (name == null || ! NAME.matcher(name).matches())
			throw new IllegalArgumentException("Handler name '" + name + "' must match ns.name (lower-case first letter per segment, at least one dot).");
		if (name.startsWith("juneau.") && ! BUILTINS.contains(name.substring(7)))
			throw new IllegalArgumentException("Handler name '" + name + "' uses the reserved juneau.* namespace.");
		return new HandlerRef(name, null);
	}

	/**
	 * A built-in handler.
	 *
	 * @param name The built-in name without prefix, e.g. {@code refresh}.
	 * @return A reference to {@code juneau.<name>}.
	 * @throws IllegalArgumentException If the name is not a built-in.
	 */
	public static HandlerRef builtin(String name) {
		if (! BUILTINS.contains(name))
			throw new IllegalArgumentException("'" + name + "' is not a built-in handler: " + new TreeSet<>(BUILTINS));
		return new HandlerRef("juneau." + name, null);
	}

	/**
	 * An inline handler body, compiled on the client as {@code function(target, event)}.
	 * Needs {@code PageSpec.allowInlineHandlers(true)}.
	 *
	 * @param body The function body.
	 * @return A new reference.
	 * @throws IllegalArgumentException If the body is blank.
	 */
	public static HandlerRef inline(String body) {
		if (body == null || body.isBlank())
			throw new IllegalArgumentException("Inline handler body must not be empty.");
		return new HandlerRef(null, body);
	}

	/**
	 * Returns the handler name.
	 *
	 * @return The name, or <jk>null</jk> for an inline reference.
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns whether this is an inline body.
	 *
	 * @return <jk>true</jk> if inline.
	 */
	public boolean isInline() {
		return inline != null;
	}

	/**
	 * Returns the contract JSON form.
	 *
	 * @return The name string, or {@code {"inline": body}}.
	 */
	public Object toJson() {
		return inline != null ? Map.of("inline", inline) : name;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof HandlerRef r && Objects.equals(name, r.name) && Objects.equals(inline, r.inline);
	}

	@Override /* Object */
	public int hashCode() {
		return Objects.hash(name, inline);
	}

	@Override /* Object */
	public String toString() {
		return inline != null ? "inline:" + inline : name;
	}
}
