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
package org.apache.juneau.marshall.stream;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * Format-neutral POJO walker that re-emits an arbitrary object graph through a {@link TokenWriter}.
 *
 * <p>
 * Used by {@link TokenWriter#object(Object)} on every format that supports the cursor-level POJO
 * bridge: the walker introspects the value (null / scalar / array / collection / map / bean) and
 * recursively emits {@link TokenWriter#startObject() startObject} / {@link TokenWriter#fieldName fieldName}
 * / {@link TokenWriter#string string} / {@link TokenWriter#number(long) number} /
 * {@link TokenWriter#bool bool} / {@link TokenWriter#nil nil} / {@link TokenWriter#binary binary}
 * events on the calling writer.  No raw bytes are written by the walker; per-format encoding is
 * the {@link TokenWriter}'s job.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li>Object swaps and {@code @Schema} annotations <em>are</em> applied during the walk via
 * 		{@link MarshallingContext#toBeanMap(Object)} (e.g. {@code java.time} types are emitted as
 * 		ISO-8601 strings).  This is the explicit carve-out from the cursor's "purely structural"
 * 		rule for the {@link TokenWriter#object(Object) object} bridge.  The bean introspection
 * 		uses the <b>live session's</b> {@link MarshallingContext} (threaded through
 * 		{@link Options#context()}) so swaps / bean-filters / locale / timezone honor the same
 * 		per-invocation configuration as {@code serialize(...)}; it falls back to
 * 		{@link MarshallingContext#DEFAULT} only when no context is supplied.
 * 	<li>Self-referencing graphs are guarded by an identity-based ancestor check: re-entering an
 * 		object already on the current walk path throws {@link IllegalStateException} rather than
 * 		recursing until the stack overflows.  (Shared but acyclic references &mdash; the same node
 * 		reachable by two distinct paths &mdash; are permitted.)
 * </ul>
 */
@SuppressWarnings({
	"resource", // TokenWriter is a fluent Closeable returning itself; discarding the chained return value is intentional, and the writer is the caller's to close.
	"java:S115" // ARG_-prefixed arg-key constants use the project's UPPER_camelCase convention.
})
public final class PojoWalker {

	private static final String ARG_w = "w";
	private static final String ARG_options = "options";

	/**
	 * Databind-level settings honored by {@link PojoWalker}.
	 *
	 * <p>
	 * Built by the format's {@code writeTokens(...)} factory from the calling
	 * {@link org.apache.juneau.marshall.serializer.Serializer.Builder Serializer.Builder} and
	 * passed through to the walker.  These are the same settings the POJO databind path
	 * ({@code Serializer.write(...)}) consults; honoring them here means
	 * {@code writeTokens(out).object(o)} produces the same shape as
	 * {@code serializer.write(o)} for the same configuration.
	 *
	 * @param keepNullProperties Whether bean fields with null values are emitted (true) or
	 * 	skipped (false).
	 * @param trimEmptyMaps Whether bean / map fields whose value is an empty map are skipped.
	 * @param trimEmptyCollections Whether bean / map fields whose value is an empty collection /
	 * 	array are skipped.
	 * @param sortMaps Whether map / bean property iteration order is sorted by key.
	 * @param sortCollections Whether collection element iteration order is sorted (a sorted copy
	 * 	is walked).
	 * @param trimStrings Whether emitted scalar strings are trimmed.
	 * @param context The live session's {@link MarshallingContext} used for bean introspection
	 * 	(swaps / bean-filters / locale / timezone).  May be <jk>null</jk>, in which case
	 * 	{@link MarshallingContext#DEFAULT} is used.
	 */
	public record Options(
			boolean keepNullProperties,
			boolean trimEmptyMaps,
			boolean trimEmptyCollections,
			boolean sortMaps,
			boolean sortCollections,
			boolean trimStrings,
			MarshallingContext context) {

		/**
		 * Default options: emit nulls, no trimming, no sorting, no bound context (falls back to
		 * {@link MarshallingContext#DEFAULT}).  Mirrors
		 * {@link org.apache.juneau.marshall.serializer.Serializer.Builder Serializer.Builder}'s
		 * defaults.
		 */
		public static final Options DEFAULT = new Options(true, false, false, false, false, false, null);

		/** Returns the bound context, or {@link MarshallingContext#DEFAULT} when none was supplied. */
		MarshallingContext contextOrDefault() {
			return or(context, MarshallingContext.DEFAULT);
		}
	}

	private PojoWalker() {
		// Static-method facade.
	}

	/**
	 * Walks the given value and emits its structure through the given writer using
	 * {@link Options#DEFAULT default options}.
	 *
	 * @param w The token writer to emit events to.  Must not be <jk>null</jk>.
	 * @param value The value to walk.  May be <jk>null</jk>.
	 * @throws IOException If the writer's underlying stream fails.
	 */
	public static void walk(TokenWriter w, Object value) throws IOException {
		walk(w, value, Options.DEFAULT);
	}

	/**
	 * Walks the given value and emits its structure through the given writer using the supplied
	 * options.
	 *
	 * @param w The token writer to emit events to.  Must not be <jk>null</jk>.
	 * @param value The value to walk.  May be <jk>null</jk>.
	 * @param options The walker's databind-level options.  Must not be <jk>null</jk>.
	 * @throws IOException If the writer's underlying stream fails.
	 */
	public static void walk(TokenWriter w, Object value, Options options) throws IOException {
		assertArgNotNull(ARG_w, w);
		assertArgNotNull(ARG_options, options);
		// Identity-based ancestor set: tracks the objects on the current walk path so a
		// self-referencing graph throws instead of overflowing the stack.  Shared-but-acyclic
		// references are fine because each node is removed from the set once its subtree completes.
		walkValue(w, value, options, Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	private static void walkValue(TokenWriter w, Object value, Options options, Set<Object> ancestors) throws IOException {
		if (value == null) {
			w.nil();
			return;
		}
		if (value instanceof CharSequence value2) {
			var s = value2.toString();
			if (options.trimStrings)
				s = s.trim();
			w.string(s);
			return;
		}
		if (value instanceof Boolean value2) {
			w.bool(value2);
			return;
		}
		if (value instanceof byte[] value2) {
			w.binary(value2);
			return;
		}
		if (value instanceof Number value2) {
			writeNumber(w, value2);
			return;
		}
		if (value instanceof Character value2) {
			w.string(value2.toString());
			return;
		}
		if (value instanceof Map<?,?> value2) {
			enterAncestor(value, ancestors);
			try {
				walkMap(w, value2, options, ancestors);
			} finally {
				ancestors.remove(value);
			}
			return;
		}
		if (value instanceof Collection<?> value2) {
			enterAncestor(value, ancestors);
			try {
				walkCollection(w, value2, options, ancestors);
			} finally {
				ancestors.remove(value);
			}
			return;
		}
		var cls = value.getClass();
		if (cls.isArray()) {
			enterAncestor(value, ancestors);
			try {
				walkArray(w, value, options, ancestors);
			} finally {
				ancestors.remove(value);
			}
			return;
		}
		if (value instanceof Iterable<?> value2) {
			enterAncestor(value, ancestors);
			try {
				walkIterable(w, value2, options, ancestors);
			} finally {
				ancestors.remove(value);
			}
			return;
		}
		if (value instanceof Enum<?> value2) {
			w.string(value2.name());
			return;
		}
		// Inspect the type via the live session's MarshallingContext to decide whether to walk it
		// as a bean or to fall through to its swapped scalar representation.  Known non-bean types
		// (Date, Calendar, Temporal, Duration, Period, URI/URL, UUID, Class, etc.) cannot be walked
		// via toBeanMap; emit them as their string form, which is the same form the databind path
		// produces via DefaultSwaps.
		var mc = options.contextOrDefault();
		var cm = mc.getClassMeta(value.getClass());
		if (cm.isBean()) {
			enterAncestor(value, ancestors);
			try {
				walkMap(w, mc.toBeanMap(value), options, ancestors);
			} finally {
				ancestors.remove(value);
			}
			return;
		}
		// Non-bean fallback: emit as string.  For java.time / Date / UUID / Class etc. the
		// toString() form is the canonical ISO-8601 / hex / FQN representation.
		w.string(value.toString());
	}

	/** Adds the value to the ancestor set, throwing if it is already on the current walk path. */
	private static void enterAncestor(Object value, Set<Object> ancestors) {
		if (!ancestors.add(value))
			throw isex("Recursion detected while walking a self-referencing object graph.  Offending type: %s", value.getClass().getName());
	}

	private static void writeNumber(TokenWriter w, Number n) throws IOException {
		if (n instanceof Long n2) { w.number(n2.longValue()); return; }
		if (n instanceof Integer n2) { w.number(n2.longValue()); return; }
		if (n instanceof Short n2) { w.number(n2.longValue()); return; }
		if (n instanceof Byte n2) { w.number(n2.longValue()); return; }
		if (n instanceof Double n2) { w.number(n2.doubleValue()); return; }
		if (n instanceof Float n2) { w.number(n2.doubleValue()); return; }
		if (n instanceof BigDecimal n2) { w.number(n2); return; }
		if (n instanceof BigInteger n2) { w.number(n2); return; }
		w.number(n);
	}

	private static void walkMap(TokenWriter w, Map<?,?> m, Options options, Set<Object> ancestors) throws IOException {
		w.startObject();
		Iterable<? extends Map.Entry<?,?>> entries = m.entrySet();
		if (options.sortMaps && !m.isEmpty())
			entries = sortedEntries(m);
		for (var entry : entries) {
			var key = entry.getKey();
			var value = entry.getValue();
			if (skipMapEntry(value, options))
				continue;
			w.fieldName(key == null ? "null" : key.toString());
			walkValue(w, value, options, ancestors);
		}
		w.endObject();
	}

	private static List<Map.Entry<Object,Object>> sortedEntries(Map<?,?> m) {
		var list = new ArrayList<Map.Entry<Object,Object>>(m.size());
		for (var entry : m.entrySet())
			list.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
		list.sort(Comparator.comparing(e -> {
			var k = e.getKey();
			return k == null ? "" : k.toString();
		}));
		return list;
	}

	private static boolean skipMapEntry(Object value, Options options) {
		if (value == null)
			return !options.keepNullProperties;
		if (options.trimEmptyMaps && value instanceof Map<?,?> value2 && value2.isEmpty())
			return true;
		if (options.trimEmptyCollections) {
			if (value instanceof Collection<?> value2 && value2.isEmpty())
				return true;
			if (value.getClass().isArray() && Array.getLength(value) == 0)
				return true;
		}
		return false;
	}

	private static void walkCollection(TokenWriter w, Collection<?> c, Options options, Set<Object> ancestors) throws IOException {
		w.startArray();
		Iterable<?> items = c;
		if (options.sortCollections && !c.isEmpty()) {
			var copy = new ArrayList<Object>(c);
			copy.sort(Comparator.comparing(o -> o == null ? "" : o.toString()));
			items = copy;
		}
		for (var item : items)
			walkValue(w, item, options, ancestors);
		w.endArray();
	}

	private static void walkIterable(TokenWriter w, Iterable<?> it, Options options, Set<Object> ancestors) throws IOException {
		w.startArray();
		for (var item : it)
			walkValue(w, item, options, ancestors);
		w.endArray();
	}

	private static void walkArray(TokenWriter w, Object array, Options options, Set<Object> ancestors) throws IOException {
		w.startArray();
		var len = Array.getLength(array);
		if (options.sortCollections && len > 0) {
			var copy = new ArrayList<Object>(len);
			for (var i = 0; i < len; i++)
				copy.add(Array.get(array, i));
			copy.sort(Comparator.comparing(o -> o == null ? "" : o.toString()));
			for (var item : copy)
				walkValue(w, item, options, ancestors);
		} else {
			for (var i = 0; i < len; i++)
				walkValue(w, Array.get(array, i), options, ancestors);
		}
		w.endArray();
	}
}
