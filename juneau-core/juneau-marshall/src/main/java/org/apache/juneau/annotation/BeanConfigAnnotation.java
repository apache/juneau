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
package org.apache.juneau.annotation;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link BeanConfig @BeanConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanConfigAnnotation {

	/**
	 * Applies {@link BeanConfig} annotations to a {@link org.apache.juneau.BeanContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<BeanConfig,BeanContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(BeanConfig.class, BeanContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<BeanConfig> ai, BeanContext.Builder b) {
			BeanConfig a = ai.inner();

			string(a.beanClassVisibility()).map(Visibility::valueOf).ifPresent(x -> b.beanClassVisibility(x));
			string(a.beanConstructorVisibility()).map(Visibility::valueOf).ifPresent(x -> b.beanConstructorVisibility(x));
			string(a.beanFieldVisibility()).map(Visibility::valueOf).ifPresent(x -> b.beanFieldVisibility(x));
			string(a.beanMethodVisibility()).map(Visibility::valueOf).ifPresent(x -> b.beanMethodVisibility(x));
			bool(a.beanMapPutReturnsOldValue()).ifPresent(x -> b.beanMapPutReturnsOldValue(x));
			bool(a.beansRequireDefaultConstructor()).ifPresent(x -> b.beansRequireDefaultConstructor(x));
			bool(a.beansRequireSerializable()).ifPresent(x -> b.beansRequireSerializable(x));
			bool(a.beansRequireSettersForGetters()).ifPresent(x -> b.beansRequireSettersForGetters(x));
			bool(a.disableBeansRequireSomeProperties()).ifPresent(x -> b.disableBeansRequireSomeProperties(x));
			bool(a.debug()).ifPresent(x -> b.debug(x));
			bool(a.findFluentSetters()).ifPresent(x -> b.findFluentSetters(x));
			bool(a.ignoreInvocationExceptionsOnGetters()).ifPresent(x -> b.ignoreInvocationExceptionsOnGetters(x));
			bool(a.ignoreInvocationExceptionsOnSetters()).ifPresent(x -> b.ignoreInvocationExceptionsOnSetters(x));
			bool(a.disableIgnoreMissingSetters()).ifPresent(x -> b.disableIgnoreMissingSetters(x));
			bool(a.disableIgnoreTransientFields()).ifPresent(x -> b.disableIgnoreTransientFields(x));
			bool(a.ignoreUnknownBeanProperties()).ifPresent(x -> b.ignoreUnknownBeanProperties(x));
			bool(a.ignoreUnknownEnumValues()).ifPresent(x -> b.ignoreUnknownEnumValues(x));
			bool(a.disableIgnoreUnknownNullBeanProperties()).ifPresent(x -> b.disableIgnoreUnknownNullBeanProperties(x));
			bool(a.sortProperties()).ifPresent(x -> b.sortProperties(x));
			bool(a.useEnumNames()).ifPresent(x -> b.useEnumNames(x));
			bool(a.disableInterfaceProxies()).ifPresent(x -> b.disableInterfaceProxies(x));
			bool(a.useJavaBeanIntrospector()).ifPresent(x -> b.useJavaBeanIntrospector(x));
			string(a.typePropertyName()).ifPresent(x -> b.typePropertyName(x));
			string(a.locale()).map(Locale::forLanguageTag).ifPresent(x -> b.locale(x));
			string(a.mediaType()).map(MediaType::of).ifPresent(x -> b.mediaType(x));
			string(a.timeZone()).map(TimeZone::getTimeZone).ifPresent(x -> b.timeZone(x));
			classes(a.dictionary()).ifPresent(x -> b.beanDictionary(x));
			classes(a.dictionary_replace()).ifPresent(x -> { b.beanDictionary().clear(); b.beanDictionary(x);});
			classes(a.swaps()).ifPresent(x -> b.swaps(x));
			classes(a.swaps_replace()).ifPresent(x -> { b.swaps().clear(); b.swaps(x);});
			classes(a.notBeanClasses()).ifPresent(x -> b.notBeanClasses(x));
			classes(a.notBeanClasses_replace()).ifPresent(x -> { b.notBeanClasses().clear(); b.notBeanClasses(x);});
			type(a.propertyNamer()).ifPresent(x -> b.propertyNamer(x));
			alist(a.interfaces()).stream().map(x -> BeanAnnotation.create(x).interfaceClass(x).build()).forEach(x -> b.annotations(x));
			strings(a.notBeanPackages()).ifPresent(x -> b.notBeanPackages(x));
			strings(a.notBeanPackages_replace()).ifPresent(x -> {b.notBeanPackages().clear(); b.notBeanPackages(x);});
		}
	}
}
