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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.common.collections.*;

/**
 * Lightweight utility class for introspecting information about a class.
 *
 * <p>
 * Provides various convenience methods for introspecting fields/methods/annotations
 * that aren't provided by the standard Java reflection APIs.
 *
 * <p>
 * Objects are designed to be lightweight to create and threadsafe.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Wrap our class inside a ClassInfo.</jc>
 * 	ClassInfo <jv>classInfo</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 *
 * 	<jc>// Get all methods in parent-to-child order, sorted alphabetically per class.</jc>
 * 	<jk>for</jk> (MethodInfo <jv>methodInfo</jv> : <jv>classInfo</jv>.getAllMethods()) {
 * 		<jc>// Do something with it.</jc>
 * 	}
 *
 * 	<jc>// Get all class-level annotations in parent-to-child order.</jc>
 * 	<jk>for</jk> (MyAnnotation <jv>annotation</jv> : <jv>classInfo</jv>.getAnnotations(MyAnnotation.<jk>class</jk>)) {
 * 		<jc>// Do something with it.</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ClassInfo {
	@SuppressWarnings("rawtypes")
	private static final Cache<Class,ClassInfo> CACHE = Cache.of(Class.class, ClassInfo.class).build();

	/** Reusable ClassInfo for Object class. */
	public static final ClassInfo OBJECT = ClassInfo.of(Object.class);

	private static final Map<Class<?>,Class<?>> pmap1 = new HashMap<>(), pmap2 = new HashMap<>();

	static {
		pmap1.put(boolean.class, Boolean.class);
		pmap1.put(byte.class, Byte.class);
		pmap1.put(short.class, Short.class);
		pmap1.put(char.class, Character.class);
		pmap1.put(int.class, Integer.class);
		pmap1.put(long.class, Long.class);
		pmap1.put(float.class, Float.class);
		pmap1.put(double.class, Double.class);
		pmap2.put(Boolean.class, boolean.class);
		pmap2.put(Byte.class, byte.class);
		pmap2.put(Short.class, short.class);
		pmap2.put(Character.class, char.class);
		pmap2.put(Integer.class, int.class);
		pmap2.put(Long.class, long.class);
		pmap2.put(Float.class, float.class);
		pmap2.put(Double.class, double.class);
	}

	@SuppressWarnings("rawtypes")
	// @formatter:off
	private static final Map<Class,Object> primitiveDefaultMap =
		mapb(Class.class,Object.class)
			.unmodifiable()
			.add(Boolean.TYPE, false)
			.add(Character.TYPE, (char)0)
			.add(Short.TYPE, (short)0)
			.add(Integer.TYPE, 0)
			.add(Long.TYPE, 0L)
			.add(Float.TYPE, 0f)
			.add(Double.TYPE, 0d)
			.add(Byte.TYPE, (byte)0)
			.add(Boolean.class, false)
			.add(Character.class, (char)0)
			.add(Short.class, (short)0)
			.add(Integer.class, 0)
			.add(Long.class, 0L)
			.add(Float.class, 0f)
			.add(Double.class, 0d)
			.add(Byte.class, (byte)0)
			.build();
	// @formatter:on

	/**
	 * Returns a class info wrapper around the specified class type.
	 *
	 * @param c The class type.
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo of(Class<?> c) {
		if (c == null)
			return null;
		return CACHE.get(c, () -> new ClassInfo(c, c));
	}

	/**
	 * Returns a class info wrapper around the specified class type.
	 *
	 * @param c The class type.
	 * @param t The generic type (if parameterized type).
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo of(Class<?> c, Type t) {
		if (c == t)
			return of(c);
		return new ClassInfo(c, t);
	}

	/**
	 * Same as using the constructor, but operates on an object instance.
	 *
	 * @param o The class instance.
	 * @return The constructed class info, or <jk>null</jk> if the object was <jk>null</jk>.
	 */
	public static ClassInfo of(Object o) {
		return of(o == null ? null : o instanceof Class ? (Class<?>)o : o.getClass());
	}

	/**
	 * Returns a class info wrapper around the specified class type.
	 *
	 * @param t The class type.
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo of(Type t) {
		if (t == null)
			return null;
		if (t instanceof Class)
			return of((Class<?>)t);
		return new ClassInfo(toClass(t), t);
	}

	/**
	 * Same as {@link #of(Object)} but attempts to deproxify the object if it's wrapped in a CGLIB proxy.
	 *
	 * @param o The class instance.
	 * @return The constructed class info, or <jk>null</jk> if the object was <jk>null</jk>.
	 */
	public static ClassInfo ofProxy(Object o) {
		if (o == null)
			return null;
		Class<?> c = getProxyFor(o);
		return c == null ? ClassInfo.of(o) : ClassInfo.of(c);
	}

	private static void extractTypes(Map<Type,Type> typeMap, Class<?> c) {
		Type gs = c.getGenericSuperclass();
		if (gs instanceof ParameterizedType pt) {
			Type[] typeParameters = ((Class<?>)pt.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			for (int i = 0; i < typeParameters.length; i++) {
				if (typeMap.containsKey(actualTypeArguments[i]))
					actualTypeArguments[i] = typeMap.get(actualTypeArguments[i]);
				typeMap.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
	}

	/**
	 * When this metadata is against a CGLIB proxy, this method finds the underlying "real" class.
	 *
	 * @param o The class instance.
	 * @return The non-proxy class, or <jk>null</jk> if it's not a CGLIB proxy.
	 */
	private static Class<?> getProxyFor(Object o) {
		Class<?> c = o.getClass();
		String s = c.getName();
		if (s.indexOf('$') == -1 || ! s.contains("$$EnhancerBySpringCGLIB$$"))
			return null;
		Value<Class<?>> v = Value.empty();
		ClassInfo.of(c).forEachPublicMethod(m -> m.hasName("getTargetClass") && m.hasNoParams() && m.hasReturnType(Class.class), m -> safe(() -> v.set(m.invoke(o))));
		return v.orElse(null);
	}

	private static boolean isInnerClass(GenericDeclaration od, GenericDeclaration id) {
		if (od instanceof Class<?> oc && id instanceof Class<?> ic) {
			while (nn(ic = ic.getEnclosingClass()))
				if (ic == oc)
					return true;
		}
		return false;
	}

	/*
	 * If the annotation is an array of other annotations, returns the inner annotations.
	 *
	 * @param a The annotation to split if repeated.
	 * @return The nested annotations, or a singleton array of the same annotation if it's not repeated.
	 */
	static Annotation[] splitRepeated(Annotation a) {
		try {
			var ci = ClassInfo.of(a.annotationType());
			MethodInfo mi = ci.getRepeatedAnnotationMethod();
			if (nn(mi))
				return mi.invoke(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return a(a);
	}

	private final Type t;
	final Class<?> c;
	private final boolean isParameterizedType;
	private volatile Boolean isRepeatedAnnotation;
	private volatile ClassInfo[] interfaces, declaredInterfaces, parents, allParents;
	private volatile MethodInfo[] publicMethods, declaredMethods, allMethods, allMethodsParentFirst;

	private volatile MethodInfo repeatedAnnotationMethod;
	private volatile ConstructorInfo[] publicConstructors, declaredConstructors;
	private volatile FieldInfo[] publicFields, declaredFields, allFields;

	private volatile Annotation[] declaredAnnotations;

	private int dim = -1;

	private ClassInfo componentType;

	private final ConcurrentHashMap<Method,MethodInfo> methods = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<Field,FieldInfo> fields = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<Constructor<?>,ConstructorInfo> constructors = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param c The class type.
	 * @param t The generic type (if parameterized type).
	 */
	protected ClassInfo(Class<?> c, Type t) {
		this.t = t;
		this.c = c;
		this.isParameterizedType = t == null ? false : (t instanceof ParameterizedType);
	}

	/**
	 * Performs an action on this object if the specified predicate test passes.
	 *
	 * @param test A test to apply to determine if action should be executed.  Can be <jk>null</jk>.
	 * @param action An action to perform on this object.
	 * @return This object.
	 */
	public ClassInfo accept(Predicate<ClassInfo> test, Consumer<ClassInfo> action) {
		if (matches(test))
			action.accept(this);
		return this;
	}

	/**
	 * Same as {@link #getFullName()} but appends to an existing string builder.
	 *
	 * @param sb The string builder to append to.
	 * @return The same string builder.
	 */
	public StringBuilder appendFullName(StringBuilder sb) {
		Class<?> ct = getComponentType().inner();
		int dim = getDimensions();
		if (nn(ct) && dim == 0 && ! isParameterizedType)
			return sb.append(ct.getName());
		sb.append(nn(ct) ? ct.getName() : t.getTypeName());
		if (isParameterizedType) {
			var pt = (ParameterizedType)t;
			sb.append('<');
			boolean first = true;
			for (var t2 : pt.getActualTypeArguments()) {
				if (! first)
					sb.append(',');
				first = false;
				of(t2).appendFullName(sb);
			}
			sb.append('>');
		}
		for (int i = 0; i < dim; i++)
			sb.append('[').append(']');
		return sb;
	}

	/**
	 * Same as {@link #getShortName()} but appends to an existing string builder.
	 *
	 * @param sb The string builder to append to.
	 * @return The same string builder.
	 */
	public StringBuilder appendShortName(StringBuilder sb) {
		Class<?> ct = getComponentType().inner();
		int dim = getDimensions();
		if (nn(ct)) {
			if (ct.isLocalClass())
				sb.append(of(ct.getEnclosingClass()).getSimpleName()).append('$').append(ct.getSimpleName());
			else if (ct.isMemberClass())
				sb.append(of(ct.getDeclaringClass()).getSimpleName()).append('$').append(ct.getSimpleName());
			else
				sb.append(ct.getSimpleName());
		} else {
			sb.append(t.getTypeName());
		}
		if (isParameterizedType) {
			var pt = (ParameterizedType)t;
			sb.append('<');
			boolean first = true;
			for (var t2 : pt.getActualTypeArguments()) {
				if (! first)
					sb.append(',');
				first = false;
				of(t2).appendShortName(sb);
			}
			sb.append('>');
		}
		for (int i = 0; i < dim; i++)
			sb.append('[').append(']');
		return sb;
	}

	/**
	 * Returns <jk>true</jk> if this type can be used as a parameter for the specified object.
	 *
	 * @param child The argument to check.
	 * @return <jk>true</jk> if this type can be used as a parameter for the specified object.
	 */
	public boolean canAcceptArg(Object child) {
		if (c == null || child == null)
			return false;
		if (c.isInstance(child))
			return true;
		if (this.isPrimitive() || child.getClass().isPrimitive()) {
			return this.getWrapperIfPrimitive().isAssignableFrom(of(child).getWrapperIfPrimitive());
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof ClassInfo o2) && eq(this, o2, (x, y) -> eq(x.t, y.t));
	}

	/**
	 * Returns the first matching annotation on this class and superclasses/interfaces.
	 *
	 * <p>
	 * Annotations are searched in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if annotation should be returned.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> A firstAnnotation(AnnotationProvider annotationProvider, Class<A> type, Predicate<A> filter) {
		if (annotationProvider == null)
			annotationProvider = AnnotationProvider.DEFAULT;
		A x = null;
		x = getPackageAnnotation(type);
		if (nn(x) && test(filter, x))
			return x;
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length - 1; i >= 0; i--) {
			x = annotationProvider.firstAnnotation(type, interfaces[i].inner(), filter);
			if (nn(x))
				return x;
		}
		ClassInfo[] parents = _getParents();
		for (int i = parents.length - 1; i >= 0; i--) {
			x = annotationProvider.firstAnnotation(type, parents[i].inner(), filter);
			if (nn(x))
				return x;
		}
		return null;
	}

	/**
	 * Returns the first matching annotation on this class and superclasses/interfaces.
	 *
	 * <p>
	 * Annotations are searched in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if annotation should be returned.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> A firstAnnotation(Class<A> type, Predicate<A> filter) {
		return firstAnnotation(null, type, filter);
	}

	/**
	 * Performs an action on all matching fields on this class and all parent classes.
	 *
	 * <p>
	 * 	Results are ordered parent-to-child, and then alphabetical per class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachAllField(Predicate<FieldInfo> filter, Consumer<FieldInfo> action) {
		for (var fi : _getAllFields())
			consumeIf(filter, action, fi);
		return this;
	}

	/**
	 * Performs an action on all matching declared methods on this class and all parent classes.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachAllMethodParentFirst(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (var mi : _getAllMethodsParentFirst())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Performs an action on all matching annotations on this class and superclasses/interfaces.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> ClassInfo forEachAnnotation(AnnotationProvider annotationProvider, Class<A> type, Predicate<A> filter, Consumer<A> action) {
		if (annotationProvider == null)
			annotationProvider = AnnotationProvider.DEFAULT;
		A t2 = getPackageAnnotation(type);
		if (nn(t2))
			consumeIf(filter, action, t2);
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length - 1; i >= 0; i--)
			annotationProvider.forEachDeclaredAnnotation(type, interfaces[i].inner(), filter, action);
		ClassInfo[] parents = _getParents();
		for (int i = parents.length - 1; i >= 0; i--)
			annotationProvider.forEachDeclaredAnnotation(type, parents[i].inner(), filter, action);
		return this;
	}

	/**
	 * Performs an action on all matching annotations on this class and superclasses/interfaces.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> ClassInfo forEachAnnotation(Class<A> type, Predicate<A> filter, Consumer<A> action) {
		return forEachAnnotation(null, type, filter, action);
	}

	/**
	 * Performs an action on all matching annotations on this class/parents/package.
	 *
	 * <p>
	 * Annotations are consumed in the following order:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachAnnotationInfo(Predicate<AnnotationInfo<?>> filter, Consumer<AnnotationInfo<?>> action) {
		AnnotationInfo.forEachAnnotationInfo(this, filter, action);
		return this;
	}

	/**
	 * Performs an action on all matching declared constructors on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachDeclaredConstructor(Predicate<ConstructorInfo> filter, Consumer<ConstructorInfo> action) {
		for (var mi : _getDeclaredConstructors())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Performs an action on all matching declared fields on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachDeclaredField(Predicate<FieldInfo> filter, Consumer<FieldInfo> action) {
		for (var fi : _getDeclaredFields())
			consumeIf(filter, action, fi);
		return this;
	}

	/**
	 * Performs an action on all matching declared methods on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachDeclaredMethod(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (var mi : _getDeclaredMethods())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Performs an action on all matching methods on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachMethod(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (var mi : _getAllMethods())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Performs an action on all matching public constructors on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachPublicConstructor(Predicate<ConstructorInfo> filter, Consumer<ConstructorInfo> action) {
		for (var mi : _getPublicConstructors())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Performs an action on all matching public fields on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachPublicField(Predicate<FieldInfo> filter, Consumer<FieldInfo> action) {
		for (var mi : _getPublicFields())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Performs an action on all matching public methods on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachPublicMethod(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (var mi : _getPublicMethods())
			consumeIf(filter, action, mi);
		return this;
	}

	/**
	 * Returns all fields on this class and all parent classes.
	 *
	 * <p>
	 * 	Results are ordered parent-to-child, and then alphabetical per class.
	 *
	 * @return
	 * 	All declared fields on this class.
	 * 	<br>List is unmodifiable.
	 */
	public List<FieldInfo> getAllFields() { return u(l(_getAllFields())); }

	/**
	 * Returns all declared methods on this class and all parent classes.
	 *
	 * @return
	 * 	All declared methods on this class and all parent classes.
	 * 	<br>Results are ordered parent-to-child, and then alphabetically per class.
	 * 	<br>List is unmodifiable.
	 */
	public List<MethodInfo> getAllMethodsParentFirst() { return u(l(_getAllMethodsParentFirst())); }

	/**
	 * Returns a list including this class and all parent classes and interfaces.
	 *
	 * <p>
	 * Results are classes-before-interfaces, then child-to-parent order.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are ordered child-to-parent order with classes listed before interfaces.
	 */
	public List<ClassInfo> getAllParents() { return u(l(_getAllParents())); }

	/**
	 * Finds the annotation of the specified type defined on this class or parent class/interface.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate class, searches methods with the same signature on the parent classes or interfaces. <br>
	 * The search is performed in child-to-parent order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return The annotation if found, or <jk>null</jk> if not.
	 */
	public <A extends Annotation> A getAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		return findAnnotation(annotationProvider, type);
	}

	/**
	 * Finds the annotation of the specified type defined on this class or parent class/interface.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate class, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The annotation if found, or <jk>null</jk> if not.
	 */
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		return getAnnotation(null, type);
	}

	/**
	 * Returns the first matching annotation of the specified type defined on the specified class or parent classes/interfaces in parent-to-child order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> A getAnnotation(Class<A> type, Predicate<A> filter) {
		return getAnnotation(null, type, filter);
	}

	/**
	 * Constructs an {@link AnnotationList} of all annotations found on this class.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationList() {
		return AnnotationInfo.getAnnotationList(this);
	}

	/**
	 * Constructs an {@link AnnotationList} of all matching annotations on this class.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered parent-to-child.
	 * 	<li>On parent classes ordered parent-to-child.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationList(Predicate<AnnotationInfo<?>> filter) {
		return AnnotationInfo.getAnnotationList(this, filter);
	}

	/**
	 * Returns all annotations of the specified type defined on this or parent classes/interfaces.
	 *
	 * <p>
	 * Returns the list in reverse (parent-to-child) order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation type to look for.
	 * @return The matching annotations.
	 */
	public <A extends Annotation> List<A> getAnnotations(AnnotationProvider annotationProvider, Class<A> type) {
		List<A> l = list();
		forEachAnnotation(annotationProvider, type, x -> true, x -> l.add(x));
		return l;
	}

	/**
	 * Returns all annotations of the specified type defined on the specified class or parent classes/interfaces in parent-to-child order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation type to look for.
	 * @return The matching annotations.
	 */
	public <A extends Annotation> List<A> getAnnotations(Class<A> type) {
		return getAnnotations(null, type);
	}

	/**
	 * Returns the first matching parent class or interface.
	 *
	 * <p>
	 * Results are classes-before-interfaces, then child-to-parent order.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The parent class or interface that matches the specified predicate.
	 */
	public ClassInfo getAnyParent(Predicate<ClassInfo> filter) {
		for (var ci : _getAllParents())
			if (test(filter, ci))
				return ci;
		return null;
	}

	/**
	 * Returns the {@link ClassLoader} for this class.
	 *
	 * <p>
	 * If this class represents a primitive type or void, <jk>null</jk> is returned.
	 *
	 * @return The class loader for this class, or <jk>null</jk> if it doesn't have one.
	 */
	public ClassLoader getClassLoader() {
		return c == null ? null : c.getClassLoader();
	}

	/**
	 * Returns the base component type of this class if it's an array.
	 *
	 * @return The base component type of this class if it's an array, or this object if it's not.
	 */
	public ClassInfo getComponentType() {
		if (componentType == null) {
			if (c == null)
				componentType = this;
			else
				getDimensions();
		}
		return componentType;
	}

	/**
	 * Returns the {@link Class} object representing the class or interface that declares the member class
	 * represented by this class.
	 *
	 * <p>
	 * Returns <jk>null</jk> if this class is not a member class.
	 *
	 * @return The declaring class, or <jk>null</jk> if this class is not a member of another class.
	 */
	public ClassInfo getDeclaringClass() {
		return c == null ? null : of(c.getDeclaringClass());
	}

	/**
	 * Returns the first matching declared constructor on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The declared constructor that matches the specified predicate.
	 */
	public ConstructorInfo getDeclaredConstructor(Predicate<ConstructorInfo> filter) {
		for (var ci : _getDeclaredConstructors())
			if (test(filter, ci))
				return ci;
		return null;
	}

	/**
	 * Returns all the constructors defined on this class.
	 *
	 * @return
	 * 	All constructors defined on this class.
	 * 	<br>List is unmodifiable.
	 */
	public List<ConstructorInfo> getDeclaredConstructors() { return u(l(_getDeclaredConstructors())); }

	/**
	 * Returns the first matching declared field on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The declared field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getDeclaredField(Predicate<FieldInfo> filter) {
		for (var f : _getDeclaredFields())
			if (test(filter, f))
				return f;
		return null;
	}

	/**
	 * Returns all declared fields on this class.
	 *
	 * @return
	 * 	All declared fields on this class.
	 * 	<br>Results are in alphabetical order.
	 * 	<br>List is unmodifiable.
	 */
	public List<FieldInfo> getDeclaredFields() { return u(l(_getDeclaredFields())); }

	/**
	 * Returns all public member classes and interfaces declared by this class and its superclasses.
	 *
	 * <p>
	 * This includes public class and interface members inherited from superclasses and
	 * public class and interface members declared by the class.
	 *
	 * @return
	 * 	An unmodifiable list of all public member classes and interfaces declared by this class.
	 * 	<br>Returns an empty list if this class has no public member classes or interfaces.
	 */
	public List<ClassInfo> getClasses() {
		if (c == null)
			return u(l());
		Class<?>[] classes = c.getClasses();
		List<ClassInfo> l = listOfSize(classes.length);
		for (Class<?> cc : classes)
			l.add(of(cc));
		return u(l);
	}

	/**
	 * Returns all classes and interfaces declared as members of this class.
	 *
	 * <p>
	 * This includes public, protected, default (package) access, and private classes and interfaces
	 * declared by the class, but excludes inherited classes and interfaces.
	 *
	 * @return
	 * 	An unmodifiable list of all classes and interfaces declared as members of this class.
	 * 	<br>Returns an empty list if this class declares no classes or interfaces as members.
	 */
	public List<ClassInfo> getDeclaredClasses() {
		if (c == null)
			return u(l());
		Class<?>[] classes = c.getDeclaredClasses();
		List<ClassInfo> l = listOfSize(classes.length);
		for (Class<?> cc : classes)
			l.add(of(cc));
		return u(l);
	}

	/**
	 * Returns a list of interfaces declared on this class.
	 *
	 * <p>
	 * Does not include interfaces declared on parent classes.
	 *
	 * <p>
	 * Results are in the same order as Class.getInterfaces().
	 *
	 * @return
	 * 	An unmodifiable list of interfaces declared on this class.
	 * 	<br>Results are in the same order as {@link Class#getInterfaces()}.
	 */
	public List<ClassInfo> getDeclaredInterfaces() { return u(l(_getDeclaredInterfaces())); }

	/**
	 * Returns the immediately enclosing class of this class.
	 *
	 * <p>
	 * Returns <jk>null</jk> if this class is a top-level class.
	 *
	 * @return The enclosing class, or <jk>null</jk> if this is a top-level class.
	 */
	public ClassInfo getEnclosingClass() {
		return c == null ? null : of(c.getEnclosingClass());
	}

	/**
	 * Returns the {@link ConstructorInfo} object representing the constructor that declares this class if this is a
	 * local or anonymous class declared within a constructor.
	 *
	 * <p>
	 * Returns <jk>null</jk> if this class was not declared within a constructor.
	 *
	 * @return The enclosing constructor, or <jk>null</jk> if this class was not declared within a constructor.
	 */
	public ConstructorInfo getEnclosingConstructor() {
		if (c == null)
			return null;
		Constructor<?> ec = c.getEnclosingConstructor();
		return ec == null ? null : getConstructorInfo(ec);
	}

	/**
	 * Returns the {@link MethodInfo} object representing the method that declares this class if this is a
	 * local or anonymous class declared within a method.
	 *
	 * <p>
	 * Returns <jk>null</jk> if this class was not declared within a method.
	 *
	 * @return The enclosing method, or <jk>null</jk> if this class was not declared within a method.
	 */
	public MethodInfo getEnclosingMethod() {
		if (c == null)
			return null;
		Method em = c.getEnclosingMethod();
		return em == null ? null : getMethodInfo(em);
	}

	/**
	 * Returns the first matching declared method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getDeclaredMethod(Predicate<MethodInfo> filter) {
		for (var mi : _getDeclaredMethods())
			if (test(filter, mi))
				return mi;
		return null;
	}

	/**
	 * Returns all methods declared on this class.
	 *
	 * @return
	 * 	All methods declared on this class.
	 * 	<br>Results are ordered alphabetically.
	 * 	<br>List is unmodifiable.
	 */
	public List<MethodInfo> getDeclaredMethods() { return u(l(_getDeclaredMethods())); }

	/**
	 * Returns the number of dimensions if this is an array type.
	 *
	 * @return The number of dimensions if this is an array type, or <c>0</c> if it is not.
	 */
	public int getDimensions() {
		if (dim == -1) {
			int d = 0;
			Class<?> ct = c;
			while (nn(ct) && ct.isArray()) {
				d++;
				ct = ct.getComponentType();
			}
			this.dim = d;
			this.componentType = ct == c ? this : of(ct);
		}
		return dim;
	}

	/**
	 * Returns the full name of this class.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"com.foo.MyClass"</js> - Normal class
	 * 	<li><js>"com.foo.MyClass[][]"</js> - Array.
	 * 	<li><js>"com.foo.MyClass$InnerClass"</js> - Inner class.
	 * 	<li><js>"com.foo.MyClass$InnerClass[][]"</js> - Inner class array.
	 * 	<li><js>"int"</js> - Primitive class.
	 * 	<li><js>"int[][]"</js> - Primitive class class.
	 * 	<li><js>"java.util.Map&lt;java.lang.String,java.lang.Object&gt;"</js> - Parameterized type.
	 * 	<li><js>"java.util.AbstractMap&lt;K,V&gt;"</js> - Parameterized generic type.
	 * 	<li><js>"V"</js> - Parameterized generic type argument.
	 * </ul>
	 *
	 * @return The underlying class name.
	 */
	public String getFullName() {
		Class<?> ct = getComponentType().inner();
		int dim = getDimensions();
		if (nn(ct) && dim == 0 && ! isParameterizedType)
			return ct.getName();
		var sb = new StringBuilder(128);
		appendFullName(sb);
		return sb.toString();
	}

	/**
	 * Returns a list of interfaces defined on this class and superclasses.
	 *
	 * <p>
	 * Results are in child-to-parent order.
	 *
	 * @return
	 * 	An unmodifiable list of interfaces defined on this class and superclasses.
	 * 	<br>Results are in child-to-parent order.
	 */
	public List<ClassInfo> getInterfaces() { return u(l(_getInterfaces())); }

	/**
	 * Returns the first matching method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getMethod(Predicate<MethodInfo> filter) {
		for (var mi : _getAllMethods())
			if (test(filter, mi))
				return mi;
		return null;
	}

	/**
	 * Returns all declared methods on this class and all parent classes.
	 *
	 * @return
	 * 	All declared methods on this class and all parent classes.
	 * 	<br>Results are ordered child-to-parent, and then alphabetically per class.
	 * 	<br>List is unmodifiable.
	 */
	public List<MethodInfo> getMethods() { return u(l(_getAllMethods())); }

	/**
	 * Returns the name of the underlying class.
	 *
	 * @return The name of the underlying class.
	 */
	public String getName() { return nn(c) ? c.getName() : t.getTypeName(); }

	/**
	 * Returns the canonical name of the underlying class.
	 *
	 * <p>
	 * The canonical name is the name that would be used in Java source code to refer to the class.
	 * For example:
	 * <ul>
	 * 	<li><js>"java.lang.String"</js> - Normal class
	 * 	<li><js>"java.lang.String[]"</js> - Array
	 * 	<li><js>"java.util.Map.Entry"</js> - Nested class
	 * 	<li><jk>null</jk> - Local or anonymous class
	 * </ul>
	 *
	 * @return The canonical name of the underlying class, or <jk>null</jk> if this class doesn't have a canonical name.
	 */
	public String getCanonicalName() {
		return c == null ? null : c.getCanonicalName();
	}

	/**
	 * Returns all possible names for this class.
	 *
	 * @return
	 * 	An array consisting of:
	 * 	<ul>
	 * 		<li>{@link #getFullName()}
	 * 		<li>{@link Class#getName()} - Note that this might be a dup.
	 * 		<li>{@link #getShortName()}
	 * 		<li>{@link #getSimpleName()}
	 * 	</ul>
	 */
	public String[] getNames() { return a(getFullName(), c.getName(), getShortName(), getSimpleName()); }

	/**
	 * Returns the module that this class is a member of.
	 *
	 * <p>
	 * If this class is not in a named module, returns the unnamed module of the class loader for this class.
	 *
	 * @return The module that this class is a member of.
	 */
	public Module getModule() {
		return c == null ? null : c.getModule();
	}

	/**
	 * Returns the Java language modifiers for this class, encoded in an integer.
	 *
	 * <p>
	 * The modifiers consist of the Java Virtual Machine's constants for <jk>public</jk>, <jk>protected</jk>,
	 * <jk>private</jk>, <jk>final</jk>, <jk>static</jk>, <jk>abstract</jk> and <jk>interface</jk>;
	 * they should be decoded using the methods of class {@link Modifier}.
	 *
	 * @return The modifiers for this class, or <c>0</c> if this object does not represent a class.
	 * @see Modifier
	 */
	public int getModifiers() {
		return c == null ? 0 : c.getModifiers();
	}

	/**
	 * Locates the no-arg constructor for this class.
	 *
	 * <p>
	 * Constructor must match the visibility requirements specified by parameter 'v'.
	 * If class is abstract, always returns <jk>null</jk>.
	 * Note that this also returns the 1-arg constructor for non-static member classes.
	 *
	 * @param v The minimum visibility.
	 * @return The constructor, or <jk>null</jk> if no no-arg constructor exists with the required visibility.
	 */
	public ConstructorInfo getNoArgConstructor(Visibility v) {
		if (isAbstract())
			return null;
		boolean isMemberClass = isNonStaticMemberClass();
		for (var cc : _getDeclaredConstructors())
			if (cc.hasNumParams(isMemberClass ? 1 : 0) && cc.isVisible(v))
				return cc.accessible(v);
		return null;
	}

	/**
	 * Returns the package of this class.
	 *
	 * @return The package of this class wrapped in a {@link PackageInfo}, or <jk>null</jk> if this class has no package.
	 */
	public PackageInfo getPackage() { return c == null ? null : PackageInfo.of(c.getPackage()); }

	/**
	 * Returns the specified annotation only if it's been declared on the package of this class.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getPackageAnnotation(Class<A> type) {
		PackageInfo pi = getPackage();
		return (pi == null ? null : pi.getAnnotation(type));
	}

	/**
	 * Finds the real parameter type of this class.
	 *
	 * @param index The zero-based index of the parameter to resolve.
	 * @param pt The parameterized type class containing the parameterized type to resolve (e.g. <c>HashMap</c>).
	 * @return The resolved real class.
	 */
	public Class<?> getParameterType(int index, Class<?> pt) {
		assertArgNotNull("pt", pt);

		// We need to make up a mapping of type names.
		var typeMap = new HashMap<Type,Type>();
		Class<?> cc = c;
		while (pt != cc.getSuperclass()) {
			extractTypes(typeMap, cc);
			cc = cc.getSuperclass();
			assertArg(nn(cc), "Class ''{0}'' is not a subclass of parameterized type ''{1}''", c.getSimpleName(), pt.getSimpleName());
		}

		Type gsc = cc.getGenericSuperclass();

		assertArg(gsc instanceof ParameterizedType, "Class ''{0}'' is not a parameterized type", pt.getSimpleName());

		var cpt = (ParameterizedType)gsc;
		Type[] atArgs = cpt.getActualTypeArguments();
		assertArg(index < atArgs.length, "Invalid type index. index={0}, argsLength={1}", index, atArgs.length);
		Type actualType = cpt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class) {
			return (Class<?>)actualType;

		} else if (actualType instanceof GenericArrayType) {
		Type gct = ((GenericArrayType)actualType).getGenericComponentType();
		if (gct instanceof ParameterizedType pt3)
			return Array.newInstance((Class<?>)pt3.getRawType(), 0).getClass();
	} else if (actualType instanceof TypeVariable<?> typeVariable) {
		List<Class<?>> nestedOuterTypes = new LinkedList<>();
		for (Class<?> ec = cc.getEnclosingClass(); nn(ec); ec = ec.getEnclosingClass()) {
			Class<?> outerClass = cc.getClass();
			nestedOuterTypes.add(outerClass);
		var outerTypeMap = new HashMap<Type,Type>();
		extractTypes(outerTypeMap, outerClass);
		for (var entry : outerTypeMap.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue();
			if (key instanceof TypeVariable<?> keyType) {
				if (keyType.getName().equals(typeVariable.getName()) && isInnerClass(keyType.getGenericDeclaration(), typeVariable.getGenericDeclaration())) {
					if (value instanceof Class<?> c)
						return c;
						typeVariable = (TypeVariable<?>)entry.getValue();
					}
				}
			}
		}
	} else if (actualType instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)actualType).getRawType();
		}
		throw illegalArg("Could not resolve variable ''{0}'' to a type.", actualType.getTypeName());
	}

	/**
	 * Returns a list including this class and all parent classes.
	 *
	 * <p>
	 * Does not include interfaces.
	 *
	 * <p>
	 * Results are in child-to-parent order.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are in child-to-parent order.
	 */
	public List<ClassInfo> getParents() { return u(l(_getParents())); }

	/**
	 * Returns the default value for this primitive class.
	 *
	 * @return The default value, or <jk>null</jk> if this is not a primitive class.
	 */
	public Object getPrimitiveDefault() { return primitiveDefaultMap.get(c); }

	/**
	 * If this class is a primitive wrapper (e.g. <code><jk>Integer</jk>.<jk>class</jk></code>) returns it's
	 * primitive class (e.g. <code>int.<jk>class</jk></code>).
	 *
	 * @return The primitive class, or <jk>null</jk> if class is not a primitive wrapper.
	 */
	public Class<?> getPrimitiveForWrapper() { return pmap2.get(c); }

	/**
	 * If this class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @return The wrapper class, or <jk>null</jk> if class is not a primitive.
	 */
	public Class<?> getPrimitiveWrapper() { return pmap1.get(c); }

	/**
	 * Returns the first matching public constructor on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The public constructor that matches the specified predicate.
	 */
	public ConstructorInfo getPublicConstructor(Predicate<ConstructorInfo> filter) {
		for (var ci : _getPublicConstructors())
			if (test(filter, ci))
				return ci;
		return null;
	}

	/**
	 * Returns all the public constructors defined on this class.
	 *
	 * @return All public constructors defined on this class.
	 */
	public List<ConstructorInfo> getPublicConstructors() { return u(l(_getPublicConstructors())); }

	/**
	 * Returns the first matching public field on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The public field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getPublicField(Predicate<FieldInfo> filter) {
		for (var f : _getPublicFields())
			if (test(filter, f))
				return f;
		return null;
	}

	/**
	 * Returns all public fields on this class.
	 *
	 * <p>
	 * Hidden fields are excluded from the results.
	 *
	 * @return
	 * 	All public fields on this class.
	 * 	<br>Results are in alphabetical order.
	 * 	<br>List is unmodifiable.
	 */
	public List<FieldInfo> getPublicFields() { return u(l(_getPublicFields())); }

	/**
	 * Returns the first matching public method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getPublicMethod(Predicate<MethodInfo> filter) {
		for (var mi : _getPublicMethods())
			if (test(filter, mi))
				return mi;
		return null;
	}

	/**
	 * Returns all public methods on this class.
	 *
	 * <p>
	 * Methods defined on the {@link Object} class are excluded from the results.
	 *
	 * @return
	 * 	All public methods on this class.
	 * 	<br>Results are ordered alphabetically.
	 */
	public List<MethodInfo> getPublicMethods() { return u(l(_getPublicMethods())); }

	/**
	 * Same as {@link #getSimpleName()} but uses <js>"Array"</js> instead of <js>"[]"</js>.
	 *
	 * @return The readable name for this class.
	 */
	public String getReadableName() {
		if (c == null)
			return t.getTypeName();
		if (! c.isArray())
			return c.getSimpleName();
		Class<?> c = this.c;
		var sb = new StringBuilder();
		while (c.isArray()) {
			sb.append("Array");
			c = c.getComponentType();
		}
		return c.getSimpleName() + sb;
	}

	/**
	 * Returns the repeated annotation method on this class.
	 *
	 * <p>
	 * The repeated annotation method is the <code>value()</code> method that returns an array
	 * of annotations who themselves are marked with the {@link Repeatable @Repeatable} annotation
	 * of this class.
	 *
	 * @return The repeated annotation method on this class, or <jk>null</jk> if it doesn't exist.
	 */
	public MethodInfo getRepeatedAnnotationMethod() {
		if (isRepeatedAnnotation()) {
			if (repeatedAnnotationMethod == null) {
				synchronized (this) {
					repeatedAnnotationMethod = getPublicMethod(x -> x.hasName("value"));
				}
			}
			return repeatedAnnotationMethod;
		}
		return null;
	}

	/**
	 * Returns the short name of the underlying class.
	 *
	 * <p>
	 * Similar to {@link #getSimpleName()} but also renders local or member class name prefixes.
	 *
	 * @return The short name of the underlying class.
	 */
	public String getShortName() {
		Class<?> ct = getComponentType().inner();
		int dim = getDimensions();
		if (nn(ct) && dim == 0 && ! (isParameterizedType || isMemberClass() || c.isLocalClass()))
			return ct.getSimpleName();
		var sb = new StringBuilder(32);
		appendShortName(sb);
		return sb.toString();
	}

	/**
	 * Returns the simple name of the underlying class.
	 *
	 * <p>
	 * Returns either {@link Class#getSimpleName()} or {@link Type#getTypeName()} depending on whether
	 * this is a class or type.
	 *
	 * @return The simple name of the underlying class;
	 */
	public String getSimpleName() { return nn(c) ? c.getSimpleName() : t.getTypeName(); }

	/**
	 * Returns the parent class.
	 *
	 * @return
	 * 	The parent class, or <jk>null</jk> if the class has no parent.
	 */
	public ClassInfo getSuperclass() { return c == null ? null : of(c.getSuperclass()); }

	/**
	 * Returns the nest host of this class.
	 *
	 * <p>
	 * Every class belongs to exactly one nest. A class that is not a member of a nest
	 * is its own nest host.
	 *
	 * @return The nest host of this class.
	 */
	public ClassInfo getNestHost() {
		return c == null ? null : of(c.getNestHost());
	}

	/**
	 * Returns an array containing all the classes and interfaces that are members of the nest
	 * to which this class belongs.
	 *
	 * @return
	 * 	An unmodifiable list of all classes and interfaces in the same nest as this class.
	 * 	<br>Returns an empty list if this object does not represent a class.
	 */
	public List<ClassInfo> getNestMembers() {
		if (c == null)
			return u(l());
		Class<?>[] members = c.getNestMembers();
		List<ClassInfo> l = listOfSize(members.length);
		for (Class<?> cc : members)
			l.add(of(cc));
		return u(l);
	}

	/**
	 * Returns the permitted subclasses of this sealed class.
	 *
	 * <p>
	 * If this class is not sealed, returns an empty list.
	 *
	 * @return
	 * 	An unmodifiable list of permitted subclasses if this is a sealed class.
	 * 	<br>Returns an empty list if this class is not sealed.
	 */
	public List<ClassInfo> getPermittedSubclasses() {
		if (c == null || ! c.isSealed())
			return u(l());
		Class<?>[] permitted = c.getPermittedSubclasses();
		List<ClassInfo> l = listOfSize(permitted.length);
		for (Class<?> cc : permitted)
			l.add(of(cc));
		return u(l);
	}

	/**
	 * Returns the record components of this record class.
	 *
	 * <p>
	 * The components are returned in the same order as they appear in the record declaration.
	 * If this class is not a record, returns an empty array.
	 *
	 * @return
	 * 	An array of record components for this record class.
	 * 	<br>Returns an empty array if this class is not a record.
	 */
	public RecordComponent[] getRecordComponents() {
		if (c == null || ! c.isRecord())
			return new RecordComponent[0];
		return c.getRecordComponents();
	}

	/**
	 * Returns the {@link Type} representing the direct superclass of this class.
	 *
	 * <p>
	 * If the superclass is a parameterized type, the {@link Type} returned reflects the actual
	 * type parameters used in the source code.
	 *
	 * @return
	 * 	The superclass of this class as a {@link Type},
	 * 	or <jk>null</jk> if this class represents {@link Object}, an interface, a primitive type, or void.
	 */
	public Type getGenericSuperclass() {
		return c == null ? null : c.getGenericSuperclass();
	}

	/**
	 * Returns the {@link Type}s representing the interfaces directly implemented by this class.
	 *
	 * <p>
	 * If a superinterface is a parameterized type, the {@link Type} returned for it reflects the actual
	 * type parameters used in the source code.
	 *
	 * @return
	 * 	An array of {@link Type}s representing the interfaces directly implemented by this class.
	 * 	<br>Returns an empty array if this class implements no interfaces.
	 */
	public Type[] getGenericInterfaces() {
		return c == null ? new Type[0] : c.getGenericInterfaces();
	}

	/**
	 * Returns an array of {@link TypeVariable} objects that represent the type variables declared by this class.
	 *
	 * <p>
	 * The type variables are returned in the same order as they appear in the class declaration.
	 *
	 * @return
	 * 	An array of {@link TypeVariable} objects representing the type parameters of this class.
	 * 	<br>Returns an empty array if this class declares no type parameters.
	 */
	public TypeVariable<?>[] getTypeParameters() {
		return c == null ? new TypeVariable[0] : c.getTypeParameters();
	}

	/**
	 * Returns an {@link AnnotatedType} object that represents the annotated superclass of this class.
	 *
	 * <p>
	 * If this class represents a class type whose superclass is annotated, the returned object reflects
	 * the annotations used in the source code to declare the superclass.
	 *
	 * @return
	 * 	An {@link AnnotatedType} object representing the annotated superclass,
	 * 	or <jk>null</jk> if this class represents {@link Object}, an interface, a primitive type, or void.
	 */
	public AnnotatedType getAnnotatedSuperclass() {
		return c == null ? null : c.getAnnotatedSuperclass();
	}

	/**
	 * Returns an array of {@link AnnotatedType} objects that represent the annotated interfaces
	 * implemented by this class.
	 *
	 * <p>
	 * If this class represents a class or interface whose superinterfaces are annotated,
	 * the returned objects reflect the annotations used in the source code to declare the superinterfaces.
	 *
	 * @return
	 * 	An array of {@link AnnotatedType} objects representing the annotated superinterfaces.
	 * 	<br>Returns an empty array if this class implements no interfaces.
	 */
	public AnnotatedType[] getAnnotatedInterfaces() {
		return c == null ? new AnnotatedType[0] : c.getAnnotatedInterfaces();
	}

	/**
	 * Returns the {@link ProtectionDomain} of this class.
	 *
	 * <p>
	 * If a security manager is installed, this method requires <c>RuntimePermission("getProtectionDomain")</c>.
	 *
	 * @return The {@link ProtectionDomain} of this class, or <jk>null</jk> if the class does not have a protection domain.
	 */
	public java.security.ProtectionDomain getProtectionDomain() {
		return c == null ? null : c.getProtectionDomain();
	}

	/**
	 * Returns the signers of this class.
	 *
	 * @return The signers of this class, or <jk>null</jk> if there are no signers.
	 */
	public Object[] getSigners() {
		return c == null ? null : c.getSigners();
	}

	/**
	 * Finds a resource with a given name.
	 *
	 * <p>
	 * The rules for searching resources associated with a given class are implemented by the defining class loader.
	 *
	 * @param name The resource name.
	 * @return A URL object for reading the resource, or <jk>null</jk> if the resource could not be found.
	 */
	public java.net.URL getResource(String name) {
		return c == null ? null : c.getResource(name);
	}

	/**
	 * Finds a resource with a given name and returns an input stream for reading the resource.
	 *
	 * <p>
	 * The rules for searching resources associated with a given class are implemented by the defining class loader.
	 *
	 * @param name The resource name.
	 * @return An input stream for reading the resource, or <jk>null</jk> if the resource could not be found.
	 */
	public java.io.InputStream getResourceAsStream(String name) {
		return c == null ? null : c.getResourceAsStream(name);
	}

	/**
	 * Returns the descriptor string of this class.
	 *
	 * <p>
	 * The descriptor is a string representing the type of the class, as specified in JVMS 4.3.2.
	 * For example, the descriptor for <c>String</c> is <js>"Ljava/lang/String;"</js>.
	 *
	 * @return The descriptor string of this class.
	 */
	public String descriptorString() {
		return c == null ? null : c.descriptorString();
	}

	/**
	 * If this class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public Class<?> getWrapperIfPrimitive() {
		if (nn(c) && ! c.isPrimitive())
			return c;
		return pmap1.get(c);
	}

	/**
	 * Same as {@link #getWrapperIfPrimitive()} but wraps it in a {@link ClassInfo}.
	 *
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public ClassInfo getWrapperInfoIfPrimitive() {
		if (c == null || ! c.isPrimitive())
			return this;
		return of(pmap1.get(c));
	}

	/**
	 * Returns <jk>true</jk> if this class has the specified annotation.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return The <jk>true</jk> if annotation if found.
	 */
	public <A extends Annotation> boolean hasAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		if (annotationProvider == null)
			annotationProvider = AnnotationProvider.DEFAULT;
		return nn(annotationProvider.firstAnnotation(type, c, x -> true));
	}

	/**
	 * Returns <jk>true</jk> if this class has the specified annotation.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The <jk>true</jk> if annotation if found.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return hasAnnotation(null, type);
	}

	@Override
	public int hashCode() {
		return t.hashCode();
	}

	/**
	 * Returns <jk>true</jk> if this class doesn't have the specified annotation.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The <jk>true</jk> if annotation if not found.
	 */
	public <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return ! hasAnnotation(type);
	}

	/**
	 * Returns <jk>true</jk> if this class is not in the root package.
	 *
	 * @return <jk>true</jk> if this class is not in the root package.
	 */
	public boolean hasPackage() {
		return nn(getPackage());
	}

	/**
	 * Returns <jk>true</jk> if the {@link #getPrimitiveWrapper()} method returns a value.
	 *
	 * @return <jk>true</jk> if the {@link #getPrimitiveWrapper()} method returns a value.
	 */
	public boolean hasPrimitiveWrapper() {
		return pmap1.containsKey(c);
	}

	/**
	 * Returns the wrapped class as a {@link Class}.
	 *
	 * @param <T> The inner class type.
	 * @return The wrapped class as a {@link Class}, or <jk>null</jk> if it's not a class (e.g. it's a {@link ParameterizedType}).
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<T> inner() {
		return (Class<T>)c;
	}

	/**
	 * Returns the wrapped class as a {@link Type}.
	 *
	 * @return The wrapped class as a {@link Type}.
	 */
	public Type innerType() {
		return t;
	}

	/**
	 * Checks for equality with the specified class.
	 *
	 * @param c The class to check equality with.
	 * @return <jk>true</jk> if the specified class is the same as this one.
	 */
	public boolean is(Class<?> c) {
		return nn(this.c) && this.c.equals(c);
	}

	/**
	 * Checks for equality with the specified class.
	 *
	 * @param c The class to check equality with.
	 * @return <jk>true</jk> if the specified class is the same as this one.
	 */
	public boolean is(ClassInfo c) {
		if (nn(this.c))
			return this.c.equals(c.inner());
		return t.equals(c.t);
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this field.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this field.
	 */
	public boolean is(ReflectFlags...flags) {
		return isAll(flags);
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 *
	 * <p>
	 * Note that interfaces are always reported as abstract.
	 *
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() { return nn(c) && Modifier.isAbstract(c.getModifiers()); }

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this class.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this class.
	 */
	public boolean isAll(ReflectFlags...flags) {
		for (var f : flags) {
			switch (f) {
				case DEPRECATED:
					if (isNotDeprecated())
						return false;
					break;
				case NOT_DEPRECATED:
					if (isDeprecated())
						return false;
					break;
				case PUBLIC:
					if (isNotPublic())
						return false;
					break;
				case NOT_PUBLIC:
					if (isPublic())
						return false;
					break;
				case STATIC:
					if (isNotStatic())
						return false;
					break;
				case NOT_STATIC:
					if (isStatic())
						return false;
					break;
				case MEMBER:
					if (isNotMemberClass())
						return false;
					break;
				case NOT_MEMBER:
					if (isMemberClass())
						return false;
					break;
				case ABSTRACT:
					if (isNotAbstract())
						return false;
					break;
				case NOT_ABSTRACT:
					if (isAbstract())
						return false;
					break;
				case INTERFACE:
					if (isClass())
						return false;
					break;
				case CLASS:
					if (isInterface())
						return false;
					break;
				default:
					throw runtimeException("Invalid flag for class: {0}", f);

			}
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class is an annotation.
	 *
	 * @return <jk>true</jk> if this class is an annotation.
	 */
	public boolean isAnnotation() { return nn(c) && c.isAnnotation(); }

	/**
	 * Returns <jk>true</jk> if this class is an anonymous class.
	 *
	 * <p>
	 * An anonymous class is a local class declared within a method or constructor that has no name.
	 *
	 * @return <jk>true</jk> if this class is an anonymous class.
	 */
	public boolean isAnonymousClass() { return nn(c) && c.isAnonymousClass(); }

	/**
	 * Returns <jk>true</jk> if this class is any of the specified types.
	 *
	 * @param types The types to check against.
	 * @return <jk>true</jk> if this class is any of the specified types.
	 */
	public boolean isAny(Class<?>...types) {
		for (var cc : types)
			if (is(cc))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this class.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this class.
	 */
	public boolean isAny(ReflectFlags...flags) {
		for (var f : flags) {
			switch (f) {
				case DEPRECATED:
					if (isDeprecated())
						return true;
					break;
				case NOT_DEPRECATED:
					if (isNotDeprecated())
						return true;
					break;
				case PUBLIC:
					if (isPublic())
						return true;
					break;
				case NOT_PUBLIC:
					if (isNotPublic())
						return true;
					break;
				case STATIC:
					if (isStatic())
						return true;
					break;
				case NOT_STATIC:
					if (isNotStatic())
						return true;
					break;
				case MEMBER:
					if (isMemberClass())
						return true;
					break;
				case NOT_MEMBER:
					if (isNotMemberClass())
						return true;
					break;
				case ABSTRACT:
					if (isAbstract())
						return true;
					break;
				case NOT_ABSTRACT:
					if (isNotAbstract())
						return true;
					break;
				case INTERFACE:
					if (isInterface())
						return true;
					break;
				case CLASS:
					if (isClass())
						return true;
					break;
				default:
					throw runtimeException("Invalid flag for class: {0}", f);
			}
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is an array.
	 *
	 * @return <jk>true</jk> if this class is an array.
	 */
	public boolean isArray() { return nn(c) && c.isArray(); }

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 */
	public boolean isChildOf(Class<?> parent) {
		return nn(c) && nn(parent) && parent.isAssignableFrom(c);
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>parent</c>.
	 */
	public boolean isChildOf(ClassInfo parent) {
		return isChildOf(parent.inner());
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>parent</c>.
	 */
	public boolean isChildOf(Type parent) {
		if (parent instanceof Class)
			return isChildOf((Class<?>)parent);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as any of the <c>parents</c>.
	 *
	 * @param parents The parents class.
	 * @return <jk>true</jk> if this class is a child or the same as any of the <c>parents</c>.
	 */
	public boolean isChildOfAny(Class<?>...parents) {
		for (var p : parents)
			if (isChildOf(p))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is not an interface.
	 *
	 * @return <jk>true</jk> if this class is not an interface.
	 */
	public boolean isClass() { return nn(c) && ! c.isInterface(); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Collection} or an array.
	 *
	 * @return <jk>true</jk> if this class is a {@link Collection} or an array.
	 */
	public boolean isCollectionOrArray() { return nn(c) && (Collection.class.isAssignableFrom(c) || c.isArray()); }

	/**
	 * Returns <jk>true</jk> if this class has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this class has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isDeprecated() { return nn(c) && c.isAnnotationPresent(Deprecated.class); }

	/**
	 * Returns <jk>true</jk> if this class is an enum.
	 *
	 * @return <jk>true</jk> if this class is an enum.
	 */
	public boolean isEnum() { return nn(c) && c.isEnum(); }

	/**
	 * Returns <jk>true</jk> if the specified value is an instance of this class.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the specified value is an instance of this class.
	 */
	public boolean isInstance(Object value) {
		if (nn(this.c))
			return c.isInstance(value);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is an interface.
	 *
	 * @return <jk>true</jk> if this class is an interface.
	 */
	public boolean isInterface() { return nn(c) && c.isInterface(); }

	/**
	 * Returns <jk>true</jk> if this class is a local class.
	 *
	 * @return <jk>true</jk> if this class is a local class.
	 */
	public boolean isLocalClass() { return nn(c) && c.isLocalClass(); }

	/**
	 * Returns <jk>true</jk> if this class is a member class.
	 *
	 * @return <jk>true</jk> if this class is a member class.
	 */
	public boolean isMemberClass() { return nn(c) && c.isMemberClass(); }

	/**
	 * Determines if this class and the specified class are nestmates.
	 *
	 * <p>
	 * Two classes are nestmates if they belong to the same nest.
	 *
	 * @param c The class to check.
	 * @return <jk>true</jk> if this class and the specified class are nestmates.
	 */
	public boolean isNestmateOf(Class<?> c) {
		return nn(this.c) && nn(c) && this.c.isNestmateOf(c);
	}

	/**
	 * Returns <jk>true</jk> if this class is a member class and not static.
	 *
	 * @return <jk>true</jk> if this class is a member class and not static.
	 */
	public boolean isNonStaticMemberClass() { return nn(c) && c.isMemberClass() && ! isStatic(); }

	/**
	 * Returns <jk>true</jk> if this class is not abstract.
	 *
	 * <p>
	 * Note that interfaces are always reported as abstract.
	 *
	 * @return <jk>true</jk> if this class is not abstract.
	 */
	public boolean isNotAbstract() { return c == null || ! Modifier.isAbstract(c.getModifiers()); }

	/**
	 * Returns <jk>true</jk> if this class doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this class doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() { return c == null || ! c.isAnnotationPresent(Deprecated.class); }

	/**
	 * Returns <jk>true</jk> if this class is a local class.
	 *
	 * @return <jk>true</jk> if this class is a local class.
	 */
	public boolean isNotLocalClass() { return c == null || ! c.isLocalClass(); }

	/**
	 * Returns <jk>true</jk> if this class is a member class.
	 *
	 * @return <jk>true</jk> if this class is a member class.
	 */
	public boolean isNotMemberClass() { return c == null || ! c.isMemberClass(); }

	/**
	 * Returns <jk>false</jk> if this class is a member class and not static.
	 *
	 * @return <jk>false</jk> if this class is a member class and not static.
	 */
	public boolean isNotNonStaticMemberClass() { return ! isNonStaticMemberClass(); }

	/**
	 * Returns <jk>true</jk> if this is not a primitive class.
	 *
	 * @return <jk>true</jk> if this is not a primitive class.
	 */
	public boolean isNotPrimitive() { return c == null || ! c.isPrimitive(); }

	/**
	 * Returns <jk>true</jk> if this class is not public.
	 *
	 * @return <jk>true</jk> if this class is not public.
	 */
	public boolean isNotPublic() { return c == null || ! Modifier.isPublic(c.getModifiers()); }

	/**
	 * Returns <jk>true</jk> if this class is not static.
	 *
	 * <p>
	 * Note that interfaces are always reported as static, and the static keyword on a member interface is meaningless.
	 *
	 * @return <jk>true</jk> if this class is not static.
	 */
	public boolean isNotStatic() { return c == null || ! Modifier.isStatic(c.getModifiers()); }

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOf(Class<?> child) {
		return nn(c) && nn(child) && c.isAssignableFrom(child);
	}

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOf(Type child) {
		if (child instanceof Class)
			return isParentOf((Class<?>)child);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 *
	 * <p>
	 * Primitive classes are converted to wrapper classes and compared.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 		ClassInfo.<jsm>of</jsm>(String.<jk>class</jk>).isParentOfFuzzyPrimitives(String.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(CharSequence.<jk>class</jk>).isParentOfFuzzyPrimitives(String.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(String.<jk>class</jk>).isParentOfFuzzyPrimitives(CharSequence.<jk>class</jk>);  <jc>// false</jc>
	 * 		ClassInfo.<jsm>of</jsm>(<jk>int</jk>.<jk>class</jk>).isParentOfFuzzyPrimitives(Integer.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(Integer.<jk>class</jk>).isParentOfFuzzyPrimitives(<jk>int</jk>.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(Number.<jk>class</jk>).isParentOfFuzzyPrimitives(<jk>int</jk>.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(<jk>int</jk>.<jk>class</jk>).isParentOfFuzzyPrimitives(Number.<jk>class</jk>);  <jc>// false</jc>
	 * 		ClassInfo.<jsm>of</jsm>(<jk>int</jk>.<jk>class</jk>).isParentOfFuzzyPrimitives(<jk>long</jk>.<jk>class</jk>);  <jc>// false</jc>
	 * </p>
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOfFuzzyPrimitives(Class<?> child) {
		if (c == null || child == null)
			return false;
		if (c.isAssignableFrom(child))
			return true;
		if (this.isPrimitive() || child.isPrimitive()) {
			return this.getWrapperIfPrimitive().isAssignableFrom(of(child).getWrapperIfPrimitive());
		}
		return false;
	}

	/**
	 * Same as {@link #isParentOfFuzzyPrimitives(Class)} but takes in a {@link ClassInfo}.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOfFuzzyPrimitives(ClassInfo child) {
		if (c == null || child == null)
			return false;
		if (c.isAssignableFrom(child.inner()))
			return true;
		if (this.isPrimitive() || child.isPrimitive()) {
			return this.getWrapperIfPrimitive().isAssignableFrom(child.getWrapperIfPrimitive());
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOfFuzzyPrimitives(Type child) {
		if (child instanceof Class)
			return isParentOfFuzzyPrimitives((Class<?>)child);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this is a primitive class.
	 *
	 * @return <jk>true</jk> if this is a primitive class.
	 */
	public boolean isPrimitive() { return nn(c) && c.isPrimitive(); }

	/**
	 * Returns <jk>true</jk> if this class is public.
	 *
	 * @return <jk>true</jk> if this class is public.
	 */
	public boolean isPublic() { return nn(c) && Modifier.isPublic(c.getModifiers()); }

	/**
	 * Returns <jk>true</jk> if this is a repeated annotation class.
	 *
	 * <p>
	 * A repeated annotation has a single <code>value()</code> method that returns an array
	 * of annotations who themselves are marked with the {@link Repeatable @Repeatable} annotation
	 * of this class.
	 *
	 * @return <jk>true</jk> if this is a repeated annotation class.
	 */
	public boolean isRepeatedAnnotation() {
		if (isRepeatedAnnotation == null) {
			synchronized (this) {
				boolean b = false;
				repeatedAnnotationMethod = getPublicMethod(x -> x.hasName("value"));
				if (nn(repeatedAnnotationMethod)) {
					ClassInfo rt = repeatedAnnotationMethod.getReturnType();
					if (rt.isArray()) {
						ClassInfo rct = rt.getComponentType();
						if (rct.hasAnnotation(Repeatable.class)) {
							Repeatable r = rct.getAnnotation(Repeatable.class);
							b = r.value().equals(c);
						}
					}
				}
				isRepeatedAnnotation = b;
			}
		}
		return isRepeatedAnnotation;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link RuntimeException}.
	 *
	 * @return <jk>true</jk> if this class is a {@link RuntimeException}.
	 */
	public boolean isRuntimeException() { return isChildOf(RuntimeException.class); }

	/**
	 * Returns <jk>true</jk> if this class is a record class.
	 *
	 * <p>
	 * A record class is a final class that extends {@link java.lang.Record}.
	 *
	 * @return <jk>true</jk> if this class is a record class.
	 */
	public boolean isRecord() { return nn(c) && c.isRecord(); }

	/**
	 * Returns <jk>true</jk> if this class is a sealed class.
	 *
	 * <p>
	 * A sealed class is a class that can only be extended by a permitted set of subclasses.
	 *
	 * @return <jk>true</jk> if this class is a sealed class.
	 */
	public boolean isSealed() { return nn(c) && c.isSealed(); }

	/**
	 * Returns <jk>true</jk> if this class is public.
	 *
	 * <p>
	 * Note that interfaces are always reported as static, and the static keyword on a member interface is meaningless.
	 *
	 * @return <jk>true</jk> if this class is public.
	 */
	public boolean isStatic() { return nn(c) && Modifier.isStatic(c.getModifiers()); }

	/**
	 * Returns <jk>true</jk> if this class is a synthetic class.
	 *
	 * <p>
	 * A synthetic class is one that is generated by the compiler and does not appear in source code.
	 *
	 * @return <jk>true</jk> if this class is synthetic.
	 */
	public boolean isSynthetic() { return nn(c) && c.isSynthetic(); }

	/**
	 * Returns <jk>true</jk> if this class is a child of <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a parent of <c>child</c>.
	 */
	public boolean isStrictChildOf(Class<?> parent) {
		return nn(c) && nn(parent) && parent.isAssignableFrom(c) && ! c.equals(parent);
	}

	/**
	 * Identifies if the specified visibility matches this constructor.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this constructor.
	 */
	public boolean isVisible(Visibility v) {
		return nn(c) && v.isVisible(c);
	}

	/**
	 * Returns the last matching annotation on this class and superclasses/interfaces.
	 *
	 * <p>
	 * Annotations are searched in the following orders:
	 * <ol>
	 * 	<li>On this class.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On the package of this class.
	 * </ol>
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if annotation should be returned.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> A lastAnnotation(AnnotationProvider annotationProvider, Class<A> type, Predicate<A> filter) {
		if (annotationProvider == null)
			annotationProvider = AnnotationProvider.DEFAULT;
		A x = null;
		ClassInfo[] parents = _getParents();
		for (var parent : parents) {
			x = annotationProvider.lastAnnotation(type, parent.inner(), filter);
			if (nn(x))
				return x;
		}
		ClassInfo[] interfaces = _getInterfaces();
		for (var element : interfaces) {
			x = annotationProvider.lastAnnotation(type, element.inner(), filter);
			if (nn(x))
				return x;
		}
		x = getPackageAnnotation(type);
		if (nn(x) && test(filter, x))
			return x;
		return null;
	}

	/**
	 * Returns the last matching annotation on this class and superclasses/interfaces.
	 *
	 * <p>
	 * Annotations are searched in the following orders:
	 * <ol>
	 * 	<li>On this class.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On the package of this class.
	 * </ol>
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @param filter A predicate to apply to the entries to determine if annotation should be returned.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> A lastAnnotation(Class<A> type, Predicate<A> filter) {
		return lastAnnotation(null, type, filter);
	}

	/**
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<ClassInfo> test) {
		return test(test, this);
	}

	/**
	 * Shortcut for calling <c>Class.getDeclaredConstructor().newInstance()</c> on the underlying class.
	 *
	 * @return A new instance of the underlying class
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public Object newInstance() throws ExecutableException {
		if (c == null)
			throw new ExecutableException("Type ''{0}'' cannot be instantiated", getFullName());
		try {
			return c.getDeclaredConstructor().newInstance();
		} catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
			throw new ExecutableException(e);
		}
	}

	@Override
	public String toString() {
		return t.toString();
	}

	/**
	 * Unwrap this class if it's a parameterized type of the specified type such as {@link Value} or {@link Optional}.
	 *
	 * @param wrapperTypes The parameterized types to unwrap if this class is one of those types.
	 * @return The class info on the unwrapped type, or just this type if this isn't one of the specified types.
	 */
	public ClassInfo unwrap(Class<?>...wrapperTypes) {
		for (var wt : wrapperTypes) {
			if (isParameterizedTypeOf(wt)) {
				Type t = getFirstParameterType(wt);
				if (nn(t))
					return of(t).unwrap(wrapperTypes); // Recursively do it again.
			}
		}
		return this;
	}

	/**
	 * Casts an object to the class represented by this {@link ClassInfo} object.
	 *
	 * @param <T> The type to cast to.
	 * @param obj The object to be cast.
	 * @return The object after casting, or <jk>null</jk> if obj is <jk>null</jk>.
	 * @throws ClassCastException If the object is not <jk>null</jk> and is not assignable to this class.
	 */
	@SuppressWarnings("unchecked")
	public <T> T cast(Object obj) {
		return c == null ? null : (T)c.cast(obj);
	}

	/**
	 * Casts this {@link ClassInfo} object to represent a subclass of the class represented by the specified class object.
	 *
	 * @param <U> The type to cast to.
	 * @param clazz The class of the type to cast to.
	 * @return This {@link ClassInfo} object, cast to represent a subclass of the specified class object.
	 * @throws ClassCastException If this class is not assignable to the specified class.
	 */
	public <U> ClassInfo asSubclass(Class<U> clazz) {
		if (c == null)
			return null;
		c.asSubclass(clazz);  // Throws ClassCastException if not assignable
		return this;
	}

	/**
	 * Returns a {@link ClassInfo} for an array type whose component type is this class.
	 *
	 * @return A {@link ClassInfo} representing an array type whose component type is this class.
	 */
	public ClassInfo arrayType() {
		return c == null ? null : of(c.arrayType());
	}

	/**
	 * Returns the component type of this class if it is an array type.
	 *
	 * <p>
	 * This is equivalent to {@link Class#getComponentType()} but returns a {@link ClassInfo} instead.
	 * Note that {@link #getComponentType()} also exists and returns the base component type for multi-dimensional arrays.
	 *
	 * @return The {@link ClassInfo} representing the component type, or <jk>null</jk> if this class does not represent an array type.
	 */
	public ClassInfo componentType() {
		return c == null ? null : of(c.componentType());
	}

	private synchronized List<MethodInfo> _appendDeclaredMethods(List<MethodInfo> l) {
		for (var mi : _getDeclaredMethods())
			l.add(mi);
		return l;
	}

	private <A extends Annotation> A findAnnotation(AnnotationProvider ap, Class<A> a) {
		if (a == null)
			return null;
		if (ap == null)
			ap = AnnotationProvider.DEFAULT;
		A t = ap.firstDeclaredAnnotation(a, c, x -> true);
		if (nn(t))
			return t;
		ClassInfo sci = getSuperclass();
		if (nn(sci)) {
			t = sci.getAnnotation(ap, a);
			if (nn(t))
				return t;
		}
		for (var c2 : _getInterfaces()) {
			t = c2.getAnnotation(ap, a);
			if (nn(t))
				return t;
		}
		return null;
	}

	private <A extends Annotation> A getAnnotation(AnnotationProvider ap, Class<A> a, Predicate<A> filter) {
		if (ap == null)
			ap = AnnotationProvider.DEFAULT;
		A t2 = getPackageAnnotation(a);
		if (nn(t2) && filter.test(t2))
			return t2;
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length - 1; i >= 0; i--) {
			A o = ap.firstDeclaredAnnotation(a, interfaces[i].inner(), filter);
			if (nn(o))
				return o;
		}
		ClassInfo[] parents = _getParents();
		for (int i = parents.length - 1; i >= 0; i--) {
			A o = ap.firstDeclaredAnnotation(a, parents[i].inner(), filter);
			if (nn(o))
				return o;
		}
		return null;
	}

	private Type getFirstParameterType(Class<?> parameterizedType) {
		if (t instanceof ParameterizedType) {
		var pt = (ParameterizedType)t;
		Type[] ta = pt.getActualTypeArguments();
		if (ta.length > 0)
			return ta[0];
	} else if (t instanceof Class<?> c) /* Class that extends Optional<T> */ {
		if (c != parameterizedType && parameterizedType.isAssignableFrom(c))
			return ClassInfo.of(c).getParameterType(0, parameterizedType);
		}
		return null;
	}

	private boolean isParameterizedTypeOf(Class<?> c) {
		return (t instanceof ParameterizedType && ((ParameterizedType)t).getRawType() == c) || (t instanceof Class && c.isAssignableFrom((Class<?>)t));
	}

	FieldInfo[] _getAllFields() {
		if (allFields == null) {
			synchronized (this) {
				List<FieldInfo> l = list();
				ClassInfo[] parents = _getAllParents();
				for (int i = parents.length - 1; i >= 0; i--)
					for (var f : parents[i]._getDeclaredFields())
						l.add(f);
				allFields = l.toArray(new FieldInfo[l.size()]);
			}
		}
		return allFields;
	}

	MethodInfo[] _getAllMethods() {
		if (allMethods == null) {
			synchronized (this) {
				List<MethodInfo> l = list();
				for (var c : _getAllParents())
					c._appendDeclaredMethods(l);
				allMethods = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return allMethods;
	}

	MethodInfo[] _getAllMethodsParentFirst() {
		if (allMethodsParentFirst == null) {
			synchronized (this) {
				List<MethodInfo> l = list();
				ClassInfo[] parents = _getAllParents();
				for (int i = parents.length - 1; i >= 0; i--)
					parents[i]._appendDeclaredMethods(l);
				allMethodsParentFirst = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return allMethodsParentFirst;
	}

	/** Results are classes-before-interfaces, then child-to-parent order. */
	ClassInfo[] _getAllParents() {
		if (allParents == null) {
			synchronized (this) {
				var a1 = _getParents();
				var a2 = _getInterfaces();
				ClassInfo[] l = new ClassInfo[a1.length + a2.length];
				for (int i = 0; i < a1.length; i++)
					l[i] = a1[i];
				for (int i = 0; i < a2.length; i++)
					l[i + a1.length] = a2[i];
				allParents = l;
			}
		}
		return allParents;
	}

	Annotation[] _getDeclaredAnnotations() {
		if (declaredAnnotations == null) {
			synchronized (this) {
				declaredAnnotations = c.getDeclaredAnnotations();
			}
		}
		return declaredAnnotations;
	}

	ConstructorInfo[] _getDeclaredConstructors() {
		if (declaredConstructors == null) {
			synchronized (this) {
				Constructor<?>[] cc = c == null ? new Constructor[0] : c.getDeclaredConstructors();
				List<ConstructorInfo> l = listOfSize(cc.length);
				for (var ccc : cc)
					l.add(getConstructorInfo(ccc));
				l.sort(null);
				declaredConstructors = l.toArray(new ConstructorInfo[l.size()]);
			}
		}
		return declaredConstructors;
	}

	FieldInfo[] _getDeclaredFields() {
		if (declaredFields == null) {
			synchronized (this) {
				Field[] ff = c == null ? new Field[0] : c.getDeclaredFields();
				List<FieldInfo> l = listOfSize(ff.length);
				for (var f : ff)
					if (! "$jacocoData".equals(f.getName()))
						l.add(getFieldInfo(f));
				l.sort(null);
				declaredFields = l.toArray(new FieldInfo[l.size()]);
			}
		}
		return declaredFields;
	}

	/** Results are in the same order as Class.getInterfaces(). */
	ClassInfo[] _getDeclaredInterfaces() {
		if (declaredInterfaces == null) {
			synchronized (this) {
				Class<?>[] ii = c == null ? new Class[0] : c.getInterfaces();
				ClassInfo[] l = new ClassInfo[ii.length];
				for (int i = 0; i < ii.length; i++)
					l[i] = of(ii[i]);
				declaredInterfaces = l;
			}
		}
		return declaredInterfaces;
	}

	MethodInfo[] _getDeclaredMethods() {
		if (declaredMethods == null) {
			synchronized (this) {
				Method[] mm = c == null ? new Method[0] : c.getDeclaredMethods();
				List<MethodInfo> l = listOfSize(mm.length);
				for (var m : mm)
					if (! "$jacocoInit".equals(m.getName())) // Jacoco adds its own simulated methods.
						l.add(getMethodInfo(m));
				l.sort(null);
				declaredMethods = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return declaredMethods;
	}

	/** Results are in child-to-parent order. */
	ClassInfo[] _getInterfaces() {
		if (interfaces == null) {
			synchronized (this) {
				Set<ClassInfo> s = set();
				for (var ci : _getParents())
					for (var ci2 : ci._getDeclaredInterfaces()) {
						s.add(ci2);
						for (var ci3 : ci2._getInterfaces())
							s.add(ci3);
					}
				interfaces = s.toArray(new ClassInfo[s.size()]);
			}
		}
		return interfaces;
	}

	/** Results are in child-to-parent order. */
	ClassInfo[] _getParents() {
		if (parents == null) {
			synchronized (this) {
				List<ClassInfo> l = list();
				Class<?> pc = c;
				while (nn(pc) && pc != Object.class) {
					l.add(of(pc));
					pc = pc.getSuperclass();
				}
				parents = l.toArray(new ClassInfo[l.size()]);
			}
		}
		return parents;
	}

	ConstructorInfo[] _getPublicConstructors() {
		if (publicConstructors == null) {
			synchronized (this) {
				Constructor<?>[] cc = c == null ? new Constructor[0] : c.getConstructors();
				List<ConstructorInfo> l = listOfSize(cc.length);
				for (var ccc : cc)
					l.add(getConstructorInfo(ccc));
				l.sort(null);
				publicConstructors = l.toArray(new ConstructorInfo[l.size()]);
			}
		}
		return publicConstructors;
	}

	FieldInfo[] _getPublicFields() {
		if (publicFields == null) {
			synchronized (this) {
				Map<String,FieldInfo> m = map();
				for (var c : _getParents()) {
					for (var f : c._getDeclaredFields()) {
						String fn = f.getName();
						if (f.isPublic() && ! (m.containsKey(fn) || "$jacocoData".equals(fn)))
							m.put(f.getName(), f);
					}
				}
				List<FieldInfo> l = toList(m.values());
				l.sort(null);
				publicFields = l.toArray(new FieldInfo[l.size()]);
			}
		}
		return publicFields;
	}

	MethodInfo[] _getPublicMethods() {
		if (publicMethods == null) {
			synchronized (this) {
				Method[] mm = c == null ? new Method[0] : c.getMethods();
				List<MethodInfo> l = listOfSize(mm.length);
				for (var m : mm)
					if (m.getDeclaringClass() != Object.class)
						l.add(getMethodInfo(m));
				l.sort(null);
				publicMethods = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return publicMethods;
	}

	ConstructorInfo getConstructorInfo(Constructor<?> x) {
		ConstructorInfo i = constructors.get(x);
		if (i == null) {
			i = new ConstructorInfo(this, x);
			constructors.put(x, i);
		}
		return i;
	}

	FieldInfo getFieldInfo(Field x) {
		FieldInfo i = fields.get(x);
		if (i == null) {
			i = new FieldInfo(this, x);
			fields.put(x, i);
		}
		return i;
	}

	MethodInfo getMethodInfo(Method x) {
		MethodInfo i = methods.get(x);
		if (i == null) {
			i = new MethodInfo(this, x);
			methods.put(x, i);
		}
		return i;
	}
}