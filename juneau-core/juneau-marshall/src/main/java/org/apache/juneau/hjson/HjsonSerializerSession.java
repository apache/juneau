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
package org.apache.juneau.hjson;

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
 * Session for serializing objects to Hjson format.
 */
@SuppressWarnings({
	"resource", "java:S110", "java:S115", "java:S3776", "java:S6541",
	"rawtypes", "unchecked"
})
public class HjsonSerializerSession extends WriterSerializerSession {

	private static final String ARG_ctx = "ctx";

	/**
	 * Builder for Hjson serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder {

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
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
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
				serializeBeanMap(hw, toBeanMap(o), getBeanTypeName(this, eType, sType, null), omitRoot);
			else
				serializeMap(hw, (Map) o, eType, omitRoot);
		} else {
			serializeAnything(hw, o, eType, "root", null);
		}
	}

	private void serializeBeanMap(HjsonWriter out, BeanMap<?> m, String typeName, boolean omitBraces) throws SerializeException {
		var i = indent;
		out.objectStart(omitBraces, i);
		indent++;
		var first = Flag.create();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			writeKey(out, pm.getName(), i, first);
			serializeString(out, typeName);
			first.set();
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = pMeta.getClassMeta();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (canIgnoreValue(cMeta, key, value))
				return;
			writeKey(out, key, i, first);
			serializeAnything(out, value, cMeta, key, pMeta);
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

	private void serializeMap(HjsonWriter out, Map m, ClassMeta<?> type, boolean omitBraces) throws SerializeException {
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
			serializeAnything(out, value, valueType, key == null ? null : toString(key), null);
			first.set();
		});

		indent--;
		out.objectEnd(omitBraces, indent);
	}

	private void serializeCollection(HjsonWriter out, Collection c, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachEntry(c, x -> {
			if (first.isSet())
				out.memberSeparator(indent);
			else
				out.cr(indent);
			serializeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}

	private void serializeString(HjsonWriter out, String s) throws SerializeException {
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

	protected HjsonWriter serializeAnything(HjsonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {
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
		} else if (sType.isNumber() || sType.isBoolean()) {
			out.append(o);
		} else if (sType.isDateOrCalendarOrTemporal()) {
			serializeString(out, Iso8601Utils.format(o, sType, getTimeZone()));
		} else if (sType.isDuration()) {
			serializeString(out, o.toString());
		} else if (sType.isBean()) {
			serializeBeanMap(out, toBeanMap(o), getBeanTypeName(this, eType, aType, pMeta), false);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			serializeString(out, getUriResolver().resolve(o));
		} else if (sType.isMap()) {
			if (o instanceof BeanMap o2)
				serializeBeanMap(out, o2, getBeanTypeName(this, eType, aType, pMeta), false);
			else
				serializeMap(out, (Map) o, eType, false);
		} else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, eType);
		} else if (sType.isArray()) {
			serializeCollection(out, toList(sType.inner(), o), eType);
		} else if (sType.isStreamable()) {
			serializeStreamable(out, o, sType, eType);
		} else if (sType.isReader()) {
			pipe((Reader) o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream) o, out, SerializerSession::handleThrown);
		} else {
			serializeString(out, toString(o));
		}

		if (!isRecursion)
			pop();
		return out;
	}

	private void serializeStreamable(HjsonWriter out, Object o, ClassMeta<?> sType, ClassMeta<?> type) throws SerializeException {
		var eType = type.getElementType();
		out.arrayStart();
		indent++;
		var first = Flag.create();
		forEachStreamableEntry(o, sType, x -> {
			if (first.isSet())
				out.memberSeparator(indent);
			else
				out.cr(indent);
			serializeAnything(out, x, eType, "<iterator>", null);
			first.set();
		});
		indent--;
		out.arrayEnd();
	}
}
