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
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Test-only stand-ins for the marshalling-side SPI seams ({@link BeanTypeResolver}, {@link BeanSession},
 * {@link BeanInfo}) that let {@code juneau-commons} unit tests drive the marshalling-only branches of
 * {@link BeanPropertyMeta} / {@link BeanMeta} without depending on {@code juneau-marshall}.
 *
 * <p>
 * Not a JUnit test class itself (no "Test" in the name), so Surefire's default include patterns skip it.
 */
final class BeanTestFakes {

	private BeanTestFakes() {}

	/**
	 * Minimal {@link BeanInfo} stand-in for a real marshalling-side {@code ClassMeta}.  All {@link ClassInfo}-
	 * inherited behavior (isCollection/isMap/isArray/isAssignableFrom/isObject/etc.) is real reflection against the
	 * wrapped {@link Class}; only the handful of members {@link BeanInfo} itself declares are configurable here.
	 */
	static class FakeBeanInfo<T> extends BeanInfo<T> {

		private boolean uri;
		private boolean bean;
		private BeanInfo<?> elementType;
		private BeanInfo<?> keyType;
		private BeanInfo<?> valueType;
		private boolean canCreateNewInstance = true;
		private Object optionalDefault;
		private Object marshallingContext;
		private final BeanConfigContext beanConfigContext;

		FakeBeanInfo(Class<T> inner) {
			this(inner, BeanConfigContext.DEFAULT);
		}

		FakeBeanInfo(Class<T> inner, BeanConfigContext beanConfigContext) {
			super(inner);
			this.beanConfigContext = beanConfigContext;
		}

		FakeBeanInfo<T> uri(boolean v) { uri = v; return this; }
		FakeBeanInfo<T> bean(boolean v) { bean = v; return this; }
		FakeBeanInfo<T> elementType(BeanInfo<?> v) { elementType = v; return this; }
		FakeBeanInfo<T> keyType(BeanInfo<?> v) { keyType = v; return this; }
		FakeBeanInfo<T> valueType(BeanInfo<?> v) { valueType = v; return this; }
		FakeBeanInfo<T> canCreateNewInstance(boolean v) { canCreateNewInstance = v; return this; }
		FakeBeanInfo<T> optionalDefault(Object v) { optionalDefault = v; return this; }
		FakeBeanInfo<T> marshallingContext(Object v) { marshallingContext = v; return this; }

		@Override public boolean isUri() { return uri; }
		@Override public boolean isBean() { return bean; }
		@Override public BeanInfo<?> getElementType() { return elementType; }
		@Override public BeanInfo<?> getKeyType() { return keyType; }
		@Override public BeanInfo<?> getValueType() { return valueType; }
		@Override public boolean canCreateNewInstance() { return canCreateNewInstance; }
		@Override public boolean canCreateNewInstance(Object outer) { return canCreateNewInstance; }

		@Override
		public T newInstance() throws ExecutableException {
			try {
				var c = inner().getDeclaredConstructor();
				c.setAccessible(true);
				return c.newInstance();
			} catch (Exception e) {
				throw new ExecutableException(e);
			}
		}

		@Override public Object getOptionalDefault() { return optionalDefault; }
		@Override public BeanConfigContext getBeanConfigContext() { return beanConfigContext; }
		@Override public Object getMarshallingContext() { return marshallingContext; }
	}

	/** Minimal {@link BeanTypeResolver} that resolves every requested Java type to a bare {@link FakeBeanInfo}. */
	static class FakeBeanTypeResolver implements BeanTypeResolver {
		private final BeanConfigContext config;

		FakeBeanTypeResolver() { this(BeanConfigContext.DEFAULT); }
		FakeBeanTypeResolver(BeanConfigContext config) { this.config = config; }

		@Override
		public BeanInfo<?> resolveType(AnnotationInfo<BeanProp> lastBeanProp, ClassInfo type, TypeVariables typeVarImpls) {
			return new FakeBeanInfo<>(type.inner(), config);
		}

		@Override
		public BeanInfo<?> objectType() { return new FakeBeanInfo<>(Object.class, config); }

		@Override
		public AnnotationProvider getAnnotationProvider() { return config.getAnnotationProvider(); }
	}

	/** Minimal {@link BeanSession} performing identity conversions — no real marshalling machinery involved. */
	static class FakeBeanSession implements BeanSession {
		@Override public Object convertToType(Object value, Object targetType) { return value; }
		@Override public Object convertToMemberType(Object outer, Object value, Object targetType) { return value; }
		@Override public Map<?,?> parseToMap(CharSequence value) { return new LinkedHashMap<>(); }
		@Override public Collection<?> parseToList(CharSequence value) { return new ArrayList<>(); }
		@Override public <T> Object toBeanMap(T bean) { return BeanMap.of(bean); }
	}

	/**
	 * Minimal {@link BeanFilter} stand-in that lets {@code juneau-commons} tests drive the {@code bf != null}
	 * branches of {@link BeanMeta}'s constructor (fixed/excluded/read-only/write-only property lists, fluent
	 * setters, stop/interface class overrides, property namer override) without depending on the marshalling-side
	 * {@code MarshalledFilter}.  All fields default to "no effect" (empty sets, <jk>null</jk> refs, <jk>false</jk>
	 * flags) so a test only needs to configure the handful of fields it cares about.
	 */
	static class FakeBeanFilter implements BeanFilter {
		private String typeName;
		private String example;
		private ClassInfo implClass;
		private ClassInfo interfaceClass;
		private ClassInfo stopClass;
		private Set<String> properties = Set.of();
		private Set<String> excludeProperties = Set.of();
		private Set<String> readOnlyProperties = Set.of();
		private Set<String> writeOnlyProperties = Set.of();
		private PropertyNamer propertyNamer;
		private List<ClassInfo> beanDictionary = List.of();
		private boolean fluentSetters;
		private boolean unsortedProperties;

		FakeBeanFilter typeName(String v) { typeName = v; return this; }
		FakeBeanFilter example(String v) { example = v; return this; }
		FakeBeanFilter implClass(ClassInfo v) { implClass = v; return this; }
		FakeBeanFilter interfaceClass(ClassInfo v) { interfaceClass = v; return this; }
		FakeBeanFilter stopClass(ClassInfo v) { stopClass = v; return this; }
		FakeBeanFilter properties(String... v) { properties = new LinkedHashSet<>(List.of(v)); return this; }
		FakeBeanFilter excludeProperties(String... v) { excludeProperties = new LinkedHashSet<>(List.of(v)); return this; }
		FakeBeanFilter readOnlyProperties(String... v) { readOnlyProperties = new LinkedHashSet<>(List.of(v)); return this; }
		FakeBeanFilter writeOnlyProperties(String... v) { writeOnlyProperties = new LinkedHashSet<>(List.of(v)); return this; }
		FakeBeanFilter propertyNamer(PropertyNamer v) { propertyNamer = v; return this; }
		FakeBeanFilter beanDictionary(List<ClassInfo> v) { beanDictionary = v; return this; }
		FakeBeanFilter fluentSetters(boolean v) { fluentSetters = v; return this; }
		FakeBeanFilter unsortedProperties(boolean v) { unsortedProperties = v; return this; }

		@Override public ClassInfoTyped<?> getBeanClass() { return null; }
		@Override public String getTypeName() { return typeName; }
		@Override public String getExample() { return example; }
		@Override public ClassInfo getImplClass() { return implClass; }
		@Override public ClassInfo getInterfaceClass() { return interfaceClass; }
		@Override public ClassInfo getStopClass() { return stopClass; }
		@Override public Set<String> getProperties() { return properties; }
		@Override public Set<String> getExcludeProperties() { return excludeProperties; }
		@Override public Set<String> getReadOnlyProperties() { return readOnlyProperties; }
		@Override public Set<String> getWriteOnlyProperties() { return writeOnlyProperties; }
		@Override public PropertyNamer getPropertyNamer() { return propertyNamer; }
		@Override public List<ClassInfo> getBeanDictionary() { return beanDictionary; }
		@Override public boolean isFluentSetters() { return fluentSetters; }
		@Override public boolean isUnsortedProperties() { return unsortedProperties; }
		@Override public Object readProperty(Object bean, String name, Object value) { return value; }
		@Override public Object writeProperty(Object bean, String name, Object value) { return value; }
	}

	/**
	 * Wraps a {@link FakeBeanFilter} in a {@link BeanMetaInitializer} whose {@code buildBeanFilter} always returns
	 * it, delegating every other SPI method to {@link BeanMetaInitializer#NOOP}.  Lets a test drive the
	 * {@code bf != null} construction paths in {@link BeanMeta} via {@link BeanMeta#create(BeanInfo, ClassInfo)}.
	 */
	static BeanMetaInitializer initializerWithFilter(BeanFilter filter) {
		return new BeanMetaInitializer() {
			@Override public boolean hasBeanRegistrationAnnotation(BeanConfigContext config, ClassInfo classInfo) { return BeanMetaInitializer.NOOP.hasBeanRegistrationAnnotation(config, classInfo); }
			@Override public String resolveTypePropertyName(BeanConfigContext config, ClassInfo classInfo) { return BeanMetaInitializer.NOOP.resolveTypePropertyName(config, classInfo); }
			@Override public String findMarshalledTypeName(BeanConfigContext config, ClassInfo classInfo) { return BeanMetaInitializer.NOOP.findMarshalledTypeName(config, classInfo); }
			@Override public BeanRegistryLookup buildBeanRegistry(Object marshallingContext, BeanFilter beanFilter, ClassInfo classInfo, BeanConfigContext config) { return BeanMetaInitializer.NOOP.buildBeanRegistry(marshallingContext, beanFilter, classInfo, config); }
			@Override public BeanRegistryLookup buildPropertyBeanRegistry(Object marshallingContext, BeanRegistryLookup parent, List<ClassInfo> dictionaryClasses) { return BeanMetaInitializer.NOOP.buildPropertyBeanRegistry(marshallingContext, parent, dictionaryClasses); }
			@Override public String findTypeNameInParents(Object marshallingContext, ClassInfo classInfo, Class<?> rawClass) { return BeanMetaInitializer.NOOP.findTypeNameInParents(marshallingContext, classInfo, rawClass); }
			@Override public BeanFilter buildBeanFilter(BeanInfo<?> cm) { return filter; }
		};
	}

	/**
	 * Wraps a fixed type name in a {@link BeanMetaInitializer} whose {@code findTypeNameInParents} always returns
	 * it, delegating every other SPI method to {@link BeanMetaInitializer#NOOP}.  Lets a test drive
	 * {@link BeanMeta}'s {@code findDictionaryName}'s "type name found via parent-class registry lookup"
	 * branch, which the commons-side {@code NOOP} implementation (always <jk>null</jk>) can never exercise.
	 */
	static BeanMetaInitializer initializerWithParentTypeName(String typeName) {
		return new BeanMetaInitializer() {
			@Override public boolean hasBeanRegistrationAnnotation(BeanConfigContext config, ClassInfo classInfo) { return BeanMetaInitializer.NOOP.hasBeanRegistrationAnnotation(config, classInfo); }
			@Override public String resolveTypePropertyName(BeanConfigContext config, ClassInfo classInfo) { return BeanMetaInitializer.NOOP.resolveTypePropertyName(config, classInfo); }
			@Override public String findMarshalledTypeName(BeanConfigContext config, ClassInfo classInfo) { return BeanMetaInitializer.NOOP.findMarshalledTypeName(config, classInfo); }
			@Override public BeanRegistryLookup buildBeanRegistry(Object marshallingContext, BeanFilter beanFilter, ClassInfo classInfo, BeanConfigContext config) { return BeanMetaInitializer.NOOP.buildBeanRegistry(marshallingContext, beanFilter, classInfo, config); }
			@Override public BeanRegistryLookup buildPropertyBeanRegistry(Object marshallingContext, BeanRegistryLookup parent, List<ClassInfo> dictionaryClasses) { return BeanMetaInitializer.NOOP.buildPropertyBeanRegistry(marshallingContext, parent, dictionaryClasses); }
			@Override public String findTypeNameInParents(Object marshallingContext, ClassInfo classInfo, Class<?> rawClass) { return typeName; }
			@Override public BeanFilter buildBeanFilter(BeanInfo<?> cm) { return null; }
		};
	}

	/**
	 * Minimal {@link BeanStore} that resolves a single fixed bean instance for any lookup by type, used to drive
	 * {@link BeanMeta}'s {@code resolveFactory} store-lookup branch.  All other members return "not found"/empty.
	 */
	static class FakeBeanStore implements BeanStore {
		private final Object bean;

		FakeBeanStore(Object bean) { this.bean = bean; }

		@SuppressWarnings("unchecked")
		@Override public <T> Optional<T> getBean(Class<T> beanType) { return beanType.isInstance(bean) ? Optional.of((T) bean) : Optional.empty(); }
		@Override public <T> Optional<T> getBean(Class<T> beanType, String name) { return getBean(beanType); }
		@Override public <T> Map<String,T> getBeansOfType(Class<T> beanType) { return Map.of(); }
		@Override public boolean hasBean(Class<?> beanType) { return beanType.isInstance(bean); }
		@Override public boolean hasBean(Class<?> beanType, String name) { return hasBean(beanType); }
		@Override public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) { return Optional.empty(); }
		@Override public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) { return Optional.empty(); }
	}
}
