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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * An simple list of HTTP parts (form-data, query-parameters, path-parameters).
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	PartList <jv>parts</jv> = PartList
 * 		.<jsm>create</jsm>()
 * 		.append(MyPart.<jsm>of</jsm>(<js>"foo"</js>))
 * 		.append(<js>"Bar"</js>, ()-&gt;<jsm>getDynamicValueFromSomewhere</jsm>());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@FluentSetters
public class PartList extends ControlledArrayList<NameValuePair> {

	private static final long serialVersionUID = 1L;

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents no part list in annotations. */
	public static final class Void extends PartList {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Instantiates a new part list.
	 *
	 * @return A new part list.
	 */
	public static PartList create() {
		return new PartList();
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
		return new PartList().append(parts);
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
		return new PartList().append(parts);
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
	public static PartList ofPairs(String...pairs) {
		PartList x = new PartList();
		if (pairs == null)
			pairs = new String[0];
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into PartList.ofPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			x.add(BasicPart.of(pairs[i], pairs[i+1]));
		return x;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private VarResolver varResolver;
	boolean caseInsensitive;

	/**
	 * Constructor.
	 */
	public PartList() {
		super(false);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy.
	 */
	protected PartList(PartList copyFrom) {
		super(false, copyFrom);
		caseInsensitive = copyFrom.caseInsensitive;
	}

	/**
	 * Makes a copy of this list.
	 *
	 * @return A new copy of this list.
	 */
	public PartList copy() {
		return new PartList(this);
	}

	/**
	 * Adds a collection of default parts.
	 *
	 * <p>
	 * Default parts are set if they're not already in the list.
	 *
	 * @param parts The list of default parts.
	 * @return This object.
	 */
	public PartList setDefault(List<NameValuePair> parts) {
		if (parts != null)
			parts.stream().filter(x -> x != null && ! contains(x.getName())).forEach(x -> set(x));
		return this;
	}

	/**
	 * Makes a copy of this list of parts and adds a collection of default parts.
	 *
	 * <p>
	 * Default parts are set if they're not already in the list.
	 *
	 * @param parts The list of default parts.
	 * @return A new list, or the same list if the parts were empty.
	 */
	public PartList setDefault(NameValuePair...parts) {
		if (parts != null)
			setDefault(Arrays.asList(parts));
		return this;
	}

	/**
	 * Replaces the first occurrence of the part with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public PartList setDefault(String name, Object value) {
		return setDefault(createPart(name, value));
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public PartList setDefault(String name, Supplier<?> value) {
		return setDefault(createPart(name, value));
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
	 * </ul>
	 *
	 * <p>
	 * Uses {@link VarResolver#DEFAULT} to resolve variables.
	 *
	 * @return This object.
	 */
	public PartList resolving() {
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
	 * </ul>
	 *
	 * @param varResolver The variable resolver to use for resolving variables.
	 * @return This object.
	 */
	public PartList resolving(VarResolver varResolver) {
		assertModifiable();
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Specifies that the parts in this list should be treated as case-sensitive.
	 *
	 * <p>
	 * The default behavior is case-sensitive.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public PartList caseInsensitive(boolean value) {
		assertModifiable();
		caseInsensitive = value;
		return this;
	}

	/**
	 * Adds the specified part to the end of the parts in this list.
	 *
	 * @param value The part to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList append(NameValuePair value) {
		if (value != null)
			add(value);
		return this;
	}

	/**
	 * Appends the specified part to the end of this list.
	 *
	 * <p>
	 * The part is added as a {@link BasicPart}.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object.
	 */
	public PartList append(String name, Object value) {
		return append(createPart(name, value));
	}

	/**
	 * Appends the specified part to the end of this list using a value supplier.
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
	public PartList append(String name, Supplier<?> value) {
		return append(createPart(name, value));
	}

	/**
	 * Adds the specified parts to the end of the parts in this list.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList append(NameValuePair...values) {
		if (values != null)
			for (int i = 0; i < values.length; i++)
				append(values[i]);
		return this;
	}

	/**
	 * Adds the specified parts to the end of the parts in this list.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList append(List<NameValuePair> values) {
		if (values != null)
			values.forEach(x -> append(x));
		return this;
	}

	/**
	 * Adds the specified part to the beginning of the parts in this list.
	 *
	 * @param value The part to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList prepend(NameValuePair value) {
		if (value != null)
			add(0, value);
		return this;
	}

	/**
	 * Appends the specified part to the beginning of this list.
	 *
	 * <p>
	 * The part is added as a {@link BasicPart}.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object.
	 */
	public PartList prepend(String name, Object value) {
		return prepend(createPart(name, value));
	}

	/**
	 * Appends the specified part to the beginning of this list using a value supplier.
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
	public PartList prepend(String name, Supplier<?> value) {
		return prepend(createPart(name, value));
	}

	/**
	 * Adds the specified parts to the beginning of the parts in this list.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList prepend(NameValuePair...values) {
		if (values != null)
			prepend(alist(values));
		return this;
	}

	/**
	 * Adds the specified parts to the beginning of the parts in this list.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList prepend(List<NameValuePair> values) {
		if (values != null)
			addAll(0, values);
		return this;
	}

	/**
	 * Removes the specified part from this list.
	 *
	 * @param value The part to remove.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList remove(NameValuePair value) {
		if (value != null)
			removeIf(x -> eq(x.getName(), value.getName()) && eq(x.getValue(), value.getValue()));
		return this;
	}

	/**
	 * Removes the specified parts from this list.
	 *
	 * @param values The parts to remove.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList remove(NameValuePair...values) {
		for (int i = 0; i < values.length; i++)
			remove(values[i]);
		return this;
	}

	/**
	 * Removes the specified parts from this list.
	 *
	 * @param values The parts to remove.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList remove(List<NameValuePair> values) {
		if (values != null)
			values.forEach(x -> remove(x));
		return this;
	}

	/**
	 * Removes the part with the specified name from this list.
	 *
	 * @param name The part name.
	 * @return This object.
	 */
	@FluentSetter
	public PartList remove(String name) {
		removeIf(x -> eq(x.getName(), name));
		return this;
	}

	/**
	 * Removes the part with the specified name from this list.
	 *
	 * @param names The part name.
	 * @return This object.
	 */
	@FluentSetter
	public PartList remove(String...names) {
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
	 * @param value The part to replace.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList set(NameValuePair value) {
		if (value != null) {
			boolean replaced = false;
			for (int i = 0, j = size(); i < j; i++) {
				NameValuePair x = get(i);
				if (eq(x.getName(), value.getName())) {
					if (replaced) {
						remove(i);
						j--;
					} else {
						set(i, value);
						replaced = true;
					}
				}
			}

			if (! replaced)
				add(value);
		}

		return this;
	}

	/**
	 * Adds or replaces the part(s) with the same name.
	 *
	 * <p>
	 * If no part with the same name is found the given part is added to the end of the list.
	 *
	 * @param values The part to replace.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList set(NameValuePair...values) {
		if (values != null)
			set(alist(values));
		return this;
	}

	/**
	 * Replaces the first occurrence of the parts with the same name.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object.
	 */
	public PartList set(String name, Object value) {
		return set(createPart(name, value));
	}

	/**
	 * Replaces the first occurrence of the parts with the same name.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object.
	 */
	public PartList set(String name, Supplier<?> value) {
		return set(createPart(name, value));
	}

	/**
	 * Replaces the first occurrence of the parts with the same name.
	 *
	 * <p>
	 * If no part with the same name is found the given part is added to the end of the list.
	 *
	 * @param values The parts to replace.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public PartList set(List<NameValuePair> values) {

		if (values != null) {
			for (int i1 = 0, j1 = values.size(); i1 < j1; i1++) {
				NameValuePair h = values.get(i1);
				if (h != null) {
					for (int i2 = 0, j2 = size(); i2 < j2; i2++) {
						NameValuePair x = get(i2);
						if (eq(x.getName(), h.getName())) {
							remove(i2);
							j2--;
						}
					}
				}
			}

			for (int i = 0, j = values.size(); i < j; i++) {
				NameValuePair x = values.get(i);
				if (x != null) {
					add(x);
				}
			}
		}

		return this;
	}

	/**
	 * Gets the first part with the given name.
	 *
	 * <p>
	 * Part name comparison is case sensitive by default.
	 *
	 * @param name The part name.
	 * @return The first matching part, or {@link Optional#empty()} if not found.
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
	 * Part name comparison is case sensitive by default.
	 *
	 * @param name The part name.
	 * @return The last matching part, or {@link Optional#empty()} if not found.
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

		return optional(new BasicStringPart(name, sb.toString()));
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
	 * Gets all of the parts.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the parts were added.
	 * Each call creates a new array not backed by this list.
	 *
	 * <p>
	 * As a general rule, it's more efficient to use the other methods with consumers to
	 * get parts.
	 *
	 * @return An array containing all parts, never <jk>null</jk>.
	 */
	public NameValuePair[] getAll() {
		return stream().toArray(NameValuePair[]::new);
	}

	/**
	 * Gets all of the parts with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the parts were added.
	 * Part name comparison is case sensitive by default.
	 * Parts with null values are ignored.
	 * Each call creates a new array not backed by this list.
	 *
	 * <p>
	 * As a general rule, it's more efficient to use the other methods with consumers to
	 * get parts.
	 *
	 * @param name The part name.
	 *
	 * @return An array containing all matching parts, never <jk>null</jk>.
	 */
	public NameValuePair[] getAll(String name) {
		return stream().filter(x -> eq(x.getName(), name)).toArray(NameValuePair[]::new);
	}

	/**
	 * Performs an action on the values for all matching parts in this list.
	 *
	 * @param filter A predicate to apply to each element to determine if it should be included.  Can be <jk>null</jk>.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public PartList forEachValue(Predicate<NameValuePair> filter, Consumer<String> action) {
		return forEach(filter, x -> action.accept(x.getValue()));
	}

	/**
	 * Performs an action on the values of all matching parts in this list.
	 *
	 * @param name The part name.
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
		return stream().filter(x -> eq(x.getName(), name)).map(x -> x.getValue()).toArray(String[]::new);
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
		return stream().anyMatch(x -> eq(x.getName(), name));
	}

	/**
	 * Returns an iterator over this list of parts.
	 *
	 * @return A new iterator over this list of parts.
	 */
	public PartIterator partIterator() {
		return new BasicPartIterator(toArray(new NameValuePair[0]), null, caseInsensitive);
	}

	/**
	 * Returns an iterator over the parts with a given name in this list.
	 *
	 * @param name The name of the parts over which to iterate, or <jk>null</jk> for all parts
	 *
	 * @return A new iterator over the matching parts in this list.
	 */
	public PartIterator partIterator(String name) {
		return new BasicPartIterator(getAll(name), name, caseInsensitive);
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
		return stream().filter(x->eq(name, x.getName()));
	}

	//-------------------------------------------------------------------------------------------------------------
	// Other methods
	//-------------------------------------------------------------------------------------------------------------

	private NameValuePair createPart(String name, Object value) {
		boolean isResolving = varResolver != null;

		if (value instanceof Supplier<?>) {
			Supplier<?> value2 = (Supplier<?>)value;
			return isResolving ? new BasicPart(name, resolver(value2)) : new BasicPart(name, value2);
		}
		return isResolving ? new BasicPart(name, resolver(value)) : new BasicPart(name, value);
	}

	private Supplier<Object> resolver(Object input) {
		return ()->varResolver.resolve(stringify(unwrap(input)));
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	private boolean eq(String s1, String s2) {
		return caseInsensitive ? StringUtils.eqic(s1, s2) : StringUtils.eq(s1, s2);
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

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.collections.ControlledArrayList */
	public PartList setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}