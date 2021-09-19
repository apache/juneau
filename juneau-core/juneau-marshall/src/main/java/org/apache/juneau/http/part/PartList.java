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
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
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
 * By default, part names are treated as case-sensitive.  This can be changed using the {@link Builder#caseInsensitive()}
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

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final NameValuePair[] EMPTY_ARRAY = new NameValuePair[0];

	/** Represents no part supplier in annotations. */
	public static final class Null extends PartList {}

	/** Predefined instance. */
	public static final PartList EMPTY = new PartList();

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
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

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends BeanBuilder<PartList> {

		final List<NameValuePair> entries;
		List<NameValuePair> defaultEntries;
		private VarResolver varResolver;
		boolean caseInsensitive = false;

		/**
		 * Constructor.
		 */
		public Builder() {
			super(PartList.class);
			entries = new ArrayList<>();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		public Builder(PartList copyFrom) {
			super(copyFrom.getClass());
			entries = new ArrayList<>(copyFrom.entries.length);
			for (int i = 0; i < copyFrom.entries.length; i++)
				entries.add(copyFrom.entries[i]);
			caseInsensitive = copyFrom.caseInsensitive;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			entries = new ArrayList<>(copyFrom.entries);
			defaultEntries = copyFrom.defaultEntries == null ? null : new ArrayList<>(copyFrom.defaultEntries);
			varResolver = copyFrom.varResolver;
			caseInsensitive = copyFrom.caseInsensitive;
		}

		@Override /* BeanBuilder */
		protected PartList buildDefault() {
			return entries.isEmpty() && defaultEntries == null ? EMPTY : new PartList(this);
		}

		/**
		 * Allows part values to contain SVL variables.
		 *
		 * <p>
		 * Resolves variables in part values when using the following methods:
		 * <ul>
		 * 	<li class='jm'>{@link #append(String, Object) append(String,Object)}
		 * 	<li class='jm'>{@link #append(String, Supplier) append(String,Supplier&lt;?&gt;)}
		 * 	<li class='jm'>{@link #prepend(String, Object) prepend(String,Object)}
		 * 	<li class='jm'>{@link #prepend(String, Supplier) prepend(String,Supplier&lt;?&gt;)}
		 * 	<li class='jm'>{@link #set(String, Object) set(String,Object)}
		 * 	<li class='jm'>{@link #set(String, Supplier) set(String,Supplier&lt;?&gt;)}
		 * 	<li class='jm'>{@link #setDefault(String, Object) set(String,Object)}
		 * 	<li class='jm'>{@link #setDefault(String, Supplier) set(String,Supplier&lt;?&gt;)}
		 * </ul>
		 *
		 * <p>
		 * Uses {@link VarResolver#DEFAULT} to resolve variables.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder resolving() {
			return resolving(VarResolver.DEFAULT);
		}

		/**
		 * Allows part values to contain SVL variables.
		 *
		 * <p>
		 * Resolves variables in part values when using the following methods:
		 * <ul>
		 * 	<li class='jm'>{@link #append(String, Object) append(String,Object)}
		 * 	<li class='jm'>{@link #append(String, Supplier) append(String,Supplier&lt;?&gt;)}
		 * 	<li class='jm'>{@link #prepend(String, Object) prepend(String,Object)}
		 * 	<li class='jm'>{@link #prepend(String, Supplier) prepend(String,Supplier&lt;?&gt;)}
		 * 	<li class='jm'>{@link #set(String, Object) set(String,Object)}
		 * 	<li class='jm'>{@link #set(String, Supplier) set(String,Supplier&lt;?&gt;)}
		 * 	<li class='jm'>{@link #setDefault(String, Object) set(String,Object)}
		 * 	<li class='jm'>{@link #setDefault(String, Supplier) set(String,Supplier&lt;?&gt;)}
		 * </ul>
		 *
		 * @param varResolver The variable resolver to use for resolving variables.
		 * @return This object (for method chaining).
		 */
		public Builder resolving(VarResolver varResolver) {
			this.varResolver = varResolver;
			return this;
		}

		/**
		 * Specifies that the part in this builder should be treated as case-insensitive.
		 *
		 * <p>
		 * The default behavior is case-sensitive.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder caseInsensitive() {
			caseInsensitive = true;
			return this;
		}

		/**
		 * Removes any parts already in this builder.
		 *
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder clear() {
			entries.clear();
			return this;
		}

		/**
		 * Adds the specified parts to the end of the parts in this builder.
		 *
		 * @param value The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder append(PartList value) {
			if (value != null)
				for (NameValuePair x : value.entries)
					append(x);
			return this;
		}

		/**
		 * Adds the specified part to the end of the parts in this builder.
		 *
		 * @param value The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder append(NameValuePair value) {
			if (value != null)
				entries.add(value);
			return this;
		}

		/**
		 * Appends the specified parts to the end of this builder.
		 *
		 * <p>
		 * The parts is added as a {@link BasicPart}.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder append(String name, Object value) {
			return append(createPart(name, value));
		}

		/**
		 * Appends the specified part to the end of this builder using a value supplier.
		 *
		 * <p>
		 * The part is added as a {@link BasicPart}.
		 *
		 * <p>
		 * Value is re-evaluated on each call to {@link BasicPart#getValue()}.
		 *
		 * @param name The part name.
		 * @param value The part value supplier.
		 * @return This object (for method chaining).
		 */
		public Builder append(String name, Supplier<?> value) {
			return append(createPart(name, value));
		}

		/**
		 * Adds the specified parts to the end of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder append(NameValuePair...values) {
			if (values != null)
				for (int i = 0; i < values.length; i++)
					append(values[i]);
			return this;
		}

		/**
		 * Adds the specified parts to the end of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder append(List<? extends NameValuePair> values) {
			if (values != null)
				for (int i = 0, j = values.size(); i < j; i++)
					append(values.get(i));
			return this;
		}

		/**
		 * Adds the specified part to the beginning of the parts in this builder.
		 *
		 * @param value The part to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder prepend(PartList value) {
			if (value != null)
				entries.addAll(0, Arrays.asList(value.entries));
			return this;
		}

		/**
		 * Adds the specified part to the beginning of the parts in this builder.
		 *
		 * @param value The part to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder prepend(NameValuePair value) {
			if (value != null)
				entries.add(0, value);
			return this;
		}

		/**
		 * Appends the specified part to the beginning of this builder.
		 *
		 * <p>
		 * The part is added as a {@link BasicPart}.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder prepend(String name, Object value) {
			return prepend(createPart(name, value));
		}

		/**
		 * Appends the specified part to the beginning of this builder using a value supplier.
		 *
		 * <p>
		 * The part is added as a {@link BasicPart}.
		 *
		 * <p>
		 * Value is re-evaluated on each call to {@link BasicPart#getValue()}.
		 *
		 * @param name The part name.
		 * @param value The part value supplier.
		 * @return This object (for method chaining).
		 */
		public Builder prepend(String name, Supplier<?> value) {
			return prepend(createPart(name, value));
		}

		/**
		 * Adds the specified parts to the beginning of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder prepend(NameValuePair...values) {
			if (values != null)
				prepend(Arrays.asList(values));
			return this;
		}

		/**
		 * Adds the specified parts to the beginning of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder prepend(List<? extends NameValuePair> values) {
			if (values != null)
				entries.addAll(0, values);
			return this;
		}

		/**
		 * Removes the specified part from this builder.
		 *
		 * @param value The part to remove.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder remove(PartList value) {
			if (value != null)
				for (int i = 0; i < value.entries.length; i++)
					remove(value.entries[i]);
			return this;
		}

		/**
		 * Removes the specified part from this builder.
		 *
		 * @param value The part to remove.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder remove(NameValuePair value) {
			if (value != null)
				entries.remove(value);
			return this;
		}

		/**
		 * Removes the specified parts from this builder.
		 *
		 * @param values The parts to remove.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder remove(NameValuePair...values) {
			for (int i = 0; i < values.length; i++)
				remove(values[i]);
			return this;
		}

		/**
		 * Removes the specified parts from this builder.
		 *
		 * @param values The parts to remove.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder remove(List<? extends NameValuePair> values) {
			for (int i = 0, j = values.size(); i < j; i++) /* See HTTPCORE-361 */
				remove(values.get(i));
			return this;
		}

		/**
		 * Removes the part with the specified name from this builder.
		 *
		 * @param name The part name.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder remove(String name) {
			for (int i = 0; i < entries.size(); i++) /* See HTTPCORE-361 */
				if (eq(entries.get(i).getName(), name))
					entries.remove(i--);
			return this;
		}

		/**
		 * Removes the part with the specified name from this builder.
		 *
		 * @param names The part name.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder remove(String...names) {
			if (names != null)
				for (int i = 0; i < names.length; i++)
					remove(names[i]);
			return this;
		}

		/**
		 * Adds or replaces the part(s) with the same name.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param value The parts to replace.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder set(NameValuePair value) {
			if (value != null) {
				boolean replaced = false;
				for (int i = 0, j = entries.size(); i < j; i++) {
					NameValuePair x = entries.get(i);
					if (eq(x.getName(), value.getName())) {
						if (replaced) {
							entries.remove(i);
							j--;
						} else {
							entries.set(i, value);
							replaced = true;
						}
					}
				}

				if (! replaced)
					entries.add(value);
			}

			return this;
		}

		/**
		 * Adds or replaces the part(s) with the same name.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param values The parts to replace.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder set(NameValuePair...values) {
			if (values != null)
				set(Arrays.asList(values));
			return this;
		}

		/**
		 * Adds or replaces the part with the specified name.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder set(String name, Object value) {
			return set(createPart(name, value));
		}

		/**
		 * Adds or replaces the part with the specified name.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder set(String name, Supplier<?> value) {
			return set(createPart(name, value));
		}

		/**
		 * Adds or replaces the parts with the specified names.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param values The parts to replace.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder set(List<? extends NameValuePair> values) {

			if (values != null) {
				for (int i1 = 0, j1 = values.size(); i1 < j1; i1++) {
					NameValuePair p = values.get(i1);
					if (p != null) {
						for (int i2 = 0, j2 = entries.size(); i2 < j2; i2++) {
							NameValuePair x = entries.get(i2);
							if (eq(x.getName(), p.getName())) {
								entries.remove(i2);
								j2--;
							}
						}
					}
				}

				for (int i = 0, j = values.size(); i < j; i++) {
					NameValuePair x = values.get(i);
					if (x != null) {
						entries.add(x);
					}
				}
			}

			return this;
		}

		/**
		 * Adds or replaces the parts with the specified names.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param values The parts to replace.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		public Builder set(PartList values) {
			if (values != null)
				set(values.entries);
			return this;
		}

		/**
		 * Sets a default value for a part.
		 *
		 * <p>
		 * If no part with the same name is found, the given part is added to the end of the list.
		 *
		 * @param value The default part to set.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder setDefault(NameValuePair value) {
			if (value != null) {
				boolean replaced = false;
				if (defaultEntries == null)
					defaultEntries = new ArrayList<>();
				for (int i = 0, j = defaultEntries.size(); i < j; i++) {
					NameValuePair x = defaultEntries.get(i);
					if (eq(x.getName(), value.getName())) {
						if (replaced) {
							defaultEntries.remove(i);
							j--;
						} else {
							defaultEntries.set(i, value);
							replaced = true;
						}
					}
				}

				if (! replaced)
					defaultEntries.add(value);
			}

			return this;
		}

		/**
		 * Sets default values for one or more parts.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param values The default parts to set.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder setDefault(NameValuePair...values) {
			if (values != null)
				setDefault(Arrays.asList(values));
			return this;
		}

		/**
		 * Sets a default value for a part.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder setDefault(String name, Object value) {
			return setDefault(createPart(name, value));
		}

		/**
		 * Sets a default value for a part.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder setDefault(String name, Supplier<?> value) {
			return setDefault(createPart(name, value));
		}

		/**
		 * Sets default values for one or more parts.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param values The default parts to set.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder setDefault(List<? extends NameValuePair> values) {

			if (values != null) {
				if (defaultEntries == null)
					defaultEntries = new ArrayList<>();
				for (int i1 = 0, j1 = values.size(); i1 < j1; i1++) {
					NameValuePair p = values.get(i1);
					if (p != null) {
						for (int i2 = 0, j2 = defaultEntries.size(); i2 < j2; i2++) {
							NameValuePair x = defaultEntries.get(i2);
							if (eq(x.getName(), p.getName())) {
								defaultEntries.remove(i2);
								j2--;
							}
						}
					}
				}

				for (NameValuePair v : values) {
					if (v != null) {
						defaultEntries.add(v);
					}
				}
			}

			return this;
		}

		/**
		 * Replaces the first occurrence of the parts with the same name.
		 *
		 * <p>
		 * If no part with the same name is found the given part is added to the end of the list.
		 *
		 * @param values The default parts to set.  <jk>null</jk> values are ignored.
		 * @return This object (for method chaining).
		 */
		public Builder setDefault(PartList values) {
			if (values != null)
				setDefault(values.entries);
			return this;
		}

		/**
		 * Adds the specify part to this list.
		 *
		 * @param flag
		 * 	What to do with the part.
		 * 	<br>Possible values:
		 * 	<ul>
		 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(NameValuePair)}.
		 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(NameValuePair)}.
		 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(NameValuePair)}.
		 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(NameValuePair)}.
		 * 	</ul>
		 * @param value The part to add.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder add(ListOperation flag, NameValuePair value) {
			if (flag == ListOperation.APPEND)
				return append(value);
			if (flag == ListOperation.PREPEND)
				return prepend(value);
			if (flag == ListOperation.SET)
				return set(value);
			if (flag == ListOperation.DEFAULT)
				return setDefault(value);
			throw runtimeException("Invalid value specified for flag parameter on add(flag,value) method: {0}", flag);
		}

		/**
		 * Adds the specified parts to this list.
		 *
		 * @param flag
		 * 	What to do with the part.
		 * 	<br>Possible values:
		 * 	<ul>
		 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(NameValuePair[])}.
		 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(NameValuePair[])}.
		 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(NameValuePair[])}.
		 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(NameValuePair[])}.
		 * 	</ul>
		 * @param values The parts to add.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder add(ListOperation flag, NameValuePair...values) {
			if (flag == ListOperation.APPEND)
				return append(values);
			if (flag == ListOperation.PREPEND)
				return prepend(values);
			if (flag == ListOperation.SET)
				return set(values);
			if (flag == ListOperation.DEFAULT)
				return setDefault(values);
			throw runtimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
		}

		/**
		 * Adds the specified part to this list.
		 *
		 * @param flag
		 * 	What to do with the part.
		 * 	<br>Possible values:
		 * 	<ul>
		 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(String,Object)}.
		 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(String,Object)}.
		 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(String,Object)}.
		 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(String,Object)}.
		 * 	</ul>
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object (for method chaining).
		 */
		public Builder add(ListOperation flag, String name, Object value) {
			if (flag == ListOperation.APPEND)
				return append(name, value);
			if (flag == ListOperation.PREPEND)
				return prepend(name, value);
			if (flag == ListOperation.SET)
				return set(name, value);
			if (flag == ListOperation.DEFAULT)
				return setDefault(name, value);
			throw runtimeException("Invalid value specified for flag parameter on add(flag,name,value) method: {0}", flag);
		}

		/**
		 * Adds the specified part to this list.
		 *
		 * @param flag
		 * 	What to do with the part.
		 * 	<br>Possible values:
		 * 	<ul>
		 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(String,Supplier)}.
		 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(String,Supplier)}.
		 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(String,Supplier)}.
		 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(String,Supplier)}.
		 * 	</ul>
		 * @param name The part name.
		 * @param value The part value supplier.
		 * @return This object (for method chaining).
		 */
		public Builder add(ListOperation flag, String name, Supplier<?> value) {
			if (flag == ListOperation.APPEND)
				return append(name, value);
			if (flag == ListOperation.PREPEND)
				return prepend(name, value);
			if (flag == ListOperation.SET)
				return set(name, value);
			if (flag == ListOperation.DEFAULT)
				return setDefault(name, value);
			throw runtimeException("Invalid value specified for flag parameter on add(flag,name,value) method: {0}", flag);
		}

		/**
		 * Adds the specified parts to this list.
		 *
		 * @param flag
		 * 	What to do with the part.
		 * 	<br>Possible values:
		 * 	<ul>
		 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(String,Supplier)}.
		 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(String,Supplier)}.
		 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(String,Supplier)}.
		 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(String,Supplier)}.
		 * 	</ul>
		 * @param values The parts to add.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder add(ListOperation flag, List<NameValuePair> values) {
			if (flag == ListOperation.APPEND)
				return append(values);
			if (flag == ListOperation.PREPEND)
				return prepend(values);
			if (flag == ListOperation.SET)
				return set(values);
			if (flag == ListOperation.DEFAULT)
				return setDefault(values);
			throw runtimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
		}

		/**
		 * Adds the specified parts to this list.
		 *
		 * @param flag
		 * 	What to do with the part.
		 * 	<br>Possible values:
		 * 	<ul>
		 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(PartList)}.
		 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(PartList)}.
		 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(PartList)}.
		 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(PartList)}.
		 * 	</ul>
		 * @param values The parts to add.
		 * @return This object (for method chaining).
		 */
		public Builder add(ListOperation flag, PartList values) {
			if (flag == ListOperation.APPEND)
				return append(values);
			if (flag == ListOperation.PREPEND)
				return prepend(values);
			if (flag == ListOperation.SET)
				return set(values);
			if (flag == ListOperation.DEFAULT)
				return setDefault(values);
			throw runtimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
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
		public Builder forEach(Consumer<NameValuePair> c) {
			for (int i = 0, j = entries.size(); i < j; i++)
				c.accept(entries.get(i));
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
		public Builder forEach(String name, Consumer<NameValuePair> c) {
			for (int i = 0, j = entries.size(); i < j; i++) {
				NameValuePair x = entries.get(i);
				if (eq(name, x.getName()))
					c.accept(x);
			}
			return this;
		}

		private boolean isResolving() {
			return varResolver != null;
		}

		private Supplier<Object> resolver(Object input) {
			return ()->varResolver.resolve(stringify(unwrap(input)));
		}

		private Object unwrap(Object o) {
			while (o instanceof Supplier)
				o = ((Supplier<?>)o).get();
			return o;
		}

		/**
		 * Creates a new part out of the specified name/value pair.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return A new header.
		 */
		public NameValuePair createPart(String name, Object value) {
			if (value instanceof Supplier<?>) {
				Supplier<?> value2 = (Supplier<?>)value;
				return isResolving() ? new BasicPart(name, resolver(value2)) : new BasicPart(name, value2);
			}
			return isResolving() ? new BasicPart(name, resolver(value)) : new BasicPart(name, value);
		}

		private boolean eq(String s1, String s2) {
			return caseInsensitive ? StringUtils.eq(s1, s2) : StringUtils.eqic(s1, s2);
		}

		/**
		 * Gets the first part with the given name.
		 *
		 * @param name The part name.
		 * @return The first matching part, or {@link Optional#empty()} if not found.
		 */
		public Optional<NameValuePair> getFirst(String name) {
			for (int i = 0; i < entries.size(); i++) {
				NameValuePair x = entries.get(i);
				if (eq(x.getName(), name))
					return Optional.of(x);
			}
			if (defaultEntries != null) {
				for (int i = 0; i < defaultEntries.size(); i ++) {
					NameValuePair x = defaultEntries.get(i);
					if (eq(x.getName(), name))
						return Optional.of(x);
				}
			}
			return Optional.empty();
		}

		/**
		 * Gets the last part with the given name.
		 *
		 * @param name The part name.
		 * @return The last matching part, or {@link Optional#empty()} if not found.
		 */
		public Optional<NameValuePair> getLast(String name) {
			for (int i = entries.size() - 1; i >= 0; i--) {
				NameValuePair x = entries.get(i);
				if (eq(x.getName(), name))
					return Optional.of(x);
			}
			if (defaultEntries != null) {
				for (int i = defaultEntries.size() - 1; i >= 0; i--) {
					NameValuePair x = defaultEntries.get(i);
					if (eq(x.getName(), name))
						return Optional.of(x);
				}
			}
			return Optional.empty();
		}

		// <FluentSetters>

		@Override /* Object */
		public String toString() {
			return "[" + join(entries, ", ") + "]";
		}
		@Override /* BeanBuilder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* BeanBuilder */
		public Builder type(Class<? extends PartList> value) {
			super.type(value);
			return this;
		}

		@Override /* BeanBuilder */
		public Builder impl(PartList value) {
			super.impl(value);
			return this;
		}

		@Override /* BeanBuilder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* BeanBuilder */
		public Builder beanStore(BeanStore value) {
			super.beanStore(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final NameValuePair[] entries;
	final boolean caseInsensitive;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public PartList(Builder builder) {
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
	public Builder copy() {
		return new Builder(this);
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
