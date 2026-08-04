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
package org.apache.juneau.bean.mcp.v20260728;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * Fluent builder for a restricted elicitation JSON Schema (MCP {@code 2026-07-28} SEP-2322), producing the
 * {@link JsonMap} consumed by {@link ElicitRequest#setRequestedSchema(java.util.Map)}.
 *
 * <p>
 * The elicitation restricted schema permits only primitive-typed, non-nested top-level properties. There is
 * deliberately no {@code objectField}/{@code arrayField} method, so a nested/object/array-typed field is a
 * compile-time impossibility, never a runtime-rejected one. {@link #build()} additionally rejects a handful of
 * structurally-invalid per-field modifier combinations (for example {@link #format(String)} on a non-string
 * field) with an {@link IllegalStateException}, so a handler author gets an immediate, in-process failure rather
 * than a wire-level schema-validation error discovered by the client much later.
 *
 * <p>
 * Each {@code xxxField(name)} call starts a new field and becomes the target of every subsequent modifier call
 * ({@link #title(String)}, {@link #description(String)}, etc.) until the next {@code xxxField(...)} call.
 * Re-declaring a field under a {@code name} already used by an earlier {@code xxxField(...)} call replaces that
 * field's definition entirely, silently discarding any per-field modifiers already applied to it
 * (last-field-wins). {@link #minLength(int)}/{@link #maxLength(int)} are additional string-only per-field
 * modifiers, alongside {@link #format(String)}. {@link #required(String...)} is a separate, builder-level (not
 * per-field) call that marks one or more field names required, emitting a top-level {@code "required":[...]}
 * array; unlike the per-field modifiers, it is not tied to the most-recently-started field, is order-independent
 * with respect to {@code xxxField(...)} (a name may be marked required before or after the field it names is
 * (re)declared, and a redeclaration of that field does not discard the marking), and may be called any time,
 * with an unknown name validated only at {@link #build()} time.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	JsonMap <jv>schema</jv> = ElicitSchema.<jsm>create</jsm>()
 * 		.booleanField(<js>"confirm"</js>).title(<js>"Confirm"</js>)
 * 		.build();
 * </p>
 */
public class ElicitSchema {

	private static final String TYPE_STRING = "string";
	private static final String TYPE_NUMBER = "number";
	private static final String TYPE_INTEGER = "integer";
	private static final String TYPE_BOOLEAN = "boolean";
	private static final String TYPE_OBJECT = "object";

	private static final Set<String> NUMERIC_TYPES = Set.of(TYPE_NUMBER, TYPE_INTEGER);

	private final Map<String,JsonMap> properties = map();
	private final Set<String> required = new LinkedHashSet<>();
	private String currentField;

	private ElicitSchema() {}

	/**
	 * Creates a new, empty schema builder.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static ElicitSchema create() {
		return new ElicitSchema();
	}

	/**
	 * Starts a {@code string}-typed field.
	 *
	 * @param name The property name. Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
	 */
	public ElicitSchema stringField(String name) {
		return field(name, TYPE_STRING);
	}

	/**
	 * Starts a {@code number}-typed (floating-point) field.
	 *
	 * @param name The property name. Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
	 */
	public ElicitSchema numberField(String name) {
		return field(name, TYPE_NUMBER);
	}

	/**
	 * Starts an {@code integer}-typed field.
	 *
	 * @param name The property name. Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
	 */
	public ElicitSchema integerField(String name) {
		return field(name, TYPE_INTEGER);
	}

	/**
	 * Starts a {@code boolean}-typed field.
	 *
	 * @param name The property name. Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank.
	 */
	public ElicitSchema booleanField(String name) {
		return field(name, TYPE_BOOLEAN);
	}

	/**
	 * Starts a closed-choice {@code string}-typed field restricted to {@code values} (wire shape:
	 * {@code type:"string", enum:[...]}). Pair with {@link #enumNames(String...)} for parallel display labels.
	 *
	 * @param name The property name. Must not be <jk>null</jk> or blank.
	 * @param values The allowed values. Must not be <jk>null</jk> or empty, and must not contain a
	 * 	<jk>null</jk> element.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code name} is <jk>null</jk> or blank, or {@code values} is
	 * 	<jk>null</jk>, empty, or contains a <jk>null</jk> element.
	 */
	public ElicitSchema enumField(String name, String...values) {
		assertArgNotNullOrBlank("name", name);
		if (values == null || values.length == 0)
			throw iaex("Field ''%s'': enumField values must not be null or empty", name);
		for (var value : values)
			if (value == null)
				throw iaex("Field ''%s'': enumField values must not contain a null element", name);
		field(name, TYPE_STRING);
		properties.get(name).put("enum", List.of(values));
		return this;
	}

	private ElicitSchema field(String name, String type) {
		assertArgNotNullOrBlank("name", name);
		var p = new JsonMap();
		p.put("type", type);
		// Last-field-wins: a redeclared field discards its prior per-field modifiers (see class Javadoc).
		// required(...) marking is a separate, builder-level set and deliberately survives a redeclaration.
		properties.put(name, p);
		currentField = name;
		return this;
	}

	/**
	 * Sets the display title of the most-recently-started field.
	 *
	 * @param value The title. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ElicitSchema title(String value) {
		currentProperty().put("title", assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Sets the description of the most-recently-started field.
	 *
	 * @param value The description. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ElicitSchema description(String value) {
		currentProperty().put("description", assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Sets the string-subtype format of the most-recently-started field (for example {@code email}, {@code uri},
	 * {@code date}, {@code date-time}). Applying this to a non-string field is rejected at {@link #build()} time.
	 *
	 * @param value The format token. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ElicitSchema format(String value) {
		currentProperty().put("format", assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Sets the minimum UTF-16 code-unit length of the most-recently-started string field. Applying this to a
	 * non-string field, to an {@link #enumField(String, String...)} (a closed-choice field, where a length
	 * constraint is meaningless), or a value that is negative or exceeds a previously-/subsequently-set
	 * {@link #maxLength(int)}, is rejected at {@link #build()} time.
	 *
	 * @param value The minimum length.
	 * @return This object (for method chaining).
	 */
	public ElicitSchema minLength(int value) {
		currentProperty().put("minLength", value);
		return this;
	}

	/**
	 * Sets the maximum UTF-16 code-unit length of the most-recently-started string field. Applying this to a
	 * non-string field, to an {@link #enumField(String, String...)} (a closed-choice field, where a length
	 * constraint is meaningless), or a value that is negative or less than a previously-/subsequently-set
	 * {@link #minLength(int)}, is rejected at {@link #build()} time.
	 *
	 * @param value The maximum length.
	 * @return This object (for method chaining).
	 */
	public ElicitSchema maxLength(int value) {
		currentProperty().put("maxLength", value);
		return this;
	}

	/**
	 * Sets the minimum value of the most-recently-started number/integer field. Applying this to a field of any
	 * other type is rejected at {@link #build()} time.
	 *
	 * @param value The minimum. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ElicitSchema min(Number value) {
		currentProperty().put("minimum", assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Sets the maximum value of the most-recently-started number/integer field. Applying this to a field of any
	 * other type is rejected at {@link #build()} time.
	 *
	 * @param value The maximum. Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ElicitSchema max(Number value) {
		currentProperty().put("maximum", assertArgNotNull("value", value));
		return this;
	}

	/**
	 * Sets the default value of the most-recently-started field.
	 *
	 * <p>
	 * Named {@code defaultValue}, not {@code default} (a reserved Java keyword) &mdash; the wire schema property
	 * this writes is still literally {@code "default"}.
	 *
	 * @param value The default value. Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ElicitSchema defaultValue(Object value) {
		currentProperty().put("default", value);
		return this;
	}

	/**
	 * Sets parallel display labels for the most-recently-started {@link #enumField(String, String...)}'s values.
	 * Applying this to a field with no {@code enum} values is rejected at {@link #build()} time.
	 *
	 * @param values The display labels, parallel in order to the field's {@code enum} values. Must not be
	 * 	<jk>null</jk> or empty, must not contain a <jk>null</jk> element, and (when the current field already
	 * 	carries {@code enum} values) must be the same length as those values.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code values} is <jk>null</jk>, empty, or contains a <jk>null</jk>
	 * 	element, or if its length does not match the current field's existing {@code enum} values' length.
	 */
	public ElicitSchema enumNames(String...values) {
		if (values == null || values.length == 0)
			throw iaex("enumNames values must not be null or empty");
		for (var value : values)
			if (value == null)
				throw iaex("enumNames values must not contain a null element");
		var p = currentProperty();
		var enumValues = (List<?>)p.get("enum");
		if (enumValues != null && enumValues.size() != values.length)
			throw iaex("enumNames length (%s) must match enum length (%s)", values.length, enumValues.size());
		p.put("enumNames", List.of(values));
		return this;
	}

	/**
	 * Marks one or more already-added field names required, contributing to the built schema's top-level
	 * {@code "required":[...]} array.
	 *
	 * <p>
	 * Unlike the per-field modifiers ({@link #title(String)}, etc.), this is a builder-level, not per-field,
	 * call &mdash; it is not tied to the most-recently-started field and may name any field(s) already added via
	 * an earlier {@code xxxField(...)} call, in any order. May be called more than once; each call adds to the
	 * previously-marked names (not replaced). Naming a field not (yet) added is rejected at {@link #build()}
	 * time, not here, since a later {@code xxxField(...)} call may still add it.
	 *
	 * @param names The field names to mark required. Must not be <jk>null</jk> or empty, and must not contain
	 * 	a <jk>null</jk> element.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code names} is <jk>null</jk>, empty, or contains a <jk>null</jk>
	 * 	element.
	 */
	public ElicitSchema required(String...names) {
		if (names == null || names.length == 0)
			throw iaex("required field names must not be null or empty");
		for (var name : names)
			if (name == null)
				throw iaex("required field names must not contain a null element");
		Collections.addAll(required, names);
		return this;
	}

	private JsonMap currentProperty() {
		if (currentField == null)
			throw isex("No field has been started yet; call stringField(...)/numberField(...)/etc. first");
		return properties.get(currentField);
	}

	/**
	 * Validates every field's modifier combination (and the builder-level {@link #required(String...)}
	 * marking) and builds the final restricted-schema {@link JsonMap}
	 * ({@code {"type":"object","properties":{...},"required":[...]}}, the last member present only if
	 * {@link #required(String...)} was called at least once).
	 *
	 * @return The built schema, ready for {@link ElicitRequest#setRequestedSchema(java.util.Map)}.
	 * @throws IllegalStateException If no field was ever added, if any field carries a structurally-invalid
	 * 	modifier combination, or if {@link #required(String...)} named a field that was never added.
	 */
	public JsonMap build() {
		if (properties.isEmpty())
			throw isex("ElicitSchema requires at least one field (an empty requestedSchema is unanswerable)");
		properties.forEach((name, p) -> {
			var type = (String)p.get("type");
			if (p.containsKey("format") && ! TYPE_STRING.equals(type))
				throw isex("Field ''%s'': format is only valid on string fields", name);
			if ((p.containsKey("minimum") || p.containsKey("maximum")) && ! NUMERIC_TYPES.contains(type))
				throw isex("Field ''%s'': min/max are only valid on number/integer fields", name);
			if ((p.containsKey("minLength") || p.containsKey("maxLength")) && ! TYPE_STRING.equals(type))
				throw isex("Field ''%s'': minLength/maxLength are only valid on string fields", name);
			if ((p.containsKey("minLength") || p.containsKey("maxLength")) && p.containsKey("enum"))
				throw isex("Field ''%s'': minLength/maxLength are not valid on an enum (closed-choice) field", name);
			if (p.containsKey("minLength") && (int)p.get("minLength") < 0)
				throw isex("Field ''%s'': minLength must not be negative", name);
			if (p.containsKey("maxLength") && (int)p.get("maxLength") < 0)
				throw isex("Field ''%s'': maxLength must not be negative", name);
			if (p.containsKey("minLength") && p.containsKey("maxLength") && (int)p.get("minLength") > (int)p.get("maxLength"))
				throw isex("Field ''%s'': minLength must not exceed maxLength", name);
			if (p.containsKey("enumNames") && ! p.containsKey("enum"))
				throw isex("Field ''%s'': enumNames requires enum values (use enumField(...))", name);
		});
		required.forEach(name -> {
			if (! properties.containsKey(name))
				throw isex("required(...) names field ''%s'', which was never added", name);
		});
		var schema = new JsonMap();
		schema.put("type", TYPE_OBJECT);
		// Deep-copy each per-field map so a caller who keeps the builder and applies further modifiers after
		// build() cannot mutate an already-returned schema.
		var propsCopy = new JsonMap();
		properties.forEach((name, p) -> propsCopy.put(name, new JsonMap(p)));
		schema.put("properties", propsCopy);
		if (! required.isEmpty())
			schema.put("required", List.copyOf(required));
		return schema;
	}
}
