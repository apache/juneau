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

import java.beans.*;
import java.lang.annotation.*;

/**
 * Annotation that can be applied to classes to control bean introspection and modeling.
 *
 * <p>
 * The bean-modeling sibling of {@code @Marshalled}.
 * Where {@code @Marshalled} (in <c>juneau-marshall</c>) carries the marshalling-only attributes (such as
 * {@code as}, {@code dictionary}, {@code example}, {@code implClass}, {@code interceptor}, {@code typeName},
 * and {@code typePropertyName}), this annotation carries the attributes that describe the bean itself —
 * which properties to include or exclude, how to name them, what stop class to use, and so on.
 *
 * <p>
 * Splitting the annotations lets a class participate in the bean model (for example, to declare its
 * property list) without dragging in any marshalling concerns, and lets the bean-modeling layer live in
 * <c>juneau-commons</c> independent of <c>juneau-marshall</c>.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean classes and parent interfaces.
 * 	<li>Non-bean modelled classes.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshalledAnnotation">@Marshalled Annotation</a>
 * </ul>
 */
@Documented
@Target({ TYPE })
@Retention(RUNTIME)
@Inherited
public @interface BeanType {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * Bean property excludes.
	 *
	 * <p>
	 * Specifies a list of properties that should be excluded from the bean's property set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from the Address class.</jc>
	 * 	<ja>@BeanType</ja>(excludeProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #xp()} is a shortened synonym for this value.
	 * 	<li class='note'>
	 * 		<b>Java Records:</b> Excluding record components is not supported during parsing.
	 * 		Because records are immutable, all components must be provided to the canonical constructor.
	 * 		Excluded components will be omitted from serialization output, but the parser will be unable to
	 * 		instantiate the record if the excluded component values are missing from the input.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String excludeProperties() default "";

	/**
	 * Bean factory class.
	 *
	 * <p>
	 * Specifies a {@link org.apache.juneau.commons.function.BeanFactory} class to use for instantiating
	 * this class instead of relying on a no-arg constructor or static {@code getInstance()} method.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link org.apache.juneau.commons.function.BeanFactory}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw BeanFactory type required for annotation attribute declaration
	})
	Class<? extends org.apache.juneau.commons.function.BeanFactory> factory() default org.apache.juneau.commons.function.BeanFactory.Void.class;

	/**
	 * Find fluent setters.
	 *
	 * <p>
	 * When <jk>true</jk>, fluent setters will be detected on beans.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@BeanType</ja>(findFluentSetters=<jk>true</jk>)
	 * 	<jk>public class</jk> MyBean {
	 * 		<jk>public int</jk> getId() {...}
	 * 		<jk>public</jk> MyBean id(<jk>int</jk> <jv>id</jv>) {...}
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
	 * @return The annotation value.
	 */
	boolean findFluentSetters() default false;

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 *
	 * <p class='bjava'>
	 * 	<jc>// Parent class</jc>
	 * 	<ja>@BeanType</ja>(interfaceClass=A.<jk>class</jk>)
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
	 * 	String <jv>json</jv> = Json5Serializer.<jsf>DEFAULT</jsf>.serialize(<jk>new</jk> A1());
	 * </p>
	 *
	 * <p>
	 * Note that this annotation can be used on the parent class so that it filters to all child classes,
	 * or can be set individually on the child classes.
	 *
	 * @return The annotation value.
	 */
	Class<?> interfaceClass() default void.class;

	/**
	 * Synonym for {@link #properties()}.
	 *
	 * @return The annotation value.
	 */
	String p() default "";

	/**
	 * Bean property includes.
	 *
	 * <p>
	 * The set and order of names of properties associated with a bean class.
	 *
	 * <p>
	 * The order specified is the same order that the entries will be returned by the bean's property set.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Address class with only street/city/state properties (in that order).</jc>
	 * 	<ja>@BeanType</ja>(properties=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #p()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String properties() default "";

	/**
	 * Associates a {@link PropertyNamer} with this bean to tailor the names of the bean properties.
	 *
	 * <p>
	 * Property namers are used to transform bean property names from standard form to some other form.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Define a class with dashed-lowercase property names.</jc>
	 * 	<ja>@BeanType</ja>(propertyNamer=PropertyNamerDLC.<jk>class</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	Class<? extends PropertyNamer> propertyNamer() default PropertyNamer.Void.class;

	/**
	 * Read-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are read-only despite having valid getters.
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from being parsed, but not serialized.</jc>
	 * 	<ja>@BeanType</ja>(readOnlyProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #ro()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String readOnlyProperties() default "";

	/**
	 * Synonym for {@link #readOnlyProperties()}.
	 *
	 * @return The annotation value.
	 */
	String ro() default "";

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
	 * <p class='bjava'>
	 * 	<jk>public class</jk> C1 {
	 * 		<jk>public int</jk> getP1();
	 * 	}
	 *
	 * 	<jk>public class</jk> C2 <jk>extends</jk> C1 {
	 * 		<jk>public int</jk> getP2();
	 * 	}
	 *
	 * 	<ja>@BeanType</ja>(stopClass=C2.<jk>class</jk>)
	 * 	<jk>public class</jk> C3 <jk>extends</jk> C2 {
	 * 		<jk>public int</jk> getP3();
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	Class<?> stopClass() default void.class;

	/**
	 * Opt out of alphabetical sorting for this specific bean's properties.
	 *
	 * <p>
	 * By default, bean properties are serialized in alphabetical order.
	 * When <jk>true</jk>, properties of this bean will use the natural JVM-dependent order instead.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Disable sorted properties for this specific bean.</jc>
	 * 	<ja>@BeanType</ja>(unsorted=<jk>true</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	boolean unsorted() default false;

	/**
	 * Synonym for {@link #writeOnlyProperties()}.
	 *
	 * @return The annotation value.
	 */
	String wo() default "";

	/**
	 * Write-only bean properties.
	 *
	 * <p>
	 * Specifies one or more properties on a bean that are write-only despite having valid setters.
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from being serialized, but not parsed.</jc>
	 * 	<ja>@BeanType</ja>(writeOnlyProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #wo()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String writeOnlyProperties() default "";

	/**
	 * Synonym for {@link #excludeProperties()}.
	 *
	 * @return The annotation value.
	 */
	String xp() default "";
}
