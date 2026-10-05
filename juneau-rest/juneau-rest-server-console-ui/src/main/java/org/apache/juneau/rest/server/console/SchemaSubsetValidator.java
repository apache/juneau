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
package org.apache.juneau.rest.server.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

/**
 * In-house JSON Schema 2020-12 validator for the keyword subset {@code juneau-page.schema.json} uses (C1-D2).
 *
 * <p>
 * Instances are parsed JSON: {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean}, or
 * {@code null}. Only local {@code $ref}s of the form {@code #/$defs/name} are supported.
 */
@SuppressWarnings({
	"java:S1192", // Schema keyword names read more clearly inline than as constants.
	"unchecked" // The schema and value nodes are untyped JSON trees (Object); each keyword handler casts to Map<String,Object>/List<Object> after the schema keyword implies that shape
})
final class SchemaSubsetValidator {

	static final Set<String> KEYWORDS = Set.of("$schema", "$id", "$defs", "$ref", "title", "description", "default",
		"type", "const", "enum", "required", "properties", "additionalProperties", "propertyNames", "items", "pattern",
		"minLength", "maxLength", "minItems", "allOf", "anyOf", "oneOf", "not");

	private final Map<String,Object> root;
	private final Map<String,Pattern> patterns = new HashMap<>();

	SchemaSubsetValidator(Map<String,Object> root) {
		this.root = root;
	}

	List<String> validate(Object instance) {
		var errors = new ArrayList<String>();
		check(root, instance, "$", errors);
		return errors;
	}

	@SuppressWarnings({
		"java:S3776" // One keyword-by-keyword pass over the schema subset; splitting it would scatter the rules.
	})
	private void check(Object schemaNode, Object v, String path, List<String> errors) {
		if (schemaNode instanceof Boolean b) {
			if (! b.booleanValue())
				errors.add(path + ": not allowed");
			return;
		}
		var s = (Map<String,Object>)schemaNode;
		if (s.containsKey("$ref"))
			check(resolve((String)s.get("$ref")), v, path, errors);
		if (s.containsKey("type") && ! typeMatches((String)s.get("type"), v)) {
			errors.add(path + ": expected " + s.get("type"));
			return;
		}
		if (s.containsKey("const") && neq(s.get("const"), v))
			errors.add(path + ": must equal " + s.get("const"));
		if (s.containsKey("enum") && ! ((List<Object>)s.get("enum")).contains(v))
			errors.add(path + ": must be one of " + s.get("enum"));
		if (v instanceof String str) {
			if (s.containsKey("minLength") && str.codePointCount(0, str.length()) < num(s, "minLength"))
				errors.add(path + ": shorter than " + num(s, "minLength"));
			if (s.containsKey("maxLength") && str.codePointCount(0, str.length()) > num(s, "maxLength"))
				errors.add(path + ": longer than " + num(s, "maxLength"));
			if (s.containsKey("pattern") && ! pattern((String)s.get("pattern")).matcher(str).find())
				errors.add(path + ": does not match " + s.get("pattern"));
		}
		if (v instanceof List<?> list) {
			if (s.containsKey("minItems") && list.size() < num(s, "minItems"))
				errors.add(path + ": fewer than " + num(s, "minItems") + " items");
			if (s.containsKey("items"))
				for (var i = 0; i < list.size(); i++)
					check(s.get("items"), list.get(i), path + "[" + i + "]", errors);
		}
		if (v instanceof Map<?,?> m) {
			var obj = (Map<String,Object>)m;
			if (s.containsKey("required"))
				for (var r : (List<String>)s.get("required"))
					if (! obj.containsKey(r))
						errors.add(path + ": missing required '" + r + "'");
			var props = (Map<String,Object>)s.getOrDefault("properties", Map.of());
			for (var e : obj.entrySet()) {
				var childPath = path + "." + e.getKey();
				if (s.containsKey("propertyNames"))
					check(s.get("propertyNames"), e.getKey(), path + "{" + e.getKey() + "}", errors);
				if (props.containsKey(e.getKey()))
					check(props.get(e.getKey()), e.getValue(), childPath, errors);
				else if (s.containsKey("additionalProperties"))
					check(s.get("additionalProperties"), e.getValue(), childPath, errors);
			}
		}
		if (s.containsKey("allOf"))
			for (var sub : (List<Object>)s.get("allOf"))
				check(sub, v, path, errors);
		if (s.containsKey("anyOf")) {
			var ok = ((List<Object>)s.get("anyOf")).stream().anyMatch(sub -> passes(sub, v, path));
			if (! ok)
				errors.add(path + ": matches none of anyOf");
		}
		if (s.containsKey("oneOf")) {
			var n = ((List<Object>)s.get("oneOf")).stream().filter(sub -> passes(sub, v, path)).count();
			if (n != 1)
				errors.add(path + ": matches " + n + " of oneOf (expected exactly 1)");
		}
		if (s.containsKey("not") && passes(s.get("not"), v, path))
			errors.add(path + ": matches a forbidden shape (not)");
	}

	private boolean passes(Object sub, Object v, String path) {
		var tmp = new ArrayList<String>();
		check(sub, v, path, tmp);
		return tmp.isEmpty();
	}

	private Object resolve(String ref) {
		if (! ref.startsWith("#/$defs/"))
			throw new IllegalStateException("Unsupported $ref: " + ref);
		var defs = (Map<String,Object>)root.get("$defs");
		var target = defs.get(ref.substring("#/$defs/".length()));
		if (target == null)
			throw new IllegalStateException("Dangling $ref: " + ref);
		return target;
	}

	private Pattern pattern(String p) {
		return patterns.computeIfAbsent(p, Pattern::compile);
	}

	private static int num(Map<String,Object> s, String k) {
		return ((Number)s.get(k)).intValue();
	}

	private static boolean typeMatches(String type, Object v) {
		return switch (type) {
			case "object" -> v instanceof Map;
			case "array" -> v instanceof List;
			case "string" -> v instanceof String;
			case "boolean" -> v instanceof Boolean;
			case "integer" -> v instanceof Integer || v instanceof Long;
			case "number" -> v instanceof Number;
			case "null" -> v == null;
			default -> throw new IllegalStateException("Unsupported type: " + type);
		};
	}
}
