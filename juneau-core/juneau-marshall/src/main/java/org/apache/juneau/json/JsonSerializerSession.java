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
package org.apache.juneau.json;

import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a> * </ul>
 */
public class JsonSerializerSession extends WriterSerializerSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(JsonSerializer ctx) {
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

		JsonSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(JsonSerializer ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public JsonSerializerSession build() {
			return new JsonSerializerSession(this);
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

	private final JsonSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSerializerSession(Builder builder) {
		super(builder);
		this.ctx = builder.ctx;
	}

	@Override /* SerializerSesssion */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getJsonWriter(out).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
	}

	/**
	 * Method that can be called from subclasses to serialize an object to JSON.
	 *
	 * <p>
	 * Used by {@link JsonSchemaSerializerSession} for serializing examples to JSON.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 * @throws Exception Error occurred.
	 */
	protected String serializeJson(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		serializeAnything(getJsonWriter(createPipe(sw)).i(getInitialDepth()), o, getExpectedRootType(o), "root", null);
		return sw.toString();
	}

	/**
	 * Workhorse method.
	 * Determines the type of object, and then calls the appropriate type-specific serialization method.
	 *
	 * @param out The output writer.
	 * @param o The object to serialize.
	 * @param eType The expected type.
	 * @param attrName The attribute name.
	 * @param pMeta The bean property currently being parsed.
	 * @return The same writer passed in.
	 * @throws SerializeException General serialization error occurred.
	 */
	@SuppressWarnings({ "rawtypes" })
	protected JsonWriter serializeAnything(JsonWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta) throws SerializeException {

		if (o == null) {
			out.append("null");
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

		String wrapperAttr = getJsonClassMeta(sType).getWrapperAttr();
		if (wrapperAttr != null) {
			out.w('{').cr(indent).attr(wrapperAttr).w(':').s(indent);
			indent++;
		}

		// '\0' characters are considered null.
		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
			out.append("null");
		} else if (sType.isNumber() || sType.isBoolean()) {
			out.append(o);
		} else if (sType.isBean()) {
			serializeBeanMap(out, toBeanMap(o), typeName);
		} else if (sType.isUri() || (pMeta != null && pMeta.isUri())) {
			out.uriValue(o);
		} else if (sType.isMap()) {
			if (o instanceof BeanMap)
				serializeBeanMap(out, (BeanMap)o, typeName);
			else
				serializeMap(out, (Map)o, eType);
		} else if (sType.isCollection()) {
			serializeCollection(out, (Collection) o, eType);
		} else if (sType.isArray()) {
			serializeCollection(out, toList(sType.getInnerClass(), o), eType);
		} else if (sType.isReader()) {
			pipe((Reader)o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream)o, out, SerializerSession::handleThrown);
		} else {
			out.stringValue(toString(o));
		}

		if (wrapperAttr != null) {
			indent--;
			out.cre(indent-1).w('}');
		}

		if (! isRecursion)
			pop();
		return out;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private SerializerWriter serializeMap(JsonWriter out, Map m, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		int i = indent;
		out.w('{');

		Flag addComma = Flag.create();
		forEachEntry(m, x -> {
			addComma.ifSet(()->out.w(',').smi(i)).set();
			Object value = x.getValue();
			Object key = generalize(x.getKey(), keyType);
			out.cr(i).attr(toString(key)).w(':').s(i);
			serializeAnything(out, value, valueType, (key == null ? null : toString(key)), null);
		});

		out.cre(i-1).w('}');

		return out;
	}

	private SerializerWriter serializeBeanMap(JsonWriter out, BeanMap<?> m, String typeName) throws SerializeException {
		int i = indent;
		out.w('{');

		Flag addComma = Flag.create();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || x != null;

		if (typeName != null) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			out.cr(i).attr(pm.getName()).w(':').s(i).stringValue(typeName);
			addComma.set();
		}

		m.forEachValue(checkNull, (pMeta,key,value,thrown) -> {
			ClassMeta<?> cMeta = pMeta.getClassMeta();
			if (thrown != null)
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(cMeta, key, value))
				return;

			addComma.ifSet(()->out.append(',').smi(i)).set();

			out.cr(i).attr(key).w(':').s(i);

			serializeAnything(out, value, cMeta, key, pMeta);
		});

		out.cre(i-1).w('}');
		return out;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SerializerWriter serializeCollection(JsonWriter out, Collection c, ClassMeta<?> type) throws SerializeException {

		ClassMeta<?> elementType = type.getElementType();

		out.w('[');
		Flag addComma = Flag.create();
		forEachEntry(c, x -> {
			addComma.ifSet(()->out.w(',').smi(indent)).set();
			out.cr(indent);
			serializeAnything(out, x, elementType, "<iterator>", null);
		});

		out.cre(indent-1).w(']');
		return out;
	}

	/**
	 * Converts the specified output target object to an {@link JsonWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in an {@link JsonWriter}.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected final JsonWriter getJsonWriter(SerializerPipe out) throws IOException {
		Object output = out.getRawOutput();
		if (output instanceof JsonWriter)
			return (JsonWriter)output;
		JsonWriter w = new JsonWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isEscapeSolidus(), getQuoteChar(),
			isSimpleAttrs(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see JsonSerializer.Builder#addBeanTypesJson()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes.
	 *
	 * @see JsonSerializer.Builder#escapeSolidus()
	 * @return
	 * 	<jk>true</jk> if solidus (e.g. slash) characters should be escaped.
	 */
	protected final boolean isEscapeSolidus() {
		return ctx.isEscapeSolidus();
	}

	/**
	 * Simple JSON attributes.
	 *
	 * @see JsonSerializer.Builder#simpleAttrs()
	 * @return
	 * 	<jk>true</jk> if JSON attribute names will only be quoted when necessary.
	 * 	<br>Otherwise, they are always quoted.
	 */
	protected final boolean isSimpleAttrs() {
		return ctx.isSimpleAttrs();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the language-specific metadata on the specified class.
	 *
	 * @param cm The class to return the metadata on.
	 * @return The metadata.
	 */
	protected JsonClassMeta getJsonClassMeta(ClassMeta<?> cm) {
		return ctx.getJsonClassMeta(cm);
	}
}
