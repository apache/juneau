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

			if (! a.beanClassVisibility().isEmpty())
				psb.set(BEAN_beanClassVisibility, visibility(a.beanClassVisibility(), "beanClassVisibility"));
			if (! a.beanConstructorVisibility().isEmpty())
				psb.set(BEAN_beanConstructorVisibility, visibility(a.beanConstructorVisibility(), "beanConstructorVisibility"));
			if (a.dictionary().length != 0)
				psb.prependTo(BEAN_beanDictionary, a.dictionary());
			if (a.dictionary_replace().length != 0)
				psb.set(BEAN_beanDictionary, a.dictionary_replace());
			if (! a.beanFieldVisibility().isEmpty())
				psb.set(BEAN_beanFieldVisibility, visibility(a.beanFieldVisibility(), "beanFieldVisibility"));
			if (! a.beanMapPutReturnsOldValue().isEmpty())
				psb.set(BEAN_beanMapPutReturnsOldValue, bool(a.beanMapPutReturnsOldValue()));
			if (! a.beanMethodVisibility().isEmpty())
				psb.set(BEAN_beanMethodVisibility, visibility(a.beanMethodVisibility(), "beanMethodVisibility"));
			if (! a.beansRequireDefaultConstructor().isEmpty())
				psb.set(BEAN_beansRequireDefaultConstructor, bool(a.beansRequireDefaultConstructor()));
			if (! a.beansRequireSerializable().isEmpty())
				psb.set(BEAN_beansRequireSerializable, bool(a.beansRequireSerializable()));
			if (! a.beansRequireSettersForGetters().isEmpty())
				psb.set(BEAN_beansRequireSettersForGetters, bool(a.beansRequireSettersForGetters()));
			if (! a.disableBeansRequireSomeProperties().isEmpty())
				psb.set(BEAN_disableBeansRequireSomeProperties, bool(a.disableBeansRequireSomeProperties()));
			if (! a.typePropertyName().isEmpty())
				psb.set(BEAN_typePropertyName, string(a.typePropertyName()));
			if (! a.debug().isEmpty())
				psb.set(CONTEXT_debug, bool(a.debug()));
			if (! a.findFluentSetters().isEmpty())
				psb.set(BEAN_findFluentSetters, bool(a.findFluentSetters()));
			if (! a.ignoreInvocationExceptionsOnGetters().isEmpty())
				psb.set(BEAN_ignoreInvocationExceptionsOnGetters, bool(a.ignoreInvocationExceptionsOnGetters()));
			if (! a.ignoreInvocationExceptionsOnSetters().isEmpty())
				psb.set(BEAN_ignoreInvocationExceptionsOnSetters, bool(a.ignoreInvocationExceptionsOnSetters()));
			if (! a.disableIgnoreMissingSetters().isEmpty())
				psb.set(BEAN_disableIgnoreMissingSetters, bool(a.disableIgnoreMissingSetters()));
			if (! a.disableIgnoreTransientFields().isEmpty())
				psb.set(BEAN_disableIgnoreTransientFields, bool(a.disableIgnoreTransientFields()));
			if (! a.ignoreUnknownBeanProperties().isEmpty())
				psb.set(BEAN_ignoreUnknownBeanProperties, bool(a.ignoreUnknownBeanProperties()));
			if (! a.disableIgnoreUnknownNullBeanProperties().isEmpty())
				psb.set(BEAN_disableIgnoreUnknownNullBeanProperties, bool(a.disableIgnoreUnknownNullBeanProperties()));
			for (Class<?> c : a.interfaces())
				psb.prependTo(BEAN_annotations, BeanAnnotation.create(c).interfaceClass(c).build());
			if (! a.locale().isEmpty())
				psb.set(CONTEXT_locale, locale(a.locale()));
			if (! a.mediaType().isEmpty())
				psb.set(CONTEXT_mediaType, mediaType(a.mediaType()));
			if (a.notBeanClasses().length != 0)
				psb.addTo(BEAN_notBeanClasses, a.notBeanClasses());
			if (a.notBeanClasses_replace().length != 0)
				psb.set(BEAN_notBeanClasses, a.notBeanClasses_replace());
			if (a.notBeanPackages().length != 0)
				psb.addTo(BEAN_notBeanPackages, stringList(a.notBeanPackages()));
			if (a.notBeanPackages_replace().length != 0)
				psb.set(BEAN_notBeanPackages, stringList(a.notBeanPackages_replace()));
			if (a.propertyNamer() != PropertyNamer.Null.class)
				psb.set(BEAN_propertyNamer, a.propertyNamer());
			if (! a.sortProperties().isEmpty())
				psb.set(BEAN_sortProperties, bool(a.sortProperties()));
			if (a.swaps().length != 0)
				psb.prependTo(BEAN_swaps, a.swaps());
			if (a.swaps_replace().length != 0)
				psb.set(BEAN_swaps, a.swaps_replace());
			if (! a.timeZone().isEmpty())
				psb.set(CONTEXT_timeZone, timeZone(a.timeZone()));
			if (! a.useEnumNames().isEmpty())
				psb.set(BEAN_useEnumNames, bool(a.useEnumNames()));
			if (! a.disableInterfaceProxies().isEmpty())
				psb.set(BEAN_disableInterfaceProxies, bool(a.disableInterfaceProxies()));
			if (! a.useJavaBeanIntrospector().isEmpty())
				psb.set(BEAN_useJavaBeanIntrospector, bool(a.useJavaBeanIntrospector()));
			if (! a.detectRecursions().isEmpty())
				psb.set(BEANTRAVERSE_detectRecursions, bool(a.detectRecursions()));
			if (! a.ignoreRecursions().isEmpty())
				psb.set(BEANTRAVERSE_ignoreRecursions, bool(a.ignoreRecursions()));
			if (! a.initialDepth().isEmpty())
				psb.set(BEANTRAVERSE_initialDepth, integer(a.initialDepth(), "initialDepth"));
			if (! a.maxDepth().isEmpty())
				psb.set(BEANTRAVERSE_maxDepth, integer(a.maxDepth(), "maxDepth"));
		}

		private Locale locale(String in) {
			return Locale.forLanguageTag(string(in));
		}

		private MediaType mediaType(String in) {
			return MediaType.of(string(in));
		}

		private TimeZone timeZone(String in) {
			return TimeZone.getTimeZone(string(in));
		}
	}
}
