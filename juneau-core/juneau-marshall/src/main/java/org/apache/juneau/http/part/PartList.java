// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http.part;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.HttpParts;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * An immutable list of HTTP parts (form-data, query-parameters, path-parameters).
 * {@review}
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	PartList <jv>parts</jv> = PartList
 * 		.<jsm>create</jsm>()
 * 		.append(MyPart.<jsm>of</jsm>("foo"))
 * 		.append(<js>"Bar"</js>, ()-><jsm>getDynamicValueFromSomewhere</jsm>())
 * 		.build();
 * </p>
 *
 * <p>
 * Convenience creators are provided for creating lists with minimal code:
 * <p class='bcode w800'>
 * 	PartList <jv>parts</jv> = PartList.<jsm>of</jsm>(BasicIntegerPart.of(<js>"foo"</js>, 1));
 * </p>
 *
 * <p>
 * Part lists are immutable, but can be appended to using the {@link #copy()} method:
 * <p class='bcode w800'>
 * 	parts = parts
 * 		.copy()
 * 		.append(BasicIntegerPart.<jsm>of</jsm>(<js>"foo"</js>, 1))
 * 		.build();
 * </p>
 *
 * <p>
 * Static methods are provided on {@link HttpParts} to further simplify creation of part lists.
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
 *
 * 	PartList <jv>parts</jv> = <jsm>partList</jsm>(<jsm>integerPart</jsm>(<js>"foo"</js>, 1), <jsm>booleanPart</jsm>(<js>"bar"<js>, <jk>false</jk>));
 * </p>
 *
 * <p>
 * The builder class supports setting default part values (i.e. add a part to the list if it isn't otherwise in the list).
 * Note that this is different from simply setting a value twice as using default values will not overwrite existing
 * parts.
 * <br>The following example notes the distinction:
 *
 * <p class='bcode w800'>
 * 	<jv>parts</jv> = PartList
 * 		.<jsm>create</jsm>()
 * 		.set(<js>"Foo"</js>, <js>"bar"</js>)
 * 		.set(<js>"Foo"</js>, <js>"baz"</js>)
 * 		.build();
 * 	<jsm>assertObject</jsm>(<jv>parts</jv>).isString(<js>"foo=baz"</js>);
 *
 * 	<jv>parts</jv> = PartList
 * 		.create()
 * 		.set(<js>"Foo"</js>, <js>"bar"</js>)
 * 		.setDefault(<js>"Foo"</js>, <js>"baz"</js>)
 * 		.build();
 * 	<jsm>assertObject</jsm>(<jv>parts</jv>).isString(<js>"foo=bar"</js>);
 * </p>
 *
 * <p>
 * Various methods are provided for iterating over the parts in this list to avoid array copies.
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #forEach(Consumer)} / {@link #forEach(String,Consumer)} - Use consumers to process parts.
 * 	<li class='jm'>{@link #iterator()} / {@link #iterator(String)} - Use an {@link PartIterator} to process parts.
 * 	<li class='jm'>{@link #stream()} / {@link #stream(String)} - Use a stream.
 * </ul>
 * <p>
 * In general, try to use these over the {@link #getAll()}/{@link #getAll(String)} methods that require array copies.
 *
 * <p>
 * The {@link #get(String)} method is special in that it will collapse multiple parts with the same name into
 * a single comma-delimited list (see <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a> for rules).
 *
 * <p>
 * The {@link #get(Class)} and {@link #get(String, Class)} methods are provided for working with {@link FormData}/{@link Query}/{@link Path}/{@link FormData}-annotated
 * beans.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	MyQueryBean <jv>foo</jv> = <jv>parts</jv>.get(MyQueryBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * By default, part names are treated as case-sensitive.  This can be changed using the {@link PartListBuilder#caseInsensitive()}
 * method.
 *
 * <p>
 * A {@link VarResolver} can be associated with this builder to create part values with embedded variables that
 * are resolved at runtime.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a part list with dynamically-resolving values pulled from a system property.</jc>
 *
 * 	System.<jsm>setProperty</jsm>(<js>"foo"</js>, <js>"bar"</js>);
 *
 * 	PartList <jv>parts</jv> = PartList
 * 		.<jsm>create</jsm>()
 * 		.resolving()
 * 		.append(<js>"X1"</js>, <js>"$S{foo}"</js>)
 * 		.append(<js>"X2"</js>, ()-><js>"$S{foo}"</js>)
 * 		.build();
 *
 * 	<jsm>assertObject</jsm>(<jv>parts</jv>).isString(<js>"X1=bar&X2=bar"</js>);
 * </p>
 *
 * <p>
 * The {@link PartList} object can be extended to defined pre-packaged lists of parts which can be used in various
 * annotations throughout the framework.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	<jc>// A predefined list of parts.</jc>
 * 	<jk>public class</jk> MyPartList <jk>extends</jk> PartList {
 * 		<jk>public</jk> MyPartList() {
 * 			<jk>super</jk>(BasicIntegerPart.<jsm>of</jsm>(<js>"foo"</js>,1), BasicBooleanPart.<jsm>of</jsm>(<js>"bar"</js>,<jk>false</jk>);
 * 		}
 * 	}
 */
@ThreadSafe
public class PartList {

	private static final NameValuePair[] EMPTY_ARRAY = new NameValuePair[0];

	/** Represents no part supplier in annotations. */
	public static final class Null extends PartList {}

	/** Predefined instance. */
	public static final PartList EMPTY = new PartList();

	final NameValuePair[] entries;
	final boolean caseInsensitive;

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static PartListBuilder create() {
		return new PartListBuilder();
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified parts.
	 *
	 * @param parts
	 * 	The parts to add to the list.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br><jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static PartList of(List<NameValuePair> parts) {
		return parts == null || parts.isEmpty() ? EMPTY : new PartList(parts);
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified parts.
	 *
	 * @param parts
	 * 	The parts to add to the list.
	 * 	<br><jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static PartList of(NameValuePair...parts) {
		return parts == null || parts.length == 0 ? EMPTY : new PartList(parts);
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified name/value pairs.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bcode w800'>
	 * 	PartList <jv>parts</jv> = PartList.<jsm>ofPairs</jsm>(<js>"foo"</js>, 1, <js>"bar"</js>, <jk>true</jk>);
	 * </p>
	 *
	 * @param pairs
	 * 	Initial list of pairs.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static PartList ofPairs(Object...pairs) {
		if (pairs == null || pairs.length == 0)
			return EMPTY;
		if (pairs.length % 2 != 0)
			throw runtimeException("Odd number of parameters passed into PartList.ofPairs()");
		ArrayBuilder<NameValuePair> b = ArrayBuilder.create(NameValuePair.class, pairs.length / 2, true);
		for (int i = 0; i < pairs.length; i+=2)
			b.add(BasicPart.of(stringify(pairs[i]), pairs[i+1]));
		return new PartList(b.toArray());
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public PartList(PartListBuilder builder) {
		if (builder.defaultEntries == null) {
			entries = builder.entries.toArray(new NameValuePair[builder.entries.size()]);
		} else {
			ArrayBuilder<NameValuePair> l = ArrayBuilder.create(NameValuePair.class, builder.entries.size() + builder.defaultEntries.size(), true);

			for (int i = 0, j = builder.entries.size(); i < j; i++)
				l.add(builder.entries.get(i));

			for (int i1 = 0, j1 = builder.defaultEntries.size(); i1 < j1; i1++) {
				NameValuePair x = builder.defaultEntries.get(i1);
				boolean exists = false;
				for (int i2 = 0, j2 = builder.entries.size(); i2 < j2 && ! exists; i2++)
					exists = eq(builder.entries.get(i2).getName(), x.getName());
				if (! exists)
					l.add(x);
			}

			entries = l.toArray();
		}
		this.caseInsensitive = builder.caseInsensitive;
	}

	/**
	 * Constructor.
	 *
	 * @param parts
	 * 	The parts to add to the list.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br><jk>null</jk> entries are ignored.
	 */
	protected PartList(List<NameValuePair> parts) {
		ArrayBuilder<NameValuePair> l = ArrayBuilder.create(NameValuePair.class, parts.size(), true);
		for (int i = 0, j = parts.size(); i < j; i++)
			l.add(parts.get(i));
		entries = l.toArray();
		caseInsensitive = false;
	}

	/**
	 * Constructor.
	 *
	 * @param parts
	 * 	The parts to add to the list.
	 * 	<br><jk>null</jk> entries are ignored.
	 */
	protected PartList(NameValuePair...parts) {
		ArrayBuilder<NameValuePair> l = ArrayBuilder.create(NameValuePair.class, parts.length, true);
		for (int i = 0; i < parts.length; i++)
			l.add(parts[i]);
		entries = l.toArray();
		caseInsensitive = false;
	}

	/**
	 * Default constructor.
	 */
	protected PartList() {
		entries = EMPTY_ARRAY;
		caseInsensitive = false;
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public PartListBuilder copy() {
		return new PartListBuilder(this);
	}

	/**
	 * Gets a part representing all of the part values with the given name.
	 *
	 * <p>
	 * If more that one part with the given name exists the values will be combined with <js>", "</js> as per
	 * <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a>.
	 *
	 * @param name The part name.
	 * @return A part with a condensed value, or {@link Optional#empty()} if no parts by the given name are present
	 */
	public Optional<NameValuePair> get(String name) {

		NameValuePair first = null;
		List<NameValuePair> rest = null;
		for (int i = 0; i < entries.length; i++) {
			NameValuePair x = entries[i];
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = new ArrayList<>();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return Optional.empty();

		if (rest == null)
			return Optional.of(first);

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(',');
			sb.append(rest.get(i).getValue());
		}

		return Optional.of(new BasicPart(name, sb.toString()));
	}

	/**
	 * Gets a part representing all of the part values with the given name.
	 *
	 * <p>
	 * If more that one part with the given name exists the values will be combined with <js>", "</js> as per
	 * <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a>.
	 *
	 * <p>
	 * The implementation class must have a public constructor taking in one of the following argument lists:
	 * <ul>
	 * 	<li><c>X(String <jv>value</jv>)</c>
	 * 	<li><c>X(Object <jv>value</jv>)</c>
	 * 	<li><c>X(String <jv>name</jv>, String <jv>value</jv>)</c>
	 * 	<li><c>X(String <jv>name</jv>, Object <jv>value</jv>)</c>
	 * </ul>
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bcode w800'>
	 * 	BasicIntegerPart <jv>age</jv> = partList.get(<js>"age"</js>, BasicIntegerPart.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The part name.
	 * @param type The part implementation class.

	 * @return A part with a condensed value or <jk>null</jk> if no parts by the given name are present
	 */
	public <T> Optional<T> get(String name, Class<T> type) {

		NameValuePair first = null;
		List<NameValuePair> rest = null;
		for (int i = 0; i < entries.length; i++) {
			NameValuePair x = entries[i];
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = new ArrayList<>();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return Optional.empty();

		if (rest == null)
			return Optional.of(PartBeanMeta.of(type).construct(name, first.getValue()));

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(',');
			sb.append(rest.get(i).getValue());
		}

		return Optional.of(PartBeanMeta.of(type).construct(name, sb.toString()));
	}

	/**
	 * Gets a part representing all of the part values with the given name.
	 *
	 * <p>
	 * Same as {@link #get(String, Class)} but the part name is pulled from the name or value attribute of the {@link FormData}/{@link Query}/{@link Path} annotation.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bcode w800'>
	 * 	Age <jv>age</jv> = partList.get(Age.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The part implementation class.
	 * @return A part with a condensed value or <jk>null</jk> if no parts by the given name are present
	 */
	public <T> Optional<T> get(Class<T> type) {
		assertArgNotNull("type", type);

		String name = PartBeanMeta.of(type).getSchema().getName();
		assertArg(name != null, "Part name could not be found on bean type ''{0}''", type.getName());

		return get(name, type);
	}

	/**
	 * Gets all of the parts with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the parts were added.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The part name.
	 *
	 * @return An array containing all matching parts, or an empty array if none are found.
	 */
	public NameValuePair[] getAll(String name) {
		List<NameValuePair> l = null;
		for (int i = 0; i < entries.length; i++) {
			NameValuePair x = entries[i];
			if (eq(x.getName(), name)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(x);
			}
		}
		return l == null ? EMPTY_ARRAY : l.toArray(new NameValuePair[l.size()]);
	}

	/**
	 * Returns the number of parts in this list.
	 *
	 * @return The number of parts in this list.
	 */
	public int size() {
		return entries.length;
	}

	/**
	 * Gets the first part with the given name.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The part name.
	 * @return The first matching part, or <jk>null</jk> if not found.
	 */
	public Optional<NameValuePair> getFirst(String name) {
		for (int i = 0; i < entries.length; i++) {
			NameValuePair x = entries[i];
			if (eq(x.getName(), name))
				return Optional.of(x);
		}
		return Optional.empty();
	}

	/**
	 * Gets the last part with the given name.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The part name.
	 * @return The last matching part, or <jk>null</jk> if not found.
	 */
	public Optional<NameValuePair> getLast(String name) {
		for (int i = entries.length - 1; i >= 0; i--) {
			NameValuePair x = entries[i];
			if (eq(x.getName(), name))
				return Optional.of(x);
		}
		return Optional.empty();
	}

	/**
	 * Gets all of the parts contained within this list.
	 *
	 * @return An array of all the parts in this list, or an empty array if no parts are present.
	 */
	public NameValuePair[] getAll() {
		return entries.length == 0 ? EMPTY_ARRAY : Arrays.copyOf(entries, entries.length);
	}

	/**
	 * Tests if parts with the given name are contained within this list.
	 *
	 * <p>
	 * Part name comparison is case insensitive.
	 *
	 * @param name The part name.
	 * @return <jk>true</jk> if at least one part with the name is present.
	 */
	public boolean contains(String name) {
		for (int i = 0; i < entries.length; i++) {
			NameValuePair x = entries[i];
			if (eq(x.getName(), name))
				return true;
		}
		return false;
	}

	/**
	 * Returns an iterator over this list of parts.
	 *
	 * @return A new iterator over this list of parts.
	 */
	public PartIterator iterator() {
		return new BasicPartIterator(entries, null, caseInsensitive);
	}

	/**
	 * Returns an iterator over the parts with a given name in this list.
	 *
	 * @param name The name of the parts over which to iterate, or <jk>null</jk> for all parts
	 *
	 * @return A new iterator over the matching parts in this list.
	 */
	public PartIterator iterator(String name) {
		return new BasicPartIterator(entries, name, caseInsensitive);
	}

	/**
	 * Performs an operation on the parts of this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over parts as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param c The consumer.
	 * @return This object (for method chaining).
	 */
	public PartList forEach(Consumer<NameValuePair> c) {
		for (int i = 0; i < entries.length; i++)
			c.accept(entries[i]);
		return this;
	}

	/**
	 * Performs an operation on the parts with the specified name in this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over parts as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param name The part name.
	 * @param c The consumer.
	 * @return This object (for method chaining).
	 */
	public PartList forEach(String name, Consumer<NameValuePair> c) {
		for (int i = 0; i < entries.length; i++)
			if (eq(name, entries[i].getName()))
				c.accept(entries[i]);
		return this;
	}

	/**
	 * Returns a stream of the parts in this list.
	 *
	 * <p>
	 * This does not involve a copy of the underlying array of <c>NameValuePair</c> objects so should perform well.
	 *
	 * @return This object (for method chaining).
	 */
	public Stream<NameValuePair> stream() {
		return Arrays.stream(entries);
	}

	/**
	 * Returns a stream of the parts in this list with the specified name.
	 *
	 * <p>
	 * This does not involve a copy of the underlying array of <c>NameValuePair</c> objects so should perform well.
	 *
	 * @param name The part name.
	 * @return This object (for method chaining).
	 */
	public Stream<NameValuePair> stream(String name) {
		return Arrays.stream(entries).filter(x->eq(name, x.getName()));
	}

	/**
	 * Returns the contents of this list as an unmodifiable list of {@link NameValuePair} objects.
	 *
	 * @return The contents of this list as an unmodifiable list of {@link NameValuePair} objects.
	 */
	public List<NameValuePair> asNameValuePairs() {
		return Collections.unmodifiableList(Arrays.asList(entries));
	}

	private boolean eq(String s1, String s2) {
		return StringUtils.eq(caseInsensitive, s1, s2);
	}

	/**
	 * Returns this list as a URL-encoded custom query.
	 */
	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < entries.length; i++) {
			NameValuePair p = entries[i];
			String v = p.getValue();
			if (v != null) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(urlEncode(p.getName())).append('=').append(urlEncode(p.getValue()));
			}
		}
		return sb.toString();
	}
}
