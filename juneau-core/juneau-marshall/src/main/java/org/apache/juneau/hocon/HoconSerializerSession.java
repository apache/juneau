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
package org.apache.juneau.hocon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.Iso8601Utils;

/**
 * Session for serializing objects to HOCON format.
 */
@SuppressWarnings({
	"resource", "java:S110", "java:S115", "java:S3776", "java:S6541",
	"rawtypes", "unchecked"
})
public class HoconSerializerSession extends WriterSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for HOCON serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder {

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
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
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
				serializeBeanMap(hw, toBeanMap(o), getBeanTypeName(this, eType, sType, null), omitRoot);
			else
				serializeMap(hw, (Map) o, eType, omitRoot);
		} else {
			serializeAnything(hw, o, eType, "root", null);
		}
	}

	private void serializeBeanMap(HoconWriter out, BeanMap<?> m, String typeName, boolean omitRootBraces) throws SerializeException {
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
			serializeString(out, typeName);
			first.set();
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = pMeta.getClassMeta();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (canIgnoreValue(cMeta, key, value))
				return;
			var isObject = cMeta.isBean() || cMeta.isMap();
			writeKeyPrefix(out, key, i, first);
			if (isObject) {
				out.append(" ");
				if (value instanceof BeanMap bm)
					serializeBeanMap(out, bm, getBeanTypeName(this, cMeta, cMeta, pMeta), false);
				else
					serializeMap(out, (Map) value, cMeta, false);
			} else {
				out.equalsSign();
				serializeAnything(out, value, cMeta, key, pMeta);
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

	private void serializeMap(HoconWriter out, Map m, ClassMeta<?> type, boolean omitRootBraces) throws SerializeException {
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
					serializeBeanMap(out, bm, getBeanTypeName(this, valueType, getClassMetaForObject(value), null), false);
				else
					serializeMap(out, (Map) value, object(), false);
			} else {
				out.equalsSign();
				serializeAnything(out, value, valueType, key == null ? null : toString(key), null);
			}
			first.set();
		});

		if (!omitRootBraces) {
			indent--;
			out.objectEnd(indent);
		}
	}

	private void serializeCollection(HoconWriter out, Collection c, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachEntry(c, x -> {
			if (first.isSet())
				out.separator(indent);
			else
				out.cr(indent);
			serializeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}

	private void serializeString(HoconWriter out, String s) throws SerializeException {
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

	protected HoconWriter serializeAnything(HoconWriter hw, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {
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
		} else if (sType.isNumber() || sType.isBoolean()) {
			hw.append(o);
		} else if (sType.isDateOrCalendarOrTemporal()) {
			serializeString(hw, Iso8601Utils.format(o, sType, getTimeZone()));
		} else if (sType.isDuration()) {
			serializeString(hw, o.toString());
		} else if (sType.isBean()) {
			serializeBeanMap(hw, toBeanMap(o), getBeanTypeName(this, eType, aType, pMeta), false);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			serializeString(hw, getUriResolver().resolve(o));
		} else if (sType.isMap()) {
			if (o instanceof BeanMap o2)
				serializeBeanMap(hw, o2, getBeanTypeName(this, eType, aType, pMeta), false);
			else
				serializeMap(hw, (Map) o, eType, false);
		} else if (sType.isCollection()) {
			serializeCollection(hw, (Collection) o, eType);
		} else if (sType.isArray()) {
			serializeCollection(hw, toList(sType.inner(), o), eType);
		} else if (sType.isStreamable()) {
			serializeStreamable(hw, o, sType, eType);
		} else if (sType.isReader()) {
			pipe((Reader) o, hw, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream) o, hw, SerializerSession::handleThrown);
		} else {
			serializeString(hw, toString(o));
		}

		if (!isRecursion)
			pop();
		return hw;
	}

	private void serializeStreamable(HoconWriter out, Object o, ClassMeta<?> sType, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachStreamableEntry(o, sType, x -> {
			if (first.isSet())
				out.separator(indent);
			else
				out.cr(indent);
			serializeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}
}
