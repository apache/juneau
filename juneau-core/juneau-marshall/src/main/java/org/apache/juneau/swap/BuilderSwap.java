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

import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * Specialized transform for builder classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.PojoBuilders">POJO Builders</a>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
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

		createObjectMethod = getBuilderBuildMethod(bci);
		if (createObjectMethod != null)
			objectClass = createObjectMethod.getReturnType().inner();

		if (objectClass == null)
			return null;

		ClassInfo pci = ClassInfo.of(objectClass);

		objectConstructor = pci.getDeclaredConstructor(x -> x.isVisible(cVis) && x.hasParamTypes(builderClass));
		if (objectConstructor == null)
			return null;

		builderConstructor = bci.getNoArgConstructor(cVis);
		createBuilderMethod = getBuilderCreateMethod(pci);
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
		Value<Class<?>> builderClass = Value.empty();
		MethodInfo objectCreateMethod, builderCreateMethod;
		ConstructorInfo objectConstructor = null;
		ConstructorInfo builderConstructor;

		bc.forEachAnnotation(org.apache.juneau.annotation.Builder.class, objectClass, x -> isNotVoid(x.value()), x -> builderClass.set(x.value()));

		ClassInfo pci = ClassInfo.of(objectClass);

		builderCreateMethod = getBuilderCreateMethod(pci);

		if (builderClass.isEmpty() && builderCreateMethod != null)
			builderClass.set(builderCreateMethod.getReturnType().inner());

		if (builderClass.isEmpty()) {
			ConstructorInfo cc = pci.getPublicConstructor(
				x -> x.isVisible(cVis)
				&& x.hasNumParams(1)
				&& x.getParamType(0).isChildOf(Builder.class)
			);
			if (cc != null) {
				objectConstructor = cc;
				builderClass.set(cc.getParamType(0).inner());
			}
		}

		if (builderClass.isEmpty())
			return null;

		ClassInfo bci = ClassInfo.of(builderClass.get());
		builderConstructor = bci.getNoArgConstructor(cVis);
		if (builderConstructor == null && builderCreateMethod == null)
			return null;

		objectCreateMethod = getBuilderBuildMethod(bci);
		Class<?> builderClass2 = builderClass.get();
		if (objectConstructor == null)
			objectConstructor = pci.getDeclaredConstructor(x -> x.isVisible(cVis) && x.hasParamTypes(builderClass2));

		if (objectConstructor == null && objectCreateMethod == null)
			return null;

		return new BuilderSwap(objectClass, builderClass.get(), objectConstructor == null ? null : objectConstructor.inner(), builderConstructor == null ? null : builderConstructor.inner(), builderCreateMethod, objectCreateMethod);
	}

	private static MethodInfo getBuilderCreateMethod(ClassInfo c) {
		return c.getPublicMethod(
			x -> x.isStatic()
			&& x.hasName("create")
			&& ! x.hasReturnType(c)
			&& hasConstructorThatTakesType(c, x.getReturnType())
		);
	}

	private static boolean hasConstructorThatTakesType(ClassInfo c, ClassInfo argType) {
		return c.getPublicConstructor(
			x -> x.hasNumParams(1)
			&& x.hasParamTypes(argType)
		) != null;
	}

	private static MethodInfo getBuilderBuildMethod(ClassInfo c) {
		return c.getDeclaredMethod(
			x -> x.isNotStatic()
			&& x.hasNoParams()
			&& (!x.hasReturnType(void.class))
			&& x.hasName("build")
		);
	}
}