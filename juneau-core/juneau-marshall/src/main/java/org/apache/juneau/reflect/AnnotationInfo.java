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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;

/**
 * Represents an annotation instance on a class and the class it was found on.
 *
 * @param <T> The annotation type.
 */
public class AnnotationInfo<T extends Annotation> {

	private ClassInfo c;
	private MethodInfo m;
	private Package p;
	private T a;

	/**
	 * Constructor.
	 *
	 * @param c The class where the annotation was found.
	 * @param m The method where the annotation was found.
	 * @param p The package where the annotation was found.
	 * @param a The annotation found.
	 */
	protected AnnotationInfo(ClassInfo c, MethodInfo m, Package p, T a) {
		this.c = c;
		this.m = m;
		this.p = p;
		this.a = a;
	}

	/**
	 * Convenience constructor when annotation is found on a class.
	 *
	 * @param c The class where the annotation was found.
	 * @param a The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <T extends Annotation> AnnotationInfo<T> of(ClassInfo c, T a) {
		return new AnnotationInfo<>(c, null, null, a);
	}

	/**
	 * Convenience constructor when annotation is found on a method.
	 *
	 * @param m The method where the annotation was found.
	 * @param a The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <T extends Annotation> AnnotationInfo<T> of(MethodInfo m, T a) {
		return new AnnotationInfo<>(null, m, null, a);
	}

	/**
	 * Convenience constructor when annotation is found on a package.
	 *
	 * @param p The package where the annotation was found.
	 * @param a The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <T extends Annotation> AnnotationInfo<T> of(Package p, T a) {
		return new AnnotationInfo<>(null, null, p, a);
	}

	/**
	 * Returns the class where the annotation was found.
	 *
	 * @return the class where the annotation was found, or <jk>null</jk> if it wasn't found on a method.
	 */
	public ClassInfo getClassOn() {
		return c;
	}

	/**
	 * Returns the method where the annotation was found.
	 *
	 * @return the method where the annotation was found, or <jk>null</jk> if it wasn't found on a method.
	 */
	public MethodInfo getMethodOn() {
		return m;
	}

	/**
	 * Returns the package where the annotation was found.
	 *
	 * @return the package where the annotation was found, or <jk>null</jk> if it wasn't found on a package.
	 */
	public Package getPackageOn() {
		return p;
	}

	/**
	 * Returns the annotation found.
	 *
	 * @return The annotation found.
	 */
	public T getAnnotation() {
		return a;
	}

	/**
	 * Converts this object to a readable JSON object for debugging purposes.
	 *
	 * @return A new map showing the attributes of this object as a JSON object.
	 */
	public ObjectMap toObjectMap() {
		ObjectMap om = new ObjectMap();
		if (c != null)
			om.put("class", c.getSimpleName());
		if (m != null)
			om.put("method", m.getShortName());
		if (p != null)
			om.put("package", p.getName());
		ObjectMap oa = new ObjectMap();
		Class<?> ca = a.annotationType();
		for (Method m : ca.getDeclaredMethods()) {
			try {
				Object v = m.invoke(a);
				Object d = m.getDefaultValue();
				if (! Objects.equals(v, d)) {
					if (! (ArrayUtils.isArray(v) && Array.getLength(v) == 0 && Array.getLength(d) == 0))
						oa.put(m.getName(), v);
				}
			} catch (Exception e) {
				oa.put(m.getName(), e.getLocalizedMessage());
			}
		}
		om.put("@" + ca.getSimpleName(), oa);
		return om;
	}

	@Override
	public String toString() {
		return SimpleJson.DEFAULT_READABLE.toString(toObjectMap());
	}
}
