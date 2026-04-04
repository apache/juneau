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
package org.apache.juneau.ini;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.json5.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.Iso8601Utils;

/**
 * Session for serializing objects to INI format.
 */
@SuppressWarnings({
	"resource", // IniWriter lifecycle managed by SerializerPipe
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"java:S3776", // Cognitive complexity acceptable for serialization logic
	"java:S6541"  // Acceptable for session implementation
})
public class IniSerializerSession extends WriterSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for INI serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder {

		private IniSerializer ctx;

		protected Builder(IniSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public IniSerializerSession build() {
			return new IniSerializerSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The serializer context.
	 * @return The builder.
	 */
	public static Builder create(IniSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final IniSerializer ctx;

	protected IniSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (o == null)
			return;
		var aType = getClassMetaForObject(o);
		if (!aType.isBean() && !aType.isMap())
			throw new SerializeException(this, "INI format requires a bean or Map<String,?> at root. Got: {0}", aType.inner().getName());
		var w = getIniWriter(out);
		var eType = getExpectedRootType(o);
		if (isAddBeanTypes() || isAddRootType()) {
			var typeName = getBeanTypeName(this, eType, aType, null);
			if (typeName == null)
				typeName = aType.inner().getSimpleName();
			var typeKey = getBeanTypePropertyName(eType);
			w.keyValue(typeKey, typeName);
		}
		if (aType.isBean())
			serializeBean(w, toBeanMap(o), "");
		else
			serializeMapAtRoot(w, (Map<?,?>)o, eType);
	}

	protected final IniWriter getIniWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof IniWriter w)
			return w;
		var w = new IniWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(),
			ctx.kvSeparator, ctx.spacedSeparator, getUriResolver());
		out.setWriter(w);
		return w;
	}

	private void serializeBean(IniWriter w, BeanMap<?> m, String sectionPath) throws IOException, SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		var simple = new ArrayList<Map.Entry<BeanPropertyMeta, Object>>();
		var sections = new ArrayList<Map.Entry<BeanPropertyMeta, Object>>();

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (canIgnoreValue(pMeta.getClassMeta(), key, value))
				return;
			var cMeta = pMeta.getClassMeta();
			var aType = value == null ? cMeta : getClassMetaForObject(value, cMeta);
			var iniMeta = ctx.getIniBeanPropertyMeta(pMeta);
			// Collections/arrays are written as inline key-value (not sections), so they must appear in
			// the default section before any [section] headers for correct parsing
			if (iniMeta.isJson5Encoding() || isSimpleOrJson5Inline(aType, value)
				|| aType.isCollectionOrArrayOrOptional()) {
				simple.add(new AbstractMap.SimpleEntry<>(pMeta, value));
			} else {
				sections.add(new AbstractMap.SimpleEntry<>(pMeta, value));
			}
		});

		// Pass 1: simple properties and JSON5-inline values
		for (var e : simple) {
			BeanPropertyMeta pMeta = e.getKey();
			if (ctx.useComments) {
				var iniMeta = ctx.getIniBeanPropertyMeta(pMeta);
				if (ne(iniMeta.getComment()))
					w.comment(iniMeta.getComment());
			}
			writeKeyValue(w, pMeta.getName(), e.getValue(), pMeta);
		}

		// Pass 2: bean and Map sections
		for (var e : sections) {
			BeanPropertyMeta pMeta = e.getKey();
			Object value = e.getValue();
			var iniMeta = ctx.getIniBeanPropertyMeta(pMeta);
			var key = ne(iniMeta.getSection()) ? iniMeta.getSection() : pMeta.getName();
			var cMeta = pMeta.getClassMeta();
			var newPath = sectionPath.isEmpty() ? key : sectionPath + "/" + key;

			if (nn(value)) {
				if (isUseWhitespace() && !sectionPath.isEmpty())
					w.blankLine();
				var aType = getClassMetaForObject(value, cMeta);
				if (aType.isBean()) {
					w.section(newPath);
					serializeBean(w, toBeanMap(value), newPath);
				} else if (aType.isMap()) {
					var map = (Map<?,?>)value;
					if (isSimpleMap(map, aType)) {
						w.section(newPath);
						serializeMapSection(w, map, aType);
					} else {
						if (ctx.useComments && ne(iniMeta.getComment()))
							w.comment(iniMeta.getComment());
						writeKeyValue(w, pMeta.getName(), value, pMeta);
					}
				} else if (aType.isCollection() || aType.isArray()) {
					if (ctx.useComments && ne(iniMeta.getComment()))
						w.comment(iniMeta.getComment());
					writeKeyValue(w, pMeta.getName(), value, pMeta);
				}
			} else if (isKeepNullProperties()) {
				if (ctx.useComments && ne(iniMeta.getComment()))
					w.comment(iniMeta.getComment());
				writeKeyValue(w, pMeta.getName(), null, pMeta);
			}
		}
	}

	private void serializeMapAtRoot(IniWriter w, Map<?,?> map, ClassMeta<?> type) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		forEachEntry(map, e -> {
			var k = toString(e.getKey());
			var v = e.getValue();
			if (!checkNull.test(v))
				return;
			try {
				var aType = getClassMetaForObject(v, type);
				if (aType.isBean()) {
					if (isUseWhitespace())
						w.blankLine();
					w.section(k);
					serializeBean(w, toBeanMap(v), k);
				} else if (aType.isMap() && v != null) {
					var nested = (Map<?,?>)v;
					if (isSimpleMap(nested, aType)) {
						if (isUseWhitespace())
							w.blankLine();
						w.section(k);
						serializeMapSection(w, nested, aType);
					} else {
						writeKeyValue(w, k, v, null);
					}
				} else {
					writeKeyValue(w, k, v, null);
				}
			} catch (SerializeException | IOException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@SuppressWarnings({
		"unused",    // type reserved for future type-aware section serialization
		"java:S1172" // Same as above
	})
	private void serializeMapSection(IniWriter w, Map<?,?> map, ClassMeta<?> type) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		forEachEntry(map, e -> {
			var k = toString(e.getKey());
			var v = e.getValue();
			if (!checkNull.test(v))
				return;
			try {
				writeKeyValue(w, k, v, null);
			} catch (SerializeException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	private void writeKeyValue(IniWriter w, String key, Object value, BeanPropertyMeta pMeta) throws SerializeException {
		var cMeta = pMeta != null ? pMeta.getClassMeta() : (value != null ? getClassMetaForObject(value) : object());
		var aType = value == null ? cMeta : getClassMetaForObject(value, cMeta);

		var swap = aType.getSwap(this);
		if (nn(swap)) {
			value = swap(swap, value);
			aType = swap.getSwapClassMeta(this);
			if (aType.isObject())
				aType = getClassMetaForObject(value);
		}

		if (value == null && !isKeepNullProperties())
			return;

		var valueStr = value == null ? "null"
			: isSimpleOrJson5Inline(aType, value) ? formatSimpleValue(value, aType)
			: encodeComplexValue(value);
		w.keyValue(key, valueStr);
	}

	private String formatSimpleValue(Object value, ClassMeta<?> aType) throws SerializeException {
		if (aType.isNumber())
			return value.toString();
		if (aType.isBoolean())
			return ((Boolean)value).toString();
		if (aType.isDateOrCalendarOrTemporal())
			return Iso8601Utils.format(value, aType, getTimeZone());
		if (aType.isDuration())
			return value.toString();
		if (aType.isEnum())
			return ((Enum<?>)value).name();
		if (aType.isCharSequence() || aType.isUri()) {
			var s = toString(value);
			return needsQuoting(s) ? "'" + s.replace("'", "''") + "'" : s;
		}
		return toString(value);
	}

	private static boolean needsQuoting(String s) {
		if (s == null || s.isEmpty())
			return true;
		if (s.equals("null") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))
			return true;
		if (s.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?"))
			return true;
		if (s.contains("=") || s.contains("[") || s.contains("]") || s.contains("#") || s.contains("\n"))
			return true;
		var trimmed = s.trim();
		if (!trimmed.equals(s))
			return true;
		return false;
	}

	private String encodeComplexValue(Object value) throws SerializeException {
		var json5 = getJson5Serializer().serialize(value);
		return json5;
	}

	private JsonSerializer getJson5Serializer() {
		var b = Json5Serializer.create().beanContext((BeanContext)getContext());
		if (isAddBeanTypes())
			b.addBeanTypes();
		if (isAddRootType())
			b.addRootType();
		return b.build();
	}

	@SuppressWarnings({
		"unused",    // value accepted for future value-aware inline detection
		"java:S1172" // Same as above
	})
	private static boolean isSimpleOrJson5Inline(ClassMeta<?> aType, Object value) {
		if (aType.isBean() || aType.isMap())
			return false;
		if (aType.isCollection() || aType.isArray())
			return false;
		if (aType.isStreamable())
			return false;
		return true;
	}

	private boolean isSimpleMap(Map<?,?> map, ClassMeta<?> mapType) {
		if (map == null)
			return false;
		var keyType = mapType.getKeyType();
		// When keyType is null or Object, check keys at runtime; otherwise require CharSequence
		if (keyType != null && !keyType.isCharSequence() && !keyType.isObject())
			return false;
		for (Object k : map.keySet()) {
			if (k != null && !(k instanceof CharSequence))
				return false;
		}
		for (Object v : map.values()) {
			if (v != null) {
				ClassMeta<?> vType = getClassMetaForObject(v);
				if (vType.isBean() || vType.isMap() || vType.isCollectionOrArray())
					return false;
			}
		}
		return true;
	}
}
