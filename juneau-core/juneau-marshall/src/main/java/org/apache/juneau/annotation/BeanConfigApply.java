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
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Applies {@link BeanConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class BeanConfigApply extends ConfigApply<BeanConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public BeanConfigApply(Class<BeanConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void apply(AnnotationInfo<BeanConfig> ai, PropertyStoreBuilder psb) {
		BeanConfig a = ai.getAnnotation();
		if (! a.beanClassVisibility().isEmpty())
			psb.set(BEAN_beanClassVisibility, visibility(a.beanClassVisibility(), "beanClassVisibility"));
		if (! a.beanConstructorVisibility().isEmpty())
			psb.set(BEAN_beanConstructorVisibility, visibility(a.beanConstructorVisibility(), "beanConstructorVisibility"));
		if (a.beanDictionary().length != 0)
			psb.addTo(BEAN_beanDictionary, a.beanDictionary());
		if (a.beanDictionary_replace().length != 0)
			psb.set(BEAN_beanDictionary, a.beanDictionary_replace());
		if (a.beanDictionary_remove().length != 0)
			psb.removeFrom(BEAN_beanDictionary, a.beanDictionary_remove());
		if (a.dictionary().length != 0)
			psb.addTo(BEAN_beanDictionary, a.dictionary());
		if (a.dictionary_replace().length != 0)
			psb.set(BEAN_beanDictionary, a.dictionary_replace());
		if (a.dictionary_remove().length != 0)
			psb.removeFrom(BEAN_beanDictionary, a.dictionary_remove());
		if (! a.beanFieldVisibility().isEmpty())
			psb.set(BEAN_beanFieldVisibility, visibility(a.beanFieldVisibility(), "beanFieldVisibility"));
		if (a.beanFilters().length != 0)
			psb.addTo(BEAN_beanFilters, a.beanFilters());
		if (a.beanFilters_replace().length != 0)
			psb.set(BEAN_beanFilters, a.beanFilters_replace());
		if (a.beanFilters_remove().length != 0)
			psb.removeFrom(BEAN_beanFilters, a.beanFilters_remove());
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
		if (! a.beansRequireSomeProperties().isEmpty())
			psb.set(BEAN_beansRequireSomeProperties, bool(a.beansRequireSomeProperties()));
		if (! a.beanTypePropertyName().isEmpty())
			psb.set(BEAN_beanTypePropertyName, string(a.beanTypePropertyName()));
		if (a.bpi().length > 0)
			psb.addTo(BEAN_includeProperties, stringsMap(a.bpi(), "bpi"));
		if (a.bpx().length > 0)
			psb.addTo(BEAN_excludeProperties, stringsMap(a.bpi(), "bpx"));
		if (! a.debug().isEmpty())
			psb.set(BEAN_debug, bool(a.debug()));
		for (CS e : a.example())
			psb.addTo(BEAN_examples, e.k().getName(), parse(e.k(), e.v(), "example"));
		if (a.examples().length > 0)
			psb.addTo(BEAN_examples, objectMap(a.examples(), "examples"));
		for (CS e : a.excludeProperties())
			psb.addTo(BEAN_excludeProperties, e.k().getName(), string(e.v()));
		for (CS e : a.bpxMap())
			psb.addTo(BEAN_excludeProperties, e.k().getName(), string(e.v()));
		if (! a.fluentSetters().isEmpty())
			psb.set(BEAN_fluentSetters, bool(a.fluentSetters()));
		if (! a.ignoreInvocationExceptionsOnGetters().isEmpty())
			psb.set(BEAN_ignoreInvocationExceptionsOnGetters, bool(a.ignoreInvocationExceptionsOnGetters()));
		if (! a.ignoreInvocationExceptionsOnSetters().isEmpty())
			psb.set(BEAN_ignoreInvocationExceptionsOnSetters, bool(a.ignoreInvocationExceptionsOnSetters()));
		if (! a.ignorePropertiesWithoutSetters().isEmpty())
			psb.set(BEAN_ignorePropertiesWithoutSetters, bool(a.ignorePropertiesWithoutSetters()));
		if (! a.ignoreUnknownBeanProperties().isEmpty())
			psb.set(BEAN_ignoreUnknownBeanProperties, bool(a.ignoreUnknownBeanProperties()));
		if (! a.ignoreUnknownNullBeanProperties().isEmpty())
			psb.set(BEAN_ignoreUnknownNullBeanProperties, bool(a.ignoreUnknownNullBeanProperties()));
		for (CC e : a.implClasses())
			psb.addTo(BEAN_implClasses, e.k().getName(), e.v());
		for (CS e : a.includeProperties())
			psb.addTo(BEAN_includeProperties, e.k().getName(), string(e.v()));
		for (CS e : a.bpiMap())
			psb.addTo(BEAN_includeProperties, e.k().getName(), string(e.v()));
		if (! a.locale().isEmpty())
			psb.set(BEAN_locale, locale(a.locale()));
		if (! a.mediaType().isEmpty())
			psb.set(BEAN_mediaType, mediaType(a.mediaType()));
		if (a.notBeanClasses().length != 0)
			psb.addTo(BEAN_notBeanClasses, a.notBeanClasses());
		if (a.notBeanClasses_replace().length != 0)
			psb.set(BEAN_notBeanClasses, a.notBeanClasses_replace());
		if (a.notBeanClasses_remove().length != 0)
			psb.removeFrom(BEAN_notBeanClasses, a.notBeanClasses_remove());
		if (a.notBeanPackages().length != 0)
			psb.addTo(BEAN_notBeanPackages, strings(a.notBeanPackages()));
		if (a.notBeanPackages_replace().length != 0)
			psb.set(BEAN_notBeanPackages, strings(a.notBeanPackages_replace()));
		if (a.notBeanPackages_remove().length != 0)
			psb.removeFrom(BEAN_notBeanPackages, strings(a.notBeanPackages_remove()));
		if (a.pojoSwaps().length != 0)
			psb.addTo(BEAN_pojoSwaps, a.pojoSwaps());
		if (a.pojoSwaps_replace().length != 0)
			psb.set(BEAN_pojoSwaps, a.pojoSwaps_replace());
		if (a.pojoSwaps_remove().length != 0)
			psb.removeFrom(BEAN_pojoSwaps, a.pojoSwaps_remove());
		if (a.propertyNamer() != PropertyNamer.Null.class)
			psb.set(BEAN_propertyNamer, a.propertyNamer());
		if (! a.sortProperties().isEmpty())
			psb.set(BEAN_sortProperties, bool(a.sortProperties()));
		if (! a.timeZone().isEmpty())
			psb.set(BEAN_timeZone, timeZone(a.timeZone()));
		if (! a.useEnumNames().isEmpty())
			psb.set(BEAN_useEnumNames, bool(a.useEnumNames()));
		if (! a.useInterfaceProxies().isEmpty())
			psb.set(BEAN_useInterfaceProxies, bool(a.useInterfaceProxies()));
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
		return MediaType.forString(string(in));
	}

	private TimeZone timeZone(String in) {
		return TimeZone.getTimeZone(string(in));
	}

	private <T> T parse(Class<T> c, String in, String loc) {
		try {
			return SimpleJson.DEFAULT.read(string(in), c);
		} catch (ParseException e) {
			throw new ConfigException("Invalid syntax for visibility on annotation @BeanConfig({0}): {1}", loc, in);
		}
	}
}
