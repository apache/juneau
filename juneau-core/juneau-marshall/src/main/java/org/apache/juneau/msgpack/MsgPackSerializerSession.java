// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.msgpack;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MsgPackDetails">MessagePack Details</a>
 * </ul>
 */
public final class MsgPackSerializerSession extends OutputStreamSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(MsgPackSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends OutputStreamSerializerSession.Builder {

		MsgPackSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(MsgPackSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public MsgPackSerializerSession build() {
			return new MsgPackSerializerSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.SerializerSession.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final MsgPackSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MsgPackSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getMsgPackOutputStream(out), o, getExpectedRootType(o), "root", null);
	}

	/*
	 * Converts the specified output target object to an {@link MsgPackOutputStream}.
	 */
	private static final MsgPackOutputStream getMsgPackOutputStream(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof MsgPackOutputStream)
			return (MsgPackOutputStream)output;
		MsgPackOutputStream os = new MsgPackOutputStream(out.getOutputStream());
		out.setOutputStream(os);
		return os;
	}

	/*
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 */
	@SuppressWarnings({ "rawtypes" })
	private MsgPackOutputStream serializeAnything(MsgPackOutputStream out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null)
			return out.appendNull();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = push2(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null)
			return out.appendNull();

		// Handle Optional<X>
		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		sType = aType;
		String typeName = getBeanTypeName(this, eType, aType, pMeta);

		// Swap if necessary
		ObjectSwap swap = aType.getSwap(this);
		if (swap != null) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);

			// If the getSwapClass() method returns Object, we need to figure out
			// the actual type now.
			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0))
			out.appendNull();
		else if (sType.isBoolean())
			out.appendBoolean((Boolean)o);
		else if (sType.isNumber())
			out.appendNumber((Number)o);
		else if (sType.isBean())
			serializeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.appendString(resolveUri(o.toString()));
		else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, eType);
		}
		else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, eType);
		}
		else if (sType.isByteArray()) {
			out.appendBinary((byte[])o);
		}
		else if (sType.isArray()) {
			serializeCollection(out, toList(sType.getInnerClass(), o), eType);
		}
		else if (sType.isReader()) {
			pipe((Reader)o, out, SerializerSession::handleThrown);
		}
		else if (sType.isInputStream()) {
			pipe((InputStream)o, out, SerializerSession::handleThrown);
		}
		else
			out.appendString(toString(o));

		if (! isRecursion)
			pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void serializeMap(MsgPackOutputStream out, Map m, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		m = sort(m);

		// The map size may change as we're iterating over it, so
		// grab a snapshot of the entries in a separate list.
		List<SimpleMapEntry> entries = list(m.size());
		m.forEach((k,v) -> entries.add(new SimpleMapEntry(k, v)));

		out.startMap(entries.size());

		entries.forEach(x -> {
			Object value = x.value;
			Object key = generalize(x.key, keyType);
			serializeAnything(out, key, keyType, null, null);
			serializeAnything(out, value, valueType, null, null);
		});
	}

	private void serializeBeanMap(MsgPackOutputStream out, final BeanMap<?> m, String typeName) throws SerializeException {

		Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;

		List<BeanPropertyValue> values = new ArrayList<>();

		if (typeName != null) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			values.add(new BeanPropertyValue(pm, pm.getName(), typeName, null));
		}

		m.forEachValue(checkNull, (pMeta,key,value,thrown) -> {
			if (thrown != null) {
				onBeanGetterException(pMeta, thrown);
				return;
			}
			BeanPropertyValue p = new BeanPropertyValue(pMeta, key, value, null);

			if ((! isKeepNullProperties()) && willRecurse(p)) {
				return; // Must handle the case where recursion occurs and property is not serialized.
			}

			values.add(p);
		});

		out.startMap(values.size());

		values.forEach(x -> {
			BeanPropertyMeta pMeta = x.getMeta();
			if (pMeta.canRead()) {
				ClassMeta<?> cMeta = x.getClassMeta();
				String key = x.getName();
				Object value = x.getValue();
				serializeAnything(out, key, null, null, null);
				serializeAnything(out, value, cMeta, key, pMeta);
			}
		});
	}

	private boolean willRecurse(BeanPropertyValue v) throws SerializeException {
		ClassMeta<?> aType = push2(v.getName(), v.getValue(), v.getClassMeta());
		 if (aType != null)
			 pop();
		 return aType == null;
	}

	private static final class SimpleMapEntry {
		final Object key;
		final Object value;

		SimpleMapEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void serializeCollection(MsgPackOutputStream out, Collection c, ClassMeta<?> type) throws SerializeException {
		ClassMeta<?> elementType = type.getElementType();
		List<Object> l = list(c.size());
		c = sort(c);
		l.addAll(c);
		out.startArray(l.size());
		l.forEach(x -> serializeAnything(out, x, elementType, "<iterator>", null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}
}
