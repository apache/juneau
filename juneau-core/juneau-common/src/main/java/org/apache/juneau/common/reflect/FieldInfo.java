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

import static org.apache.juneau.common.reflect.ClassArrayFormat.*;
import static org.apache.juneau.common.reflect.ClassNameFormat.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;

/**
 * Lightweight utility class for introspecting information about a field.
 *
 * <p>
 * Extends {@link AccessibleInfo} to provide {@link AccessibleObject} functionality.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class FieldInfo extends AccessibleInfo implements Comparable<FieldInfo>, Annotatable {
	/**
	 * Convenience method for instantiating a {@link FieldInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param f The field being wrapped.
	 * @return A new {@link FieldInfo} object, or <jk>null</jk> if the field was null.
	 */
	public static FieldInfo of(ClassInfo declaringClass, Field f) {
		if (f == null)
			return null;
		return ClassInfo.of(declaringClass).getFieldInfo(f);
	}

	/**
	 * Convenience method for instantiating a {@link FieldInfo};
	 *
	 * @param f The field being wrapped.
	 * @return A new {@link FieldInfo} object, or <jk>null</jk> if the field was null.
	 */
	public static FieldInfo of(Field f) {
		if (f == null)
			return null;
		return ClassInfo.of(f.getDeclaringClass()).getFieldInfo(f);
	}

	Field f;  // Effectively final
	private final ClassInfo declaringClass;
	private final Supplier<ClassInfo> typeCache = memoize(() -> ClassInfo.of(f.getType()));
	private final Supplier<List<AnnotationInfo<Annotation>>> annotations = memoize(this::_findAnnotations);
	private final Supplier<String> fullName = memoize(this::findFullName);

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param f The field being wrapped.
	 */
	protected FieldInfo(ClassInfo declaringClass, Field f) {
		super(f, f.getModifiers());
		this.declaringClass = declaringClass;
		this.f = f;
	}

	private List<AnnotationInfo<Annotation>> _findAnnotations() {
		return stream(f.getAnnotations()).map(a -> AnnotationInfo.of(this, a)).toList();
	}

	private String findFullName() {
		var sb = new StringBuilder(128);
		var dc = declaringClass;
		var pi = dc.getPackage();
		if (nn(pi))
			sb.append(pi.getName()).append('.');
		dc.appendNameFormatted(sb, SHORT, true, '$', BRACKETS);
		sb.append('.').append(getName());
		return sb.toString();
	}

	/**
	 * Returns all annotations declared on this field.
	 *
	 * @return An unmodifiable list of all annotations declared on this field.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotationInfos() {
		return annotations.get();
	}

	/**
	 * Returns all annotations of the specified type declared on this field.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return A stream of all matching annotations.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotationInfos(Class<A> type) {
		return annotations.get().stream()
			.filter(x -> type.isInstance(x.inner()))
			.map(x -> (AnnotationInfo<A>)x);
	}

	/**
	 * Returns the first annotation of the specified type declared on this field.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return An optional containing the first matching annotation, or empty if not found.
	 */
	public <A extends Annotation> Optional<AnnotationInfo<A>> getAnnotationInfo(Class<A> type) {
		return getAnnotationInfos(type).findFirst();
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return This object.
	 */
	public FieldInfo accessible() {
		setAccessible();
		return this;
	}

	@Override
	public int compareTo(FieldInfo o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * Returns the field value on the specified object.
	 *
	 * @param o The object containing the field.
	 * @param <T> The object type to retrieve.
	 * @return The field value.
	 * @throws BeanRuntimeException Field was not accessible or field does not belong to object.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Object o) throws BeanRuntimeException {
		try {
			f.setAccessible(true);
			return (T)f.get(o);
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Returns metadata about the declaring class.
	 *
	 * @return Metadata about the declaring class.
	 */
	public ClassInfo getDeclaringClass() { return declaringClass; }

	/**
	 * Returns the full name of this field.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"com.foo.MyClass.myField"</js> - Method.
	 * </ul>
	 *
	 * @return The underlying executable name.
	 */
	public String getFullName() {
		return fullName.get();
	}

	/**
	 * Returns the name of this field.
	 *
	 * @return The name of this field.
	 */
	public String getName() { return f.getName(); }

	/**
	 * Same as {@link #get(Object)} but wraps the results in an {@link Optional}.
	 *
	 * @param o The object containing the field.
	 * @param <T> The object type to retrieve.
	 * @return The field value.
	 * @throws BeanRuntimeException Field was not accessible or field does not belong to object.
	 */
	public <T> Optional<T> getOptional(Object o) throws BeanRuntimeException {
		return Optional.ofNullable(get(o));
	}

	/**
	 * Returns the type of this field.
	 *
	 * @return The type of this field.
	 */
	public ClassInfo getType() {
		return typeCache.get();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present.
	 */
	public <A extends Annotation> boolean hasAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		return nn(annotationProvider.firstAnnotation(type, f, x -> true));
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return f.isAnnotationPresent(type);
	}

	/**
	 * Returns <jk>true</jk> if the field has the specified name.
	 *
	 * @param name The name to compare against.
	 * @return <jk>true</jk> if the field has the specified name.
	 */
	public boolean hasName(String name) {
		return f.getName().equals(name);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is not present.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is not present.
	 */
	public <A extends Annotation> boolean hasNoAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		return ! hasAnnotation(annotationProvider, type);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is not present on this field.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is not present on this field.
	 */
	public <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return ! hasAnnotation(type);
	}

	/**
	 * Returns the wrapped field.
	 *
	 * @return The wrapped field.
	 */
	public Field inner() {
		return f;
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this field.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this field.
	 */
	@Override
	public boolean is(ElementFlag flag) {
		return switch (flag) {
			case DEPRECATED -> isDeprecated();
			case NOT_DEPRECATED -> isNotDeprecated();
			case ENUM_CONSTANT -> isEnumConstant();
			case NOT_ENUM_CONSTANT -> !isEnumConstant();
			case SYNTHETIC -> isSynthetic();
			case NOT_SYNTHETIC -> !isSynthetic();
			default -> super.is(flag);
		};
	}

	@Override
	public boolean isAll(ElementFlag...flags) {
		return stream(flags).allMatch(this::is);
	}

	@Override
	public boolean isAny(ElementFlag...flags) {
		return stream(flags).anyMatch(this::is);
	}

	/**
	 * Returns <jk>true</jk> if this field has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this field has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isDeprecated() { return f.isAnnotationPresent(Deprecated.class); }

	/**
	 * Returns <jk>true</jk> if this field doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this field doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() { return ! f.isAnnotationPresent(Deprecated.class); }

	/**
	 * Identifies if the specified visibility matches this field.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this field.
	 */
	public boolean isVisible(Visibility v) {
		return v.isVisible(f);
	}

	/**
	 * Sets the field value on the specified object.
	 *
	 * @param o The object containing the field.
	 * @param value The new field value.
	 * @throws BeanRuntimeException Field was not accessible or field does not belong to object.
	 */
	public void set(Object o, Object value) throws BeanRuntimeException {
		try {
			f.setAccessible(true);
			f.set(o, value);
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Sets the field value on the specified object if the value is <jk>null</jk>.
	 *
	 * @param o The object containing the field.
	 * @param value The new field value.
	 * @throws BeanRuntimeException Field was not accessible or field does not belong to object.
	 */
	public void setIfNull(Object o, Object value) {
		Object v = get(o);
		if (v == null)
			set(o, value);
	}

	/**
	 * Returns <jk>true</jk> if this field is a synthetic field as defined by the Java Language Specification.
	 *
	 * <p>
	 * Same as calling {@link Field#isSynthetic()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Filter out compiler-generated fields</jc>
	 * 	FieldInfo <jv>fi</jv> = ...;
	 * 	<jk>if</jk> (! <jv>fi</jv>.isSynthetic()) {
	 * 		<jc>// Process real field</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this field is a synthetic field.
	 * @see Field#isSynthetic()
	 */
	public boolean isSynthetic() {
		return f.isSynthetic();
	}

	/**
	 * Returns <jk>true</jk> if this field represents an element of an enumerated type.
	 *
	 * <p>
	 * Same as calling {@link Field#isEnumConstant()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if field is an enum constant</jc>
	 * 	FieldInfo <jv>fi</jv> = ClassInfo.<jsm>of</jsm>(MyEnum.<jk>class</jk>).getField(<js>"VALUE1"</js>);
	 * 	<jk>if</jk> (<jv>fi</jv>.isEnumConstant()) {
	 * 		<jc>// Handle enum constant</jc>
	 * 	}
	 * </p>
	 *
	 * @return <jk>true</jk> if this field represents an enum constant.
	 * @see Field#isEnumConstant()
	 */
	public boolean isEnumConstant() {
		return f.isEnumConstant();
	}

	/**
	 * Returns an {@link AnnotatedType} object that represents the use of a type to specify the declared type of the field.
	 *
	 * <p>
	 * Same as calling {@link Field#getAnnotatedType()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get annotated type: @NotNull String name</jc>
	 * 	FieldInfo <jv>fi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getField(<js>"name"</js>);
	 * 	AnnotatedType <jv>aType</jv> = <jv>fi</jv>.getAnnotatedType();
	 * 	<jc>// Check for @NotNull on the type</jc>
	 * </p>
	 *
	 * @return An {@link AnnotatedType} object representing the declared type.
	 * @see Field#getAnnotatedType()
	 */
	public AnnotatedType getAnnotatedType() {
		return f.getAnnotatedType();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Field-Specific Methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a string describing this field, including its generic type.
	 *
	 * <p>
	 * Same as calling {@link Field#toGenericString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get generic string for: public static final List&lt;String&gt; VALUES</jc>
	 * 	FieldInfo <jv>fi</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>).getField(<js>"VALUES"</js>);
	 * 	String <jv>str</jv> = <jv>fi</jv>.toGenericString();
	 * 	<jc>// Returns: "public static final java.util.List&lt;java.lang.String&gt; com.example.MyClass.VALUES"</jc>
	 * </p>
	 *
	 * @return A string describing this field.
	 * @see Field#toGenericString()
	 */
	public String toGenericString() {
		return f.toGenericString();
	}

	@Override
	public String toString() {
		return cn(f.getDeclaringClass()) + "." + f.getName();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() {
		return AnnotatableType.FIELD_TYPE;
	}

	@Override /* Annotatable */
	public ClassInfo getClassInfo() {
		return getDeclaringClass();
	}

	@Override /* Annotatable */
	public String getAnnotatableName() {
		return getName();
	}
}