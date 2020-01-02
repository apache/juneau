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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * Allows arbitrary objects to be mapped to classes and methods base on class/method name keys.
 *
 * @param <V> The type of object in this map.
 */
public class ReflectionMap<V> {

	private final List<ClassEntry<V>> classEntries;
	private final List<MemberEntry<V>> memberEntries;
	private final boolean noClassEntries, noMemberEntries;

	/**
	 * Constructor.
	 *
	 * @param b Initializer object.
	 */
	ReflectionMap(Builder<V> b) {
		this.classEntries = Collections.unmodifiableList(new ArrayList<>(b.classEntries));
		this.memberEntries = Collections.unmodifiableList(new ArrayList<>(b.memberEntries));
		this.noClassEntries = classEntries.isEmpty();
		this.noMemberEntries = memberEntries.isEmpty();
	}

	/**
	 * Static builder creator.
	 *
	 * @param <V> The type of object in this map.
	 * @param c The type of object in this map.
	 * @return A new instance of this object.
	 */
	public static <V> ReflectionMap.Builder<V> create(Class<V> c) {
		return new ReflectionMap.Builder<>();
	}

	/**
	 * Creates a new builder object for {@link ReflectionMap} objects.
	 *
	 * @param <V> The type of object in this map.
	 */
	public static class Builder<V> {
		List<ClassEntry<V>> classEntries = new ArrayList<>();
		List<MemberEntry<V>> memberEntries = new ArrayList<>();

		/**
		 * Adds a mapping to this builder.
		 *
		 * @param key
		 * 	The mapping key.
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>Full class name (e.g. <js>"com.foo.MyClass"</js>).
		 * 		<li>Simple class name (e.g. <js>"MyClass"</js>).
		 * 		<li>Full method name (e.g. <js>"com.foo.MyClass.myMethod"</js>).
		 * 		<li>Simple method name (e.g. <js>"MyClass.myMethod"</js>).
		 * 		<li>A comma-delimited list of anything on this list.
		 * 	</ul>
		 * @param value The value for this mapping.
		 * @return This object (for method chaining).
		 */
		public Builder<V> append(String key, V value) {
			if (isEmpty(key))
				throw new RuntimeException("Invalid reflection signature: [" + key + "]");
			for (String s : split(key)) {
				if (s.endsWith("."))
					throw new RuntimeException("Invalid reflection signature: [" + key + "]");
				int i = s.lastIndexOf('.');
				if (i == -1) {
					classEntries.add(new ClassEntry<>(s, value));
				} else {
					String s1 = s.substring(0, i), s2 = s.substring(i+1);
					if (Character.isUpperCase(s2.charAt(0))) {
						classEntries.add(new ClassEntry<>(s, value));
					} else {
						memberEntries.add(new MemberEntry<>(s1, s2, value));
					}
				}
			}

			return this;
		}

		/**
		 * Create new instance of {@link ReflectionMap} based on the contents of this builder.
		 *
		 * @return A new {@link ReflectionMap} object.
		 */
		public ReflectionMap<V> build() {
			return new ReflectionMap<>(this);
		}
	}

	/**
	 * Finds all entries in this map that matches the specified class.
	 *
	 * @param c The class to test for.
	 * @return All matching objects.  Never <jk>null</jk>.
	 */
	public List<V> find(Class<?> c) {
		return noClassEntries ? Collections.emptyList() : classEntries.stream().filter(x -> x.matches(c)).map(x -> x.value).collect(Collectors.toList());
	}

	/**
	 * Finds first in this map that matches the specified class.
	 *
	 * @param c The class to test for.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> findFirst(Class<?> c) {
		return noClassEntries ? Optional.empty() : classEntries.stream().filter(x -> x.matches(c)).map(x -> x.value).findFirst();
	}

	/**
	 * Finds all entries in this map that matches the specified class.
	 *
	 * @param c The class to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return All matching objects.  Never <jk>null</jk>.
	 */
	public List<V> find(Class<?> c, Class<? extends V> ofType) {
		return noClassEntries ? Collections.emptyList() : classEntries.stream().filter(x -> x.matches(c)).map(x -> x.value).filter(x -> ofType.isInstance(x)).collect(Collectors.toList());
	}

	/**
	 * Finds first in this map that matches the specified class.
	 *
	 * @param c The class to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> findFirst(Class<?> c, Class<? extends V> ofType) {
		return noClassEntries ? Optional.empty() : classEntries.stream().filter(x -> x.matches(c)).map(x -> x.value).filter(x -> ofType.isInstance(x)).findFirst();
	}

	/**
	 * Finds all entries in this map that matches the specified method.
	 *
	 * @param m The method to test for.
	 * @return All matching objects.  Never <jk>null</jk>.
	 */
	public List<V> find(Member m) {
		return noMemberEntries ? Collections.emptyList() : memberEntries.stream().filter(x -> x.matches(m)).map(x -> x.value).collect(Collectors.toList());
	}

	/**
	 * Finds first entry in this map that matches the specified method.
	 *
	 * @param m The method to test for.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> findFirst(Member m) {
		return noMemberEntries ? Optional.empty() : memberEntries.stream().filter(x -> x.matches(m)).map(x -> x.value).findFirst();
	}

	/**
	 * Finds all entries in this map that matches the specified method.
	 *
	 * @param m The method to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return All matching objects.  Never <jk>null</jk>.
	 */
	public List<V> find(Member m, Class<? extends V> ofType) {
		return noMemberEntries ? Collections.emptyList() : memberEntries.stream().filter(x -> x.matches(m)).map(x -> x.value).filter(x -> ofType.isInstance(x)).collect(Collectors.toList());
	}

	/**
	 * Finds first entries in this map that matches the specified method.
	 *
	 * @param m The method to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> findFirst(Member m, Class<? extends V> ofType) {
		return noMemberEntries ? Optional.empty() : memberEntries.stream().filter(x -> x.matches(m)).map(x -> x.value).filter(x -> ofType.isInstance(x)).findFirst();
	}

	static class ClassEntry<V> {
		String className;
		V value;

		ClassEntry(String className, V value) {
			this.className = className;
			this.value = value;
		}

		public boolean matches(Class<?> c) {
			return c != null && (className.equals(c.getName()) || className.equals(c.getSimpleName()));
		}
	}

	static class MemberEntry<V> {
		String className, methodName;
		V value;

		MemberEntry(String className, String methodName, V value) {
			this.className = className;
			this.methodName = methodName;
			this.value = value;
		}

		public boolean matches(Member m) {
			if (m == null)
				return false;
			Class<?> c = m.getDeclaringClass();
			return m.getName().equals(methodName) && (c.getName().equals(className) || c.getSimpleName().equals(className));
		}
	}
}
