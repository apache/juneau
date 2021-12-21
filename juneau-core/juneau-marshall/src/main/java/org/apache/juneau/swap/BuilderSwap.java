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
package org.apache.juneau.swap;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;

/**
 * Specialized transform for builder classes.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.PojoBuilders}
 * 	<li class='link'>{@doc jm.Swaps}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The bean class.
 * @param <B> The builder class.
 */
@SuppressWarnings("unchecked")
public class BuilderSwap<T,B> {

	private final Class<T> objectClass;
	private final Class<B> builderClass;
	private final Constructor<T> objectConstructor;          // public Pojo(Builder);
	private final Constructor<B> builderConstructor;       // protected Builder();
	private final MethodInfo createBuilderMethod;          // Builder create();
	private final MethodInfo createObjectMethod;             // Pojo build();
	private ClassMeta<?> builderClassMeta;

	/**
	 * Constructor.
	 *
	 * @param objectClass The object class created by the builder class.
	 * @param builderClass The builder class.
	 * @param objectConstructor The object constructor that takes in a builder as a parameter.
	 * @param builderConstructor The builder no-arg constructor.
	 * @param createBuilderMethod The static create() method on the object class.
	 * @param createObjectMethod The build() method on the builder class.
	 */
	protected BuilderSwap(Class<T> objectClass, Class<B> builderClass, Constructor<T> objectConstructor, Constructor<B> builderConstructor, MethodInfo createBuilderMethod, MethodInfo createObjectMethod) {
		this.objectClass = objectClass;
		this.builderClass = builderClass;
		this.objectConstructor = objectConstructor;
		this.builderConstructor = builderConstructor;
		this.createBuilderMethod = createBuilderMethod;
		this.createObjectMethod = createObjectMethod;
	}

	/**
	 * The object class.
	 *
	 * @return The object class.
	 */
	public Class<T> getObjectClass() {
		return objectClass;
	}

	/**
	 * The builder class.
	 *
	 * @return The builder class.
	 */
	public Class<B> getBuilderClass() {
		return builderClass;
	}

	/**
	 * Returns the {@link ClassMeta} of the transformed class type.
	 *
	 * <p>
	 * This value is cached for quick lookup.
	 *
	 * @param session
	 * 	The bean context to use to get the class meta.
	 * 	This is always going to be the same bean context that created this swap.
	 * @return The {@link ClassMeta} of the transformed class type.
	 */
	public ClassMeta<?> getBuilderClassMeta(BeanSession session) {
		if (builderClassMeta == null)
			builderClassMeta = session.getClassMeta(getBuilderClass());
		return builderClassMeta;
	}

	/**
	 * Creates a new builder object.
	 *
	 * @param session The current bean session.
	 * @param hint A hint about the class type.
	 * @return A new object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public B create(BeanSession session, ClassMeta<?> hint) throws ExecutableException {
		if (createBuilderMethod != null)
			return (B)createBuilderMethod.invoke(null);
		try {
			return builderConstructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Creates a new object from the specified builder.
	 *
	 * @param session The current bean session.
	 * @param builder The object builder.
	 * @param hint A hint about the class type.
	 * @return A new object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public T build(BeanSession session, B builder, ClassMeta<?> hint) throws ExecutableException {
		if (createObjectMethod != null)
			return (T)createObjectMethod.invoke(builder);
		try {
			return objectConstructor.newInstance(builder);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ExecutableException(e);
		}
	}

	/**
	 * Creates a BuilderSwap from the specified builder class if it qualifies as one.
	 *
	 * @param builderClass The potential builder class.
	 * @param cVis Minimum constructor visibility.
	 * @param mVis Minimum method visibility.
	 * @return A new swap instance, or <jk>null</jk> if class wasn't a builder class.
	 */
	@SuppressWarnings("rawtypes")
	public static BuilderSwap<?,?> findSwapFromBuilderClass(Class<?> builderClass, Visibility cVis, Visibility mVis) {
		ClassInfo bci = ClassInfo.of(builderClass);
		if (bci.isNotPublic())
			return null;

		Class<?> objectClass = ClassInfo.of(builderClass).getParameterType(0, Builder.class);

		MethodInfo createObjectMethod, createBuilderMethod;
		ConstructorInfo objectConstructor;
		ConstructorInfo builderConstructor;

		createObjectMethod = bci.getBuilderBuildMethod();
		if (createObjectMethod != null)
			objectClass = createObjectMethod.getReturnType().inner();

		if (objectClass == null)
			return null;

		ClassInfo pci = ClassInfo.of(objectClass);

		objectConstructor = pci.getConstructor(cVis, builderClass);
		if (objectConstructor == null)
			return null;

		builderConstructor = bci.getNoArgConstructor(cVis);
		createBuilderMethod = pci.getBuilderCreateMethod();
		if (builderConstructor == null && createBuilderMethod == null)
			return null;

		return new BuilderSwap(objectClass, builderClass, objectConstructor.inner(), builderConstructor == null ? null : builderConstructor.inner(), createBuilderMethod, createObjectMethod);
	}


	/**
	 * Creates a BuilderSwap from the specified object class if it has one.
	 *
	 * @param bc The bean context to use to look up annotations.
	 * @param objectClass The object class to check.
	 * @param cVis Minimum constructor visibility.
	 * @param mVis Minimum method visibility.
	 * @return A new swap instance, or <jk>null</jk> if class didn't have a builder class.
	 */
	@SuppressWarnings("rawtypes")
	public static BuilderSwap<?,?> findSwapFromObjectClass(BeanContext bc, Class<?> objectClass, Visibility cVis, Visibility mVis) {
		Class<?> builderClass = null;
		MethodInfo objectCreateMethod, builderCreateMethod;
		ConstructorInfo objectConstructor = null;
		ConstructorInfo builderConstructor;

		for (org.apache.juneau.annotation.Builder b : bc.getAnnotations(org.apache.juneau.annotation.Builder.class, objectClass))
			if (b.value() != Null.class)
				builderClass = b.value();

		ClassInfo pci = ClassInfo.of(objectClass);

		builderCreateMethod = pci.getBuilderCreateMethod();

		if (builderClass == null && builderCreateMethod != null)
			builderClass = builderCreateMethod.getReturnType().inner();

		if (builderClass == null) {
			for (ConstructorInfo cc : pci.getPublicConstructors()) {
				if (cc.isVisible(cVis) && cc.hasNumParams(1)) {
					ClassInfo pt = cc.getParamType(0);
					if (pt.isChildOf(Builder.class)) {
						objectConstructor = cc;
						builderClass = pt.inner();
					}
				}
			}
		}

		if (builderClass == null)
			return null;

		ClassInfo bci = ClassInfo.of(builderClass);
		builderConstructor = bci.getNoArgConstructor(cVis);
		if (builderConstructor == null && builderCreateMethod == null)
			return null;

		objectCreateMethod = bci.getBuilderBuildMethod();
		if (objectConstructor == null)
			objectConstructor = pci.getConstructor(cVis, builderClass);

		if (objectConstructor == null && objectCreateMethod == null)
			return null;

		return new BuilderSwap(objectClass, builderClass, objectConstructor == null ? null : objectConstructor.inner(), builderConstructor == null ? null : builderConstructor.inner(), builderCreateMethod, objectCreateMethod);
	}
}