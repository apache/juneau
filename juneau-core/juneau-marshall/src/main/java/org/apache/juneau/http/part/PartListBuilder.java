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
import static org.apache.juneau.internal.ExceptionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * Builder for {@link PartList} objects.
 * {@review}
 */
@FluentSetters
@NotThreadSafe
public class PartListBuilder {

	final List<NameValuePair> entries;
	List<NameValuePair> defaultEntries;
	private VarResolver varResolver;
	boolean caseInsensitive = false;

	/** Predefined instance. */
	private static final PartList EMPTY = new PartList();

	/**
	 * Constructor.
	 */
	public PartListBuilder() {
		entries = new ArrayList<>();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public PartListBuilder(PartList copyFrom) {
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
	protected PartListBuilder(PartListBuilder copyFrom) {
		entries = new ArrayList<>(copyFrom.entries);
		defaultEntries = copyFrom.defaultEntries == null ? null : new ArrayList<>(copyFrom.defaultEntries);
		varResolver = copyFrom.varResolver;
		caseInsensitive = copyFrom.caseInsensitive;
	}

	/**
	 * Creates a modifiable copy of this builder.
	 *
	 * @return A shallow copy of this builder.
	 */
	public PartListBuilder copy() {
		return new PartListBuilder(this);
	}

	/**
	 * Creates a new {@link PartList} bean based on the contents of this builder.
	 *
	 * @return A new {@link PartList} bean.
	 */
	public PartList build() {
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
	public PartListBuilder resolving() {
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
	public PartListBuilder resolving(VarResolver varResolver) {
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
	public PartListBuilder caseInsensitive() {
		caseInsensitive = true;
		return this;
	}

	/**
	 * Removes any parts already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder clear() {
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
	public PartListBuilder append(PartList value) {
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
	public PartListBuilder append(NameValuePair value) {
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
	public PartListBuilder append(String name, Object value) {
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
	public PartListBuilder append(String name, Supplier<?> value) {
		return append(createPart(name, value));
	}

	/**
	 * Adds the specified parts to the end of the parts in this builder.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder append(NameValuePair...values) {
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
	public PartListBuilder append(List<? extends NameValuePair> values) {
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
	public PartListBuilder prepend(PartList value) {
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
	public PartListBuilder prepend(NameValuePair value) {
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
	public PartListBuilder prepend(String name, Object value) {
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
	public PartListBuilder prepend(String name, Supplier<?> value) {
		return prepend(createPart(name, value));
	}

	/**
	 * Adds the specified parts to the beginning of the parts in this builder.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder prepend(NameValuePair...values) {
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
	public PartListBuilder prepend(List<? extends NameValuePair> values) {
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
	public PartListBuilder remove(PartList value) {
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
	public PartListBuilder remove(NameValuePair value) {
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
	public PartListBuilder remove(NameValuePair...values) {
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
	public PartListBuilder remove(List<? extends NameValuePair> values) {
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
	public PartListBuilder remove(String name) {
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
	public PartListBuilder remove(String...names) {
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
	public PartListBuilder set(NameValuePair value) {
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
	public PartListBuilder set(NameValuePair...values) {
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
	public PartListBuilder set(String name, Object value) {
		return set(createPart(name, value));
	}

	/**
	 * Adds or replaces the part with the specified name.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder set(String name, Supplier<?> value) {
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
	public PartListBuilder set(List<? extends NameValuePair> values) {

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
	public PartListBuilder set(PartList values) {
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
	public PartListBuilder setDefault(NameValuePair value) {
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
	public PartListBuilder setDefault(NameValuePair...values) {
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
	public PartListBuilder setDefault(String name, Object value) {
		return setDefault(createPart(name, value));
	}

	/**
	 * Sets a default value for a part.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder setDefault(String name, Supplier<?> value) {
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
	public PartListBuilder setDefault(List<? extends NameValuePair> values) {

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
	public PartListBuilder setDefault(PartList values) {
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
	public PartListBuilder add(ListOperation flag, NameValuePair value) {
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
	public PartListBuilder add(ListOperation flag, NameValuePair...values) {
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
	public PartListBuilder add(ListOperation flag, String name, Object value) {
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
	public PartListBuilder add(ListOperation flag, String name, Supplier<?> value) {
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
	public PartListBuilder add(ListOperation flag, List<NameValuePair> values) {
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
	public PartListBuilder add(ListOperation flag, PartList values) {
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
	public PartListBuilder forEach(Consumer<NameValuePair> c) {
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
	public PartListBuilder forEach(String name, Consumer<NameValuePair> c) {
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

	@Override /* Object */
	public String toString() {
		return "[" + join(entries, ", ") + "]";
	}

	// <FluentSetters>

	// </FluentSetters>
}
