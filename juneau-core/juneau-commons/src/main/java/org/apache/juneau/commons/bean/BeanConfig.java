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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

/**
 * Annotation for specifying bean-modeling config properties on REST classes and methods.
 *
 * <p>
 * The bean-modeling sibling of {@code @MarshalledConfig}.
 * Where {@code @MarshalledConfig} (in <c>juneau-marshall</c>) carries the marshalling-only attributes
 * (such as {@code dictionary}, {@code swaps}, {@code typePropertyName}, {@code locale}, {@code mediaType},
 * and {@code timeZone}), this annotation carries the attributes that describe how the bean MODEL is
 * detected and introspected — visibility settings, required-properties toggles, fluent-setter detection,
 * property naming, not-bean exclusions, etc.
 *
 * <p>
 * Splitting the annotations lets a class/method participate in bean-modeling configuration without
 * dragging in any marshalling concerns, and lets the bean-modeling layer live in <c>juneau-commons</c>
 * independent of <c>juneau-marshall</c>.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Classes and methods (typically REST classes/methods).
 * </ul>
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@SuppressWarnings({
	"java:S100" // Annotation methods use underscore suffix to avoid Java keyword conflicts
})
public @interface BeanConfig {

	/**
	 * Minimum bean class visibility.
	 *
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 *
	 * <p>
	 * For example, if the visibility is <c>PUBLIC</c> and the bean class is <jk>protected</jk>, then the class
	 * will not be interpreted as a bean class and be serialized as a string.
	 * <br>Use this setting to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanClassVisibility() default "";

	/**
	 * Minimum bean constructor visibility.
	 *
	 * <p>
	 * Only look for constructors with the specified minimum visibility.
	 *
	 * <p>
	 * This setting affects the logic for finding no-arg constructors for bean.
	 * <br>Normally, only <jk>public</jk> no-arg constructors are used.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanConstructorVisibility() default "";

	/**
	 * Minimum bean field visibility.
	 *
	 * <p>
	 * Only look for bean fields with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which fields on a bean class are considered bean properties.
	 * <br>Normally only <jk>public</jk> fields are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanFieldVisibility() default "";

	/**
	 * BeanMap.put() returns old property value.
	 *
	 * <p>
	 * If <js>"true"</js>, then the {@code BeanMap.put()} method will return old property values.
	 * <br>Otherwise, it returns <jk>null</jk>.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default because it introduces a slight performance penalty during serialization)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanMapPutReturnsOldValue() default "";

	/**
	 * Minimum bean method visibility.
	 *
	 * <p>
	 * Only look for bean methods with the specified minimum visibility.
	 *
	 * <p>
	 * This affects which methods are detected as getters and setters on a bean class.
	 * <br>Normally only <jk>public</jk> getters and setters are considered.
	 * <br>Use this setting if you want to reduce the visibility requirement.
	 *
	 * <ul class='values'>
	 * 	<li><js>"PUBLIC"</js> (default)
	 * 	<li><js>"PROTECTED"</js>
	 * 	<li><js>"DEFAULT"</js>
	 * 	<li><js>"PRIVATE"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beanMethodVisibility() default "";

	/**
	 * Beans require no-arg constructors.
	 *
	 * <p>
	 * If <js>"true"</js>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beansRequireDefaultConstructor() default "";

	/**
	 * Beans require Serializable interface.
	 *
	 * <p>
	 * If <js>"true"</js>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beansRequireSerializable() default "";

	/**
	 * Beans require setters for getters.
	 *
	 * <p>
	 * If <js>"true"</js>, only getters that have equivalent setters will be considered as properties on a bean.
	 * <br>Otherwise, they will be ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String beansRequireSettersForGetters() default "";

	/**
	 * Beans don't require at least one property.
	 *
	 * <p>
	 * If <js>"true"</js>, then a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * <br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableBeansRequireSomeProperties() default "";

	/**
	 * Don't silently ignore missing setters.
	 *
	 * <p>
	 * If <js>"true"</js>, trying to set a value on a bean property without a setter will throw a {@code BeanRuntimeException}.
	 * <br>Otherwise it will be sliently ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableIgnoreMissingSetters() default "";

	/**
	 * Don't ignore transient fields.
	 *
	 * <p>
	 * If <jk>true</jk>, methods and fields marked as <jk>transient</jk> will not be ignored as bean properties.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableIgnoreTransientFields() default "";

	/**
	 * Don't ignore unknown properties with null values.
	 *
	 * <p>
	 * If <js>"true"</js>, trying to set a <jk>null</jk> value on a non-existent bean property will throw a {@code BeanRuntimeException}.
	 * Otherwise it will be silently ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableIgnoreUnknownNullBeanProperties() default "";

	/**
	 * Don't use interface proxies.
	 *
	 * <p>
	 * Disables the feature where interfaces will be instantiated as proxy classes through the use of an
	 * {@link InvocationHandler} if there is no other way of instantiating them.
	 * <br>Setting this to <js>"true"</js> causes this to be a {@link org.apache.juneau.commons.reflect.BeanRuntimeException}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableInterfaceProxies() default "";

	/**
	 * Find fluent setters.
	 *
	 * <p>
	 * When enabled, fluent setters are detected on beans.
	 *
	 * <p>
	 * Fluent setters must have the following attributes:
	 * <ul>
	 * 	<li>Public.
	 * 	<li>Not static.
	 * 	<li>Take in one parameter.
	 * 	<li>Return the bean itself.
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link BeanType#findFluentSetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String findFluentSetters() default "";

	/**
	 * Ignore invocation errors on getters.
	 *
	 * <p>
	 * If <js>"true"</js>, errors thrown when calling bean getter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreInvocationExceptionsOnGetters() default "";

	/**
	 * Ignore invocation errors on setters.
	 *
	 * <p>
	 * If <js>"true"</js>, errors thrown when calling bean setter methods will silently be ignored.
	 * <br>Otherwise, a {@code BeanRuntimeException} is thrown.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreInvocationExceptionsOnSetters() default "";

	/**
	 * Ignore unknown properties.
	 *
	 * <p>
	 * If <js>"true"</js>, trying to set a value on a non-existent bean property will silently be ignored.
	 * <br>Otherwise, a {@code RuntimeException} is thrown.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreUnknownBeanProperties() default "";

	/**
	 * Schema validation mode.
	 *
	 * <p>
	 * If <js>"true"</js>, bean property values are validated against the constraints declared by
	 * {@link org.apache.juneau.commons.Schema @Schema} annotations on those properties.  Validation runs
	 * during both <b>parsing</b> (value set on the bean) and <b>serialization</b> (value read from the bean).
	 *
	 * <p>
	 * Backed by the typed {@code JsonSchema} bean in {@code juneau-bean-jsonschema} and JSON Schema Draft 2020-12
	 * semantics.  Validation failures throw
	 * {@link org.apache.juneau.commons.httppart.SchemaValidationException} wrapped in {@code BeanRuntimeException};
	 * the parser surfaces them as parse exceptions and the serializer surfaces them as serialize exceptions.
	 *
	 * <p>
	 * If {@code juneau-bean-jsonschema} is not on the classpath, this setting becomes a silent no-op.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 * @since 10.0.0
	 */
	String validateSchema() default "";

	/**
	 * Ignore unknown enum values.
	 *
	 * <p>
	 * If <js>"true"</js>, unknown enum values are set to <jk>null</jk> instead of throwing an exception.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ignoreUnknownEnumValues() default "";

	/**
	 * Identifies a set of interfaces.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization
	 * of implementation classes.  Additional properties on subclasses will be ignored.
	 *
	 * <p class='bjava'>
	 * 	<jc>// Parent class or interface</jc>
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>foo</jf> = <js>"foo"</js>;
	 * 	}
	 *
	 * 	<jc>// Sub class</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>bar</jf> = <js>"bar"</js>;
	 * 	}
	 *
	 * 	<jc>// Apply it to a config</jc>
	 * 	<ja>@BeanConfig</ja>(
	 * 		interfaces={
	 * 			A.<jk>class</jk>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * This annotation can be used on the parent class so that it filters to all child classes, or can be set
	 * individually on the child classes.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>The {@link BeanType#interfaceClass() @BeanType(interfaceClass)} annotation is the equivalent annotation-based solution.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] interfaces() default {};

	/**
	 * Bean class exclusions.
	 *
	 * <p>
	 * List of classes that should not be treated as beans even if they appear to be bean-like.
	 * <br>Not-bean classes are converted to <c>Strings</c> during serialization.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] notBeanClasses() default {};

	/**
	 * Replace classes that should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanClasses()} but replaces any existing value.
	 *
	 * @return The annotation value.
	 */
	Class<?>[] notBeanClasses_replace() default {};

	/**
	 * Bean package exclusions.
	 *
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 *
	 * <p>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 *
	 * <p>
	 * Note that you can specify suffix patterns to include all subpackages.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The default value excludes the following packages:
	 * 		<ul class='compact'>
	 * 			<li class='jp'><c>java.lang</c>
	 * 			<li class='jp'><c>java.lang.annotation</c>
	 * 			<li class='jp'><c>java.lang.ref</c>
	 * 			<li class='jp'><c>java.lang.reflect</c>
	 * 			<li class='jp'><c>java.io</c>
	 * 			<li class='jp'><c>java.net</c>
	 * 			<li class='jp'><c>java.nio.*</c>
	 * 			<li class='jp'><c>java.util.*</c>
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] notBeanPackages() default {};

	/**
	 * Replace packages whose classes should not be considered beans.
	 *
	 * <p>
	 * Same as {@link #notBeanPackages()} but replaces any existing value.
	 *
	 * @return The annotation value.
	 */
	String[] notBeanPackages_replace() default {};

	/**
	 * Bean property namer.
	 *
	 * <p>
	 * The class to use for calculating bean property names.
	 *
	 * <p>
	 * Predefined classes:
	 * <ul>
	 * 	<li>{@link BasicPropertyNamer} (default)
	 * 	<li>{@link PropertyNamerDLC} - Dashed-lower-case names.
	 * 	<li>{@link PropertyNamerULC} - Dashed-upper-case names.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends PropertyNamer> propertyNamer() default PropertyNamer.Void.class;

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	/**
	 * Disable sorted bean properties.
	 *
	 * <p>
	 * When <jk>true</jk>, bean properties are serialized and accessed in natural JVM order instead of the default alphabetical order.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default — properties are sorted alphabetically)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String unsortedProperties() default "";

	/**
	 * Use Java Introspector.
	 *
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * <br>Most {@link BeanType @BeanType} annotations will be ignored.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useJavaBeanIntrospector() default "";
}
