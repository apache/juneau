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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;

/**
 * Utility class for JSON-schema.
 */
public class JsonSchemaUtils {
	
	/**
	 * Returns the JSON-schema for the specified type.
	 * 
	 * @param bs The current bean session.
	 * @param type The object type.
	 * @return The schema for the type.
	 * @throws Exception
	 */
	public static ObjectMap getSchema(BeanSession bs, Type type) throws Exception {
		return getSchema(bs, bs.getClassMeta(type), null, null);
	}
	
	/**
	 * Returns the JSON-schema for the specified type.
	 * 
	 * @param bs The current bean session.
	 * @param cm The object type.
	 * @return The schema for the type.
	 * @throws Exception
	 */
	public static ObjectMap getSchema(BeanSession bs, ClassMeta<?> cm) throws Exception {
		return getSchema(bs, cm, null, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ObjectMap getSchema(BeanSession bs, ClassMeta<?> eType, String attrName, String[] pNames) throws Exception {
		ObjectMap out = new ObjectMap();

		if (eType == null)
			eType = bs.object();

		ClassMeta<?> aType;			// The actual type (will be null if recursion occurs)
		ClassMeta<?> sType;			// The serialized type
		
		aType = eType;

		sType = eType.getSerializedClassMeta(bs);
		String type = null, format = null;

		if (sType.isEnum() || sType.isCharSequence() || sType.isChar()) {
			type = "string";
		} else if (sType.isNumber()) {
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
			type = "boolean";
		} else if (sType.isMapOrBean()) {
			type = "object";
		} else if (sType.isCollectionOrArray()) {
			type = "array";
		} else {
			type = "string";
		}

		out.put("type", type);
		out.appendIfNotNull("format", format);
		
		out.put("description", eType.toString());
		PojoSwap f = eType.getPojoSwap(bs);
		if (f != null)
			out.put("transform", f);

		if (aType != null) {
			if (sType.isEnum())
				out.put("enum", getEnumStrings((Class<Enum<?>>)sType.getInnerClass()));
			else if (sType.isCollectionOrArray()) {
				ClassMeta componentType = sType.getElementType();
				if (sType.isCollection() && isParentClass(Set.class, sType.getInnerClass()))
					out.put("uniqueItems", true);
				out.put("items", getSchema(bs, componentType, "items", pNames));
			} else if (sType.isBean()) {
				ObjectMap properties = new ObjectMap();
				BeanMeta bm = bs.getBeanMeta(sType.getInnerClass());
				if (pNames != null)
					bm = new BeanMetaFiltered(bm, pNames);
				for (Iterator<BeanPropertyMeta> i = bm.getPropertyMetas().iterator(); i.hasNext();) {
					BeanPropertyMeta p = i.next();
					if (p.canRead())
						properties.put(p.getName(), getSchema(bs, p.getClassMeta(), p.getName(), p.getProperties()));
				}
				out.put("properties", properties);
			}
		}
		return out;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<String> getEnumStrings(Class<? extends Enum> c) {
		List<String> l = new LinkedList<>();
		for (Object e : EnumSet.allOf(c))
			l.add(e.toString());
		return l;
	}
}
