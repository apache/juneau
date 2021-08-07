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
	 * Applies {@link BeanConfig} annotations to a {@link ContextPropertiesBuilder}.
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
		public void apply(AnnotationInfo<BeanConfig> ai, ContextPropertiesBuilder cpb, VarResolverSession vr) {
			BeanConfig a = ai.getAnnotation();

			cpb.setIfNotEmpty(BEAN_beanClassVisibility, visibility(a.beanClassVisibility(), "beanClassVisibility"));
			cpb.setIfNotEmpty(BEAN_beanConstructorVisibility, visibility(a.beanConstructorVisibility(), "beanConstructorVisibility"));
			cpb.prependTo(BEAN_beanDictionary, a.dictionary());
			cpb.setIfNotEmpty(BEAN_beanDictionary, a.dictionary_replace());
			cpb.setIfNotEmpty(BEAN_beanFieldVisibility, visibility(a.beanFieldVisibility(), "beanFieldVisibility"));
			cpb.setIfNotEmpty(BEAN_beanMapPutReturnsOldValue, bool(a.beanMapPutReturnsOldValue()));
			cpb.setIfNotEmpty(BEAN_beanMethodVisibility, visibility(a.beanMethodVisibility(), "beanMethodVisibility"));
			cpb.setIfNotEmpty(BEAN_beansRequireDefaultConstructor, bool(a.beansRequireDefaultConstructor()));
			cpb.setIfNotEmpty(BEAN_beansRequireSerializable, bool(a.beansRequireSerializable()));
			cpb.setIfNotEmpty(BEAN_beansRequireSettersForGetters, bool(a.beansRequireSettersForGetters()));
			cpb.setIfNotEmpty(BEAN_disableBeansRequireSomeProperties, bool(a.disableBeansRequireSomeProperties()));
			cpb.setIfNotEmpty(BEAN_typePropertyName, string(a.typePropertyName()));
			cpb.setIfNotEmpty(CONTEXT_debug, bool(a.debug()));
			cpb.setIfNotEmpty(BEAN_findFluentSetters, bool(a.findFluentSetters()));
			cpb.setIfNotEmpty(BEAN_ignoreInvocationExceptionsOnGetters, bool(a.ignoreInvocationExceptionsOnGetters()));
			cpb.setIfNotEmpty(BEAN_ignoreInvocationExceptionsOnSetters, bool(a.ignoreInvocationExceptionsOnSetters()));
			cpb.setIfNotEmpty(BEAN_disableIgnoreMissingSetters, bool(a.disableIgnoreMissingSetters()));
			cpb.setIfNotEmpty(BEAN_disableIgnoreTransientFields, bool(a.disableIgnoreTransientFields()));
			cpb.setIfNotEmpty(BEAN_ignoreUnknownBeanProperties, bool(a.ignoreUnknownBeanProperties()));
			cpb.setIfNotEmpty(BEAN_disableIgnoreUnknownNullBeanProperties, bool(a.disableIgnoreUnknownNullBeanProperties()));
			asList(a.interfaces()).stream().map(x -> BeanAnnotation.create(x).interfaceClass(x).build()).forEach(x -> cpb.prependTo(BEAN_annotations, x));
			cpb.setIfNotEmpty(BEAN_locale, locale(a.locale()));
			cpb.setIfNotEmpty(BEAN_mediaType, mediaType(a.mediaType()));
			cpb.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses());
			cpb.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses_replace());
			cpb.addTo(BEAN_notBeanPackages, stringList(a.notBeanPackages()));
			cpb.setIfNotEmpty(BEAN_notBeanPackages, stringList(a.notBeanPackages_replace()));
			cpb.setIf(a.propertyNamer() != PropertyNamer.Null.class, BEAN_propertyNamer, a.propertyNamer());
			cpb.setIfNotEmpty(BEAN_sortProperties, bool(a.sortProperties()));
			cpb.prependTo(BEAN_swaps, a.swaps());
			cpb.setIfNotEmpty(BEAN_swaps, a.swaps_replace());
			cpb.setIfNotEmpty(BEAN_timeZone, timeZone(a.timeZone()));
			cpb.setIfNotEmpty(BEAN_useEnumNames, bool(a.useEnumNames()));
			cpb.setIfNotEmpty(BEAN_disableInterfaceProxies, bool(a.disableInterfaceProxies()));
			cpb.setIfNotEmpty(BEAN_useJavaBeanIntrospector, bool(a.useJavaBeanIntrospector()));
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
