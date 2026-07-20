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
package org.apache.juneau.marshall.hocon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Session for serializing objects to HOCON format.
 */
@SuppressWarnings({
	"resource", "java:S110", "java:S115", "java:S3776", "java:S6541",
	"rawtypes", "unchecked"
})
public class HoconSerializerSession extends WriterSerializerSession implements RecordWritable {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for HOCON serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

		private HoconSerializer ctx;

		protected Builder(HoconSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public HoconSerializerSession build() {
			return new HoconSerializerSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The serializer context.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return The builder.
	 */
	public static Builder create(HoconSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final HoconSerializer ctx;

	protected HoconSerializerSession(Builder builder) {
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

	protected final HoconWriter getHoconWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof HoconWriter w)
			return w;
		var w = new HoconWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(),
			ctx.useNewlineSeparators, ctx.useUnquotedStrings, ctx.useUnquotedKeys, ctx.omitRootBraces,
			ctx.useMultilineStrings, ctx.useEqualsSign, getUriResolver());
		out.setWriter(w);
		return w;
	}

	@Override
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (o == null)
			return;
		var aType = getClassMetaForObject(o);
		var eType = getExpectedRootType(o);
		var hw = getHoconWriter(out).i(getInitialDepth());
		ClassMeta<?> sType = aType;
		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}
		if (sType.isBean() || sType.isMap()) {
			var omitRoot = ctx.omitRootBraces;
			if (sType.isBean())
				writeBeanMap(hw, toBeanMap(o), getBeanTypeName(this, eType, sType, null), omitRoot);
			else
				writeMap(hw, (Map) o, eType, omitRoot);
		} else {
			writeAnything(hw, o, eType, "root", null);
		}
	}

	private void writeBeanMap(HoconWriter out, BeanMap<?> m, String typeName, boolean omitRootBraces) throws SerializeException {
		var i = indent;
		if (!omitRootBraces) {
			out.objectStart(i);
			indent++;
		}
		var first = Flag.create();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			writeKeyPrefix(out, pm.getName(), i, first);
			out.equalsSign();
			writeString(out, typeName);
			first.set();
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (canIgnoreValue(pMeta, key, value))
				return;
			var isObject = cMeta.isBean() || cMeta.isMap();
			writeKeyPrefix(out, key, i, first);
			if (isObject) {
				out.append(" ");
				if (value instanceof BeanMap bm)
					writeBeanMap(out, bm, getBeanTypeName(this, cMeta, cMeta, pMeta), false);
				else if (cMeta.isBean())
					writeBeanMap(out, toBeanMap(value), getBeanTypeName(this, cMeta, cMeta, pMeta), false);
				else
					writeMap(out, (Map) value, cMeta, false);
			} else {
				out.equalsSign();
				writeAnything(out, value, cMeta, key, pMeta);
			}
			first.set();
		});

		if (!omitRootBraces) {
			indent--;
			out.objectEnd(indent);
		}
	}

	private void writeKeyPrefix(HoconWriter out, Object key, int depth, Flag first) throws SerializeException {
		if (first.isSet())
			out.separator(depth);
		else
			out.cr(depth);
		var keyStr = toString(key);
		out.key(keyStr);
	}

	private void writeMap(HoconWriter out, Map m, ClassMeta<?> type, boolean omitRootBraces) throws SerializeException {
		var i = indent;
		if (!omitRootBraces) {
			out.objectStart(i);
			indent++;
		}
		var keyType = type.getKeyType();
		var valueType = type.getValueType();
		var first = Flag.create();

		forEachEntry(m, x -> {
			var value = x.getValue();
			var key = generalize(x.getKey(), keyType);
			var isObject = value != null && (value instanceof Map || value instanceof BeanMap);
			writeKeyPrefix(out, key, i, first);
			if (isObject) {
				out.append(" ");
				if (value instanceof BeanMap bm)
					writeBeanMap(out, bm, getBeanTypeName(this, valueType, getClassMetaForObject(value), null), false);
				else
					writeMap(out, (Map) value, object(), false);
			} else {
				out.equalsSign();
				writeAnything(out, value, valueType, key == null ? null : toString(key), null);
			}
			first.set();
		});

		if (!omitRootBraces) {
			indent--;
			out.objectEnd(indent);
		}
	}

	private void writeCollection(HoconWriter out, Collection c, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachEntry(c, x -> {
			if (first.isSet())
				out.separator(indent);
			else
				out.cr(indent);
			writeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}

	private void writeString(HoconWriter out, String s) throws SerializeException {
		if (s == null) {
			out.w("null");
			return;
		}
		if (ctx.useMultilineStrings && s.contains("\n"))
			out.tripleQuotedString(s);
		else if (out.isSimpleValue(s))
			out.unquotedString(s);
		else
			out.quotedString(s);
	}

	protected HoconWriter writeAnything(HoconWriter hw, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {
		if (o == null) {
			hw.w("null");
			return hw;
		}
		if (eType == null)
			eType = object();

		var aType = push2(attrName, o, eType);
		var isRecursion = aType == null;

		if (aType == null) {
			o = null;
			aType = object();
		}

		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		var sType = aType;
		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		if (o == null || (sType.isChar() && ((Character) o).charValue() == 0)) {
			hw.w("null");
		} else if (o instanceof byte[] bytes) {
			// byte[] gate: encode as Base64 before isArray would otherwise route it as a generic array.
			writeString(hw, Base64.getEncoder().encodeToString(bytes));
		} else if (sType.isBean()) {
			writeBeanMap(hw, toBeanMap(o), getBeanTypeName(this, eType, aType, pMeta), false);
		} else if (sType.isMap()) {
			if (sType.isBeanMap())
				writeBeanMap(hw, (BeanMap) o, getBeanTypeName(this, eType, aType, pMeta), false);
			else
				writeMap(hw, (Map) o, eType, false);
		} else if (sType.isCollection()) {
			writeCollection(hw, (Collection) o, eType);
		} else if (sType.isArray()) {
			writeCollection(hw, toList(sType.inner(), o), eType);
		} else if (sType.isNumber() || sType.isBoolean()) {
			hw.append(o);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			writeString(hw, getUriResolver().resolve(o));
		} else if (sType.isDate()) {
			writeString(hw, writeDate((Date)o, sType));
		} else if (sType.isCalendar()) {
			writeString(hw, writeCalendar(o, sType));
		} else if (sType.isTemporal()) {
			writeString(hw, writeTemporal((TemporalAccessor)o, sType));
		} else if (sType.isDuration()) {
			var value = writeDuration((Duration)o);
			if (getDurationFormat().isNumeric())
				hw.append(value);
			else
				writeString(hw, value);
		} else if (sType.isPeriod()) {
			writeString(hw, writePeriod((Period)o));
		} else if (sType.isStreamable()) {
			writeStreamable(hw, o, sType, eType);
		} else if (sType.isReader()) {
			pipe((Reader) o, hw, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream) o, hw, SerializerSession::handleThrown);
		} else {
			writeString(hw, toString(o));
		}

		if (!isRecursion)
			pop();
		return hw;
	}

	private void writeStreamable(HoconWriter out, Object o, ClassMeta<?> sType, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachStreamableEntry(o, sType, x -> {
			if (first.isSet())
				out.separator(indent);
			else
				out.cr(indent);
			writeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}
}
