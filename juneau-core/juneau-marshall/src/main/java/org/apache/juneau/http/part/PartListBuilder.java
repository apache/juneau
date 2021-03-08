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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.svl.*;

/**
 * Builder for {@link PartList} objects.
 */
@FluentSetters
public class PartListBuilder {

	final List<Part> parts;
	private volatile VarResolver varResolver;

	/** Predefined instance. */
	private static final PartList EMPTY = new PartList();

	/**
	 * Constructor.
	 */
	public PartListBuilder() {
		parts = new ArrayList<>();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public PartListBuilder(PartList copyFrom) {
		parts = new ArrayList<>(copyFrom.parts);

	}

	/**
	 * Creates a new {@link PartList} bean based on the contents of this builder.
	 *
	 * @return A new {@link PartList} bean.
	 */
	public PartList build() {
		return parts.isEmpty() ? EMPTY : new PartList(this);
	}

	/**
	 * Allows part values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in part values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartType, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartType,HttpPartSerializerSession,HttpPartSchema,boolean)}
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
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartType, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartType,HttpPartSerializerSession,HttpPartSchema,boolean)}
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
	 * Removes any parts already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder clear() {
		parts.clear();
		return this;
	}

	/**
	 * Adds the specified part to the end of the parts in this builder.
	 *
	 * @param value The part to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(Part value) {
		if (value != null)
			parts.add(value);
		return this;
	}

	/**
	 * Adds the specified part to the end of the parts in this builder.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(String name, String value) {
		Part x = isResolving() ? new BasicPart(name, resolver(value)) : new BasicPart(name, value);
		return add(x);
	}

	/**
	 * Appends the specified part to the end of this list.
	 *
	 * <p>
	 * The part is added as a {@link BasicPart}.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder add(String name, Object value) {
		Part x = isResolving() ? new BasicPart(name, resolver(value)) : new BasicPart(name, value);
		return add(x);
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
	 * @return This object (for method chaining).
	 */
	public PartListBuilder add(String name, Supplier<?> value) {
		Part x = isResolving() ? new BasicPart(name, resolver(value)) : new BasicPart(name, value);
		return add(x);
	}

	/**
	 * Appends the specified part to the end of this list.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @param type The HTTP part type.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder add(String name, Object value, HttpPartType type, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		Part x = isResolving() ? new SerializedPart(name, resolver(value), type, serializer, schema, skipIfEmpty) : new SerializedPart(name, value, type, serializer, schema, skipIfEmpty);
		return add(x);
	}

	/**
	 * Adds the specified parts to the end of the parts in this builder.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(Part...values) {
		for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
			add(values[i]);
		return this;
	}

	/**
	 * Adds the specified parts to the end of the parts in this builder.
	 *
	 * @param values The parts to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(List<Part> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			add(values.get(i));
		return this;
	}

	/**
	 * Adds the specified parts to the end of the parts in this builder.
	 *
	 * @param values The part to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder add(PartList values) {
		if (values != null)
			add(values.getAll());
		return this;
	}

	/**
	 * Removes the specified parts from this builder.
	 *
	 * @param value The parts to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder remove(Part value) {
		if (value != null)
			parts.remove(value);
		return this;
	}

	/**
	 * Removes the specified parts from this builder.
	 *
	 * @param values The parts to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder remove(Part...values) {
		for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
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
	public PartListBuilder remove(List<Part> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
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
		for (int i = 0; i < parts.size(); i++) /* See HTTPCORE-361 */
			if (parts.get(i).getName().equals(name))
				parts.remove(i--);
		return null;
	}

	/**
	 * Replaces the first occurrence of the part with the same name.
	 *
	 * <p>
	 * If no part with the same name is found the given part is added to the end of the list.
	 *
	 * @param value The parts to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder update(Part value) {
		if (value == null)
			return this;

		for (int i = 0; i < parts.size(); i++) {
			Part x = parts.get(i);
			if (x.getName().equals(value.getName())) {
				parts.set(i, value);
				return this;
			}
		}

		parts.add(value);
		return this;
	}

	/**
	 * Replaces the first occurrence of the parts with the same name.
	 *
	 * <p>
	 * If no part with the same name is found the given part is added to the end of the list.
	 *
	 * @param values The parts to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder update(Part...values) {
		for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
			update(values[i]);
		return this;
	}

	/**
	 * Replaces the first occurrence of the parts with the same name.
	 *
	 * <p>
	 * If no part with the same name is found the given part is added to the end of the list.
	 *
	 * @param values The parts to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder update(List<Part> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			update(values.get(i));
		return this;
	}

	/**
	 * Sets all of the parts contained within this list overriding any existing parts.
	 *
	 * <p>
	 * The parts are added in the order in which they appear in the array.
	 *
	 * @param values The parts to set
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder set(Part...values) {
		clear();
		Collections.addAll(parts, values);
		return this;
	}

	/**
	 * Sets all of the parts contained within this list overriding any existing parts.
	 *
	 * <p>
	 * The parts are added in the order in which they appear in the list.
	 *
	 * @param values The parts to set
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder set(List<Part> values) {
		clear();
		parts.addAll(values);
		return this;
	}

	/**
	 * Appends or replaces the part values in this list.
	 *
	 * <p>
	 * If the part already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder appendUnique(Part...values) {
		for (Part h : values) {
			boolean replaced = false;
			for (ListIterator<Part> li = parts.listIterator(); li.hasNext();) {
				Part h2 = li.next();
				if (h2.getName().equals(h.getName())) {
					li.set(h);
					replaced = true;
					break;
				}
			}
			if (! replaced)
				add(h);
		}
		return this;
	}


	/**
	 * Appends or replaces the part values in this list.
	 *
	 * <p>
	 * If the part already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder appendUnique(Collection<Part> values) {
		for (Part h : values) {
			boolean replaced = false;
			for (ListIterator<Part> li = parts.listIterator(); li.hasNext();) {
				Part h2 = li.next();
				if (h2.getName().equals(h.getName())) {
					li.set(h);
					replaced = true;
					break;
				}
			}
			if (! replaced)
				add(h);
		}
		return this;
	}

	private boolean isResolving() {
		return varResolver != null;
	}

	private Supplier<Object> resolver(Object input) {
		return ()->(varResolver == null ? unwrap(input) : varResolver.resolve(stringify(unwrap(input))));
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	// <FluentSetters>

	// </FluentSetters>
}
