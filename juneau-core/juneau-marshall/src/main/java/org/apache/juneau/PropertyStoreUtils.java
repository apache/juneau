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

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.parser.*;

/**
 * Utility methods for working with property store properties.
 */
public class PropertyStoreUtils {
	
	/**
	 * Merges a new object into an existing set.
	 * 
	 * @param oldSet The previous set.
	 * @param pc The property converter to use to convert values in the new value.
	 * @param o The new value to add.
	 * @return A new set containing the merged contents.
	 * @throws ParseException
	 */
	public static Set<Object> merge(Set<Object> oldSet, PropertyConverter<?> pc, Object o) throws ParseException {
		return merge(oldSet, new LinkedHashSet<>(), pc, o);
	}

	@SuppressWarnings("unchecked")
	static Set<Object> merge(Set<Object> oldSet, Set<Object> newSet, PropertyConverter<?> pc, Object o) throws ParseException {
		if (o == null) {
			// Ignore.
		} else if (o.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				merge(oldSet, newSet, pc, Array.get(o, i));
		} else if (o instanceof Collection) {
			for (Object o2 : (Collection<Object>)o)
				merge(oldSet, newSet, pc, o2);
		} else if (isObjectList(o)) {
			merge(oldSet, newSet, pc, new ObjectList(o.toString()));
		} else if (isNone(o)) {
			newSet.clear();
		} else if (isInherit(o)) {
			if (oldSet != null)
				for (Object o2 : oldSet)
					newSet.add(o2);
		} else {
			newSet.add(pc == null ? o : pc.convert(o));
		}
		return newSet;
	}

	/**
	 * Merges a new object into an existing list.
	 * 
	 * @param oldList The previous list.
	 * @param pc The property converter to use to convert values in the new value.
	 * @param o The new value to add.
	 * @return A new list containing the merged contents.
	 * @throws ParseException
	 */
	public static List<Object> merge(List<Object> oldList, PropertyConverter<?> pc, Object o) throws ParseException {
		return merge(oldList, new ArrayList<>(), pc, o);
	}

	@SuppressWarnings("unchecked")
	private static List<Object> merge(List<Object> oldList, List<Object> newList, PropertyConverter<?> pc, Object o) throws ParseException {
		if (o == null) {
			// Ignore
		} else if (o.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				merge(oldList, newList, pc, Array.get(o, i));
		} else if (o instanceof Collection) {
			for (Object o2 : (Collection<Object>)o)
				merge(oldList, newList, pc, o2);
		} else if (isIndexed(o)) {
			Matcher lm = INDEXED_LINK_PATTERN.matcher(o.toString());
			lm.matches();
			String key = lm.group(1);
			int i2 = Math.min(newList.size(), Integer.parseInt(lm.group(2)));
			String remainder = lm.group(3);
			newList.add(i2, key.isEmpty() ? remainder : key + ":" + remainder);
		} else if (isObjectList(o)) {
			merge(oldList, newList, pc, new ObjectList(o.toString()));
		} else if (isNone(o)) {
			newList.clear();
		} else if (isInherit(o)) {
			if (oldList != null)
				for (Object o2 : oldList)
					newList.add(o2);
		} else {
			o = pc == null ? o : pc.convert(o);
			newList.remove(o);
			newList.add(o);
		}
		return newList;
	}
	
	//-------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-------------------------------------------------------------------------------------------------------------------
	
	private static boolean isObjectList(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return (s.startsWith("[") && s.endsWith("]") && BeanContext.DEFAULT != null);
		}
		return false;
	}

	private static boolean isNone(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return "NONE".equals(s);
		}
		return false;
	}

	private static boolean isIndexed(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return s.indexOf('[') != -1 && INDEXED_LINK_PATTERN.matcher(s).matches();
		}
		return false;
	}

	private static final Pattern INDEXED_LINK_PATTERN = Pattern.compile("(?s)(\\S*)\\[(\\d+)\\]\\:(.*)");
	
	private static boolean isInherit(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString();
			return "INHERIT".equals(s);
		}
		return false;
	}
}
