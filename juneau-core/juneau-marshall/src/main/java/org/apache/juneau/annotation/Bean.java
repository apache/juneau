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
 * 
 * <h5 class='topic'>Documentation</h5>
 * <ul>
 * 	<li><a class="doclink" href="../../../../overview-summary.html#juneau-marshall.BeanAnnotation">Overview &gt; @Bean Annotation</a>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanDictionary}
	 * </ul>
	 */
	Class<?>[] beanDictionary() default {};
	
	/**
	 * Specifies a list of properties that should be excluded from {@link BeanMap#entrySet()}.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from the Address class.</jc>
	 * 	<ja>@Bean</ja>(excludeProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_excludeProperties}
	 * </ul>
	 */
	String excludeProperties() default "";

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 * 
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 * 
	 * <p class='bcode'>
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
	 * 	String json = JsonSerializer.<jsf>DEFAULT_LAX</jsf>.serialize(<jk>new</jk> A1());
	 * </p>
	 * 
	 * <p>
	 * Note that this annotation can be used on the parent class so that it filters to all child classes,
	 * or can be set individually on the child classes.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanFilters}
	 * </ul>
	 */
	Class<?> interfaceClass() default Object.class;

	/**
	 * The set and order of names of properties associated with a bean class.
	 * 
	 * <p>
	 * The order specified is the same order that the entries will be returned by the {@link BeanMap#entrySet()} and
	 * related methods.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Address class with only street/city/state properties (in that order).</jc>
	 * 	<ja>@Bean</ja>(properties=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_includeProperties}
	 * </ul>
	 */
	String properties() default "";

	/**
	 * Property filter.
	 * 
	 * <p>
	 * Property filters can be used to intercept calls to getters and setters and alter their values in transit. 
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <p class='bcode'>
	 * 	<jc>// Define a class with dashed-lowercase property names.</jc>
	 * 	<ja>@Bean</ja>(propertyNamer=PropertyNamerDashedLC.<jk>class</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <p class='bcode'>
	 * 	<jc>// Sort bean properties alphabetically during serialization.</jc>
	 * 	<ja>@Bean</ja>(sort=<jk>true</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * For example, in the following class hierarchy, instances of <code>C3</code> will include property <code>p3</code>,
	 * but not <code>p1</code> or <code>p2</code>.
	 * <p class='bcode'>
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
	 * <br>For example, if a bean property is of type <code>Object</code>, then the serializer will add the name to the
	 * output so that the class can be determined during parsing.
	 * 
	 * <p>
	 * It is also used to specify element names in XML.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Use _type='mybean' to identify this bean.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <p class='bcode'>
	 * 	<jc>// Use 'type' instead of '_type' for bean names.</jc>
	 * 	<ja>@Bean</ja>(typePropertyName=<js>"type"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link BeanContext#BEAN_beanTypePropertyName}
	 * </ul>
	 */
	String typePropertyName() default "";
}