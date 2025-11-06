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
import static org.apache.juneau.common.reflect.ClassNameFormat.*;
import static org.apache.juneau.common.reflect.ClassArrayFormat.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

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
@SuppressWarnings({"unchecked","rawtypes"})
public class ClassInfo extends ElementInfo implements Annotatable {

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

	// The underlying Type object (may be Class, ParameterizedType, GenericArrayType, etc.).
	private final Type t;

	// The underlying Class object (null for non-class types like TypeVariable).  Effectively final.
	private Class<?> c;

	// True if this represents a ParameterizedType (e.g., List<String>).
	private final boolean isParameterizedType;

	// Number of array dimensions (0 if not an array).
	private final Supplier<Integer> dimensions = memoize(this::findDimensions);

	// Base component type for arrays (e.g., String for String[][]), also handles GenericArrayType.  Cached and never null.
	private final Supplier<ClassInfo> componentType = memoize(this::findComponentType);

	// The package this class belongs to (null for primitive types and arrays).
	private final Supplier<PackageInfo> packageInfo = memoize(() -> opt(c).map(x -> PackageInfo.of(x.getPackage())).orElse(null));

	// All superclasses of this class in child-to-parent order, starting with this class.
	private final Supplier<List<ClassInfo>> parents = memoize(this::findParents);

	// All annotations declared directly on this class.
	private final Supplier<List<Annotation>> declaredAnnotations = memoize(() -> opt(c).map(x -> u(l(x.getDeclaredAnnotations()))).orElse(liste()));

	// All annotations declared directly on this class, wrapped in AnnotationInfo.
	private final Supplier<List<AnnotationInfo>> declaredAnnotations2 = memoize(() -> (List)declaredAnnotations.get().stream().map(a -> AnnotationInfo.of(this, a)).toList());

	// Fully qualified class name with generics (e.g., "java.util.List<java.lang.String>").
	private final Supplier<String> fullName = memoize(() -> getNameFormatted(FULL, true, '$', BRACKETS));

	// Simple class name with generics (e.g., "List<String>").
	private final Supplier<String> shortName = memoize(() -> getNameFormatted(SHORT, true, '$', BRACKETS));

	// Human-readable class name without generics (e.g., "List").
	private final Supplier<String> readableName = memoize(() -> getNameFormatted(SIMPLE, false, '$', WORD));

	// All interfaces declared directly by this class.
	private final Supplier<List<ClassInfo>> declaredInterfaces = memoize(() -> opt(c).map(x -> stream(x.getInterfaces()).map(ClassInfo::of).toList()).orElse(liste()));

	// All interfaces implemented by this class and its parents, in child-to-parent order.
	private final Supplier<List<ClassInfo>> interfaces = memoize(() -> getParents().stream().flatMap(x -> x.getDeclaredInterfaces().stream()).flatMap(ci2 -> concat(Stream.of(ci2), ci2.getInterfaces().stream())).distinct().toList());

	// All parent classes and interfaces, classes first, then in child-to-parent order.
	// TODO - Determine if this field is still needed now that we have parentsAndInterfaces which handles the hierarchy better.
	private final Supplier<List<ClassInfo>> allParents = memoize(() -> concat(getParents().stream(), getInterfaces().stream()).toList());

	// All parent classes and interfaces with proper traversal of interface hierarchy to avoid duplicates.
	private final Supplier<List<ClassInfo>> parentsAndInterfaces = memoize(this::findParentsAndInterfaces);

	/**
	 * Finds all parent classes and interfaces with proper traversal of interface hierarchy.
	 *
	 * @return A list of all parent classes and interfaces without duplicates.
	 */
	private List<ClassInfo> findParentsAndInterfaces() {
		var set = new LinkedHashSet<ClassInfo>();

		// Process all parent classes (includes this class)
		var parents = getParents();
		for (int i = 0; i < parents.size(); i++) {
			var parent = parents.get(i);
			set.add(parent);

			// Process interfaces declared on this parent (and their parent interfaces)
			var declaredInterfaces = parent.getDeclaredInterfaces();
			for (int j = 0; j < declaredInterfaces.size(); j++)
				addInterfaceHierarchy(set, declaredInterfaces.get(j));
		}

		return u(new ArrayList<>(set));
	}

	/**
	 * Helper method to recursively add an interface and its parent interfaces to the set.
	 *
	 * @param set The set to add to.
	 * @param iface The interface to add.
	 */
	private void addInterfaceHierarchy(LinkedHashSet<ClassInfo> set, ClassInfo iface) {
		if (!set.add(iface))
			return;

		// Process parent interfaces recursively
		var parentInterfaces = iface.getDeclaredInterfaces();
		for (int i = 0; i < parentInterfaces.size(); i++)
			addInterfaceHierarchy(set, parentInterfaces.get(i));
	}

	/**
	 * Finds all annotations on this class and parent classes/interfaces in child-to-parent order.
	 *
	 * <p>
	 * This is similar to {@link org.apache.juneau.common.reflect.AnnotationProvider2#find(Class)} but without runtime annotations.
	 *
	 * <p>
	 * Order of traversal:
	 * <ol>
	 * 	<li>Annotations declared on this class
	 * 	<li>Annotations declared on parent classes (child-to-parent order)
	 * 	<li>For each parent class, annotations on interfaces declared on that class (child-to-parent interface hierarchy)
	 * 	<li>Annotations on the package of this class
	 * </ol>
	 *
	 * @return A list of all annotation infos in child-to-parent order.
	 */
	private List<AnnotationInfo<Annotation>> findAnnotationInfos() {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		// On all parent classes and interfaces (properly traversed to avoid duplicates)
		var parentsAndInterfaces = getParentsAndInterfaces();
		for (int i = 0; i < parentsAndInterfaces.size(); i++) {
			var ci = parentsAndInterfaces.get(i);
			// Add declared annotations from this class/interface
			for (var a : ci.inner().getDeclaredAnnotations())
				for (var a2 : splitRepeated(a))
					list.add(AnnotationInfo.of(ci, a2));
		}

		// On the package of this class
		var pkg = getPackage();
		if (nn(pkg)) {
			var pi = PackageInfo.of(pkg.inner());
			for (var a : pkg.inner().getAnnotations())
				for (var a2 : splitRepeated(a))
					list.add(AnnotationInfo.of(pi, a2));
		}

		return u(list);
	}

	// All annotations on this class and parent classes/interfaces in child-to-parent order.
	private final Supplier<List<AnnotationInfo<Annotation>>> annotationInfos = memoize(this::findAnnotationInfos);

	// All record components if this is a record class (Java 14+).
	private final Supplier<List<RecordComponent>> recordComponents = memoize(() -> opt(c).filter(Class::isRecord).map(x -> u(l(x.getRecordComponents()))).orElse(liste()));

	// All generic interface types (e.g., List<String> implements Comparable<List<String>>).
	private final Supplier<List<Type>> genericInterfaces = memoize(() -> opt(c).map(x -> u(l(x.getGenericInterfaces()))).orElse(liste()));

	// All type parameters declared on this class (e.g., <T, U> in class Foo<T, U>).
	private final Supplier<List<TypeVariable<?>>> typeParameters = memoize(() -> opt(c).map(x -> u(l((TypeVariable<?>[])x.getTypeParameters()))).orElse(liste()));

	// All annotated interface types with their annotations.
	private final Supplier<List<AnnotatedType>> annotatedInterfaces = memoize(() -> opt(c).map(x -> u(l(x.getAnnotatedInterfaces()))).orElse(liste()));

	// All signers of this class (for signed JARs).
	private final Supplier<List<Object>> signers = memoize(() -> opt(c).map(Class::getSigners).map(x -> u(l(x))).orElse(liste()));

	// All public methods on this class and inherited, excluding Object methods.
	private final Supplier<List<MethodInfo>> publicMethods = memoize(() -> opt(c).map(x -> stream(x.getMethods()).filter(m -> ne(m.getDeclaringClass(), Object.class)).map(this::getMethodInfo).sorted().toList()).orElse(liste()));

	// All methods declared directly on this class (public, protected, package, private).
	private final Supplier<List<MethodInfo>> declaredMethods = memoize(() -> opt(c).map(x -> stream(x.getDeclaredMethods()).filter(m -> ne("$jacocoInit", m.getName())).map(this::getMethodInfo).sorted().toList()).orElse(liste()));

	// All methods from this class and all parents, in child-to-parent order.
	private final Supplier<List<MethodInfo>> allMethods = memoize(() -> allParents.get().stream().flatMap(c -> c.getDeclaredMethods().stream()).toList());

	// All methods from this class and all parents, in parent-to-child order.
	private final Supplier<List<MethodInfo>> allMethodsParentFirst = memoize(() -> rstream(getAllParents()).flatMap(c -> c.getDeclaredMethods().stream()).toList());

	// All public fields from this class and parents, deduplicated by name (child wins).
	private final Supplier<List<FieldInfo>> publicFields = memoize(() -> parents.get().stream().flatMap(c -> c.getDeclaredFields().stream()).filter(f -> f.isPublic() && ne("$jacocoData", f.getName())).collect(toMap(FieldInfo::getName, x -> x, (a, b) -> a, LinkedHashMap::new)).values().stream().sorted().collect(toList()));

	// All fields declared directly on this class (public, protected, package, private).
	private final Supplier<List<FieldInfo>> declaredFields = memoize(() -> opt(c).map(x -> stream(x.getDeclaredFields()).filter(f -> ne("$jacocoData", f.getName())).map(this::getFieldInfo).sorted().toList()).orElse(liste()));

	// All fields from this class and all parents, in parent-to-child order.
	private final Supplier<List<FieldInfo>> allFields = memoize(() -> rstream(allParents.get()).flatMap(c -> c.getDeclaredFields().stream()).toList());

	// All public constructors declared on this class.
	private final Supplier<List<ConstructorInfo>> publicConstructors = memoize(() -> opt(c).map(x -> stream(x.getConstructors()).map(this::getConstructorInfo).sorted().toList()).orElse(liste()));

	// All constructors declared on this class (public, protected, package, private).
	private final Supplier<List<ConstructorInfo>> declaredConstructors = memoize(() -> opt(c).map(x -> stream(x.getDeclaredConstructors()).map(this::getConstructorInfo).sorted().toList()).orElse(liste()));

	// The repeated annotation method (value()) if this class is a @Repeatable container.
	private final Supplier<MethodInfo> repeatedAnnotationMethod = memoize(this::findRepeatedAnnotationMethod);

	// Cache of wrapped Method objects.
	private final Cache<Method,MethodInfo> methodCache = Cache.of(Method.class, MethodInfo.class).build();

	// Cache of wrapped Field objects.
	private final Cache<Field,FieldInfo> fieldCache = Cache.of(Field.class, FieldInfo.class).build();

	// Cache of wrapped Constructor objects.
	private final Cache<Constructor,ConstructorInfo> constructorCache = Cache.of(Constructor.class, ConstructorInfo.class).build();

	/**
	 * Constructor.
	 *
	 * @param c The class type.
	 * @param t The generic type (if parameterized type).
	 */
	protected ClassInfo(Class<?> c, Type t) {
		super(c == null ? 0 : c.getModifiers());
		this.t = t;
		this.c = c;
		this.isParameterizedType = t == null ? false : (t instanceof ParameterizedType);
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
		var interfaces2 = interfaces.get();
		for (int i = interfaces2.size() - 1; i >= 0; i--)
			annotationProvider.forEachDeclaredAnnotation(type, interfaces2.get(i).inner(), filter, action);
		var parents2 = parents.get();
		for (int i = parents2.size() - 1; i >= 0; i--)
			annotationProvider.forEachDeclaredAnnotation(type, parents2.get(i).inner(), filter, action);
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
	 * Returns all fields on this class and all parent classes.
	 *
	 * <p>
	 * 	Results are ordered parent-to-child, and then alphabetical per class.
	 *
	 * @return
	 * 	All declared fields on this class.
	 * 	<br>List is unmodifiable.
	 */
	public List<FieldInfo> getAllFields() { return allFields.get(); }

	/**
	 * Returns all declared methods on this class and all parent classes.
	 *
	 * @return
	 * 	All declared methods on this class and all parent classes.
	 * 	<br>Results are ordered parent-to-child, and then alphabetically per class.
	 * 	<br>List is unmodifiable.
	 */
	public List<MethodInfo> getAllMethodsParentFirst() { return allMethodsParentFirst.get(); }

	/**
	 * Returns a list including this class and all parent classes and interfaces.
	 *
	 * <p>
	 * Results are classes-before-interfaces, then child-to-parent order.
	 *
	 * @return An unmodifiable list including this class and all parent classes.
	 * 	<br>Results are ordered child-to-parent order with classes listed before interfaces.
	 */
	public List<ClassInfo> getAllParents() { return allParents.get(); }

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
		for (var ci : allParents.get())
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
	 * Returns the base component type of this class.
	 *
	 * <p>
	 * For array types (e.g., <c>String[][]</c>), returns the deepest component type (e.g., <c>String</c>).
	 * <br>For non-array types, returns this class itself.
	 *
	 * <p>
	 * <b>Note:</b> Unlike {@link Class#getComponentType()}, this method also handles generic array types (e.g., <c>List&lt;String&gt;[]</c>)
	 * and returns the full parameterized type information (e.g., <c>List&lt;String&gt;</c>).
	 * Additionally, this method never returns <jk>null</jk> - non-array types return <jk>this</jk> instead.
	 *
	 * @return The base component type of an array, or this class if not an array.
	 */
	public ClassInfo getComponentType() {
		return componentType.get();
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
		for (var ci : declaredConstructors.get())
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
	public List<ConstructorInfo> getDeclaredConstructors() { return declaredConstructors.get(); }

	/**
	 * Returns the first matching declared field on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The declared field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getDeclaredField(Predicate<FieldInfo> filter) {
		for (var f : declaredFields.get())
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
	public List<FieldInfo> getDeclaredFields() { return declaredFields.get(); }

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
	public List<ClassInfo> getDeclaredInterfaces() { return declaredInterfaces.get(); }

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
		for (var mi : declaredMethods.get())
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
	public List<MethodInfo> getDeclaredMethods() { return declaredMethods.get(); }

	/**
	 * Returns the number of dimensions if this is an array type.
	 *
	 * @return The number of dimensions if this is an array type, or <c>0</c> if it is not.
	 */
	public int getDimensions() {
		return dimensions.get();
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
	public String getNameFull() {
		return fullName.get();
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
	public List<ClassInfo> getInterfaces() { return interfaces.get(); }

	/**
	 * Returns all parent classes and interfaces in proper traversal order.
	 *
	 * <p>
	 * This method returns a unique list of all parent classes (including this class) and all interfaces
	 * (including interface hierarchies) with proper handling of duplicates. The order is:
	 * <ol>
	 * 	<li>This class
	 * 	<li>Parent classes in child-to-parent order
	 * 	<li>For each class, interfaces declared on that class and their parent interfaces
	 * </ol>
	 *
	 * <p>
	 * This is useful for annotation processing where you need to traverse the complete type hierarchy
	 * without duplicates.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Interface hierarchy:</jc>
	 * 	<jk>interface</jk> ISuperGrandParent {}
	 * 	<jk>interface</jk> IGrandParent <jk>extends</jk> ISuperGrandParent {}
	 * 	<jk>interface</jk> ISuperParent {}
	 * 	<jk>interface</jk> IParent <jk>extends</jk> ISuperParent {}
	 * 	<jk>interface</jk> IChild {}
	 *
	 * 	<jc>// Class hierarchy:</jc>
	 * 	<jk>class</jk> GrandParent <jk>implements</jk> IGrandParent {}
	 * 	<jk>class</jk> Parent <jk>extends</jk> GrandParent <jk>implements</jk> IParent {}
	 * 	<jk>class</jk> Child <jk>extends</jk> Parent <jk>implements</jk> IChild {}
	 *
	 * 	<jc>// For Child, returns (in this order):</jc>
	 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(Child.<jk>class</jk>);
	 * 	List&lt;ClassInfo&gt; <jv>result</jv> = <jv>ci</jv>.getParentsAndInterfaces();
	 * 	<jc>// Result: [</jc>
	 * 	<jc>//   Child,                  // 1. This class</jc>
	 * 	<jc>//   IChild,                 // 2. Interface on Child</jc>
	 * 	<jc>//   Parent,                 // 3. Parent class</jc>
	 * 	<jc>//   IParent,                // 4. Interface on Parent</jc>
	 * 	<jc>//   ISuperParent,           // 5. Parent interface of IParent</jc>
	 * 	<jc>//   GrandParent,            // 6. Grandparent class</jc>
	 * 	<jc>//   IGrandParent,           // 7. Interface on GrandParent</jc>
	 * 	<jc>//   ISuperGrandParent       // 8. Parent interface of IGrandParent</jc>
	 * 	<jc>// ]</jc>
	 * </p>
	 *
	 * @return An unmodifiable list of all parent classes and interfaces, properly ordered without duplicates.
	 */
	public List<ClassInfo> getParentsAndInterfaces() { return parentsAndInterfaces.get(); }

	/**
	 * Returns all annotations on this class and parent classes/interfaces in child-to-parent order.
	 *
	 * <p>
	 * This returns all declared annotations from:
	 * <ol>
	 * 	<li>This class
	 * 	<li>Parent classes in child-to-parent order
	 * 	<li>For each class, interfaces declared on that class and their parent interfaces
	 * 	<li>The package of this class
	 * </ol>
	 *
	 * <p>
	 * This does NOT include runtime annotations. For runtime annotation support, use
	 * {@link org.apache.juneau.common.reflect.AnnotationProvider2}.
	 *
	 * @return An unmodifiable list of all annotation infos.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotationInfos() { return annotationInfos.get(); }

	/**
	 * Returns all annotations of the specified type on this class and parent classes/interfaces in child-to-parent order.
	 *
	 * <p>
	 * This returns all declared annotations from:
	 * <ol>
	 * 	<li>This class
	 * 	<li>Parent classes in child-to-parent order
	 * 	<li>For each class, interfaces declared on that class and their parent interfaces
	 * 	<li>The package of this class
	 * </ol>
	 *
	 * <p>
	 * This does NOT include runtime annotations. For runtime annotation support, use
	 * {@link org.apache.juneau.common.reflect.AnnotationProvider2}.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type to filter by.
	 * @return A stream of annotation infos of the specified type.
	 */
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotationInfos(Class<A> type) {
		assertArgNotNull("type", type);
		return getAnnotationInfos().stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Returns the first matching method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getMethod(Predicate<MethodInfo> filter) {
		for (var mi : allMethods.get())
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
	public List<MethodInfo> getMethods() { return allMethods.get(); }

	/**
	 * Returns the name of the underlying class.
	 *
	 * <p>
	 * Equivalent to calling {@link Class#getName()} or {@link Type#getTypeName()} depending on whether
	 * this is a class or type.
	 *
	 * <p>
	 * This method returns the JVM internal format for class names:
	 * <ul>
	 * 	<li>Uses fully qualified package names
	 * 	<li>Uses <js>'$'</js> separator for nested classes
	 * 	<li>Uses JVM notation for arrays (e.g., <js>"[Ljava.lang.String;"</js>)
	 * 	<li>Uses single letters for primitive arrays (e.g., <js>"[I"</js> for <c>int[]</c>)
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"java.lang.String"</js> - Normal class
	 * 	<li><js>"[Ljava.lang.String;"</js> - Array
	 * 	<li><js>"[[Ljava.lang.String;"</js> - Multi-dimensional array
	 * 	<li><js>"java.util.Map$Entry"</js> - Nested class
	 * 	<li><js>"int"</js> - Primitive class
	 * 	<li><js>"[I"</js> - Primitive array
	 * 	<li><js>"[[I"</js> - Multi-dimensional primitive array
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><c>{@link #getNameCanonical()}</c> - Java source code format (uses <js>'.'</js> and <js>"[]"</js>)
	 * 	<li><c>{@link #getNameSimple()}</c> - Simple class name without package
	 * 	<li><c>{@link #getNameFull()}</c> - Full name with type parameters
	 * 	<li><c>{@link #getNameShort()}</c> - Short name with type parameters
	 * 	<li><c>{@link #getNameReadable()}</c> - Human-readable name (uses <js>"Array"</js> suffix)
	 * </ul>
	 *
	 * @return The name of the underlying class in JVM format.
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
	public String getNameCanonical() {
		// For Class objects, delegate to Class.getCanonicalName() which handles local/anonymous classes
		// For Type objects (ParameterizedType, etc.), compute the canonical name
		if (c != null && !isParameterizedType) {
			return c.getCanonicalName();
		}
		// For ParameterizedType, we can't have a true canonical name with type parameters
		// Return null to maintain consistency with Class.getCanonicalName() behavior
		return null;
	}

	/**
	 * Returns all possible names for this class.
	 *
	 * @return
	 * 	An array consisting of:
	 * 	<ul>
	 * 		<li>{@link #getNameFull()}
	 * 		<li>{@link Class#getName()} - Note that this might be a dup.
	 * 		<li>{@link #getNameShort()}
	 * 		<li>{@link #getNameSimple()}
	 * 	</ul>
	 */
	public String[] getNames() { return a(getNameFull(), c.getName(), getNameShort(), getNameSimple()); }

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
		int expectedParams = isNonStaticMemberClass() ? 1 : 0;
		return getDeclaredConstructors().stream()
			.filter(cc -> cc.hasNumParameters(expectedParams))
			.filter(cc -> cc.isVisible(v))
			.map(cc -> cc.accessible(v))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns the package of this class.
	 *
	 * @return The package of this class wrapped in a {@link PackageInfo}, or <jk>null</jk> if this class has no package.
	 */
	public PackageInfo getPackage() {
		return packageInfo.get();
	}

	/**
	 * Returns the specified annotation only if it's been declared on the package of this class.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation class.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getPackageAnnotation(Class<A> type) {
		PackageInfo pi = getPackage();
		if (pi == null)
			return null;
		var ai = pi.getAnnotation(type);
		return ai == null ? null : ai.inner();
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
	public List<ClassInfo> getParents() { return parents.get(); }

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
		for (var ci : publicConstructors.get())
			if (test(filter, ci))
				return ci;
		return null;
	}

	/**
	 * Returns all the public constructors defined on this class.
	 *
	 * @return All public constructors defined on this class.
	 */
	public List<ConstructorInfo> getPublicConstructors() { return publicConstructors.get(); }

	/**
	 * Returns the first matching public field on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The public field, or <jk>null</jk> if not found.
	 */
	public FieldInfo getPublicField(Predicate<FieldInfo> filter) {
		for (var f : publicFields.get())
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
	public List<FieldInfo> getPublicFields() { return publicFields.get(); }

	/**
	 * Returns the first matching public method on this class.
	 *
	 * @param filter A predicate to apply to the entries to determine if value should be used.  Can be <jk>null</jk>.
	 * @return The first matching method, or <jk>null</jk> if no methods matched.
	 */
	public MethodInfo getPublicMethod(Predicate<MethodInfo> filter) {
		for (var mi : publicMethods.get())
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
	public List<MethodInfo> getPublicMethods() { return publicMethods.get(); }

	/**
	 * Same as {@link #getNameSimple()} but uses <js>"Array"</js> instead of <js>"[]"</js>.
	 *
	 * @return The readable name for this class.
	 */
	public String getNameReadable() {
		return readableName.get();
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
		return repeatedAnnotationMethod.get();
	}

	/**
	 * Returns the short name of the underlying class.
	 *
	 * <p>
	 * Similar to {@link #getNameSimple()} but also renders local or member class name prefixes.
	 *
	 * @return The short name of the underlying class.
	 */
	public String getNameShort() {
		return shortName.get();
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
	public String getNameSimple() { return nn(c) ? c.getSimpleName() : t.getTypeName(); }

	/**
	 * Returns a formatted class name with configurable options.
	 *
	 * <p>
	 * This is a unified method that can produce output equivalent to all other name methods
	 * by varying the parameters.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Given: java.util.HashMap&lt;String,Integer&gt;[]</jc>
	 *
	 * 	<jc>// Full name with generics</jc>
	 * 	getFormattedName(ClassNameFormat.<jsf>FULL</jsf>, <jk>true</jk>, '$', ClassArrayFormat.<jsf>BRACKETS</jsf>)
	 * 	<jc>// → "java.util.HashMap&lt;java.lang.String,java.lang.Integer&gt;[]"</jc>
	 *
	 * 	<jc>// Short name with generics</jc>
	 * 	getFormattedName(ClassNameFormat.<jsf>SHORT</jsf>, <jk>true</jk>, '$', ClassArrayFormat.<jsf>BRACKETS</jsf>)
	 * 	<jc>// → "HashMap&lt;String,Integer&gt;[]"</jc>
	 *
	 * 	<jc>// Simple name</jc>
	 * 	getFormattedName(ClassNameFormat.<jsf>SIMPLE</jsf>, <jk>false</jk>, '$', ClassArrayFormat.<jsf>BRACKETS</jsf>)
	 * 	<jc>// → "HashMap[]"</jc>
	 *
	 * 	<jc>// With dot separator</jc>
	 * 	getFormattedName(ClassNameFormat.<jsf>SHORT</jsf>, <jk>false</jk>, '.', ClassArrayFormat.<jsf>BRACKETS</jsf>)
	 * 	<jc>// → "Map.Entry"</jc>
	 *
	 * 	<jc>// Word format for arrays</jc>
	 * 	getFormattedName(ClassNameFormat.<jsf>SIMPLE</jsf>, <jk>false</jk>, '$', ClassArrayFormat.<jsf>WORD</jsf>)
	 * 	<jc>// → "HashMapArray"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Equivalent Methods:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><c>getName()</c> = <c>getFormattedName(FULL, <jk>false</jk>, '$', JVM)</c>
	 * 	<li><c>getCanonicalName()</c> = <c>getFormattedName(FULL, <jk>false</jk>, '.', BRACKETS)</c>
	 * 	<li><c>getSimpleName()</c> = <c>getFormattedName(SIMPLE, <jk>false</jk>, '$', BRACKETS)</c>
	 * 	<li><c>getFullName()</c> = <c>getFormattedName(FULL, <jk>true</jk>, '$', BRACKETS)</c>
	 * 	<li><c>getShortName()</c> = <c>getFormattedName(SHORT, <jk>true</jk>, '$', BRACKETS)</c>
	 * 	<li><c>getReadableName()</c> = <c>getFormattedName(SIMPLE, <jk>false</jk>, '$', WORD)</c>
	 * </ul>
	 *
	 * @param nameFormat
	 * 	Controls which parts of the class name to include (package, outer classes).
	 * @param includeTypeParams
	 * 	If <jk>true</jk>, include generic type parameters recursively.
	 * 	<br>For example: <js>"HashMap&lt;String,Integer&gt;"</js> instead of <js>"HashMap"</js>
	 * @param separator
	 * 	Character to use between outer and inner class names.
	 * 	<br>Typically <js>'$'</js> (JVM format) or <js>'.'</js> (canonical format).
	 * 	<br>Ignored when <c>nameFormat</c> is {@link ClassNameFormat#SIMPLE}.
	 * @param arrayFormat
	 * 	How to format array dimensions.
	 * @return
	 * 	The formatted class name.
	 */
	public String getNameFormatted(ClassNameFormat nameFormat, boolean includeTypeParams, char separator, ClassArrayFormat arrayFormat) {
		var sb = new StringBuilder(128);
		appendNameFormatted(sb, nameFormat, includeTypeParams, separator, arrayFormat);
		return sb.toString();
	}

	/**
	 * Appends a formatted class name to a StringBuilder with configurable options.
	 *
	 * <p>
	 * This is the core implementation method used by all other name formatting methods.
	 * Using this method directly avoids String allocations when building complex strings.
	 * The method is recursive to handle nested generic type parameters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	StringBuilder sb = <jk>new</jk> StringBuilder();
	 * 	sb.append(<js>"Class: "</js>);
	 * 	ClassInfo.<jsm>of</jsm>(HashMap.<jk>class</jk>).appendFormattedName(sb, ClassNameFormat.<jsf>FULL</jsf>, <jk>true</jk>, '$', ClassArrayFormat.<jsf>BRACKETS</jsf>);
	 * 	<jc>// sb now contains: "Class: java.util.HashMap"</jc>
	 * </p>
	 *
	 * @param sb
	 * 	The StringBuilder to append to.
	 * @param nameFormat
	 * 	Controls which parts of the class name to include (package, outer classes).
	 * @param includeTypeParams
	 * 	If <jk>true</jk>, include generic type parameters recursively.
	 * @param separator
	 * 	Character to use between outer and inner class names.
	 * 	<br>Ignored when <c>nameFormat</c> is {@link ClassNameFormat#SIMPLE}.
	 * @param arrayFormat
	 * 	How to format array dimensions.
	 * @return
	 * 	The same StringBuilder for method chaining.
	 */
	@SuppressWarnings("null")
	public StringBuilder appendNameFormatted(StringBuilder sb, ClassNameFormat nameFormat, boolean includeTypeParams, char separator, ClassArrayFormat arrayFormat) {
		var dim = getDimensions();

		// Handle arrays - format component type recursively, then add array notation
		if (dim > 0) {
			var componentType = getComponentType();
			componentType.appendNameFormatted(sb, nameFormat, includeTypeParams, separator, arrayFormat);

			if (arrayFormat == ClassArrayFormat.WORD) {
				for (int i = 0; i < dim; i++)
					sb.append("Array");
			} else if (arrayFormat == ClassArrayFormat.BRACKETS) {
				for (int i = 0; i < dim; i++)
					sb.append("[]");
			}
			// JVM format is already in getName() - would need special handling

			return sb;
		}

		// Get the raw class - for ParameterizedType, extract the raw type
		var ct = c;
		if (ct == null && isParameterizedType) {
			var pt = (ParameterizedType)t;
			ct = (Class<?>)pt.getRawType();
		}

		// Append base class name based on format
		switch (nameFormat) {
			case FULL:
				// Full package name + outer classes
				if (nn(ct)) {
					sb.append(ct.getName());
					// Apply separator if not '$'
					if (separator != '$' && sb.indexOf("$") != -1) {
						for (int i = 0; i < sb.length(); i++) {
							if (sb.charAt(i) == '$')
								sb.setCharAt(i, separator);
						}
					}
				} else {
					sb.append(t.getTypeName());
				}
				break;

			case SHORT:
				// Outer classes but no package
				if (nn(ct)) {
					if (ct.isLocalClass()) {
						// Local class: include enclosing class simple name
						sb.append(of(ct.getEnclosingClass()).getNameSimple())
						  .append(separator)
						  .append(ct.getSimpleName());
					} else if (ct.isMemberClass()) {
						// Member class: include declaring class simple name
						sb.append(of(ct.getDeclaringClass()).getNameSimple())
						  .append(separator)
						  .append(ct.getSimpleName());
					} else {
						// Regular class: just simple name
						sb.append(ct.getSimpleName());
					}
				} else {
					sb.append(t.getTypeName());
				}
				break;

			default /* SIMPLE */:
				// Simple name only - no package, no outer classes
				if (nn(ct)) {
					sb.append(ct.getSimpleName());
				} else {
					sb.append(t.getTypeName());
				}
				break;
		}

		// Append type parameters if requested
		if (includeTypeParams && isParameterizedType) {
			var pt = (ParameterizedType)t;
			sb.append('<');
			var first = true;
			for (var t2 : pt.getActualTypeArguments()) {
				if (!first)
					sb.append(',');
				first = false;
				of(t2).appendNameFormatted(sb, nameFormat, includeTypeParams, separator, arrayFormat);
			}
			sb.append('>');
		}

		return sb;
	}

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
		var members = c.getNestMembers();
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
	 * Returns a cached, unmodifiable list of record components.
	 * The components are returned in the same order as they appear in the record declaration.
	 * If this class is not a record, returns an empty list.
	 *
	 * @return An unmodifiable list of record components, or an empty list if this class is not a record.
	 */
	public List<RecordComponent> getRecordComponents() {
		return recordComponents.get();
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
	 * Returns a cached, unmodifiable list.
	 * If a superinterface is a parameterized type, the {@link Type} returned for it reflects the actual
	 * type parameters used in the source code.
	 *
	 * @return
	 * 	An unmodifiable list of {@link Type}s representing the interfaces directly implemented by this class.
	 * 	<br>Returns an empty list if this class implements no interfaces.
	 */
	public List<Type> getGenericInterfaces() {
		return genericInterfaces.get();
	}

	/**
	 * Returns a list of {@link TypeVariable} objects that represent the type variables declared by this class.
	 *
	 * <p>
	 * Returns a cached, unmodifiable list.
	 * The type variables are returned in the same order as they appear in the class declaration.
	 *
	 * @return
	 * 	An unmodifiable list of {@link TypeVariable} objects representing the type parameters of this class.
	 * 	<br>Returns an empty list if this class declares no type parameters.
	 */
	public List<TypeVariable<?>> getTypeParameters() {
		return typeParameters.get();
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
	 * Returns a list of {@link AnnotatedType} objects that represent the annotated interfaces
	 * implemented by this class.
	 *
	 * <p>
	 * Returns a cached, unmodifiable list.
	 * If this class represents a class or interface whose superinterfaces are annotated,
	 * the returned objects reflect the annotations used in the source code to declare the superinterfaces.
	 *
	 * @return
	 * 	An unmodifiable list of {@link AnnotatedType} objects representing the annotated superinterfaces.
	 * 	<br>Returns an empty list if this class implements no interfaces.
	 */
	public List<AnnotatedType> getAnnotatedInterfaces() {
		return annotatedInterfaces.get();
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
	 * <p>
	 * Returns a cached, unmodifiable list.
	 *
	 * @return An unmodifiable list of signers, or an empty list if there are no signers.
	 */
	public List<Object> getSigners() {
		return signers.get();
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
		return getAnnotationInfos(type).findFirst().isPresent();
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
	 * Returns <jk>true</jk> if all specified flags are applicable to this class.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this class.
	 */
	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case ANNOTATION -> isAnnotation();
			case NOT_ANNOTATION -> !isAnnotation();
			case ANONYMOUS -> isAnonymousClass();
			case NOT_ANONYMOUS -> !isAnonymousClass();
			case ARRAY -> isArray();
			case NOT_ARRAY -> !isArray();
			case CLASS -> !isInterface();
			case DEPRECATED -> isDeprecated();
			case NOT_DEPRECATED -> isNotDeprecated();
			case ENUM -> isEnum();
			case NOT_ENUM -> !isEnum();
			case LOCAL -> isLocalClass();
			case NOT_LOCAL -> !isLocalClass();
			case MEMBER -> isMemberClass();
			case NOT_MEMBER -> isNotMemberClass();
			case NON_STATIC_MEMBER -> isNonStaticMemberClass();
			case NOT_NON_STATIC_MEMBER -> !isNonStaticMemberClass();
			case PRIMITIVE -> isPrimitive();
			case NOT_PRIMITIVE -> !isPrimitive();
			case RECORD -> isRecord();
			case NOT_RECORD -> !isRecord();
			case SEALED -> isSealed();
			case NOT_SEALED -> !isSealed();
			case SYNTHETIC -> isSynthetic();
			case NOT_SYNTHETIC -> !isSynthetic();
			default -> super.is(flag);
		};
	}

	@Override
	public boolean isAll(ElementFlag...flags) {
		return stream(flags).allMatch(this::is);
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

	@Override
	public boolean isAny(ElementFlag...flags) {
		return stream(flags).anyMatch(this::is);
	}

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
	 * 		ClassInfo.<jsm>of</jsm>(String.<jk>class</jk>).isParentOfLenient(String.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(CharSequence.<jk>class</jk>).isParentOfLenient(String.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(String.<jk>class</jk>).isParentOfLenient(CharSequence.<jk>class</jk>);  <jc>// false</jc>
	 * 		ClassInfo.<jsm>of</jsm>(<jk>int</jk>.<jk>class</jk>).isParentOfLenient(Integer.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(Integer.<jk>class</jk>).isParentOfLenient(<jk>int</jk>.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(Number.<jk>class</jk>).isParentOfLenient(<jk>int</jk>.<jk>class</jk>);  <jc>// true</jc>
	 * 		ClassInfo.<jsm>of</jsm>(<jk>int</jk>.<jk>class</jk>).isParentOfLenient(Number.<jk>class</jk>);  <jc>// false</jc>
	 * 		ClassInfo.<jsm>of</jsm>(<jk>int</jk>.<jk>class</jk>).isParentOfLenient(<jk>long</jk>.<jk>class</jk>);  <jc>// false</jc>
	 * </p>
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOfLenient(Class<?> child) {
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
	 * Same as {@link #isParentOfLenient(Class)} but takes in a {@link ClassInfo}.
	 *
	 * @param child The child class.
	 * @return <jk>true</jk> if this class is a parent or the same as <c>child</c>.
	 */
	public boolean isParentOfLenient(ClassInfo child) {
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
	public boolean isParentOfLenient(Type child) {
		if (child instanceof Class)
			return isParentOfLenient((Class<?>)child);
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this is a primitive class.
	 *
	 * @return <jk>true</jk> if this is a primitive class.
	 */
	public boolean isPrimitive() { return nn(c) && c.isPrimitive(); }

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
		return getRepeatedAnnotationMethod() != null;
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
		x = annotationProvider.lastAnnotation(type, inner(), filter);
		if (nn(x) && test(filter, x))
			return x;
		var parents2 = parents.get();
		for (var parent : parents2) {
			x = annotationProvider.lastAnnotation(type, parent.inner(), filter);
			if (nn(x))
				return x;
		}
		var interfaces2 = interfaces.get();
		for (var element : interfaces2) {
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
	 * Shortcut for calling <c>Class.getDeclaredConstructor().newInstance()</c> on the underlying class.
	 *
	 * @return A new instance of the underlying class
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public Object newInstance() throws ExecutableException {
		if (c == null)
			throw new ExecutableException("Type ''{0}'' cannot be instantiated", getNameFull());
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
		for (var c2 : interfaces.get()) {
			t = c2.getAnnotation(ap, a);
			if (nn(t))
				return t;
		}
		return null;
	}

	/**
	 * Returns the first type parameter of this type if it's parameterized (e.g., T in Optional&lt;T&gt;).
	 *
	 * @param parameterizedType The expected parameterized class (e.g., Optional.class).
	 * @return The first type parameter, or <jk>null</jk> if not parameterized or no parameters exist.
	 */
	private Type getFirstParameterType(Class<?> parameterizedType) {
		if (t instanceof ParameterizedType pt) {
			var ta = pt.getActualTypeArguments();
			if (ta.length > 0)
				return ta[0];
		} else if (t instanceof Class<?> c) /* Class that extends Optional<T> */ {
			if (c != parameterizedType && parameterizedType.isAssignableFrom(c))
				return ClassInfo.of(c).getParameterType(0, parameterizedType);
		}
		return null;
	}

	/**
	 * Returns <jk>true</jk> if this type is a parameterized type of the specified class or a subclass of it.
	 *
	 * @param c The class to check against (e.g., Optional.class, List.class).
	 * @return <jk>true</jk> if this is Optional&lt;T&gt; or a subclass like MyOptional extends Optional&lt;String&gt;.
	 */
	private boolean isParameterizedTypeOf(Class<?> c) {
		return (t instanceof ParameterizedType t2 && t2.getRawType() == c) || (t instanceof Class && c.isAssignableFrom((Class<?>)t));
	}

	/**
	 * Returns all annotations declared directly on this class.
	 *
	 * <p>
	 * This includes annotations explicitly applied to the class declaration, but excludes inherited annotations
	 * from parent classes.
	 *
	 * @return
	 * 	An unmodifiable list of annotations declared directly on this class.
	 * 	<br>List is empty if no annotations are declared.
	 * 	<br>Results are in declaration order.
	 */
	public List<Annotation> getDeclaredAnnotations() {
		return declaredAnnotations.get();
	}

	/**
	 * Returns all annotations declared directly on this class, wrapped in {@link AnnotationInfo} objects.
	 *
	 * <p>
	 * This is equivalent to {@link #getDeclaredAnnotations()} but with each annotation wrapped for additional
	 * functionality such as annotation member access and metadata inspection.
	 *
	 * @return
	 * 	An unmodifiable list of {@link AnnotationInfo} wrappers for annotations declared directly on this class.
	 * 	<br>List is empty if no annotations are declared.
	 * 	<br>Results are in declaration order.
	 */
	public List<AnnotationInfo> getDeclaredAnnotationInfos() {
		return declaredAnnotations2.get();
	}

	ConstructorInfo getConstructorInfo(Constructor<?> x) {
		return constructorCache.get(x, () -> new ConstructorInfo(this, x));
	}

	FieldInfo getFieldInfo(Field x) {
		return fieldCache.get(x, () -> new FieldInfo(this, x));
	}

	MethodInfo getMethodInfo(Method x) {
		return methodCache.get(x, () -> new MethodInfo(this, x));
	}

	private List<ClassInfo> findParents() {
		List<ClassInfo> l = list();
		Class<?> pc = c;
		while (nn(pc) && pc != Object.class) {
			l.add(of(pc));
			pc = pc.getSuperclass();
		}
		return u(l);
	}

	private MethodInfo findRepeatedAnnotationMethod() {
		return getPublicMethods().stream()
			.filter(m -> m.hasName("value"))
			.filter(m -> m.getReturnType().isArray())
			.filter(m -> {
				var rct = m.getReturnType().getComponentType();
				if (rct.hasAnnotation(Repeatable.class)) {
					var r = rct.getAnnotationInfos(Repeatable.class).findFirst().map(AnnotationInfo::inner).orElse(null);
					return r != null && r.value().equals(c);
				}
				return false;
			})
			.findFirst()
			.orElse(null);
	}

	private int findDimensions() {
		int d = 0;
		Type ct = t;

		// Handle GenericArrayType (e.g., List<String>[])
		while (ct instanceof GenericArrayType gat) {
			d++;
			ct = gat.getGenericComponentType();
		}

		// Handle regular arrays
		Class<?> cc = c;
		while (nn(cc) && cc.isArray()) {
			d++;
			cc = cc.getComponentType();
		}

		return d;
	}

	private ClassInfo findComponentType() {
		Type ct = t;
		Class<?> cc = c;

		// Handle GenericArrayType (e.g., List<String>[])
		while (ct instanceof GenericArrayType gat) {
			ct = gat.getGenericComponentType();
		}

		// Handle regular arrays
		while (nn(cc) && cc.isArray()) {
			cc = cc.getComponentType();
		}

		// Return the deepest component type found
		if (ct != t) {
			return of(ct);
		} else if (cc != c) {
			return of(cc);
		} else {
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.CLASS_TYPE;
	}

	@Override /* Annotatable */
	public ClassInfo getClassInfo() {
		return this;
	}

	@Override /* Annotatable */
	public String getAnnotatableName() {
		return getNameSimple();
	}
}