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
package org.apache.juneau.reflect;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

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
 * <p class='bcode w800'>
 * 	<jc>// Wrap our class inside a ClassInfo.</jc>
 * 	ClassInfo ci = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 *
 * 	<jc>// Get all methods in parent-to-child order, sorted alphabetically per class.</jc>
 * 	<jk>for</jk> (MethodInfo mi : ci.getAllMethodInfos(<jk>true</jk>, <jk>true</jk>)) {
 * 		<jc>// Do something with it.</jc>
 * 	}
 *
 * 	<jc>// Get all class-level annotations in parent-to-child order.</jc>
 * 	<jk>for</jk> (MyAnnotation a : ci.getAnnotations(MyAnnotation.<jk>class</jk>, <jk>true</jk>)) {
 * 		// Do something with it.
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class ClassInfo {

	/** Reusable ClassInfo for Object class. */
	public static final ClassInfo OBJECT = ClassInfo.of(Object.class);

	private final Type t;
	final Class<?> c;
	private final boolean isParameterizedType;
	private Boolean isRepeatedAnnotation;
	private ClassInfo[] interfaces, declaredInterfaces, parents, allParents;
	private MethodInfo[] publicMethods, declaredMethods, allMethods, allMethodsParentFirst;
	private MethodInfo repeatedAnnotationMethod;
	private ConstructorInfo[] publicConstructors, declaredConstructors;
	private FieldInfo[] publicFields, declaredFields, allFields, allFieldsParentFirst;
	private int dim = -1;
	private ClassInfo componentType;

	private static final Map<Class<?>,ClassInfo> CACHE = new ConcurrentHashMap<>();

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation
	//-----------------------------------------------------------------------------------------------------------------

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
	 * Returns a class info wrapper around the specified class type.
	 *
	 * @param t The class type.
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo of(Type t) {
		if (t == null)
			return null;
		return new ClassInfo(ClassUtils.toClass(t), t);
	}

	/**
	 * Returns a class info wrapper around the specified class type.
	 *
	 * @param c The class type.
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo of(Class<?> c) {
		if (c == null)
			return null;
		return new ClassInfo(c, c);
	}

	/**
	 * Same as {@link #of(Class)}} but caches the result for faster future lookup.
	 *
	 * @param c The class type.
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo ofc(Class<?> c) {
		if (c == null)
			return null;
		ClassInfo ci = CACHE.get(c);
		if (ci == null) {
			ci = ClassInfo.of(c);
			CACHE.put(c, ci);
		}
		return ci;
	}

	/**
	 * Returns a class info wrapper around the specified class type.
	 *
	 * @param c The class type.
	 * @param t The generic type (if parameterized type).
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo of(Class<?> c, Type t) {
		return new ClassInfo(c, t);
	}

	/**
	 * Same as using the constructor, but operates on an object instance.
	 *
	 * @param o The class instance.
	 * @return The constructed class info, or <jk>null</jk> if the object was <jk>null</jk>.
	 */
	public static ClassInfo of(Object o) {
		if (o == null)
			return null;
		return new ClassInfo(o.getClass(), o.getClass());
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

	/**
	 * Same as {@link #of(Object)}} but caches the result for faster future lookup.
	 *
	 * @param o The class instance.
	 * @return The constructed class info, or <jk>null</jk> if the type was <jk>null</jk>.
	 */
	public static ClassInfo ofc(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ClassInfo ci = CACHE.get(c);
		if (ci == null) {
			ci = ClassInfo.of(o);
			CACHE.put(c, ci);
		}
		return ci;
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
		for (Method m : c.getMethods()) {
			if (m.getName().equals("getTargetClass") && m.getParameterCount() == 0 && m.getReturnType().equals(Class.class)) {
				try {
					return (Class<?>) m.invoke(o);
				} catch (Exception e) {}
			}
		}
		return null;
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
	 * Returns the wrapped class as a {@link Class}.
	 *
	 * @return The wrapped class as a {@link Class}, or <jk>null</jk> if it's not a class (e.g. it's a {@link ParameterizedType}).
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<T> inner() {
		return (Class<T>)c;
	}

	/**
	 * Unwrap this class if it's a parameterized type of the specified type such as {@link Value} or {@link Optional}.
	 *
	 * @param wrapperTypes The parameterized types to unwrap if this class is one of those types.
	 * @return The class info on the unwrapped type, or just this type if this isn't one of the specified types.
	 */
	public ClassInfo unwrap(Class<?>...wrapperTypes) {
		for (Class<?> wt : wrapperTypes) {
			if (isParameterizedTypeOf(wt)) {
				Type t = getFirstParameterType(wt);
				if (t != null)
					return of(t).unwrap(wrapperTypes); // Recursively do it again.
			}
		}
		return this;
	}

	private boolean isParameterizedTypeOf(Class<?> c) {
		return
			(t instanceof ParameterizedType && ((ParameterizedType)t).getRawType() == c)
			|| (t instanceof Class && c.isAssignableFrom((Class<?>)t));
	}

	private Type getFirstParameterType(Class<?> parameterizedType) {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			Type[] ta = pt.getActualTypeArguments();
			if (ta.length > 0)
				return ta[0];
		} else if (t instanceof Class) /* Class that extends Optional<T> */ {
			Class<?> c = (Class<?>)t;
			if (c != parameterizedType && parameterizedType.isAssignableFrom(c))
				return ClassInfo.of(c).getParameterType(0, parameterizedType);
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parent classes and interfaces.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parent class.
	 *
	 * @return
	 * 	The parent class, or <jk>null</jk> if the class has no parent.
	 */
	public ClassInfo getParent() {
		return c == null ? null : ofc(c.getSuperclass());
	}

	/**
	 * Returns a list of interfaces declared on this class.
	 *
	 * <p>
	 * Does not include interfaces declared on parent classes.
	 *
	 * @return
	 * 	An unmodifiable list of interfaces declared on this class.
	 * 	<br>Results are in the same order as {@link Class#getInterfaces()}.
	 */
	public List<ClassInfo> getDeclaredInterfaces() {
		return new UnmodifiableArray<>(_getDeclaredInterfaces());
	}

	/**
	 * Returns a list of interfaces defined on this class and superclasses.
	 *
	 * @return
	 * 	An unmodifiable list of interfaces defined on this class and superclasses.
	 * 	<br>Results are in child-to-parent order.
	 */
	public List<ClassInfo> getInterfaces() {
		return new UnmodifiableArray<>(_getInterfaces());
	}

	/**
	 * Returns a list including this class and all parent classes.
	 *
	 * <p>
	 * Does not include interfaces.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are in child-to-parent order.
	 */
	public List<ClassInfo> getParents() {
		return new UnmodifiableArray<>(_getParents());
	}

	/**
	 * Returns a list including this class and all parent classes.
	 *
	 * <p>
	 * Does not include interfaces.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are in parent-to-child order.
	 */
	public List<ClassInfo> getParentsParentFirst() {
		return new UnmodifiableArray<>(_getParents(), true);
	}

	/**
	 * Returns a list including this class and all parent classes and interfaces.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are ordered child-to-parent order with classes listed before interfaces.
	 */
	public List<ClassInfo> getAllParentsChildFirst() {
		return new UnmodifiableArray<>(_getAllParents());
	}

	/**
	 * Returns a list including this class and all parent classes and interfaces.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are ordered parent-to-child order with interfaces listed before classes.
	 */
	public List<ClassInfo> getAllParentsParentFirst() {
		return new UnmodifiableArray<>(_getAllParents(), true);
	}

	ClassInfo[] _getInterfaces() {
		if (interfaces == null) {
			Set<ClassInfo> s = new LinkedHashSet<>();
			for (ClassInfo ci : _getParents())
				for (ClassInfo ci2 : ci._getDeclaredInterfaces()) {
					s.add(ci2);
					for (ClassInfo ci3 : ci2._getInterfaces())
						s.add(ci3);
				}
			interfaces = s.toArray(new ClassInfo[s.size()]);
		}
		return interfaces;
	}

	private ClassInfo[] _getDeclaredInterfaces() {
		if (declaredInterfaces == null) {
			Class<?>[] ii = c == null ? new Class[0] : c.getInterfaces();
			ClassInfo[] l = new ClassInfo[ii.length];
			for (int i = 0; i < ii.length; i++)
				l[i] = of(ii[i]);
			declaredInterfaces = l;
		}
		return declaredInterfaces;
	}

	ClassInfo[] _getParents() {
		if (parents == null) {
			List<ClassInfo> l = new ArrayList<>();
			Class<?> pc = c;
			while (pc != null && pc != Object.class) {
				l.add(of(pc));
				pc = pc.getSuperclass();
			}
			parents = l.toArray(new ClassInfo[l.size()]);
		}
		return parents;
	}

	private ClassInfo[] _getAllParents() {
		if (allParents == null) {
			ClassInfo[] a1 = _getParents(), a2 = _getInterfaces();
			ClassInfo[] l = new ClassInfo[a1.length + a2.length];
			for (int i = 0; i < a1.length; i++)
				l[i] = a1[i];
			for (int i = 0; i < a2.length; i++)
				l[i+a1.length] = a2[i];
			allParents = l;
		}
		return allParents;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Methods
	//-----------------------------------------------------------------------------------------------------------------

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
	public List<MethodInfo> getPublicMethods() {
		return new UnmodifiableArray<>(_getPublicMethods());
	}

	/**
	 * Returns all methods declared on this class.
	 *
	 * @return
	 * 	All methods declared on this class.
	 * 	<br>Results are ordered alphabetically.
	 */
	public List<MethodInfo> getDeclaredMethods() {
		return new UnmodifiableArray<>(_getDeclaredMethods());
	}

	/**
	 * Returns all declared methods on this class and all parent classes.
	 *
	 * @return
	 * 	All declared methods on this class and all parent classes.
	 * 	<br>Results are ordered child-to-parent, and then alphabetically per class.
	 */
	public List<MethodInfo> getAllMethods() {
		return new UnmodifiableArray<>(_getAllMethods());
	}

	/**
	 * Returns the public method that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public final MethodInfo getPublicMethod(Predicate<MethodInfo> predicate) {
		for (MethodInfo mi : _getPublicMethods())
			if (predicate.test(mi))
				return mi;
		return null;
	}

	/**
	 * Returns the declared method that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getDeclaredMethod(Predicate<MethodInfo> predicate) {
		for (MethodInfo mi : _getDeclaredMethods())
			if (predicate.test(mi))
				return mi;
		return null;
	}

	/**
	 * Returns the method that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getMethod(Predicate<MethodInfo> predicate) {
		for (MethodInfo mi : _getAllMethods())
			if (predicate.test(mi))
				return mi;
		return null;
	}

	/**
	 * Returns the public method that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @param consumer The consumer of the matching predicate.
	 * @return This object.
	 */
	public final ClassInfo getPublicMethods(Predicate<MethodInfo> predicate, Consumer<MethodInfo> consumer) {
		for (MethodInfo mi : _getPublicMethods())
			if (predicate.test(mi))
				consumer.accept(mi);
		return this;
	}

	/**
	 * Returns the declared method that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @param consumer The consumer of the matching predicate.
	 * @return This object.
	 */
	public final ClassInfo getDeclaredMethods(Predicate<MethodInfo> predicate, Consumer<MethodInfo> consumer) {
		for (MethodInfo mi : _getDeclaredMethods())
			if (predicate.test(mi))
				consumer.accept(mi);
		return this;
	}

	/**
	 * Returns the method that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @param consumer The consumer of the matching predicate.
	 * @return This object.
	 */
	public final ClassInfo getMethods(Predicate<MethodInfo> predicate, Consumer<MethodInfo> consumer) {
		for (MethodInfo mi : _getAllMethods())
			if (predicate.test(mi))
				consumer.accept(mi);
		return this;
	}

	/**
	 * Returns all declared methods on this class and all parent classes.
	 *
	 *
	 * @return
	 * 	All declared methods on this class and all parent classes.
	 * 	<br>Results are ordered parent-to-child, and then alphabetically per class.
	 */
	public List<MethodInfo> getAllMethodsParentFirst() {
		return new UnmodifiableArray<>(_getAllMethodsParentFirst());
	}

	private MethodInfo[] _getPublicMethods() {
		if (publicMethods == null) {
			Method[] mm = c == null ? new Method[0] : c.getMethods();
			List<MethodInfo> l = new ArrayList<>(mm.length);
			for (Method m : mm)
				if (m.getDeclaringClass() != Object.class)
					l.add(MethodInfo.of(this, m));
			l.sort(null);
			publicMethods = l.toArray(new MethodInfo[l.size()]);
		}
		return publicMethods;
	}

	private MethodInfo[] _getDeclaredMethods() {
		if (declaredMethods == null) {
			Method[] mm = c == null ? new Method[0] : c.getDeclaredMethods();
			List<MethodInfo> l = new ArrayList<>(mm.length);
			for (Method m : mm)
				if (! "$jacocoInit".equals(m.getName())) // Jacoco adds its own simulated methods.
					l.add(MethodInfo.of(this, m));
			l.sort(null);
			declaredMethods = l.toArray(new MethodInfo[l.size()]);
		}
		return declaredMethods;
	}

	private MethodInfo[] _getAllMethods() {
		if (allMethods == null) {
			List<MethodInfo> l = new ArrayList<>();
			for (ClassInfo c : _getAllParents())
				c._appendDeclaredMethods(l);
			allMethods = l.toArray(new MethodInfo[l.size()]);
		}
		return allMethods;
	}

	private MethodInfo[] _getAllMethodsParentFirst() {
		if (allMethodsParentFirst == null) {
			List<MethodInfo> l = new ArrayList<>();
			ClassInfo[] parents = _getAllParents();
			for (int i = parents.length-1; i >=0; i--)
				parents[i]._appendDeclaredMethods(l);
			allMethodsParentFirst = l.toArray(new MethodInfo[l.size()]);
		}
		return allMethodsParentFirst;
	}

	private List<MethodInfo> _appendDeclaredMethods(List<MethodInfo> l) {
		for (MethodInfo mi : _getDeclaredMethods())
			l.add(mi);
		return l;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all the public constructors defined on this class.
	 *
	 * @return All public constructors defined on this class.
	 */
	public List<ConstructorInfo> getPublicConstructors() {
		return new UnmodifiableArray<>(_getPublicConstructors());
	}

	/**
	 * Returns the public constructor that matches the specified predicate.
	 *
	 * @param predicate The predicate.
	 * @return The public constructor that matches the specified predicate.
	 */
	public ConstructorInfo getPublicConstructor(Predicate<ConstructorInfo> predicate) {
		for (ConstructorInfo ci : _getPublicConstructors())
			if (predicate.test(ci))
				return ci;
		return null;
	}

	/**
	 * Returns the public constructor that passes the specified predicate test.
	 *
	 * <p>
	 * The {@link ReflectionFilters} class has predefined predicates that can be used for testing.
	 *
	 * @param test The test that the public constructor must pass.
	 * @return The first matching public constructor.
	 */
	public ConstructorInfo getConstructor(Predicate<ExecutableInfo> test) {
		for (ConstructorInfo ci : _getDeclaredConstructors())
			if (test.test(ci))
				return ci;
		return null;
	}

	/**
	 * Returns the declared constructor that matches the specified predicate.
	 *
	 * @param predicate The predicate to match.
	 * @return The declared constructor that matches the specified predicate.
	 */
	public ConstructorInfo getDeclaredConstructor(Predicate<ConstructorInfo> predicate) {
		for (ConstructorInfo ci : _getDeclaredConstructors())
			if (predicate.test(ci))
				return ci;
		return null;
	}

	/**
	 * Returns all the constructors defined on this class.
	 *
	 * @return All constructors defined on this class.
	 */
	public List<ConstructorInfo> getDeclaredConstructors() {
		return new UnmodifiableArray<>(_getDeclaredConstructors());
	}

	private ConstructorInfo[] _getPublicConstructors() {
		if (publicConstructors == null) {
			Constructor<?>[] cc = c == null ? new Constructor[0] : c.getConstructors();
			List<ConstructorInfo> l = new ArrayList<>(cc.length);
			for (Constructor<?> ccc : cc)
				l.add(ConstructorInfo.of(this, ccc));
			l.sort(null);
			publicConstructors = l.toArray(new ConstructorInfo[l.size()]);
		}
		return publicConstructors;
	}

	private ConstructorInfo[] _getDeclaredConstructors() {
		if (declaredConstructors == null) {
			Constructor<?>[] cc = c == null ? new Constructor[0] : c.getDeclaredConstructors();
			List<ConstructorInfo> l = new ArrayList<>(cc.length);
			for (Constructor<?> ccc : cc)
				l.add(ConstructorInfo.of(this, ccc));
			l.sort(null);
			declaredConstructors = l.toArray(new ConstructorInfo[l.size()]);
		}
		return declaredConstructors;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Special constructors
	//-----------------------------------------------------------------------------------------------------------------

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
		for (ConstructorInfo cc : _getDeclaredConstructors())
			if (cc.hasNumParams(isMemberClass ? 1 : 0) && cc.isVisible(v))
				return cc.accessible(v);
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fields
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all public fields on this class.
	 *
	 * <p>
	 * Hidden fields are excluded from the results.
	 *
	 * @return
	 * 	All public fields on this class.
	 * 	<br>Results are in alphabetical order.
	 */
	public List<FieldInfo> getPublicFields() {
		return new UnmodifiableArray<>(_getPublicFields());
	}

	/**
	 * Returns all declared fields on this class.
	 *
	 * @return
	 * 	All declared fields on this class.
	 * 	<br>Results are in alphabetical order.
	 */
	public List<FieldInfo> getDeclaredFields() {
		return new UnmodifiableArray<>(_getDeclaredFields());
	}

	/**
	 * Returns all declared fields on this class and all parent classes.
	 *
	 * @return
	 * 	All declared fields on this class.
	 * 	<br>Results are ordered child-to-parent, and then alphabetical per class.
	 */
	public List<FieldInfo> getAllFields() {
		return new UnmodifiableArray<>(_getAllFields());
	}

	/**
	 * Returns all declared fields on this class and all parent classes.
	 *
	 * @return
	 * 	All declared fields on this class.
	 * 	<br>Results are ordered parent-to-child, and then alphabetical per class.
	 */
	public List<FieldInfo> getAllFieldsParentFirst() {
		return new UnmodifiableArray<>(_getAllFieldsParentFirst());
	}

	/**
	 * Returns the public field with the specified name.
	 *
	 * @param name The field name.
	 * @return The public field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getPublicField(String name) {
		for (FieldInfo f : _getPublicFields())
			if (f.getName().equals(name))
				return f;
		return null;
	}

	/**
	 * Returns the declared field with the specified name.
	 *
	 * @param name The field name.
	 * @return The declared field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getDeclaredField(String name) {
		for (FieldInfo f : _getDeclaredFields())
			if (f.getName().equals(name))
				return f;
		return null;
	}

	/**
	 * Returns the static public field with the specified name.
	 *
	 * @param name The field name.
	 * @return The public field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getStaticPublicField(String name) {
		for (FieldInfo f : _getPublicFields())
			if (f.isStatic() && f.getName().equals(name))
				return f;
		return null;
	}

	/**
	 * Returns the static public field with the specified name.
	 *
	 * @param name The field name.
	 * @return The public field, or <jk>null</jk> if not found.
	 */
	public Field getStaticPublicFieldInner(String name) {
		for (FieldInfo f : _getPublicFields())
			if (f.isStatic() && f.getName().equals(name))
				return f.inner();
		return null;
	}

	private List<FieldInfo> _appendDeclaredFields(List<FieldInfo> l) {
		for (FieldInfo f : _getDeclaredFields())
			l.add(f);
		return l;
	}

	private Map<String,FieldInfo> _appendDeclaredPublicFields(Map<String,FieldInfo> m) {
		for (FieldInfo f : _getDeclaredFields()) {
			String fn = f.getName();
			if (f.isPublic() && ! (m.containsKey(fn) || "$jacocoData".equals(fn)))
					m.put(f.getName(), f);
		}
		return m;
	}

	private FieldInfo[] _getPublicFields() {
		if (publicFields == null) {
			Map<String,FieldInfo> m = new LinkedHashMap<>();
			for (ClassInfo c : _getParents())
				c._appendDeclaredPublicFields(m);
			List<FieldInfo> l = new ArrayList<>(m.values());
			l.sort(null);
			publicFields = l.toArray(new FieldInfo[l.size()]);
		}
		return publicFields;
	}

	private FieldInfo[] _getDeclaredFields() {
		if (declaredFields == null) {
			Field[] ff = c == null ? new Field[0] : c.getDeclaredFields();
			List<FieldInfo> l = new ArrayList<>(ff.length);
			for (Field f : ff)
				if (! "$jacocoData".equals(f.getName()))
					l.add(FieldInfo.of(this, f));
			l.sort(null);
			declaredFields = l.toArray(new FieldInfo[l.size()]);
		}
		return declaredFields;
	}

	private FieldInfo[] _getAllFields() {
		if (allFields == null) {
			List<FieldInfo> l = new ArrayList<>();
			for (ClassInfo c : _getAllParents())
				c._appendDeclaredFields(l);
			allFields = l.toArray(new FieldInfo[l.size()]);
		}
		return allFields;
	}

	private FieldInfo[] _getAllFieldsParentFirst() {
		if (allFieldsParentFirst == null) {
			List<FieldInfo> l = new ArrayList<>();
			ClassInfo[] parents = _getAllParents();
			for (int i = parents.length-1; i >=0; i--)
				parents[i]._appendDeclaredFields(l);
			allFieldsParentFirst = l.toArray(new FieldInfo[l.size()]);
		}
		return allFieldsParentFirst;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the annotation of the specified type defined on this class or parent class/interface.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate class, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The annotation if found, or <jk>null</jk> if not.
	 */
	public <T extends Annotation> T getLastAnnotation(Class<T> a) {
		return getLastAnnotation(a, MetaProvider.DEFAULT);
	}

	/**
	 * Finds the annotation of the specified type defined on this class or parent class/interface.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate class, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @param mp The meta provider for looking up annotations on reflection objects (classes, methods, fields, constructors).
	 * @return
	 * 	The annotation if found, or <jk>null</jk> if not.
	 */
	public <T extends Annotation> T getLastAnnotation(Class<T> a, MetaProvider mp) {
		return findAnnotation(a, mp);
	}

	/**
	 * Returns <jk>true</jk> if this class has the specified annotation.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	The <jk>true</jk> if annotation if found.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return getLastAnnotation(a) != null;
	}

	/**
	 * Returns the specified annotation only if it's been declared on this class.
	 *
	 * <p>
	 * More efficient than calling {@link Class#getAnnotation(Class)} since it doesn't recursively look for the class
	 * up the parent chain.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <T extends Annotation> T getDeclaredAnnotation(Class<T> a) {
		return a == null ? null : c.getDeclaredAnnotation(a);
	}

	/**
	 * Returns the specified annotation only if it's been declared on the package of this class.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <T extends Annotation> T getPackageAnnotation(Class<T> a) {
		Package p = c == null ? null : c.getPackage();
		return (p == null ? null : p.getAnnotation(a));
	}

	/**
	 * Same as {@link #getDeclaredAnnotation(Class)} but returns the annotation wrapped in a {@link AnnotationInfo}.
	 *
	 * @param a The annotation to search for.
	 * @return The annotation if found, or <jk>null</jk> if not.
	 */
	public <T extends Annotation> AnnotationInfo<T> getDeclaredAnnotationInfo(Class<T> a) {
		T ca = getDeclaredAnnotation(a);
		return ca == null ? null : AnnotationInfo.of(this, ca);
	}

	/**
	 * Same as {@link #getPackageAnnotation(Class)} but returns the annotation wrapped in a {@link AnnotationInfo}.
	 *
	 * @param a The annotation to search for.
	 * @return The annotation if found, or <jk>null</jk> if not.
	 */
	public <T extends Annotation> AnnotationInfo<T> getPackageAnnotationInfo(Class<T> a) {
		T ca = getPackageAnnotation(a);
		return ca == null ? null : AnnotationInfo.of(getPackage(), ca);
	}

	/**
	 * Returns all annotations of the specified type defined on the specified class or parent classes/interfaces in parent-to-child order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a) {
		return appendAnnotations(new ArrayList<>(), a);
	}

	/**
	 * Returns all annotations of the specified type defined on the specified class or parent classes/interfaces.
	 *
	 * <p>
	 * Returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @param mp The meta provider for looking up annotations on reflection objects (classes, methods, fields, constructors).
	 * @return
	 * 	A list of all matching annotations found or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a, MetaProvider mp) {
		return appendAnnotations(new ArrayList<>(), a, mp);
	}

	/**
	 * Same as getAnnotations(Class) except returns the annotations with the accompanying class.
	 *
	 * <p>
	 * Results are ordered parent-to-child.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @return The found matches, or an empty list if annotation was not found.
	 */
	public <T extends Annotation> List<AnnotationInfo<T>> getAnnotationInfos(Class<T> a) {
		return appendAnnotationInfos(new ArrayList<>(), a);
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
		return getAnnotationList(null);
	}

	/**
	 * Constructs an {@link AnnotationList} of all annotations found on this class that belong to the specified
	 * annotation group.
	 *
	 * @param group The annotation group.  See {@link AnnotationGroup}.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationGroupList(Class<? extends Annotation> group) {
		return getAnnotationList(x -> x.isInGroup(group));
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
	 * @param filter
	 * 	Optional filter to apply to limit which annotations are added to the list.
	 * 	<br>Can be <jk>null</jk> for no filtering.
	 * @return A new {@link AnnotationList} object on every call.
	 */
	public AnnotationList getAnnotationList(Predicate<AnnotationInfo<?>> filter) {
		return appendAnnotationList(new AnnotationList(filter));
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param l The list of annotations.
	 * @param a The annotation to search for.
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a) {
		return appendAnnotations(l, a, MetaProvider.DEFAULT);
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param l The list of annotations.
	 * @param a The annotation to search for.
	 * @param mp The meta provider for looking up annotations on reflection objects (classes, methods, fields, constructors).
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a, MetaProvider mp) {
		addIfNotNull(l, getPackageAnnotation(a));
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--)
			for (T t : mp.getDeclaredAnnotations(a, interfaces[i].inner()))
				l.add(t);
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--)
			for (T t : mp.getDeclaredAnnotations(a, parents[i].inner()))
				l.add(t);
		return l;
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * <p>
	 * Annotations are appended in the following orders:
	 * <ol>
	 * 	<li>On the package of this class.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On this class.
	 * </ol>
	 *
	 * @param l The list of annotations.
	 * @param a The annotation to search for.
	 * @return The same list.
	 */
	public <T extends Annotation> List<AnnotationInfo<T>> appendAnnotationInfos(List<AnnotationInfo<T>> l, Class<T> a) {
		addIfNotNull(l, getPackageAnnotationInfo(a));
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--)
			addIfNotNull(l, interfaces[i].getDeclaredAnnotationInfo(a));
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--)
			addIfNotNull(l, parents[i].getDeclaredAnnotationInfo(a));
		return l;
	}

	/**
	 * Searches up the parent hierarchy of this class for the first annotation in the list it finds.
	 *
	 * @param mp Metadata provider.
	 * @param annotations The annotations to search for.
	 * @return The first annotation found, or <jk>null</jk> if not found.
	 */
	@SafeVarargs
	public final Annotation getAnyLastAnnotation(MetaProvider mp, Class<? extends Annotation>...annotations) {
		for (Class<? extends Annotation> ca : annotations) {
			Annotation x = getLastAnnotation(ca, mp);
			if (x != null)
				return x;
		}
		for (ClassInfo ci : _getInterfaces()) {
			for (Class<? extends Annotation> ca : annotations) {
				Annotation x = ci.getLastAnnotation(ca, mp);
				if (x != null)
					return x;
			}
		}
		ClassInfo ci = getParent();
		return ci == null ? null : ci.getAnyLastAnnotation(mp, annotations);
	}


	AnnotationList appendAnnotationList(AnnotationList m) {
		Package p = c.getPackage();
		if (p != null)
			for (Annotation a : p.getDeclaredAnnotations())
				for (Annotation a2 : splitRepeated(a))
					m.add(AnnotationInfo.of(p, a2));
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--)
			for (Annotation a : interfaces[i].c.getDeclaredAnnotations())
				for (Annotation a2 : splitRepeated(a))
					m.add(AnnotationInfo.of(interfaces[i], a2));
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--)
			for (Annotation a : parents[i].c.getDeclaredAnnotations())
				for (Annotation a2 : splitRepeated(a))
					m.add(AnnotationInfo.of(parents[i], a2));
		return m;
	}

	/**
	 * If the annotation is an array of other annotations, returns the inner annotations.
	 *
	 * @param a The annotation to split if repeated.
	 * @return The nested annotations, or a singleton array of the same annotation if it's not repeated.
	 */
	private static Annotation[] splitRepeated(Annotation a) {
		try {
			ClassInfo ci = ClassInfo.ofc(a.annotationType());
			MethodInfo mi = ci.getRepeatedAnnotationMethod();
			if (mi != null)
				return mi.invoke(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Annotation[]{a};
	}

	<T extends Annotation> T findAnnotation(Class<T> a, MetaProvider mp) {
		if (a == null)
			return null;

		for (T t : mp.getDeclaredAnnotations(a, c))
			return t;

		T t;
		ClassInfo sci = getParent();
		if (sci != null) {
			t = sci.getLastAnnotation(a, mp);
			if (t != null)
				return t;
		}

		for (ClassInfo c2 : _getInterfaces()) {
			t = c2.getLastAnnotation(a, mp);
			if (t != null)
				return t;
		}

		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this class.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this class.
	 */
	public boolean isAll(ReflectFlags...flags) {
		for (ReflectFlags f : flags) {
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
	 * Returns <jk>true</jk> if all specified flags are applicable to this class.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this class.
	 */
	public boolean isAny(ReflectFlags...flags) {
		for (ReflectFlags f : flags) {
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
	 * Returns <jk>true</jk> if this class has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this class has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isDeprecated() {
		return c != null && c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this class doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this class doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() {
		return c == null || ! c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is public.
	 *
	 * @return <jk>true</jk> if this class is public.
	 */
	public boolean isPublic() {
		return c != null && Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is not public.
	 *
	 * @return <jk>true</jk> if this class is not public.
	 */
	public boolean isNotPublic() {
		return c == null || ! Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is public.
	 *
	 * <p>
	 * Note that interfaces are always reported as static, and the static keyword on a member interface is meaningless.
	 *
	 * @return <jk>true</jk> if this class is public.
	 */
	public boolean isStatic() {
		return c != null && Modifier.isStatic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is not static.
	 *
	 * <p>
	 * Note that interfaces are always reported as static, and the static keyword on a member interface is meaningless.
	 *
	 * @return <jk>true</jk> if this class is not static.
	 */
	public boolean isNotStatic() {
		return c == null || ! Modifier.isStatic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 *
	 * <p>
	 * Note that interfaces are always reported as abstract.
	 *
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() {
		return c != null && Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is not abstract.
	 *
	 * <p>
	 * Note that interfaces are always reported as abstract.
	 *
	 * @return <jk>true</jk> if this class is not abstract.
	 */
	public boolean isNotAbstract() {
		return c == null || ! Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is a member class.
	 *
	 * @return <jk>true</jk> if this class is a member class.
	 */
	public boolean isMemberClass() {
		return c != null && c.isMemberClass();
	}

	/**
	 * Returns <jk>true</jk> if this class is a member class.
	 *
	 * @return <jk>true</jk> if this class is a member class.
	 */
	public boolean isNotMemberClass() {
		return c == null || ! c.isMemberClass();
	}

	/**
	 * Returns <jk>true</jk> if this class is a member class and not static.
	 *
	 * @return <jk>true</jk> if this class is a member class and not static.
	 */
	public boolean isNonStaticMemberClass() {
		return c != null && c.isMemberClass() && ! isStatic();
	}

	/**
	 * Returns <jk>false</jk> if this class is a member class and not static.
	 *
	 * @return <jk>false</jk> if this class is a member class and not static.
	 */
	public boolean isNotNonStaticMemberClass() {
		return ! isNonStaticMemberClass();
	}

	/**
	 * Returns <jk>true</jk> if this class is a local class.
	 *
	 * @return <jk>true</jk> if this class is a local class.
	 */
	public boolean isLocalClass() {
		return c != null && c.isLocalClass();
	}

	/**
	 * Returns <jk>true</jk> if this class is a local class.
	 *
	 * @return <jk>true</jk> if this class is a local class.
	 */
	public boolean isNotLocalClass() {
		return c == null || ! c.isLocalClass();
	}

	/**
	 * Identifies if the specified visibility matches this constructor.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this constructor.
	 */
	public boolean isVisible(Visibility v) {
		return c != null && v.isVisible(c);
	}

	/**
	 * Returns <jk>true</jk> if this is a primitive class.
	 *
	 * @return <jk>true</jk> if this is a primitive class.
	 */
	public boolean isPrimitive() {
		return c != null && c.isPrimitive();
	}

	/**
	 * Returns <jk>true</jk> if this is not a primitive class.
	 *
	 * @return <jk>true</jk> if this is not a primitive class.
	 */
	public boolean isNotPrimitive() {
		return c == null || ! c.isPrimitive();
	}

	/**
	 * Returns <jk>true</jk> if this class is an interface.
	 *
	 * @return <jk>true</jk> if this class is an interface.
	 */
	public boolean isInterface() {
		return c != null && c.isInterface();
	}

	/**
	 * Returns <jk>true</jk> if this class is not an interface.
	 *
	 * @return <jk>true</jk> if this class is not an interface.
	 */
	public boolean isClass() {
		return c != null && ! c.isInterface();
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link RuntimeException}.
	 *
	 * @return <jk>true</jk> if this class is a {@link RuntimeException}.
	 */
	public boolean isRuntimeException() {
		return isChildOf(RuntimeException.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive wrappers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the {@link #getPrimitiveWrapper()} method returns a value.
	 *
	 * @return <jk>true</jk> if the {@link #getPrimitiveWrapper()} method returns a value.
	 */
	public boolean hasPrimitiveWrapper() {
		return pmap1.containsKey(c);
	}

	/**
	 * If this class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @return The wrapper class, or <jk>null</jk> if class is not a primitive.
	 */
	public Class<?> getPrimitiveWrapper() {
		return pmap1.get(c);
	}

	/**
	 * If this class is a primitive wrapper (e.g. <code><jk>Integer</jk>.<jk>class</jk></code>) returns it's
	 * primitive class (e.g. <code>int.<jk>class</jk></code>).
	 *
	 * @return The primitive class, or <jk>null</jk> if class is not a primitive wrapper.
	 */
	public Class<?> getPrimitiveForWrapper() {
		return pmap2.get(c);
	}

	/**
	 * If this class is a primitive (e.g. <code><jk>int</jk>.<jk>class</jk></code>) returns it's wrapper class
	 * (e.g. <code>Integer.<jk>class</jk></code>).
	 *
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public Class<?> getWrapperIfPrimitive() {
		if (c != null && ! c.isPrimitive())
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
	 * Returns the default value for this primitive class.
	 *
	 * @return The default value, or <jk>null</jk> if this is not a primitive class.
	 */
	public Object getPrimitiveDefault() {
		return primitiveDefaultMap.get(c);
	}

	private static final Map<Class<?>, Class<?>>
		pmap1 = new HashMap<>(),
		pmap2 = new HashMap<>();
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

	private static final Map<Class<?>,Object> primitiveDefaultMap = Collections.unmodifiableMap(
		AMap.<Class<?>,Object>create()
			.a(Boolean.TYPE, false)
			.a(Character.TYPE, (char)0)
			.a(Short.TYPE, (short)0)
			.a(Integer.TYPE, 0)
			.a(Long.TYPE, 0l)
			.a(Float.TYPE, 0f)
			.a(Double.TYPE, 0d)
			.a(Byte.TYPE, (byte)0)
			.a(Boolean.class, false)
			.a(Character.class, (char)0)
			.a(Short.class, (short)0)
			.a(Integer.class, 0)
			.a(Long.class, 0l)
			.a(Float.class, 0f)
			.a(Double.class, 0d)
			.a(Byte.class, (byte)0)
	);

	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

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
		if (ct != null && dim == 0 && ! isParameterizedType)
			return ct.getName();
		StringBuilder sb = new StringBuilder(128);
		appendFullName(sb);
		return sb.toString();
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
	public String[] getNames() {
		return new String[]{ getFullName(), c.getName(), getShortName(), getSimpleName() };
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
		if (ct != null && dim == 0 && ! isParameterizedType)
			return sb.append(ct.getName());
		sb.append(ct != null ? ct.getName() : t.getTypeName());
		if (isParameterizedType) {
			ParameterizedType pt = (ParameterizedType)t;
			sb.append('<');
			boolean first = true;
			for (Type t2 : pt.getActualTypeArguments()) {
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
		if (ct != null && dim == 0 && ! (isParameterizedType || isMemberClass() || c.isLocalClass()))
			return ct.getSimpleName();
		StringBuilder sb = new StringBuilder(32);
		appendShortName(sb);
		return sb.toString();
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
		if (ct != null) {
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
			ParameterizedType pt = (ParameterizedType)t;
			sb.append('<');
			boolean first = true;
			for (Type t2 : pt.getActualTypeArguments()) {
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
	 * Returns the simple name of the underlying class.
	 *
	 * <p>
	 * Returns either {@link Class#getSimpleName()} or {@link Type#getTypeName()} depending on whether
	 * this is a class or type.
	 *
	 * @return The simple name of the underlying class;
	 */
	public String getSimpleName() {
		return c != null ? c.getSimpleName() : t.getTypeName();
	}

	/**
	 * Returns the name of the underlying class.
	 *
	 * @return The name of the underlying class.
	 */
	public String getName() {
		return c != null ? c.getName() : t.getTypeName();
	}

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
		StringBuilder sb = new StringBuilder();
		while (c.isArray()) {
			sb.append("Array");
			c = c.getComponentType();
		}
		return c.getSimpleName() + sb;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Hierarchy
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOf(Class<?> child) {
		return c != null && child != null && c.isAssignableFrom(child);
	}

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 *
	 * <p>
	 * Primitive classes are converted to wrapper classes and compared.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
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
	public boolean isParentOf(Type child) {
		if (child instanceof Class)
			return isParentOf((Class<?>)child);
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
	 * Returns <jk>true</jk> if this class is a child of <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a parent of <c>child</c>.
	 */
	public boolean isStrictChildOf(Class<?> parent) {
		return c != null && parent != null && parent.isAssignableFrom(c) && ! c.equals(parent);
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 */
	public boolean isChildOf(Class<?> parent) {
		return c != null && parent != null && parent.isAssignableFrom(c);
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as any of the <c>parents</c>.
	 *
	 * @param parents The parents class.
	 * @return <jk>true</jk> if this class is a child or the same as any of the <c>parents</c>.
	 */
	public boolean isChildOfAny(Class<?>...parents) {
		for (Class<?> p : parents)
			if (isChildOf(p))
				return true;
		return false;
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
	 * Returns <jk>true</jk> if this class is a child or the same as <c>parent</c>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>parent</c>.
	 */
	public boolean isChildOf(ClassInfo parent) {
		return isChildOf(parent.inner());
	}

	/**
	 * Checks for equality with the specified class.
	 *
	 * @param c The class to check equality with.
	 * @return <jk>true</jk> if the specified class is the same as this one.
	 */
	public boolean is(Class<?> c) {
		return this.c != null && this.c.equals(c);
	}

	/**
	 * Performs a predicate check on this class.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if the predicate test passes.
	 */
	public boolean is(Predicate<Class<?>> test) {
		return test.test(c);
	}

	/**
	 * Checks for equality with the specified class.
	 *
	 * @param c The class to check equality with.
	 * @return <jk>true</jk> if the specified class is the same as this one.
	 */
	public boolean is(ClassInfo c) {
		if (this.c != null)
			return this.c.equals(c.inner());
		return t.equals(c.t);
	}

	/**
	 * Returns <jk>true</jk> if the specified value is an instance of this class.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the specified value is an instance of this class.
	 */
	public boolean isInstance(Object value) {
		if (this.c != null)
			return c.isInstance(value);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is any of the specified types.
	 *
	 * @param types The types to check against.
	 * @return <jk>true</jk> if this class is any of the specified types.
	 */
	public boolean isAny(Class<?>...types) {
		for (Class<?> cc : types)
			if (is(cc))
				return true;
		return false;
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
	 * Returns the package of this class.
	 *
	 * @return The package of this class.
	 */
	public Package getPackage() {
		return c == null ? null : c.getPackage();
	}

	/**
	 * Returns <jk>true</jk> if this class is not in the root package.
	 *
	 * @return <jk>true</jk> if this class is not in the root package.
	 */
	public boolean hasPackage() {
		return getPackage() != null;
	}

	/**
	 * Returns the number of dimensions if this is an array type.
	 *
	 * @return The number of dimensions if this is an array type, or <c>0</c> if it is not.
	 */
	public int getDimensions() {
		if (dim == -1) {
			int d = 0;
			Class<?> ct = c;
			while (ct != null && ct.isArray()) {
				d++;
				ct = ct.getComponentType();
			}
			this.dim = d;
			this.componentType = ct == c ? this : of(ct);
		}
		return dim;
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
	 * Returns <jk>true</jk> if this class is an enum.
	 *
	 * @return <jk>true</jk> if this class is an enum.
	 */
	public boolean isEnum() {
		return c != null && c.isEnum();
	}

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
			boolean b = false;
			MethodInfo mi = getPublicMethod(x -> x.hasName("value"));
			if (mi != null) {
				ClassInfo rt = mi.getReturnType();
				if (rt.isArray()) {
					ClassInfo rct = rt.getComponentType();
					if (rct.hasAnnotation(Repeatable.class)) {
						Repeatable r = rct.getLastAnnotation(Repeatable.class) ;
							b = r.value().equals(c);
					}
				}
			}
			isRepeatedAnnotation = b;
		}
		return isRepeatedAnnotation;
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
			if (repeatedAnnotationMethod == null)
				repeatedAnnotationMethod = getPublicMethod(x -> x.hasName("value"));
			return repeatedAnnotationMethod;
		}
		return null;
	}

	/**
	 * Returns <jk>true</jk> if this class is an array.
	 *
	 * @return <jk>true</jk> if this class is an array.
	 */
	public boolean isArray() {
		return c != null && c.isArray();
	}

	/**
	 * Returns <jk>true</jk> if this class is an annotation.
	 *
	 * @return <jk>true</jk> if this class is an annotation.
	 */
	public boolean isAnnotation() {
		return c != null && c.isAnnotation();
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Collection} or an array.
	 *
	 * @return <jk>true</jk> if this class is a {@link Collection} or an array.
	 */
	public boolean isCollectionOrArray() {
		return c != null && (Collection.class.isAssignableFrom(c) || c.isArray());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling {@link Class#newInstance()} on the underlying class.
	 *
	 * @return A new instance of the underlying class
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public Object newInstance() throws ExecutableException {
		if (c == null)
			throw new ExecutableException("Type ''{0}'' cannot be instantiated", getFullName());
		try {
			return c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ExecutableException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter types
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the real parameter type of this class.
	 *
	 * @param index The zero-based index of the parameter to resolve.
	 * @param pt The parameterized type class containing the parameterized type to resolve (e.g. <c>HashMap</c>).
	 * @return The resolved real class.
	 */
	@SuppressWarnings("null")
	public Class<?> getParameterType(int index, Class<?> pt) {
		assertArgNotNull("pt", pt);

		// We need to make up a mapping of type names.
		Map<Type,Type> typeMap = new HashMap<>();
		Class<?> cc = c;
		while (pt != cc.getSuperclass()) {
			extractTypes(typeMap, cc);
			cc = cc.getSuperclass();
			assertArg(cc != null, "Class ''{0}'' is not a subclass of parameterized type ''{1}''", c.getSimpleName(), pt.getSimpleName());
		}

		Type gsc = cc.getGenericSuperclass();

		assertArg(gsc instanceof ParameterizedType, "Class ''{0}'' is not a parameterized type", pt.getSimpleName());

		ParameterizedType cpt = (ParameterizedType)gsc;
		Type[] atArgs = cpt.getActualTypeArguments();
		assertArg(index < atArgs.length, "Invalid type index. index={0}, argsLength={1}", index, atArgs.length);
		Type actualType = cpt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class) {
			return (Class<?>)actualType;

		} else if (actualType instanceof GenericArrayType) {
			Type gct = ((GenericArrayType)actualType).getGenericComponentType();
			if (gct instanceof ParameterizedType)
				return Array.newInstance((Class<?>)((ParameterizedType)gct).getRawType(), 0).getClass();
		} else if (actualType instanceof TypeVariable) {
			TypeVariable<?> typeVariable = (TypeVariable<?>)actualType;
			List<Class<?>> nestedOuterTypes = new LinkedList<>();
			for (Class<?> ec = cc.getEnclosingClass(); ec != null; ec = ec.getEnclosingClass()) {
				Class<?> outerClass = cc.getClass();
				nestedOuterTypes.add(outerClass);
				Map<Type,Type> outerTypeMap = new HashMap<>();
				extractTypes(outerTypeMap, outerClass);
				for (Map.Entry<Type,Type> entry : outerTypeMap.entrySet()) {
					Type key = entry.getKey(), value = entry.getValue();
					if (key instanceof TypeVariable) {
						TypeVariable<?> keyType = (TypeVariable<?>)key;
						if (keyType.getName().equals(typeVariable.getName()) && isInnerClass(keyType.getGenericDeclaration(), typeVariable.getGenericDeclaration())) {
							if (value instanceof Class)
								return (Class<?>)value;
							typeVariable = (TypeVariable<?>)entry.getValue();
						}
					}
				}
			}
		} else if (actualType instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)actualType).getRawType();
		}
		throw illegalArgumentException("Could not resolve variable ''{0}'' to a type.", actualType.getTypeName());
	}

	private static boolean isInnerClass(GenericDeclaration od, GenericDeclaration id) {
		if (od instanceof Class && id instanceof Class) {
			Class<?> oc = (Class<?>)od;
			Class<?> ic = (Class<?>)id;
			while ((ic = ic.getEnclosingClass()) != null)
				if (ic == oc)
					return true;
		}
		return false;
	}

	private static void extractTypes(Map<Type,Type> typeMap, Class<?> c) {
		Type gs = c.getGenericSuperclass();
		if (gs instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)gs;
			Type[] typeParameters = ((Class<?>)pt.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			for (int i = 0; i < typeParameters.length; i++) {
				if (typeMap.containsKey(actualTypeArguments[i]))
					actualTypeArguments[i] = typeMap.get(actualTypeArguments[i]);
				typeMap.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
	}

	private static <T> List<T> addIfNotNull(List<T> l, T o) {
		if (o != null)
			l.add(o);
		return l;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	public String toString() {
		return t.toString();
	}

	@Override
	public int hashCode() {
		return t.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof ClassInfo) && eq(this, (ClassInfo)o, (x,y)->eq(x.t, y.t));
	}
}
