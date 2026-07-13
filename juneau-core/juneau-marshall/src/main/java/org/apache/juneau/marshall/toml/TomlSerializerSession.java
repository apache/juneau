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
package org.apache.juneau.marshall.toml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session for serializing objects to TOML format.
 */
@SuppressWarnings({
	"resource",   // TomlWriter lifecycle managed by SerializerPipe
	"rawtypes",   // Raw types necessary for generic type handling
	"java:S110",  // Inheritance depth acceptable for serializer session hierarchy
	"java:S115",  // Constants use naming conventions that embed type info or config keys (e.g. PROP_trimWhitespace)
	"java:S3776", // Cognitive complexity acceptable for serialization logic
	"java:S6541"  // Acceptable for session implementation
})
public class TomlSerializerSession extends WriterSerializerSession implements RecordWritable {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for TOML serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

		private TomlSerializer ctx;

		protected Builder(TomlSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public TomlSerializerSession build() {
			return new TomlSerializerSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The serializer context.
	 * @return The builder.
	 */
	public static Builder create(TomlSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final TomlSerializer ctx;

	protected TomlSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override /* RecordWritable */
	public RecordWriter serializeRecords(Object output) throws IOException {
		return RecordAdapter.writer(this, output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (o == null)
			return;
		TomlWriter w = getTomlWriter(out);
		serializeRoot(w, o);
	}

	protected final TomlWriter getTomlWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof TomlWriter w)
			return w;
		var w = new TomlWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	private void serializeRoot(TomlWriter w, Object o) throws SerializeException {
		ClassMeta<?> eType = getExpectedRootType(o);
		if (eType.isBean()) {
			serializeBean(w, toBeanMap(o), "");
		} else if (eType.isMap()) {
			serializeMapAtRoot(w, (Map<?,?>)o, eType);
		} else if (eType.isCollection() || eType.isArray()) {
			Collection<?> c = eType.isArray() ? toList(eType.inner(), o) : (Collection<?>)o;
			ClassMeta<?> elType = eType.getElementType();
			if (elType.isBean() || elType.isMap()) {
				// Array of tables at root - use "item" as table name
				for (Object item : c) {
					w.blankLine();
					w.arrayOfTablesHeader("item");
					if (elType.isBean())
						serializeBean(w, toBeanMap(item), "item");
					else
						serializeMapAsTable(w, "item", (Map)item, elType);
				}
			} else {
				// Root array of simples: wrap in _value
				w.bareKey("_value");
				w.w(" = ");
				serializeArray(w, c, eType);
				w.w('\n');
			}
		} else {
			// Root primitive: wrap in _value
			w.bareKey("_value");
			w.w(" = ");
			serializeValue(w, o, eType);
			w.w('\n');
		}
	}

	private void serializeMapAtRoot(TomlWriter w, Map<?,?> map, ClassMeta<?> type) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		forEachEntry(map, e -> {
			String k = e.getKey() == null ? "null" : toString(e.getKey());
			Object v = e.getValue();
			if (!checkNull.test(v))
				return;
			ClassMeta<?> aType = getClassMetaForObject(v, type);
			if (aType.isMap() && v instanceof Map<?,?> nested) {
				w.blankLine();
				w.tableHeader(k);
				serializeMapAsTable(w, k, nested, aType);
			} else {
				writeKeyValue(w, k, v, null);
			}
		});
	}

	private void serializeBean(TomlWriter w, BeanMap<?> m, String tablePath) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		List<Map.Entry<BeanPropertyMeta, Object>> simple = new ArrayList<>();
		List<Map.Entry<BeanPropertyMeta, Object>> complex = new ArrayList<>();

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			ClassMeta<?> cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			if (canIgnoreValue(pMeta, key, value))
				return;
			ClassMeta<?> aType = value == null ? cMeta : getClassMetaForObject(value, cMeta);
			if (isSimpleOrInlineTable(aType, value, pMeta)) {
				simple.add(new AbstractMap.SimpleEntry<>(pMeta, value));
			} else {
				complex.add(new AbstractMap.SimpleEntry<>(pMeta, value));
			}
		});

		if (ctx.sortKeys) {
			simple.sort(Comparator.comparing(e -> e.getKey().getName()));
			complex.sort(Comparator.comparing(e -> e.getKey().getName()));
		}

		// Pass 1: simple values and inline tables
		for (var e : simple) {
			String key = e.getKey().getName();
			Object value = e.getValue();
			BeanPropertyMeta pMeta = e.getKey();
			writeKeyValue(w, key, value, pMeta);
		}

		// Pass 2: tables and array of tables
		for (var e : complex) {
			String key = e.getKey().getName();
			Object value = e.getValue();
			BeanPropertyMeta pMeta = e.getKey();
			ClassMeta<?> cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			String newPath = tablePath.isEmpty() ? key : tablePath + "." + key;

			if (nn(value)) {
				w.blankLine();
				ClassMeta<?> aType = getClassMetaForObject(value, cMeta);
				if (aType.isBean()) {
					w.tableHeader(newPath);
					serializeBean(w, toBeanMap(value), newPath);
				} else if (aType.isMap()) {
					w.tableHeader(newPath);
					serializeMapAsTable(w, newPath, (Map)value, cMeta);
				} else if (aType.isCollection() || aType.isArray()) {
					Collection<?> col = aType.isArray() ? toList(aType.inner(), value) : (Collection<?>)value;
					ClassMeta<?> elType = aType.getElementType();
					if (elType.isBean() || elType.isMap()) {
						for (Object item : col) {
							w.blankLine();
							w.arrayOfTablesHeader(newPath);
							if (elType.isBean())
								serializeBean(w, toBeanMap(item), newPath);
							else
								serializeMapAsTable(w, newPath, (Map)item, elType);
						}
					} else {
						writeKeyValue(w, key, value, pMeta);
					}
				}
			}
		}
	}

	private void writeKeyValue(TomlWriter w, String key, Object value, BeanPropertyMeta pMeta) throws SerializeException {
		ClassMeta<?> cMeta;
		if (pMeta != null)
			cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
		else if (value != null)
			cMeta = getClassMetaForObject(value);
		else
			cMeta = object();
		ClassMeta<?> aType = value == null ? cMeta : getClassMetaForObject(value, cMeta);

		// Apply swap
		var swap = aType.getSwap(this);
		if (nn(swap)) {
			value = swap(swap, value);
			aType = swap.getSwapClassMeta(this);
			if (aType.isObject())
				aType = getClassMetaForObject(value);
		}

		if (value == null && !isKeepNullProperties())
			return;

		writeKey(w, key);
		w.w(" = ");
		if (value == null)
			w.stringValue(ctx.getNullValue());
		else
			writeValue(w, value, aType, pMeta);
		w.w('\n');
	}

	private static void writeKey(TomlWriter w, String key) {
		if (w.isBareKey(key))
			w.bareKey(key);
		else
			w.quotedKey(key);
	}

	@SuppressWarnings({
		"unused",       // pMeta accepted for API consistency; may be used by future format-specific logic
		"java:S1172"    // Same as above
	})
	private void writeValue(TomlWriter w, Object value, ClassMeta<?> aType, BeanPropertyMeta pMeta) throws SerializeException {
		if (value == null) {
			w.stringValue(ctx.getNullValue());
			return;
		}
		// Resolve to the runtime type and apply any registered swap.  Required for collection/array
		// elements that arrive here without having gone through writeKeyValue's swap step (e.g.
		// List<Locale> elements where the bean-property swap is on the List, not on Locale).
		// Bug #8: without this, Locale list-elements wire as Locale.toString() ("en_US") regardless of
		// the configured LocaleFormat, breaking BCP_47 round-trips on the parser side.
		var rType = getClassMetaForObject(value, aType);
		var swap = rType.getSwap(this);
		if (nn(swap)) {
			value = swap(swap, value);
			rType = swap.getSwapClassMeta(this);
			if (rType.isObject())
				rType = getClassMetaForObject(value);
			aType = rType;
		}
		if (value == null) {
			w.stringValue(ctx.getNullValue());
			return;
		}
		if (aType.isBean()) {
			BeanMap<?> bm = toBeanMap(value);
			if (shouldUseInlineTable(bm)) {
				w.inlineTableStart();
				boolean[] first = {true};
				Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
				bm.forEachValue(checkNull, (pm, k, v, th) -> {
					if (!first[0]) w.w(", ");
					first[0] = false;
					writeKey(w, k);
					w.w(" = ");
				writeValue(w, v, (ClassMeta<?>) pm.getBeanInfo(), pm);
				});
				w.inlineTableEnd();
			} else {
				serializeBean(w, bm, "");
			}
		} else if (aType.isNumber()) {
			if (value instanceof Float || value instanceof Double)
				w.floatValue(((Number)value).doubleValue());
			else
				w.integerValue(((Number)value).longValue());
		} else if (aType.isBoolean()) {
			w.booleanValue((Boolean)value);
		} else if (aType.isEnum()) {
			w.stringValue(((Enum<?>)value).name());
		} else if (aType.isDate()) {
			w.w(serializeDate((Date)value, aType));
		} else if (aType.isCalendar()) {
			w.w(serializeCalendar(value, aType));
		} else if (aType.isTemporal()) {
			Class<?> inner = aType.inner();
			if (inner == Year.class || inner == YearMonth.class)
				w.stringValue(serializeTemporal((TemporalAccessor)value, aType));
			else
				w.w(serializeTemporal((TemporalAccessor)value, aType));
		} else if (aType.isDuration()) {
			w.stringValue(serializeDuration((Duration)value));
		} else if (aType.isPeriod()) {
			w.stringValue(serializePeriod((Period)value));
		} else if (value.getClass().isArray()) {
			// Bug #11/#12 a04 residual (generalized): when aType arrives erased — most commonly
			// because the value is a typed List<T[]> bean-property element and the parent List's
			// elementType resolved to Object (getClassMetaForObject(value, cMeta) ignores the
			// cMeta hint when value != null and returns the raw runtime ArrayList ClassMeta) —
			// the isCollection/isArray dispatch below doesn't fire and the value would otherwise
			// stringify via Object.toString() ("[B@<hash>", "[I@<hash>", …).  Detect the array
			// shape from the runtime class and emit a TOML inline-array, recursing on each
			// (boxed) element through writeValue so the per-element wire form (integer / float
			// / bool / string) follows the standard dispatch.  Subsumes the byte[]-only fix that
			// originally landed for the BinaryFormat.NOT_SET round-trip and extends it to
			// int[]/long[]/short[]/float[]/double[]/boolean[]/char[]/String[]/Object[] (and any
			// nested array shape via the recursive writeValue step).  At configured (non-NOT_SET)
			// binary formats the per-property BinarySwap has already fired in writeKeyValue's
			// swap step, so byte[] values reach this branch only at NOT_SET — a no-op for the
			// configured-format paths.  java.lang.reflect.Array.get auto-boxes primitive
			// elements, so a single runtime-typed loop covers every primitive component type.
			int len = Array.getLength(value);
			w.arrayStart();
			for (int i = 0; i < len; i++) {
				if (i > 0) w.w(", ");
				Object el = Array.get(value, i);
				if (el == null)
					w.stringValue(ctx.getNullValue());
				else
					writeValue(w, el, getClassMetaForObject(el), null);
			}
			w.arrayEnd();
		} else if (aType.isCollection() || aType.isArray()) {
			Collection<?> c = aType.isArray() ? toList(aType.inner(), value) : (Collection<?>)value;
			ClassMeta<?> elType = aType.getElementType();
			if (elType.isBean() || elType.isMap()) {
				// Should have been handled in complex pass
				w.stringValue(value.toString());
			} else {
				w.arrayStart();
				boolean first = true;
				for (Object el : c) {
					if (!first) w.w(", ");
					first = false;
					if (el == null)
						w.stringValue(ctx.getNullValue());
					else
						writeValue(w, el, elType, null);
				}
				w.arrayEnd();
			}
		} else {
			w.stringValue(toString(value));
		}
	}

	private void serializeValue(TomlWriter w, Object value, ClassMeta<?> aType) throws SerializeException {
		writeValue(w, value, aType, null);
	}

	@SuppressWarnings({
		"unused",       // path and type reserved for future section-header and type-aware serialization
		"java:S1172"    // Same as above
	})
	private void serializeMapAsTable(TomlWriter w, String path, Map<?,?> map, ClassMeta<?> type) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		forEachEntry(map, e -> {
			String k = e.getKey() == null ? "null" : toString(e.getKey());
			Object v = e.getValue();
			if (!checkNull.test(v))
				return;
			writeKeyValue(w, k, v, null);
		});
	}

	private void serializeArray(TomlWriter w, Collection<?> c, ClassMeta<?> type) throws SerializeException {
		ClassMeta<?> elType = type.getElementType();
		w.arrayStart();
		boolean first = true;
		for (Object el : c) {
			if (!first) w.w(", ");
			first = false;
			serializeValue(w, el, elType);
		}
		w.arrayEnd();
	}

	@SuppressWarnings({
		"unused",       // pMeta accepted for API consistency with callers that track property context
		"java:S1172"    // Same as above
	})
	private boolean isSimpleOrInlineTable(ClassMeta<?> aType, Object value, BeanPropertyMeta pMeta) throws SerializeException {
		if (aType.isBean()) {
			BeanMap<?> bm = toBeanMap(value);
			return shouldUseInlineTable(bm);
		}
		return !aType.isMap() && !(aType.isCollectionOrArray() && aType.getElementType().isBean());
	}

	private boolean shouldUseInlineTable(BeanMap<?> bm) {
		if (!ctx.useInlineTables)
			return false;
		int count = 0;
		for (BeanPropertyMeta p : bm.getMeta().getProperties().values()) {
			if (!p.isReadOnly()) {
				ClassMeta<?> cm = (ClassMeta<?>) p.getBeanInfo();
				if (cm.isBean() || cm.isMap() || (cm.isCollectionOrArray() && cm.getElementType().isBean()))
					return false;
				count++;
				if (count > ctx.inlineTableThreshold)
					return false;
			}
		}
		return count <= ctx.inlineTableThreshold;
	}
}
