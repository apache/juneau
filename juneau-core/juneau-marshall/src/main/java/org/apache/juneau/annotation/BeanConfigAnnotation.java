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
package org.apache.juneau.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link BeanConfig @BeanConfig} annotation.
 *
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

			// @formatter:off
			string(a.beanClassVisibility()).map(Visibility::valueOf).ifPresent(b::beanClassVisibility);
			string(a.beanConstructorVisibility()).map(Visibility::valueOf).ifPresent(b::beanConstructorVisibility);
			string(a.beanFieldVisibility()).map(Visibility::valueOf).ifPresent(b::beanFieldVisibility);
			string(a.beanMethodVisibility()).map(Visibility::valueOf).ifPresent(b::beanMethodVisibility);
			bool(a.beanMapPutReturnsOldValue()).ifPresent(b::beanMapPutReturnsOldValue);
			bool(a.beansRequireDefaultConstructor()).ifPresent(b::beansRequireDefaultConstructor);
			bool(a.beansRequireSerializable()).ifPresent(b::beansRequireSerializable);
			bool(a.beansRequireSettersForGetters()).ifPresent(b::beansRequireSettersForGetters);
			bool(a.disableBeansRequireSomeProperties()).ifPresent(b::disableBeansRequireSomeProperties);
			bool(a.debug()).ifPresent(b::debug);
			bool(a.findFluentSetters()).ifPresent(b::findFluentSetters);
			bool(a.ignoreInvocationExceptionsOnGetters()).ifPresent(b::ignoreInvocationExceptionsOnGetters);
			bool(a.ignoreInvocationExceptionsOnSetters()).ifPresent(b::ignoreInvocationExceptionsOnSetters);
			bool(a.disableIgnoreMissingSetters()).ifPresent(b::disableIgnoreMissingSetters);
			bool(a.disableIgnoreTransientFields()).ifPresent(b::disableIgnoreTransientFields);
			bool(a.ignoreUnknownBeanProperties()).ifPresent(b::ignoreUnknownBeanProperties);
			bool(a.ignoreUnknownEnumValues()).ifPresent(b::ignoreUnknownEnumValues);
			bool(a.disableIgnoreUnknownNullBeanProperties()).ifPresent(b::disableIgnoreUnknownNullBeanProperties);
			bool(a.sortProperties()).ifPresent(b::sortProperties);
			bool(a.useEnumNames()).ifPresent(b::useEnumNames);
			bool(a.disableInterfaceProxies()).ifPresent(b::disableInterfaceProxies);
			bool(a.useJavaBeanIntrospector()).ifPresent(b::useJavaBeanIntrospector);
			string(a.typePropertyName()).ifPresent(b::typePropertyName);
			string(a.locale()).map(Locale::forLanguageTag).ifPresent(b::locale);
			string(a.mediaType()).map(MediaType::of).ifPresent(b::mediaType);
			string(a.timeZone()).map(TimeZone::getTimeZone).ifPresent(b::timeZone);
			classes(a.dictionary()).ifPresent(b::beanDictionary);
			classes(a.dictionary_replace()).ifPresent(x -> { b.beanDictionary().clear(); b.beanDictionary(x);});
			classes(a.swaps()).ifPresent(b::swaps);
			classes(a.swaps_replace()).ifPresent(x -> { b.swaps().clear(); b.swaps(x);});
			classes(a.notBeanClasses()).ifPresent(b::notBeanClasses);
			classes(a.notBeanClasses_replace()).ifPresent(x -> { b.notBeanClasses().clear(); b.notBeanClasses(x);});
			type(a.propertyNamer()).ifPresent(b::propertyNamer);
			l(a.interfaces()).stream().map(x -> BeanAnnotation.create(x).interfaceClass(x).build()).forEach(b::annotations);
			strings(a.notBeanPackages()).ifPresent(b::notBeanPackages);
			strings(a.notBeanPackages_replace()).ifPresent(x -> {b.notBeanPackages().clear(); b.notBeanPackages(x);});
			// @formatter:on
		}
	}
}