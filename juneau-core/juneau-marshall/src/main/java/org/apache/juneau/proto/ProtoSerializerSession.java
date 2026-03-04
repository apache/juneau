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
package org.apache.juneau.proto;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.Iso8601Utils;

/**
 * Session for serializing objects to Protobuf Text Format.
 */
@SuppressWarnings({
	"resource", // Writer managed by SerializerPipe; caller closes
	"rawtypes", // Raw types necessary for generic Map/Collection handling
	"java:S110", // Session classes inherit many parameters from base
	"java:S115", // ARG_/CONST_ prefix follows framework convention
	"java:S3776", // Cognitive complexity acceptable for serialize dispatch
	"java:S6541"  // Brain method acceptable for serializeAnything
})
public class ProtoSerializerSession extends WriterSerializerSession {

	private static final String ARG_ctx = "ctx";
	private static final String CONST_value = "_value";

	/** Enable with -Djuneau.proto.serialize.debug=true or ProtoSerializerSession.setDebugTrace(true) */
	private static boolean debugTrace = Boolean.getBoolean("juneau.proto.serialize.debug");

	/**
	 * Enable debug trace for investigation (captures to traceLog).
	 *
	 * @param value Enable or disable debug trace.
	 */
	public static void setDebugTrace(boolean value) { debugTrace = value; }

	/** Last trace output when debugTrace was enabled. */
	protected static final List<String> traceLog = new ArrayList<>();

	/**
	 * Builder for Proto serializer session.
	 */
	public static class Builder extends WriterSerializerSession.Builder {

		private ProtoSerializer ctx;

		protected Builder(ProtoSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override
		public ProtoSerializerSession build() {
			return new ProtoSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder.
	 *
	 * @param ctx The serializer context.
	 * @return A new builder.
	 */
	public static Builder create(ProtoSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final ProtoSerializer ctx;

	protected ProtoSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		if (o == null)
			return;
		if (debugTrace) traceLog.add("[Proto] doSerialize: o=" + o.getClass().getName() + ", getExpectedRootType=" + getExpectedRootType(o).inner().getName());
		var w = getProtoWriter(out);
		var eType = getExpectedRootType(o);
		var cm = getClassMetaForObject(o);
		// For root object: if it's a bean (or toBeanMap works), serialize as bean directly to avoid
		// serializeAnything swap/optional complexity that can produce empty output for some POJOs
		if (!cm.isMap() && !cm.isCollection() && !cm.isArray() && !cm.isStreamable()) {
			try {
				var bm = toBeanMap(o);
				if (bm != null && !bm.isEmpty()) {
					serializeBeanMap(w, bm, getBeanTypeName(this, eType, cm, null));
					return;
				}
			} catch (@SuppressWarnings("unused") Exception e) {
				// Not a bean, fall through to serializeAnything
			}
		}
		serializeAnything(w, o, eType, null, null);
	}

	protected final ProtoWriter getProtoWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof ProtoWriter output2)
			return output2;
		var w = new ProtoWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	private void serializeAnything(ProtoWriter out, Object o, ClassMeta<?> eType, String fieldName, BeanPropertyMeta pMeta) throws SerializeException {
		if (o == null)
			return;
		if (eType == null)
			eType = object();

		var aType = push2(fieldName, o, eType);
		var isRecursion = aType == null;
		if (debugTrace && fieldName == null)
			traceLog.add("[Proto] serializeAnything(root): push2 aType=" + (aType == null ? "null" : aType.inner().getName())
				+ ", isBean=" + (aType != null ? aType.isBean() : "n/a") + ", isMap=" + (aType != null ? aType.isMap() : "n/a") + ", isRecursion=" + isRecursion);
		if (aType == null) {
			pop();
			return;
		}

		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			aType = swap.getSwapClassMeta(this);
			if (aType.isObject())
				aType = getClassMetaForObject(o);
		}

		if (o == null)
			return;

		var sType = aType;
		var typeName = getBeanTypeName(this, eType, aType, pMeta);

		if (debugTrace && fieldName == null) {
			var path = sType.isBean() ? "bean" : sType.isMap() ? "map" : (sType.isCollection() || sType.isArray()) ? "collection" : "scalar";
			traceLog.add("[Proto] serializeAnything(root): sType=" + sType.inner().getName() + ", isBean=" + sType.isBean() + ", isMap=" + sType.isMap()
				+ ", path=" + path);
		}
		// Workaround: when isBean() is false but toBeanMap works (e.g. POJO with public fields),
		// treat as bean so we serialize fields instead of Object.toString()
		var treatAsBean = sType.isBean();
		if (!treatAsBean && fieldName == null && !sType.isMap() && !sType.isCollection() && !sType.isArray() && !sType.isStreamable()) {
			try {
				var bm = toBeanMap(o);
				treatAsBean = bm != null && !bm.isEmpty();
			} catch (@SuppressWarnings("unused") Exception e) {
				// Not a bean, will use scalar path
			}
		}
		if (treatAsBean) {
			var bm = toBeanMap(o);
			if (nn(fieldName)) {
				out.messageStart(fieldName, ctx.useColonForMessages);
				indent++;
				serializeBeanMap(out, bm, typeName);
				indent--;
				out.messageEnd(indent);
			} else {
				serializeBeanMap(out, bm, typeName);
			}
		} else if (sType.isMap()) {
			if (nn(fieldName)) {
				out.messageStart(fieldName, ctx.useColonForMessages);
				indent++;
				if (o instanceof BeanMap o2)
					serializeBeanMap(out, o2, typeName);
				else
					serializeMap(out, (Map) o, sType);
				indent--;
				out.messageEnd(indent);
			} else {
				if (o instanceof BeanMap o2)
					serializeBeanMap(out, o2, typeName);
				else
					serializeMap(out, (Map) o, sType);
			}
		} else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, sType, nn(fieldName) ? fieldName : CONST_value);
		} else if (sType.isArray()) {
			serializeCollection(out, toList(sType.inner(), o), sType, nn(fieldName) ? fieldName : CONST_value);
		} else if (sType.isStreamable()) {
			serializeStreamable(out, o, sType, nn(fieldName) ? fieldName : CONST_value);
		} else {
			var name = nn(fieldName) ? fieldName : CONST_value;
			out.scalarField(name);
			serializeScalarValue(out, o, sType);
			out.w('\n');
		}

		if (!isRecursion)
			pop();
	}

	private void serializeBeanMap(ProtoWriter out, BeanMap<?> m, String typeName) throws SerializeException {
		if (isAddBeanTypes() && nn(typeName)) {
			out.cr(indent);
			out.scalarField("_type");
			out.stringValue(typeName);
			out.w('\n');
		}
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);
		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (value == null)
				return;
			if (canIgnoreValue(pMeta.getClassMeta(), key, value))
				return;
			var protoPMeta = ctx.getProtoBeanPropertyMeta(pMeta);
			if (nn(protoPMeta) && !protoPMeta.getComment().isEmpty())
				out.comment(protoPMeta.getComment());
			out.cr(indent);
			var cMeta = pMeta.getClassMeta();
			var aType = getClassMetaForObject(value, cMeta);
			if (aType.isBean() || aType.isMap()) {
				out.messageStart(key, ctx.useColonForMessages);
				indent++;
				if (aType.isBean())
					serializeBeanMap(out, toBeanMap(value), getBeanTypeName(this, cMeta, aType, pMeta));
				else
					serializeMap(out, (Map) value, aType);
				indent--;
				out.messageEnd(indent);
			} else if (aType.isCollection() || aType.isArray()) {
				var c = aType.isArray() ? toList(aType.inner(), value) : (Collection<?>) value;
				var elType = aType.getElementType();
				if (elType.isBean() || elType.isMap()) {
					if (ctx.useListSyntaxForBeans) {
						out.scalarField(key);
						out.listStart();
						var first = true;
						for (Object item : c) {
							if (!first)
								out.w(", ");
							first = false;
							out.w("{\n");
							indent++;
							serializeBeanMap(out, toBeanMap(item), null);
							indent--;
							out.i(indent).w('}');
						}
						out.listEnd().w('\n');
					} else {
						for (var item : c) {
							out.messageStart(key, ctx.useColonForMessages);
							indent++;
							serializeBeanMap(out, toBeanMap(item), null);
							indent--;
							out.messageEnd(indent);
						}
					}
				} else {
					out.scalarField(key);
					out.listStart();
					var first = true;
					for (var el : c) {
						if (!first)
							out.w(", ");
						first = false;
						serializeScalarValue(out, el, elType);
					}
					out.listEnd().w('\n');
				}
			} else {
				out.scalarField(key);
				serializeScalarValue(out, value, aType);
				out.w('\n');
			}
		});
	}

	private void serializeMap(ProtoWriter out, Map<?, ?> map, ClassMeta<?> type) throws SerializeException {
		forEachEntry(map, e -> {
			var k = toString(e.getKey());
			var v = e.getValue();
			if (v == null)
				return;
			out.cr(indent);
			var valueType = type.getValueType();
			var aType = getClassMetaForObject(v, valueType);
			if (aType.isBean() || aType.isMap()) {
				out.messageStart(k, ctx.useColonForMessages);
				indent++;
				if (aType.isBean())
					serializeBeanMap(out, toBeanMap(v), null);
				else
					serializeMap(out, (Map) v, aType);
				indent--;
				out.messageEnd(indent);
			} else if (aType.isCollection() || aType.isArray()) {
				var c = aType.isArray() ? toList(aType.inner(), v) : (Collection<?>) v;
				serializeCollection(out, c, aType, k);
			} else {
				out.scalarField(k);
				serializeScalarValue(out, v, aType);
				out.w('\n');
			}
		});
	}

	private void serializeCollectionItem(ProtoWriter out, Object item) throws SerializeException {
		var aType = getClassMetaForObject(item);
		if (aType.isBean())
			serializeBeanMap(out, toBeanMap(item), null);
		else if (aType.isMap())
			serializeMap(out, (Map) item, aType);
		else
			serializeScalarValue(out, item, aType);
	}

	private void serializeCollection(ProtoWriter out, Collection<?> c, ClassMeta<?> type, String fieldName) throws SerializeException {
		var elType = type.getElementType();
		// When element type is Object, resolve actual type from first element for beans/maps
		var elementIsBeanOrMap = elType.isBean() || elType.isMap();
		if (!elementIsBeanOrMap && elType.isObject() && !c.isEmpty()) {
			var first = c.iterator().next();
			elementIsBeanOrMap = getClassMetaForObject(first).isBean() || getClassMetaForObject(first).isMap();
		}
		if (elementIsBeanOrMap) {
			if (ctx.useListSyntaxForBeans && nn(fieldName)) {
				out.scalarField(fieldName);
				out.listStart();
				var first = true;
				for (var item : c) {
					if (!first)
						out.w(", ");
					first = false;
					out.w("{\n");
					indent++;
					serializeCollectionItem(out, item);
					indent--;
					out.i(indent).w('}');
				}
				out.listEnd().w('\n');
			} else if (nn(fieldName)) {
				for (var item : c) {
					out.messageStart(fieldName, ctx.useColonForMessages);
					indent++;
					serializeCollectionItem(out, item);
					indent--;
					out.messageEnd(indent);
				}
			}
		} else if (nn(fieldName)) {
			out.scalarField(fieldName);
			out.listStart();
			var first = true;
			for (var el : c) {
				if (!first)
					out.w(", ");
				first = false;
				var actualElType = elType.isObject() ? getClassMetaForObject(el, elType) : elType;
				serializeScalarValue(out, el, actualElType);
			}
			out.listEnd().w('\n');
		}
	}

	private void serializeStreamable(ProtoWriter out, Object o, ClassMeta<?> sType, String fieldName) throws SerializeException {
		var elType = sType.getElementType();
		if (elType.isBean() || elType.isMap()) {
			forEachStreamableEntry(o, sType, item -> {
				out.messageStart(fieldName, ctx.useColonForMessages);
				indent++;
				serializeCollectionItem(out, item);
				indent--;
				out.messageEnd(indent);
			});
		} else if (nn(fieldName)) {
			out.scalarField(fieldName);
			out.listStart();
			var first = Flag.create();
			forEachStreamableEntry(o, sType, el -> {
				first.ifSet(() -> out.w(", ")).set();
				serializeScalarValue(out, el, elType);
			});
			out.listEnd().w('\n');
		}
	}

	private void serializeScalarValue(ProtoWriter out, Object value, ClassMeta<?> type) throws SerializeException {
		if (value == null)
			return;
		if (type.isNumber()) {
			if (value instanceof Float || value instanceof Double)
				out.floatValue(((Number) value).doubleValue());
			else
				out.integerValue(((Number) value).longValue());
		} else if (type.isBoolean()) {
			out.booleanValue((Boolean) value);
		} else if (type.isDateOrCalendarOrTemporal()) {
			out.stringValue(Iso8601Utils.format(value, type, getTimeZone()));
		} else if (type.isDuration()) {
			out.stringValue(value.toString());
		} else if (type.isEnum()) {
			out.enumValue(((Enum<?>) value).name());
		} else if (value instanceof byte[] value2) {
			out.bytesValue(value2);
		} else {
			out.stringValue(toString(value));
		}
	}

}
