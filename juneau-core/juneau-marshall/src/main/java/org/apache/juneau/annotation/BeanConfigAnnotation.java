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

import static org.apache.juneau.BeanContext.*;
import static java.util.Arrays.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link BeanConfig @BeanConfig} annotation.
 */
public class BeanConfigAnnotation {

	/**
	 * Applies {@link BeanConfig} annotations to a {@link BeanContextBuilder}.
	 */
	public static class Applier extends AnnotationApplier<BeanConfig,BeanContextBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(BeanConfig.class, BeanContextBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<BeanConfig> ai, BeanContextBuilder b) {
			BeanConfig a = ai.getAnnotation();

			visibility(a.beanClassVisibility(), "beanClassVisibility").ifPresent(x -> b.beanClassVisibility(x));
			visibility(a.beanConstructorVisibility(), "beanConstructorVisibility").ifPresent(x -> b.beanConstructorVisibility(x));
			visibility(a.beanFieldVisibility(), "beanFieldVisibility").ifPresent(x -> b.beanFieldVisibility(x));
			visibility(a.beanMethodVisibility(), "beanMethodVisibility").ifPresent(x -> b.beanMethodVisibility(x));
			b.prependTo(BEAN_beanDictionary, a.dictionary());
			b.setIfNotEmpty(BEAN_beanDictionary, a.dictionary_replace());
			bool(a.beanMapPutReturnsOldValue()).ifPresent(x -> b.set(BEAN_beanMapPutReturnsOldValue, x));
			bool(a.beansRequireDefaultConstructor()).ifPresent(x -> b.set(BEAN_beansRequireDefaultConstructor, x));
			bool(a.beansRequireSerializable()).ifPresent(x -> b.set(BEAN_beansRequireSerializable, x));
			bool(a.beansRequireSettersForGetters()).ifPresent(x -> b.set(BEAN_beansRequireSettersForGetters, x));
			bool(a.disableBeansRequireSomeProperties()).ifPresent(x -> b.set(BEAN_disableBeansRequireSomeProperties, x));
			string(a.typePropertyName()).ifPresent(x -> b.set(BEAN_typePropertyName, x));
			bool(a.debug()).ifPresent(x -> b.set(CONTEXT_debug, x));
			bool(a.findFluentSetters()).ifPresent(x -> b.set(BEAN_findFluentSetters, x));
			bool(a.ignoreInvocationExceptionsOnGetters()).ifPresent(x -> b.set(BEAN_ignoreInvocationExceptionsOnGetters, x));
			bool(a.ignoreInvocationExceptionsOnSetters()).ifPresent(x -> b.set(BEAN_ignoreInvocationExceptionsOnSetters, x));
			bool(a.disableIgnoreMissingSetters()).ifPresent(x -> b.set(BEAN_disableIgnoreMissingSetters, x));
			bool(a.disableIgnoreTransientFields()).ifPresent(x -> b.set(BEAN_disableIgnoreTransientFields, x));
			bool(a.ignoreUnknownBeanProperties()).ifPresent(x -> b.set(BEAN_ignoreUnknownBeanProperties, x));
			bool(a.disableIgnoreUnknownNullBeanProperties()).ifPresent(x -> b.set(BEAN_disableIgnoreUnknownNullBeanProperties, x));
			asList(a.interfaces()).stream().map(x -> BeanAnnotation.create(x).interfaceClass(x).build()).forEach(x -> b.annotations(x));
			string(a.locale()).map(Locale::forLanguageTag).ifPresent(x -> b.set(BEAN_locale, x));
			string(a.mediaType()).map(MediaType::of).ifPresent(x -> b.set(BEAN_mediaType, x));
			b.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses());
			b.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses_replace());
			b.addTo(BEAN_notBeanPackages, stringList(a.notBeanPackages()));
			b.setIfNotEmpty(BEAN_notBeanPackages, stringList(a.notBeanPackages_replace()));
			type(a.propertyNamer()).ifPresent(x -> b.set(BEAN_propertyNamer, x));
			bool(a.sortProperties()).ifPresent(x -> b.set(BEAN_sortProperties, x));
			b.prependTo(BEAN_swaps, a.swaps());
			b.setIfNotEmpty(BEAN_swaps, a.swaps_replace());
			string(a.timeZone()).map(TimeZone::getTimeZone).ifPresent(x -> b.set(BEAN_timeZone, x));
			bool(a.useEnumNames()).ifPresent(x -> b.set(BEAN_useEnumNames, x));
			bool(a.disableInterfaceProxies()).ifPresent(x -> b.set(BEAN_disableInterfaceProxies, x));
			bool(a.useJavaBeanIntrospector()).ifPresent(x -> b.set(BEAN_useJavaBeanIntrospector, x));
		}
	}
}
