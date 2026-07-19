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
package org.apache.juneau.marshall.hjson;

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
 * Session for serializing objects to Hjson format.
 */
@SuppressWarnings({
	"resource", "java:S110", "java:S115", "java:S3776", "java:S6541",
	"rawtypes", "unchecked"
})
public class HjsonSerializerSession extends WriterSerializerSession implements RecordWritable {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for Hjson serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder<Builder> {

		private HjsonSerializer ctx;

		protected Builder(HjsonSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public HjsonSerializerSession build() {
			return new HjsonSerializerSession(this);
		}
	}

	/**
	 * Creates a session builder.
	 *
	 * @param ctx The serializer context.
	 * @return The builder.
	 */
	public static Builder create(HjsonSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final HjsonSerializer ctx;

	protected HjsonSerializerSession(Builder builder) {
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

	protected final HjsonWriter getHjsonWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof HjsonWriter w)
			return w;
		var w = new HjsonWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(),
			ctx.useNewlineSeparators, ctx.useQuotelessStrings, ctx.useQuotelessKeys, ctx.omitRootBraces,
			ctx.useMultilineStrings, isUseWhitespace(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	@Override
	protected void doWrite(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (o == null)
			return;
		var aType = getClassMetaForObject(o);
		var eType = getExpectedRootType(o);
		var hw = getHjsonWriter(out).i(getInitialDepth());
		// Apply object swap before type check (e.g., surrogate serializers convert D1 → D2).
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

	private void writeBeanMap(HjsonWriter out, BeanMap<?> m, String typeName, boolean omitBraces) throws SerializeException {
		var i = indent;
		out.objectStart(omitBraces, i);
		indent++;
		var first = Flag.create();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			writeKey(out, pm.getName(), i, first);
			writeString(out, typeName);
			first.set();
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = (ClassMeta<?>) pMeta.getBeanInfo();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (canIgnoreValue(pMeta, key, value))
				return;
			writeKey(out, key, i, first);
			writeAnything(out, value, cMeta, key, pMeta);
			first.set();
		});

		indent--;
		out.objectEnd(omitBraces, indent);
	}

	private void writeKey(HjsonWriter out, Object key, int depth, Flag first) throws SerializeException {
		if (first.isSet())
			out.memberSeparator(depth);
		else
			out.cr(depth);
		var keyStr = toString(key);
		if (out.needsQuoting(keyStr))
			out.quotedString(keyStr);
		else
			out.quotelessString(keyStr);
		out.keyValueSeparator();
	}

	private void writeMap(HjsonWriter out, Map m, ClassMeta<?> type, boolean omitBraces) throws SerializeException {
		var i = indent;
		out.objectStart(omitBraces, i);
		indent++;
		var keyType = type.getKeyType();
		var valueType = type.getValueType();
		var first = Flag.create();

		forEachEntry(m, x -> {
			var value = x.getValue();
			var key = generalize(x.getKey(), keyType);
			writeKey(out, key, i, first);
			writeAnything(out, value, valueType, key == null ? null : toString(key), null);
			first.set();
		});

		indent--;
		out.objectEnd(omitBraces, indent);
	}

	private void writeCollection(HjsonWriter out, Collection c, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachEntry(c, x -> {
			if (first.isSet())
				out.memberSeparator(indent);
			else
				out.cr(indent);
			writeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}

	private void writeString(HjsonWriter out, String s) throws SerializeException {
		if (s == null) {
			out.w("null");
			return;
		}
		if (ctx.useMultilineStrings && s.contains("\n"))
			out.multilineString(s);
		else if (out.isSimpleValue(s))
			out.quotelessString(s);
		else
			out.quotedString(s);
	}

	protected HjsonWriter writeAnything(HjsonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {
		if (o == null) {
			out.w("null");
			return out;
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
			out.w("null");
		} else if (sType.isBean()) {
			writeBeanMap(out, toBeanMap(o), getBeanTypeName(this, eType, aType, pMeta), false);
		} else if (sType.isMap()) {
			if (sType.isBeanMap())
				writeBeanMap(out, (BeanMap) o, getBeanTypeName(this, eType, aType, pMeta), false);
			else
				writeMap(out, (Map) o, eType, false);
		} else if (sType.isCollection()) {
			writeCollection(out, (Collection) o, eType);
		} else if (sType.isArray()) {
			writeCollection(out, toList(sType.inner(), o), eType);
		} else if (sType.isNumber() || sType.isBoolean()) {
			out.append(o);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			writeString(out, getUriResolver().resolve(o));
		} else if (sType.isDate()) {
			writeString(out, writeDate((Date)o, sType));
		} else if (sType.isCalendar()) {
			writeString(out, writeCalendar(o, sType));
		} else if (sType.isTemporal()) {
			writeString(out, writeTemporal((TemporalAccessor)o, sType));
		} else if (sType.isDuration()) {
			var value = writeDuration((Duration)o);
			if (getDurationFormat().isNumeric())
				out.append(value);
			else
				writeString(out, value);
		} else if (sType.isPeriod()) {
			writeString(out, writePeriod((Period)o));
		} else if (sType.isStreamable()) {
			writeStreamable(out, o, sType, eType);
		} else if (sType.isReader()) {
			pipe((Reader) o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream) o, out, SerializerSession::handleThrown);
		} else {
			writeString(out, toString(o));
		}

		if (!isRecursion)
			pop();
		return out;
	}

	private void writeStreamable(HjsonWriter out, Object o, ClassMeta<?> sType, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachStreamableEntry(o, sType, x -> {
			if (first.isSet())
				out.memberSeparator(indent);
			else
				out.cr(indent);
			writeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}
}
