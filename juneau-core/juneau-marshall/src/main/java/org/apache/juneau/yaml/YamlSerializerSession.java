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
package org.apache.juneau.yaml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Session object that lives for the duration of a single use of {@link YamlSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/YamlBasics">YAML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115" // Constants use UPPER_snakeCase naming convention
})
public class YamlSerializerSession extends WriterSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializerSession.Builder {

		private YamlSerializer ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(YamlSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
		}

		@Override /* Overridden from Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override
		public YamlSerializerSession build() {
			return new YamlSerializerSession(this);
		}

		@Override /* Overridden from Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder fileCharset(Charset value) {
			super.fileCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder resolver(VarResolverSession value) {
			super.resolver(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder streamCharset(Charset value) {
			super.streamCharset(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* Overridden from Builder */
		public Builder useWhitespace(Boolean value) {
			super.useWhitespace(value);
			return this;
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(YamlSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	private final YamlSerializer ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected YamlSerializerSession(Builder builder) {
		super(builder);
		this.ctx = builder.ctx;
	}

	@Override /* Overridden from SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws IOException, SerializeException {
		serializeAnything(getYamlWriter(out), o, getExpectedRootType(o), "root", null, true);
	}

	/**
	 * Converts the specified output target object to a {@link YamlWriter}.
	 *
	 * @param out The output target object.
	 * @return The output target object wrapped in a {@link YamlWriter}.
	 */
	protected final YamlWriter getYamlWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof YamlWriter output2)
			return output2;
		var w = new YamlWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
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
	 * @param asField If <jk>true</jk>, this value appears inline after a key or at root level.
	 * 	If <jk>false</jk>, this value needs its own indentation as a standalone block.
	 * @return The same writer passed in.
	 * @throws SerializeException General serialization error occurred.
	 */
	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic type handling
		"java:S3776", // Cognitive complexity acceptable for this specific logic
	})
	protected YamlWriter serializeAnything(YamlWriter out, Object o, ClassMeta<?> eType, String attrName, BeanPropertyMeta pMeta, boolean asField) throws SerializeException {

		if (o == null) {
			out.append("null");
			return out;
		}

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;
		ClassMeta<?> sType;

		aType = push2(attrName, o, eType);
		boolean isRecursion = aType == null;

		if (aType == null) {
			o = null;
			aType = object();
		}

		if (isOptional(aType)) {
			o = getOptionalValue(o);
			eType = getOptionalType(eType);
			aType = getClassMetaForObject(o, object());
		}

		sType = aType;
		String typeName = getBeanTypeName(this, eType, aType, pMeta);

		var swap = aType.getSwap(this);
		if (nn(swap)) {
			o = swap(swap, o);
			sType = swap.getSwapClassMeta(this);

			if (sType.isObject())
				sType = getClassMetaForObject(o);
		}

		if (o == null || (sType.isChar() && ((Character)o).charValue() == 0)) {
			out.append("null");
		} else if (sType.isNumber() || sType.isBoolean()) {
			out.append(o);
		} else if (sType.isBean()) {
			serializeBeanMap(out, toBeanMap(o), typeName);
		} else if (sType.isUri() || (nn(pMeta) && pMeta.isUri())) {
			out.uriValue(o);
		} else if (sType.isMap()) {
			if (o instanceof BeanMap o2)
				serializeBeanMap(out, o2, typeName);
			else
				serializeMap(out, (Map)o, eType);
		} else if (sType.isCollection()) {
			serializeCollection(out, (Collection)o, eType, asField);
		} else if (sType.isArray()) {
			serializeCollection(out, toList(sType.inner(), o), eType, asField);
		} else if (sType.isReader()) {
			pipe((Reader)o, out, SerializerSession::handleThrown);
		} else if (sType.isInputStream()) {
			pipe((InputStream)o, out, SerializerSession::handleThrown);
		} else {
			out.textValue(toString(o));
		}

		if (! isRecursion)
			pop();
		return out;
	}

	/**
	 * Determines whether a ClassMeta represents a complex type that requires block-style YAML formatting.
	 */
	private boolean isComplexValue(ClassMeta<?> cm) {
		if (cm == null)
			return false;
		if (cm.isMapOrBean() || cm.isCollectionOrArrayOrOptional())
			return true;
		var swap = cm.getSwap(this);
		if (swap != null) {
			var swapType = swap.getSwapClassMeta(this);
			return swapType.isMapOrBean() || swapType.isCollectionOrArrayOrOptional();
		}
		return false;
	}

	private SerializerWriter serializeBeanMap(YamlWriter out, BeanMap<?> m, String typeName) throws SerializeException {
		int i = indent - 1;

		var addAtt = Flag.create();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		if (nn(typeName)) {
			BeanPropertyMeta pm = m.getMeta().getTypeProperty();
			out.key(pm.getName()).textValue(typeName);
			addAtt.set();
		}

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = pMeta.getClassMeta();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);

			if (canIgnoreValue(cMeta, key, value))
				return;

			addAtt.ifSet(() -> out.yamlIndent(i)).set();

			ClassMeta<?> actualType = value == null ? cMeta : getClassMetaForObject(value, cMeta);
			if (value != null && isComplexValue(actualType)) {
				out.keyName(key).w(": ");
				if (actualType.isCollectionOrArrayOrOptional()) {
					serializeAnything(out, value, cMeta, key, pMeta, false);
				} else {
					out.yamlIndent(i + 1);
					serializeAnything(out, value, cMeta, key, pMeta, true);
				}
			} else {
				out.key(key);
				serializeAnything(out, value, cMeta, key, pMeta, true);
			}
		});

		if (addAtt.isUnset())
			out.w("{}");

		return out;
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic collection/map serialization
		"unchecked", // Type erasure requires unchecked casts in collection/map serialization
	})
	private SerializerWriter serializeMap(YamlWriter out, Map m, ClassMeta<?> type) throws SerializeException {
		int i = indent - 1;

		var keyType = type.getKeyType();
		var valueType = type.getValueType();

		if (m.isEmpty()) {
			out.w("{}");
			return out;
		}

		var addAtt = Flag.create();
		forEachEntry(m, x -> {
			addAtt.ifSet(() -> out.yamlIndent(i)).set();
			Object value = x.getValue();
			Object key = generalize(x.getKey(), keyType);

			ClassMeta<?> actualType = value == null ? valueType : getClassMetaForObject(value, valueType);
			if (value != null && isComplexValue(actualType)) {
				out.keyName(toString(key)).w(": ");
				if (actualType.isCollectionOrArrayOrOptional()) {
					serializeAnything(out, value, valueType, (key == null ? null : toString(key)), null, false);
				} else {
					out.yamlIndent(i + 1);
					serializeAnything(out, value, valueType, (key == null ? null : toString(key)), null, true);
				}
			} else {
				out.key(toString(key));
				serializeAnything(out, value, valueType, (key == null ? null : toString(key)), null, true);
			}
		});

		return out;
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic collection/map serialization
		"unchecked", // Type erasure requires unchecked casts in collection/map serialization
	})
	private SerializerWriter serializeCollection(YamlWriter out, Collection c, ClassMeta<?> type, boolean asField) throws SerializeException {
		int i = indent - 1;

		if (c.isEmpty()) {
			out.w("[]");
			return out;
		}

		var elementType = type.getElementType();

		var addEntry = Flag.create();
		forEachEntry(c, x -> {
			if (asField && addEntry.isUnset()) {
				out.w("- ");
			} else {
				out.listEntry(i);
			}
			addEntry.set();
			serializeAnything(out, x, elementType, "<iterator>", null, true);
		});

		return out;
	}

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() { return ctx.isAddBeanTypes(); }
}
