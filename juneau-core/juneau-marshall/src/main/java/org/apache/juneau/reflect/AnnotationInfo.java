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
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.svl.*;

/**
 * Represents an annotation instance on a class and the class it was found on.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The annotation type.
 */
public class AnnotationInfo<T extends Annotation> {

	private final ClassInfo c;
	private final MethodInfo m;
	private final Package p;
	private final T a;
	final int rank;

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
		this.rank = getRank(a);
	}

	private static int getRank(Object a) {
		ClassInfo ci = ClassInfo.ofc(a);
		MethodInfo mi = ci.getPublicMethod(x -> x.hasName("rank") && x.hasNoParams() && x.hasReturnType(int.class));
		if (mi != null) {
			try {
				return (int)mi.invoke(a);
			} catch (ExecutableException e) {
				e.printStackTrace();
			}
		}
		return 0;
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
	public OMap toOMap() {
		OMap om = new OMap();
		if (c != null)
			om.put("class", c.getSimpleName());
		if (m != null)
			om.put("method", m.getShortName());
		if (p != null)
			om.put("package", p.getName());
		OMap oa = new OMap();
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

	private Constructor<? extends AnnotationApplier<?,?>>[] applyConstructors;

	/**
	 * If this annotation has a {@link ContextApply} annotation, returns an instance of the specified {@link AnnotationApplier} class.
	 *
	 * @param vrs Variable resolver passed to the {@link AnnotationApplier} object.
	 * @return A new {@link AnnotationApplier} object.  Never <jk>null</jk>.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public AnnotationApplier<Annotation,Object>[] getApplies(VarResolverSession vrs) throws ExecutableException {
		try {
			if (applyConstructors == null) {
				ContextApply cpa = a.annotationType().getAnnotation(ContextApply.class);
				if (cpa == null)
					applyConstructors = new Constructor[]{ AnnotationApplier.NoOp.class.getConstructor(VarResolverSession.class) };
				else {
					applyConstructors = new Constructor[cpa.value().length];
					for (int i = 0; i < cpa.value().length; i++)
						applyConstructors[i] = (Constructor<? extends AnnotationApplier<?,?>>) cpa.value()[i].getConstructor(VarResolverSession.class);
				}
			}
			AnnotationApplier<Annotation,Object>[] aa = new AnnotationApplier[applyConstructors.length];
			for (int i = 0; i < aa.length; i++)
				aa[i] = (AnnotationApplier<Annotation,Object>) applyConstructors[i].newInstance(vrs);
			return aa;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Returns the class that this annotation was found on.
	 *
	 * @return The class that this annotation was found on, or <jk>null</jk> if it was found on a package.
	 */
	public ClassInfo getClassInfo() {
		if (this.c != null)
			return this.c;
		if (this.m != null)
			return this.m.getDeclaringClass();
		return null;
	}

	/**
	 * Returns <jk>true</jk> if this annotation is the specified type.
	 *
	 * @param a The type to test against.
	 * @return <jk>true</jk> if this annotation is the specified type.
	 */
	public boolean isType(Class<? extends Annotation> a) {
		Class<? extends Annotation> at = this.a.annotationType();
		return at == a;
	}

	/**
	 * Returns <jk>true</jk> if this annotation has the specified annotation defined on it.
	 *
	 * @param a The annotation to test for.
	 * @return <jk>true</jk> if this annotation has the specified annotation defined on it.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return this.a.annotationType().getAnnotation(a) != null;
	}

	/**
	 * Returns <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup group}.
	 *
	 * @param group The group annotation.
	 * @return <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup group}.
	 */
	public boolean isInGroup(Class<? extends Annotation> group) {
		AnnotationGroup x = a.annotationType().getAnnotation(AnnotationGroup.class);
		return (x != null && x.value().equals(group));
	}

	@Override
	public String toString() {
		return SimpleJson.DEFAULT_READABLE.toString(toOMap());
	}

	/**
	 * Returns a value on this annotation.
	 *
	 * @param type The annotation field type.
	 * @param name The annotation field name.
	 * @return The value on this annotation if the field exists and is the specified type.
	 */
	@SuppressWarnings("unchecked")
	public <V> Optional<V> getValue(Class<V> type, String name) {
		for (Method m : a.annotationType().getMethods())
			if (m.getName().equals(name) && m.getReturnType().equals(type)) {
				try {
					return Optional.ofNullable((V)m.invoke(a));
				} catch (Exception e) {
					e.printStackTrace(); // Shouldn't happen.
				}
			}
		return Optional.empty();
	}
}
