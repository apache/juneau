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
package org.apache.juneau.bean.jsonschema;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.math.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Validates values against a {@link JsonSchema} bean per the JSON Schema Draft 2020-12 specification.
 *
 * <p>
 * This is the {@code juneau-bean-jsonschema} implementation of the commons-side
 * {@link PropertyValidator} SPI.  It is registered as a {@link PropertyValidatorFactory} via
 * {@link ServiceLoader ServiceLoader} so that marshalling-side code (e.g.
 * {@code MarshalledPropertyPostProcessor}) can install schema validation closures on bean properties without taking a
 * compile-time dependency on this module.
 *
 * <h5 class='section'>Supported keywords (v1):</h5>
 * <ul class='spaced-list'>
 * 	<li>Any type: {@code type}, {@code enum}, {@code const}.
 * 	<li>Numeric: {@code minimum}, {@code maximum}, {@code exclusiveMinimum}, {@code exclusiveMaximum},
 * 		{@code multipleOf} (Draft 2020-12 numeric form for the exclusive bounds).
 * 	<li>String: {@code minLength}, {@code maxLength}, {@code pattern}.
 * 	<li>Array: {@code minItems}, {@code maxItems}, {@code uniqueItems}, {@code items} (recursive — single-schema
 * 		form only; tuple form via {@link JsonSchemaArray} is not yet validated).
 * 	<li>Object: {@code minProperties}, {@code maxProperties}, {@code required}, {@code properties} (recursive on
 * 		{@link Map}-shaped values).
 * </ul>
 *
 * <h5 class='section'>Deferred keywords:</h5>
 * <ul class='spaced-list'>
 * 	<li>{@code format} — informational only by default; validators may treat it as advisory.
 * 	<li>{@code patternProperties}, {@code additionalProperties}, {@code prefixItems}, {@code contains},
 * 		{@code minContains}, {@code maxContains}, {@code dependentRequired}, {@code dependentSchemas}.
 * 	<li>Composition keywords: {@code allOf}, {@code anyOf}, {@code oneOf}, {@code not}, {@code if}/{@code then}/{@code else}.
 * 	<li>References: {@code $ref}, {@code $defs} (the bean carries them; the validator does not resolve them yet).
 * </ul>
 *
 * <h5 class='section'>Descriptive keywords (always ignored):</h5>
 * <ul class='spaced-list'>
 * 	<li>{@code title}, {@code summary}, {@code description}, {@code examples}, {@code $comment}, {@code deprecated},
 * 		{@code readOnly}, {@code writeOnly}.
 * </ul>
 *
 * <h5 class='section'>Java-value mapping:</h5>
 * <p>
 * Java values are matched against JSON Schema {@code type} keywords as follows:
 * <ul class='spaced-list'>
 * 	<li>{@code string} — {@link CharSequence} or {@link Character}.
 * 	<li>{@code integer} — integral {@link Number} (Byte, Short, Integer, Long, BigInteger) or a floating-point number
 * 		whose value has no fractional part.
 * 	<li>{@code number} — any {@link Number}.
 * 	<li>{@code boolean} — {@link Boolean}.
 * 	<li>{@code array} — {@link Collection} or Java array.
 * 	<li>{@code object} — {@link Map} or any other reference type (treated as a bean — the marshalling layer installs
 * 		separate validators on the bean's own {@code @Schema}-annotated properties).
 * 	<li>{@code null} — {@code null}.
 * 	<li>{@code any} — always matches.
 * </ul>
 *
 * <h5 class='section'>Nested validation:</h5>
 * <p>
 * For property-level validation, the validator recurses into:
 * <ul class='spaced-list'>
 * 	<li>{@code items} — every element of an array / collection is validated against the items schema.
 * 	<li>{@code properties} on a {@link Map} value — each declared sub-property is validated against its sub-schema.
 * </ul>
 *
 * <p>
 * The validator does <b>not</b> recurse into Java beans via reflection.  Inner {@code @Schema} annotations on a
 * nested bean's own properties are validated independently by the marshalling layer's per-property install hooks.
 *
 * <h5 class='section'>Thread safety:</h5>
 * Instances are immutable and safe to share across threads.  The compiled {@link Pattern} for the {@code pattern}
 * keyword is cached on construction.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a href="https://json-schema.org/draft/2020-12/json-schema-validation.html">JSON Schema 2020-12 Validation</a>
 * </ul>
 *
 * @see PropertyValidator
 * @see JsonSchemaPropertyValidatorFactory
 * @since 10.0.0
 */
public final class JsonSchemaValidator implements PropertyValidator {

	private final JsonSchema<?> schema;
	private final Pattern patternCache;

	/**
	 * Builds a validator from a {@link JsonSchema} bean.
	 *
	 * @param schema The schema to validate against.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return A new validator.
	 */
	public static JsonSchemaValidator of(JsonSchema<?> schema) {
		assertArgNotNull("schema", schema);
		return new JsonSchemaValidator(schema);
	}

	/**
	 * Builds a validator from a JSON-Schema-shaped {@link JsonMap}.
	 *
	 * <p>
	 * Equivalent to {@code of(JsonSchemaBeanGenerator.toBean(schemaMap))}.
	 *
	 * @param schemaMap The schema map.  Must not be <jk>null</jk>, or an {@link IllegalArgumentException} is thrown.
	 * @return A new validator.
	 */
	public static JsonSchemaValidator of(JsonMap schemaMap) {
		assertArgNotNull("schemaMap", schemaMap);
		return of(JsonSchemaBeanGenerator.toBean(schemaMap));
	}

	private JsonSchemaValidator(JsonSchema<?> schema) {
		this.schema = schema;
		var p = schema.getPattern();
		this.patternCache = p == null ? null : Pattern.compile(p);
	}

	/**
	 * Returns the schema this validator was built from.
	 *
	 * @return The schema.
	 */
	public JsonSchema<?> getSchema() {
		return schema;
	}

	@Override
	public void validate(Object value) throws SchemaValidationException {
		validateAgainst(schema, value, patternCache);
	}

	@SuppressWarnings({
		"java:S3776"  // Cognitive complexity — dispatch table over the JSON Schema keyword set.
	})
	private static void validateAgainst(JsonSchema<?> s, Object value, Pattern patternOverride) {
		validateEnum(s, value);
		validateConst(s, value);

		if (value == null)
			return;

		validateType(s, value);

		if (value instanceof CharSequence value2) {
			validateString(s, value2.toString(), patternOverride);
		} else if (value instanceof Character value2) {
			validateString(s, value2.toString(), patternOverride);
		} else if (value instanceof Number value2) {
			validateNumber(s, value2);
		} else if (value instanceof Collection<?> value2) {
			validateArray(s, value2);
		} else if (value.getClass().isArray()) {
			validateArray(s, arrayAsList(value));
		} else if (value instanceof Map<?,?> value2) {
			validateObject(s, value2);
		}
		// Java beans: per-property @Schema validators are installed independently on the bean side.
	}

	// =================================================================================================================
	// Type / enum / const
	// =================================================================================================================

	private static void validateEnum(JsonSchema<?> s, Object value) {
		var enums = s.getEnum();
		if (ie(enums))
			return;
		for (var e : enums) {
			if (jsonEquals(e, value))
				return;
		}
		throw new SchemaValidationException("Value '%s' does not match one of the allowed enum values.", value);
	}

	private static void validateConst(JsonSchema<?> s, Object value) {
		var c = s.getConst();
		if (c == null)
			return;  // const=null is indistinguishable from "not set" in the bean; treat as unset.
		if (! jsonEquals(c, value))
			throw new SchemaValidationException("Value '%s' does not match required const '%s'.", value, c);
	}

	private static void validateType(JsonSchema<?> s, Object value) {
		var single = s.getTypeAsJsonType();
		if (nn(single)) {
			if (! matchesType(single, value))
				throw new SchemaValidationException("Value of type '%s' does not match expected schema type '%s'.", typeName(value), single);
			return;
		}
		var arr = s.getTypeAsJsonTypeArray();
		if (ie(arr))
			return;
		for (var t : arr) {
			if (matchesType(t, value))
				return;
		}
		throw new SchemaValidationException("Value of type '%s' does not match any expected schema types '%s'.", typeName(value), arr);
	}

	@SuppressWarnings({
		"java:S1142"  // Multiple returns — switch arms each return their own predicate result.
	})
	private static boolean matchesType(JsonType t, Object value) {
		switch (t) {
			case STRING:
				return value instanceof CharSequence || value instanceof Character;
			case INTEGER:
				return isIntegralValue(value);
			case NUMBER:
				return value instanceof Number;
			case BOOLEAN:
				return value instanceof Boolean;
			case ARRAY:
				return value instanceof Collection<?> || (nn(value) && value.getClass().isArray());
			case OBJECT:
				return value instanceof Map<?,?> || isBeanLike(value);
			case NULL:
				return value == null;
			case ANY:
				return true;
			default:
				return true;
		}
	}

	private static boolean isIntegralValue(Object value) {
		if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigInteger)
			return true;
		if (value instanceof Number value2) {
			try {
				var bd = toBigDecimal(value2);
				return bd.stripTrailingZeros().scale() <= 0;
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static boolean isBeanLike(Object value) {
		if (value == null)
			return false;
		var c = value.getClass();
		return ! (value instanceof CharSequence || value instanceof Character || value instanceof Number
			|| value instanceof Boolean || value instanceof Collection<?> || c.isArray());
	}

	private static String typeName(Object value) {
		return value == null ? "null" : cns(value);
	}

	// =================================================================================================================
	// String
	// =================================================================================================================

	private static void validateString(JsonSchema<?> s, String value, Pattern patternOverride) {
		var minL = s.getMinLength();
		var maxL = s.getMaxLength();
		var len = value.codePointCount(0, value.length());
		if (nn(minL) && len < minL)
			throw new SchemaValidationException("String length %s is less than minLength %s.", len, minL);
		if (nn(maxL) && len > maxL)
			throw new SchemaValidationException("String length %s exceeds maxLength %s.", len, maxL);
		var pattern = patternOverride;
		if (pattern == null && nn(s.getPattern()))
			pattern = Pattern.compile(s.getPattern());
		if (pattern != null && ! pattern.matcher(value).find())
			throw new SchemaValidationException("Value '%s' does not match pattern '%s'.", value, s.getPattern());
	}

	// =================================================================================================================
	// Number
	// =================================================================================================================

	@SuppressWarnings({
		"java:S3776"  // Numeric keyword fan-out is intentionally flat; each branch maps to one Draft 2020-12 keyword.
	})
	private static void validateNumber(JsonSchema<?> s, Number value) {
		BigDecimal bd;
		try {
			bd = toBigDecimal(value);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new SchemaValidationException("Value '%s' is not a finite number.", value);
		}

		if (nn(s.getMinimum()) && bd.compareTo(toBigDecimal(s.getMinimum())) < 0)
			throw new SchemaValidationException("Value %s is less than minimum %s.", bd, s.getMinimum());
		if (nn(s.getMaximum()) && bd.compareTo(toBigDecimal(s.getMaximum())) > 0)
			throw new SchemaValidationException("Value %s exceeds maximum %s.", bd, s.getMaximum());
		if (nn(s.getExclusiveMinimum()) && bd.compareTo(toBigDecimal(s.getExclusiveMinimum())) <= 0)
			throw new SchemaValidationException("Value %s is not greater than exclusive minimum %s.", bd, s.getExclusiveMinimum());
		if (nn(s.getExclusiveMaximum()) && bd.compareTo(toBigDecimal(s.getExclusiveMaximum())) >= 0)
			throw new SchemaValidationException("Value %s is not less than exclusive maximum %s.", bd, s.getExclusiveMaximum());
		if (nn(s.getMultipleOf())) {
			var mof = toBigDecimal(s.getMultipleOf());
			if (mof.signum() != 0 && bd.remainder(mof).compareTo(BigDecimal.ZERO) != 0)
				throw new SchemaValidationException("Value %s is not a multiple of %s.", bd, mof);
		}
	}

	// =================================================================================================================
	// Array
	// =================================================================================================================

	private static void validateArray(JsonSchema<?> s, Collection<?> value) {
		var min = s.getMinItems();
		var max = s.getMaxItems();
		var size = value.size();
		if (nn(min) && size < min)
			throw new SchemaValidationException("Array size %s is less than minItems %s.", size, min);
		if (nn(max) && size > max)
			throw new SchemaValidationException("Array size %s exceeds maxItems %s.", size, max);
		if (isTrue(s.getUniqueItems()) && hasDuplicates(value))
			throw new SchemaValidationException("Array contains duplicate items but uniqueItems is true.");
		var items = s.getItemsAsSchema();
		if (nn(items)) {
			for (var item : value)
				validateAgainst(items, item, null);
		}
	}

	private static List<Object> arrayAsList(Object array) {
		var len = Array.getLength(array);
		var out = new ArrayList<>(len);
		for (var i = 0; i < len; i++)
			out.add(Array.get(array, i));
		return out;
	}

	private static boolean hasDuplicates(Collection<?> c) {
		if (c.size() < 2)
			return false;
		var seen = new ArrayList<>(c.size());
		for (var e : c) {
			for (var s : seen) {
				if (jsonEquals(e, s))
					return true;
			}
			seen.add(e);
		}
		return false;
	}

	// =================================================================================================================
	// Object (Map / bean shape)
	// =================================================================================================================

	@SuppressWarnings({
		"java:S3776"  // Cognitive complexity — flat fan-out over the Draft 2020-12 object keywords (min/maxProperties, required, properties).
	})
	private static void validateObject(JsonSchema<?> s, Map<?,?> value) {
		var minP = s.getMinProperties();
		var maxP = s.getMaxProperties();
		var size = value.size();
		if (nn(minP) && size < minP)
			throw new SchemaValidationException("Object property count %s is less than minProperties %s.", size, minP);
		if (nn(maxP) && size > maxP)
			throw new SchemaValidationException("Object property count %s exceeds maxProperties %s.", size, maxP);
		var required = s.getRequired();
		if (nn(required)) {
			for (var key : required) {
				if (! value.containsKey(key))
					throw new SchemaValidationException("Required property '%s' is missing.", key);
			}
		}
		var props = s.getProperties();
		if (nn(props)) {
			for (var entry : props.entrySet()) {
				var name = entry.getKey();
				var sub = entry.getValue();
				if (value.containsKey(name))
					validateAgainst(sub, value.get(name), null);
			}
		}
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	private static BigDecimal toBigDecimal(Number n) {
		if (n instanceof BigDecimal n2)
			return n2;
		if (n instanceof BigInteger n2)
			return new BigDecimal(n2);
		if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte)
			return BigDecimal.valueOf(n.longValue());
		return new BigDecimal(n.toString());
	}

	@SuppressWarnings({
		"java:S3776"  // Equality fan-out — mirrors the type matrix exposed by deserialized JSON Schema bean values.
	})
	private static boolean jsonEquals(Object a, Object b) {
		if (a == b)
			return true;
		if (a == null || b == null)
			return false;
		if (a instanceof Number a2 && b instanceof Number b2)
			return toBigDecimal(a2).compareTo(toBigDecimal(b2)) == 0;
		if (a instanceof Number a2 && b instanceof CharSequence b2) {
			var bd = tryAsBigDecimal(b2.toString());
			return nn(bd) && toBigDecimal(a2).compareTo(bd) == 0;
		}
		if (b instanceof Number b2 && a instanceof CharSequence a2) {
			var bd = tryAsBigDecimal(a2.toString());
			return nn(bd) && toBigDecimal(b2).compareTo(bd) == 0;
		}
		if (a instanceof CharSequence a2 && b instanceof CharSequence b2)
			return a2.toString().equals(b2.toString());
		if (a.equals(b))
			return true;
		try {
			return Json.of(a).equals(Json.of(b));
		} catch (@SuppressWarnings("unused") Exception e) {
			return false;
		}
	}

	private static BigDecimal tryAsBigDecimal(String s) {
		try {
			return new BigDecimal(s);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}
}
