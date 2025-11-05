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
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;

/**
 * Enhanced annotation provider for classes that returns {@link AnnotationInfo} objects instead of raw {@link Annotation} objects.
 *
 * <p>
 * This class provides a modern API for retrieving class annotations with the following benefits:
 * <ul>
 * 	<li>Returns {@link AnnotationInfo} wrappers that provide additional methods and type safety
 * 	<li>Supports filtering by annotation type using streams
 * 	<li>Properly handles repeatable annotations
 * 	<li>Searches up the class hierarchy (class → parents → interfaces → package)
 * 	<li>Caches results for performance
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create with default settings</jc>
 * 	AnnotationProvider2 <jv>provider</jv> = AnnotationProvider2.<jsm>create</jsm>().build();
 *
 * 	<jc>// Create with caching disabled</jc>
 * 	AnnotationProvider2 <jv>provider</jv> = AnnotationProvider2
 * 		.<jsm>create</jsm>()
 * 		.disableCaching()
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link AnnotationProvider}
 * 	<li class='jc'>{@link AnnotationInfo}
 * </ul>
 */
public class AnnotationProvider2 {

	/**
	 * Disable annotation caching.
	 */
	private static final boolean DISABLE_ANNOTATION_CACHING = Boolean.getBoolean("juneau.disableAnnotationCaching");

	/**
	 * Default instance.
	 */
	public static final AnnotationProvider2 INSTANCE = new AnnotationProvider2(create());

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder for creating configured {@link AnnotationProvider2} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	AnnotationProvider2 <jv>provider</jv> = AnnotationProvider2
	 * 		.<jsm>create</jsm>()
	 * 		.disableCaching()
	 * 		.build();
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link AnnotationProvider2#create()}
	 * </ul>
	 */
	public static class Builder {
		boolean disableCaching;
		ReflectionMap2.Builder<Annotation> runtimeAnnotations = ReflectionMap2.create(Annotation.class);

		Builder() {
			disableCaching = DISABLE_ANNOTATION_CACHING;
		}

		/**
		 * Builds a new {@link AnnotationProvider2} instance with the configured settings.
		 *
		 * @return A new immutable {@link AnnotationProvider2} instance.
		 */
		public AnnotationProvider2 build() {
			return new AnnotationProvider2(this);
		}

		/**
		 * Disables annotation caching entirely.
		 *
		 * <p>
		 * When disabled, annotation lookups will always perform fresh searches without caching results.
		 *
		 * @return This object for method chaining.
		 */
		public Builder disableCaching() {
			disableCaching = true;
			return this;
		}

		/**
		 * Conditionally disables or enables annotation caching.
		 *
		 * @param value Whether to disable caching.
		 * @return This object for method chaining.
		 */
		public Builder disableCaching(boolean value) {
			disableCaching = value;
			return this;
		}

		/**
		 * Adds runtime annotations to be applied to classes and methods.
		 *
		 * <p>
		 * Annotations must define either an {@code onClass()} method that returns a {@code Class[]} array,
		 * or an {@code on()} method that returns a {@code String[]} array to specify the targets.
		 *
		 * @param annotations The annotations to add.
		 * @return This object for method chaining.
		 * @throws BeanRuntimeException If the annotations are invalid.
		 */
		public Builder addRuntimeAnnotations(List<Annotation> annotations) {

			for (var a : annotations) {
				try {
					var ci = ClassInfo.of(a.getClass());

					MethodInfo mi = ci.getPublicMethod(x -> x.hasName("onClass"));
					if (nn(mi)) {
						if (! mi.getReturnType().is(Class[].class))
							throw new BeanRuntimeException("Invalid annotation @{0} used in runtime annotations.  Annotation must define an onClass() method that returns a Class array.", scn(a));
						for (var c : (Class<?>[])mi.accessible().invoke(a))
							runtimeAnnotations.append(c.getName(), a);
					}

					mi = ci.getPublicMethod(x -> x.hasName("on"));
					if (nn(mi)) {
						if (! mi.getReturnType().is(String[].class))
							throw new BeanRuntimeException("Invalid annotation @{0} used in runtime annotations.  Annotation must define an on() method that returns a String array.", scn(a));
						for (var s : (String[])mi.accessible().invoke(a))
							runtimeAnnotations.append(s, a);
					}

				} catch (BeanRuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new BeanRuntimeException(e, null, "Invalid annotation @{0} used in runtime annotations.", cn(a));
				}
			}
			return this;
		}

		public Builder addRuntimeAnnotations(Annotation...annotations) {
			return addRuntimeAnnotations(l(annotations));
		}
	}

	/**
	 * Creates a new {@link Builder} for constructing an annotation provider.
	 *
	 * @return A new builder for configuring the annotation provider.
	 */
	public static Builder create() {
		return new Builder();
	}

	// @formatter:off
	private final Cache<Class<?>,List<AnnotationInfo<Annotation>>> classAnnotations;
	private final Cache<Class<?>,List<AnnotationInfo<Annotation>>> classDeclaredAnnotations;
	private final Cache<Method,List<AnnotationInfo<Annotation>>> methodAnnotations;
	private final Cache<Field,List<AnnotationInfo<Annotation>>> fieldAnnotations;
	private final Cache<Constructor<?>,List<AnnotationInfo<Annotation>>> constructorAnnotations;
	private final ReflectionMap2<Annotation> runtimeAnnotations;
	// @formatter:on

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected AnnotationProvider2(Builder builder) {
		this.classAnnotations = Cache.<Class<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findClassAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.classDeclaredAnnotations = Cache.<Class<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findClassDeclaredAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.methodAnnotations = Cache.<Method,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findMethodAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.fieldAnnotations = Cache.<Field,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findFieldAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.constructorAnnotations = Cache.<Constructor<?>,List<AnnotationInfo<Annotation>>>create()
			.supplier(this::findConstructorAnnotations)
			.disableCaching(builder.disableCaching)
			.build();
		this.runtimeAnnotations = builder.runtimeAnnotations.build();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Public API
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds all annotations on the specified class.
	 *
	 * <p>
	 * Returns annotations in child-to-parent order.
	 *
	 * @param onClass The class to search on.
	 * @return A list of {@link AnnotationInfo} objects representing annotations on the specified class and its parents.
	 * 	Never <jk>null</jk>.
	 */
	public List<AnnotationInfo<Annotation>> find(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		return classAnnotations.get(onClass);
	}

	/**
	 * Finds all annotations of the specified type on the specified class.
	 *
	 * <p>
	 * Returns annotations in child-to-parent order.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects representing annotations of the specified type on the specified class and its parents.
	 * 	Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return find(onClass).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	public List<AnnotationInfo<Annotation>> findDeclared(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		return classDeclaredAnnotations.get(onClass);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> findDeclared(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return findDeclared(onClass).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	/**
	 * Finds all declared annotations on the specified class in parent-to-child order (reversed).
	 *
	 * <p>
	 * This method returns annotations in the opposite order from {@link #findDeclared(Class)}.
	 * It processes parent/declared annotations first (lower priority), then runtime annotations (higher priority).
	 * This is useful when you want to process multiple annotation values where child annotations
	 * can override values from parent annotations.
	 *
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects in parent-to-child order.
	 */
	public Stream<AnnotationInfo<Annotation>> findDeclaredParentFirst(Class<?> onClass) {
		assertArgNotNull("onClass", onClass);
		var list = classDeclaredAnnotations.get(onClass);
		// Iterate backwards to get parent-to-child order
		return java.util.stream.IntStream.range(0, list.size())
			.map(i -> list.size() - 1 - i)
			.mapToObj(list::get);
	}

	/**
	 * Finds all declared annotations of the specified type on the specified class in parent-to-child order (reversed).
	 *
	 * <p>
	 * This method returns annotations in the opposite order from {@link #findDeclared(Class, Class)}.
	 *
	 * @param <A> The annotation type to find.
	 * @param type The annotation type to find.
	 * @param onClass The class to search on.
	 * @return A stream of {@link AnnotationInfo} objects in parent-to-child order.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> findDeclaredParentFirst(Class<A> type, Class<?> onClass) {
		assertArgNotNull("type", type);
		assertArgNotNull("onClass", onClass);
		return findDeclaredParentFirst(onClass)
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	public List<AnnotationInfo<Annotation>> find(Method onMethod) {
		assertArgNotNull("onMethod", onMethod);
		return methodAnnotations.get(onMethod);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Method onMethod) {
		assertArgNotNull("type", type);
		assertArgNotNull("onMethod", onMethod);
		return find(onMethod).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	public List<AnnotationInfo<Annotation>> find(Field onField) {
		assertArgNotNull("onField", onField);
		return fieldAnnotations.get(onField);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Field onField) {
		assertArgNotNull("type", type);
		assertArgNotNull("onField", onField);
		return find(onField).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	public List<AnnotationInfo<Annotation>> find(Constructor<?> onConstructor) {
		assertArgNotNull("onConstructor", onConstructor);
		return constructorAnnotations.get(onConstructor);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> find(Class<A> type, Constructor<?> onConstructor) {
		assertArgNotNull("type", type);
		assertArgNotNull("onConstructor", onConstructor);
		return find(onConstructor).stream()
			.filter(a -> a.isType(type))
			.map(a -> (AnnotationInfo<A>)a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Private implementation
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Finds all annotations on the specified class in child-to-parent order.
	 *
	 * <p>
	 * Annotations are appended in the following order:
	 * <ol>
	 * 	<li>On this class.
	 * 	<li>On parent classes ordered child-to-parent.
	 * 	<li>On interfaces ordered child-to-parent.
	 * 	<li>On the package of this class.
	 * </ol>
	 *
	 * @param forClass The class to find annotations on.
	 * @return A list of {@link AnnotationInfo} objects in child-to-parent order.
	 */
	private List<AnnotationInfo<Annotation>> findClassAnnotations(Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		// On all parent classes and interfaces (properly traversed to avoid duplicates)
		var parentsAndInterfaces = ci.getParentsAndInterfaces();
		for (int i = 0; i < parentsAndInterfaces.size(); i++)
			findDeclaredAnnotations(list, parentsAndInterfaces.get(i).inner());

		// On the package of this class
		var pkg = ci.getPackage();
		if (nn(pkg)) {
			var pi = PackageInfo.of(pkg.inner());
			for (var a : pkg.inner().getAnnotations())
				for (var a2 : splitRepeated(a))
					list.add(AnnotationInfo.of(pi, a2));
		}

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findClassDeclaredAnnotations(Class<?> forClass) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		// On this class
		findDeclaredAnnotations(list, forClass);

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findMethodAnnotations(Method forMethod) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		MethodInfo.of(forMethod).getMatchingMethods().forEach(m -> {
			runtimeAnnotations.findMatching(m.inner()).forEach(a -> list.add(AnnotationInfo.of(m, a)));
			list.addAll(m.getDeclaredAnnotationInfos());
		});

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findFieldAnnotations(Field forField) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		FieldInfo fi = FieldInfo.of(forField);
		runtimeAnnotations.findMatching(forField).forEach(a -> list.add(AnnotationInfo.of(fi, a)));
		list.addAll(fi.getAnnotationInfos());

		return u(list);
	}

	private List<AnnotationInfo<Annotation>> findConstructorAnnotations(Constructor<?> forConstructor) {
		var list = new ArrayList<AnnotationInfo<Annotation>>();

		ConstructorInfo ci = ConstructorInfo.of(forConstructor);
		runtimeAnnotations.findMatching(forConstructor).forEach(a -> list.add(AnnotationInfo.of(ci, a)));
		list.addAll(ci.getDeclaredAnnotationInfos());

		return u(list);
	}

	/**
	 * Finds all declared annotations on the specified class and appends them to the list.
	 *
	 * @param appendTo The list to append to.
	 * @param forClass The class to find declared annotations on.
	 */
	private void findDeclaredAnnotations(List<AnnotationInfo<Annotation>> appendTo, Class<?> forClass) {
		var ci = ClassInfo.of(forClass);
		runtimeAnnotations.findMatching(forClass).forEach(x -> appendTo.add(AnnotationInfo.of(ClassInfo.of(forClass), x)));
		for (var a : forClass.getDeclaredAnnotations())
			for (var a2 : splitRepeated(a))
				appendTo.add(AnnotationInfo.of(ci, a2));
	}

	/**
	 * Finds all annotations on the specified package and appends them to the list.
	 *
	 * @param appendTo The list to append to.
	 * @param forPackage The package to find annotations on.
	 */
	private void findDeclaredAnnotations(List<AnnotationInfo<Annotation>> appendTo, Package forPackage) {
		var pi = PackageInfo.of(forPackage);
		for (var a : forPackage.getAnnotations())
			for (var a2 : splitRepeated(a))
				appendTo.add(AnnotationInfo.of(pi, a2));
	}
}