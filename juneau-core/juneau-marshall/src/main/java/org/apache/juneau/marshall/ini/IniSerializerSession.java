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
package org.apache.juneau.marshall.ini;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session for serializing objects to INI format.
 */
@SuppressWarnings({
	"resource", // IniWriter lifecycle managed by SerializerPipe
	"java:S110", // Inheritance depth acceptable for serializer session hierarchy
	"java:S115", // Constants use naming conventions that embed type info or config keys (e.g. PROP_trimWhitespace)
	"java:S3776", // Cognitive complexity acceptable for serialization logic
	"java:S6541"  // Acceptable for session implementation
})
public class IniSerializerSession extends WriterSerializerSession implements RecordWritable {

	private static final String ARG_ctx = "ctx";
	private static final String SECTION_PATH_DELIMITER = "/";

	/**
	 * Builder for INI serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

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
	 * 	<br>Cannot be <jk>null</jk>.
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

	@Override /* RecordWritable */
	public RecordWriter writeRecords(Object output) throws IOException {
		return RecordAdapter.writer(this, output);
	}

	@Override /* RecordWritable */
	public boolean isRecordStreaming() {
		return false;
	}

	@Override
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (o == null)
			return;
		var aType = getClassMetaForObject(o);
		if (!aType.isBean() && !aType.isMap())
			throw new SerializeException(this, "INI format requires a bean or Map<String,?> at root. Got: %s", aType.inner().getName());
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
			writeBean(w, toBeanMap(o), "");
		else
			writeMapAtRoot(w, (Map<?,?>)o, eType);
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

	private void writeBean(IniWriter w, BeanMap<?> m, String sectionPath) throws IOException, SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		var simple = new ArrayList<Map.Entry<BeanPropertyMeta, Object>>();
		var sections = new ArrayList<Map.Entry<BeanPropertyMeta, Object>>();

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			if (canIgnoreValue(pMeta, key, value))
				return;
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
				if (ine(iniMeta.getComment()))
					w.comment(iniMeta.getComment());
			}
			writeKeyValue(w, pMeta.getName(), e.getValue(), pMeta);
		}

		// Pass 2: bean and Map sections
		for (var e : sections) {
			BeanPropertyMeta pMeta = e.getKey();
			Object value = e.getValue();
			var iniMeta = ctx.getIniBeanPropertyMeta(pMeta);
			var key = ine(iniMeta.getSection()) ? iniMeta.getSection() : pMeta.getName();
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			var newPath = sectionPath.isEmpty() ? key : sectionPath + SECTION_PATH_DELIMITER + key;

			if (nn(value)) {
				if (isUseWhitespace() && !sectionPath.isEmpty())
					w.blankLine();
				var aType = getClassMetaForObject(value, cMeta);
				if (aType.isBean()) {
					w.section(newPath);
					writeBean(w, toBeanMap(value), newPath);
				} else if (aType.isMap()) {
					var map = (Map<?,?>)value;
					if (isSimpleMap(map, aType)) {
						w.section(newPath);
						writeMapSection(w, map, aType);
					} else {
						if (ctx.useComments && ine(iniMeta.getComment()))
							w.comment(iniMeta.getComment());
						writeKeyValue(w, pMeta.getName(), value, pMeta);
					}
				} else if (aType.isCollection() || aType.isArray()) {
					if (ctx.useComments && ine(iniMeta.getComment()))
						w.comment(iniMeta.getComment());
					writeKeyValue(w, pMeta.getName(), value, pMeta);
				}
			} else if (isKeepNullProperties()) {
				if (ctx.useComments && ine(iniMeta.getComment()))
					w.comment(iniMeta.getComment());
				writeKeyValue(w, pMeta.getName(), null, pMeta);
			}
		}
	}

	private void writeMapAtRoot(IniWriter w, Map<?,?> map, ClassMeta<?> type) throws SerializeException {
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
					writeBean(w, toBeanMap(v), k);
				} else if (aType.isMap() && v != null) {
					var nested = (Map<?,?>)v;
					if (isSimpleMap(nested, aType)) {
						if (isUseWhitespace())
							w.blankLine();
						w.section(k);
						writeMapSection(w, nested, aType);
					} else {
						writeKeyValue(w, k, v, null);
					}
				} else {
					writeKeyValue(w, k, v, null);
				}
			} catch (SerializeException | IOException ex) {
				throw rex(ex);
			}
		});
	}

	@SuppressWarnings({
		"unused",    // type reserved for future type-aware section serialization
		"java:S1172" // Same as above
	})
	private void writeMapSection(IniWriter w, Map<?,?> map, ClassMeta<?> type) throws SerializeException {
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		forEachEntry(map, e -> {
			var k = toString(e.getKey());
			var v = e.getValue();
			if (!checkNull.test(v))
				return;
			writeKeyValue(w, k, v, null);
		});
	}

	private void writeKeyValue(IniWriter w, String key, Object value, BeanPropertyMeta pMeta) throws SerializeException {
		ClassMeta<?> cMeta;
		if (pMeta != null)
			cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
		else if (value != null)
			cMeta = getClassMetaForObject(value);
		else
			cMeta = object();
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

		String valueStr;
		if (value == null)
			valueStr = "null";
		else if (isSimpleOrJson5Inline(aType, value))
			valueStr = formatSimpleValue(value, aType);
		else
			valueStr = encodeComplexValue(value);
		w.keyValue(key, valueStr);
	}

	private String formatSimpleValue(Object value, ClassMeta<?> aType) throws SerializeException {
		if (aType.isCharSequence() || aType.isUri()) {
			var s = toString(value);
			return needsQuoting(s) ? "'" + s.replace("'", "''") + "'" : s;
		}
		if (aType.isNumber())
			return value.toString();
		if (aType.isBoolean())
			return ((Boolean)value).toString();
		if (aType.isEnum())
			return ((Enum<?>)value).name();
		if (aType.isDate())
			return writeDate((Date)value, aType);
		if (aType.isCalendar())
			return writeCalendar(value, aType);
		if (aType.isTemporal())
			return writeTemporal((TemporalAccessor)value, aType);
		if (aType.isDuration())
			return writeDuration((Duration)value);
		if (aType.isPeriod())
			return writePeriod((Period)value);
		return toString(value);
	}

	private static boolean needsQuoting(String s) {
		if (ie(s))
			return true;
		if (s.equals("null") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))
			return true;
		// Whole-string numeric check; mirrors -?\d+(\.\d+)?([eE][+-]?\d+)? (integer part allows leading zeros).
		if (StringUtils.matchNumberPrefix(s, true) == s.length())
			return true;
		if (s.contains("=") || s.contains("[") || s.contains("]") || s.contains("#") || s.contains("\n"))
			return true;
		var trimmed = s.trim();
		return !trimmed.equals(s);
	}

	private String encodeComplexValue(Object value) throws SerializeException {
		return getJson5Serializer().write(value);
	}

	private JsonSerializer getJson5Serializer() {
		var b = Json5Serializer.create().marshallingContext((MarshallingContext)getContext());
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
		return !aType.isStreamable();
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
