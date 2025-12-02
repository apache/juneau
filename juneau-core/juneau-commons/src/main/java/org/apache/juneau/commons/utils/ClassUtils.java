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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Utility methods for working with classes.
 *
 */
public class ClassUtils {
	/**
	 * Predicate check to filter out void classes.
	 */
	public static final Predicate<Class<?>> NOT_VOID = ClassUtils::isNotVoid;

	@SuppressWarnings("rawtypes")
	private static Cache<Class,Boolean> MODIFIABLE_COLLECTION_TYPES = Cache.of(Class.class, Boolean.class).build();

	/**
	 * Determines whether the specified collection supports modification operations (e.g., {@code add()}, {@code remove()}).
	 *
	 * <p>
	 * This method performs a heuristic check based on the collection's class name to determine if it's likely modifiable.
	 * It checks whether the class name contains indicators of immutability such as "Immutable", "Unmodifiable",
	 * or "Arrays$ArrayList" (which represents the unmodifiable list returned by {@link Arrays#asList(Object...)}).
	 *
	 * <p>
	 * Results are cached for performance, so repeated calls for the same collection type are very fast.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Modifiable collections</jc>
	 * 	canAddTo(<jk>new</jk> ArrayList&lt;&gt;());        <jc>// true</jc>
	 * 	canAddTo(<jk>new</jk> LinkedList&lt;&gt;());       <jc>// true</jc>
	 * 	canAddTo(<jk>new</jk> HashSet&lt;&gt;());          <jc>// true</jc>
	 *
	 * 	<jc>// Unmodifiable collections</jc>
	 * 	canAddTo(Collections.unmodifiableList(...));    <jc>// false</jc>
	 * 	canAddTo(Collections.unmodifiableSet(...));     <jc>// false</jc>
	 * 	canAddTo(Arrays.asList(<js>"a"</js>, <js>"b"</js>));               <jc>// false</jc>
	 * 	canAddTo(List.of(<js>"a"</js>, <js>"b"</js>));                     <jc>// false (ImmutableCollections)</jc>
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This is a heuristic check based on naming conventions. It does not attempt to actually
	 * modify the collection, so it never throws exceptions. However, it may produce false positives for
	 * custom collection implementations with misleading names.
	 *
	 * @param value The collection to check. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the collection is likely modifiable, <jk>false</jk> if it's likely unmodifiable.
	 * @throws IllegalArgumentException If value is <jk>null</jk>.
	 */
	public static boolean canAddTo(Collection<?> value) {
		assertArgNotNull("value", value);
		return canAddTo(value.getClass());
	}

	/**
	 * Determines whether the specified map supports modification operations (e.g., {@code put()}, {@code remove()}).
	 *
	 * <p>
	 * This method performs a heuristic check based on the map's class name to determine if it's likely modifiable.
	 * It checks whether the class name contains indicators of immutability such as "Immutable", "Unmodifiable",
	 * or "Arrays$ArrayList" (which represents unmodifiable collections).
	 *
	 * <p>
	 * Results are cached for performance, so repeated calls for the same map type are very fast.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Modifiable maps</jc>
	 * 	canPutTo(<jk>new</jk> HashMap&lt;&gt;());          <jc>// true</jc>
	 * 	canPutTo(<jk>new</jk> LinkedHashMap&lt;&gt;());    <jc>// true</jc>
	 * 	canPutTo(<jk>new</jk> TreeMap&lt;&gt;());          <jc>// true</jc>
	 *
	 * 	<jc>// Unmodifiable maps</jc>
	 * 	canPutTo(Collections.unmodifiableMap(...));     <jc>// false</jc>
	 * 	canPutTo(Map.of(<js>"key"</js>, <js>"value"</js>));              <jc>// false (ImmutableCollections)</jc>
	 * </p>
	 *
	 * <p>
	 * <b>Note:</b> This is a heuristic check based on naming conventions. It does not attempt to actually
	 * modify the map, so it never throws exceptions. However, it may produce false positives for
	 * custom map implementations with misleading names.
	 *
	 * @param value The map to check. Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the map is likely modifiable, <jk>false</jk> if it's likely unmodifiable.
	 * @throws IllegalArgumentException If value is <jk>null</jk>.
	 */
	public static boolean canPutTo(Map<?,?> value) {
		assertArgNotNull("value", value);
		return canAddTo(value.getClass());
	}

	/**
	 * Returns the fully-qualified class name for the specified object.
	 *
	 * <p>
	 * This method returns the canonical JVM class name including the full package path.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	className(String.<jk>class</jk>);                  <jc>// "java.lang.String"</jc>
	 * 	className(<jk>new</jk> HashMap&lt;&gt;());                <jc>// "java.util.HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	className(Map.Entry.<jk>class</jk>);               <jc>// "java.util.Map$Entry"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	className(<jk>int</jk>.<jk>class</jk>);                      <jc>// "int"</jc>
	 * 	className(<jk>boolean</jk>.<jk>class</jk>);                  <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	className(String[].<jk>class</jk>);                <jc>// "[Ljava.lang.String;"</jc>
	 * 	className(<jk>int</jk>[].<jk>class</jk>);                    <jc>// "[I"</jc>
	 * 	className(String[][].<jk>class</jk>);              <jc>// "[[Ljava.lang.String;"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	className(<jk>null</jk>);                          <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the class name for.
	 * @return The name of the class or <jk>null</jk> if the value was null.
	 */
	public static String className(Object value) {
		return value == null ? null : value instanceof Class<?> ? ((Class<?>)value).getName() : value.getClass().getName();
	}

	/**
	 * Returns the simple (non-qualified) class name for the specified object.
	 *
	 * <p>
	 * This method returns only the simple class name without any package or outer class information.
	 * For inner classes, only the innermost class name is returned.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	simpleClassName(String.<jk>class</jk>);            <jc>// "String"</jc>
	 * 	simpleClassName(<jk>new</jk> HashMap&lt;&gt;());          <jc>// "HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	simpleClassName(Map.Entry.<jk>class</jk>);         <jc>// "Entry"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	simpleClassName(<jk>int</jk>.<jk>class</jk>);                <jc>// "int"</jc>
	 * 	simpleClassName(<jk>boolean</jk>.<jk>class</jk>);            <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	simpleClassName(String[].<jk>class</jk>);          <jc>// "String[]"</jc>
	 * 	simpleClassName(<jk>int</jk>[].<jk>class</jk>);              <jc>// "int[]"</jc>
	 * 	simpleClassName(String[][].<jk>class</jk>);        <jc>// "String[][]"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	simpleClassName(<jk>null</jk>);                    <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the simple class name for.
	 * @return The simple name of the class or <jk>null</jk> if the value was null.
	 */
	public static String classNameSimple(Object value) {
		if (value instanceof ClassInfo value2)
			return value2.getNameSimple();
		return value == null ? null : value instanceof Class<?> c ? c.getSimpleName() : value.getClass().getSimpleName();
	}

	/**
	 * Returns the simple qualified class name for the specified object.
	 *
	 * <p>
	 * This returns the simple class name including outer class names, but without the package.
	 * Inner class separators ($) are replaced with dots (.).
	 * Array types are properly formatted with brackets.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	simpleQualifiedClassName(String.<jk>class</jk>);                     <jc>// "String"</jc>
	 * 	simpleQualifiedClassName(<jk>new</jk> HashMap&lt;&gt;());                   <jc>// "HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	simpleQualifiedClassName(Map.Entry.<jk>class</jk>);                  <jc>// "Map.Entry"</jc>
	 * 	simpleQualifiedClassName(Outer.Inner.Deep.<jk>class</jk>);           <jc>// "Outer.Inner.Deep"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	simpleQualifiedClassName(<jk>int</jk>.<jk>class</jk>);                         <jc>// "int"</jc>
	 * 	simpleQualifiedClassName(<jk>boolean</jk>.<jk>class</jk>);                     <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Object arrays</jc>
	 * 	simpleQualifiedClassName(String[].<jk>class</jk>);                   <jc>// "String[]"</jc>
	 * 	simpleQualifiedClassName(Map.Entry[].<jk>class</jk>);                <jc>// "Map.Entry[]"</jc>
	 * 	simpleQualifiedClassName(String[][].<jk>class</jk>);                 <jc>// "String[][]"</jc>
	 *
	 * 	<jc>// Primitive arrays</jc>
	 * 	simpleQualifiedClassName(<jk>int</jk>[].<jk>class</jk>);                       <jc>// "int[]"</jc>
	 * 	simpleQualifiedClassName(<jk>boolean</jk>[][].<jk>class</jk>);                 <jc>// "boolean[][]"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	simpleQualifiedClassName(<jk>null</jk>);                             <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the simple qualified class name for.
	 * @return The simple qualified name of the class or <jk>null</jk> if the value was null.
	 */
	public static String classNameSimpleQualified(Object value) {
		if (value == null)
			return null;
		var clazz = value instanceof Class<?> ? (Class<?>)value : value.getClass();

		// Handle array types by recursively getting component type
		if (clazz.isArray()) {
			return classNameSimpleQualified(clazz.getComponentType()) + "[]";
		}

		// Handle non-array types
		var className = clazz.getName();
		var lastDot = className.lastIndexOf('.');
		var simpleName = lastDot == -1 ? className : className.substring(lastDot + 1);
		return simpleName.replace('$', '.');
	}

	/**
	 * Extracts generic type parameter mappings from a class's superclass declaration and adds them to a type map.
	 *
	 * <p>
	 * This method builds a mapping between type variables (e.g., <c>T</c>, <c>K</c>, <c>V</c>) and their actual
	 * type arguments in the inheritance hierarchy. It's used to resolve generic type parameters when navigating
	 * through class hierarchies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given these classes:</jc>
	 * 	<jk>class</jk> MyList&lt;T&gt; <jk>extends</jk> ArrayList&lt;T&gt; {}
	 * 	<jk>class</jk> StringList <jk>extends</jk> MyList&lt;String&gt; {}
	 *
	 * 	<jc>// When called on StringList, this would extract:</jc>
	 * 	<jc>//   T (from MyList) -&gt; String</jc>
	 * </p>
	 *
	 * <p>
	 * The method handles transitive type mappings by checking if an actual type argument is itself a type variable
	 * that's already been mapped, and resolving it to its final type.
	 *
	 * @param typeMap
	 * 	The map to populate with type variable to actual type mappings.
	 * 	<br>Existing entries are used to resolve transitive mappings.
	 * @param c
	 * 	The class whose generic superclass should be examined for type parameters.
	 */
	public static void extractTypes(Map<Type,Type> typeMap, Class<?> c) {
		var gs = c.getGenericSuperclass();
		if (gs instanceof ParameterizedType gs2) {
			var typeParameters = ((Class<?>)gs2.getRawType()).getTypeParameters();
			var actualTypeArguments = gs2.getActualTypeArguments();
			for (var i = 0; i < typeParameters.length; i++) {
				if (typeMap.containsKey(actualTypeArguments[i]))
					actualTypeArguments[i] = typeMap.get(actualTypeArguments[i]);
				typeMap.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
	}

	/**
	 * Returns the class types for the specified arguments.
	 *
	 * @param args The objects we're getting the classes of.
	 * @return The classes of the arguments.
	 */
	public static Class<?>[] getClasses(Object...args) {
		var pt = new Class<?>[args.length];
		for (var i = 0; i < args.length; i++)
			pt[i] = args[i] == null ? null : args[i].getClass();
		return pt;
	}

	/**
	 * Matches arguments to a list of parameter types.
	 *
	 * <p>
	 * This method intelligently matches a variable number of arguments to a fixed set of parameter types,
	 * handling cases where arguments may be provided in a different order, or where some arguments are
	 * missing or extra arguments are provided. This is particularly useful for reflective method/constructor
	 * invocation where parameter order flexibility is desired.
	 *
	 * <h5 class='section'>Matching Rules:</h5>
	 * <ul>
	 * 	<li>If arguments already match parameter types in order and count, they are returned as-is (fast path)
	 * 	<li>Otherwise, each parameter type is matched with the first compatible argument
	 * 	<li>Extra arguments are ignored
	 * 	<li>Missing parameters are left as <jk>null</jk>
	 * 	<li>Primitive types are automatically matched with their wrapper equivalents
	 * 	<li>Type hierarchy is respected (subclasses match parent parameters)
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Already in correct order - fast path returns original array</jc>
	 * 	Class&lt;?&gt;[] types = {String.<jk>class</jk>, Integer.<jk>class</jk>};
	 * 	Object[] args = {<js>"hello"</js>, 42};
	 * 	Object[] result = getMatchingArgs(types, args);
	 * 	<jc>// Returns: ["hello", 42]</jc>
	 *
	 * 	<jc>// Arguments in wrong order - method reorders them</jc>
	 * 	Class&lt;?&gt;[] types = {Integer.<jk>class</jk>, String.<jk>class</jk>};
	 * 	Object[] args = {<js>"hello"</js>, 42};
	 * 	Object[] result = getMatchingArgs(types, args);
	 * 	<jc>// Returns: [42, "hello"]</jc>
	 *
	 * 	<jc>// Extra arguments are ignored</jc>
	 * 	Class&lt;?&gt;[] types = {String.<jk>class</jk>};
	 * 	Object[] args = {<js>"hello"</js>, 42, <jk>true</jk>};
	 * 	Object[] result = getMatchingArgs(types, args);
	 * 	<jc>// Returns: ["hello"]</jc>
	 *
	 * 	<jc>// Missing arguments become null</jc>
	 * 	Class&lt;?&gt;[] types = {String.<jk>class</jk>, Integer.<jk>class</jk>, Boolean.<jk>class</jk>};
	 * 	Object[] args = {<js>"hello"</js>};
	 * 	Object[] result = getMatchingArgs(types, args);
	 * 	<jc>// Returns: ["hello", null, null]</jc>
	 *
	 * 	<jc>// Handles primitive types and their wrappers</jc>
	 * 	Class&lt;?&gt;[] types = {<jk>int</jk>.<jk>class</jk>, String.<jk>class</jk>};
	 * 	Object[] args = {<js>"hello"</js>, 42};  <jc>// Integer object matches int.class</jc>
	 * 	Object[] result = getMatchingArgs(types, args);
	 * 	<jc>// Returns: [42, "hello"]</jc>
	 *
	 * 	<jc>// Respects type hierarchy - subclasses match parent types</jc>
	 * 	Class&lt;?&gt;[] types = {Number.<jk>class</jk>, String.<jk>class</jk>};
	 * 	Object[] args = {<js>"hello"</js>, 42};  <jc>// Integer extends Number</jc>
	 * 	Object[] result = getMatchingArgs(types, args);
	 * 	<jc>// Returns: [42, "hello"]</jc>
	 * </p>
	 *
	 * <p>
	 * This method is used internally by {@link ClassInfo}, {@link MethodInfo}, and {@link ConstructorInfo}
	 * to provide flexible parameter matching during reflective invocation.
	 *
	 * @param paramTypes The parameter types to match against. Must not be <jk>null</jk>.
	 * @param args The arguments to match to the parameter types. Can be empty or contain <jk>null</jk> values.
	 * @return
	 * 	An array of arguments matched to the parameter types. The returned array will always have
	 * 	the same length as {@code paramTypes}. Returns the original {@code args} array if it already
	 * 	matches (fast path optimization).
	 */
	public static Object[] getMatchingArgs(Class<?>[] paramTypes, Object...args) {
		assertArgNotNull("args", args);
		var needsShuffle = paramTypes.length != args.length;
		if (! needsShuffle) {
			for (var i = 0; i < paramTypes.length; i++) {
				if (! paramTypes[i].isInstance(args[i]))
					needsShuffle = true;
			}
		}
		if (! needsShuffle)
			return args;
		var params = new Object[paramTypes.length];
		for (var i = 0; i < paramTypes.length; i++) {
			var pt = info(paramTypes[i]).getWrapperIfPrimitive();
			for (var arg : args) {
				if (nn(arg) && pt.isParentOf(arg.getClass())) {
					params[i] = arg;
					break;
				}
			}
		}
		return params;
	}

	/**
	 * Returns the generic parameter type at the specified index for a class that extends a parameterized type.
	 *
	 * @param c The class to examine.
	 * @param index The zero-based index of the parameter to retrieve.
	 * @param pt The parameterized superclass or interface.
	 * @return The parameter type at the specified index.
	 * @throws IllegalArgumentException If the class is not a subclass of the parameterized type or if the index is invalid.
	 */
	public static Class<?> getParameterType(Class<?> c, int index, Class<?> pt) {
		assertArgsNotNull("pt", pt, "c", c);

		// We need to make up a mapping of type names.
		var typeMap = new HashMap<Type,Type>();
		var cc = c;
		while (pt != cc.getSuperclass()) {
			extractTypes(typeMap, cc);
			cc = cc.getSuperclass();
			assertArg(nn(cc), "Class ''{0}'' is not a subclass of parameterized type ''{1}''", cns(c), cns(pt));
		}

		var gsc = cc.getGenericSuperclass();

		assertArg(gsc instanceof ParameterizedType, "Class ''{0}'' is not a parameterized type", cns(pt));

		var cpt = (ParameterizedType)gsc;
		var atArgs = cpt.getActualTypeArguments();
		assertArg(index < atArgs.length, "Invalid type index. index={0}, argsLength={1}", index, atArgs.length);
		var actualType = cpt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class actualType2) {
			return actualType2;

		}
		if (actualType instanceof GenericArrayType actualType2) {
			var gct = actualType2.getGenericComponentType();
			if (gct instanceof ParameterizedType gct2)
				return Array.newInstance((Class<?>)gct2.getRawType(), 0).getClass();
		} else if (actualType instanceof TypeVariable<?> actualType3) {
			var nestedOuterTypes = new LinkedList<Class<?>>();
			for (var ec = cc.getEnclosingClass(); nn(ec); ec = ec.getEnclosingClass()) {
				var outerClass = cc.getClass();
				nestedOuterTypes.add(outerClass);
				var outerTypeMap = new HashMap<Type,Type>();
				extractTypes(outerTypeMap, outerClass);
				for (var entry : outerTypeMap.entrySet()) {
					var key = entry.getKey();
					var value = entry.getValue();
					if ((key instanceof TypeVariable<?> key2) && (key2.getName().equals(actualType3.getName()) && isInnerClass(key2.getGenericDeclaration(), actualType3.getGenericDeclaration()))) {
						if (value instanceof Class<?> value2)
							return value2;
						actualType3 = (TypeVariable<?>)entry.getValue();
					}
				}
			}
		} else if (actualType instanceof ParameterizedType actualType2) {
			return (Class<?>)actualType2.getRawType();
		}
		throw illegalArg("Could not resolve variable ''{0}'' to a type.", actualType.getTypeName());
	}

	/**
	 * Attempts to unwrap a proxy object and return the underlying "real" class.
	 *
	 * <p>
	 * This method handles several common proxy types used in Java applications:
	 * <ul>
	 * 	<li><b>JDK Dynamic Proxies</b> - Created via {@link Proxy#newProxyInstance}
	 * 	<li><b>CGLIB Proxies</b> - Created by Spring's CGLIB enhancer (class name contains <js>"$$EnhancerBySpringCGLIB$$"</js>)
	 * 	<li><b>Javassist Proxies</b> - Created by Javassist proxy factory (class name contains <js>"_$$_javassist"</js> or <js>"_$$_jvst"</js>)
	 * 	<li><b>ByteBuddy Proxies</b> - Created by ByteBuddy (class name contains <js>"$ByteBuddy$"</js>)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get the real class behind a Spring proxy</jc>
	 * 	<ja>@Service</ja>
	 * 	<jk>class</jk> MyService {}
	 *
	 * 	MyService proxy = applicationContext.getBean(MyService.<jk>class</jk>);
	 * 	Class&lt;?&gt; realClass = ClassUtils.<jsm>getProxyFor</jsm>(proxy);
	 * 	<jc>// Returns MyService.class instead of the CGLIB proxy class</jc>
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>For JDK dynamic proxies, returns the first interface implemented by the proxy
	 * 	<li>For CGLIB/Javassist/ByteBuddy proxies, returns the superclass
	 * 	<li>For Spring CGLIB proxies with a <c>getTargetClass()</c> method, invokes that method for more accurate results
	 * 	<li>Returns <jk>null</jk> if the object is not a recognized proxy type
	 * </ul>
	 *
	 * @param o The object to unwrap. Can be <jk>null</jk>.
	 * @return
	 * 	The underlying class, or <jk>null</jk> if:
	 * 	<ul>
	 * 		<li>The object is <jk>null</jk>
	 * 		<li>The object is not a recognized proxy type
	 * 		<li>The proxy cannot be unwrapped
	 * 	</ul>
	 */
	public static Class<?> getProxyFor(Object o) {
		if (o == null)
			return null;

		var c = o.getClass();
		var s = c.getName();

		// Quick exit: Most classes aren't proxies, and proxies typically have '$' in their name
		if (s.indexOf('$') == -1)
			return null;

		// JDK Dynamic Proxy: Created via Proxy.newProxyInstance()
		// These implement interfaces and are instances of java.lang.reflect.Proxy
		if (Proxy.isProxyClass(c)) {
			var interfaces = c.getInterfaces();
			return interfaces.length > 0 ? interfaces[0] : null;
		}

		// CGLIB Proxy: Spring's CGLIB enhancer creates subclasses
		// Pattern: com.example.MyClass$$EnhancerBySpringCGLIB$$abc123
		if (s.contains("$$EnhancerBySpringCGLIB$$")) {
			// Try to invoke getTargetClass() if available (Spring specific)
			var v = Value.<Class<?>>empty();
			info(c).getPublicMethods().stream().filter(m -> m.hasName("getTargetClass") && m.getParameterCount() == 0 && m.hasReturnType(Class.class)).forEach(m -> safe(() -> v.set(m.invoke(o))));
			return v.isPresent() ? v.get() : c.getSuperclass();
		}

		// Javassist Proxy: Created by Javassist ProxyFactory
		// Pattern: com.example.MyClass_$$_javassist_123 or com.example.MyClass_$$_jvst123
		// ByteBuddy Proxy: Created by ByteBuddy framework
		// Pattern: com.example.MyClass$ByteBuddy$abc123
		if (s.contains("_$$_javassist") || s.contains("_$$_jvst") || s.contains("$ByteBuddy$")) {
			return c.getSuperclass();
		}

		// Not a recognized proxy type
		return null;
	}

	/**
	 * Returns the generic parameter type of the Value type.
	 *
	 * @param t The type to find the parameter type of.
	 * @return The parameter type of the value, or <jk>null</jk> if the type is not a subclass of <c>Value</c>.
	 */
	public static Type getValueParameterType(Type t) {
		if (t instanceof ParameterizedType t2) {
			if (t2.getRawType() == Value.class) {
				var ta = t2.getActualTypeArguments();
				if (ta.length > 0)
					return ta[0];
			}
		} else if ((t instanceof Class<?> t3) && Value.class.isAssignableFrom(t3)) {
			return getParameterType(t3, 0, Value.class);
		}

		return null;
	}

	/**
	 * Checks if one generic declaration (typically a class) is an inner class of another.
	 *
	 * <p>
	 * This method walks up the enclosing class hierarchy of the potential inner class to determine if it's
	 * nested within the outer class at any level. It's used during generic type resolution to determine if
	 * type variables from different scopes refer to the same logical type parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given these classes:</jc>
	 * 	<jk>class</jk> Outer&lt;T&gt; {
	 * 		<jk>class</jk> Inner&lt;T&gt; {
	 * 			<jc>// The T here could refer to either Outer.T or Inner.T</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// This method helps determine the relationship:</jc>
	 * 	isInnerClass(Outer.<jk>class</jk>, Inner.<jk>class</jk>);  <jc>// Returns true</jc>
	 * 	isInnerClass(Inner.<jk>class</jk>, Outer.<jk>class</jk>);  <jc>// Returns false</jc>
	 * </p>
	 *
	 * @param od
	 * 	The potential outer declaration (typically an outer class).
	 * @param id
	 * 	The potential inner declaration (typically an inner class).
	 * @return
	 * 	<jk>true</jk> if <c>id</c> is nested within <c>od</c> at any level, <jk>false</jk> otherwise.
	 * 	<br>Returns <jk>false</jk> if either parameter is not a <c>Class</c>.
	 */
	public static boolean isInnerClass(GenericDeclaration od, GenericDeclaration id) {
		if (od instanceof Class<?> oc && id instanceof Class<?> ic) {
			while (nn(ic = ic.getEnclosingClass()))
				if (ic == oc)
					return true;
		}
		return false;
	}

	/**
	 * Returns <jk>false</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 *
	 * @param c The class to check.
	 * @return <jk>false</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isNotVoid(Class c) {
		return ! isVoid(c);
	}

	/**
	 * Returns <jk>true</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 *
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specific class is <jk>null</jk> or <c><jk>void</jk>.<jk>class</jk></c> or {@link Void} or has the simple name <js>"Void</js>.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isVoid(Class c) {
		return c == null || c == void.class || c == Void.class || cns(c).equalsIgnoreCase("void");
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x) {
		try {
			if (nn(x))
				x.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The field.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Field x) {
		try {
			if (nn(x))
				x.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The method.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Method x) {
		try {
			if (nn(x))
				x.setAccessible(true);
			return true;
		} catch (@SuppressWarnings("unused") SecurityException e) {
			return false;
		}
	}

	/**
	 * Returns the specified type as a <c>Class</c>.
	 *
	 * <p>
	 * If it's already a <c>Class</c>, it just does a cast.
	 * <br>If it's a <c>ParameterizedType</c>, it returns the raw type.
	 *
	 * @param t The type to convert.
	 * @return The type converted to a <c>Class</c>, or <jk>null</jk> if it could not be converted.
	 */
	public static Class<?> toClass(Type t) {
		if (t instanceof Class<?> c)
			return c;
		if (t instanceof ParameterizedType t2) {
			// The raw type should always be a class (right?)
			return (Class<?>)t2.getRawType();
		}
		return null;
	}

	private static boolean canAddTo(Class<?> c) {
		var b = MODIFIABLE_COLLECTION_TYPES.get(c);
		if (b == null) {
			var name = c.getName();
			b = (! name.contains("Immutable") && ! name.contains("Unmodifiable") && ! name.contains("Arrays$ArrayList"));
			MODIFIABLE_COLLECTION_TYPES.put(c, b);
		}
		return b;
	}
}
