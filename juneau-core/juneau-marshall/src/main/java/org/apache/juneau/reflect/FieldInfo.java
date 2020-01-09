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

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;

/**
 * Lightweight utility class for introspecting information about a field.
 */
@BeanIgnore
public final class FieldInfo implements Comparable<FieldInfo> {

	private final Field f;
	private ClassInfo declaringClass, type;

	//-----------------------------------------------------------------------------------------------------------------
	// Instantiation.
	//-----------------------------------------------------------------------------------------------------------------

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
	 * Convenience method for instantiating a {@link FieldInfo};
	 *
	 * @param declaringClass The class that declares this method.
	 * @param f The field being wrapped.
	 * @return A new {@link FieldInfo} object, or <jk>null</jk> if the field was null.
	 */
	public static FieldInfo of(ClassInfo declaringClass, Field f) {
		if (f == null)
			return null;
		return new FieldInfo(declaringClass, f);
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
		return new FieldInfo(null, f);
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
		if (declaringClass == null)
			declaringClass = ClassInfo.of(f.getDeclaringClass());
		return declaringClass;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotations
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the specified annotation on this field.
	 *
	 * @param a The annotation to look for.
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <T extends Annotation> T getAnnotation(Class<T> a) {
		return getAnnotation(a, MetaProvider.DEFAULT);
	}

	/**
	 * Returns the specified annotation on this field.
	 *
	 * @param a The annotation to look for.
	 * @param mp The meta provider for looking up annotations on reflection objects (classes, methods, fields, constructors).
	 * @return The annotation, or <jk>null</jk> if not found.
	 */
	public <T extends Annotation> T getAnnotation(Class<T> a, MetaProvider mp) {
		return mp.getAnnotation(a, f);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present.
	 *
	 * @param a The annotation to check for.
	 * @return <jk>true</jk> if the specified annotation is present.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return f.isAnnotationPresent(a);
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation is present.
	 *
	 * @param a The annotation to check for.
	 * @param mp The meta provider for looking up annotations on reflection objects (classes, methods, fields, constructors).
	 * @return <jk>true</jk> if the specified annotation is present.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a, MetaProvider mp) {
		return mp.getAnnotation(a, f) != null;
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
					throw new RuntimeException("Invalid flag for field: " + f);
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
					throw new RuntimeException("Invalid flag for field: " + f);
			}
		}
		return false;
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
	 * @return <jk>true</jk> if call was successful.
	 */
	public boolean setAccessible() {
		try {
			if (! (f.isAccessible()))
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
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the type of this field.
	 *
	 * @return The type of this field.
	 */
	public ClassInfo getType() {
		if (type == null)
			type = ClassInfo.of(f.getType());
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
}
