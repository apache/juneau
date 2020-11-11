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

import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.jsonschema.TypeCategory.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSchemaSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class JsonSchemaGeneratorSession extends BeanTraverseSession {

	private final JsonSchemaGenerator ctx;
	private final Map<String,OMap> defs;
	private JsonSerializerSession jsSession;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected JsonSchemaGeneratorSession(JsonSchemaGenerator ctx, BeanSessionArgs args) {
		super(ctx, args);
		this.ctx = ctx;
		if (isUseBeanDefs())
			defs = new TreeMap<>();
		else
			defs = null;
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
	public OMap getSchema(Object o) throws BeanRecursionException, SerializeException {
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
	public OMap getSchema(Type type) throws BeanRecursionException, SerializeException {
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
	public OMap getSchema(ClassMeta<?> cm) throws BeanRecursionException, SerializeException {
		return getSchema(cm, "root", null, false, false, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private OMap getSchema(ClassMeta<?> eType, String attrName, String[] pNames, boolean exampleAdded, boolean descriptionAdded, JsonSchemaBeanPropertyMeta jsbpm) throws BeanRecursionException, SerializeException {

		if (ctx.isIgnoredType(eType))
			return null;

		OMap out = new OMap();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> sType;			// The serialized type
		PojoSwap pojoSwap = eType.getSwap(this);

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
			return new OMap().a("$ref", getBeanDefUri(sType));
		}

		JsonSchemaClassMeta jscm = null;
		ClassMeta pojoSwapCM = pojoSwap == null ? null : getClassMeta(pojoSwap.getClass());
		if (pojoSwapCM != null && pojoSwapCM.hasAnnotation(Schema.class))
			jscm = getJsonSchemaClassMeta(pojoSwapCM);
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
			out.appendAll(jsbpm.getSchema());
		}

		out.appendAll(jscm.getSchema());

		out.appendIf(false, true, true, "type", type);
		out.appendIf(false, true, true, "format", format);

		if (aType != null) {

			example = getExample(sType, tc, exampleAdded);
			description = getDescription(sType, tc, descriptionAdded);
			exampleAdded |= example != null;
			descriptionAdded |= description != null;

			if (tc == BEAN) {
				OMap properties = new OMap();
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
				OMap om = getSchema(sType.getValueType(), "additionalProperties", null, exampleAdded, descriptionAdded, null);
				if (! om.isEmpty())
					out.put("additionalProperties", om);

			}
		}

		out.appendAll(jscm.getSchema());

		out.appendIf(false, true, true, "description", description);
		out.appendIf(false, true, true, "x-example", example);

//		if (ds != null)
//			out.appendAll(ds);

		if (useDef) {
			defs.put(getBeanDefId(sType), out);
			out = OMap.of("$ref", getBeanDefUri(sType));
		}

		pop();

		return out;
	}

	private List<String> getEnums(ClassMeta<?> cm) {
		List<String> l = new ArrayList<>();
		for (Enum<?> e : getEnumConstants(cm.getInnerClass()))
			l.add(cm.toString(e));
		return l;
	}

	private Object getExample(ClassMeta<?> sType, TypeCategory t, boolean exampleAdded) throws SerializeException {
		boolean canAdd = isAllowNestedExamples() || ! exampleAdded;
		if (canAdd && (getAddExamplesTo().contains(t) || getAddExamplesTo().contains(ANY))) {
			Object example = sType.getExample(this);
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
			jsSession = ctx.getJsonSerializer().createSession(null);
		return jsSession.serializeToString(o);
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
	 * 	The definitions that were gathered during this session, or <jk>null</jk> if {@link JsonSchemaGenerator#JSONSCHEMA_useBeanDefs} was not enabled.
	 */
	public Map<String,OMap> getBeanDefs() {
		return defs;
	}

	/**
	 * Adds a schema definition to this session.
	 *
	 * @param id The definition ID.
	 * @param def The definition schema.
	 * @return This object (for method chaining).
	 */
	public JsonSchemaGeneratorSession addBeanDef(String id, OMap def) {
		if (defs != null)
			defs.put(id, def);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add descriptions to types.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_addDescriptionsTo
	 * @return
	 * 	Set of categories of types that descriptions should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddDescriptionsTo() {
		return ctx.getAddDescriptionsTo();
	}

	/**
	 * Configuration property:  Add examples.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_addExamplesTo
	 * @return
	 * 	Set of categories of types that examples should be automatically added to generated schemas.
	 */
	protected final Set<TypeCategory> getAddExamplesTo() {
		return ctx.getAddExamplesTo();
	}

	/**
	 * Configuration property:  Allow nested descriptions.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_allowNestedDescriptions
	 * @return
	 * 	<jk>true</jk> if nested descriptions are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedDescriptions() {
		return ctx.isAllowNestedDescriptions();
	}

	/**
	 * Configuration property:  Allow nested examples.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_allowNestedExamples
	 * @return
	 * 	<jk>true</jk> if nested examples are allowed in schema definitions.
	 */
	protected final boolean isAllowNestedExamples() {
		return ctx.isAllowNestedExamples();
	}

	/**
	 * Configuration property:  Bean schema definition mapper.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_beanDefMapper
	 * @return
	 * 	Interface to use for converting Bean classes to definition IDs and URIs.
	 */
	protected final BeanDefMapper getBeanDefMapper() {
		return ctx.getBeanDefMapper();
	}

	/**
	 * Configuration property:  Ignore types from schema definitions.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_ignoreTypes
	 * @return
	 * 	Custom schema information for particular class types.
	 */
	protected final Set<Pattern> getIgnoreTypes() {
		return ctx.getIgnoreTypes();
	}

	/**
	 * Configuration property:  Use bean definitions.
	 *
	 * @see JsonSchemaGenerator#JSONSCHEMA_useBeanDefs
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

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("JsonSchemaGeneratorSession", new DefaultFilteringOMap()
			);
	}
}
