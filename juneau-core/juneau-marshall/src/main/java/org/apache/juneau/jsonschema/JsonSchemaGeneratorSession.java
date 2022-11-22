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
package org.apache.juneau.jsonschema;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.jsonschema.TypeCategory.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSchemaSerializer}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonSchemaDetails">JSON-Schema Support</a>
 * </ul>
 */
public class JsonSchemaGeneratorSession extends BeanTraverseSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(JsonSchemaGenerator ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanTraverseSession.Builder {

		JsonSchemaGenerator ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(JsonSchemaGenerator ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public JsonSchemaGeneratorSession build() {
			return new JsonSchemaGeneratorSession(this);
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

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final JsonSchemaGenerator ctx;
	private final Map<String,JsonMap> defs;
	private JsonSerializerSession jsSession;
	private JsonParserSession jpSession;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JsonSchemaGeneratorSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		defs = isUseBeanDefs() ? new TreeMap<>() : null;
	}

	/**
	 * Returns the JSON-schema for the specified object.
	 *
	 * @param o
	 * 	The object.
	 * 	<br>Can either be a POJO or a <c>Class</c>/<c>Type</c>.
	 * @return The schema for the type.
	 * @throws BeanRecursionException Bean recursion occurred.
	 * @throws SerializeException Error occurred.
	 */
	public JsonMap getSchema(Object o) throws BeanRecursionException, SerializeException {
		return getSchema(toClassMeta(o), "root", null, false, false, null);
	}

	/**
	 * Returns the JSON-schema for the specified type.
	 *
	 * @param type The object type.
	 * @return The schema for the type.
	 * @throws BeanRecursionException Bean recursion occurred.
	 * @throws SerializeException Error occurred.
	 */
	public JsonMap getSchema(Type type) throws BeanRecursionException, SerializeException {
		return getSchema(getClassMeta(type), "root", null, false, false, null);
	}

	/**
	 * Returns the JSON-schema for the specified type.
	 *
	 * @param cm The object type.
	 * @return The schema for the type.
	 * @throws BeanRecursionException Bean recursion occurred.
	 * @throws SerializeException Error occurred.
	 */
	public JsonMap getSchema(ClassMeta<?> cm) throws BeanRecursionException, SerializeException {
		return getSchema(cm, "root", null, false, false, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JsonMap getSchema(ClassMeta<?> eType, String attrName, String[] pNames, boolean exampleAdded, boolean descriptionAdded, JsonSchemaBeanPropertyMeta jsbpm) throws BeanRecursionException, SerializeException {

		if (ctx.isIgnoredType(eType))
			return null;

		JsonMap out = new JsonMap();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> sType;			// The serialized type
		ObjectSwap objectSwap = eType.getSwap(this);

		aType = push(attrName, eType, null);

		sType = eType.getSerializedClassMeta(this);

		String type = null, format = null;
		Object example = null, description = null;

		boolean useDef = isUseBeanDefs() && sType.isBean() && pNames == null;

		if (useDef) {
			exampleAdded = false;
			descriptionAdded = false;
		}

		if (useDef && defs.containsKey(getBeanDefId(sType))) {
			pop();
			return new JsonMap().append("$ref", getBeanDefUri(sType));
		}

		JsonSchemaClassMeta jscm = null;
		ClassMeta objectSwapCM = objectSwap == null ? null : getClassMeta(objectSwap.getClass());
		if (objectSwapCM != null && objectSwapCM.hasAnnotation(Schema.class))
			jscm = getJsonSchemaClassMeta(objectSwapCM);
		if (jscm == null)
			jscm = getJsonSchemaClassMeta(sType);

		TypeCategory tc = null;

		if (sType.isNumber()) {
			tc = NUMBER;
			if (sType.isDecimal()) {
				type = "number";
				if (sType.isFloat()) {
					format = "float";
				} else if (sType.isDouble()) {
					format = "double";
				}
			} else {
				type = "integer";
				if (sType.isShort()) {
					format = "int16";
				} else if (sType.isInteger()) {
					format = "int32";
				} else if (sType.isLong()) {
					format = "int64";
				}
			}
		} else if (sType.isBoolean()) {
			tc = BOOLEAN;
			type = "boolean";
		} else if (sType.isMap()) {
			tc = MAP;
			type = "object";
		} else if (sType.isBean()) {
			tc = BEAN;
			type = "object";
		} else if (sType.isCollection()) {
			tc = COLLECTION;
			type = "array";
		} else if (sType.isArray()) {
			tc = ARRAY;
			type = "array";
		} else if (sType.isEnum()) {
			tc = ENUM;
			type = "string";
		} else if (sType.isCharSequence() || sType.isChar()) {
			tc = STRING;
			type = "string";
		} else if (sType.isUri()) {
			tc = STRING;
			type = "string";
			format = "uri";
		} else {
			tc = STRING;
			type = "string";
		}

		// Add info from @Schema on bean property.
		if (jsbpm != null) {
			out.append(jsbpm.getSchema());
		}

		out.append(jscm.getSchema());

		Predicate<String> ne = StringUtils::isNotEmpty;
		out.appendIfAbsentIf(ne, "type", type);
		out.appendIfAbsentIf(ne, "format", format);

		if (aType != null) {

			example = getExample(sType, tc, exampleAdded);
			description = getDescription(sType, tc, descriptionAdded);
			exampleAdded |= example != null;
			descriptionAdded |= description != null;

			if (tc == BEAN) {
				JsonMap properties = new JsonMap();
				BeanMeta bm = getBeanMeta(sType.getInnerClass());
				if (pNames != null)
					bm = new BeanMetaFiltered(bm, pNames);
				for (Iterator<BeanPropertyMeta> i = bm.getPropertyMetas().iterator(); i.hasNext();) {
					BeanPropertyMeta p = i.next();
					if (p.canRead())
						properties.put(p.getName(), getSchema(p.getClassMeta(), p.getName(), p.getProperties(), exampleAdded, descriptionAdded, getJsonSchemaBeanPropertyMeta(p)));
				}
				out.put("properties", properties);

			} else if (tc == COLLECTION) {
				ClassMeta et = sType.getElementType();
				if (sType.isCollection() && sType.getInfo().isChildOf(Set.class))
					out.put("uniqueItems", true);
				out.put("items", getSchema(et, "items", pNames, exampleAdded, descriptionAdded, null));

			} else if (tc == ARRAY) {
				ClassMeta et = sType.getElementType();
				if (sType.isCollection() && sType.getInfo().isChildOf(Set.class))
					out.put("uniqueItems", true);
				out.put("items", getSchema(et, "items", pNames, exampleAdded, descriptionAdded, null));

			} else if (tc == ENUM) {
				out.put("enum", getEnums(sType));

			} else if (tc == MAP) {
				JsonMap om = getSchema(sType.getValueType(), "additionalProperties", null, exampleAdded, descriptionAdded, null);
				if (! om.isEmpty())
					out.put("additionalProperties", om);

			}
		}

		out.append(jscm.getSchema());

		Predicate<Object> neo = ObjectUtils::isNotEmpty;
		out.appendIfAbsentIf(neo, "description", description);
		out.appendIfAbsentIf(neo, "example", example);

		if (useDef) {
			defs.put(getBeanDefId(sType), out);
			out = JsonMap.of("$ref", getBeanDefUri(sType));
		}

		pop();

		return out;
	}

	@SuppressWarnings("unchecked")
	private List<String> getEnums(ClassMeta<?> cm) {
		List<String> l = list();
		for (Enum<?> e : ((Class<Enum<?>>)cm.getInnerClass()).getEnumConstants())
			l.add(cm.toString(e));
		return l;
	}

	private Object getExample(ClassMeta<?> sType, TypeCategory t, boolean exampleAdded) throws SerializeException {
		boolean canAdd = isAllowNestedExamples() || ! exampleAdded;
		if (canAdd && (getAddExamplesTo().contains(t) || getAddExamplesTo().contains(ANY))) {
			Object example = sType.getExample(this, jpSession());
			if (example != null) {
				try {
					return JsonParser.DEFAULT.parse(toJson(example), Object.class);
				} catch (ParseException e) {
					throw new SerializeException(e);
				}
			}
		}
		return null;
	}

	private String toJson(Object o) throws SerializeException {
		if (jsSession == null)
			jsSession = ctx.getJsonSerializer().getSession();
		return jsSession.serializeToString(o);
	}

	private JsonParserSession jpSession() {
		if (jpSession == null)
			jpSession = ctx.getJsonParser().getSession();
		return jpSession;
	}

	private Object getDescription(ClassMeta<?> sType, TypeCategory t, boolean descriptionAdded) {
		boolean canAdd = isAllowNestedDescriptions() || ! descriptionAdded;
		if (canAdd && (getAddDescriptionsTo().contains(t) || getAddDescriptionsTo().contains(ANY)))
			return sType.toString();
		return null;
	}

	/**
	 * Returns the definition ID for the specified class.
	 *
	 * @param cm The class to get the definition ID of.
	 * @return The definition ID for the specified class.
	 */
	public String getBeanDefId(ClassMeta<?> cm) {
		return getBeanDefMapper().getId(cm);
	}

	/**
	 * Returns the definition URI for the specified class.
	 *
	 * @param cm The class to get the definition URI of.
	 * @return The definition URI for the specified class.
	 */
	public java.net.URI getBeanDefUri(ClassMeta<?> cm) {
		return getBeanDefMapper().getURI(cm);
	}

	/**
	 * Returns the definition URI for the specified class.
	 *
	 * @param id The definition ID to get the definition URI of.
	 * @return The definition URI for the specified class.
	 */
	public java.net.URI getBeanDefUri(String id) {
		return getBeanDefMapper().getURI(id);
	}

	/**
	 * Returns the definitions that were gathered during this session.
	 *
	 * <p>
	 * This map is modifiable and affects the map in the session.
	 *
	 * @return
	 * 	The definitions that were gathered during this session, or <jk>null</jk> if {@link JsonSchemaGenerator.Builder#useBeanDefs()} was not enabled.
	 */
	public Map<String,JsonMap> getBeanDefs() {
		return defs;
	}

	/**
	 * Adds a schema definition to this session.
	 *
	 * @param id The definition ID.
	 * @param def The definition schema.
	 * @return This object.
	 */
	public JsonSchemaGeneratorSession addBeanDef(String id, JsonMap def) {
		if (defs != null)
			defs.put(id, def);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add descriptions to types.
	 *
	 * @see JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() {
		return ctx.getAddDescriptionsTo();
	}

	/**
	 * Add examples.
	 *
	 * @see JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() {
		return ctx.getAddExamplesTo();
	}

	/**
	 * Allow nested descriptions.
	 *
	 * @see JsonSchemaGenerator.Builder#allowNestedDescriptions()
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() {
		return ctx.isAllowNestedDescriptions();
	}

	/**
	 * Allow nested examples.
	 *
	 * @see JsonSchemaGenerator.Builder#allowNestedExamples()
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() {
		return ctx.isAllowNestedExamples();
	}

	/**
	 * Bean schema definition mapper.
	 *
	 * @see JsonSchemaGenerator.Builder#beanDefMapper(Class)
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final BeanDefMapper getBeanDefMapper() {
		return ctx.getBeanDefMapper();
	}

	/**
	 * Ignore types from schema definitions.
	 *
	 * @see JsonSchemaGenerator.Builder#ignoreTypes(String...)
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	protected final List<Pattern> getIgnoreTypes() {
		return ctx.getIgnoreTypes();
	}

	/**
	 * Use bean definitions.
	 *
	 * @see JsonSchemaGenerator.Builder#useBeanDefs()
	 * @return
	 * 	<jk>true</jk> if schemas on beans will be serialized with <js>'$ref'</js> tags.
	 */
	protected final boolean isUseBeanDefs() {
		return ctx.isUseBeanDefs();
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
	public JsonSchemaClassMeta getJsonSchemaClassMeta(ClassMeta<?> cm) {
		return ctx.getJsonSchemaClassMeta(cm);
	}

	/**
	 * Returns the language-specific metadata on the specified bean property.
	 *
	 * @param bpm The bean property to return the metadata on.
	 * @return The metadata.
	 */
	public JsonSchemaBeanPropertyMeta getJsonSchemaBeanPropertyMeta(BeanPropertyMeta bpm) {
		return ctx.getJsonSchemaBeanPropertyMeta(bpm);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private ClassMeta<?> toClassMeta(Object o) {
		if (o instanceof Type)
			return getClassMeta((Type)o);
		return getClassMetaForObject(o);
	}
}
