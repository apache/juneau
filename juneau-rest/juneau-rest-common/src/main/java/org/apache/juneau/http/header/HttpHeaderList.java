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
package org.apache.juneau.http.header;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.*;

/**
 * A mutable, ordered list of {@link HttpHeader} instances.
 *
 * <p>
 * Mirrors the semantics of {@code HeaderList} from {@code juneau-rest-common-classic} without the
 * Apache HttpCore dependency. Header-name comparisons are case-insensitive by default.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	HttpHeaderList <jv>headers</jv> = HttpHeaderList.<jsm>create</jsm>()
 * 		.append(HttpHeaderBean.<jsm>of</jsm>(<js>"Accept"</js>, <js>"text/xml"</js>))
 * 		.append(<js>"Content-Type"</js>, <js>"text/xml"</js>);
 * </p>
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
public class HttpHeaderList extends ArrayList<HttpHeader> {

	private static final long serialVersionUID = 1L;

	/** Represents no header list in annotations. */
	public static final class Void extends HttpHeaderList {
		private static final long serialVersionUID = 1L;
	}

	private boolean caseSensitive;

	/**
	 * Creates a new empty {@link HttpHeaderList}.
	 *
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpHeaderList create() {
		return new HttpHeaderList();
	}

	/**
	 * Creates a new {@link HttpHeaderList} initialized with the given headers.
	 *
	 * @param headers The headers to include. {@code null} entries are ignored.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpHeaderList of(HttpHeader...headers) {
		return new HttpHeaderList().append(headers);
	}

	/**
	 * Creates a new {@link HttpHeaderList} initialized with the given headers.
	 *
	 * @param headers The headers to include. {@code null} entries are ignored.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpHeaderList of(List<HttpHeader> headers) {
		return new HttpHeaderList().append(headers);
	}

	/**
	 * Creates a new {@link HttpHeaderList} initialized with the given alternating {@code name}/{@code value} pairs.
	 *
	 * @param pairs Alternating name/value strings. Can be <jk>null</jk> (treated as empty). Length must be even.
	 * @return A new instance. Never <jk>null</jk>.
	 * @throws IllegalArgumentException If the number of arguments is odd.
	 */
	public static HttpHeaderList ofPairs(String...pairs) {
		var x = new HttpHeaderList();
		if (pairs == null)
			pairs = new String[0];
		assertArg(pairs.length % 2 == 0, "Odd number of parameters passed into HttpHeaderList.ofPairs()");
		for (var i = 0; i < pairs.length; i += 2)
			x.add(HttpHeaderBean.of(pairs[i], pairs[i + 1]));
		return x;
	}

	/** Constructor. */
	public HttpHeaderList() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The list to copy. Must not be <jk>null</jk>.
	 */
	protected HttpHeaderList(HttpHeaderList copyFrom) {
		super(copyFrom);
		this.caseSensitive = copyFrom.caseSensitive;
	}

	/**
	 * Sets whether header-name comparisons are case-sensitive. Defaults to {@code false}.
	 *
	 * @param value New value.
	 * @return This object.
	 */
	public HttpHeaderList caseSensitive(boolean value) {
		this.caseSensitive = value;
		return this;
	}

	/**
	 * Appends the given header.
	 *
	 * @param value The header to add. {@code null} is ignored.
	 * @return This object.
	 */
	public HttpHeaderList append(HttpHeader value) {
		if (value != null)
			add(value);
		return this;
	}

	/**
	 * Appends the given headers in order.
	 *
	 * @param values The headers to add. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpHeaderList append(HttpHeader...values) {
		if (values != null)
			for (var v : values)
				append(v);
		return this;
	}

	/**
	 * Appends the given headers in order.
	 *
	 * @param values The headers to add. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpHeaderList append(List<HttpHeader> values) {
		if (values != null)
			values.forEach(this::append);
		return this;
	}

	/**
	 * Appends a header constructed from the given name and string value.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpHeaderList append(String name, String value) {
		return append(HttpHeaderBean.of(name, value));
	}

	/**
	 * Appends a header constructed from the given name and lazy value supplier.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value Supplier of the header value. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpHeaderList append(String name, Supplier<String> value) {
		return append(HttpHeaderBean.of(name, value));
	}

	/**
	 * Sets the given header as a default. If a header with the same name already exists, this is a no-op
	 * (first-in-chain wins per name).
	 *
	 * @param value The header to set. {@code null} is ignored.
	 * @return This object.
	 */
	public HttpHeaderList setDefault(HttpHeader value) {
		if (value != null && !contains(value.getName()))
			add(value);
		return this;
	}

	/**
	 * Sets the given headers as defaults, in order. For each, if a header with the same name already exists,
	 * the entry is skipped (first-in-chain wins per name).
	 *
	 * @param values The headers to set. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpHeaderList setDefault(HttpHeader...values) {
		if (values != null)
			for (var v : values)
				setDefault(v);
		return this;
	}

	/**
	 * Sets a default header with the given name and value (first-in-chain wins per name).
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpHeaderList setDefault(String name, String value) {
		return setDefault(HttpHeaderBean.of(name, value));
	}

	/**
	 * Sets the given header, replacing any prior entry with the same name.
	 *
	 * @param value The header to set. {@code null} is ignored.
	 * @return This object.
	 */
	public HttpHeaderList set(HttpHeader value) {
		if (value == null)
			return this;
		removeAll(value.getName());
		add(value);
		return this;
	}

	/**
	 * Sets the given headers, each replacing any prior entry with the same name.
	 *
	 * @param values The headers to set. {@code null} entries are ignored.
	 * @return This object.
	 */
	public HttpHeaderList set(HttpHeader...values) {
		if (values != null)
			for (var v : values)
				set(v);
		return this;
	}

	/**
	 * Sets a header with the given name and value, replacing any prior entry with the same name.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. Can be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpHeaderList set(String name, String value) {
		return set(HttpHeaderBean.of(name, value));
	}

	/**
	 * Removes all headers with the given name.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public HttpHeaderList removeAll(String name) {
		assertArgNotNull("name", name);
		removeIf(h -> nameMatches(h, name));
		return this;
	}

	/**
	 * Returns {@code true} if a header with the given name is present.
	 *
	 * @param name The header name.
	 * @return {@code true} if present.
	 */
	public boolean contains(String name) {
		return stream().anyMatch(h -> nameMatches(h, name));
	}

	/**
	 * Returns the first header with the given name, or {@code null} if absent.
	 *
	 * @param name The header name.
	 * @return The first matching header, or {@code null}.
	 */
	public HttpHeader getFirst(String name) {
		return stream().filter(h -> nameMatches(h, name)).findFirst().orElse(null);
	}

	/**
	 * Returns the last header with the given name, or {@code null} if absent.
	 *
	 * @param name The header name.
	 * @return The last matching header, or {@code null}.
	 */
	public HttpHeader getLast(String name) {
		HttpHeader last = null;
		for (var h : this)
			if (nameMatches(h, name))
				last = h;
		return last;
	}

	/**
	 * Returns all headers with the given name in insertion order.
	 *
	 * @param name The header name.
	 * @return A new list of matching headers. Never <jk>null</jk>.
	 */
	public List<HttpHeader> getAll(String name) {
		return stream().filter(h -> nameMatches(h, name)).toList();
	}

	/**
	 * Returns all headers in this list as a typed array.
	 *
	 * <p>
	 * Mirrors {@code HeaderList.getAll()} from {@code juneau-rest-common-classic}.
	 *
	 * @return A new array. Never <jk>null</jk>.
	 */
	public HttpHeader[] getAll() {
		return toHeaderArray();
	}

	/**
	 * Performs an action on each header whose name matches.
	 *
	 * @param name The header name.
	 * @param action The action.
	 * @return This object.
	 */
	public HttpHeaderList forEach(String name, Consumer<HttpHeader> action) {
		for (var h : this)
			if (nameMatches(h, name))
				action.accept(h);
		return this;
	}

	/**
	 * Returns a copy of this list.
	 *
	 * @return A new copy. Never <jk>null</jk>.
	 */
	public HttpHeaderList copy() {
		return new HttpHeaderList(this);
	}

	/**
	 * Returns an unmodifiable snapshot of this list.
	 *
	 * <p>
	 * D1 idempotency: if this list is already an {@link Unmodifiable} snapshot, it is returned as-is.
	 *
	 * @return An unmodifiable snapshot of this list, or this list if it is already unmodifiable.
	 */
	public HttpHeaderList unmodifiable() {
		return this instanceof Unmodifiable ? this : new Unmodifiable(this);
	}

	/**
	 * Returns <jk>true</jk> if this list is an unmodifiable snapshot.
	 *
	 * @return <jk>true</jk> if this list is an {@link UnmodifiableBean} snapshot.
	 */
	public boolean isUnmodifiable() {
		return this instanceof UnmodifiableBean;
	}

	/**
	 * Returns the headers in this list as a typed array.
	 *
	 * @return A new array. Never <jk>null</jk>.
	 */
	public HttpHeader[] toHeaderArray() {
		return toArray(new HttpHeader[0]);
	}

	private boolean nameMatches(HttpHeader h, String name) {
		return caseSensitive ? eq(h.getName(), name) : eqic(h.getName(), name);
	}

	/**
	 * An unmodifiable snapshot of a {@link HttpHeaderList}.
	 *
	 * <p>
	 * This is the D4 "collection-mutator-override" variant of the immutability paradigm.  Because {@link HttpHeaderList}
	 * extends {@link ArrayList}, a single funnel cannot intercept the inherited {@code ArrayList} mutators, so this
	 * subclass snapshot-copies the elements in its constructor and overrides the entire collection-mutator surface — its
	 * own fluent mutators plus the inherited {@code ArrayList} mutators (including the {@code iterator()} /
	 * {@code listIterator()} mutators) — to throw {@link UnsupportedOperationException}.  Read operations and
	 * non-mutating iteration continue to work.
	 */
	@SuppressWarnings({
		"java:S2160" // equals() inherited from ArrayList; list equality is element-based which is correct
	})
	public static class Unmodifiable extends HttpHeaderList implements UnmodifiableBean {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param copyFrom The list to snapshot-copy. Must not be <jk>null</jk>.
		 */
		Unmodifiable(HttpHeaderList copyFrom) {
			super(copyFrom);
		}

		// --- Fluent mutators -----------------------------------------------------------------------------------------

		@Override public HttpHeaderList append(HttpHeader value) { throw uoex(RO); }
		@Override public HttpHeaderList append(HttpHeader...values) { throw uoex(RO); }
		@Override public HttpHeaderList append(List<HttpHeader> values) { throw uoex(RO); }
		@Override public HttpHeaderList append(String name, String value) { throw uoex(RO); }
		@Override public HttpHeaderList append(String name, Supplier<String> value) { throw uoex(RO); }
		@Override public HttpHeaderList caseSensitive(boolean value) { throw uoex(RO); }
		@Override public HttpHeaderList removeAll(String name) { throw uoex(RO); }
		@Override public HttpHeaderList set(HttpHeader value) { throw uoex(RO); }
		@Override public HttpHeaderList set(HttpHeader...values) { throw uoex(RO); }
		@Override public HttpHeaderList set(String name, String value) { throw uoex(RO); }
		@Override public HttpHeaderList setDefault(HttpHeader value) { throw uoex(RO); }
		@Override public HttpHeaderList setDefault(HttpHeader...values) { throw uoex(RO); }
		@Override public HttpHeaderList setDefault(String name, String value) { throw uoex(RO); }

		// --- Inherited ArrayList collection mutators -----------------------------------------------------------------

		@Override public boolean add(HttpHeader e) { throw uoex(RO); }
		@Override public void add(int index, HttpHeader element) { throw uoex(RO); }
		@Override public boolean addAll(Collection<? extends HttpHeader> c) { throw uoex(RO); }
		@Override public boolean addAll(int index, Collection<? extends HttpHeader> c) { throw uoex(RO); }
		@Override public void clear() { throw uoex(RO); }
		@Override public boolean remove(Object o) { throw uoex(RO); }
		@Override public HttpHeader remove(int index) { throw uoex(RO); }
		@Override public boolean removeAll(Collection<?> c) { throw uoex(RO); }
		@Override public boolean removeIf(Predicate<? super HttpHeader> filter) { throw uoex(RO); }
		@Override public void replaceAll(UnaryOperator<HttpHeader> operator) { throw uoex(RO); }
		@Override public boolean retainAll(Collection<?> c) { throw uoex(RO); }
		@Override public HttpHeader set(int index, HttpHeader element) { throw uoex(RO); }
		@Override public void sort(Comparator<? super HttpHeader> c) { throw uoex(RO); }

		@Override public Iterator<HttpHeader> iterator() {
			var i = super.iterator();
			return new Iterator<>() {
				@Override public boolean hasNext() { return i.hasNext(); }
				@Override public HttpHeader next() { return i.next(); }
				@Override public void remove() { throw uoex(RO); }
			};
		}

		@Override public ListIterator<HttpHeader> listIterator() { return listIterator(0); }

		@Override public ListIterator<HttpHeader> listIterator(int index) {
			var i = super.listIterator(index);
			return new ListIterator<>() {
				@Override public boolean hasNext() { return i.hasNext(); }
				@Override public HttpHeader next() { return i.next(); }
				@Override public boolean hasPrevious() { return i.hasPrevious(); }
				@Override public HttpHeader previous() { return i.previous(); }
				@Override public int nextIndex() { return i.nextIndex(); }
				@Override public int previousIndex() { return i.previousIndex(); }
				@Override public void add(HttpHeader e) { throw uoex(RO); }
				@Override public void set(HttpHeader e) { throw uoex(RO); }
				@Override public void remove() { throw uoex(RO); }
			};
		}
	}

	private static final String RO = "Bean is unmodifiable.";
}
