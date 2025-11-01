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

import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.net.*;

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
public class PackageInfo {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Cache<Package,PackageInfo> CACHE = Cache.of(Package.class, PackageInfo.class).build();

	/**
	 * Returns a package info wrapper around the specified package object.
	 *
	 * @param p The package object.  Can be <jk>null</jk>.
	 * @return A package info wrapper, or <jk>null</jk> if the parameter was null.
	 */
	public static PackageInfo of(Package p) {
		if (p == null)
			return null;
		return CACHE.get(p, () -> new PackageInfo(p));
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

	private final Package p;

	/**
	 * Constructor.
	 *
	 * @param p The package object.
	 */
	protected PackageInfo(Package p) {
		this.p = p;
	}

	/**
	 * Returns the wrapped {@link Package} object.
	 *
	 * @return The wrapped {@link Package} object.
	 */
	public Package inner() {
		return p;
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
		return p.getName();
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
		return p.getSpecificationTitle();
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
		return p.getSpecificationVersion();
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
		return p.getSpecificationVendor();
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
		return p.getImplementationTitle();
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
		return p.getImplementationVersion();
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
		return p.getImplementationVendor();
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
		return p.isSealed();
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
		return p.isSealed(url);
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
		return p.isCompatibleWith(desired);
	}

	/**
	 * Returns this package's annotation for the specified type if such an annotation is <em>present</em>, else <jk>null</jk>.
	 *
	 * <p>
	 * Same as calling {@link Package#getAnnotation(Class)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if package has @Deprecated annotation</jc>
	 * 	PackageInfo <jv>pi</jv> = PackageInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
	 * 	Deprecated <jv>d</jv> = <jv>pi</jv>.getAnnotation(Deprecated.<jk>class</jk>);
	 * 	<jk>if</jk> (<jv>d</jv> != <jk>null</jk>) {
	 * 		<jc>// Package is deprecated</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return This package's annotation for the specified annotation type if present, else <jk>null</jk>.
	 * @see Package#getAnnotation(Class)
	 */
	public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
		return p.getAnnotation(annotationClass);
	}

	/**
	 * Returns annotations that are <em>present</em> on this package.
	 *
	 * <p>
	 * Same as calling {@link Package#getAnnotations()}.
	 *
	 * @return Annotations present on this package, or an empty array if there are none.
	 * @see Package#getAnnotations()
	 */
	public Annotation[] getAnnotations() {
		return p.getAnnotations();
	}

	/**
	 * Returns annotations that are <em>directly present</em> on this package (not inherited).
	 *
	 * <p>
	 * Same as calling {@link Package#getDeclaredAnnotations()}.
	 *
	 * @return Annotations directly present on this package, or an empty array if there are none.
	 * @see Package#getDeclaredAnnotations()
	 */
	public Annotation[] getDeclaredAnnotations() {
		return p.getDeclaredAnnotations();
	}

	/**
	 * Returns this package's annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Same as calling {@link Package#getAnnotationsByType(Class)}.
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this package's annotations of the specified type, or an empty array if there are none.
	 * @see Package#getAnnotationsByType(Class)
	 */
	public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
		return p.getAnnotationsByType(annotationClass);
	}

	/**
	 * Returns this package's declared annotations of the specified type (including repeated annotations).
	 *
	 * <p>
	 * Same as calling {@link Package#getDeclaredAnnotationsByType(Class)}.
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The Class object corresponding to the annotation type.
	 * @return All this package's declared annotations of the specified type, or an empty array if there are none.
	 * @see Package#getDeclaredAnnotationsByType(Class)
	 */
	public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
		return p.getDeclaredAnnotationsByType(annotationClass);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Juneau-specific methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this package has the specified annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if package is deprecated</jc>
	 * 	<jk>if</jk> (PackageInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).hasAnnotation(Deprecated.<jk>class</jk>)) {
	 * 		<jc>// Package is deprecated</jc>
	 * 	}
	 * </p>
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation to check for.
	 * @return <jk>true</jk> if this package has the specified annotation.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return nn(p.getAnnotation(type));
	}

	/**
	 * Returns <jk>true</jk> if this package does not have the specified annotation.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation to check for.
	 * @return <jk>true</jk> if this package does not have the specified annotation.
	 */
	public <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return ! hasAnnotation(type);
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
		return p.hashCode();
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
			return p.equals(((PackageInfo)obj).p);
		return p.equals(obj);
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
		return p.toString();
	}
}

