/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.html;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.serializer.SerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Serializes POJO metamodels to HTML.
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/html+schema</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/html</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Essentially the same as {@link HtmlSerializer}, except serializes the POJO metamodel
 * 		instead of the model itself.
 * <p>
 * 	Produces output that describes the POJO metamodel similar to an XML schema document.
 * <p>
 * 	The easiest way to create instances of this class is through the {@link HtmlSerializer#getSchemaSerializer()},
 * 		which will create a schema serializer with the same settings as the originating serializer.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Produces(value="text/html+schema", contentType="text/html")
public final class HtmlSchemaDocSerializer extends HtmlDocSerializer {

	/**
	 * Constructor.
	 */
	public HtmlSchemaDocSerializer() {
		setProperty(SERIALIZER_detectRecursions, true);
		setProperty(SERIALIZER_ignoreRecursions, true);
	}

	/**
	 * Constructor.
	 *
	 * @param cf The context factory to use for creating the context for this serializer.
	 */
	public HtmlSchemaDocSerializer(ContextFactory cf) {
		getContextFactory().copyFrom(cf);
		setProperty(SERIALIZER_detectRecursions, true);
		setProperty(SERIALIZER_ignoreRecursions, true);
	}

	@Override /* Serializer */
	public HtmlDocSerializerSession createSession(Object output, ObjectMap properties, Method javaMethod) {
		return new HtmlDocSerializerSession(getContext(HtmlDocSerializerContext.class), getBeanContext(), output, properties, javaMethod);
	}

	@Override /* ISchemaSerializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		HtmlSerializerSession s = (HtmlSerializerSession)session;
		ObjectMap schema = getSchema(s, s.getBeanContext().getClassMetaForObject(o), "root", null);
		super.doSerialize(s, schema);
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
	private ObjectMap getSchema(HtmlSerializerSession session, ClassMeta<?> eType, String attrName, String[] pNames) throws Exception {

		ObjectMap out = new ObjectMap();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> gType;			// The generic type

		aType = session.push(attrName, eType, null);

		gType = eType.getTransformedClassMeta();
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
		out.put("class", eType.toString());
		PojoSwap t = eType.getPojoSwap();
		if (t != null)
			out.put("transform", t);

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
}
