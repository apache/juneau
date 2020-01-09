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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.beans.*;
import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;

/**
 * Used to tailor how beans get interpreted by the framework.
 *
 * <p>
 * This annotation can be applied to classes and interfaces.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.Transforms.BeanAnnotation}
 * </ul>
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface Bean {

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary for all properties in this class and all subclasses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 * @deprecated Use {@link #dictionary()}.
	 */
	@Deprecated
	Class<?>[] beanDictionary() default {};

	/**
	 * Bean property includes.
	 *
	 * <p>
	 * The set and order of names of properties associated with a bean class.
	 *
	 * <p>
	 * The order specified is the same order that the entries will be returned by the {@link BeanMap#entrySet()} and
	 * related methods.
	 *
	 * <p>
	 * This value is entirely optional if you simply want to expose all the getters and public fields on
	 * a class as bean properties.
	 * <br>However, it's useful if you want certain getters to be ignored or you want the properties to be
	 * serialized in a particular order.
	 * <br>Note that on IBM JREs, the property order is the same as the order in the source code,
	 * whereas on Oracle JREs, the order is entirely random.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Address class with only street/city/state properties (in that order).</jc>
	 * 	<ja>@Bean</ja>(bpi=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_bpi}
	 * </ul>
	 */
	String bpi() default "";

	/**
	 * Bean property excludes.
	 *
	 * <p>
	 * Specifies a list of properties that should be excluded from {@link BeanMap#entrySet()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from the Address class.</jc>
	 * 	<ja>@Bean</ja>(bpx=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_bpx}
	 * </ul>
	 */
	String bpx() default "";

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from being parsed, but not serialized.</jc>
	 * 	<ja>@Bean</ja>(bpro=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_bpro}
	 * </ul>
	 */
	String bpro() default "";

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from being serialized, but not parsed.</jc>
	 * 	<ja>@Bean</ja>(bpro=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_bpwo}
	 * </ul>
	 */
	String bpwo() default "";

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary for all properties in this class and all subclasses.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 */
	Class<?>[] dictionary() default {};

	/**
	 * Specifies a list of properties that should be excluded from {@link BeanMap#entrySet()}.
	 *
	 * @deprecated Use {@link #bpx()}
	 */
	@Deprecated String excludeProperties() default "";

	/**
	 * Find fluent setters.
	 *
	 * <p>
	 * When <jk>true</jk>, fluent setters will be detected on beans.
	 *
	 * <p>
	 * Fluent setters
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Bean</ja>(fluentSetters=<jk>true</jk>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public int</jk> getId() {...}
	 * 		<jk>public</jk> MyBean id(<jk>int</jk> id) {...}
	 * 	}
	 * </p>
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
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_fluentSetters}
	 * </ul>
	 */
	boolean fluentSetters() default false;

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Parent class</jc>
	 * 	<ja>@Bean</ja>(interfaceClass=A.<jk>class</jk>)
	 * 	<jk>public abstract class</jk> A {
	 * 		<jk>public</jk> String <jf>f0</jf> = <js>"f0"</js>;
	 * 	}
	 *
	 * 	<jc>// Sub class</jc>
	 * 	<jk>public class</jk> A1 <jk>extends</jk> A {
	 * 		<jk>public</jk> String <jf>f1</jf> = <js>"f1"</js>;
	 * 	}
	 *
	 * 	<jc>// Produces "{f0:'f0'}"</jc>
	 * 	String json = SimpleJsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jk>new</jk> A1());
	 * </p>
	 *
	 * <p>
	 * Note that this annotation can be used on the parent class so that it filters to all child classes,
	 * or can be set individually on the child classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 */
	Class<?> interfaceClass() default Object.class;

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Used in conjunction with the {@link BeanConfig#applyBean()}.
	 * It is ignored when the annotation is applied directly to classes.
	 *
	 * <p>
	 * The following example shows the equivalent methods for applying the {@link Bean @Bean} annotation to REST methods:
	 * <p class='bpcode w800'>
	 * 	<jc>// Class with explicit annotation.</jc>
	 * 	<ja>@Bean</ja>(bpi=<jk>"street,city,state"</js>)
	 * 	<jk>public class</jk> A {...}
	 *
	 * 	<jc>// Class with annotation applied via @BeanConfig</jc>
	 * 	<jk>public class</jk> B {...}
	 *
	 * 	<jc>// Java REST method with @BeanConfig annotation.</jc>
	 * 	<ja>@RestMethod</ja>(...)
	 * 	<ja>@BeanConfig</ja>(
	 * 		applyBean={
	 * 			<ja>@Bean</ja>(on=<js>"B"</js>, bpi=<jk>"street,city,state"</js>)
	 * 		}
	 * 	)
	 * 	<jk>public void</jk> doFoo() {...}
	 * </p>
	 *
	 * The valid pattern matches are:
	 * <ul>
	 * 	<li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass"</js>
	 * 			<li>Fully qualified inner class: <js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 			<li>Simple: <js>"MyClass"</js>
	 * 			<li>Simple inner: <js>"MyClass$Inner1$Inner2"</js> or <js>"Inner1$Inner2"</js> or <js>"Inner2"</js>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc juneau-marshall.DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String on() default "";

	/**
	 * The set and order of names of properties associated with a bean class.
	 *
	 * @deprecated Use {@link #bpi()}
	 */
	@Deprecated String properties() default "";

	/**
	 * Property filter.
	 *
	 * <p>
	 * Property filters can be used to intercept calls to getters and setters and alter their values in transit.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link PropertyFilter}
	 * </ul>
	 */
	Class<? extends PropertyFilter> propertyFilter() default PropertyFilter.class;

	/**
	 * Associates a {@link PropertyNamer} with this bean to tailor the names of the bean properties.
	 *
	 * <p>
	 * Property namers are used to transform bean property names from standard form to some other form.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define a class with dashed-lowercase property names.</jc>
	 * 	<ja>@Bean</ja>(propertyNamer=PropertyNamerDashedLC.<jk>class</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_propertyNamer}
	 * </ul>
	 */
	Class<? extends PropertyNamer> propertyNamer() default PropertyNamerDefault.class;

	/**
	 * Sort bean properties in alphabetical order.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * <br>Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Sort bean properties alphabetically during serialization.</jc>
	 * 	<ja>@Bean</ja>(sort=<jk>true</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_sortProperties}
	 * </ul>
	 */
	boolean sort() default false;

	/**
	 * Identifies a stop class for the annotated class.
	 *
	 * <p>
	 * Identical in purpose to the stop class specified by {@link Introspector#getBeanInfo(Class, Class)}.
	 * Any properties in the stop class or in its base classes will be ignored during analysis.
	 *
	 * <p>
	 * For example, in the following class hierarchy, instances of <c>C3</c> will include property <c>p3</c>,
	 * but not <c>p1</c> or <c>p2</c>.
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> C1 {
	 * 		<jk>public int</jk> getP1();
	 * 	}
	 *
	 * 	<jk>public class</jk> C2 <jk>extends</jk> C1 {
	 * 		<jk>public int</jk> getP2();
	 * 	}
	 *
	 * 	<ja>@Bean</ja>(stopClass=C2.<jk>class</jk>)
	 * 	<jk>public class</jk> C3 <jk>extends</jk> C2 {
	 * 		<jk>public int</jk> getP3();
	 * 	}
	 * </p>
	 */
	Class<?> stopClass() default Object.class;

	/**
	 * An identifying name for this class.
	 *
	 * <p>
	 * The name is used to identify the class type during parsing when it cannot be inferred through reflection.
	 * <br>For example, if a bean property is of type <c>Object</c>, then the serializer will add the name to the
	 * output so that the class can be determined during parsing.
	 *
	 * <p>
	 * It is also used to specify element names in XML.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Use _type='mybean' to identify this bean.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 */
	String typeName() default "";

	/**
	 * The property name to use for representing the type name.
	 *
	 * <p>
	 * This can be used to override the name used for the <js>"_type"</js> property used by the {@link #typeName()} setting.
	 *
	 * <p>
	 * The default value if not specified is <js>"_type"</js> .
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Use 'type' instead of '_type' for bean names.</jc>
	 * 	<ja>@Bean</ja>(typePropertyName=<js>"type"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanTypePropertyName}
	 * </ul>
	 */
	String typePropertyName() default "";
}