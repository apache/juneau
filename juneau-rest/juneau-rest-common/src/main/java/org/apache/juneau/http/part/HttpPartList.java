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
package org.apache.juneau.http.part;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.*;

/**
 * A mutable, ordered list of {@link HttpPart} instances.
 *
 * <p>
 * Mirrors the semantics of {@code PartList} from {@code juneau-rest-common-classic} without the
 * Apache HttpCore dependency. Part-name comparisons are case-sensitive by default (matching the
 * classic default for parts; headers use case-insensitive comparisons).
 *
 * <p>
 * Use {@link PartList} when you need an immutable list bound to an HTTP body (e.g.
 * {@code application/x-www-form-urlencoded}); use {@code HttpPartList} when you need a mutable
 * builder for default form data / query parameter lists.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}). It is not API-frozen: binary- and source-incompatible changes may appear in
 * the <b>next major</b> Juneau release (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S2160" // equals() inherited from ArrayList; list equality is element-based which is correct
})
public class HttpPartList extends ArrayList<HttpPart> {

	private static final long serialVersionUID = 1L;

	/** Represents no part list in annotations. */
	public static final class Void extends HttpPartList {
		private static final long serialVersionUID = 1L;
	}

	private boolean caseInsensitive;

	/**
	 * Creates a new empty {@link HttpPartList}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpPartList create() {
		return new HttpPartList();
	}

	/**
	 * Creates a new {@link HttpPartList} initialized with the given parts.
	 *
	 * @param parts The parts to include. {@code null} entries are ignored.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpPartList of(HttpPart...parts) {
		return new HttpPartList().append(parts);
	}

	/**
	 * Creates a new {@link HttpPartList} initialized with the given parts.
	 *
	 * @param parts The parts to include. {@code null} entries are ignored.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpPartList of(List<HttpPart> parts) {
		return new HttpPartList().append(parts);
	}

	/**
	 * Creates a new {@link HttpPartList} initialized with the given alternating {@code name}/{@code value} pairs.
	 *
	 * @param pairs Alternating name/value strings. Can be <jk>null</jk> (treated as empty). Length must be even.
	 * @return A new instance. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the number of arguments is odd.
	 */
	public static HttpPartList ofPairs(String...pairs) {
		var x = new HttpPartList();
		if (pairs == null)
			pairs = new String[0];
		assertArg(pairs.length % 2 == 0, "Odd number of parameters passed into HttpPartList.ofPairs()");
		for (var i = 0; i < pairs.length; i += 2)
			x.add(HttpPartBean.of(pairs[i], pairs[i + 1]));
		return x;
	}

	/** Constructor. */
	public HttpPartList() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The list to copy. Must not be <jk>null</jk>.
	 */
	protected HttpPartList(HttpPartList copyFrom) {
		super(copyFrom);
		this.caseInsensitive = copyFrom.caseInsensitive;
	}

	/**
	 * Sets whether part-name comparisons are case-insensitive. Defaults to {@code false} (case-sensitive).
	 *
	 * @param value New value.
	 * @return This object.
	 */
	public HttpPartList caseInsensitive(boolean value) {
		this.caseInsensitive = value;
		return this;
	}

	/**
	 * Appends the given part.
	 *
	 * @param value The part to add. {@code null} is ignored.
	 * @return This object.
	 */
	public HttpPartList append(HttpPart value) {
		if (value != null)
			add(value);
		return this;
	}

	/**
	 * Appends the given parts in order.
	 *
	 * @param values The parts to add. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpPartList append(HttpPart...values) {
		if (values != null)
			for (var v : values)
				append(v);
		return this;
	}

	/**
	 * Appends the given parts in order.
	 *
	 * @param values The parts to add. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpPartList append(List<HttpPart> values) {
		if (values != null)
			values.forEach(this::append);
		return this;
	}

	/**
	 * Appends a part constructed from the given name and string value.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpPartList append(String name, String value) {
		return append(HttpPartBean.of(name, value));
	}

	/**
	 * Sets the given part as a default. If a part with the same name already exists, this is a no-op
	 * (first-in-chain wins per name).
	 *
	 * @param value The part to set. {@code null} is ignored.
	 * @return This object.
	 */
	public HttpPartList setDefault(HttpPart value) {
		if (value != null && !contains(value.getName()))
			add(value);
		return this;
	}

	/**
	 * Sets the given parts as defaults, in order. For each, if a part with the same name already exists,
	 * the entry is skipped (first-in-chain wins per name).
	 *
	 * @param values The parts to set. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpPartList setDefault(HttpPart...values) {
		if (values != null)
			for (var v : values)
				setDefault(v);
		return this;
	}

	/**
	 * Sets a default part with the given name and value (first-in-chain wins per name).
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpPartList setDefault(String name, String value) {
		return setDefault(HttpPartBean.of(name, value));
	}

	/**
	 * Sets the given part, replacing any prior entry with the same name.
	 *
	 * @param value The part to set. {@code null} is ignored.
	 * @return This object.
	 */
	public HttpPartList set(HttpPart value) {
		if (value == null)
			return this;
		removeAll(value.getName());
		add(value);
		return this;
	}

	/**
	 * Sets the given parts, each replacing any prior entry with the same name.
	 *
	 * @param values The parts to set. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpPartList set(HttpPart...values) {
		if (values != null)
			for (var v : values)
				set(v);
		return this;
	}

	/**
	 * Sets a part with the given name and value, replacing any prior entry with the same name.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @param value The part value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpPartList set(String name, String value) {
		return set(HttpPartBean.of(name, value));
	}

	/**
	 * Removes all parts with the given name.
	 *
	 * @param name The part name. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpPartList removeAll(String name) {
		assertArgNotNull("name", name);
		removeIf(p -> nameMatches(p, name));
		return this;
	}

	/**
	 * Returns {@code true} if a part with the given name is present.
	 *
	 * @param name The part name.
	 * @return {@code true} if present.
	 */
	public boolean contains(String name) {
		return stream().anyMatch(p -> nameMatches(p, name));
	}

	/**
	 * Returns the first part with the given name, or {@code null} if absent.
	 *
	 * @param name The part name.
	 * @return The first matching part, or {@code null}.
	 */
	public HttpPart getFirst(String name) {
		return stream().filter(p -> nameMatches(p, name)).findFirst().orElse(null);
	}

	/**
	 * Returns the last part with the given name, or {@code null} if absent.
	 *
	 * @param name The part name.
	 * @return The last matching part, or {@code null}.
	 */
	public HttpPart getLast(String name) {
		HttpPart last = null;
		for (var p : this)
			if (nameMatches(p, name))
				last = p;
		return last;
	}

	/**
	 * Returns all parts with the given name in insertion order.
	 *
	 * @param name The part name.
	 * @return A new list of matching parts. Never <jk>null</jk>.
	 */
	public List<HttpPart> getAll(String name) {
		return stream().filter(p -> nameMatches(p, name)).toList();
	}

	/**
	 * Returns all parts in this list as a typed array.
	 *
	 * @return A new array. Never <jk>null</jk>.
	 */
	public HttpPart[] getAll() {
		return toPartArray();
	}

	/**
	 * Performs an action on each part whose name matches.
	 *
	 * @param name The part name.
	 * @param action The action.
	 * @return This object.
	 */
	public HttpPartList forEach(String name, Consumer<HttpPart> action) {
		for (var p : this)
			if (nameMatches(p, name))
				action.accept(p);
		return this;
	}

	/**
	 * Returns a copy of this list.
	 *
	 * @return A new copy. Never <jk>null</jk>.
	 */
	public HttpPartList copy() {
		return new HttpPartList(this);
	}

	/**
	 * Returns the parts in this list as a typed array.
	 *
	 * @return A new array. Never <jk>null</jk>.
	 */
	public HttpPart[] toPartArray() {
		return toArray(new HttpPart[0]);
	}

	private boolean nameMatches(HttpPart p, String name) {
		return caseInsensitive ? eqic(p.getName(), name) : eq(p.getName(), name);
	}
}
