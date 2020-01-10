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
import static java.lang.Character.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Allows arbitrary objects to be mapped to classes and methods base on class/method name keys.
 *
 * <p>
 * The valid pattern matches are:
 * <ul>
 * 	<li>Classes:
 * 		<ul>
 * 			<li>Fully qualified: <js>"com.foo.MyClass"</js>
 * 			<li>Fully qualified inner class: <js>"com.foo.MyClass$Inner1$Inner2"</js>
 * 			<li>Simple: <js>"MyClass"</js>
 * 			<li>Simple inner: <js>"MyClass$Inner1$Inner2"</js> or <js>"Inner1$Inner2"</js> or <js>"Inner2"</js>
 * 		</ul>
 * 	<li>Methods:
 * 		<ul>
 * 			<li>Fully qualified with args: <js>"com.foo.MyClass.myMethod(String,int)"</js> or <js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js> or <js>"com.foo.MyClass.myMethod()"</js>
 * 			<li>Fully qualified: <js>"com.foo.MyClass.myMethod"</js>
 * 			<li>Simple with args: <js>"MyClass.myMethod(String,int)"</js> or <js>"MyClass.myMethod(java.lang.String,int)"</js> or <js>"MyClass.myMethod()"</js>
 * 			<li>Simple: <js>"MyClass.myMethod"</js>
 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2.myMethod"</js> or <js>"Inner1$Inner2.myMethod"</js> or <js>"Inner2.myMethod"</js>
 * 		</ul>
 * 	<li>Fields:
 * 		<ul>
 * 			<li>Fully qualified: <js>"com.foo.MyClass.myField"</js>
 * 			<li>Simple: <js>"MyClass.muyField"</js>
 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2.myField"</js> or <js>"Inner1$Inner2.myField"</js> or <js>"Inner2.myField"</js>
 * 		</ul>
 * 	<li>Constructors:
 * 		<ul>
 * 			<li>Fully qualified with args: <js>"com.foo.MyClass(String,int)"</js> or <js>"com.foo.MyClass(java.lang.String,int)"</js> or <js>"com.foo.MyClass()"</js>
 * 			<li>Simple with args: <js>"MyClass(String,int)"</js> or <js>"MyClass(java.lang.String,int)"</js> or <js>"MyClass()"</js>
 * 			<li>Simple inner class: <js>"MyClass$Inner1$Inner2()"</js> or <js>"Inner1$Inner2()"</js> or <js>"Inner2()"</js>
 * 		</ul>
 * 	<li>A comma-delimited list of anything on this list.
 * </ul>
 *
 * @param <V> The type of object in this map.
 */
public class ReflectionMap<V> {

	private final List<ClassEntry<V>> classEntries;
	private final List<MethodEntry<V>> methodEntries;
	private final List<FieldEntry<V>> fieldEntries;
	private final List<ConstructorEntry<V>> constructorEntries;
	final boolean noClassEntries, noMethodEntries, noFieldEntries, noConstructorEntries;

	/**
	 * Constructor.
	 *
	 * @param b Initializer object.
	 */
	ReflectionMap(Builder<V> b) {
		this.classEntries = Collections.unmodifiableList(new ArrayList<>(b.classEntries));
		this.methodEntries = Collections.unmodifiableList(new ArrayList<>(b.methodEntries));
		this.fieldEntries = Collections.unmodifiableList(new ArrayList<>(b.fieldEntries));
		this.constructorEntries = Collections.unmodifiableList(new ArrayList<>(b.constructorEntries));
		this.noClassEntries = classEntries.isEmpty();
		this.noMethodEntries = methodEntries.isEmpty();
		this.noFieldEntries = fieldEntries.isEmpty();
		this.noConstructorEntries = constructorEntries.isEmpty();
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
		List<MethodEntry<V>> methodEntries = new ArrayList<>();
		List<FieldEntry<V>> fieldEntries = new ArrayList<>();
		List<ConstructorEntry<V>> constructorEntries = new ArrayList<>();

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
			try {
				for (String k : splitNames(key)) {
					if (k.endsWith(")")) {
						int i = k.substring(0, k.indexOf('(')).lastIndexOf('.');
						if (i == -1 || isUpperCase(k.charAt(i+1))) {
							constructorEntries.add(new ConstructorEntry<>(k, value));
						} else {
							methodEntries.add(new MethodEntry<>(k, value));
						}
					} else {
						int i = k.lastIndexOf('.');
						if (i == -1) {
							classEntries.add(new ClassEntry<>(k, value));
						} else if (isUpperCase(k.charAt(i+1))) {
							classEntries.add(new ClassEntry<>(k, value));
							fieldEntries.add(new FieldEntry<>(k, value));
						} else {
							methodEntries.add(new MethodEntry<>(k, value));
							fieldEntries.add(new FieldEntry<>(k, value));
						}
					}
				}
			} catch (IndexOutOfBoundsException e) {
				throw new RuntimeException("Invalid reflection signature: [" + key + "]");
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

	static List<String> splitNames(String key) {
		if (key.indexOf(',') == -1)
			return Collections.singletonList(key.trim());
		List<String> l = new ArrayList<>();

		int m = 0;
		boolean escaped = false;
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (c == '(')
				escaped = true;
			else if (c == ')')
				escaped = false;
			else if (c == ',' && ! escaped) {
				l.add(key.substring(m, i).trim());
				m = i+1;
			}
		}
		l.add(key.substring(m).trim());

		return l;
	}

	/**
	 * Finds first value in this map that matches the specified class.
	 *
	 * @param c The class to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> find(Class<?> c, Class<? extends V> ofType) {
		if (! noClassEntries)
			for (ClassEntry<V> e : classEntries)
				if (e.matches(c))
					if (ofType == null || ofType.isInstance(e.value))
						return Optional.of(e.value);
		return Optional.empty();
	}

	/**
	 * Finds first value in this map that matches the specified method.
	 *
	 * @param m The method to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> find(Method m, Class<? extends V> ofType) {
		if (! noMethodEntries)
			for (MethodEntry<V> e : methodEntries)
				if (e.matches(m))
					if (ofType == null || ofType.isInstance(e.value))
						return Optional.of(e.value);
		return Optional.empty();
	}

	/**
	 * Finds first value in this map that matches the specified field.
	 *
	 * @param f The field to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> find(Field f, Class<? extends V> ofType) {
		if (! noFieldEntries)
			for (FieldEntry<V> e : fieldEntries)
				if (e.matches(f))
					if (ofType == null || ofType.isInstance(e.value))
						return Optional.of(e.value);
		return Optional.empty();
	}

	/**
	 * Finds first value in this map that matches the specified constructor.
	 *
	 * @param c The constructor to test for.
	 * @param ofType Only return objects of the specified type.
	 * @return The matching object.  Never <jk>null</jk>.
	 */
	public Optional<V> find(Constructor<?> c, Class<? extends V> ofType) {
		if (! noConstructorEntries)
			for (ConstructorEntry<V> e : constructorEntries)
				if (e.matches(c))
					if (ofType == null || ofType.isInstance(e.value))
						return Optional.of(e.value);
		return Optional.empty();
	}

	static class ClassEntry<V> {
		final String simpleName, fullName;
		final V value;

		ClassEntry(String name, V value) {
			this.simpleName = simpleClassName(name);
			this.fullName = name;
			this.value = value;
		}

		public boolean matches(Class<?> c) {
			if (c == null)
				return false;
			return classMatches(simpleName, fullName, c);
		}

		public ObjectMap asMap() {
			return new ObjectMap()
				.append("simpleName", simpleName)
				.append("fullName", fullName)
				.append("value", value);
		}

		@Override
		public String toString() {
			return asMap().toString();
		}
	}

	static class MethodEntry<V> {
		String simpleClassName, fullClassName, methodName, args[];
		V value;

		MethodEntry(String name, V value) {
			int i = name.indexOf('(');
			this.args = i == -1 ? null : split(name.substring(i+1, name.length()-1));
			name = i == -1 ? name : name.substring(0, i);
			i = name.lastIndexOf('.');
			String s1 = name.substring(0, i).trim(), s2 = name.substring(i+1).trim();
			this.simpleClassName = simpleClassName(s1);
			this.fullClassName = s1;
			this.methodName = s2;
			this.value = value;
		}

		public boolean matches(Method m) {
			if (m == null)
				return false;
			Class<?> c = m.getDeclaringClass();
			return
				classMatches(simpleClassName, fullClassName, c)
				&& (isEquals(m.getName(), methodName))
				&& (argsMatch(args, m.getParameterTypes()));
		}

		public ObjectMap asMap() {
			return new ObjectMap()
				.append("simpleClassName", simpleClassName)
				.append("fullClassName", fullClassName)
				.append("methodName", methodName)
				.append("args", args)
				.append("value", value);
		}

		@Override
		public String toString() {
			return asMap().toString();
		}
	}

	static class ConstructorEntry<V> {
		String simpleClassName, fullClassName, args[];
		V value;

		ConstructorEntry(String name, V value) {
			int i = name.indexOf('(');
			this.args = split(name.substring(i+1, name.length()-1));
			name = name.substring(0, i).trim();
			this.simpleClassName = simpleClassName(name);
			this.fullClassName = name;
			this.value = value;
		}

		public boolean matches(Constructor<?> m) {
			if (m == null)
				return false;
			Class<?> c = m.getDeclaringClass();
			return
				classMatches(simpleClassName, fullClassName, c)
				&& (argsMatch(args, m.getParameterTypes()));
		}

		public ObjectMap asMap() {
			return new ObjectMap()
				.append("simpleClassName", simpleClassName)
				.append("fullClassName", fullClassName)
				.append("args", args)
				.append("value", value);
		}

		@Override
		public String toString() {
			return asMap().toString();
		}
	}

	static class FieldEntry<V> {
		String simpleClassName, fullClassName, fieldName;
		V value;

		FieldEntry(String name, V value) {
			int i = name.lastIndexOf('.');
			String s1 = name.substring(0, i), s2 = name.substring(i+1);
			this.simpleClassName = simpleClassName(s1);
			this.fullClassName = s1;
			this.fieldName = s2;
			this.value = value;
		}

		public boolean matches(Field f) {
			if (f == null)
				return false;
			Class<?> c = f.getDeclaringClass();
			return
				classMatches(simpleClassName, fullClassName, c)
				&& (isEquals(f.getName(), fieldName));
		}

		public ObjectMap asMap() {
			return new ObjectMap()
				.append("simpleClassName", simpleClassName)
				.append("fullClassName", fullClassName)
				.append("fieldName", fieldName)
				.append("value", value);
		}

		@Override
		public String toString() {
			return asMap().toString();
		}
	}

	static boolean argsMatch(String[] names, Class<?>[] args) {
		if (names == null)
			return true;
		if (names.length != args.length)
			return false;
		for (int i = 0; i < args.length; i++) {
			String n = names[i];
			Class<?> a = args[i];
			if (! (isEquals(n, a.getSimpleName()) || isEquals(n, a.getName())))
				return false;
		}
		return true;
	}

	static String simpleClassName(String name) {
		int i = name.indexOf('.');
		if (i == -1)
			return name;
		return null;
	}

	static boolean classMatches(String simpleName, String fullName, Class<?> c) {
		// For class org.apache.juneau.a.rttests.RountTripBeansWithBuilders$Ac$Builder
		// c.getSimpleName() == "Builder"
		// c.getFullName() == "org.apache.juneau.a.rttests.RountTripBeansWithBuilders$Ac$Builder"
		// c.getPackage() == "org.apache.juneau.a.rttests"
		String cSimple = c.getSimpleName(), cFull = c.getName();
		if (isEquals(simpleName, cSimple) || isEquals(fullName, cFull))
			return true;
		if (cFull.indexOf('$') != -1) {
			Package p = c.getPackage();
			if (p != null)
				cFull = cFull.substring(p.getName().length() + 1);
			if (isEquals(simpleName, cFull))
				return true;
			int i = cFull.indexOf('$');
			while (i != -1) {
				cFull = cFull.substring(i+1);
				if (isEquals(simpleName, cFull))
					return true;
				i = cFull.indexOf('$');
			}
		}
		return false;
	}

	@Override /* Object */
	public String toString() {
		return new ObjectMap()
			.append("classEntries", classEntries)
			.append("methodEntries", methodEntries)
			.append("fieldEntries", fieldEntries)
			.append("constructorEntries", constructorEntries)
			.toString();
	}
}
