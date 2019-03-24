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
package org.apache.juneau.reflection;

import static org.apache.juneau.internal.ClassFlags.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

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
 * <h5 class='figure'>
 * <p class='bpcode w800'>
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
 */
@BeanIgnore
public final class ClassInfo {

	private final Type t;
	private final Class<?> c;
	private List<Class<?>> interfaces;
	private List<ClassInfo> interfaceInfos;
	private Map<Class<?>,Optional<Annotation>> annotationMap;
	private Map<Class<?>,Optional<Annotation>> declaredAnnotationMap;

	/**
	 * Constructor.
	 *
	 * @param t The class type.
	 */
	protected ClassInfo(Type t) {
		this.t = t;
		this.c = ClassUtils.toClass(t);
	}

	/**
	 * Constructor.
	 *
	 * @param c The class type.
	 */
	protected ClassInfo(Class<?> c) {
		this.t = c;
		this.c = c;
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
		return new ClassInfo(t);
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
		return new ClassInfo(c);
	}

	/**
	 * Same as using the constructor, but operates on an object instance.
	 *
	 * @param o The class instance.
	 * @return The constructed class info.
	 */
	public static ClassInfo of(Object o) {
		if (o == null)
			return null;
		return new ClassInfo(o.getClass());
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
	public Class<?> inner() {
		return c;
	}

	/**
	 * If this class is a parameterized {@link Value} type, returns the parameterized type.
	 *
	 * @return The parameterized type, or this object if this class is not a parameterized {@link Value} type.
	 */
	public ClassInfo resolved() {
		if (Value.isType(t))
			return of(Value.getParameterType(t));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parent classes and interfaces.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the parent class.
	 *
	 * @return The parent class, or <jk>null</jk> if the class has no parent.
	 */
	public Class<?> getParent() {
		return c.getSuperclass();
	}

	/**
	 * Returns the parent class wrapped in {@link ClassInfo}.
	 *
	 * @return The parent class wrapped in {@link ClassInfo}, or <jk>null</jk> if the class has no parent.
	 */
	public ClassInfo getParentInfo() {
		return of(getParent());
	}

	/**
	 * Returns a list of interfaces defined on this class.
	 *
	 * <p>
	 * Does not include interfaces defined on parent classes.
	 *
	 * @return
	 * 	An unmodifiable list of interfaces defined on this class.
	 */
	public List<Class<?>> getInterfaces() {
		if (interfaces == null)
			interfaces = unmodifiableList(c.getInterfaces());
		return interfaces;
	}

	/**
	 * Returns a list of interfaces defined on this class wrapped in {@link ClassInfo}.
	 *
	 * <p>
	 * Does not include interfaces defined on parent classes.
	 *
	 * @return
	 * 	An unmodifiable list of interfaces defined on this class wrapped in {@link ClassInfo}.
	 */
	public List<ClassInfo> getInterfaceInfos() {
		if (interfaceInfos == null) {
			List<Class<?>> interfaces = getInterfaces();
			List<ClassInfo> l = new ArrayList<>(interfaces.size());
			for (Class<?> i : interfaces)
				l.add(of(i));
			interfaceInfos = unmodifiableList(l);
		}
		return interfaceInfos;
	}

	/**
	 * Returns a list including this class and all parent classes in child-to-parent order.
	 *
	 * <p>
	 * Does not include interfaces.
	 *
	 * @return An unmodifiable list including this class and all parent classes in child-to-parent order.
	 */
	public List<ClassInfo> getParentInfos() {
		return getParentInfos(false, false);
	}

	/**
	 * Returns a list including this class and all parent classes and interfaces.
	 *
	 * <h5 class='figure'>Examples</h5>
	 * <p class='bpcode'>
	 * 	<jc>// Class hierarchy</jc>
	 * 	<jk>interface</jk> I1 {}
	 * 	<jk>interface</jk> I2 <jk>extends</jk> I11 {}
	 * 	<jk>interface</jk> I3 {}
	 * 	<jk>interface</jk> I4 {}
	 * 	<jk>class</jk> CA <jk>implements</jk> I1, I2 {}
	 * 	<jk>class</jk> CB <jk>extends</jk> CA <jk>implements</jk> I3 {}
	 * 	<jk>class</jk> CC <jk>extends</jk> CB {}
	 *
	 * 	<jc>// Examples</jc>
	 * 	ClassInfo cc = ClassInfo.<jsm>of</jsm>(CC.<jk>class</jk>);
	 *
	 * 	<jc>// Parent last, no interfaces.
	 * 	cc.getParentInfos();             // CC,CB,CA
	 * 	cc.getParentInfos(false,false);  // CC,CB,CA
	 *
	 * 	<jc>// Parent first, no interfaces.
	 * 	cc.getParentInfos(true,false);   // CA,CB,CC
	 *
	 * 	<jc>// Parent last, include interfaces.
	 * 	cc.getParentInfos(false,true);   // CC,CB,I3,CA,I1,I2
	 *
	 * 	<jc>// Parent first, include interfaces.
	 * 	cc.getParentInfos(true,true);    // I2,I1,CA,I3,CB,CC
	 * </p>
	 *
	 * @param parentFirst
	 * 	If <jk>true</jk>, results are ordered parent-first.
	 * @param includeInterfaces
	 * 	If <jk>true</jk>, results include interfaces.
	 * @return An unmodifiable list including this class and all parent classes and interfaces.
	 */
	public List<ClassInfo> getParentInfos(boolean parentFirst, boolean includeInterfaces) {
		return unmodifiableList(findParents(new ArrayList<>(), c, parentFirst, includeInterfaces));
	}

	private static List<ClassInfo> findParents(List<ClassInfo> l, Class<?> c, boolean parentFirst, boolean includeInterfaces) {
		if (parentFirst) {
			if (hasSuperclass(c))
				findParents(l, c.getSuperclass(), parentFirst, includeInterfaces);
			if (includeInterfaces)
				for (Class<?> i : iterable(c.getInterfaces(), true))
					l.add(of(i));
			l.add(of(c));
		} else {
			l.add(of(c));
			if (includeInterfaces)
				for (Class<?> i : c.getInterfaces())
					l.add(of(i));
			if (hasSuperclass(c))
				findParents(l, c.getSuperclass(), parentFirst, includeInterfaces);
		}
		return l;
	}

	private static boolean hasSuperclass(Class<?> c) {
		if (c.getSuperclass() == Object.class)
			return false;
		if (c.getSuperclass() == null)
			return false;
		return true;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all declared methods on this class and all parent classes in child-to-parent order.
	 *
	 * <p>
	 * Methods are ordered per the natural ordering of the underlying JVM.
	 * <br>Some JVMs (IBM) preserve the declaration order of methods.  Other JVMs (Oracle) do not and return them in random order.
	 *
	 * @return All declared methods on this class and all parent classes in child-to-parent order.
	 */
	public Iterable<MethodInfo> getAllMethodInfos() {
		return getAllMethodInfos(false, false);
	}

	/**
	 * Returns all declared methods on this class and all parent classes in child-to-parent order.
	 *
	 * @param parentFirst If <jk>true</jk>, methods on parent classes are listed first.
	 * @param sort
	 * 	If <jk>true</jk>, methods are sorted alphabetically per class.
	 * 	Otherwise, uses the order of the methods in the underlying JVM.
	 * @return All declared methods on this class and all parent classes.
	 */
	public Iterable<MethodInfo> getAllMethodInfos(boolean parentFirst, boolean sort) {
		return findAllMethods(parentFirst, sort);
	}

	/**
	 * Returns all methods declared on this class.
	 *
	 * <p>
	 * Methods are ordered per the natural ordering of the underlying JVM.
	 * <br>Some JVMs (IBM) preserve the declaration order of methods.  Other JVMs (Oracle) do not and return them in random order.
	 *
	 * @return All methods declared on this class.
	 */
	public Iterable<MethodInfo> getDeclaredMethodInfos() {
		return getDeclaredMethodInfos(false);
	}

	/**
	 * Returns all methods declared on this class.
	 *
	 * @param sort
	 * 	If <jk>true</jk>, methods are sorted alphabetically.
	 * 	Otherwise, uses the order of the methods in the underlying JVM.
	 * @return All methods declared on this class.
	 */
	public Iterable<MethodInfo> getDeclaredMethodInfos(boolean sort) {
		return findDeclaredMethods(null, sort);
	}

	/**
	 * Returns all public methods on this class.
	 *
	 * <p>
	 * Returns the methods (in the same order) as the call to {@link Class#getMethods()}.
	 *
	 * @return All public methods on this class.
	 */
	public Iterable<MethodInfo> getPublicMethodInfos() {
		return getPublicMethodInfos(false);
	}

	/**
	 * Returns all public methods on this class.
	 *
	 * @param sort
	 * 	If <jk>true</jk>, methods are sorted alphabetically.
	 * 	Otherwise, uses the order of the methods in the underlying JVM.
	 * @return All public methods on this class.
	 */
	public Iterable<MethodInfo> getPublicMethodInfos(boolean sort) {
		return findPublicMethods(sort);
	}

	private List<MethodInfo> findAllMethods(boolean parentFirst, boolean sort) {
		List<MethodInfo> l = new ArrayList<>();
		for (ClassInfo c : getParentInfos(parentFirst, true))
			c.findDeclaredMethods(l, sort);
		return l;
	}

	private List<MethodInfo> findDeclaredMethods(List<MethodInfo> l, boolean sort) {
		Method[] mm = c.getDeclaredMethods();
		if (sort)
			mm = sort(mm);
		if (l == null)
			l = new ArrayList<>(mm.length);
		for (Method m : mm)
			l.add(MethodInfo.of(this, m));
		return l;
	}

	private List<MethodInfo> findPublicMethods(boolean sorted) {
		Method[] mm = c.getMethods();
		List<MethodInfo> l = new ArrayList<>(mm.length);
		if (sorted)
			mm = sort(mm);
		for (Method m : mm)
			l.add(MethodInfo.of(this, m));
		return l;
	}

	private static Comparator<Method> METHOD_COMPARATOR = new Comparator<Method>() {
		@Override
		public int compare(Method o1, Method o2) {
			int i = o1.getName().compareTo(o2.getName());
			if (i == 0) {
				i = o1.getParameterTypes().length - o2.getParameterTypes().length;
				if (i == 0) {
					for (int j = 0; j < o1.getParameterTypes().length && i == 0; j++) {
						i = o1.getParameterTypes()[j].getName().compareTo(o2.getParameterTypes()[j].getName());
					}
				}
			}
			return i;
		}
	};

	private static Method[] sort(Method[] m) {
		Arrays.sort(m, METHOD_COMPARATOR);
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Special methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the public static "fromString" method on this class.
	 *
	 * <p>
	 * Looks for the following method names:
	 * <ul>
	 * 	<li><code>fromString</code>
	 * 	<li><code>fromValue</code>
	 * 	<li><code>valueOf</code>
	 * 	<li><code>parse</code>
	 * 	<li><code>parseString</code>
	 * 	<li><code>forName</code>
	 * 	<li><code>forString</code>
	 * </ul>
	 *
	 * @return The static method, or <jk>null</jk> if it couldn't be found.
	 */
	public MethodInfo getFromStringMethodInfo() {
		for (String methodName : new String[]{"create","fromString","fromValue","valueOf","parse","parseString","forName","forString"})
			for (MethodInfo m : getPublicMethodInfos())
				if (m.isAll(STATIC, PUBLIC, NOT_DEPRECATED) && m.hasName(methodName) && m.hasReturnType(c) && m.hasArgs(String.class))
					return m;
		return null;
	}

	/**
	 * Find the public static creator method on this class.
	 *
	 * @param ic The argument type.
	 * @param name The method name.
	 * @return The static method, or <jk>null</jk> if it couldn't be found.
	 */
	public MethodInfo getStaticCreateMethodInfo(Class<?> ic, String name) {
		for (MethodInfo m : getPublicMethodInfos())
			if (m.isAll(STATIC, PUBLIC, NOT_DEPRECATED) && m.hasName(name) && m.hasReturnType(c) && m.hasArgs(ic))
				return m;
		return null;
	}

	/**
	 * Returns the <code>public static Builder create()</code> method on this class.
	 *
	 * @return The <code>public static Builder create()</code> method on this class, or <jk>null</jk> if it doesn't exist.
	 */
	public MethodInfo getBuilderCreateMethodInfo() {
		for (MethodInfo m : getDeclaredMethodInfos())
			if (m.isAll(PUBLIC, STATIC) && m.hasName("create") && ! m.hasReturnType(Void.class))
				return m;
		return null;
	}

	/**
	 * Returns the <code>T build()</code> method on this class.
	 *
	 * @return The <code>T build()</code> method on this class, or <jk>null</jk> if it doesn't exist.
	 */
	public MethodInfo getBuilderBuildMethodInfo() {
		for (MethodInfo m : getDeclaredMethodInfos())
			if (m.isAll(NOT_STATIC) && m.hasName("build") && ! m.hasReturnType(Void.class))
				return m;
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all the public constructors defined on this class.
	 *
	 * @return All public constructors defined on this class.
	 */
	public Iterable<ConstructorInfo> getConstructorInfos() {
		return findConstructors();
	}

	private List<ConstructorInfo> findConstructors() {
		Constructor<?>[] cc = c.getConstructors();
		List<ConstructorInfo> l = new ArrayList<>(cc.length);
		for (Constructor<?> ccc : cc)
			l.add(ConstructorInfo.of(this, ccc));
		return Collections.unmodifiableList(l);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fields
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all field on this class and all parent classes in child-to-parent order.
	 *
	 * <p>
	 * Fields are ordered per the natural ordering of the underlying JVM.
	 * <br>Some JVMs (IBM) preserve the declaration order of fields.  Other JVMs (Oracle) do not and return them in random order.
	 *
	 * @return All declared methods on this class and all parent classes in child-to-parent order.
	 */
	public Iterable<FieldInfo> getAllFieldInfos() {
		return getAllFieldInfos(false, false);
	}

	/**
	 * Returns all field on this class and all parent classes.
	 *
	 * @param parentFirst If <jk>true</jk>, fields are listed in parent-to-child order.
	 * @param sort
	 * 	If <jk>true</jk>, fields are sorted alphabetically.
	 * 	Otherwise, uses the order of the fields in the underlying JVM.
	 * @return All declared methods on this class and all parent classes.
	 */
	public Iterable<FieldInfo> getAllFieldInfos(boolean parentFirst, boolean sort) {
		return findAllFields(null, parentFirst, sort);
	}

	/**
	 * Returns all fields declared on this class.
	 *
	 * <p>
	 * Fields are ordered per the natural ordering of the underlying JVM.
	 * <br>Some JVMs (IBM) preserve the declaration order of fields.  Other JVMs (Oracle) do not and return them in random order.
	 *
	 * @return All fields declared on this class.
	 */
	public Iterable<FieldInfo> getDeclaredFieldInfos() {
		return getDeclaredFieldInfos(false);
	}

	/**
	 * Returns all fields declared on this class.
	 *
	 * @param sort
	 * 	If <jk>true</jk>, fields are sorted alphabetically.
	 * 	Otherwise, uses the order of the fields in the underlying JVM.
	 * @return All fields declared on this class.
	 */
	public Iterable<FieldInfo> getDeclaredFieldInfos(boolean sort) {
		return findDeclaredFields(null, sort);
	}

	private List<FieldInfo> findAllFields(List<FieldInfo> l, boolean parentFirst, boolean sort) {
		if (l == null)
			l = new ArrayList<>();
		for (ClassInfo c : getParentInfos(parentFirst, false))
			c.findDeclaredFields(l, sort);
		return l;
	}

	private List<FieldInfo> findDeclaredFields(List<FieldInfo> l, boolean sort) {
		Field[] ff = c.getDeclaredFields();
		if (sort)
			ff = sort(ff);
		if (l == null)
			l = new ArrayList<>(ff.length);
		for (Field f : ff)
			l.add(FieldInfo.of(this, f));
		return l;
	}

	private static Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
		@Override
		public int compare(Field o1, Field o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	private static Field[] sort(Field[] m) {
		Arrays.sort(m, FIELD_COMPARATOR);
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
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
	public ConstructorInfo getNoArgConstructorInfo(Visibility v) {
		if (isAbstract())
			return null;
		boolean isMemberClass = isMemberClass() && ! isStatic();
		for (ConstructorInfo cc : getConstructorInfos())
			if (cc.hasNumArgs(isMemberClass ? 1 : 0) && cc.isVisible(v) && cc.isNotDeprecated())
				return cc.transform(v);
		return null;
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
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getAnnotation(Class<T> a) {
		Optional<Annotation> o = annotationMap().get(a);
		if (o == null) {
			o = Optional.ofNullable(findAnnotation(a));
			annotationMap().put(a, o);
		}
		return o.isPresent() ? (T)o.get() : null;
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
		return getAnnotation(a) != null;
	}

	/**
	 * Returns all annotations of the specified type defined on the specified class or parent classes/interfaces.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found in child-to-parent order, or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a) {
		return getAnnotations(a, false);
	}

	/**
	 * Identical to {@link #getAnnotations(Class)} but optionally returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @param parentFirst If <jk>true</jk>, results are in parent-to-child order.
	 * @return
	 * 	A list of all matching annotations found or an empty list if none found.
	 */
	public <T extends Annotation> List<T> getAnnotations(Class<T> a, boolean parentFirst) {
		return appendAnnotations(new ArrayList<>(), a, parentFirst);
	}

	/**
	 * Returns the specified annotation only if it's been declared on the specified class.
	 *
	 * <p>
	 * More efficient than calling {@link Class#getAnnotation(Class)} since it doesn't recursively look for the class
	 * up the parent chain.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> T getDeclaredAnnotation(Class<T> a) {
		Optional<Annotation> o = declaredAnnotationMap().get(a);
		if (o == null) {
			o = Optional.ofNullable(findDeclaredAnnotation(a));
			declaredAnnotationMap().put(a, o);
		}
		return o.isPresent() ? (T)o.get() : null;
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * <p>
	 * Results are ordered in child-to-parent order.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a) {
		return appendAnnotations(l, a, false);
	}

	/**
	 * Finds and appends the specified annotation on the specified class and superclasses/interfaces to the specified
	 * list.
	 *
	 * @param l The list of annotations.
	 * @param a The annotation.
	 * @param parentFirst If <jk>true</jk>, results are ordered in parent-to-child order.
	 * @return The same list.
	 */
	public <T extends Annotation> List<T> appendAnnotations(List<T> l, Class<T> a, boolean parentFirst) {
		if (c != null) {
			if (parentFirst) {
				for (Class<?> c2 : c.getInterfaces())
					of(c2).appendAnnotations(l, a, true);

				ClassInfo sci = of(c.getSuperclass());
				if (sci != null)
					sci.appendAnnotations(l, a, true);

				if (c.getPackage() != null)
					addIfNotNull(l, c.getPackage().getAnnotation(a));

				addIfNotNull(l, getDeclaredAnnotation(a));
			} else {
				addIfNotNull(l, getDeclaredAnnotation(a));

				if (c.getPackage() != null)
					addIfNotNull(l, c.getPackage().getAnnotation(a));

				ClassInfo sci = of(c.getSuperclass());
				if (sci != null)
					sci.appendAnnotations(l, a, false);

				for (Class<?> c2 : c.getInterfaces())
					of(c2).appendAnnotations(l, a, false);
			}
		}
		return l;
	}

	/**
	 * Same as getAnnotations(Class) except returns the annotations with the accompanying class.
	 *
	 * <p>
	 * Results are ordered child-to-parent.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @return The found matches, or an empty list if annotation was not found.
	 */
	public <T extends Annotation> List<ClassAnnotation<T>> getClassAnnotations(Class<T> a) {
		return getClassAnnotations(a, false);
	}

	/**
	 * Same as {@link #getClassAnnotations(Class)} except optionally returns results in parent-to-child order.
	 *
	 * @param <T> The annotation class type.
	 * @param a The annotation class type.
	 * @param parentFirst If <jk>true</jk>, annotations are returned in parent-to-child order.
	 * @return The found matches, or an empty list if annotation was not found.
	 */
	public <T extends Annotation> List<ClassAnnotation<T>> getClassAnnotations(Class<T> a, boolean parentFirst) {
		return findClassAnnotations(new ArrayList<>(), a, parentFirst);
	}

	private <T extends Annotation> List<ClassAnnotation<T>> findClassAnnotations(List<ClassAnnotation<T>> l, Class<T> a, boolean parentFirst) {
		if (c != null) {
			if (parentFirst) {
				for (Class<?> c2 : c.getInterfaces())
					of(c2).findClassAnnotations(l, a, true);

				ClassInfo sci = of(c.getSuperclass());
				if (sci != null)
					sci.findClassAnnotations(l, a, true);

				T t2 = getDeclaredAnnotation(a);
				if (t2 != null)
					l.add(ClassAnnotation.of(this, t2));

			} else {
				T t2 = getDeclaredAnnotation(a);
				if (t2 != null)
					l.add(ClassAnnotation.of(this, t2));

				ClassInfo sci = of(c.getSuperclass());
				if (sci != null)
					sci.findClassAnnotations(l, a, false);

				for (Class<?> c2 : c.getInterfaces())
					of(c2).findClassAnnotations(l, a, false);
			}
		}
		return l;
	}

	private <T extends Annotation> T findAnnotation(Class<T> a) {
		if (c != null) {
			T t2 = getDeclaredAnnotation(a);
			if (t2 != null)
				return t2;

			ClassInfo sci = getParentInfo();
			if (sci != null) {
				t2 = sci.getAnnotation(a);
				if (t2 != null)
					return t2;
			}

			for (Class<?> c2 : getInterfaces()) {
				t2 = c2.getAnnotation(a);
				if (t2 != null)
					return t2;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T extends Annotation> T findDeclaredAnnotation(Class<T> a) {
		for (Annotation a2 : c.getDeclaredAnnotations())
			if (a2.annotationType() == a)
				return (T)a2;
		return null;
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null)
			annotationMap = new ConcurrentHashMap<>();
		return annotationMap;
	}

	private synchronized Map<Class<?>,Optional<Annotation>> declaredAnnotationMap() {
		if (declaredAnnotationMap == null)
			declaredAnnotationMap = new ConcurrentHashMap<>();
		return declaredAnnotationMap;
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
	public boolean isAll(ClassFlags...flags) {
		for (ClassFlags f : flags) {
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
				case ABSTRACT:
					if (isNotAbstract())
						return false;
					break;
				case NOT_ABSTRACT:
					if (isAbstract())
						return false;
					break;
				case HAS_ARGS:
				case HAS_NO_ARGS:
				case TRANSIENT:
				case NOT_TRANSIENT:
				default:
					break;

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
	public boolean isAny(ClassFlags...flags) {
		for (ClassFlags f : flags) {
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
				case ABSTRACT:
					if (isAbstract())
						return true;
					break;
				case NOT_ABSTRACT:
					if (isNotAbstract())
						return true;
					break;
				case TRANSIENT:
				case NOT_TRANSIENT:
				case HAS_ARGS:
				case HAS_NO_ARGS:
				default:
					break;

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
		return c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this class doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this class doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() {
		return ! c.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is public.
	 *
	 * @return <jk>true</jk> if this class is public.
	 */
	public boolean isPublic() {
		return Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is not public.
	 *
	 * @return <jk>true</jk> if this class is not public.
	 */
	public boolean isNotPublic() {
		return ! Modifier.isPublic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is public.
	 *
	 * @return <jk>true</jk> if this class is public.
	 */
	public boolean isStatic() {
		return Modifier.isStatic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is not static.
	 *
	 * @return <jk>true</jk> if this class is not static.
	 */
	public boolean isNotStatic() {
		return ! Modifier.isStatic(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 *
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() {
		return Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is not abstract.
	 *
	 * @return <jk>true</jk> if this class is not abstract.
	 */
	public boolean isNotAbstract() {
		return ! Modifier.isAbstract(c.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this class is a member class.
	 *
	 * @return <jk>true</jk> if this class is a member class.
	 */
	public boolean isMemberClass() {
		return c.isMemberClass();
	}

	/**
	 * Identifies if the specified visibility matches this constructor.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this constructor.
	 */
	public boolean isVisible(Visibility v) {
		return v.isVisible(c);
	}

	/**
	 * Returns <jk>true</jk> if this is a primitive class.
	 *
	 * @return <jk>true</jk> if this is a primitive class.
	 */
	public boolean isPrimitive() {
		return c.isPrimitive();
	}

	/**
	 * Returns <jk>true</jk> if this is not a primitive class.
	 *
	 * @return <jk>true</jk> if this is not a primitive class.
	 */
	public boolean isNotPrimitive() {
		return ! c.isPrimitive();
	}

	/**
	 * Returns the underlying class name.
	 *
	 * @return The underlying class name.
	 */
	public String getName() {
		return c.getName();
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
	 * Returns the short name of the underlying class.
	 *
	 * <p>
	 * Similar to {@link #getSimpleName()} but also renders local or member class name prefixes.
	 *
	 * @return The short name of the underlying class.
	 */
	public String getShortName() {
		if (t instanceof Class) {
			if (c.isLocalClass())
				return of(c.getEnclosingClass()).getShortName() + '.' + c.getSimpleName();
			if (c.isMemberClass())
				return of(c.getDeclaringClass()).getShortName() + '.' + c.getSimpleName();
			return c.getSimpleName();
		}
		if (t instanceof ParameterizedType) {
			StringBuilder sb = new StringBuilder();
			ParameterizedType pt = (ParameterizedType)t;
			sb.append(of(pt.getRawType()).getShortName());
			sb.append("<");
			boolean first = true;
			for (Type t2 : pt.getActualTypeArguments()) {
				if (! first)
					sb.append(',');
				first = false;
				sb.append(of(t2).getShortName());
			}
			sb.append(">");
			return sb.toString();
		}
		return null;

	}

	/**
	 * Shortcut for calling <code><jsm>getReadableClassName</jsm>(c.getName())</code>
	 *
	 * @return A readable class type name, or <jk>null</jk> if parameter is <jk>null</jk>.
	 */
	public String getReadableName() {
		return ClassUtils.getReadableClassName(c != null ? c.getName() : t.getTypeName());
	}

	/**
	 * Finds the public constructor that can take in the specified arguments.
	 *
	 * @param args The argument types we want to pass into the constructor.
	 * @return
	 * 	The constructor, or <jk>null</jk> if a public constructor could not be found that takes in the specified
	 * 	arguments.
	 */
	public <T> Constructor<T> getPublicConstructor(Class<?>...args) {
		return getPublicConstructor(false, args);
	}

	/**
	 * Finds a public constructor with the specified parameters without throwing an exception.
	 *
	 * @param fuzzyArgs
	 * 	Use fuzzy-arg matching.
	 * 	Find a constructor that best matches the specified args.
	 * @param argTypes
	 * 	The argument types in the constructor.
	 * 	Can be subtypes of the actual constructor argument types.
	 * @return The matching constructor, or <jk>null</jk> if constructor could not be found.
	 */
	public <T> Constructor<T> getPublicConstructor(boolean fuzzyArgs, Class<?>...argTypes) {
		return getConstructor(Visibility.PUBLIC, fuzzyArgs, argTypes);
	}

	/**
	 * Finds a constructor with the specified parameters without throwing an exception.
	 *
	 * @param vis The minimum visibility.
	 * @param fuzzyArgs
	 * 	Use fuzzy-arg matching.
	 * 	Find a constructor that best matches the specified args.
	 * @param argTypes
	 * 	The argument types in the constructor.
	 * 	Can be subtypes of the actual constructor argument types.
	 * @return The matching constructor, or <jk>null</jk> if constructor could not be found.
	 */
	@SuppressWarnings("unchecked")
	public <T> Constructor<T> getConstructor(Visibility vis, boolean fuzzyArgs, Class<?>...argTypes) {
		ConstructorCacheEntry cce = CONSTRUCTOR_CACHE.get(c);
		if (cce != null && ClassUtils.argsMatch(cce.paramTypes, argTypes) && cce.isVisible(vis))
			return (Constructor<T>)cce.constructor;

		if (fuzzyArgs) {
			int bestCount = -1;
			Constructor<?> bestMatch = null;
			for (Constructor<?> n : c.getDeclaredConstructors()) {
				if (vis.isVisible(n)) {
					int m = ClassUtils.fuzzyArgsMatch(n.getParameterTypes(), argTypes);
					if (m > bestCount) {
						bestCount = m;
						bestMatch = n;
					}
				}
			}
			if (bestCount >= 0)
				CONSTRUCTOR_CACHE.put(c, new ConstructorCacheEntry(c, bestMatch));
			return (Constructor<T>)bestMatch;
		}

		final boolean isMemberClass = isMemberClass() && ! isStatic();
		for (Constructor<?> n : c.getConstructors()) {
			Class<?>[] paramTypes = n.getParameterTypes();
			if (isMemberClass)
				paramTypes = Arrays.copyOfRange(paramTypes, 1, paramTypes.length);
			if (ClassUtils.argsMatch(paramTypes, argTypes) && vis.isVisible(n)) {
				CONSTRUCTOR_CACHE.put(c, new ConstructorCacheEntry(c, n));
				return (Constructor<T>)n;
			}
		}

		return null;
	}

	private static final Map<Class<?>,ConstructorCacheEntry> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();



	private static final class ConstructorCacheEntry {
		final Constructor<?> constructor;
		final Class<?>[] paramTypes;

		ConstructorCacheEntry(Class<?> forClass, Constructor<?> constructor) {
			this.constructor = constructor;
			this.paramTypes = constructor.getParameterTypes();
		}

		boolean isVisible(Visibility vis) {
			return vis.isVisible(constructor);
		}
	}

	/**
	 * Finds the public constructor that can take in the specified arguments.
	 *
	 * @param fuzzyArgs
	 * 	Use fuzzy-arg matching.
	 * 	Find a constructor that best matches the specified args.
	 * @param args The arguments we want to pass into the constructor.
	 * @return
	 * 	The constructor, or <jk>null</jk> if a public constructor could not be found that takes in the specified
	 * 	arguments.
	 */
	public <T> Constructor<T> getPublicConstructor(boolean fuzzyArgs, Object...args) {
		return getPublicConstructor(fuzzyArgs, ClassUtils.getClasses(args));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive wrappers.
	//-----------------------------------------------------------------------------------------------------------------


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
		if (! c.isPrimitive())
			return c;
		return pmap1.get(c);
	}

	/**
	 * Same as {@link #getWrapperIfPrimitive()} but wraps it in a {@link ClassInfo}.
	 *
	 * @return The wrapper class if it's primitive, or the same class if class is not a primitive.
	 */
	public ClassInfo getWrapperInfoIfPrimitive() {
		if (! c.isPrimitive())
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

	private static final Map<Class<?>,Object> primitiveDefaultMap = Collections.unmodifiableMap(
		new AMap<Class<?>,Object>()
			.append(Boolean.TYPE, false)
			.append(Character.TYPE, (char)0)
			.append(Short.TYPE, (short)0)
			.append(Integer.TYPE, 0)
			.append(Long.TYPE, 0l)
			.append(Float.TYPE, 0f)
			.append(Double.TYPE, 0d)
			.append(Byte.TYPE, (byte)0)
			.append(Boolean.class, false)
			.append(Character.class, (char)0)
			.append(Short.class, (short)0)
			.append(Integer.class, 0)
			.append(Long.class, 0l)
			.append(Float.class, 0f)
			.append(Double.class, 0d)
			.append(Byte.class, (byte)0)
	);

	/**
	 * Returns <jk>true</jk> if this class is a parent of <code>child</code>.
	 *
	 * @param child The child class.
	 * @param strict If <jk>true</jk> returns <jk>false</jk> if the classes are the same.
	 * @return <jk>true</jk> if this class is a parent of <code>child</code>.
	 */
	public boolean isParentOf(Class<?> child, boolean strict) {
		return c.isAssignableFrom(child) && ((!strict) || ! c.equals(child));
	}

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <code>child</code>.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <code>child</code>.
	 */
	public boolean isParentOf(Class<?> child) {
		return isParentOf(child, false);
	}

	/**
	 * Returns <jk>true</jk> if this class is a parent or the same as <code>child</code>.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <code>child</code>.
	 */
	public boolean isParentOf(Type child) {
		if (child instanceof Class)
			return isParentOf((Class<?>)child);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is a child of <code>parent</code>.
	 *
	 * @param parent The parent class.
	 * @param strict If <jk>true</jk> returns <jk>false</jk> if the classes are the same.
	 * @return <jk>true</jk> if this class is a parent of <code>child</code>.
	 */
	public boolean isChildOf(Class<?> parent, boolean strict) {
		return parent.isAssignableFrom(c) && ((!strict) || ! c.equals(parent));
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as <code>parent</code>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a child or the same as <code>parent</code>.
	 */
	public boolean isChildOf(Class<?> parent) {
		return isChildOf(parent, false);
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as any of the <code>parents</code>.
	 *
	 * @param parents The parents class.
	 * @return <jk>true</jk> if this class is a child or the same as any of the <code>parents</code>.
	 */
	public boolean isChildOfAny(Class<?>...parents) {
		for (Class<?> p : parents)
			if (isChildOf(p))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class is a child or the same as <code>parent</code>.
	 *
	 * @param parent The parent class.
	 * @return <jk>true</jk> if this class is a parent or the same as <code>parent</code>.
	 */
	public boolean isChildOf(Type parent) {
		if (parent instanceof Class)
			return isChildOf((Class<?>)parent);
		return false;
	}

	/**
	 * Checks for equality with the specified class.
	 *
	 * @param c The class to check equality with.
	 * @return <jk>true</jk> if the specified class is the same as this one.
	 */
	public boolean is(Class<?> c) {
		return this.c.equals(c);
	}

	/**
	 * Shortcut for calling {@link Class#newInstance()} on the underlying class.
	 *
	 * @return A new instance of the underlying class
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public Object newInstance() throws InstantiationException, IllegalAccessException {
		return c.newInstance();
	}

	/**
	 * Returns <jk>true</jk> if this class is an interface.
	 *
	 * @return <jk>true</jk> if this class is an interface.
	 */
	public boolean isInterface() {
		return c.isInterface();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter types.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the real parameter type of the specified class.
	 *
	 * @param index The zero-based index of the parameter to resolve.
	 * @param oc The class we're trying to resolve the parameter type for.
	 * @return The resolved real class.
	 */
	public Class<?> getParameterType(int index, Class<?> oc) {

		// We need to make up a mapping of type names.
		Map<Type,Type> typeMap = new HashMap<>();
		while (c != oc.getSuperclass()) {
			extractTypes(typeMap, oc);
			oc = oc.getSuperclass();
		}

		Type gsc = oc.getGenericSuperclass();

		// Not actually a parameterized type.
		if (! (gsc instanceof ParameterizedType))
			return Object.class;

		ParameterizedType opt = (ParameterizedType)gsc;
		Type actualType = opt.getActualTypeArguments()[index];

		if (typeMap.containsKey(actualType))
			actualType = typeMap.get(actualType);

		if (actualType instanceof Class) {
			return (Class<?>)actualType;

		} else if (actualType instanceof GenericArrayType) {
			Class<?> cmpntType = (Class<?>)((GenericArrayType)actualType).getGenericComponentType();
			return Array.newInstance(cmpntType, 0).getClass();

		} else if (actualType instanceof TypeVariable) {
			TypeVariable<?> typeVariable = (TypeVariable<?>)actualType;
			List<Class<?>> nestedOuterTypes = new LinkedList<>();
			for (Class<?> ec = oc.getEnclosingClass(); ec != null; ec = ec.getEnclosingClass()) {
				try {
					Class<?> outerClass = oc.getClass();
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
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			throw new FormattedRuntimeException("Could not resolve type: {0}", actualType);
		} else {
			throw new FormattedRuntimeException("Invalid type found in resolveParameterType: {0}", actualType);
		}
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

	@Override
	public String toString() {
		return t.toString();
	}

}
