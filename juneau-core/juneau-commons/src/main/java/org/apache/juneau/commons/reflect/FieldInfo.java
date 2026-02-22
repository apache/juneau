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
package org.apache.juneau.commons.reflect;

import static org.apache.juneau.commons.reflect.ClassArrayFormat.*;
import static org.apache.juneau.commons.reflect.ClassNameFormat.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.utils.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsReflection">Reflection Package</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115",  // Constants use UPPER_snakeCase convention (e.g., CONST_value)
	"java:S3011"  // Reflection access needed for field introspection
})
public class FieldInfo extends AccessibleInfo implements Comparable<FieldInfo>, Annotatable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_declaringClass = "declaringClass";
	private static final String ARG_inner = "inner";

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
		assertArgNotNull(ARG_declaringClass, declaringClass);
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
		assertArgNotNull(ARG_inner, inner);
		return ClassInfo.of(inner.getDeclaringClass()).getField(inner);
	}

	private final Field inner;
	private final ClassInfo declaringClass;
	private final Supplier<ClassInfo> type;
	private final Supplier<List<AnnotationInfo<Annotation>>> annotations;  // All annotations declared directly on this field.
	private final Supplier<String> nameFull;  // Fully qualified field name (declaring-class.field-name).
	private final Supplier<String> toString;  // String representation with modifiers, type, and full name.

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
		assertArgNotNull(ARG_inner, inner);
		this.declaringClass = declaringClass;
		this.inner = inner;
		this.type = mem(() -> ClassInfo.of(inner.getType(), inner.getGenericType()));
		this.annotations = mem(() -> stream(inner.getAnnotations()).flatMap(AnnotationUtils::streamRepeated).map(a -> ai(this, a)).toList());
		this.nameFull = mem(this::findNameFull);
		this.toString = mem(this::findToString);
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
		return cmp(getName(), o.getName());
	}

	/**
	 * Returns the field value on the specified object.
	 *
	 * <p>
	 * If the underlying field is static, then the specified <c>o</c> argument is ignored. It may be <jk>null</jk>.
	 *
	 * @param <T> The object type to retrieve.
	 * @param o The object containing the field.  Can be <jk>null</jk> for static fields.
	 * @return The field value.
	 * @throws BeanRuntimeException Field was not accessible or field does not belong to object.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast to T for field retrieval
	})
	public <T> T get(Object o) throws BeanRuntimeException {
		return safe(() -> {
			inner.setAccessible(true);
			return (T)inner.get(o);
		}, e -> bex(e));
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
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for annotation stream
	})
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
	public String getNameFull() { return nameFull.get(); }

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
	 * Compares this FieldInfo with the specified object for equality.
	 *
	 * <p>
	 * Two FieldInfo objects are considered equal if they wrap the same underlying {@link Field} object.
	 * This delegates to the underlying {@link Field#equals(Object)} method.
	 *
	 * <p>
	 * This method makes FieldInfo suitable for use as keys in hash-based collections such as {@link HashMap}
	 * and {@link HashSet}.
	 *
	 * @param obj The object to compare with.
	 * @return <jk>true</jk> if the objects are equal, <jk>false</jk> otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof FieldInfo other && eq(this, other, (x, y) -> eq(x.inner, y.inner));
	}

	/**
	 * Returns a hash code value for this FieldInfo.
	 *
	 * <p>
	 * This delegates to the underlying {@link Field#hashCode()} method.
	 *
	 * <p>
	 * This method makes FieldInfo suitable for use as keys in hash-based collections such as {@link HashMap}
	 * and {@link HashSet}.
	 *
	 * @return A hash code value for this FieldInfo.
	 */
	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this field.
	 *
	 * @param flag The flag to test for.
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
			case NOT_SYNTHETIC -> ! isSynthetic();  // HTT
			default -> super.is(flag);
		};
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
	 * <p>
	 * If the underlying field is static, then the specified <c>o</c> argument is ignored. It may be <jk>null</jk>.
	 *
	 * @param o The object containing the field.  Can be <jk>null</jk> for static fields.
	 * @param value The new field value.
	 * @throws BeanRuntimeException Field was not accessible or field does not belong to object.
	 */
	public void set(Object o, Object value) throws BeanRuntimeException {
		safe((Snippet)() -> {
			inner.setAccessible(true);
			inner.set(o, value);
		}, e -> bex(e));
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

	/**
	 * Returns a detailed string representation of this field.
	 *
	 * <p>
	 * The returned string includes:
	 * <ul>
	 * 	<li>Modifiers (public, private, protected, static, final, volatile, transient)
	 * 	<li>Field type with generics (e.g., "List&lt;String&gt;")
	 * 	<li>Fully qualified field name (declaring-class.field-name)
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Simple field</jc>
	 * 	FieldInfo <jv>fi</jv> = ...;
	 * 	<jv>fi</jv>.toString();
	 * 	<jc>// Returns: "public java.lang.String org.example.MyClass.name"</jc>
	 *
	 * 	<jc>// Static final field</jc>
	 * 	<jc>// Returns: "public static final int org.example.MyClass.MAX_VALUE"</jc>
	 *
	 * 	<jc>// Generic field</jc>
	 * 	<jc>// Returns: "private java.util.List&lt;java.lang.String&gt; org.example.MyClass.items"</jc>
	 *
	 * 	<jc>// Volatile field</jc>
	 * 	<jc>// Returns: "volatile boolean org.example.MyClass.flag"</jc>
	 * </p>
	 *
	 * @return A detailed string representation including modifiers, type, and full name.
	 */
	@Override
	public String toString() {
		return toString.get();
	}

	private String findToString() {
		var sb = new StringBuilder(256);

		// Modifiers
		var mods = Modifier.toString(getModifiers());
		if (nn(mods) && ! mods.isEmpty()) {
			sb.append(mods).append(" ");
		}

		// Field type (use generic type to show generics)
		var genericType = inner.getGenericType();
		ClassInfo.of(genericType).appendNameFormatted(sb, FULL, true, '$', BRACKETS);

		// Fully qualified field name
		sb.append(" ").append(getNameFull());

		return sb.toString();
	}

	private String findNameFull() {
		var sb = new StringBuilder(128);
		var dc = declaringClass;
		var pi = dc.getPackage();
		if (nn(pi))
			sb.append(pi.getName()).append('.');
		// HTT - false branch (pi == null) is hard to test: some classloaders return Package objects
		// even for default package classes, though Java API spec says it should return null
		dc.appendNameFormatted(sb, SHORT, true, '$', BRACKETS);
		sb.append('.').append(getName());
		return sb.toString();
	}

	/**
	 * Resolves field value from the bean store and sets it on the specified bean instance.
	 *
	 * <p>
	 * This method resolves the field value using the same logic as parameter resolution,
	 * supporting single beans, {@code Optional}, arrays, {@code List}, {@code Set}, and {@code Map}.
	 * The field is made accessible before setting its value.
	 *
	 * <h5 class='section'>Field Resolution:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>Single beans</b> - Resolved using {@link BeanStore#getBean(Class)} or {@link BeanStore#getBean(Class, String)}.
	 * 		Throws {@link ExecutableException} if not found.
	 * 	<li><b>Optional beans</b> - Wrapped in <c>Optional</c>, or <c>Optional.empty()</c> if not found.
	 * 		Never throws an exception.
	 * 	<li><b>Arrays</b> - All beans of the element type are collected into an array (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Lists</b> - All beans of the element type are collected into a <c>List</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Sets</b> - All beans of the element type are collected into a <c>LinkedHashSet</c> (may be empty).
	 * 		Never throws an exception.
	 * 	<li><b>Maps</b> - All beans of the value type are collected into a <c>LinkedHashMap</c> keyed by bean name (may be empty).
	 * 		Unnamed beans use an empty string as the key.  Never throws an exception.
	 * </ul>
	 *
	 * @param <T> The bean type.
	 * @param beanStore The bean store to resolve the field value from.
	 * @param bean The object instance containing the field.
	 * @return The same bean instance (for method chaining).
	 * @throws ExecutableException If a required field (non-Optional, non-collection) cannot be resolved from the bean store.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for dependency injection
	})
	public <T> T inject(BeanStore beanStore, T bean) {
		accessible();
		var fieldType = getFieldType();

		// Find qualifier from @Named or @Qualifier annotation (same logic as ParameterInfo.getResolvedQualifier)
		var beanQualifier = getAnnotations().stream()
			.filter(ai -> ai.hasNameSimple("Named") || ai.hasNameSimple("Qualifier"))
			.map(ai -> ai.getValue().orElse(null))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);

		var ptUnwrapped = fieldType.unwrap(Optional.class);

		// Handle collections and arrays
		Object collectionValue = null;
		if (ptUnwrapped.isInjectCollectionType()) {
			// Extract element type from field type
			Class<?> elementType = null;

			if (ptUnwrapped.isArray()) {
				elementType = ptUnwrapped.getComponentType().inner();
			} else {
				Type parameterizedType = fieldType.innerType();
				var inner2 = opt(ptUnwrapped.inner()).orElse(Object.class);

				if (eq(inner2, List.class) || eq(inner2, Set.class)) {
					if (parameterizedType instanceof ParameterizedType pt2) {
						var typeArgs = pt2.getActualTypeArguments();
						if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementClass) {
							elementType = elementClass;
						}
					}
				} else if (eq(inner2, Map.class) && parameterizedType instanceof ParameterizedType pt2) {
					var typeArgs = pt2.getActualTypeArguments();
					if (typeArgs.length >= 2 && typeArgs[0] == String.class && typeArgs[1] instanceof Class<?> valueClass) {
						elementType = valueClass;
					}
				}
			}

			collectionValue = ReflectionUtils.resolveCollectionValue(elementType, beanStore, ptUnwrapped);
		}

		Object value;
		if (nn(collectionValue)) {
			value = fieldType.is(Optional.class) ? Optional.of(collectionValue) : collectionValue;
		} else {
			// Handle single bean
			var ptc = ptUnwrapped.inner();
			var o2 = beanQualifier == null ? beanStore.getBean(ptc) : beanStore.getBean(ptc, beanQualifier);

			if (fieldType.is(Optional.class)) {
				value = o2;
			} else if (o2.isPresent()) {
				value = o2.get();
			} else {
				throw exex("Could not resolve value for field {0}", this);
			}
		}

		set(bean, value);
		return bean;
	}
}