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

import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;

/**
 * Lightweight wrapper around a {@link Package} object.
 *
 * <p>
 * Provides caching and convenient access to package metadata and annotations.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class PackageInfo implements Annotatable {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Cache<Package,PackageInfo> CACHE = Cache.of(Package.class, PackageInfo.class).build();

	/**
	 * Returns a package info wrapper around the specified package object.
	 *
	 * @param inner The package object.  Can be <jk>null</jk>.
	 * @return A package info wrapper, or <jk>null</jk> if the parameter was null.
	 */
	public static PackageInfo of(Package inner) {
		if (inner == null)
			return null;
		return CACHE.get(inner, () -> new PackageInfo(inner));
	}

	/**
	 * Returns a package info wrapper around the package of the specified class.
	 *
	 * @param c The class whose package to retrieve.  Can be <jk>null</jk>.
	 * @return A package info wrapper, or <jk>null</jk> if the parameter was null or has no package.
	 */
	public static PackageInfo of(Class<?> c) {
		return c == null ? null : of(c.getPackage());
	}

	/**
	 * Returns a package info wrapper around the package of the specified class info.
	 *
	 * @param ci The class info whose package to retrieve.  Can be <jk>null</jk>.
	 * @return A package info wrapper, or <jk>null</jk> if the parameter was null or has no package.
	 */
	public static PackageInfo of(ClassInfo ci) {
		return ci == null ? null : ci.getPackage();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private Package inner;  // Effectively final
	private final Supplier<List<AnnotationInfo<Annotation>>> annotations;  // All annotations on this package, wrapped in AnnotationInfo. Repeated annotations have been unwrapped and are present as individual instances.

	/**
	 * Constructor.
	 *
	 * @param p The package object.
	 */
	protected PackageInfo(Package p) {
		this.inner = p;
		this.annotations = memoize(() -> opt(inner).map(pkg -> stream(pkg.getAnnotations()).flatMap(a -> stream(splitRepeated(a))).map(a -> AnnotationInfo.of(this, a)).toList()).orElse(liste()));
	}

	/**
	 * Returns the wrapped {@link Package} object.
	 *
	 * @return The wrapped {@link Package} object.
	 */
	public Package inner() {
		return inner;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Package methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the name of this package.
	 *
	 * <p>
	 * Same as calling {@link Package#getName()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	PackageInfo <jv>pi</jv> = PackageInfo.<jsm>of</jsm>(String.<jk>class</jk>);
	 * 	String <jv>name</jv> = <jv>pi</jv>.getName();  <jc>// "java.lang"</jc>
	 * </p>
	 *
	 * @return The fully-qualified name of this package.
	 * @see Package#getName()
	 */
	public String getName() {
		return inner.getName();
	}

	/**
	 * Returns the title of the specification that this package implements.
	 *
	 * <p>
	 * Same as calling {@link Package#getSpecificationTitle()}.
	 *
	 * @return The specification title, or <jk>null</jk> if it is not known.
	 * @see Package#getSpecificationTitle()
	 */
	public String getSpecificationTitle() {
		return inner.getSpecificationTitle();
	}

	/**
	 * Returns the version number of the specification that this package implements.
	 *
	 * <p>
	 * Same as calling {@link Package#getSpecificationVersion()}.
	 *
	 * <p>
	 * This version string must be a sequence of nonnegative decimal integers separated by periods
	 * and may have leading zeros.
	 *
	 * @return The specification version, or <jk>null</jk> if it is not known.
	 * @see Package#getSpecificationVersion()
	 */
	public String getSpecificationVersion() {
		return inner.getSpecificationVersion();
	}

	/**
	 * Returns the name of the organization that maintains the specification implemented by this package.
	 *
	 * <p>
	 * Same as calling {@link Package#getSpecificationVendor()}.
	 *
	 * @return The specification vendor, or <jk>null</jk> if it is not known.
	 * @see Package#getSpecificationVendor()
	 */
	public String getSpecificationVendor() {
		return inner.getSpecificationVendor();
	}

	/**
	 * Returns the title of this package's implementation.
	 *
	 * <p>
	 * Same as calling {@link Package#getImplementationTitle()}.
	 *
	 * @return The implementation title, or <jk>null</jk> if it is not known.
	 * @see Package#getImplementationTitle()
	 */
	public String getImplementationTitle() {
		return inner.getImplementationTitle();
	}

	/**
	 * Returns the version of this package's implementation.
	 *
	 * <p>
	 * Same as calling {@link Package#getImplementationVersion()}.
	 *
	 * @return The implementation version, or <jk>null</jk> if it is not known.
	 * @see Package#getImplementationVersion()
	 */
	public String getImplementationVersion() {
		return inner.getImplementationVersion();
	}

	/**
	 * Returns the name of the organization that provided this package's implementation.
	 *
	 * <p>
	 * Same as calling {@link Package#getImplementationVendor()}.
	 *
	 * @return The implementation vendor, or <jk>null</jk> if it is not known.
	 * @see Package#getImplementationVendor()
	 */
	public String getImplementationVendor() {
		return inner.getImplementationVendor();
	}

	/**
	 * Returns <jk>true</jk> if this package is sealed.
	 *
	 * <p>
	 * Same as calling {@link Package#isSealed()}.
	 *
	 * @return <jk>true</jk> if this package is sealed.
	 * @see Package#isSealed()
	 */
	public boolean isSealed() {
		return inner.isSealed();
	}

	/**
	 * Returns <jk>true</jk> if this package is sealed with respect to the specified code source URL.
	 *
	 * <p>
	 * Same as calling {@link Package#isSealed(URL)}.
	 *
	 * @param url The code source URL.
	 * @return <jk>true</jk> if this package is sealed with respect to the specified URL.
	 * @see Package#isSealed(URL)
	 */
	public boolean isSealed(URL url) {
		return inner.isSealed(url);
	}

	/**
	 * Compares this package's specification version with a desired version.
	 *
	 * <p>
	 * Same as calling {@link Package#isCompatibleWith(String)}.
	 *
	 * <p>
	 * The version strings are compared by comparing each decimal integer in the version string.
	 *
	 * @param desired The desired version.
	 * @return <jk>true</jk> if this package's specification version is compatible with the desired version.
	 * @throws NumberFormatException If the desired or current version is not in the correct format.
	 * @see Package#isCompatibleWith(String)
	 */
	public boolean isCompatibleWith(String desired) throws NumberFormatException {
		return inner.isCompatibleWith(desired);
	}

	/**
	 * Returns this package's annotation for the specified type wrapped in an {@link AnnotationInfo}, else <jk>null</jk>.
	 *
	 * <p>
	 * Searches the memoized annotations list for the first matching annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if package has @Deprecated annotation</jc>
	 * 	PackageInfo <jv>pi</jv> = PackageInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
	 * 	AnnotationInfo&lt;Deprecated&gt; <jv>d</jv> = <jv>pi</jv>.getAnnotation(Deprecated.<jk>class</jk>);
	 * 	<jk>if</jk> (<jv>d</jv> != <jk>null</jk>) {
	 * 		<jc>// Package is deprecated</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return This package's annotation for the specified annotation type wrapped in AnnotationInfo, or <jk>null</jk> if not present.
	 */
	public <A extends Annotation> AnnotationInfo<A> getAnnotation(Class<A> annotationClass) {
		return getAnnotations(annotationClass)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns all annotations on this package, wrapped in {@link AnnotationInfo} objects.
	 *
	 * <p>
	 * Cached and unmodifiable.
	 * Repeated annotations (from {@code @Repeatable} containers) have been automatically unwrapped and are present as individual instances in the list.
	 *
	 * @return All annotations present on this package, or an empty list if there are none.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotations() {
		return annotations.get();
	}

	/**
	 * Returns this package's annotations of the specified type (including repeated annotations), wrapped in {@link AnnotationInfo}.
	 *
	 * <p>
	 * Filters the cached annotations list by type.
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this package's annotations of the specified type wrapped in AnnotationInfo, or an empty list if there are none.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotations(Class<A> annotationClass) {
		return getAnnotations().stream()
			.filter(ai -> annotationClass.isInstance(ai.inner()))
			.map(ai -> (AnnotationInfo<A>)ai);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Object methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the hash code of this package.
	 *
	 * <p>
	 * Same as calling {@link Package#hashCode()}.
	 *
	 * @return The hash code of this package.
	 */
	@Override /* Object */
	public int hashCode() {
		return inner.hashCode();
	}

	/**
	 * Returns <jk>true</jk> if the specified object is equal to this package.
	 *
	 * <p>
	 * Two packages are equal if they have the same name.
	 *
	 * @param obj The object to compare to.
	 * @return <jk>true</jk> if the specified object is equal to this package.
	 */
	@Override /* Object */
	public boolean equals(Object obj) {
		if (obj instanceof PackageInfo)
			return inner.equals(((PackageInfo)obj).inner);
		return inner.equals(obj);
	}

	/**
	 * Returns the string representation of this package.
	 *
	 * <p>
	 * Same as calling {@link Package#toString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	PackageInfo <jv>pi</jv> = PackageInfo.<jsm>of</jsm>(String.<jk>class</jk>);
	 * 	String <jv>s</jv> = <jv>pi</jv>.toString();  <jc>// "package java.lang"</jc>
	 * </p>
	 *
	 * @return The string representation of this package.
	 * @see Package#toString()
	 */
	@Override /* Object */
	public String toString() {
		return inner.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.PACKAGE_TYPE;
	}

	@Override /* Annotatable */
	public ClassInfo getClassInfo() {
		return null;  // Package has no declaring class
	}

	@Override /* Annotatable */
	public String getAnnotatableName() {
		return getName();
	}
}

