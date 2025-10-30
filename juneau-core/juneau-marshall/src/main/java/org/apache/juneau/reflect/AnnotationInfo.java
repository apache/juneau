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
package org.apache.juneau.reflect;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.PredicateUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.svl.*;

/**
 * Represents an annotation instance on a class and the class it was found on.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The annotation type.
 */
public class AnnotationInfo<T extends Annotation> {

	/**
	 * Convenience constructor when annotation is found on a class.
	 *
	 * @param <A> The annotation class.
	 * @param onClass The class where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(ClassInfo onClass, A value) {
		return new AnnotationInfo<>(onClass, null, null, value);
	}

	/**
	 * Convenience constructor when annotation is found on a method.
	 *
	 * @param <A> The annotation class.
	 * @param onMethod The method where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(MethodInfo onMethod, A value) {
		return new AnnotationInfo<>(null, onMethod, null, value);
	}

	/**
	 * Convenience constructor when annotation is found on a package.
	 *
	 * @param <A> The annotation class.
	 * @param onPackage The package where the annotation was found.
	 * @param value The annotation found.
	 * @return A new {@link AnnotationInfo} object.
	 */
	public static <A extends Annotation> AnnotationInfo<A> of(Package onPackage, A value) {
		return new AnnotationInfo<>(null, null, onPackage, value);
	}

	private static int getRank(Object a) {
		ClassInfo ci = ClassInfo.of(a);
		MethodInfo mi = ci.getPublicMethod(x -> x.hasName("rank") && x.hasNoParams() && x.hasReturnType(int.class));
		if (nn(mi)) {
			try {
				return (int)mi.invoke(a);
			} catch (ExecutableException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	private final ClassInfo c;
	private final MethodInfo m;

	private final Package p;

	private final T a;

	private volatile Method[] methods;

	final int rank;

	private Constructor<? extends AnnotationApplier<?,?>>[] applyConstructors;

	/**
	 * Constructor.
	 *
	 * @param c The class where the annotation was found.
	 * @param m The method where the annotation was found.
	 * @param p The package where the annotation was found.
	 * @param a The annotation found.
	 */
	AnnotationInfo(ClassInfo c, MethodInfo m, Package p, T a) {
		this.c = c;
		this.m = m;
		this.p = p;
		this.a = a;
		this.rank = getRank(a);
	}

	/**
	 * Performs an action on this object if the specified predicate test passes.
	 *
	 * @param test A test to apply to determine if action should be executed.  Can be <jk>null</jk>.
	 * @param action An action to perform on this object.
	 * @return This object.
	 */
	public AnnotationInfo<?> accept(Predicate<AnnotationInfo<?>> test, Consumer<AnnotationInfo<?>> action) {
		if (matches(test))
			action.accept(this);
		return this;
	}

	/**
	 * Performs an action on all matching values on this annotation.
	 *
	 * @param <V> The annotation field type.
	 * @param type The annotation field type.
	 * @param name The annotation field name.
	 * @param test A predicate to apply to the value to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the value.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <V> AnnotationInfo<?> forEachValue(Class<V> type, String name, Predicate<V> test, Consumer<V> action) {
		for (var m : _getMethods())
			if (m.getName().equals(name) && m.getReturnType().equals(type))
				safe(() -> consumeIf(test, action, (V)m.invoke(a)));
		return this;
	}

	/**
	 * If this annotation has a {@link ContextApply} annotation, consumes an instance of the specified {@link AnnotationApplier} class.
	 *
	 * @param vrs Variable resolver passed to the {@link AnnotationApplier} object.
	 * @param consumer The consumer.
	 * @return This object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public AnnotationInfo<T> getApplies(VarResolverSession vrs, Consumer<AnnotationApplier<Annotation,Object>> consumer) throws ExecutableException {
		try {
			if (applyConstructors == null) {
				ContextApply cpa = a.annotationType().getAnnotation(ContextApply.class);
				if (cpa == null)
					applyConstructors = a(AnnotationApplier.NoOp.class.getConstructor(VarResolverSession.class));
				else {
					applyConstructors = new Constructor[cpa.value().length];
					for (int i = 0; i < cpa.value().length; i++)
						applyConstructors[i] = (Constructor<? extends AnnotationApplier<?,?>>)cpa.value()[i].getConstructor(VarResolverSession.class);
				}
			}
			for (var applyConstructor : applyConstructors)
				consumer.accept((AnnotationApplier<Annotation,Object>)applyConstructor.newInstance(vrs));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ExecutableException(e);
		}
		return this;
	}

	/**
	 * Returns the class that this annotation was found on.
	 *
	 * @return The class that this annotation was found on, or <jk>null</jk> if it was found on a package.
	 */
	public ClassInfo getClassInfo() {
		if (nn(this.c))
			return this.c;
		if (nn(this.m))
			return this.m.getDeclaringClass();
		return null;
	}

	/**
	 * Returns the class where the annotation was found.
	 *
	 * @return the class where the annotation was found, or <jk>null</jk> if it wasn't found on a method.
	 */
	public ClassInfo getClassOn() { return c; }

	/**
	 * Returns the method where the annotation was found.
	 *
	 * @return the method where the annotation was found, or <jk>null</jk> if it wasn't found on a method.
	 */
	public MethodInfo getMethodOn() { return m; }

	/**
	 * Returns the class name of the annotation.
	 *
	 * @return The simple class name of the annotation.
	 */
	public String getName() { return scn(a.annotationType()); }

	/**
	 * Returns the package where the annotation was found.
	 *
	 * @return the package where the annotation was found, or <jk>null</jk> if it wasn't found on a package.
	 */
	public Package getPackageOn() { return p; }

	/**
	 * Returns a matching value on this annotation.
	 *
	 * @param <V> The annotation field type.
	 * @param type The annotation field type.
	 * @param name The annotation field name.
	 * @param test A predicate to apply to the value to determine if value should be used.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public <V> Optional<V> getValue(Class<V> type, String name, Predicate<V> test) {
		for (var m : _getMethods())
			if (m.getName().equals(name) && m.getReturnType().equals(type)) {
				try {
					V v = (V)m.invoke(a);
					if (test(test, v))
						return opt(v);
				} catch (Exception e) {
					e.printStackTrace(); // Shouldn't happen.
				}
			}
		return opte();
	}

	/**
	 * Returns <jk>true</jk> if this annotation has the specified annotation defined on it.
	 *
	 * @param <A> The annotation class.
	 * @param type The annotation to test for.
	 * @return <jk>true</jk> if this annotation has the specified annotation defined on it.
	 */
	public <A extends Annotation> boolean hasAnnotation(Class<A> type) {
		return nn(this.a.annotationType().getAnnotation(type));
	}

	/**
	 * Returns the annotation found.
	 *
	 * @return The annotation found.
	 */
	public T inner() {
		return a;
	}

	/**
	 * Returns <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup group}.
	 *
	 * @param <A> The annotation class.
	 * @param group The group annotation.
	 * @return <jk>true</jk> if this annotation is in the specified {@link AnnotationGroup group}.
	 */
	public <A extends Annotation> boolean isInGroup(Class<A> group) {
		AnnotationGroup x = a.annotationType().getAnnotation(AnnotationGroup.class);
		return (nn(x) && x.value().equals(group));
	}

	/**
	 * Returns <jk>true</jk> if this annotation is the specified type.
	 *
	 * @param <A> The annotation class.
	 * @param type The type to test against.
	 * @return <jk>true</jk> if this annotation is the specified type.
	 */
	public <A extends Annotation> boolean isType(Class<A> type) {
		Class<? extends Annotation> at = this.a.annotationType();
		return at == type;
	}

	/**
	 * Returns <jk>true</jk> if this object passes the specified predicate test.
	 *
	 * @param test The test to perform.
	 * @return <jk>true</jk> if this object passes the specified predicate test.
	 */
	public boolean matches(Predicate<AnnotationInfo<?>> test) {
		return test(test, this);
	}

	/**
	 * Converts this object to a readable JSON object for debugging purposes.
	 *
	 * @return A new map showing the attributes of this object as a JSON object.
	 */
	public JsonMap toJsonMap() {
		JsonMap jm = new JsonMap();
		if (nn(c))
			jm.put("class", c.getSimpleName());
		if (nn(m))
			jm.put("method", m.getShortName());
		if (nn(p))
			jm.put("package", p.getName());
		JsonMap ja = new JsonMap();
		ClassInfo ca = ClassInfo.of(a.annotationType());
		ca.forEachDeclaredMethod(null, x -> {
			try {
				Object v = x.invoke(a);
				Object d = x.inner().getDefaultValue();
				if (ne(v, d)) {
					if (! (isArray(v) && Array.getLength(v) == 0 && Array.getLength(d) == 0))
						ja.put(m.getName(), v);
				}
			} catch (Exception e) {
				ja.put(m.getName(), lm(e));
			}
		});
		jm.put("@" + ca.getSimpleName(), ja);
		return jm;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return Json5.DEFAULT_READABLE.write(toJsonMap());
	}

	Method[] _getMethods() {
		if (methods == null)
			synchronized (this) {
				methods = a.annotationType().getMethods();
			}
		return methods;
	}
}