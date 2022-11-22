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
package org.apache.juneau.uon;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link UonSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.UonDetails">UON Details</a>
 * </ul>
 */
public class UonSerializerSession extends WriterSerializerSession implements HttpPartSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(UonSerializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends WriterSerializerSession.Builder {

		UonSerializer ctx;
		boolean encoding;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(UonSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
			encoding = ctx.encoding;
		}

		@Override
		public UonSerializerSession build() {
			return new UonSerializerSession(this);
		}

		/**
		 * Overrides the encoding setting for this session.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder encoding(boolean value) {
			encoding = value;
			return this;
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

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.serializer.WriterSerializerSession.Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final UonSerializer ctx;
	private final boolean plainTextParams;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UonSerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		plainTextParams = ctx.getParamFormat() == ParamFormat.PLAINTEXT;
	}

	/**
	 * Converts the specified output target object to an {@link UonWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link UonWriter}.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected final UonWriter getUonWriter(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof UonWriter)
			return (UonWriter)output;
		UonWriter w = new UonWriter(this, out.getWriter(), isUseWhitespace(), getMaxIndent(), isEncoding(), isTrimStrings(), plainTextParams, getQuoteChar(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	private final UonWriter getUonWriter(Writer out) throws Exception {
		return new UonWriter(this, out, isUseWhitespace(), getMaxIndent(), isEncoding(), isTrimStrings(), plainTextParams, getQuoteChar(), getUriResolver());
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getUonWriter(out).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
	}

	/**
	 * Workhorse method.
	 *
	 * <p>
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 *
	 * @param out The writer to serialize to.
	 * @param o The object being serialized.
	 * @param eType The expected type of the object if this is a bean property.
	 * @param attrName
	 * 	The bean property name if this is a bean property.
	 * 	<jk>null</jk> if this isn't a bean property being serialized.
	 * @param pMeta The bean property metadata.
	 * @return The same writer passed in.
	 * @throws SerializeException Generic serialization error occurred.
	 */
	@SuppressWarnings({ "rawtypes" })
	protected SerializerWriter serializeAnything(UonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null) {
			out.appendObject(null, false);
			return out;
		}

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type
		ClassMeta<?> sType;			// The serialized type

		aType = push2(attrName, o, eType);
		boolean isRecursion = aType == null;

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

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
			out.appendObject(null, false);
		else if (sType.isBoolean())
			out.appendBoolean(o);
		else if (sType.isNumber())
			out.appendNumber(o);
		else if (sType.isBean())
			serializeBeanMap(out, toBeanMap(o), typeName);
		else if (sType.isUri() || (pMeta != null && pMeta.isUri()))
			out.appendUri(o);
		else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, eType);
		}
		else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, eType);
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
		else {
			out.appendObject(o, false);
		}

		if (! isRecursion)
			pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(UonWriter out, Map m, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		if (! plainTextParams)
			out.append('(');

		Flag addComma = Flag.create();
		forEachEntry(m, x -> {
			addComma.ifSet(()->out.append(',')).set();
			Object value = x.getValue();
			Object key = generalize(x.getKey(), keyType);
			out.cr(indent).appendObject(key, false).append('=');
			serializeAnything(out, value, valueType, toString(key), null);
		});

		addComma.ifSet(()->out.cre(indent-1));

		if (! plainTextParams)
			out.append(')');

		return out;
	}

	private SerializerWriter serializeBeanMap(UonWriter out, BeanMap<?> m, String typeName) throws SerializeException {

		if (! plainTextParams)
			out.append('(');

		Flag addComma = Flag.create();

		if (typeName != null) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			out.cr(indent).appendObject(pm.getName(), false).append('=').appendObject(typeName, false);
			addComma.set();
		}

		Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;
		m.forEachValue(checkNull, (pMeta,key,value,thrown) -> {
			ClassMeta<?> cMeta = pMeta.getClassMeta();

			if (thrown != null)
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(cMeta, key, value))
				return;

			addComma.ifSet(() -> out.append(',')).set();

			out.cr(indent).appendObject(key, false).append('=');

			serializeAnything(out, value, cMeta, key, pMeta);
		});

		if (m.size() > 0)
			out.cre(indent-1);
		if (! plainTextParams)
			out.append(')');

		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeCollection(UonWriter out, Collection c, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> elementType = type.getElementType();

		if (! plainTextParams)
			out.append('@').append('(');

		Flag addComma = Flag.create();
		forEachEntry(c, x -> {
			addComma.ifSet(()->out.append(',')).set();
			out.cr(indent);
			serializeAnything(out, x, elementType, "<iterator>", null);
		});

		addComma.ifSet(()->out.cre(indent-1));
		if (! plainTextParams)
			out.append(')');

		return out;
	}

	@Override /* HttpPartSerializer */
	public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
		try {
			// Shortcut for simple types.
			ClassMeta<?> cm = getClassMetaForObject(value);
			if (cm != null && (schema == null || schema.getType() == HttpPartDataType.NO_TYPE)) {
				if (cm.isNumber() || cm.isBoolean())
					return Mutaters.toString(value);
				if (cm.isString()) {
					String s = Mutaters.toString(value);
					if (s.isEmpty() || ! UonUtils.needsQuotes(s))
						return s;
				}
			}
			StringWriter w = new StringWriter();
			serializeAnything(getUonWriter(w).i(getInitialDepth()), value, getExpectedRootType(value), "root", null);
			return w.toString();
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see UonSerializer.Builder#addBeanTypesUon()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Encode non-valid URI characters.
	 *
	 * @see UonSerializer.Builder#encoding()
	 * @return
	 * 	<jk>true</jk> if non-valid URI characters should be encoded with <js>"%xx"</js> constructs.
	 */
	protected final boolean isEncoding() {
		return ctx.isEncoding();
	}

	/**
	 * Format to use for query/form-data/header values.
	 *
	 * @see UonSerializer.Builder#paramFormat(ParamFormat)
	 * @return
	 * 	Specifies the format to use for URL GET parameter keys and values.
	 */
	protected final ParamFormat getParamFormat() {
		return ctx.getParamFormat();
	}

	/**
	 * Quote character.
	 *
	 * @see UonSerializer.Builder#quoteCharUon(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	@Override
	protected final char getQuoteChar() {
		return ctx.getQuoteChar();
	}
}
