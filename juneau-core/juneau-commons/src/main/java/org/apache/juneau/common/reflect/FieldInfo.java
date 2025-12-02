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
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.utils.*;

/**
 * Lightweight utility class for introspecting information about a Java field.
 *
 * <p>
 * This class provides a convenient wrapper around {@link Field} that extends the standard Java reflection
 * API with additional functionality for field introspection, annotation handling, and value access.
 * It extends {@link AccessibleInfo} to provide {@link AccessibleObject} functionality for accessing
 * private fields.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Field introspection - access field metadata, type, modifiers
 * 	<li>Annotation support - get annotations declared on the field
 * 	<li>Value access - get and set field values with type safety
 * 	<li>Accessibility control - make private fields accessible
 * 	<li>Thread-safe - instances are immutable and safe for concurrent access
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Introspecting field metadata for code generation or analysis
 * 	<li>Accessing field values in beans or data objects
 * 	<li>Finding annotations on fields
 * 	<li>Working with field types and modifiers
 * 	<li>Building frameworks that need to analyze or manipulate field values
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Get FieldInfo from a class</jc>
 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
 * 	FieldInfo <jv>field</jv> = <jv>ci</jv>.getField(<js>"myField"</js>);
 *
 * 	<jc>// Get field type</jc>
 * 	ClassInfo <jv>type</jv> = <jv>field</jv>.getType();
 *
 * 	<jc>// Get annotations</jc>
 * 	List&lt;AnnotationInfo&lt;MyAnnotation&gt;&gt; <jv>annotations</jv> =
 * 		<jv>field</jv>.getAnnotations(MyAnnotation.<jk>class</jk>).toList();
 *
 * 	<jc>// Access field value</jc>
 * 	MyClass <jv>obj</jv> = <jk>new</jk> MyClass();
 * 	<jv>field</jv>.accessible();  <jc>// Make accessible if private</jc>
 * 	Object <jv>value</jv> = <jv>field</jv>.get(<jv>obj</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ClassInfo} - Class introspection
 * 	<li class='jc'>{@link MethodInfo} - Method introspection
 * 	<li class='jc'>{@link ConstructorInfo} - Constructor introspection
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonReflect">juneau-common-reflect</a>
 * </ul>
 */
public class FieldInfo extends AccessibleInfo implements Comparable<FieldInfo>, Annotatable {
	/**
	 * Creates a FieldInfo wrapper for the specified field.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ClassInfo <jv>ci</jv> = ClassInfo.<jsm>of</jsm>(MyClass.<jk>class</jk>);
	 * 	Field <jv>f</jv> = MyClass.<jk>class</jk>.getField(<js>"myField"</js>);
	 * 	FieldInfo <jv>fi</jv> = FieldInfo.<jsm>of</jsm>(<jv>ci</jv>, <jv>f</jv>);
	 * </p>
	 *
	 * @param declaringClass The ClassInfo for the class that declares this field. Must not be <jk>null</jk>.
	 * @param inner The field being wrapped. Must not be <jk>null</jk>.
	 * @return A new FieldInfo object wrapping the field.
	 */
	public static FieldInfo of(ClassInfo declaringClass, Field inner) {
		assertArgNotNull("declaringClass", declaringClass);
		return declaringClass.getField(inner);
	}

	/**
	 * Creates a FieldInfo wrapper for the specified field.
	 *
	 * <p>
	 * This convenience method automatically determines the declaring class from the field.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Field <jv>f</jv> = MyClass.<jk>class</jk>.getField(<js>"myField"</js>);
	 * 	FieldInfo <jv>fi</jv> = FieldInfo.<jsm>of</jsm>(<jv>f</jv>);
	 * </p>
	 *
	 * @param inner The field being wrapped. Must not be <jk>null</jk>.
	 * @return A new FieldInfo object wrapping the field.
	 */
	public static FieldInfo of(Field inner) {
		assertArgNotNull("inner", inner);
		return ClassInfo.of(inner.getDeclaringClass()).getField(inner);
	}

	private final Field inner;
	private final ClassInfo declaringClass;
	private final Supplier<ClassInfo> type;
	private final Supplier<List<AnnotationInfo<Annotation>>> annotations;  // All annotations declared directly on this field.
	private final Supplier<String> fullName;  // Fully qualified field name (declaring-class.field-name).

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new FieldInfo wrapper for the specified field. This constructor is protected
	 * and should not be called directly. Use the static factory methods {@link #of(Field)} or
	 * obtain FieldInfo instances from {@link ClassInfo#getField(Field)}.
	 *
	 * @param declaringClass The ClassInfo for the class that declares this field.
	 * @param inner The field being wrapped.
	 */
	protected FieldInfo(ClassInfo declaringClass, Field inner) {
		super(inner, inner.getModifiers());
		assertArgNotNull("inner", inner);
		this.declaringClass = declaringClass;
		this.inner = inner;
		this.type = memoize(() -> ClassInfo.of(inner.getType()));
		this.annotations = memoize(() -> stream(inner.getAnnotations()).flatMap(a -> AnnotationUtils.streamRepeated(a)).map(a -> ai(this, a)).toList());
		this.fullName = memoize(this::findFullName);
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
			inner.setAccessible(true);
			return (T)inner.get(o);
		} catch (Exception e) {
			throw bex(e);
		}
	}

	@Override /* Annotatable */
	public AnnotatableType getAnnotatableType() { return AnnotatableType.FIELD_TYPE; }

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
	public AnnotatedType getAnnotatedType() { return inner.getAnnotatedType(); }

	/**
	 * Returns all annotations declared on this field.
	 *
	 * <p>
	 * <b>Note on Repeatable Annotations:</b>
	 * Repeatable annotations (those marked with {@link java.lang.annotation.Repeatable @Repeatable}) are automatically
	 * expanded into their individual annotation instances. For example, if a field has multiple {@code @Bean} annotations,
	 * this method returns each {@code @Bean} annotation separately, rather than the container annotation.
	 *
	 * @return
	 * 	An unmodifiable list of all annotations declared on this field.
	 * 	<br>Repeatable annotations are expanded into individual instances.
	 */
	public List<AnnotationInfo<Annotation>> getAnnotations() { return annotations.get(); }

	/**
	 * Returns all annotations of the specified type declared on this field.
	 *
	 * @param <A> The annotation type.
	 * @param type The annotation type.
	 * @return A stream of all matching annotations.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> Stream<AnnotationInfo<A>> getAnnotations(Class<A> type) {
		return annotations.get().stream().filter(x -> type.isInstance(x.inner())).map(x -> (AnnotationInfo<A>)x);
	}

	/**
	 * Returns metadata about the declaring class.
	 *
	 * @return Metadata about the declaring class.
	 */
	public ClassInfo getDeclaringClass() { return declaringClass; }

	/**
	 * Returns the type of this field.
	 *
	 * @return The type of this field.
	 */
	public ClassInfo getFieldType() { return type.get(); }

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
	public String getFullName() { return fullName.get(); }

	@Override /* Annotatable */
	public String getLabel() { return getDeclaringClass().getNameSimple() + "." + getName(); }

	/**
	 * Returns the name of this field.
	 *
	 * @return The name of this field.
	 */
	public String getName() { return inner.getName(); }

	/**
	 * Returns <jk>true</jk> if the specified annotation is present.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is present.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return getAnnotations(type).findAny().isPresent();
	}

	/**
	 * Returns <jk>true</jk> if the field has the specified name.
	 *
	 * @param name The name to compare against.
	 * @return <jk>true</jk> if the field has the specified name.
	 */
	public boolean hasName(String name) {
		return inner.getName().equals(name);
	}

	/**
	 * Returns the wrapped field.
	 *
	 * @return The wrapped field.
	 */
	public Field inner() {
		return inner;
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
			case NOT_ENUM_CONSTANT -> ! isEnumConstant();
			case SYNTHETIC -> isSynthetic();
			case NOT_SYNTHETIC -> ! isSynthetic();
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
	public boolean isDeprecated() { return inner.isAnnotationPresent(Deprecated.class); }

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
	public boolean isEnumConstant() { return inner.isEnumConstant(); }

	/**
	 * Returns <jk>true</jk> if this field doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this field doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() { return ! inner.isAnnotationPresent(Deprecated.class); }

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
	public boolean isSynthetic() { return inner.isSynthetic(); }

	/**
	 * Identifies if the specified visibility matches this field.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this field.
	 */
	public boolean isVisible(Visibility v) {
		return v.isVisible(inner);
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
			inner.setAccessible(true);
			inner.set(o, value);
		} catch (Exception e) {
			throw bex(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Field-Specific Methods
	//-----------------------------------------------------------------------------------------------------------------

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
		return inner.toGenericString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotatable interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	public String toString() {
		return cn(inner.getDeclaringClass()) + "." + inner.getName();
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
}