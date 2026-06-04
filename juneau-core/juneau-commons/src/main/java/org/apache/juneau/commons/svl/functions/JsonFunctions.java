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
package org.apache.juneau.commons.svl.functions;

import java.util.*;

import org.apache.juneau.commons.svl.*;

/**
 * JSON-navigation functions for the {@code #{...}} script catalog.
 *
 * <p>
 * All functions accept JSON strings as input and parse on each call via {@link MiniJson}. This
 * is intentionally a per-resolve cost — for repeated lookups against the same JSON document the
 * caller can pre-compile the surrounding template ({@link VarResolver#compile(String)}) so the
 * tokenization is amortized; the JSON parse itself is not yet cached and is a known follow-on
 * optimization opportunity.
 */
public final class JsonFunctions {

	private JsonFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"unchecked", // Cast is safe: type verified by caller context.
		"java:S2386" // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API, so visibility cannot be reduced.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		JsonPath.class, Get.class, Keys.class, Values.class, Size.class
	};

	/**
	 * {@code #{jsonPath(json, path)}} / {@code #{jsonPath(json, path, default)}} — extracts a
	 * value via a slash-rooted path (e.g. {@code "/data/0/name"}).
	 *
	 * <p>
	 * Returns the matched value as a String (objects/arrays serialize back to JSON). Missing
	 * path returns {@code ""} for the two-arg form, or {@code default} for the three-arg form.
	 */
	public static class JsonPath extends TypedFunction {
		@Override public String name() { return "jsonPath"; }
		public String invoke(String json, String path) { return invoke(json, path, ""); }
		public String invoke(String json, String path, String defaultValue) {
			var root = MiniJson.parse(json);
			var v = navigate(root, path);
			if (v == null) return defaultValue == null ? "" : defaultValue;
			return MiniJson.render(v);
		}

		@SuppressWarnings({
			"unchecked",  // Cast is safe: type verified by caller context.
			"java:S3776"  // Cognitive complexity acceptable for JSON path navigation dispatch
		})
		private static Object navigate(Object root, String path) {
			if (root == null || path == null) return null;
			var p = path.startsWith("/") ? path.substring(1) : path;
			if (p.isEmpty()) return root;
			var current = root;
			for (var seg : p.split("/")) {
				if (current == null) return null;
				if (current instanceof Map<?, ?> m) {
					current = ((Map<String, Object>) m).get(seg);
				} else if (current instanceof List<?> l) {
					int idx;
					try { idx = Integer.parseInt(seg); } catch (@SuppressWarnings("unused") NumberFormatException e) { return null; }
					if (idx < 0 || idx >= l.size()) return null;
					current = l.get(idx);
				} else {
					return null;
				}
			}
			return current;
		}
	}

	/**
	 * {@code #{get(json, key)}} — extracts {@code json[key]} (object) or {@code json[index]}
	 * (array, when {@code key} parses as int).
	 */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static class Get extends TypedFunction {
		@Override public String name() { return "get"; }
		public String invoke(String json, String key) {
			var v = MiniJson.parse(json);
			if (v == null || key == null) return "";
			if (v instanceof Map<?, ?> m) {
				var x = ((Map<String, Object>) m).get(key);
				return x == null ? "" : MiniJson.render(x);
			}
			if (v instanceof List<?> l) {
				int idx;
				try { idx = Integer.parseInt(key); } catch (@SuppressWarnings("unused") NumberFormatException e) { return ""; }
				if (idx < 0 || idx >= l.size()) return "";
				var x = l.get(idx);
				return x == null ? "" : MiniJson.render(x);
			}
			return "";
		}
	}

	/** {@code #{keys(json)}} — returns a JSON-array-shortcut string of an object's keys. */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static class Keys extends TypedFunction {
		@Override public String name() { return "keys"; }
		public String invoke(String json) {
			var v = MiniJson.parse(json);
			if (!(v instanceof Map<?, ?> m)) return "[]";
			return JsonShortcut.encodeArray(new ArrayList<>(((Map<String, Object>) m).keySet()));
		}
	}

	/** {@code #{values(json)}} — returns a JSON-array-shortcut string of an object's values (or array elements). */
	@SuppressWarnings({
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static class Values extends TypedFunction {
		@Override public String name() { return "values"; }
		public String invoke(String json) {
			var v = MiniJson.parse(json);
			if (v instanceof Map<?, ?> m) {
				var out = new ArrayList<String>();
				for (var x : ((Map<String, Object>) m).values()) out.add(MiniJson.render(x));
				return JsonShortcut.encodeArray(out);
			}
			if (v instanceof List<?> l) {
				var out = new ArrayList<String>();
				for (var x : l) out.add(MiniJson.render(x));
				return JsonShortcut.encodeArray(out);
			}
			return "[]";
		}
	}

	/**
	 * {@code #{size(json)}} — returns the count: object key count, array length, string
	 * length, or {@code 0} for null/primitives.
	 */
	public static class Size extends TypedFunction {
		@Override public String name() { return "size"; }
		public String invoke(String json) {
			var v = MiniJson.parse(json);
			if (v == null) return "0";
			if (v instanceof Map<?, ?> m) return String.valueOf(m.size());
			if (v instanceof List<?> l) return String.valueOf(l.size());
			if (v instanceof String s) return String.valueOf(s.length());
			return "0";
		}
	}
}
