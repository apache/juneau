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
package org.apache.juneau.commons.bean;

import java.util.*;

import org.apache.juneau.commons.reflect.*;

/**
 * Bean-modeling SPI seam that exposes marshalling-aware {@link BeanMeta} construction hooks.
 *
 * <p>
 * The bean-modeling layer in {@code commons.bean} can construct a {@link BeanMeta} from a {@link Class} plus a
 * {@link BeanConfigContext} alone, but the full marshalling-side construction path also needs to:
 * <ul>
 *  <li>Read {@code @Marshalled} / {@code @BeanType} annotations to detect bean registration, dictionary names,
 *      and the type-property name.
 *  <li>Build a {@code BeanRegistry} for polymorphic dispatch (bean-level and per-property).
 *  <li>Walk the parent class/interface hierarchy to find an inherited dictionary name.
 * </ul>
 *
 * <p>
 * These operations require the marshalling-side {@code MarshallingContext}, so they are exposed here as SPI
 * methods that take the marshalling context as an opaque {@link Object}.  The marshalling-side implementation
 * in {@code MarshalledBeanMetaInitializer} narrows the argument internally; the bean-modeling layer never
 * imports the marshalling-side types.
 *
 * <p>
 * The default {@link #NOOP} implementation is wired on the commons-side path and returns conservative defaults
 * (no marshalling-side help available).  {@code @BeanType} reads remain functional via the configured
 * {@link BeanConfigContext#getAnnotationProvider()}.
 *
 * <h5 class='topic'>Thread safety</h5>
 * Thread safety depends on implementation.
 *
 * @see BeanConfigContext.Builder#beanMetaInitializer(BeanMetaInitializer)
 */
public interface BeanMetaInitializer {

	/**
	 * Singleton commons-side default — no marshalling-side knowledge.
	 *
	 * <p>
	 * Returns conservative defaults: no bean dictionary, no inherited type-names, no {@code @Marshalled} reads.
	 * {@link #hasBeanRegistrationAnnotation(BeanConfigContext, ClassInfo)} still reads {@code @BeanType} via
	 * the supplied {@link BeanConfigContext#getAnnotationProvider() annotation provider}.
	 */
	BeanMetaInitializer NOOP = new BeanMetaInitializer() {
		@Override
		public boolean hasBeanRegistrationAnnotation(BeanConfigContext config, ClassInfo classInfo) {
			return ! config.getAnnotationProvider().find(BeanType.class, classInfo).isEmpty();
		}

		@Override
		public String resolveTypePropertyName(BeanConfigContext config, ClassInfo classInfo) {
			return null;
		}

		@Override
		public String findMarshalledTypeName(BeanConfigContext config, ClassInfo classInfo) {
			return null;
		}

		@Override
		public BeanRegistryLookup buildBeanRegistry(Object marshallingContext, BeanFilter beanFilter, ClassInfo classInfo, BeanConfigContext config) {
			return null;
		}

		@Override
		public BeanRegistryLookup buildPropertyBeanRegistry(Object marshallingContext, BeanRegistryLookup parent, List<ClassInfo> dictionaryClasses) {
			return null;
		}

		@Override
		public String findTypeNameInParents(Object marshallingContext, ClassInfo classInfo, Class<?> rawClass) {
			return null;
		}

		@Override
		public BeanFilter buildBeanFilter(BeanInfo<?> cm) {
			return null;
		}
	};

	/**
	 * Returns <jk>true</jk> if the supplied class has a "registered as bean" annotation (e.g. {@code @Marshalled}
	 * or {@code @BeanType}).  Used to relax constructor-visibility / serializable / class-visibility checks during
	 * bean detection.
	 *
	 * @param config The bean-modeling configuration.
	 * @param classInfo The bean's class info.
	 * @return <jk>true</jk> if the class has a registration annotation.
	 */
	boolean hasBeanRegistrationAnnotation(BeanConfigContext config, ClassInfo classInfo);

	/**
	 * Returns the marshalling-side {@code @Marshalled(typePropertyName)} value for the bean, or <jk>null</jk> if
	 * not set.  Callers fall back to {@link BeanConfigContext#getBeanTypePropertyName()} when this returns <jk>null</jk>.
	 *
	 * @param config The bean-modeling configuration.
	 * @param classInfo The bean's class info.
	 * @return The resolved type property name, or <jk>null</jk>.
	 */
	String resolveTypePropertyName(BeanConfigContext config, ClassInfo classInfo);

	/**
	 * Returns the marshalling-side {@code @Marshalled(typeName)} value for the bean, or <jk>null</jk> if not set.
	 *
	 * @param config The bean-modeling configuration.
	 * @param classInfo The bean's class info.
	 * @return The marshalled type name, or <jk>null</jk>.
	 */
	String findMarshalledTypeName(BeanConfigContext config, ClassInfo classInfo);

	/**
	 * Builds the bean-level {@link BeanRegistryLookup} for the supplied class.  Returns <jk>null</jk> on the
	 * commons-side path (no marshalling context).
	 *
	 * @param marshallingContext The marshalling-side context (as an opaque {@link Object}).  Can be <jk>null</jk>.  The effect is implementation-dependent — see {@link org.apache.juneau.marshall.MarshalledBeanMetaInitializer}, the sole concrete implementer of this SPI.
	 * @param beanFilter The bean filter applied to this bean meta.  Can be <jk>null</jk>.  The effect is implementation-dependent — see {@link org.apache.juneau.marshall.MarshalledBeanMetaInitializer}.
	 * @param classInfo The class info of the bean.  Must not be <jk>null</jk>.
	 * @param config The bean-modeling configuration.  Must not be <jk>null</jk>.
	 * @return The bean-level registry, or <jk>null</jk>.
	 */
	BeanRegistryLookup buildBeanRegistry(Object marshallingContext, BeanFilter beanFilter, ClassInfo classInfo, BeanConfigContext config);

	/**
	 * Builds the per-property {@link BeanRegistryLookup} chained on top of the bean-level registry.
	 *
	 * @param marshallingContext The marshalling-side context.  Must not be <jk>null</jk> on the marshalling path.
	 * @param parent The bean-level registry (chained behind the per-property one).  May be <jk>null</jk>.
	 * @param dictionaryClasses The per-property dictionary class infos.  Must not be <jk>null</jk>.
	 * @return The per-property registry, or <jk>null</jk> when there is no marshalling context.
	 */
	BeanRegistryLookup buildPropertyBeanRegistry(Object marshallingContext, BeanRegistryLookup parent, List<ClassInfo> dictionaryClasses);

	/**
	 * Walks the parent classes/interfaces of {@code classInfo} and returns the first dictionary-name mapping for
	 * {@code rawClass} found in any parent's registry, or <jk>null</jk> if none.
	 *
	 * @param marshallingContext The marshalling-side context.  Can be <jk>null</jk>.  The effect is implementation-dependent — see {@link org.apache.juneau.marshall.MarshalledBeanMetaInitializer}.
	 * @param classInfo The bean's class info.
	 * @param rawClass The raw class to look up.
	 * @return The dictionary name found in a parent's registry, or <jk>null</jk>.
	 */
	String findTypeNameInParents(Object marshallingContext, ClassInfo classInfo, Class<?> rawClass);

	/**
	 * Builds the marshalling-side {@link BeanFilter} for the supplied class, reading {@code @Marshalled} and
	 * {@code @BeanType} annotations.  Returns <jk>null</jk> on the commons-side path (no marshalling-side
	 * filter construction is available).
	 *
	 * @param cm The bean type info.
	 * @return The bean filter, or <jk>null</jk>.
	 */
	BeanFilter buildBeanFilter(BeanInfo<?> cm);
}
