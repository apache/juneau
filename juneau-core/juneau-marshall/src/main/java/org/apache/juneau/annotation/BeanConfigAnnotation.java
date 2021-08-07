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
	public static class Apply extends ContextApplier<BeanConfig,ContextPropertiesBuilder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(BeanConfig.class, ContextPropertiesBuilder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<BeanConfig> ai, ContextPropertiesBuilder b) {
			BeanConfig a = ai.getAnnotation();

			b.setIfNotEmpty(BEAN_beanClassVisibility, visibility(a.beanClassVisibility(), "beanClassVisibility"));
			b.setIfNotEmpty(BEAN_beanConstructorVisibility, visibility(a.beanConstructorVisibility(), "beanConstructorVisibility"));
			b.prependTo(BEAN_beanDictionary, a.dictionary());
			b.setIfNotEmpty(BEAN_beanDictionary, a.dictionary_replace());
			b.setIfNotEmpty(BEAN_beanFieldVisibility, visibility(a.beanFieldVisibility(), "beanFieldVisibility"));
			b.setIfNotEmpty(BEAN_beanMapPutReturnsOldValue, bool(a.beanMapPutReturnsOldValue()));
			b.setIfNotEmpty(BEAN_beanMethodVisibility, visibility(a.beanMethodVisibility(), "beanMethodVisibility"));
			b.setIfNotEmpty(BEAN_beansRequireDefaultConstructor, bool(a.beansRequireDefaultConstructor()));
			b.setIfNotEmpty(BEAN_beansRequireSerializable, bool(a.beansRequireSerializable()));
			b.setIfNotEmpty(BEAN_beansRequireSettersForGetters, bool(a.beansRequireSettersForGetters()));
			b.setIfNotEmpty(BEAN_disableBeansRequireSomeProperties, bool(a.disableBeansRequireSomeProperties()));
			b.setIfNotEmpty(BEAN_typePropertyName, string(a.typePropertyName()));
			b.setIfNotEmpty(CONTEXT_debug, bool(a.debug()));
			b.setIfNotEmpty(BEAN_findFluentSetters, bool(a.findFluentSetters()));
			b.setIfNotEmpty(BEAN_ignoreInvocationExceptionsOnGetters, bool(a.ignoreInvocationExceptionsOnGetters()));
			b.setIfNotEmpty(BEAN_ignoreInvocationExceptionsOnSetters, bool(a.ignoreInvocationExceptionsOnSetters()));
			b.setIfNotEmpty(BEAN_disableIgnoreMissingSetters, bool(a.disableIgnoreMissingSetters()));
			b.setIfNotEmpty(BEAN_disableIgnoreTransientFields, bool(a.disableIgnoreTransientFields()));
			b.setIfNotEmpty(BEAN_ignoreUnknownBeanProperties, bool(a.ignoreUnknownBeanProperties()));
			b.setIfNotEmpty(BEAN_disableIgnoreUnknownNullBeanProperties, bool(a.disableIgnoreUnknownNullBeanProperties()));
			asList(a.interfaces()).stream().map(x -> BeanAnnotation.create(x).interfaceClass(x).build()).forEach(x -> b.prependTo(BEAN_annotations, x));
			b.setIfNotEmpty(BEAN_locale, locale(a.locale()));
			b.setIfNotEmpty(BEAN_mediaType, mediaType(a.mediaType()));
			b.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses());
			b.setIfNotEmpty(BEAN_notBeanClasses, a.notBeanClasses_replace());
			b.addTo(BEAN_notBeanPackages, stringList(a.notBeanPackages()));
			b.setIfNotEmpty(BEAN_notBeanPackages, stringList(a.notBeanPackages_replace()));
			b.setIf(a.propertyNamer() != PropertyNamer.Null.class, BEAN_propertyNamer, a.propertyNamer());
			b.setIfNotEmpty(BEAN_sortProperties, bool(a.sortProperties()));
			b.prependTo(BEAN_swaps, a.swaps());
			b.setIfNotEmpty(BEAN_swaps, a.swaps_replace());
			b.setIfNotEmpty(BEAN_timeZone, timeZone(a.timeZone()));
			b.setIfNotEmpty(BEAN_useEnumNames, bool(a.useEnumNames()));
			b.setIfNotEmpty(BEAN_disableInterfaceProxies, bool(a.disableInterfaceProxies()));
			b.setIfNotEmpty(BEAN_useJavaBeanIntrospector, bool(a.useJavaBeanIntrospector()));
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
