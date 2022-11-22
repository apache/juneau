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
package org.apache.juneau;


import static org.apache.juneau.collections.JsonMap.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all classes that traverse POJOs.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Base class that serves as the parent class for all serializers and other classes that traverse POJOs.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public abstract class BeanTraverseContext extends BeanContextable {

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public abstract static class Builder extends BeanContextable.Builder {

		boolean detectRecursions, ignoreRecursions;
		int initialDepth, maxDepth;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			detectRecursions = env("BeanTraverseContext.detectRecursions", false);
			ignoreRecursions = env("BeanTraverseContext.ignoreRecursions", false);
			initialDepth = env("BeanTraverseContext.initialDepth", 0);
			maxDepth = env("BeanTraverseContext.maxDepth", 100);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(BeanTraverseContext copyFrom) {
			super(copyFrom);
			detectRecursions = copyFrom.detectRecursions;
			ignoreRecursions = copyFrom.ignoreRecursions;
			initialDepth = copyFrom.initialDepth;
			maxDepth = copyFrom.maxDepth;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			detectRecursions = copyFrom.detectRecursions;
			ignoreRecursions = copyFrom.ignoreRecursions;
			initialDepth = copyFrom.initialDepth;
			maxDepth = copyFrom.maxDepth;
		}

		@Override /* Context.Builder */
		public abstract Builder copy();

		@Override /* Context.Builder */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				detectRecursions,
				ignoreRecursions,
				initialDepth,
				maxDepth
			);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Automatically detect POJO recursions.
		 *
		 * <p>
		 * When enabled, specifies that recursions should be checked for during traversal.
		 *
		 * <p>
		 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
		 * <br>In general, unchecked recursions cause stack-overflow-errors.
		 * <br>These show up as {@link BeanRecursionException BeanRecursionException} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Checking for recursion can cause a small performance penalty.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that automatically checks for recursions.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.detectRecursions()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean();
		 * 	<jv>bean</jv>.<jf>f</jf> = <jv>bean</jv>;
		 *
		 * 	<jc>// Throws a SerializeException and not a StackOverflowError</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>bean</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder detectRecursions() {
			return detectRecursions(true);
		}

		/**
		 * Same as {@link #detectRecursions()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder detectRecursions(boolean value) {
			detectRecursions = value;
			return this;
		}

		/**
		 * Ignore recursion errors.
		 *
		 * <p>
		 * When enabled, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
		 *
		 * <p>
		 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
		 * 	the following when this setting is <jk>true</jk>...
		 *
		 * <p class='bjson'>
		 * 	{A:{B:{C:<jk>null</jk>}}}
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Checking for recursion can cause a small performance penalty.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer ignores recursions.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ignoreRecursions()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean();
		 * 	<jv>bean</jv>.<jf>f</jf> = <jv>bean</jv>;
		 *
		 * 	<jc>// Produces "{f:null}"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jv>bean</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreRecursions() {
			return ignoreRecursions(true);
		}

		/**
		 * Same as {@link #ignoreRecursions()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		@FluentSetter
		public Builder ignoreRecursions(boolean value) {
			ignoreRecursions = value;
			return this;
		}

		/**
		 * Initial depth.
		 *
		 * <p>
		 * The initial indentation level at the root.
		 *
		 * <p>
		 * Useful when constructing document fragments that need to be indented at a certain level when whitespace is enabled.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer with whitespace enabled and an initial depth of 2.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ws()
		 * 		.initialDepth(2)
		 * 		.build();
		 *
		 * 	<jc>// Produces "\t\t{\n\t\t\t'foo':'bar'\n\t\t}\n"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <c>0</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder initialDepth(int value) {
			initialDepth = value;
			return this;
		}

		/**
		 * Max traversal depth.
		 *
		 * <p>
		 * When enabled, abort traversal if specified depth is reached in the POJO tree.
		 *
		 * <p>
		 * If this depth is exceeded, an exception is thrown.
		 *
		 * <p>
		 * This prevents stack overflows from occurring when trying to traverse models with recursive references.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that throws an exception if the depth reaches greater than 20.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.maxDepth(20)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link Builder#maxDepth(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <c>100</c>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder maxDepth(int value) {
			maxDepth = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final int initialDepth, maxDepth;
	final boolean
		detectRecursions,
		ignoreRecursions;

	private final boolean actualDetectRecursions;

	/**
	 * Constructor
	 *
	 * @param builder The builder for this object.
	 */
	protected BeanTraverseContext(Builder builder) {
		super(builder);

		maxDepth = builder.maxDepth;
		initialDepth = builder.initialDepth;
		ignoreRecursions = builder.ignoreRecursions;
		detectRecursions = builder.detectRecursions;

		actualDetectRecursions = detectRecursions || ignoreRecursions || super.isDebug();
	}

	@Override /* Context */
	public abstract Builder copy();

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Automatically detect POJO recursions.
	 *
	 * @see Builder#detectRecursions()
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	public final boolean isDetectRecursions() {
		return actualDetectRecursions;
	}

	/**
	 * Ignore recursion errors.
	 *
	 * @see Builder#ignoreRecursions()
	 * @return
	 * 	<jk>true</jk> if when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * 	<br>Otherwise, an exception is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	public final boolean isIgnoreRecursions() {
		return ignoreRecursions;
	}

	/**
	 * Initial depth.
	 *
	 * @see Builder#initialDepth(int)
	 * @return
	 * 	The initial indentation level at the root.
	 */
	public final int getInitialDepth() {
		return initialDepth;
	}

	/**
	 * Max traversal depth.
	 *
	 * @see Builder#maxDepth(int)
	 * @return
	 * 	The depth at which traversal is aborted if depth is reached in the POJO tree.
	 *	<br>If this depth is exceeded, an exception is thrown.
	 */
	public final int getMaxDepth() {
		return maxDepth;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	protected JsonMap properties() {
		return filteredMap("detectRecursions", detectRecursions, "maxDepth", maxDepth, "ignoreRecursions", ignoreRecursions, "initialDepth", initialDepth);
	}
}
