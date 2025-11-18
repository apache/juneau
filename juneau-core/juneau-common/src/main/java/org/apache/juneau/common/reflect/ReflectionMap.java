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
 * Maps arbitrary values to classes, methods, fields, and constructors based on name-based patterns.
 *
 * <p>
 * This utility allows you to create flexible mappings between reflection elements and custom values.
 * It supports various pattern formats for identifying classes and their members, with intelligent
 * matching that handles inner classes, method signatures, and field names.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map that associates visibility levels with specific classes/methods</jc>
 * 	ReflectionMap&lt;Visibility&gt; <jv>map</jv> = ReflectionMap
 * 		.<jsm>create</jsm>(Visibility.<jk>class</jk>)
 * 		.append(<js>"com.foo.MyClass"</js>, Visibility.<jsf>PUBLIC</jsf>)
 * 		.append(<js>"com.foo.MyClass.secretMethod"</js>, Visibility.<jsf>PRIVATE</jsf>)
 * 		.append(<js>"com.foo.MyClass.internalField"</js>, Visibility.<jsf>PACKAGE</jsf>)
 * 		.build();
 *
 * 	<jc>// Find values for specific reflection elements</jc>
 * 	Stream&lt;Visibility&gt; <jv>classVisibility</jv> = <jv>map</jv>.find(MyClass.<jk>class</jk>);
 * 	Stream&lt;Visibility&gt; <jv>methodVisibility</jv> = <jv>map</jv>.find(<jv>secretMethod</jv>);
 * </p>
 *
 * <h5 class='section'>Supported Pattern Formats:</h5>
 *
 * <p>
 * The following pattern formats are supported for mapping keys:
 *
 * <ul class='spaced-list'>
 *  <li><b>Classes:</b>
 * 		<ul>
 * 			<li>Fully qualified:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass"</js>
 * 				</ul>
 * 			<li>Fully qualified inner class:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
 * 				</ul>
 * 			<li>Simple class name:
 * 				<ul>
 * 					<li><js>"MyClass"</js>
 * 				</ul>
 * 			<li>Simple inner class:
 * 				<ul>
 * 					<li><js>"MyClass$Inner1$Inner2"</js>
 * 					<li><js>"Inner1$Inner2"</js>
 * 					<li><js>"Inner2"</js>
 * 				</ul>
 * 			<li>All classes wildcard:
 * 				<ul>
 * 					<li><js>"*"</js>
 * 				</ul>
 * 		</ul>
 * 	<li><b>Methods:</b>
 * 		<ul>
 * 			<li>Fully qualified with args:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
 * 				</ul>
 * 			<li>Fully qualified without args (matches any args):
 * 				<ul>
 * 					<li><js>"com.foo.MyClass.myMethod"</js>
 * 				</ul>
 * 			<li>Simple with args:
 * 				<ul>
 * 					<li><js>"MyClass.myMethod(String,int)"</js>
 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
 * 					<li><js>"MyClass.myMethod()"</js>
 * 				</ul>
 * 			<li>Simple without args (matches any args):
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
 * 	<li><b>Fields:</b>
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
 * 	<li><b>Constructors:</b>
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
 * 	<li><b>Multiple patterns:</b>
 * 		<ul>
 * 			<li>A comma-delimited list of any patterns above:
 * 				<ul>
 * 					<li><js>"com.foo.MyClass, com.bar.OtherClass"</js>
 * 					<li><js>"MyClass.method1, MyClass.method2, MyClass.field1"</js>
 * 				</ul>
 * 		</ul>
 * </ul>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>Method and constructor patterns without parentheses match any method signature.
 * 	<li>Method and constructor argument types can be specified with simple or fully qualified names.
 * 	<li>Array types in signatures use either <js>"Type[]"</js> or JVM notation like <js>"[LType;"</js>.
 * 	<li>Generic type parameters are stripped from method signatures during matching.
 * 	<li>Patterns are case-sensitive.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='seealso'>
 * 	<li class='jc'>{@link ClassInfo}
 * 	<li class='jc'>{@link MethodInfo}
 * 	<li class='jc'>{@link FieldInfo}
 * 	<li class='jc'>{@link ConstructorInfo}
 * </ul>
 *
 * @param <V> The type of values stored in this map.
 */
public class ReflectionMap<V> {

	/**
	 * Builder for creating {@link ReflectionMap} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ReflectionMap&lt;String&gt; <jv>map</jv> = ReflectionMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.append(<js>"MyClass"</js>, <js>"value1"</js>)
	 * 		.append(<js>"MyClass.myMethod"</js>, <js>"value2"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param <V> The type of values stored in this map.
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
		 * Adds one or more mappings to this builder.
		 *
		 * <p>
		 * This method accepts pattern strings that identify classes, methods, fields, or constructors,
		 * and associates them with the specified value. Multiple patterns can be specified in a single
		 * key using comma-delimited format.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	Builder&lt;String&gt; <jv>builder</jv> = ReflectionMap.<jsm>create</jsm>(String.<jk>class</jk>);
		 *
		 * 	<jc>// Map a class</jc>
		 * 	<jv>builder</jv>.append(<js>"com.foo.MyClass"</js>, <js>"classValue"</js>);
		 *
		 * 	<jc>// Map a specific method</jc>
		 * 	<jv>builder</jv>.append(<js>"MyClass.myMethod(String,int)"</js>, <js>"methodValue"</js>);
		 *
		 * 	<jc>// Map multiple patterns at once</jc>
		 * 	<jv>builder</jv>.append(<js>"MyClass.field1, MyClass.field2, MyClass.field3"</js>, <js>"fieldValue"</js>);
		 * </p>
		 *
		 * @param key
		 * 	The mapping key pattern(s).
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>Fully qualified class name (e.g., <js>"com.foo.MyClass"</js>)
		 * 		<li>Simple class name (e.g., <js>"MyClass"</js>)
		 * 		<li>All classes wildcard (e.g., <js>"*"</js>)
		 * 		<li>Method with signature (e.g., <js>"com.foo.MyClass.myMethod(String,int)"</js>)
		 * 		<li>Method without signature (e.g., <js>"MyClass.myMethod"</js>) - matches any signature
		 * 		<li>Field (e.g., <js>"MyClass.myField"</js>)
		 * 		<li>Constructor (e.g., <js>"MyClass(String,int)"</js>)
		 * 		<li>Comma-delimited list of any of the above (e.g., <js>"MyClass, MyClass.method1, MyClass.field1"</js>)
		 * 	</ul>
		 * @param value
		 * 	The value to associate with the matching reflection element(s).
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 * @throws RuntimeException If the key pattern is invalid or empty.
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
		 * Builds a new {@link ReflectionMap} from the current state of this builder.
		 *
		 * <p>
		 * Multiple calls to this method will create independent copies of the map.
		 *
		 * @return A new immutable {@link ReflectionMap} instance.
		 */
		public ReflectionMap<V> build() {
			return new ReflectionMap<>(this);
		}
	}

	private static class ClassEntry<V> {
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
				.build()
				.toString();
			// @formatter:on
		}
	}

	private static class ConstructorEntry<V> {
		String simpleClassName, fullClassName, args[];
		V value;

		ConstructorEntry(String name, V value) {
			int i = name.indexOf('(');
			this.args = splitMethodArgs(name.substring(i + 1, name.length() - 1));
			for (int j = 0; j < args.length; j++) {
				// Strip off generic parameters (e.g., "List<String>[]" -> "List[]")
				args[j] = stripGenerics(args[j]);
			}
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
				.build()
				.toString();
			// @formatter:on
		}
	}

	private static class FieldEntry<V> {
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
				.build()
				.toString();
			// @formatter:on
		}
	}

	private static class MethodEntry<V> {
		String simpleClassName, fullClassName, methodName, args[];
		V value;

		MethodEntry(String name, V value) {
			int i = name.indexOf('(');
			this.args = i == -1 ? null : splitMethodArgs(name.substring(i + 1, name.length() - 1));
			if (nn(args)) {
				for (int j = 0; j < args.length; j++) {
					// Strip off generic parameters (e.g., "List<String>[]" -> "List[]")
					args[j] = stripGenerics(args[j]);
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
				.add("args", opt(args).map(x -> '[' + toCdl(x) + "]").orElse(null))
				.add("value", value)
				.build()
				.toString();
			// @formatter:on
		}
	}

	/**
	 * Creates a new builder for constructing a {@link ReflectionMap}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ReflectionMap&lt;String&gt; <jv>map</jv> = ReflectionMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.append(<js>"com.foo.MyClass"</js>, <js>"value1"</js>)
	 * 		.append(<js>"com.foo.MyClass.myMethod"</js>, <js>"value2"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param <V> The type of values stored in the map.
	 * @param c The class type of values (used for type safety, not stored).
	 * @return A new builder instance.
	 */
	public static <V> Builder<V> create(Class<V> c) {
		return new Builder<>();
	}

	private static boolean argsMatch(String[] names, Class<?>[] args) {
		if (names == null)
			return true;
		if (names.length != args.length)
			return false;
		for (int i = 0; i < args.length; i++) {
			if (!argMatches(names[i], args[i]))
				return false;
		}
		return true;
	}

	private static boolean argMatches(String pattern, Class<?> type) {
		// Extract base type and dimensions from pattern
		var patternDims = 0;
		var patternBase = pattern;
		while (patternBase.endsWith("[]")) {
			patternDims++;
			patternBase = patternBase.substring(0, patternBase.length() - 2);
		}

		// Extract base type and dimensions from actual type
		var typeDims = 0;
		var typeBase = type;
		while (typeBase.isArray()) {
			typeDims++;
			typeBase = typeBase.getComponentType();
		}

		// Array dimensions must match
		if (patternDims != typeDims)
			return false;

		// If non-array, use simple comparison
		if (patternDims == 0)
			return eq(pattern, type.getSimpleName()) || eq(pattern, type.getName());

		// For arrays, compare the component types (simple name or full name)
		return eq(patternBase, typeBase.getSimpleName()) || eq(patternBase, typeBase.getName());
	}

	private static String stripGenerics(String type) {
		if (type.indexOf('<') == -1)
			return type;
		var sb = new StringBuilder(type.length());
		int depth = 0;
		for (int i = 0; i < type.length(); i++) {
			char c = type.charAt(i);
			if (c == '<') {
				depth++;
			} else if (c == '>') {
				depth--;
			} else if (depth == 0) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private static boolean classMatches(String simpleName, String fullName, Class<?> c) {
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

	private static String simpleClassName(String name) {
		int i = name.indexOf('.');
		if (i == -1)
			return name;
		// Return null for fully qualified names to ensure they only match by fullName, not by simpleName.
		// This prevents ambiguous matches where a simple name pattern might accidentally match
		// a fully qualified pattern's simple part.
		return null;
	}

	private static void splitNames(String key, Consumer<String> consumer) {
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
	 * @param b The builder containing the mappings to initialize this map with.
	 */
	protected ReflectionMap(Builder<V> b) {
		this.classEntries = u(copyOf(b.classEntries));
		this.methodEntries = u(copyOf(b.methodEntries));
		this.fieldEntries =  u(copyOf(b.fieldEntries));
		this.constructorEntries = u(copyOf(b.constructorEntries));
	}

	/**
	 * Finds all values associated with the specified class.
	 *
	 * <p>
	 * This method searches for mappings that match the given class, including patterns for
	 * the fully qualified name, simple name, inner class names, and wildcard patterns.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ReflectionMap&lt;String&gt; <jv>map</jv> = ReflectionMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.append(<js>"com.foo.MyClass"</js>, <js>"value1"</js>)
	 * 		.append(<js>"MyClass"</js>, <js>"value2"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Find all values for MyClass</jc>
	 * 	Stream&lt;String&gt; <jv>values</jv> = <jv>map</jv>.find(MyClass.<jk>class</jk>);
	 * 	<jc>// Returns stream containing ["value1", "value2"]</jc>
	 * </p>
	 *
	 * @param c The class to find mappings for. Can be <jk>null</jk>.
	 * @return A stream of all values associated with the class. Empty stream if no matches found.
	 */
	public Stream<V> find(Class<?> c) {
		return classEntries.stream().filter(x -> x.matches(c)).map(x -> x.value);
	}

	/**
	 * Finds all values associated with the specified constructor.
	 *
	 * <p>
	 * This method searches for mappings that match the given constructor, considering both
	 * the declaring class and parameter types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ReflectionMap&lt;String&gt; <jv>map</jv> = ReflectionMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.append(<js>"MyClass(String,int)"</js>, <js>"value1"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Find value for specific constructor</jc>
	 * 	Constructor&lt;?&gt; <jv>ctor</jv> = MyClass.<jk>class</jk>.getConstructor(String.<jk>class</jk>, <jk>int</jk>.<jk>class</jk>);
	 * 	Stream&lt;String&gt; <jv>values</jv> = <jv>map</jv>.find(<jv>ctor</jv>);
	 * </p>
	 *
	 * @param c The constructor to find mappings for. Can be <jk>null</jk>.
	 * @return A stream of all values associated with the constructor. Empty stream if no matches found.
	 */
	public Stream<V> find(Constructor<?> c) {
		return constructorEntries.stream().filter(x -> x.matches(c)).map(x -> x.value);
	}

	/**
	 * Finds all values associated with the specified field.
	 *
	 * <p>
	 * This method searches for mappings that match the given field, considering both
	 * the declaring class and field name.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ReflectionMap&lt;String&gt; <jv>map</jv> = ReflectionMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.append(<js>"MyClass.myField"</js>, <js>"value1"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Find value for specific field</jc>
	 * 	Field <jv>field</jv> = MyClass.<jk>class</jk>.getField(<js>"myField"</js>);
	 * 	Stream&lt;String&gt; <jv>values</jv> = <jv>map</jv>.find(<jv>field</jv>);
	 * </p>
	 *
	 * @param f The field to find mappings for. Can be <jk>null</jk>.
	 * @return A stream of all values associated with the field. Empty stream if no matches found.
	 */
	public Stream<V> find(Field f) {
		return fieldEntries.stream().filter(x -> x.matches(f)).map(x -> x.value);
	}

	/**
	 * Finds all values associated with the specified method.
	 *
	 * <p>
	 * This method searches for mappings that match the given method, considering the
	 * declaring class, method name, and parameter types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ReflectionMap&lt;String&gt; <jv>map</jv> = ReflectionMap
	 * 		.<jsm>create</jsm>(String.<jk>class</jk>)
	 * 		.append(<js>"MyClass.myMethod"</js>, <js>"value1"</js>)
	 * 		.append(<js>"MyClass.myMethod(String)"</js>, <js>"value2"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Find values for specific method</jc>
	 * 	Method <jv>method</jv> = MyClass.<jk>class</jk>.getMethod(<js>"myMethod"</js>, String.<jk>class</jk>);
	 * 	Stream&lt;String&gt; <jv>values</jv> = <jv>map</jv>.find(<jv>method</jv>);
	 * 	<jc>// Returns stream containing ["value1", "value2"] - first matches any signature</jc>
	 * </p>
	 *
	 * @param m The method to find mappings for. Can be <jk>null</jk>.
	 * @return A stream of all values associated with the method. Empty stream if no matches found.
	 */
	public Stream<V> find(Method m) {
		return methodEntries.stream().filter(x -> x.matches(m)).map(x -> x.value);
	}

	@Override /* Overridden from Object */
	public String toString() {
		// @formatter:off
		return mapb().filtered()
			.add("classEntries", classEntries)
			.add("methodEntries", methodEntries)
			.add("fieldEntries", fieldEntries)
			.add("constructorEntries", constructorEntries)
			.build()
			.toString();
		// @formatter:on
	}
}