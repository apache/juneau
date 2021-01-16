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
import static org.apache.juneau.BeanTraverseContext.*;
import static java.util.Arrays.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link BeanConfig @BeanConfig} annotation.
 */
public class BeanConfigAnnotation {

	/**
	 * Applies {@link BeanConfig} annotations to a {@link PropertyStoreBuilder}.
	 */
	public static class Apply extends ConfigApply<BeanConfig> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<BeanConfig> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<BeanConfig> ai, PropertyStoreBuilder psb, VarResolverSession vr) {
			BeanConfig a = ai.getAnnotation();

			psb.setIfNotEmpty(BEAN_beanClassVisibility, visibility(a.beanClassVisibility(), "beanClassVisibility"));
			psb.setIfNotEmpty(BEAN_beanConstructorVisibility, visibility(a.beanConstructorVisibility(), "beanConstructorVisibility"));
			psb.prependTo(BEAN_beanDictionary, a.dictionary());
			psb.setIfNotEmpty(BEAN_beanDictionary, a.dictionary_replace());
			psb.setIfNotEmpty(BEAN_beanFieldVisibility, visibility(a.beanFieldVisibility(), "beanFieldVisibility"));
			psb.setIfNotEmpty(BEAN_beanMapPutReturnsOldValue, bool(a.beanMapPutReturnsOldValue()));
			psb.setIfNotEmpty(BEAN_beanMethodVisibility, visibility(a.beanMethodVisibility(), "beanMethodVisibility"));
			psb.setIfNotEmpty(BEAN_beansRequireDefaultConstructor, bool(a.beansRequireDefaultConstructor()));
			psb.setIfNotEmpty(BEAN_beansRequireSerializable, bool(a.beansRequireSerializable()));
			psb.setIfNotEmpty(BEAN_beansRequireSettersForGetters, bool(a.beansRequireSettersForGetters()));
			psb.setIfNotEmpty(BEAN_disableBeansRequireSomeProperties, bool(a.disableBeansRequireSomeProperties()));
			psb.setIfNotEmpty(BEAN_typePropertyName, string(a.typePropertyName()));
			psb.setIfNotEmpty(CONTEXT_debug, bool(a.debug()));
			psb.setIfNotEmpty(BEAN_findFluentSetters, bool(a.findFluentSetters()));
			psb.setIfNotEmpty(BEAN_ignoreInvocationExceptionsOnGetters, bool(a.ignoreInvocationExceptionsOnGetters()));
			psb.setIfNotEmpty(BEAN_ignoreInvocationExceptionsOnSetters, bool(a.ignoreInvocationExceptionsOnSetters()));
			psb.setIfNotEmpty(BEAN_disableIgnoreMissingSetters, bool(a.disableIgnoreMissingSetters()));
			psb.setIfNotEmpty(BEAN_disableIgnoreTransientFields, bool(a.disableIgnoreTransientFields()));
			psb.setIfNotEmpty(BEAN_ignoreUnknownBeanProperties, bool(a.ignoreUnknownBeanProperties()));
			psb.setIfNotEmpty(BEAN_disableIgnoreUnknownNullBeanProperties, bool(a.disableIgnoreUnknownNullBeanProperties()));
			asList(a.interfaces()).stream().map(x -> BeanAnnotation.create(x).interfaceClass(x).build()).forEach(x -> psb.prependTo(BEAN_annotations, x));
			psb.setIfNotEmpty(CONTEXT_locale, locale(a.locale()));
			psb.setIfNotEmpty(CONTEXT_mediaType, mediaType(a.mediaType()));
			psb.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses());
			psb.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses_replace());
			psb.addTo(BEAN_notBeanPackages, stringList(a.notBeanPackages()));
			psb.setIfNotEmpty(BEAN_notBeanPackages, stringList(a.notBeanPackages_replace()));
			psb.setIf(a.propertyNamer() != PropertyNamer.Null.class, BEAN_propertyNamer, a.propertyNamer());
			psb.setIfNotEmpty(BEAN_sortProperties, bool(a.sortProperties()));
			psb.prependTo(BEAN_swaps, a.swaps());
			psb.setIfNotEmpty(BEAN_swaps, a.swaps_replace());
			psb.setIfNotEmpty(CONTEXT_timeZone, timeZone(a.timeZone()));
			psb.setIfNotEmpty(BEAN_useEnumNames, bool(a.useEnumNames()));
			psb.setIfNotEmpty(BEAN_disableInterfaceProxies, bool(a.disableInterfaceProxies()));
			psb.setIfNotEmpty(BEAN_useJavaBeanIntrospector, bool(a.useJavaBeanIntrospector()));
			psb.setIfNotEmpty(BEANTRAVERSE_detectRecursions, bool(a.detectRecursions()));
			psb.setIfNotEmpty(BEANTRAVERSE_ignoreRecursions, bool(a.ignoreRecursions()));
			psb.setIfNotEmpty(BEANTRAVERSE_initialDepth, integer(a.initialDepth(), "initialDepth"));
			psb.setIfNotEmpty(BEANTRAVERSE_maxDepth, integer(a.maxDepth(), "maxDepth"));
		}

		private Locale locale(String in) {
			in = string(in);
			return in == null ? null : Locale.forLanguageTag(in);
		}

		private MediaType mediaType(String in) {
			in = string(in);
			return in == null ? null : MediaType.of(in);
		}

		private TimeZone timeZone(String in) {
			in = string(in);
			return in == null ? null : TimeZone.getTimeZone(in);
		}
	}
}
