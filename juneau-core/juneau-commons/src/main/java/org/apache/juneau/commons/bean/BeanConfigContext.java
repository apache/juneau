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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Immutable runtime configuration for the bean-modeling layer.
 *
 * <p>
 * {@code BeanConfigContext} is the runtime sibling of the {@link BeanConfig @BeanConfig} annotation:
 * it carries the resolved values that drive how Java types are introspected as beans, independent
 * of any marshalling concern.  Settings include visibility thresholds, required-property toggles,
 * fluent-setter detection, property naming, not-bean exclusions, the active {@link BeanStore} and
 * {@link AnnotationProvider}, and a few related hooks.
 *
 * <p>
 * This type is the bean-modeling counterpart of the marshalling-layer context that lives in
 * {@code juneau-marshall}.  It exists so the bean-modeling runtime (in {@code commons.bean}) can
 * be used independently of the marshalling stack, and so the marshalling layer can compose a
 * {@code BeanConfigContext} snapshot to feed bean-modeling code without exposing marshalling-only
 * APIs.
 *
 * <h5 class='section'>Construction:</h5>
 * <p class='bjava'>
 * 	<jc>// All defaults.</jc>
 * 	BeanConfigContext <jv>defaults</jv> = BeanConfigContext.<jsf>DEFAULT</jsf>;
 *
 * 	<jc>// Customized.</jc>
 * 	BeanConfigContext <jv>ctx</jv> = BeanConfigContext.<jsm>create</jsm>()
 * 		.beanClassVisibility(Visibility.<jsf>PROTECTED</jsf>)
 * 		.findFluentSetters(<jk>true</jk>)
 * 		.propertyNamer(<jk>new</jk> PropertyNamerDLC())
 * 		.build();
 *
 * 	<jc>// Copy and tweak.</jc>
 * 	BeanConfigContext <jv>ctx2</jv> = <jv>ctx</jv>.copy()
 * 		.unsortedProperties(<jk>true</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanConfig @BeanConfig} — the annotation form.
 * </ul>
 */
@SuppressWarnings({
	"java:S107" // Builder.build() invokes a multi-arg constructor; high cardinality is inherent to a configuration POJO.
})
public final class BeanConfigContext {

	/**
	 * Default {@link BeanConfigContext} instance with all settings at their defaults.
	 *
	 * <p>
	 * Equivalent to {@code BeanConfigContext.create().build()}.
	 */
	public static final BeanConfigContext DEFAULT = create().build();

	/**
	 * Creates a new builder for {@link BeanConfigContext}.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Visibility beanClassVisibility;
	private final Visibility beanConstructorVisibility;
	private final Visibility beanFieldVisibility;
	private final Visibility beanMethodVisibility;

	private final boolean beanMapPutReturnsOldValue;
	private final boolean beansRequireDefaultConstructor;
	private final boolean beansRequireSerializable;
	private final boolean beansRequireSettersForGetters;
	private final boolean beansRequireSomeProperties;
	private final boolean findFluentSetters;
	private final boolean ignoreInvocationExceptionsOnGetters;
	private final boolean ignoreInvocationExceptionsOnSetters;
	private final boolean ignoreMissingSetters;
	private final boolean ignoreTransientFields;
	private final boolean ignoreUnknownBeanProperties;
	private final boolean ignoreUnknownNullBeanProperties;
	private final boolean unsortedProperties;
	private final boolean useInterfaceProxies;
	private final boolean useJavaBeanIntrospector;

	private final PropertyNamer propertyNamer;
	private final String beanTypePropertyName;

	private final Set<String> notBeanPackageNames;
	private final Set<String> notBeanPackagePrefixes;
	private final Set<Class<?>> notBeanClasses;

	private final BeanStore beanStore;
	private final AnnotationProvider annotationProvider;
	private final Predicate<ClassInfo> notABeanPredicate;

	private BeanConfigContext(Builder b) {
		beanClassVisibility = b.beanClassVisibility;
		beanConstructorVisibility = b.beanConstructorVisibility;
		beanFieldVisibility = b.beanFieldVisibility;
		beanMethodVisibility = b.beanMethodVisibility;
		beanMapPutReturnsOldValue = b.beanMapPutReturnsOldValue;
		beansRequireDefaultConstructor = b.beansRequireDefaultConstructor;
		beansRequireSerializable = b.beansRequireSerializable;
		beansRequireSettersForGetters = b.beansRequireSettersForGetters;
		beansRequireSomeProperties = b.beansRequireSomeProperties;
		findFluentSetters = b.findFluentSetters;
		ignoreInvocationExceptionsOnGetters = b.ignoreInvocationExceptionsOnGetters;
		ignoreInvocationExceptionsOnSetters = b.ignoreInvocationExceptionsOnSetters;
		ignoreMissingSetters = b.ignoreMissingSetters;
		ignoreTransientFields = b.ignoreTransientFields;
		ignoreUnknownBeanProperties = b.ignoreUnknownBeanProperties;
		ignoreUnknownNullBeanProperties = b.ignoreUnknownNullBeanProperties;
		unsortedProperties = b.unsortedProperties;
		useInterfaceProxies = b.useInterfaceProxies;
		useJavaBeanIntrospector = b.useJavaBeanIntrospector;
		propertyNamer = b.propertyNamer;
		beanTypePropertyName = b.beanTypePropertyName;
		notBeanPackageNames = u(b.notBeanPackageNames);
		notBeanPackagePrefixes = u(b.notBeanPackagePrefixes);
		notBeanClasses = u(b.notBeanClasses);
		beanStore = b.beanStore;
		annotationProvider = b.annotationProvider;
		notABeanPredicate = b.notABeanPredicate;
	}

	/**
	 * Returns a builder pre-populated with the values from this context.
	 *
	 * @return A new builder.
	 */
	public Builder copy() {
		return new Builder(this);
	}

	/**
	 * Minimum bean class visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet this minimum visibility requirement.
	 *
	 * @return The minimum bean class visibility.  Never <jk>null</jk>.
	 */
	public Visibility getBeanClassVisibility() { return beanClassVisibility; }

	/**
	 * Minimum bean constructor visibility.
	 *
	 * @return The minimum bean constructor visibility.  Never <jk>null</jk>.
	 */
	public Visibility getBeanConstructorVisibility() { return beanConstructorVisibility; }

	/**
	 * Minimum bean field visibility.
	 *
	 * @return The minimum bean field visibility.  Never <jk>null</jk>.
	 */
	public Visibility getBeanFieldVisibility() { return beanFieldVisibility; }

	/**
	 * Minimum bean method visibility.
	 *
	 * @return The minimum bean method visibility.  Never <jk>null</jk>.
	 */
	public Visibility getBeanMethodVisibility() { return beanMethodVisibility; }

	/**
	 * Returns whether {@code BeanMap.put(String,Object)} returns the previous property value.
	 *
	 * <p>
	 * When <jk>false</jk> (the default), {@code BeanMap.put(...)} always returns <jk>null</jk> for performance reasons —
	 * the underlying bean's getter is skipped before the setter is invoked.
	 *
	 * @return <jk>true</jk> if old values are returned.
	 */
	public boolean isBeanMapPutReturnsOldValue() { return beanMapPutReturnsOldValue; }

	/**
	 * Returns whether classes must have a no-arg constructor to be considered beans.
	 *
	 * @return <jk>true</jk> if a no-arg constructor is required.
	 */
	public boolean isBeansRequireDefaultConstructor() { return beansRequireDefaultConstructor; }

	/**
	 * Returns whether classes must implement {@link java.io.Serializable} to be considered beans.
	 *
	 * @return <jk>true</jk> if {@code Serializable} is required.
	 */
	public boolean isBeansRequireSerializable() { return beansRequireSerializable; }

	/**
	 * Returns whether bean properties must have a setter to be considered writable from the getter.
	 *
	 * @return <jk>true</jk> if setters are required for getters.
	 */
	public boolean isBeansRequireSettersForGetters() { return beansRequireSettersForGetters; }

	/**
	 * Returns whether classes must have at least one property to be considered beans.
	 *
	 * @return <jk>true</jk> if at least one property is required.
	 */
	public boolean isBeansRequireSomeProperties() { return beansRequireSomeProperties; }

	/**
	 * Returns whether fluent-style setters (returning <c>this</c>) should be detected.
	 *
	 * @return <jk>true</jk> if fluent setters are detected.
	 */
	public boolean isFindFluentSetters() { return findFluentSetters; }

	/**
	 * Returns whether exceptions thrown from bean property getters should be silently swallowed.
	 *
	 * <p>
	 * When <jk>true</jk>, an exception thrown by a getter is treated as a <jk>null</jk> property value rather than
	 * propagated to the caller.
	 *
	 * @return <jk>true</jk> if getter invocation exceptions are ignored.
	 */
	public boolean isIgnoreInvocationExceptionsOnGetters() { return ignoreInvocationExceptionsOnGetters; }

	/**
	 * Returns whether exceptions thrown from bean property setters should be silently swallowed.
	 *
	 * <p>
	 * When <jk>true</jk>, an exception thrown by a setter is suppressed rather than propagated to the caller.
	 *
	 * @return <jk>true</jk> if setter invocation exceptions are ignored.
	 */
	public boolean isIgnoreInvocationExceptionsOnSetters() { return ignoreInvocationExceptionsOnSetters; }

	/**
	 * Returns whether bean properties without setters should be silently ignored during deserialization.
	 *
	 * @return <jk>true</jk> if missing setters are ignored.
	 */
	public boolean isIgnoreMissingSetters() { return ignoreMissingSetters; }

	/**
	 * Returns whether {@code transient} fields should be excluded from bean property detection.
	 *
	 * @return <jk>true</jk> if transient fields are ignored.
	 */
	public boolean isIgnoreTransientFields() { return ignoreTransientFields; }

	/**
	 * Returns whether unknown properties on incoming bean payloads should be ignored.
	 *
	 * @return <jk>true</jk> if unknown properties are ignored.
	 */
	public boolean isIgnoreUnknownBeanProperties() { return ignoreUnknownBeanProperties; }

	/**
	 * Returns whether attempts to set unknown bean properties to <jk>null</jk> should be silently ignored.
	 *
	 * <p>
	 * When <jk>true</jk> (the default), trying to set an unknown property to <jk>null</jk> is a no-op rather than an
	 * error.  When <jk>false</jk>, the property setter still rejects unknown property names.
	 *
	 * @return <jk>true</jk> if unknown null properties are silently ignored.
	 */
	public boolean isIgnoreUnknownNullBeanProperties() { return ignoreUnknownNullBeanProperties; }

	/**
	 * Returns whether properties should preserve their JVM-discovered (non-alphabetical) order.
	 *
	 * @return <jk>true</jk> if properties remain unsorted.
	 */
	public boolean isUnsortedProperties() { return unsortedProperties; }

	/**
	 * Returns whether interface proxies should be created for bean interfaces.
	 *
	 * @return <jk>true</jk> if interface proxies are enabled.
	 */
	public boolean isUseInterfaceProxies() { return useInterfaceProxies; }

	/**
	 * Returns whether {@link java.beans.Introspector} should be used to discover bean properties.
	 *
	 * @return <jk>true</jk> if the JavaBeans introspector is used.
	 */
	public boolean isUseJavaBeanIntrospector() { return useJavaBeanIntrospector; }

	/**
	 * Returns the {@link PropertyNamer} used to derive property names from getter/setter/field names.
	 *
	 * @return The active property namer.  Never <jk>null</jk>.
	 */
	public PropertyNamer getPropertyNamer() { return propertyNamer; }

	/**
	 * Returns the property name used to embed the bean dictionary type name (default: <js>"_type"</js>).
	 *
	 * @return The bean type property name.  Never <jk>null</jk>.
	 */
	public String getBeanTypePropertyName() { return beanTypePropertyName; }

	/**
	 * Returns the set of fully qualified package names whose classes are excluded from bean detection.
	 *
	 * @return The not-a-bean package name set.  Never <jk>null</jk>.  Unmodifiable.
	 */
	public Set<String> getNotBeanPackageNames() { return notBeanPackageNames; }

	/**
	 * Returns the set of fully qualified package prefixes whose classes are excluded from bean detection.
	 *
	 * @return The not-a-bean package prefix set.  Never <jk>null</jk>.  Unmodifiable.
	 */
	public Set<String> getNotBeanPackagePrefixes() { return notBeanPackagePrefixes; }

	/**
	 * Returns the set of classes (and supertypes) that are explicitly excluded from bean detection.
	 *
	 * @return The not-a-bean class set.  Never <jk>null</jk>.  Unmodifiable.
	 */
	public Set<Class<?>> getNotBeanClasses() { return notBeanClasses; }

	/**
	 * Returns the active {@link BeanStore} used for factory-based bean instantiation, or <jk>null</jk> if none configured.
	 *
	 * @return The bean store, or <jk>null</jk>.
	 */
	public BeanStore getBeanStore() { return beanStore; }

	/**
	 * Returns the {@link AnnotationProvider} used to discover annotations on classes/methods/fields/parameters.
	 *
	 * @return The annotation provider.  Never <jk>null</jk>.
	 */
	public AnnotationProvider getAnnotationProvider() { return annotationProvider; }

	/**
	 * Returns <jk>true</jk> if the specified class is excluded from bean detection.
	 *
	 * <p>
	 * The default implementation checks {@link #getNotBeanClasses()}, {@link #getNotBeanPackageNames()}, and
	 * {@link #getNotBeanPackagePrefixes()}, and rejects arrays/primitives/enums/annotations.  A custom
	 * predicate (set via {@link Builder#notABeanPredicate(Predicate)}) overrides this behavior entirely.
	 *
	 * @param ci The class info being tested.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if the class is excluded from bean detection.
	 */
	public boolean isNotABean(ClassInfo ci) {
		assertArgNotNull("ci", ci);
		if (notABeanPredicate != null)
			return notABeanPredicate.test(ci);
		if (ci.isArray() || ci.isPrimitive() || ci.isEnum() || ci.isAnnotation())
			return true;
		var p = ci.getPackage();
		if (nn(p)) {
			var pn = p.getName();
			for (var p2 : notBeanPackageNames)
				if (pn.equals(p2))
					return true;
			for (var p2 : notBeanPackagePrefixes)
				if (pn.startsWith(p2))
					return true;
		}
		for (var exclude : notBeanClasses)
			if (ci.isAssignableTo(exclude))
				return true;
		return false;
	}

	/**
	 * Builder for {@link BeanConfigContext}.
	 */
	public static final class Builder {

		private Visibility beanClassVisibility = Visibility.PUBLIC;
		private Visibility beanConstructorVisibility = Visibility.PUBLIC;
		private Visibility beanFieldVisibility = Visibility.PUBLIC;
		private Visibility beanMethodVisibility = Visibility.PUBLIC;

		private boolean beanMapPutReturnsOldValue;
		private boolean beansRequireDefaultConstructor;
		private boolean beansRequireSerializable;
		private boolean beansRequireSettersForGetters;
		private boolean beansRequireSomeProperties = true;
		private boolean findFluentSetters;
		private boolean ignoreInvocationExceptionsOnGetters;
		private boolean ignoreInvocationExceptionsOnSetters;
		private boolean ignoreMissingSetters = true;
		private boolean ignoreTransientFields = true;
		private boolean ignoreUnknownBeanProperties;
		private boolean ignoreUnknownNullBeanProperties = true;
		private boolean unsortedProperties;
		private boolean useInterfaceProxies = true;
		private boolean useJavaBeanIntrospector;

		private PropertyNamer propertyNamer = new BasicPropertyNamer();
		private String beanTypePropertyName = "_type";

		private Set<String> notBeanPackageNames = new LinkedHashSet<>();
		private Set<String> notBeanPackagePrefixes = new LinkedHashSet<>();
		private Set<Class<?>> notBeanClasses = new LinkedHashSet<>();

		private BeanStore beanStore;
		private AnnotationProvider annotationProvider = AnnotationProvider.INSTANCE;
		private Predicate<ClassInfo> notABeanPredicate;

		private Builder() {}

		private Builder(BeanConfigContext src) {
			beanClassVisibility = src.beanClassVisibility;
			beanConstructorVisibility = src.beanConstructorVisibility;
			beanFieldVisibility = src.beanFieldVisibility;
			beanMethodVisibility = src.beanMethodVisibility;
			beanMapPutReturnsOldValue = src.beanMapPutReturnsOldValue;
			beansRequireDefaultConstructor = src.beansRequireDefaultConstructor;
			beansRequireSerializable = src.beansRequireSerializable;
			beansRequireSettersForGetters = src.beansRequireSettersForGetters;
			beansRequireSomeProperties = src.beansRequireSomeProperties;
			findFluentSetters = src.findFluentSetters;
			ignoreInvocationExceptionsOnGetters = src.ignoreInvocationExceptionsOnGetters;
			ignoreInvocationExceptionsOnSetters = src.ignoreInvocationExceptionsOnSetters;
			ignoreMissingSetters = src.ignoreMissingSetters;
			ignoreTransientFields = src.ignoreTransientFields;
			ignoreUnknownBeanProperties = src.ignoreUnknownBeanProperties;
			ignoreUnknownNullBeanProperties = src.ignoreUnknownNullBeanProperties;
			unsortedProperties = src.unsortedProperties;
			useInterfaceProxies = src.useInterfaceProxies;
			useJavaBeanIntrospector = src.useJavaBeanIntrospector;
			propertyNamer = src.propertyNamer;
			beanTypePropertyName = src.beanTypePropertyName;
			notBeanPackageNames = new LinkedHashSet<>(src.notBeanPackageNames);
			notBeanPackagePrefixes = new LinkedHashSet<>(src.notBeanPackagePrefixes);
			notBeanClasses = new LinkedHashSet<>(src.notBeanClasses);
			beanStore = src.beanStore;
			annotationProvider = src.annotationProvider;
			notABeanPredicate = src.notABeanPredicate;
		}

		/**
		 * Builds a new {@link BeanConfigContext} from this builder's state.
		 *
		 * @return A new immutable {@link BeanConfigContext}.
		 */
		public BeanConfigContext build() {
			return new BeanConfigContext(this);
		}

		/**
		 * Sets the minimum bean class visibility.
		 *
		 * @param value The visibility threshold.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanClassVisibility(Visibility value) { beanClassVisibility = assertArgNotNull("value", value); return this; }

		/**
		 * Sets the minimum bean constructor visibility.
		 *
		 * @param value The visibility threshold.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanConstructorVisibility(Visibility value) { beanConstructorVisibility = assertArgNotNull("value", value); return this; }

		/**
		 * Sets the minimum bean field visibility.
		 *
		 * @param value The visibility threshold.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanFieldVisibility(Visibility value) { beanFieldVisibility = assertArgNotNull("value", value); return this; }

		/**
		 * Sets the minimum bean method visibility.
		 *
		 * @param value The visibility threshold.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanMethodVisibility(Visibility value) { beanMethodVisibility = assertArgNotNull("value", value); return this; }

		/**
		 * Toggles whether {@code BeanMap.put(String,Object)} returns the previous value rather than <jk>null</jk>.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder beanMapPutReturnsOldValue(boolean value) { beanMapPutReturnsOldValue = value; return this; }

		/**
		 * Toggles the requirement that beans have a no-arg default constructor.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder beansRequireDefaultConstructor(boolean value) { beansRequireDefaultConstructor = value; return this; }

		/**
		 * Toggles the requirement that beans implement {@link java.io.Serializable}.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder beansRequireSerializable(boolean value) { beansRequireSerializable = value; return this; }

		/**
		 * Toggles the requirement that bean getters have matching setters.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder beansRequireSettersForGetters(boolean value) { beansRequireSettersForGetters = value; return this; }

		/**
		 * Toggles the requirement that beans expose at least one property.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder beansRequireSomeProperties(boolean value) { beansRequireSomeProperties = value; return this; }

		/**
		 * Toggles fluent-setter detection (setters that return <c>this</c>).
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder findFluentSetters(boolean value) { findFluentSetters = value; return this; }

		/**
		 * Toggles silent suppression of exceptions thrown by bean property getters.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder ignoreInvocationExceptionsOnGetters(boolean value) { ignoreInvocationExceptionsOnGetters = value; return this; }

		/**
		 * Toggles silent suppression of exceptions thrown by bean property setters.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder ignoreInvocationExceptionsOnSetters(boolean value) { ignoreInvocationExceptionsOnSetters = value; return this; }

		/**
		 * Toggles silent ignoring of properties without setters during deserialization.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder ignoreMissingSetters(boolean value) { ignoreMissingSetters = value; return this; }

		/**
		 * Toggles exclusion of {@code transient} fields from bean property detection.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder ignoreTransientFields(boolean value) { ignoreTransientFields = value; return this; }

		/**
		 * Toggles silent ignoring of unknown properties on incoming bean payloads.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder ignoreUnknownBeanProperties(boolean value) { ignoreUnknownBeanProperties = value; return this; }

		/**
		 * Toggles silent ignoring of attempts to set unknown bean properties to <jk>null</jk>.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder ignoreUnknownNullBeanProperties(boolean value) { ignoreUnknownNullBeanProperties = value; return this; }

		/**
		 * Toggles whether properties remain in JVM-discovered (non-alphabetical) order.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder unsortedProperties(boolean value) { unsortedProperties = value; return this; }

		/**
		 * Toggles automatic creation of interface proxies for bean interfaces.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder useInterfaceProxies(boolean value) { useInterfaceProxies = value; return this; }

		/**
		 * Toggles use of {@link java.beans.Introspector} for property discovery.
		 *
		 * @param value The new value.
		 * @return This object.
		 */
		public Builder useJavaBeanIntrospector(boolean value) { useJavaBeanIntrospector = value; return this; }

		/**
		 * Sets the {@link PropertyNamer} used to derive property names.
		 *
		 * @param value The property namer.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder propertyNamer(PropertyNamer value) { propertyNamer = assertArgNotNull("value", value); return this; }

		/**
		 * Sets the property name used to embed the bean dictionary type (default: <js>"_type"</js>).
		 *
		 * @param value The property name.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanTypePropertyName(String value) { beanTypePropertyName = assertArgNotNull("value", value); return this; }

		/**
		 * Adds package names whose classes should be excluded from bean detection.
		 *
		 * @param values The package names.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder notBeanPackageNames(String...values) {
			assertArgNotNull("values", values);
			Collections.addAll(notBeanPackageNames, values);
			return this;
		}

		/**
		 * Adds package prefixes whose classes should be excluded from bean detection.
		 *
		 * @param values The package prefixes.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder notBeanPackagePrefixes(String...values) {
			assertArgNotNull("values", values);
			Collections.addAll(notBeanPackagePrefixes, values);
			return this;
		}

		/**
		 * Adds classes (and supertypes) that should be excluded from bean detection.
		 *
		 * @param values The classes.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder notBeanClasses(Class<?>...values) {
			assertArgNotNull("values", values);
			Collections.addAll(notBeanClasses, values);
			return this;
		}

		/**
		 * Replaces the not-a-bean package name set.
		 *
		 * @param values The package names.  May be <jk>null</jk> for an empty set.
		 * @return This object.
		 */
		public Builder notBeanPackageNames(Collection<String> values) {
			notBeanPackageNames = new LinkedHashSet<>(values == null ? sete() : values);
			return this;
		}

		/**
		 * Replaces the not-a-bean package prefix set.
		 *
		 * @param values The package prefixes.  May be <jk>null</jk> for an empty set.
		 * @return This object.
		 */
		public Builder notBeanPackagePrefixes(Collection<String> values) {
			notBeanPackagePrefixes = new LinkedHashSet<>(values == null ? sete() : values);
			return this;
		}

		/**
		 * Replaces the not-a-bean class set.
		 *
		 * @param values The classes.  May be <jk>null</jk> for an empty set.
		 * @return This object.
		 */
		public Builder notBeanClasses(Collection<? extends Class<?>> values) {
			notBeanClasses = new LinkedHashSet<>(values == null ? sete() : values);
			return this;
		}

		/**
		 * Sets the active {@link BeanStore} used for factory-based bean instantiation.
		 *
		 * @param value The bean store.  May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder beanStore(BeanStore value) { beanStore = value; return this; }

		/**
		 * Sets the active {@link AnnotationProvider}.
		 *
		 * @param value The annotation provider.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder annotationProvider(AnnotationProvider value) { annotationProvider = assertArgNotNull("value", value); return this; }

		/**
		 * Installs a custom predicate that fully overrides {@link BeanConfigContext#isNotABean(ClassInfo)}.
		 *
		 * <p>
		 * When set, the default not-bean computation (package/class exclusions, array/primitive/enum/annotation
		 * checks) is bypassed entirely.  Pass <jk>null</jk> to revert to the built-in behavior.
		 *
		 * @param value The predicate.  May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder notABeanPredicate(Predicate<ClassInfo> value) { notABeanPredicate = value; return this; }
	}
}
