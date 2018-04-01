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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.jsonschema.JsonSchemaSerializer.*;
import static org.apache.juneau.jsonschema.TypeCategory.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link JsonSchemaSerializer}.
 * 
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class JsonSchemaSerializerSession extends JsonSerializerSession {

	private final boolean useBeanDefs, allowNestedExamples, allowNestedDescriptions;
	private final BeanDefMapper beanDefMapper;
	private final Map<String,ObjectMap> defs;
	private final Map<String,ObjectMap> defaultSchemas;
	private final Set<TypeCategory> addExamples, addDescriptions;

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
	protected JsonSchemaSerializerSession(JsonSchemaSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		useBeanDefs = getProperty(JSONSCHEMA_useBeanDefs, boolean.class, ctx.useBeanDefs);
		allowNestedExamples = getProperty(JSONSCHEMA_allowNestedExamples, boolean.class, ctx.allowNestedExamples);
		allowNestedDescriptions = getProperty(JSONSCHEMA_allowNestedDescriptions, boolean.class, ctx.allowNestedDescriptions);
		beanDefMapper = getInstanceProperty(JSONSCHEMA_beanDefMapper, BeanDefMapper.class, ctx.beanDefMapper);
		defaultSchemas = getProperty(JSONSCHEMA_defaultSchemas, Map.class, ctx.defaultSchemas);
		addExamples = getProperty(JSONSCHEMA_addExamples, Set.class, ctx.addExamples);
		addDescriptions = getProperty(JSONSCHEMA_addDescriptions, Set.class, ctx.addDescriptions);
		defs = useBeanDefs ? new LinkedHashMap<String,ObjectMap>() : null;
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		ObjectMap schema = getSchema(getClassMetaForObject(o), "root", null, false, false, null);
		serializeAnything(getJsonWriter(out), schema, getExpectedRootType(o), "root", null);
	}

	/**
	 * Returns the JSON-schema for the specified type.
	 * 
	 * @param type The object type.
	 * @return The schema for the type.
	 * @throws Exception
	 */
	public ObjectMap getSchema(Type type) throws Exception {
		return getSchema(getClassMeta(type), "root", null, false, false, null);
	}
	
	/**
	 * Returns the JSON-schema for the specified type.
	 * 
	 * @param cm The object type.
	 * @return The schema for the type.
	 * @throws Exception
	 */
	public ObjectMap getSchema(ClassMeta<?> cm) throws Exception {
		return getSchema(cm, "root", null, false, false, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ObjectMap getSchema(ClassMeta<?> eType, String attrName, String[] pNames, boolean exampleAdded, boolean descriptionAdded, JsonSchemaBeanPropertyMeta jsbpm) throws Exception {
		
		ObjectMap out = new ObjectMap();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> sType;			// The serialized type
		PojoSwap pojoSwap = eType.getPojoSwap(this);
		
		aType = push(attrName, eType, null);

		sType = eType.getSerializedClassMeta(this);
		String type = null, format = null;
		Object example = null, description = null;

		boolean useDef = useBeanDefs && sType.isBean() && pNames == null;
		
		if (useDef && defs.containsKey(getBeanDefId(sType))) 
			return new ObjectMap().append("$ref", getBeanDefUri(sType));
		
		ObjectMap ds = defaultSchemas.get(sType.getInnerClass().getName());
		if (ds != null && ds.containsKey("type")) 
			return out.appendAll(ds);

		JsonSchemaClassMeta jscm = null;
		if (pojoSwap != null && pojoSwap.getClass().getAnnotation(JsonSchema.class) != null)
			jscm = getClassMeta(pojoSwap.getClass()).getExtendedMeta(JsonSchemaClassMeta.class);
		if (jscm == null)
			jscm = sType.getExtendedMeta(JsonSchemaClassMeta.class);
			
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
		}

		// Add info from @JsonSchema on bean property.
		if (jsbpm != null) {
			out.appendIf(false, true, true, "type", jsbpm.getType());
			out.appendIf(false, true, true, "format", jsbpm.getFormat());
		}
		
		out.appendIf(false, true, true, "type", jscm.getType());
		out.appendIf(false, true, true, "format", jscm.getFormat());
		
		out.appendIf(false, true, true, "type", type);
		out.appendIf(false, true, true, "format", format);
		
		if (aType != null) {

			example = getExample(sType, tc, exampleAdded);
			description = getDescription(sType, tc, descriptionAdded);
			exampleAdded |= example != null;
			descriptionAdded |= description != null;
			
			if (tc == BEAN) {
				ObjectMap properties = new ObjectMap();
				BeanMeta bm = getBeanMeta(sType.getInnerClass());
				if (pNames != null)
					bm = new BeanMetaFiltered(bm, pNames);
				for (Iterator<BeanPropertyMeta> i = bm.getPropertyMetas().iterator(); i.hasNext();) {
					BeanPropertyMeta p = i.next();
					if (p.canRead()) 
						properties.put(p.getName(), getSchema(p.getClassMeta(), p.getName(), p.getProperties(), exampleAdded, descriptionAdded, p.getExtendedMeta(JsonSchemaBeanPropertyMeta.class)));
				}
				out.put("properties", properties);
				
			} else if (tc == COLLECTION) {
				ClassMeta et = sType.getElementType();
				if (sType.isCollection() && isParentClass(Set.class, sType.getInnerClass()))
					out.put("uniqueItems", true);
				out.put("items", getSchema(et, "items", pNames, exampleAdded, descriptionAdded, null));

			} else if (tc == ARRAY) {
				ClassMeta et = sType.getElementType();
				if (sType.isCollection() && isParentClass(Set.class, sType.getInnerClass()))
					out.put("uniqueItems", true);
				out.put("items", getSchema(et, "items", pNames, exampleAdded, descriptionAdded, null));
				
			} else if (tc == ENUM) {
				out.put("enum", getEnums(sType));
				
			} else if (tc == MAP) {
				ObjectMap om = getSchema(sType.getValueType(), "additionalProperties", null, exampleAdded, descriptionAdded, null);
				if (! om.isEmpty())
					out.put("additionalProperties", om);

			}
		}

		// Add info from @JsonSchema on bean property.
		if (jsbpm != null) {
			out.appendIf(false, true, true, "description", jsbpm.getDescription());
			out.appendIf(false, true, true, "example", jsbpm.getExample());
		}

		out.appendIf(false, true, true, "description", jscm.getDescription());
		out.appendIf(false, true, true, "example", jscm.getExample());

		out.appendIf(false, true, true, "description", description);
		out.appendIf(false, true, true, "example", example);

		if (ds != null)
			out.appendAll(ds);
		
		if (useDef) {
			defs.put(getBeanDefId(sType), out);
			out = new ObjectMap().append("$ref", getBeanDefUri(sType));
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
	
	private Object getExample(ClassMeta<?> sType, TypeCategory t, boolean exampleAdded) throws Exception {
		boolean canAdd = allowNestedExamples || ! exampleAdded;
		if (canAdd && (addExamples.contains(t) || addExamples.contains(ANY))) {
			Object example = sType.getExample(this);
			if (example != null)
				return JsonParser.DEFAULT.parse(serializeJson(example), Object.class);
		}
		return null;
	}
	
	private Object getDescription(ClassMeta<?> sType, TypeCategory t, boolean descriptionAdded) {
		boolean canAdd = allowNestedDescriptions || ! descriptionAdded;
		if (canAdd && (addDescriptions.contains(t) || addDescriptions.contains(ANY)))
			return sType.getReadableName();
		return null;
	}
	
	/**
	 * Returns the definition ID for the specified class.
	 * 
	 * @param cm The class to get the definition ID of.
	 * @return The definition ID for the specified class.
	 */
	public String getBeanDefId(ClassMeta<?> cm) {
		return beanDefMapper.getId(cm);
	}

	/**
	 * Returns the definition URI for the specified class.
	 * 
	 * @param cm The class to get the definition URI of.
	 * @return The definition URI for the specified class.
	 */
	public java.net.URI getBeanDefUri(ClassMeta<?> cm) {
		return beanDefMapper.getURI(cm);
	}

	/**
	 * Returns the definition URI for the specified class.
	 * 
	 * @param id The definition ID to get the definition URI of.
	 * @return The definition URI for the specified class.
	 */
	public java.net.URI getBeanDefUri(String id) {
		return beanDefMapper.getURI(id);
	}
	
	/**
	 * Returns the definitions that were gathered during this session.
	 * 
	 * <p>
	 * This map is modifiable and affects the map in the session.
	 * 
	 * @return 
	 * 	The definitions that were gathered during this session, or <jk>null</jk> if {@link JsonSchemaSerializer#JSONSCHEMA_useBeanDefs} was not enabled.
	 */
	public Map<String,ObjectMap> getBeanDefs() {
		return defs;
	}
	
	/**
	 * Adds a schema definition to this session.
	 * 
	 * @param id The definition ID.
	 * @param def The definition schema.
	 * @return This object (for method chaining).
	 */
	public JsonSchemaSerializerSession addBeanDef(String id, ObjectMap def) {
		if (defs != null)
			defs.put(id, def);
		return this;
	}
}
