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
package org.apache.juneau;

import static org.apache.juneau.PropertyConverter.*;

import java.util.*;

/**
 * Represents possible property types of properties in a {@link PropertyStore}.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public enum PropertyType {

	/** String */
	STRING("String", "s", STRING_CONVERTER),

	/** Boolean */
	BOOLEAN("Boolean", "b", BOOLEAN_CONVERTER),

	/** Integer */
	INTEGER("Integer", "i", INTEGER_CONVERTER),

	/** Class */
	CLASS("Class", "c", CLASS_CONVERTER),

	/** Object */
	OBJECT("Object", "o", OBJECT_CONVERTER),

	/** Set&lt;String&gt; */
	SET_STRING("Set<String>", "ss", STRING_CONVERTER),

	/** Set&lt;Integer&gt; */
	SET_INTEGER("Set<Integer>", "si", INTEGER_CONVERTER),

	/** Set&lt;Class&gt; */
	SET_CLASS("Set<Class>", "sc", CLASS_CONVERTER),

	/** List&lt;String&gt; */
	LIST_STRING("List<String>", "ls", STRING_CONVERTER),

	/** List&lt;Integer&gt; */
	LIST_INTEGER("List<Integer>", "li", INTEGER_CONVERTER),

	/** List&lt;Class&gt; */
	LIST_CLASS("List<Class>", "lc", CLASS_CONVERTER),

	/** List&lt;Object&gt; */
	LIST_OBJECT("List<Object>", "lo", OBJECT_CONVERTER),

	/** TreeMap&lt;String,String&gt; */
	SORTED_MAP_STRING("Map<String,String>", "sms", STRING_CONVERTER),

	/** TreeMap&lt;String,Integer&gt; */
	SORTED_MAP_INTEGER("Map<String,Integer>", "smi", INTEGER_CONVERTER),

	/** TreeMap&lt;String,Class&gt; */
	SORTED_MAP_CLASS("Map<String,Class>", "smc", CLASS_CONVERTER),

	/** TreeMap&lt;String,Object&gt; */
	SORTED_MAP_OBJECT("Map<String,Object>", "smo", OBJECT_CONVERTER),

	/** LinkedHashMap&lt;String,String&gt; */
	ORDERED_MAP_STRING("Map<String,String>", "oms", STRING_CONVERTER),

	/** LinkedHashMap&lt;String,Integer&gt; */
	ORDERED_MAP_INTEGER("Map<String,Integer>", "omi", INTEGER_CONVERTER),

	/** LinkedHashMap&lt;String,Class&gt; */
	ORDERED_MAP_CLASS("Map<String,Class>", "omc", CLASS_CONVERTER),

	/** LinkedHashMap&lt;String,Object&gt; */
	ORDERED_MAP_OBJECT("Map<String,Object>", "omo", OBJECT_CONVERTER);

	private final String type, suffix;
	final PropertyConverter<?> converter;

	private PropertyType(String type, String suffix, PropertyConverter<?> converter) {
		this.type = type;
		this.suffix = suffix;
		this.converter = converter;
	}

	@Override /* Override */
	public String toString() {
		return type;
	}

	String getSuffix() {
		return suffix;
	}

	/**
	 * Returns the comparator to use for entries in this property.
	 *
	 * @return The comparator to use for entries in this property, or <jk>null</jk> to use the default comparator.
	 */
	Comparator<? super Object> comparator() {
		switch(this) {
			case SET_CLASS: return CLASS_COMPARATOR;
			default: return null;
		}
	}

	private static final Comparator CLASS_COMPARATOR = new Comparator<Class<?>>() {
		@Override /* Comparator */
		public int compare(Class<?> object1, Class<?> object2) {
			return object1.getName().compareTo(object2.getName());
		}
	};
}