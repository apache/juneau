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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Utility class for introspecting information about a class.
 */
@BeanIgnore
public final class ClassInfo {

	private final Type type;
	private final Class<?> c;
	private Map<Class<?>,Optional<Annotation>> annotationMap;
	private Map<Class<?>,List<?>> annotationsMap;
	private Map<Class<?>,List<?>> annotationsPfMap;
	private Optional<ClassInfo> parent;
	private ClassInfo[] interfaces;
	private List<FieldInfo> allFields, allFieldsPf, declaredFields;
	private List<MethodInfo> allMethods, allMethodsPf, declaredMethods, publicMethods;
	private List<ClassInfo> parentClasses, parentClassesAndInterfaces;

	private static final Map<Type,ClassInfo> CACHE = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param t The class type.
	 */
	public ClassInfo(Type t) {
		this.type = t;
		this.c = ClassUtils.toClass(t);
	}

	/**
	 * Same as using the constructor, but returns <jk>null</jk> if the type is <jk>null</jk>.
	 *
	 * @param t The class type.
	 * @return The constructed class info.
	 */
	public static ClassInfo create(Type t) {
		if (t == null)
			return null;
		return new ClassInfo(t);
	}

	/**
	 * Returns the cached instance of the specified type.
	 *
	 * @param t The class type.
	 * @return The cached class info, or <jk>null</jk> if the type is <jk>null</jk>.
	 */
	public synchronized static ClassInfo lookup(Type t) {
		if (t == null)
			return null;
		ClassInfo ci = CACHE.get(t);
		if (ci == null) {
			ci = create(t);
			CACHE.put(t, ci);
		}
		return ci;
	}

	/**
	 * Returns the wrapped class.
	 *
	 * @return The wrapped class.
	 */
	public Type getInner() {
		return type;
	}

	/**
	 * Returns the wrapped class.
	 *
	 * @return The wrapped class or <jk>null</jk> if it's not a class.
	 */
	public Class<?> getInnerClass() {
		return c;
	}

	/**
	 * Returns the parent class info.
	 *
	 * @return The parent class info, or <jk>null</jk> if the class has no parent.
	 */
	public synchronized ClassInfo getParent() {
		if (parent == null)
			parent = Optional.ofNullable(c == null ? null : create(c.getSuperclass()));
		return parent.isPresent() ? parent.get() : null;
	}

	/**
	 * Returns the interfaces info.
	 *
	 * @return The implemented interfaces info, or an empty array if the class has no interfaces.
	 */
	public synchronized ClassInfo[] getInterfaces() {
		if (interfaces == null) {
			interfaces = new ClassInfo[c == null ? 0 : c.getInterfaces().length];
			for (int i = 0; i < interfaces.length; i++)
				interfaces[i] = ClassInfo.create(c.getInterfaces()[i]);
		}
		return interfaces;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Classes
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an iterable over this class and all parent classes in child-to-parent order.
	 *
	 * <p>
	 * Does not include interfaces.
	 *
	 * @return An iterable over this class and all parent classes in child-to-parent order.
	 */
	public Iterable<ClassInfo> getParentClasses() {
		if (parentClasses == null)
			parentClasses = Collections.unmodifiableList(findParentClasses(new ArrayList<>(), c));
		return parentClasses;
	}

	/**
	 * Returns an iterable over this class and all parent classes in parent-to-child order.
	 *
	 * <p>
	 * Does not include interfaces.
	 *
	 * @return An iterable over this class and all parent classes in parent-to-child order.
	 */
	public Iterable<ClassInfo> getParentClassesParentFirst() {
		if (parentClasses == null)
			parentClasses = Collections.unmodifiableList(findParentClasses(new ArrayList<>(), c));
		return ReverseIterable.of(parentClasses);
	}

	/**
	 * Returns an iterable over this class and all parent classes and interfaces in child-to-parent order.
	 *
	 * @return An iterable over this class and all parent classes and interfaces in child-to-parent order.
	 */
	public Iterable<ClassInfo> getParentClassesAndInterfaces() {
		if (parentClassesAndInterfaces == null)
			parentClassesAndInterfaces = Collections.unmodifiableList(findParentClassesAndInterfaces(new ArrayList<>(), c));
		return parentClassesAndInterfaces;
	}

	/**
	 * Returns an iterable over this class and all parent classes and interfaces in parent-to-child order.
	 *
	 * @return An iterable over this class and all parent classes and interfaces in parent-to-child order.
	 */
	public Iterable<ClassInfo> getParentClassesAndInterfacesParentFirst() {
		if (parentClassesAndInterfaces == null)
			parentClassesAndInterfaces = Collections.unmodifiableList(findParentClassesAndInterfaces(new ArrayList<>(), c));
		return ReverseIterable.of(parentClassesAndInterfaces);
	}

	private static List<ClassInfo> findParentClasses(List<ClassInfo> l, Class<?> c) {
		l.add(ClassInfo.lookup(c));
		if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
			findParentClasses(l, c.getSuperclass());
		return l;
	}

	private static List<ClassInfo> findParentClassesAndInterfaces(List<ClassInfo> l, Class<?> c) {
		l.add(ClassInfo.lookup(c));
		if (c.getSuperclass() != Object.class && c.getSuperclass() != null)
			findParentClassesAndInterfaces(l, c.getSuperclass());
		for (Class<?> i : c.getInterfaces())
			l.add(ClassInfo.lookup(i));
		return l;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all declared methods on this class and all parent classes in child-to-parent order.
	 *
	 * <p>
	 * Methods are sorted alphabetically per class before being aggregated.
	 *
	 * @return All declared methods on this class and all parent classes in child-to-parent order.
	 */
	public Iterable<MethodInfo> getAllMethods() {
		if (allMethods == null)
			allMethods = Collections.unmodifiableList(findAllMethods());
		return allMethods;
	}

	/**
	 * Returns all declared methods on this class and all parent classes in parent-to-child order.
	 *
	 * <p>
	 * Methods are sorted alphabetically per class before being aggregated.
	 *
	 * @return All declared methods on this class and all parent classes in parent-to-child order.
	 */
	public Iterable<MethodInfo> getAllMethodsParentFirst() {
		if (allMethodsPf == null)
			allMethodsPf = Collections.unmodifiableList(findAllMethodsParentFirst());
		return ReverseIterable.of(allMethodsPf);
	}

	/**
	 * Returns all methods declared on this class.
	 *
	 * @return All methods declared on this class in alphabetical order.
	 */
	public Iterable<MethodInfo> getDeclaredMethods() {
		if (declaredMethods == null)
			declaredMethods = Collections.unmodifiableList(findDeclaredMethods());
		return declaredMethods;
	}

	/**
	 * Returns all public methods on this class.
	 *
	 * <p>
	 * Returns the methods (in the same order) as the call to {@link Class#getMethods()}.
	 *
	 * @return All public methods on this class.
	 */
	public Iterable<MethodInfo> getPublicMethods() {
		if (publicMethods == null)
			publicMethods = Collections.unmodifiableList(findPublicMethods());
		return publicMethods;
	}

	private List<MethodInfo> findAllMethods() {
		List<MethodInfo> l = new ArrayList<>();
		for (ClassInfo c : getParentClasses())
			for (MethodInfo m : c.getDeclaredMethods())
				l.add(m);
		return l;
	}

	private List<MethodInfo> findAllMethodsParentFirst() {
		List<MethodInfo> l = new ArrayList<>();
		for (ClassInfo c : getParentClassesParentFirst())
			for (MethodInfo m : c.getDeclaredMethods())
				l.add(m);
		return l;
	}

	private List<MethodInfo> findDeclaredMethods() {
		List<MethodInfo> l = new ArrayList<>(c.getDeclaredMethods().length);
		for (Method m : ClassUtils.sort(c.getDeclaredMethods()))
			l.add(MethodInfo.create(this, m));
		return l;
	}

	private List<MethodInfo> findPublicMethods() {
		List<MethodInfo> l = new ArrayList<>(c.getMethods().length);
		for (Method m : c.getMethods())
			l.add(MethodInfo.create(this, m));
		return l;
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
	public MethodInfo findPublicFromStringMethod() {
		for (String methodName : new String[]{"create","fromString","fromValue","valueOf","parse","parseString","forName","forString"})
			for (MethodInfo m : getPublicMethods())
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
	public MethodInfo findPublicStaticCreateMethod(Class<?> ic, String name) {
		for (MethodInfo m : getPublicMethods())
			if (m.isAll(STATIC, PUBLIC, NOT_DEPRECATED) && m.hasName(name) && m.hasReturnType(c) && m.hasArgs(ic))
				return m;
		return null;
	}

	/**
	 * Returns the <code>public static Builder create()</code> method on this class.
	 *
	 * @return The <code>public static Builder create()</code> method on this class, or <jk>null</jk> if it doesn't exist.
	 */
	public MethodInfo findBuilderCreateMethod() {
		for (MethodInfo m : getDeclaredMethods())
			if (m.isAll(PUBLIC, STATIC) && m.hasName("create") && ! m.hasReturnType(Void.class))
				return m;
		return null;
	}

	/**
	 * Returns the <code>T build()</code> method on this class.
	 *
	 * @return The <code>T build()</code> method on this class, or <jk>null</jk> if it doesn't exist.
	 */
	public MethodInfo findCreatePojoMethod() {
		for (MethodInfo m : getDeclaredMethods())
			if (m.isAll(NOT_STATIC) && m.hasName("build") && ! m.hasReturnType(Void.class))
				return m;
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fields
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all field on this class and all parent classes in child-to-parent order.
	 *
	 * <p>
	 * Fields are sorted alphabetically per class before being aggregated.
	 *
	 * @return All declared methods on this class and all parent classes in child-to-parent order.
	 */
	public Iterable<FieldInfo> getAllFields() {
		if (allFields == null)
			allFields = findAllFields();
		return allFields;
	}

	/**
	 * Returns all field on this class and all parent classes in parent-to-child order.
	 *
	 * <p>
	 * Fields are sorted alphabetically per class before being aggregated.
	 *
	 * @return All declared methods on this class and all parent classes in parent-to-child order.
	 */
	public Iterable<FieldInfo> getAllFieldsParentFirst() {
		if (allFieldsPf == null)
			allFieldsPf = findAllFieldsParentFirst();
		return allFieldsPf;
	}

	/**
	 * Returns all fields declared on this class.
	 *
	 * @return All fields declared on this class in alphabetical order.
	 */
	public Iterable<FieldInfo> getDeclaredFields() {
		if (declaredFields == null)
			declaredFields = Collections.unmodifiableList(findDeclaredFields());
		return declaredFields;
	}

	private List<FieldInfo> findAllFieldsParentFirst() {
		List<FieldInfo> l = new ArrayList<>();
		for (ClassInfo c : getParentClassesParentFirst())
			for (FieldInfo f : c.getDeclaredFields())
				l.add(f);
		return l;
	}

	private List<FieldInfo> findAllFields() {
		List<FieldInfo> l = new ArrayList<>();
		for (ClassInfo c : getParentClasses())
			for (FieldInfo f : c.getDeclaredFields())
				l.add(f);
		return l;
	}

	private List<FieldInfo> findDeclaredFields() {
		List<FieldInfo> l = new ArrayList<>(c.getDeclaredFields().length);
		for (Field f : ClassUtils.sort(c.getDeclaredFields()))
			l.add(FieldInfo.create(this, f));
		return l;
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
	public Constructor<?> findNoArgConstructor(Visibility v) {
		int mod = c.getModifiers();
		if (Modifier.isAbstract(mod))
			return null;
		boolean isMemberClass = c.isMemberClass() && ! ClassUtils.isStatic(c);
		for (Constructor<?> cc : c.getConstructors()) {
			mod = cc.getModifiers();
			if (ClassUtils.hasNumArgs(cc, isMemberClass ? 1 : 0) && v.isVisible(mod) && ClassUtils.isNotDeprecated(cc))
				return v.transform(cc);
		}
		return null;
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
	public <T> Constructor<T> findConstructor(Visibility vis, boolean fuzzyArgs, Class<?>...argTypes) {
		return (Constructor<T>)ClassUtils.findConstructor(c, vis, fuzzyArgs, argTypes);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds the annotation of the specified type defined on this method.
	 *
	 * <p>
	 * If the annotation cannot be found on the immediate method, searches methods with the same
	 * signature on the parent classes or interfaces.
	 * <br>The search is performed in child-to-parent order.
	 *
	 * <p>
	 * If still not found, searches for the annotation on the return type of the method.
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
	 * Returns <jk>true</jk> if this method has the specified annotation.
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
	 * Returns all annotations of the specified type defined on the specified method.
	 *
	 * <p>
	 * Searches all methods with the same signature on the parent classes or interfaces
	 * and the return type on the method.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found in child-to-parent order, or an empty list if none found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> getAnnotations(Class<T> a) {
		List<T> l = (List<T>)annotationsMap().get(a);
		if (l == null) {
			l = Collections.unmodifiableList(findAnnotations(a));
			annotationsMap().put(a, l);
		}
		return l;
	}

	/**
	 * Identical to {@link #getAnnotations(Class)} but returns the list in reverse (parent-to-child) order.
	 *
	 * @param a
	 * 	The annotation to search for.
	 * @return
	 * 	A list of all matching annotations found in parent-to-child order, or an empty list if none found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> getAnnotationsParentFirst(Class<T> a) {
		List<T> l = (List<T>)annotationsPfMap().get(a);
		if (l == null) {
			l = new ArrayList<>(getAnnotations(a));
			Collections.reverse(l);
			l = Collections.unmodifiableList(l);
			annotationsPfMap().put(a, l);
		}
		return l;
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
		if (c != null)
			for (Annotation a2 : c.getDeclaredAnnotations())
				if (a2.annotationType() == a)
					return (T)a2;
		return null;
	}

	private <T extends Annotation> T findAnnotation(Class<T> a) {
		if (c != null) {
			T t2 = getDeclaredAnnotation(a);
			if (t2 != null)
				return t2;

			ClassInfo sci = getParent();
			if (sci != null) {
				t2 = sci.getAnnotation(a);
				if (t2 != null)
					return t2;
			}

			for (ClassInfo c2 : getInterfaces()) {
				t2 = c2.getAnnotation(a);
				if (t2 != null)
					return t2;
			}
		}
		return null;
	}

	private <T extends Annotation> List<T> findAnnotations(Class<T> a) {
		List<T> l = new LinkedList<>();
		ClassUtils.appendAnnotations(a, type, l);
		return l;
	}

	private synchronized Map<Class<?>,Optional<Annotation>> annotationMap() {
		if (annotationMap == null)
			annotationMap = new ConcurrentHashMap<>();
		return annotationMap;
	}

	private synchronized Map<Class<?>,List<?>> annotationsMap() {
		if (annotationsMap == null)
			annotationsMap = new ConcurrentHashMap<>();
		return annotationsMap;
	}

	private synchronized Map<Class<?>,List<?>> annotationsPfMap() {
		if (annotationsPfMap == null)
			annotationsPfMap = new ConcurrentHashMap<>();
		return annotationsPfMap;
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
}
