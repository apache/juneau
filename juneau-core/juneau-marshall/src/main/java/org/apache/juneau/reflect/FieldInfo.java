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

import static org.apache.juneau.internal.ConsumerUtils.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Lightweight utility class for introspecting information about a field.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class FieldInfo implements Comparable<FieldInfo> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Field f;
	private final ClassInfo declaringClass;
	private volatile ClassInfo type;

	/**
	 * Constructor.
	 *
	 * @param declaringClass The class that declares this method.
	 * @param f The field being wrapped.
	 */
	protected FieldInfo(ClassInfo declaringClass, Field f) {
		this.declaringClass = declaringClass;
		this.f = f;
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
	 * Returns metadata about the declaring class.
	 *
	 * @return Metadata about the declaring class.
	 */
	public ClassInfo getDeclaringClass() {
		return declaringClass;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the specified annotation on this field.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		return getAnnotation(AnnotationProvider.DEFAULT, type);
	}

	/**
	 * Returns the specified annotation on this field.
	 *
	 * @param <A> The annotation type to look for.
	 * @param annotationProvider The annotation provider.
	 * @param type The annotation to look for.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <A extends Annotation> A getAnnotation(AnnotationProvider annotationProvider, Class<A> type) {
		Value<A> t = Value.empty();
		annotationProvider.forEachAnnotation(type, f, x -> true, x -> t.set(x));
		return t.orElse(null);
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
	 * Returns <jk>true</jk> if the specified annotation is not present on this field.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to look for.
	 * @return <jk>true</jk> if the specified annotation is not present on this field.
	 */
	public final <A extends Annotation> boolean hasNoAnnotation(Class<A> type) {
		return ! hasAnnotation(type);
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
		return annotationProvider.firstAnnotation(type, f, x -> true) != null;
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

	//-----------------------------------------------------------------------------------------------------------------
	// Characteristics
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this field.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this field.
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
				case TRANSIENT:
					if (isNotTransient())
						return false;
					break;
				case NOT_TRANSIENT:
					if (isTransient())
						return false;
					break;
				default:
					throw new BasicRuntimeException("Invalid flag for field: {0}", f);
			}
		}
		return true;
	}

	/**
	 * Returns <jk>true</jk> if all specified flags are applicable to this field.
	 *
	 * @param flags The flags to test for.
	 * @return <jk>true</jk> if all specified flags are applicable to this field.
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
				case TRANSIENT:
					if (isTransient())
						return true;
					break;
				case NOT_TRANSIENT:
					if (isNotTransient())
						return true;
					break;
				default:
					throw new BasicRuntimeException("Invalid flag for field: {0}", f);
			}
		}
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
	 * Returns <jk>true</jk> if this field has the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this field has the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isDeprecated() {
		return f.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this field doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 *
	 * @return <jk>true</jk> if this field doesn't have the {@link Deprecated @Deprecated} annotation on it.
	 */
	public boolean isNotDeprecated() {
		return ! f.isAnnotationPresent(Deprecated.class);
	}

	/**
	 * Returns <jk>true</jk> if this field is public.
	 *
	 * @return <jk>true</jk> if this field is public.
	 */
	public boolean isPublic() {
		return Modifier.isPublic(f.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this field is not public.
	 *
	 * @return <jk>true</jk> if this field is not public.
	 */
	public boolean isNotPublic() {
		return ! Modifier.isPublic(f.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this field is static.
	 *
	 * @return <jk>true</jk> if this field is static.
	 */
	public boolean isStatic() {
		return Modifier.isStatic(f.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this field is not static.
	 *
	 * @return <jk>true</jk> if this field is not static.
	 */
	public boolean isNotStatic() {
		return ! Modifier.isStatic(f.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this field is transient.
	 *
	 * @return <jk>true</jk> if this field is transient.
	 */
	public boolean isTransient() {
		return Modifier.isTransient(f.getModifiers());
	}

	/**
	 * Returns <jk>true</jk> if this field is not transient.
	 *
	 * @return <jk>true</jk> if this field is not transient.
	 */
	public boolean isNotTransient() {
		return ! Modifier.isTransient(f.getModifiers());
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

	//-----------------------------------------------------------------------------------------------------------------
	// Visibility
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return This object.
	 */
	public FieldInfo accessible() {
		setAccessible();
		return this;
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @return <jk>true</jk> if call was successful.
	 */
	public boolean setAccessible() {
		try {
			if (f != null)
				f.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Identifies if the specified visibility matches this field.
	 *
	 * @param v The visibility to validate against.
	 * @return <jk>true</jk> if this visibility matches the modifier attribute of this field.
	 */
	public boolean isVisible(Visibility v) {
		return v.isVisible(f);
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
	public boolean matches(Predicate<FieldInfo> test) {
		return test(test, this);
	}

	/**
	 * Performs an action on this object if the specified predicate test passes.
	 *
	 * @param test A test to apply to determine if action should be executed.  Can be <jk>null</jk>.
	 * @param action An action to perform on this object.
	 * @return This object.
	 */
	public FieldInfo accept(Predicate<FieldInfo> test, Consumer<FieldInfo> action) {
		if (matches(test))
			action.accept(this);
		return this;
	}

	/**
	 * Returns the type of this field.
	 *
	 * @return The type of this field.
	 */
	public ClassInfo getType() {
		if (type == null) {
			synchronized(this) {
				type = ClassInfo.of(f.getType());
			}
		}
		return type;
	}

	@Override
	public String toString() {
		return f.getDeclaringClass().getName() + "." + f.getName();
	}

	@Override
	public int compareTo(FieldInfo o) {
		return getName().compareTo(o.getName());
	}

	/**
	 * Returns the name of this field.
	 *
	 * @return The name of this field.
	 */
	public String getName() {
		return f.getName();
	}

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
		StringBuilder sb = new StringBuilder(128);
		ClassInfo dc = declaringClass;
		Package p = dc.getPackage();
		if (p != null)
			sb.append(p.getName()).append('.');
		dc.appendShortName(sb);
		sb.append(".").append(getName());
		return sb.toString();
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
}
