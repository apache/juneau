/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.common.reflect;

import static java.lang.Character.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.utils.*;

/**
 * Allows arbitrary objects to be mapped to classes and methods base on class/method name keys.
 *
 * <p>
 * The valid pattern matches are:
 * <ul class='spaced-list'>
 *  <li>Classes:
 * 		<ul>
 * 			<li>Fully qualified:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass"</js>
 * 				</ul>
 * 			<li>Fully qualified inner class:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
 * 				</ul>
 * 			<li>Simple:
 * 				<ul>
 * 					<li><js>"MyClass"</js>
 * 				</ul>
 * 			<li>Simple inner:
 * 				<ul>
 * 					<li><js>"MyClass$Inner1$Inner2"</js>
 * 					<li><js>"Inner1$Inner2"</js>
 * 					<li><js>"Inner2"</js>
 * 				</ul>
 * 		</ul>
 * 	<li>Methods:
 * 		<ul>
 * 			<li>Fully qualified with args:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
 * 				</ul>
 * 			<li>Fully qualified:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass.myMethod"</js>
 * 				</ul>
 * 			<li>Simple with args:
 * 				<ul>
 * 					<li><js>"MyClass.myMethod(String,int)"</js>
 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
 * 					<li><js>"MyClass.myMethod()"</js>
 * 				</ul>
 * 			<li>Simple:
 * 				<ul>
 * 					<li><js>"MyClass.myMethod"</js>
 * 				</ul>
 * 			<li>Simple inner class:
 * 				<ul>
 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
 * 					<li><js>"Inner1$Inner2.myMethod"</js>
 * 					<li><js>"Inner2.myMethod"</js>
 * 				</ul>
 * 		</ul>
 * 	<li>Fields:
 * 		<ul>
 * 			<li>Fully qualified:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass.myField"</js>
 * 				</ul>
 * 			<li>Simple:
 * 				<ul>
 * 					<li><js>"MyClass.myField"</js>
 * 				</ul>
 * 			<li>Simple inner class:
 * 				<ul>
 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
 * 					<li><js>"Inner1$Inner2.myField"</js>
 * 					<li><js>"Inner2.myField"</js>
 * 				</ul>
 * 		</ul>
 * 	<li>Constructors:
 * 		<ul>
 * 			<li>Fully qualified with args:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass(String,int)"</js>
 * 					<li><js>"com.foo.MyClass(java.lang.String,int)"</js>
 * 					<li><js>"com.foo.MyClass()"</js>
 * 				</ul>
 * 			<li>Simple with args:
 * 				<ul>
 * 					<li><js>"MyClass(String,int)"</js>
 * 					<li><js>"MyClass(java.lang.String,int)"</js>
 * 					<li><js>"MyClass()"</js>
 * 				</ul>
 * 			<li>Simple inner class:
 * 				<ul>
 * 					<li><js>"MyClass$Inner1$Inner2()"</js>
 * 					<li><js>"Inner1$Inner2()"</js>
 * 					<li><js>"Inner2()"</js>
 * 				</ul>
 * 		</ul>
 * 	<li>A comma-delimited list of anything on this list.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <V> The type of object in this map.
 */
public class ReflectionMap2<V> {

	/**
	 * Builder class.
	 * @param <V> The type of object in this map.
	 */
	public static class Builder<V> {
		final List<ClassEntry<V>> classEntries;
		final List<MethodEntry<V>> methodEntries;
		final List<FieldEntry<V>> fieldEntries;
		final List<ConstructorEntry<V>> constructorEntries;

		/**
		 * Constructor.
		 */
		protected Builder() {
			classEntries = list();
			methodEntries = list();
			fieldEntries = list();
			constructorEntries = list();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder being copied.
		 */
		protected Builder(Builder<V> copyFrom) {
			classEntries = copyOf(copyFrom.classEntries);
			methodEntries = copyOf(copyFrom.methodEntries);
			fieldEntries = copyOf(copyFrom.fieldEntries);
			constructorEntries = copyOf(copyFrom.constructorEntries);
		}

		/**
		 * Adds a mapping to this builder.
		 *
		 * @param key
		 * 	The mapping key.
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>Full class name (e.g. <js>"com.foo.MyClass"</js>).
		 * 		<li>Simple class name (e.g. <js>"MyClass"</js>).
		 * 		<li>All classes (e.g. <js>"*"</js>).
		 * 		<li>Full method name (e.g. <js>"com.foo.MyClass.myMethod"</js>).
		 * 		<li>Simple method name (e.g. <js>"MyClass.myMethod"</js>).
		 * 		<li>A comma-delimited list of anything on this list.
		 * 	</ul>
		 * @param value The value for this mapping.
		 * @return This object.
		 */
		public Builder<V> append(String key, V value) {
			if (StringUtils.isEmpty(key))
				throw runtimeException("Invalid reflection signature: [{0}]", key);
			try {
				splitNames(key, k -> {
					if (k.endsWith(")")) {
						int i = k.substring(0, k.indexOf('(')).lastIndexOf('.');
						if (i == -1 || isUpperCase(k.charAt(i + 1))) {
							constructorEntries.add(new ConstructorEntry<>(k, value));
						} else {
							methodEntries.add(new MethodEntry<>(k, value));
						}
					} else {
						int i = k.lastIndexOf('.');
						if (i == -1) {
							classEntries.add(new ClassEntry<>(k, value));
						} else if (isUpperCase(k.charAt(i + 1))) {
							classEntries.add(new ClassEntry<>(k, value));
							fieldEntries.add(new FieldEntry<>(k, value));
						} else {
							methodEntries.add(new MethodEntry<>(k, value));
							fieldEntries.add(new FieldEntry<>(k, value));
						}
					}
				});
			} catch (@SuppressWarnings("unused") IndexOutOfBoundsException e) {
				throw runtimeException("Invalid reflection signature: [{0}]", key);
			}

			return this;
		}

		/**
		 * Create new instance of {@link ReflectionMap2} based on the contents of this builder.
		 *
		 * @return A new {@link ReflectionMap2} object.
		 */
		public ReflectionMap2<V> build() {
			return new ReflectionMap2<>(this);
		}

		/**
		 * Creates a copy of this builder.
		 *
		 * @return A copy of this builder.
		 */
		public Builder<V> copy() {
			return new Builder<>(this);
		}
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

		@Override
		public String toString() {
			// @formatter:off
			return mapb().filtered()
				.add("simpleName", simpleName)
				.add("fullName", fullName)
				.add("value", value)
				.toString();
			// @formatter:on
		}
	}

	static class ConstructorEntry<V> {
		String simpleClassName, fullClassName, args[];
		V value;

		ConstructorEntry(String name, V value) {
			int i = name.indexOf('(');
			this.args = splita(name.substring(i + 1, name.length() - 1));
			name = name.substring(0, i).trim();
			this.simpleClassName = simpleClassName(name);
			this.fullClassName = name;
			this.value = value;
		}

		public boolean matches(Constructor<?> m) {
			if (m == null)
				return false;
			var c = m.getDeclaringClass();
			return classMatches(simpleClassName, fullClassName, c) && (argsMatch(args, m.getParameterTypes()));
		}

		@Override
		public String toString() {
			// @formatter:off
			return mapb().filtered()
				.add("simpleClassName", simpleClassName)
				.add("fullClassName", fullClassName)
				.add("args", args)
				.add("value", value)
				.toString();
			// @formatter:on
		}
	}

	static class FieldEntry<V> {
		String simpleClassName, fullClassName, fieldName;
		V value;

		FieldEntry(String name, V value) {
			int i = name.lastIndexOf('.');
			var s1 = name.substring(0, i);
			var s2 = name.substring(i + 1);
			this.simpleClassName = simpleClassName(s1);
			this.fullClassName = s1;
			this.fieldName = s2;
			this.value = value;
		}

		public boolean matches(Field f) {
			if (f == null)
				return false;
			var c = f.getDeclaringClass();
			return classMatches(simpleClassName, fullClassName, c) && (eq(f.getName(), fieldName));
		}

		@Override
		public String toString() {
			// @formatter:off
			return mapb().filtered()
				.add("simpleClassName", simpleClassName)
				.add("fullClassName", fullClassName)
				.add("fieldName", fieldName)
				.add("value", value)
				.toString();
			// @formatter:on
		}
	}

	static class MethodEntry<V> {
		String simpleClassName, fullClassName, methodName, args[];
		V value;

		MethodEntry(String name, V value) {
			int i = name.indexOf('(');
			this.args = i == -1 ? null : splitMethodArgs(name.substring(i + 1, name.length() - 1));
			if (nn(args)) {
				for (int j = 0; j < args.length; j++) {

					// Strip off generic parameters.
					int k = args[j].indexOf('<');
					if (k > 0)
						args[j] = args[j].substring(0, k);

					// Convert from xxx[][] to [[Lxxx; notation.
					if (args[j].endsWith("[]")) {
						int l = 0;
						while (args[j].endsWith("[]")) {
							l++;
							args[j] = args[j].substring(0, args[j].length() - 2);
						}
						var sb = new StringBuilder(args[j].length() + l + 2);
						for (int m = 0; m < l; m++)
							sb.append('[');
						sb.append('L').append(args[j]).append(';');
						args[j] = sb.toString();
					}
				}
			}
			name = i == -1 ? name : name.substring(0, i);
			i = name.lastIndexOf('.');
			var s1 = name.substring(0, i).trim();
			var s2 = name.substring(i + 1).trim();
			this.simpleClassName = simpleClassName(s1);
			this.fullClassName = s1;
			this.methodName = s2;
			this.value = value;
		}

		public boolean matches(Method m) {
			if (m == null)
				return false;
			var c = m.getDeclaringClass();
			// @formatter:off
			return
				classMatches(simpleClassName, fullClassName, c)
				&& (eq(m.getName(), methodName))
				&& (argsMatch(args, m.getParameterTypes()));
			// @formatter:on
		}

		@Override
		public String toString() {
			// @formatter:off
			return mapb().filtered()
				.add("simpleClassName", simpleClassName)
				.add("fullClassName", fullClassName)
				.add("methodName", methodName)
				.add("args", args)
				.add("value", value)
				.toString();
			// @formatter:on
		}
	}

	/**
	 * Static builder creator.
	 *
	 * @param <V> The type of object in this map.
	 * @param c The type of object in this map.
	 * @return A new instance of this object.
	 */
	public static <V> Builder<V> create(Class<V> c) {
		return new Builder<>();
	}

	static boolean argsMatch(String[] names, Class<?>[] args) {
		if (names == null)
			return true;
		if (names.length != args.length)
			return false;
		for (int i = 0; i < args.length; i++) {
			var n = names[i];
			var a = args[i];
			if (! (eq(n, a.getSimpleName()) || eq(n, a.getName())))
				return false;
		}
		return true;
	}

	static boolean classMatches(String simpleName, String fullName, Class<?> c) {
		// For class org.apache.juneau.a.rttests.RountTripBeansWithBuilders$Ac$Builder
		// c.getSimpleName() == "Builder"
		// c.getFullName() == "org.apache.juneau.a.rttests.RountTripBeansWithBuilders$Ac$Builder"
		// c.getPackage() == "org.apache.juneau.a.rttests"
		var cSimple = scn(c);
		var cFull = cn(c);
		if (eq(simpleName, cSimple) || eq(fullName, cFull) || "*".equals(simpleName))
			return true;
		if (cFull.indexOf('$') != -1) {
			var p = c.getPackage();
			if (nn(p))
				cFull = cFull.substring(p.getName().length() + 1);
			if (eq(simpleName, cFull))
				return true;
			int i = cFull.indexOf('$');
			while (i != -1) {
				cFull = cFull.substring(i + 1);
				if (eq(simpleName, cFull))
					return true;
				i = cFull.indexOf('$');
			}
		}
		return false;
	}

	static String simpleClassName(String name) {
		int i = name.indexOf('.');
		if (i == -1)
			return name;
		return null;
	}

	static void splitNames(String key, Consumer<String> consumer) {
		if (key.indexOf(',') == -1) {
			consumer.accept(key);
		} else {
			int m = 0;
			boolean escaped = false;
			for (int i = 0; i < key.length(); i++) {
				char c = key.charAt(i);
				if (c == '(')
					escaped = true;
				else if (c == ')')
					escaped = false;
				else if (c == ',' && ! escaped) {
					consumer.accept(key.substring(m, i).trim());
					m = i + 1;
				}
			}
			consumer.accept(key.substring(m).trim());
		}
	}

	final List<ClassEntry<V>> classEntries;

	final List<MethodEntry<V>> methodEntries;

	final List<FieldEntry<V>> fieldEntries;

	final List<ConstructorEntry<V>> constructorEntries;

	/**
	 * Constructor.
	 *
	 * @param b Initializer object.
	 */
	protected ReflectionMap2(Builder<V> b) {
		this.classEntries = u(copyOf(b.classEntries));
		this.methodEntries = u(copyOf(b.methodEntries));
		this.fieldEntries =  u(copyOf(b.fieldEntries));
		this.constructorEntries = u(copyOf(b.constructorEntries));
	}

	public Stream<V> findMatching(Class<?> c) {
		return classEntries.stream().filter(x -> x.matches(c)).map(x -> x.value);
	}

	public Stream<V> findMatching(Method m) {
		return methodEntries.stream().filter(x -> x.matches(m)).map(x -> x.value);
	}

	public Stream<V> findMatching(Field f) {
		return fieldEntries.stream().filter(x -> x.matches(f)).map(x -> x.value);
	}

	public Stream<V> findMatching(Constructor c) {
		return constructorEntries.stream().filter(x -> x.matches(c)).map(x -> x.value);
	}

	@Override /* Overridden from Object */
	public String toString() {
		// @formatter:off
		return mapb().filtered()
			.add("classEntries", classEntries)
			.add("methodEntries", methodEntries)
			.add("fieldEntries", fieldEntries)
			.add("constructorEntries", constructorEntries)
			.toString();
		// @formatter:on
	}
}