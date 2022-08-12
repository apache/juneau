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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ArgUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.HttpParts;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * An immutable list of HTTP parts (form-data, query-parameters, path-parameters).
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	PartList <jv>parts</jv> = PartList
 * 		.<jsm>create</jsm>()
 * 		.append(MyPart.<jsm>of</jsm>(<js>"foo"</js>))
 * 		.append(<js>"Bar"</js>, ()-&gt;<jsm>getDynamicValueFromSomewhere</jsm>())
 * 		.build();
 * </p>
 *
 * <p>
 * Convenience creators are provided for creating lists with minimal code:
 * <p class='bjava'>
 * 	PartList <jv>parts</jv> = PartList.<jsm>of</jsm>(BasicIntegerPart.<jsm>of</jsm>(<js>"foo"</js>, 1));
 * </p>
 *
 * <p>
 * Part lists are immutable, but can be appended to using the {@link #copy()} method:
 * <p class='bjava'>
 * 	<jv>parts</jv> = <jv>parts</jv>
 * 		.copy()
 * 		.append(BasicIntegerPart.<jsm>of</jsm>(<js>"foo"</js>, 1))
 * 		.build();
 * </p>
 *
 * <p>
 * Static methods are provided on {@link HttpParts} to further simplify creation of part lists.
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.http.HttpParts.*;
 *
 * 	PartList <jv>parts</jv> = <jsm>partList</jsm>(<jsm>integerPart</jsm>(<js>"foo"</js>, 1), <jsm>booleanPart</jsm>(<js>"bar"</js>, <jk>false</jk>));
 * </p>
 *
 * <p>
 * The builder class supports setting default part values (i.e. add a part to the list if it isn't otherwise in the list).
 * Note that this is different from simply setting a value twice as using default values will not overwrite existing
 * parts.
 * <br>The following example notes the distinction:
 *
 * <p class='bjava'>
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
 * 	<li class='jm'>{@link #forEach(Consumer)} / {@link #forEach(String,Consumer)} / {@link #forEach(Predicate,Consumer)} - Use consumers to process parts.
 * 	<li class='jm'>{@link #partIterator()} / {@link #partIterator(String)} - Use an {@link PartIterator} to process parts.
 * 	<li class='jm'>{@link #stream()} / {@link #stream(String)} - Use a stream.
 * </ul>
 * <p>
 * In general, try to use these over the {@link #getAll()}/{@link #getAll(String)} methods that require array copies.
 *
 * <p>
 * Similar to the way multiple headers can be collapsed into a single value, the {@link #get(String)} method is special in that it will collapse multiple
 * parts with the same name into a single comma-delimited list.
 *
 * <p>
 * The {@link #get(Class)} and {@link #get(String, Class)} methods are provided for working with {@link FormData}/{@link Query}/{@link Path}-annotated
 * beans.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	MyQueryBean <jv>foo</jv> = <jv>parts</jv>.get(MyQueryBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * By default, part names are treated as case-sensitive.  This can be changed using the {@link PartList.Builder#caseInsensitive()}
 * method.
 *
 * <p>
 * A {@link VarResolver} can be associated with this builder to create part values with embedded variables that
 * are resolved at runtime.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	<jc>// Create a part list with dynamically-resolving values pulled from a system property.</jc>
 *
 * 	System.<jsm>setProperty</jsm>(<js>"foo"</js>, <js>"bar"</js>);
 *
 * 	PartList <jv>parts</jv> = PartList
 * 		.<jsm>create</jsm>()
 * 		.resolving()
 * 		.append(<js>"X1"</js>, <js>"$S{foo}"</js>)
 * 		.append(<js>"X2"</js>, ()-&gt;<js>"$S{foo}"</js>)
 * 		.build();
 *
 * 	<jsm>assertObject</jsm>(<jv>parts</jv>).isString(<js>"X1=bar&amp;X2=bar"</js>);
 * </p>
 *
 * <p>
 * The {@link PartList} object can be extended to defined pre-packaged lists of parts which can be used in various
 * annotations throughout the framework.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	<jc>// A predefined list of parts.</jc>
 * 	<jk>public class</jk> MyPartList <jk>extends</jk> PartList {
 * 		<jk>public</jk> MyPartList() {
 * 			<jk>super</jk>(BasicIntegerPart.<jsm>of</jsm>(<js>"foo"</js>,1), BasicBooleanPart.<jsm>of</jsm>(<js>"bar"</js>,<jk>false</jk>));
 * 		}
 * 	}
 * </p>
 *
 * <ul class='notes'>
 * 	<li class='note'>This class is thread safe.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class PartList extends ControlledArrayList<NameValuePair> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final NameValuePair[] EMPTY_ARRAY = new NameValuePair[0];
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Predicate<NameValuePair> NOT_NULL = x -> x != null;

	/** Represents no part supplier in annotations. */
	public static final class Void extends PartList {
		Void() {
			super(false);
		}
		private static final long serialVersionUID = 1L;
	}

	/** Predefined instance. */
	public static final PartList EMPTY = new PartList(false);

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
		return parts == null || parts.isEmpty() ? EMPTY : new PartList(true, parts);
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
		return parts == null || parts.length == 0 ? EMPTY : new PartList(true, parts);
	}

	/**
	 * Creates a new {@link PartList} initialized with the specified name/value pairs.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bjava'>
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
			throw new RuntimeException("Odd number of parameters passed into PartList.ofPairs()");
		ArrayBuilder<NameValuePair> b = ArrayBuilder.of(NameValuePair.class).filter(NOT_NULL).size(pairs.length / 2);
		for (int i = 0; i < pairs.length; i+=2)
			b.add(BasicPart.of(stringify(pairs[i]), pairs[i+1]));
		return new PartList(true, b.orElse(EMPTY_ARRAY));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<PartList> {

		final List<NameValuePair> entries;
		List<NameValuePair> defaultEntries;
		private VarResolver varResolver;
		boolean caseInsensitive = false, unmodifiable = false;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(PartList.class);
			entries = list();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy.
		 */
		protected Builder(PartList copyFrom) {
			super(copyFrom.getClass());
			entries = copyOf(copyFrom);
			caseInsensitive = copyFrom.caseInsensitive;
			unmodifiable = false;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			entries = copyOf(copyFrom.entries);
			defaultEntries = copyOf(copyFrom.defaultEntries);
			varResolver = copyFrom.varResolver;
			caseInsensitive = copyFrom.caseInsensitive;
			unmodifiable = copyFrom.unmodifiable;
		}

		@Override /* BeanBuilder */
		protected PartList buildDefault() {
			return entries.isEmpty() && defaultEntries == null ? EMPTY : new PartList(this);
		}

		/**
		 * Makes a copy of this builder.
		 *
		 * @return A new copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

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
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
		 */
		public Builder caseInsensitive() {
			caseInsensitive = true;
			return this;
		}

		/**
		 * Specifies that the resulting list should be unmodifiable.
		 *
		 * <p>
		 * The default behavior is modifiable.
		 *
		 * @return This object.
		 */
		public Builder unmodifiable() {
			unmodifiable = true;
			return this;
		}

		/**
		 * Removes any parts already in this builder.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder clear() {
			entries.clear();
			return this;
		}

		/**
		 * Adds the specified part to the end of the parts in this builder.
		 *
		 * @param value The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
		 */
		public Builder append(String name, Supplier<?> value) {
			return append(createPart(name, value));
		}

		/**
		 * Adds the specified parts to the end of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object.
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder append(List<? extends NameValuePair> values) {
			if (values != null)
				values.forEach(x -> append(x));
			return this;
		}

		/**
		 * Adds the specified part to the beginning of the parts in this builder.
		 *
		 * @param value The part to add.  <jk>null</jk> values are ignored.
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
		 */
		public Builder prepend(String name, Supplier<?> value) {
			return prepend(createPart(name, value));
		}

		/**
		 * Adds the specified parts to the beginning of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder prepend(NameValuePair...values) {
			if (values != null)
				prepend(alist(values));
			return this;
		}

		/**
		 * Adds the specified parts to the beginning of the parts in this builder.
		 *
		 * @param values The parts to add.  <jk>null</jk> values are ignored.
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(List<? extends NameValuePair> values) {
			if (values != null)
				values.forEach(x -> remove(x));
			return this;
		}

		/**
		 * Removes the part with the specified name from this builder.
		 *
		 * @param name The part name.
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder set(NameValuePair...values) {
			if (values != null)
				set(alist(values));
			return this;
		}

		/**
		 * Adds or replaces the part with the specified name.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object.
		 */
		public Builder set(String name, Object value) {
			return set(createPart(name, value));
		}

		/**
		 * Adds or replaces the part with the specified name.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object.
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
		 * @return This object.
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
		 * Sets a default value for a part.
		 *
		 * <p>
		 * If no part with the same name is found, the given part is added to the end of the list.
		 *
		 * @param value The default part to set.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder setDefault(NameValuePair value) {
			if (value != null) {
				boolean replaced = false;
				if (defaultEntries == null)
					defaultEntries = list();
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder setDefault(NameValuePair...values) {
			if (values != null)
				setDefault(alist(values));
			return this;
		}

		/**
		 * Sets a default value for a part.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object.
		 */
		public Builder setDefault(String name, Object value) {
			return setDefault(createPart(name, value));
		}

		/**
		 * Sets a default value for a part.
		 *
		 * @param name The part name.
		 * @param value The part value.
		 * @return This object.
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder setDefault(List<? extends NameValuePair> values) {

			if (values != null) {
				if (defaultEntries == null)
					defaultEntries = list();
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

				values.forEach(x -> {
					if (x != null)
						defaultEntries.add(x);
				});
			}

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
		 * @return This object.
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
			throw new BasicRuntimeException("Invalid value specified for flag parameter on add(flag,value) method: {0}", flag);
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
		 * @return This object.
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
			throw new BasicRuntimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
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
		 * @return This object.
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
			throw new BasicRuntimeException("Invalid value specified for flag parameter on add(flag,name,value) method: {0}", flag);
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
		 * @return This object.
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
			throw new BasicRuntimeException("Invalid value specified for flag parameter on add(flag,name,value) method: {0}", flag);
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
		 * @return This object.
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
			throw new BasicRuntimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
		}

		/**
		 * Performs an action on all the parts in this list.
		 *
		 * <p>
		 * This is the preferred method for iterating over parts as it does not involve
		 * creation or copy of lists/arrays.
		 *
		 * @param action An action to perform on each element.
		 * @return This object.
		 */
		public Builder forEach(Consumer<NameValuePair> action) {
			for (int i = 0, j = entries.size(); i < j; i++)
				action.accept(entries.get(i));
			return this;
		}

		/**
		 * Performs an action on the parts with the specified name in this list.
		 *
		 * <p>
		 * This is the preferred method for iterating over parts as it does not involve
		 * creation or copy of lists/arrays.
		 *
		 * @param name The part name.
		 * @param action An action to perform on each element.
		 * @return This object.
		 */
		public Builder forEach(String name, Consumer<NameValuePair> action) {
			for (int i = 0, j = entries.size(); i < j; i++) {
				NameValuePair x = entries.get(i);
				if (eq(name, x.getName()))
					action.accept(x);
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
					return optional(x);
			}
			if (defaultEntries != null) {
				for (int i = 0; i < defaultEntries.size(); i ++) {
					NameValuePair x = defaultEntries.get(i);
					if (eq(x.getName(), name))
						return optional(x);
				}
			}
			return empty();
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
					return optional(x);
			}
			if (defaultEntries != null) {
				for (int i = defaultEntries.size() - 1; i >= 0; i--) {
					NameValuePair x = defaultEntries.get(i);
					if (eq(x.getName(), name))
						return optional(x);
				}
			}
			return empty();
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		//-------------------------------------------------------------------------------------------------------------
		// Other methods
		//-------------------------------------------------------------------------------------------------------------

		@Override /* Object */
		public String toString() {
			return "[" + join(entries, ", ") + "]";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final boolean caseInsensitive;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public PartList(Builder builder) {
		super(! builder.unmodifiable, builder.entries);

		if (builder.defaultEntries != null) {
			for (int i1 = 0, j1 = builder.defaultEntries.size(); i1 < j1; i1++) {
				NameValuePair x = builder.defaultEntries.get(i1);
				boolean exists = false;
				for (int i2 = 0, j2 = builder.entries.size(); i2 < j2 && ! exists; i2++)
					exists = eq(builder.entries.get(i2).getName(), x.getName());
				if (! exists)
					overrideAdd(x);
			}
		}
		this.caseInsensitive = builder.caseInsensitive;
	}

	/**
	 * Constructor.
	 *
	 * @param modifiable Whether this list should be modifiable.
	 * @param parts
	 * 	The parts to add to the list.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br><jk>null</jk> entries are ignored.
	 */
	protected PartList(boolean modifiable, List<NameValuePair> parts) {
		super(modifiable, parts);
		caseInsensitive = false;
	}

	/**
	 * Constructor.
	 *
	 * @param modifiable Whether this list should be modifiable.
	 * @param parts
	 * 	The parts to add to the list.
	 * 	<br><jk>null</jk> entries are ignored.
	 */
	protected PartList(boolean modifiable, NameValuePair...parts) {
		super(modifiable, Arrays.asList(parts));
		caseInsensitive = false;
	}

	/**
	 * Default constructor.
	 *
	 * @param modifiable Whether this list should be modifiable.
	 */
	protected PartList(boolean modifiable) {
		super(modifiable);
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
		for (NameValuePair x : this) {
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = list();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return empty();

		if (rest == null)
			return optional(first);

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(',');
			sb.append(rest.get(i).getValue());
		}

		return optional(new BasicPart(name, sb.toString()));
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
	 * <p class='bjava'>
	 * 	BasicIntegerPart <jv>age</jv> = <jv>partList</jv>.get(<js>"age"</js>, BasicIntegerPart.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The part implementation class.
	 * @param name The part name.
	 * @param type The part implementation class.
	 * @return A part with a condensed value or <jk>null</jk> if no parts by the given name are present
	 */
	public <T> Optional<T> get(String name, Class<T> type) {

		NameValuePair first = null;
		List<NameValuePair> rest = null;
		for (NameValuePair x : this) {
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = list();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return empty();

		if (rest == null)
			return optional(PartBeanMeta.of(type).construct(name, first.getValue()));

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(',');
			sb.append(rest.get(i).getValue());
		}

		return optional(PartBeanMeta.of(type).construct(name, sb.toString()));
	}

	/**
	 * Gets a part representing all of the part values with the given name.
	 *
	 * <p>
	 * Same as {@link #get(String, Class)} but the part name is pulled from the name or value attribute of the {@link FormData}/{@link Query}/{@link Path} annotation.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bjava'>
	 * 	Age <jv>age</jv> = <jv>partList</jv>.get(Age.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The part implementation class.
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
	 * Gets all of the parts contained within this list.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the parts were added.
	 * Each call creates a new array not backed by this list.
	 *
	 * @return
	 * 	An array of all the parts in this list, or an empty array if no parts are present.
	 */
	public NameValuePair[] getAll() {
		return size() == 0 ? EMPTY_ARRAY : toArray(new NameValuePair[size()]);
	}

	/**
	 * Gets all of the parts with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the parts were added.
	 * Parts with null values are ignored.
	 * Each call creates a new array not backed by this list.
	 *
	 * @param name The part name.
	 *
	 * @return An array containing all matching parts, or an empty array if none are found.
	 */
	public NameValuePair[] getAll(String name) {
		ArrayBuilder<NameValuePair> b = ArrayBuilder.of(NameValuePair.class).filter(NOT_NULL);
		for (NameValuePair x : this)
			if (eq(x.getName(), name))
				b.add(x);
		return b.orElse(EMPTY_ARRAY);
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
		for (int i = 0; i < size(); i++) {
			NameValuePair x = get(i);
			if (eq(x.getName(), name))
				return optional(x);
		}
		return empty();
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
		for (int i = size() - 1; i >= 0; i--) {
			NameValuePair x = get(i);
			if (eq(x.getName(), name))
				return optional(x);
		}
		return empty();
	}

	/**
	 * Performs an action on the string values for all matching parts.
	 *
	 * @param filter A predicate to apply to each element to determine if it should be included.  Can be <jk>null</jk>.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public PartList forEachValue(Predicate<NameValuePair> filter, Consumer<String> action) {
		return forEach(filter, x -> action.accept(x.getValue()));
	}

	/**
	 * Performs an action on the string values for all parts with the specified name.
	 *
	 * @param name The header name.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public PartList forEachValue(String name, Consumer<String> action) {
		return forEach(name, x -> action.accept(x.getValue()));
	}

	/**
	 * Returns all the string values for all parts with the specified name.
	 *
	 * @param name The part name.
	 * @return An array containing all values.  Never <jk>null</jk>.
	 */
	public String[] getValues(String name) {
		ArrayBuilder<String> b = ArrayBuilder.of(String.class).size(1);
		forEach(name, x -> b.add(x.getValue()));
		return b.orElse(EMPTY_STRING_ARRAY);
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
		for (NameValuePair x : this) {
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
	public PartIterator partIterator() {
		return new BasicPartIterator(getAll(), null, caseInsensitive);
	}

	/**
	 * Returns an iterator over the parts with a given name in this list.
	 *
	 * @param name The name of the parts over which to iterate, or <jk>null</jk> for all parts
	 *
	 * @return A new iterator over the matching parts in this list.
	 */
	public PartIterator partIterator(String name) {
		return new BasicPartIterator(getAll(), name, caseInsensitive);
	}

	/**
	 * Performs an action on all parts in this list with the specified name.
	 *
	 * <p>
	 * This is the preferred method for iterating over parts as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param name The parts name.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public PartList forEach(String name, Consumer<NameValuePair> action) {
		return forEach(x -> eq(name, x.getName()), action);
	}

	/**
	 * Performs an action on all the matching parts in this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over parts as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param filter A predicate to apply to each element to determine if it should be included.  Can be <jk>null</jk>.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public PartList forEach(Predicate<NameValuePair> filter, Consumer<NameValuePair> action) {
		forEach(x -> consume(filter, action, x));
		return this;
	}

	/**
	 * Returns a stream of the parts in this list with the specified name.
	 *
	 * <p>
	 * This does not involve a copy of the underlying array of <c>NameValuePair</c> objects so should perform well.
	 *
	 * @param name The part name.
	 * @return This object.
	 */
	public Stream<NameValuePair> stream(String name) {
		return Arrays.stream(getAll(name)).filter(x->eq(name, x.getName()));
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
		forEach(p -> {
			if (p != null) {
				String v = p.getValue();
				if (v != null) {
					if (sb.length() > 0)
						sb.append("&");
					sb.append(urlEncode(p.getName())).append('=').append(urlEncode(p.getValue()));
				}
			}
		});
		return sb.toString();
	}
}
