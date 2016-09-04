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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.serializer.SerializerContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO metadata to HTTP responses as JSON.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>application/json+schema, text/json+schema</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>application/json</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Produces the JSON-schema for the JSON produced by the {@link JsonSerializer} class with the same properties.
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces(value={"application/json+schema","text/json+schema"},contentType="application/json")
public final class JsonSchemaSerializer extends JsonSerializer {

	/**
	 * Constructor.
	 */
	public JsonSchemaSerializer() {
		setProperty(SERIALIZER_detectRecursions, true);
		setProperty(SERIALIZER_ignoreRecursions, true);
	}

	/**
	 * Constructor.
	 *
	 * @param config Initialize with the specified config property store.
	 */
	public JsonSchemaSerializer(ContextFactory config) {
		getContextFactory().copyFrom(config);
		setProperty(SERIALIZER_detectRecursions, true);
		setProperty(SERIALIZER_ignoreRecursions, true);
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* JsonSerializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		JsonSerializerSession s = (JsonSerializerSession)session;
		ObjectMap schema = getSchema(s, s.getBeanContext().getClassMetaForObject(o), "root", null);
		serializeAnything(s, s.getWriter(), schema, null, "root", null);
	}

	/*
	 * Creates a schema representation of the specified class type.
	 *
	 * @param eType The class type to get the schema of.
	 * @param ctx Serialize context used to prevent infinite loops.
	 * @param attrName The name of the current attribute.
	 * @return A schema representation of the specified class.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ObjectMap getSchema(JsonSerializerSession session, ClassMeta<?> eType, String attrName, String[] pNames) throws Exception {
		ObjectMap out = new ObjectMap();

		if (eType == null)
			eType = object();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> gType;			// The generic type

		aType = session.push(attrName, eType, null);

		gType = eType.getSerializedClassMeta();
		String type = null;

		if (gType.isEnum() || gType.isCharSequence() || gType.isChar())
			type = "string";
		else if (gType.isNumber())
			type = "number";
		else if (gType.isBoolean())
			type = "boolean";
		else if (gType.isBean() || gType.isMap())
			type = "object";
		else if (gType.isCollection() || gType.isArray())
			type = "array";
		else
			type = "any";

		out.put("type", type);
		out.put("description", eType.toString());
		PojoSwap f = eType.getPojoSwap();
		if (f != null)
			out.put("transform", f);

		if (aType != null) {
			if (gType.isEnum())
				out.put("enum", getEnumStrings((Class<Enum<?>>)gType.getInnerClass()));
			else if (gType.isCollection() || gType.isArray()) {
				ClassMeta componentType = gType.getElementType();
				if (gType.isCollection() && isParentClass(Set.class, gType.getInnerClass()))
					out.put("uniqueItems", true);
				out.put("items", getSchema(session, componentType, "items", pNames));
			} else if (gType.isBean()) {
				ObjectMap properties = new ObjectMap();
				BeanMeta bm = session.getBeanContext().getBeanMeta(gType.getInnerClass());
				if (pNames != null)
					bm = new BeanMetaFiltered(bm, pNames);
				for (Iterator<BeanPropertyMeta> i = bm.getPropertyMetas().iterator(); i.hasNext();) {
					BeanPropertyMeta p = i.next();
					properties.put(p.getName(), getSchema(session, p.getClassMeta(), p.getName(), p.getProperties()));
				}
				out.put("properties", properties);
			}
		}
		session.pop();
		return out;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> getEnumStrings(Class<? extends Enum> c) {
		List<String> l = new LinkedList<String>();
		for (Object e : EnumSet.allOf(c))
			l.add(e.toString());
		return l;
	}


	@Override /* Lockable */
	public JsonSchemaSerializer lock() {
		super.lock();
		return this;
	}
}
