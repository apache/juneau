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

import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
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
public final class ClassInfo {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Map<Class<?>,ClassInfo> CACHE = new ConcurrentHashMap<>();

	/** Reusable ClassInfo for Object class. */
	public static final ClassInfo OBJECT = ClassInfo.of(Object.class);

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
		ClassInfo ci = CACHE.get(c);
		if (ci == null) {
			ci = new ClassInfo(c, c);
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
		ClassInfo.of(c).forEachPublicMethod(
			m -> m.hasName("getTargetClass") && m.hasNoParams() && m.hasReturnType(Class.class),
			m -> safeRun(() -> v.set(m.invoke(o)))
		);
		return v.orElse(null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

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
	 * @param <T> The inner class type.
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

	MethodInfo getMethodInfo(Method x) {
		MethodInfo i = methods.get(x);
		if (i == null) {
			i = new MethodInfo(this, x);
			methods.put(x, i);
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

	ConstructorInfo getConstructorInfo(Constructor<?> x) {
		ConstructorInfo i = constructors.get(x);
		if (i == null) {
			i = new ConstructorInfo(this, x);
			constructors.put(x, i);
		}
		return i;
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
	public ClassInfo getSuperclass() {
		return c == null ? null : of(c.getSuperclass());
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
	public List<ClassInfo> getDeclaredInterfaces() {
		return ulist(_getDeclaredInterfaces());
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
	public List<ClassInfo> getInterfaces() {
		return ulist(_getInterfaces());
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
	public List<ClassInfo> getParents() {
		return ulist(_getParents());
	}

	/**
	 * Returns a list including this class and all parent classes and interfaces.
	 *
	 * <p>
	 * Results are classes-before-interfaces, then child-to-parent order.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are ordered child-to-parent order with classes listed before interfaces.
	 */
	public List<ClassInfo> getAllParents() {
		return ulist(_getAllParents());
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
		for (ClassInfo ci : _getAllParents())
			if (test(filter, ci))
				return ci;
		return null;
	}

	/** Results are in child-to-parent order. */
	ClassInfo[] _getInterfaces() {
		if (interfaces == null) {
			synchronized(this) {
				Set<ClassInfo> s = set();
				for (ClassInfo ci : _getParents())
					for (ClassInfo ci2 : ci._getDeclaredInterfaces()) {
						s.add(ci2);
						for (ClassInfo ci3 : ci2._getInterfaces())
							s.add(ci3);
					}
				interfaces = s.toArray(new ClassInfo[s.size()]);
			}
		}
		return interfaces;
	}

	/** Results are in the same order as Class.getInterfaces(). */
	ClassInfo[] _getDeclaredInterfaces() {
		if (declaredInterfaces == null) {
			synchronized(this) {
				Class<?>[] ii = c == null ? new Class[0] : c.getInterfaces();
				ClassInfo[] l = new ClassInfo[ii.length];
				for (int i = 0; i < ii.length; i++)
					l[i] = of(ii[i]);
				declaredInterfaces = l;
			}
		}
		return declaredInterfaces;
	}

	/** Results are in child-to-parent order. */
	ClassInfo[] _getParents() {
		if (parents == null) {
			synchronized(this) {
				List<ClassInfo> l = list();
				Class<?> pc = c;
				while (pc != null && pc != Object.class) {
					l.add(of(pc));
					pc = pc.getSuperclass();
				}
				parents = l.toArray(new ClassInfo[l.size()]);
			}
		}
		return parents;
	}

	/** Results are classes-before-interfaces, then child-to-parent order. */
	ClassInfo[] _getAllParents() {
		if (allParents == null) {
			synchronized(this) {
				ClassInfo[] a1 = _getParents(), a2 = _getInterfaces();
				ClassInfo[] l = new ClassInfo[a1.length + a2.length];
				for (int i = 0; i < a1.length; i++)
					l[i] = a1[i];
				for (int i = 0; i < a2.length; i++)
					l[i+a1.length] = a2[i];
				allParents = l;
			}
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
		return ulist(_getPublicMethods());
	}

	/**
	 * Performs an action on all matching public methods on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachPublicMethod(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (MethodInfo mi : _getPublicMethods())
			consume(filter, action, mi);
		return this;
	}

	/**
	 * Returns the first matching public method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public final MethodInfo getPublicMethod(Predicate<MethodInfo> filter) {
		for (MethodInfo mi : _getPublicMethods())
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
	public List<MethodInfo> getDeclaredMethods() {
		return ulist(_getDeclaredMethods());
	}

	/**
	 * Performs an action on all matching declared methods on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachDeclaredMethod(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (MethodInfo mi : _getDeclaredMethods())
			consume(filter, action, mi);
		return this;
	}

	/**
	 * Returns the first matching declared method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getDeclaredMethod(Predicate<MethodInfo> filter) {
		for (MethodInfo mi : _getDeclaredMethods())
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
	public List<MethodInfo> getMethods() {
		return ulist(_getAllMethods());
	}

	/**
	 * Performs an action on all matching methods on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachMethod(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (MethodInfo mi : _getAllMethods())
			consume(filter, action, mi);
		return this;
	}

	/**
	 * Returns the first matching method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getMethod(Predicate<MethodInfo> filter) {
		for (MethodInfo mi : _getAllMethods())
			if (test(filter, mi))
				return mi;
		return null;
	}

	/**
	 * Returns all declared methods on this class and all parent classes.
	 *
	 * @return
	 * 	All declared methods on this class and all parent classes.
	 * 	<br>Results are ordered parent-to-child, and then alphabetically per class.
	 * 	<br>List is unmodifiable.
	 */
	public List<MethodInfo> getAllMethodsParentFirst() {
		return ulist(_getAllMethodsParentFirst());
	}

	/**
	 * Performs an action on all matching declared methods on this class and all parent classes.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachAllMethodParentFirst(Predicate<MethodInfo> filter, Consumer<MethodInfo> action) {
		for (MethodInfo mi : _getAllMethodsParentFirst())
			consume(filter, action, mi);
		return this;
	}

	MethodInfo[] _getPublicMethods() {
		if (publicMethods == null) {
			synchronized(this) {
				Method[] mm = c == null ? new Method[0] : c.getMethods();
				List<MethodInfo> l = list(mm.length);
				for (Method m : mm)
					if (m.getDeclaringClass() != Object.class)
						l.add(getMethodInfo(m));
				l.sort(null);
				publicMethods = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return publicMethods;
	}

	MethodInfo[] _getDeclaredMethods() {
		if (declaredMethods == null) {
			synchronized(this) {
				Method[] mm = c == null ? new Method[0] : c.getDeclaredMethods();
				List<MethodInfo> l = list(mm.length);
				for (Method m : mm)
					if (! "$jacocoInit".equals(m.getName())) // Jacoco adds its own simulated methods.
						l.add(getMethodInfo(m));
				l.sort(null);
				declaredMethods = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return declaredMethods;
	}

	MethodInfo[] _getAllMethods() {
		if (allMethods == null) {
			synchronized(this) {
				List<MethodInfo> l = list();
				for (ClassInfo c : _getAllParents())
					c._appendDeclaredMethods(l);
				allMethods = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return allMethods;
	}

	MethodInfo[] _getAllMethodsParentFirst() {
		if (allMethodsParentFirst == null) {
			synchronized(this) {
				List<MethodInfo> l = list();
				ClassInfo[] parents = _getAllParents();
				for (int i = parents.length-1; i >=0; i--)
					parents[i]._appendDeclaredMethods(l);
				allMethodsParentFirst = l.toArray(new MethodInfo[l.size()]);
			}
		}
		return allMethodsParentFirst;
	}

	private synchronized List<MethodInfo> _appendDeclaredMethods(List<MethodInfo> l) {
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
		return ulist(_getPublicConstructors());
	}

	/**
	 * Performs an action on all matching public constructors on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachPublicConstructor(Predicate<ConstructorInfo> filter, Consumer<ConstructorInfo> action) {
		for (ConstructorInfo mi : _getPublicConstructors())
			consume(filter, action, mi);
		return this;
	}

	/**
	 * Returns the first matching public constructor on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The public constructor that matches the specified predicate.
	 */
	public ConstructorInfo getPublicConstructor(Predicate<ConstructorInfo> filter) {
		for (ConstructorInfo ci : _getPublicConstructors())
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
	public List<ConstructorInfo> getDeclaredConstructors() {
		return ulist(_getDeclaredConstructors());
	}

	/**
	 * Performs an action on all matching declared constructors on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachDeclaredConstructor(Predicate<ConstructorInfo> filter, Consumer<ConstructorInfo> action) {
		for (ConstructorInfo mi : _getDeclaredConstructors())
			consume(filter, action, mi);
		return this;
	}

	/**
	 * Returns the first matching declared constructor on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The declared constructor that matches the specified predicate.
	 */
	public ConstructorInfo getDeclaredConstructor(Predicate<ConstructorInfo> filter) {
		for (ConstructorInfo ci : _getDeclaredConstructors())
			if (test(filter, ci))
				return ci;
		return null;
	}

	ConstructorInfo[] _getPublicConstructors() {
		if (publicConstructors == null) {
			synchronized(this) {
				Constructor<?>[] cc = c == null ? new Constructor[0] : c.getConstructors();
				List<ConstructorInfo> l = list(cc.length);
				for (Constructor<?> ccc : cc)
					l.add(getConstructorInfo(ccc));
				l.sort(null);
				publicConstructors = l.toArray(new ConstructorInfo[l.size()]);
			}
		}
		return publicConstructors;
	}

	ConstructorInfo[] _getDeclaredConstructors() {
		if (declaredConstructors == null) {
			synchronized(this) {
				Constructor<?>[] cc = c == null ? new Constructor[0] : c.getDeclaredConstructors();
				List<ConstructorInfo> l = list(cc.length);
				for (Constructor<?> ccc : cc)
					l.add(getConstructorInfo(ccc));
				l.sort(null);
				declaredConstructors = l.toArray(new ConstructorInfo[l.size()]);
			}
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
	 * 	<br>List is unmodifiable.
	 */
	public List<FieldInfo> getPublicFields() {
		return ulist(_getPublicFields());
	}

	/**
	 * Performs an action on all matching public fields on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public final ClassInfo forEachPublicField(Predicate<FieldInfo> filter, Consumer<FieldInfo> action) {
		for (FieldInfo mi : _getPublicFields())
			consume(filter, action, mi);
		return this;
	}

	/**
	 * Returns the first matching public field on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The public field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getPublicField(Predicate<FieldInfo> filter) {
		for (FieldInfo f : _getPublicFields())
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
	public List<FieldInfo> getDeclaredFields() {
		return ulist(_getDeclaredFields());
	}

	/**
	 * Performs an action on all matching declared fields on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public ClassInfo forEachDeclaredField(Predicate<FieldInfo> filter, Consumer<FieldInfo> action) {
		for (FieldInfo fi : _getDeclaredFields())
			consume(filter, action, fi);
		return this;
	}

	/**
	 * Returns the first matching declared field on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The declared field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getDeclaredField(Predicate<FieldInfo> filter) {
		for (FieldInfo f : _getDeclaredFields())
			if (test(filter, f))
				return f;
		return null;
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
	public List<FieldInfo> getAllFields() {
		return ulist(_getAllFields());
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
		for (FieldInfo fi : _getAllFields())
			consume(filter, action, fi);
		return this;
	}

	FieldInfo[] _getPublicFields() {
		if (publicFields == null) {
			synchronized(this) {
				Map<String,FieldInfo> m = map();
				for (ClassInfo c : _getParents()) {
					for (FieldInfo f : c._getDeclaredFields()) {
						String fn = f.getName();
						if (f.isPublic() && ! (m.containsKey(fn) || "$jacocoData".equals(fn)))
							m.put(f.getName(), f);
					}
				}
				List<FieldInfo> l = listFrom(m.values());
				l.sort(null);
				publicFields = l.toArray(new FieldInfo[l.size()]);
			}
		}
		return publicFields;
	}

	FieldInfo[] _getDeclaredFields() {
		if (declaredFields == null) {
			synchronized(this) {
				Field[] ff = c == null ? new Field[0] : c.getDeclaredFields();
				List<FieldInfo> l = list(ff.length);
				for (Field f : ff)
					if (! "$jacocoData".equals(f.getName()))
						l.add(getFieldInfo(f));
				l.sort(null);
				declaredFields = l.toArray(new FieldInfo[l.size()]);
			}
		}
		return declaredFields;
	}

	FieldInfo[] _getAllFields() {
		if (allFields == null) {
			synchronized(this) {
				List<FieldInfo> l = list();
				ClassInfo[] parents = _getAllParents();
				for (int i = parents.length-1; i >=0; i--)
					for (FieldInfo f : parents[i]._getDeclaredFields())
						l.add(f);
				allFields = l.toArray(new FieldInfo[l.size()]);
			}
		}
		return allFields;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

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
		forEachAnnotation(annotationProvider, type, x-> true, x -> l.add(x));
		return l;
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
		if (t2 != null)
			consume(filter, action, t2);
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--)
			annotationProvider.forEachDeclaredAnnotation(type, interfaces[i].inner(), filter, action);
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--)
			annotationProvider.forEachDeclaredAnnotation(type, parents[i].inner(), filter, action);
		return this;
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
		if (x != null && test(filter, x))
			return x;
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--) {
			x = annotationProvider.firstAnnotation(type, interfaces[i].inner(), filter);
			if (x != null)
				return x;
		}
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--) {
			x = annotationProvider.firstAnnotation(type, parents[i].inner(), filter);
			if (x != null)
				return x;
		}
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
		for (int i = 0; i < parents.length; i++) {
			x = annotationProvider.lastAnnotation(type, parents[i].inner(), filter);
			if (x != null)
				return x;
		}
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			x = annotationProvider.lastAnnotation(type, interfaces[i].inner(), filter);
			if (x != null)
				return x;
		}
		x = getPackageAnnotation(type);
		if (x != null && test(filter, x))
			return x;
		return null;
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
	 * Returns <jk>true</jk> if this class has the specified annotation.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The <jk>true</jk> if annotation if found.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return hasAnnotation(null, type);
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
		return annotationProvider.firstAnnotation(type, c, x -> true) != null;
	}

	/**
	 * Returns the specified annotation only if it's been declared on the package of this class.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getPackageAnnotation(Class<A> type) {
		Package p = c == null ? null : c.getPackage();
		return (p == null ? null : p.getAnnotation(type));
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
		return getAnnotationList(x -> true);
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
		AnnotationList l = new AnnotationList();
		forEachAnnotationInfo(filter, x -> l.add(x));
		return l;
	}

	/*
	 * If the annotation is an array of other annotations, returns the inner annotations.
	 *
	 * @param a The annotation to split if repeated.
	 * @return The nested annotations, or a singleton array of the same annotation if it's not repeated.
	 */
	private static Annotation[] splitRepeated(Annotation a) {
		try {
			ClassInfo ci = ClassInfo.of(a.annotationType());
			MethodInfo mi = ci.getRepeatedAnnotationMethod();
			if (mi != null)
				return mi.invoke(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Annotation[]{a};
	}

	private <A extends Annotation> A findAnnotation(AnnotationProvider ap, Class<A> a) {
		if (a == null)
			return null;
		if (ap == null)
			ap = AnnotationProvider.DEFAULT;
		A t = ap.firstDeclaredAnnotation(a, c, x -> true);
		if (t != null)
			return t;
		ClassInfo sci = getSuperclass();
		if (sci != null) {
			t = sci.getAnnotation(ap, a);
			if (t != null)
				return t;
		}
		for (ClassInfo c2 : _getInterfaces()) {
			t = c2.getAnnotation(ap, a);
			if (t != null)
				return t;
		}
		return null;
	}

	private <A extends Annotation> A getAnnotation(AnnotationProvider ap, Class<A> a, Predicate<A> filter) {
		if (ap == null)
			ap = AnnotationProvider.DEFAULT;
		A t2 = getPackageAnnotation(a);
		if (t2 != null && filter.test(t2))
			return t2;
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--) {
			A o = ap.firstDeclaredAnnotation(a, interfaces[i].inner(), filter);
			if (o != null)
				return o;
		}
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--) {
			A o = ap.firstDeclaredAnnotation(a, parents[i].inner(), filter);
			if (o != null)
				return o;
		}
		return null;
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
		Package p = c.getPackage();
		if (p != null)
			for (Annotation a : p.getDeclaredAnnotations())
				for (Annotation a2 : splitRepeated(a))
					AnnotationInfo.of(p, a2).accept(filter, action);
		ClassInfo[] interfaces = _getInterfaces();
		for (int i = interfaces.length-1; i >= 0; i--)
			for (Annotation a : interfaces[i].c.getDeclaredAnnotations())
				for (Annotation a2 : splitRepeated(a))
					AnnotationInfo.of(interfaces[i], a2).accept(filter, action);
		ClassInfo[] parents = _getParents();
		for (int i = parents.length-1; i >= 0; i--)
			for (Annotation a : parents[i].c.getDeclaredAnnotations())
				for (Annotation a2 : splitRepeated(a))
					AnnotationInfo.of(parents[i], a2).accept(filter, action);
		return this;
	}

	Annotation[] _getDeclaredAnnotations() {
		if (declaredAnnotations == null) {
			synchronized(this) {
				declaredAnnotations = c.getDeclaredAnnotations();
			}
		}
		return declaredAnnotations;
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
					throw new BasicRuntimeException("Invalid flag for class: {0}", f);

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
					throw new BasicRuntimeException("Invalid flag for class: {0}", f);
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

	@SuppressWarnings("rawtypes")
	private static final Map<Class,Object> primitiveDefaultMap =
		mapBuilder(Class.class,Object.class).unmodifiable()
			.add(Boolean.TYPE, false)
			.add(Character.TYPE, (char)0)
			.add(Short.TYPE, (short)0)
			.add(Integer.TYPE, 0)
			.add(Long.TYPE, 0l)
			.add(Float.TYPE, 0f)
			.add(Double.TYPE, 0d)
			.add(Byte.TYPE, (byte)0)
			.add(Boolean.class, false)
			.add(Character.class, (char)0)
			.add(Short.class, (short)0)
			.add(Integer.class, 0)
			.add(Long.class, 0l)
			.add(Float.class, 0f)
			.add(Double.class, 0d)
			.add(Byte.class, (byte)0)
			.build();

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
			synchronized(this) {
				boolean b = false;
				repeatedAnnotationMethod = getPublicMethod(x -> x.hasName("value"));
				if (repeatedAnnotationMethod != null) {
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
				synchronized(this) {
					repeatedAnnotationMethod = getPublicMethod(x -> x.hasName("value"));
				}
			}
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
		throw new IllegalArgumentException("Could not resolve variable '"+actualType.getTypeName()+"' to a type.");
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

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

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
