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
package org.apache.juneau.html;

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;
import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSchemaDocSerializer} and its subclasses.
 *
 * <p>
 * See {@link SerializerContext} for details.
 *
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class HtmlSchemaDocSerializerSession extends HtmlDocSerializerSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 */
	protected HtmlSchemaDocSerializerSession(HtmlDocSerializerContext ctx, SerializerSessionArgs args) {
		super(ctx, args);
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		ObjectMap schema = getSchema(getClassMetaForObject(o), "root", null);
		super.doSerialize(out, schema);
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
	private ObjectMap getSchema(ClassMeta<?> eType, String attrName, String[] pNames) throws Exception {

		ObjectMap out = new ObjectMap();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> sType;			// The serialized type

		aType = push(attrName, eType, null);

		sType = eType.getSerializedClassMeta(this);
		String type = null;

		if (sType.isEnum() || sType.isCharSequence() || sType.isChar())
			type = "string";
		else if (sType.isNumber())
			type = "number";
		else if (sType.isBoolean())
			type = "boolean";
		else if (sType.isMapOrBean())
			type = "object";
		else if (sType.isCollectionOrArray())
			type = "array";
		else
			type = "any";

		out.put("type", type);
		out.put("class", eType.toString());
		PojoSwap t = eType.getPojoSwap(this);
		if (t != null)
			out.put("transform", t);

		if (aType != null) {
			if (sType.isEnum())
				out.put("enum", getEnumStrings((Class<Enum<?>>)sType.getInnerClass()));
			else if (sType.isCollectionOrArray()) {
				ClassMeta componentType = sType.getElementType();
				if (sType.isCollection() && isParentClass(Set.class, sType.getInnerClass()))
					out.put("uniqueItems", true);
				out.put("items", getSchema(componentType, "items", pNames));
			} else if (sType.isBean()) {
				ObjectMap properties = new ObjectMap();
				BeanMeta bm = getBeanMeta(sType.getInnerClass());
				if (pNames != null)
					bm = new BeanMetaFiltered(bm, pNames);
				for (Iterator<BeanPropertyMeta> i = bm.getPropertyMetas().iterator(); i.hasNext();) {
					BeanPropertyMeta p = i.next();
					properties.put(p.getName(), getSchema(p.getClassMeta(), p.getName(), p.getProperties()));
				}
				out.put("properties", properties);
			}
		}
		pop();
		return out;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<String> getEnumStrings(Class<? extends Enum> c) {
		List<String> l = new LinkedList<String>();
		for (Object e : EnumSet.allOf(c))
			l.add(e.toString());
		return l;
	}
}
