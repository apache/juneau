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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Used to tailor how beans get interpreted by the framework.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean classes and parent interfaces.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when an {@link #on()} value is specified.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.BeanAnnotation">@Bean Annotation</a> * </ul>
 */
@Documented
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(BeanAnnotation.Array.class)
@ContextApply(BeanAnnotation.Applier.class)
public @interface Bean {

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary for all properties in this class and all subclasses.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Beanp#dictionary()}
	 * 	<li class='ja'>{@link BeanConfig#dictionary()}
	 * 	<li class='ja'>{@link BeanConfig#dictionary_replace()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary() default {};

	/**
	 * POJO example.
	 *
	 * <p>
	 * Specifies an example of the specified class in Simplified JSON format.
	 *
	 * <p>
	 * Examples are used in cases such as POJO examples in Swagger documents.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Bean</ja>(example=<js>"{foo:'bar'}"</js>)
	 * 	<jk>public class</jk> MyClass {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Setting applies to specified class and all subclasses.
	 * 	<li class='note'>
	 * 		Keys are the class of the example.
	 * 		<br>Values are JSON 5 representation of that class.
	 * 	<li class='note'>
	 * 		POJO examples can also be defined on classes via the following:
	 * 		<ul class='spaced-list'>
	 * 			<li>A static field annotated with {@link Example @Example}.
	 * 			<li>A static method annotated with {@link Example @Example} with zero arguments or one {@link BeanSession} argument.
	 * 			<li>A static method with name <c>example</c> with no arguments or one {@link BeanSession} argument.
	 * 		</ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Example}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String example() default "";

	/**
	 * Bean property excludes.
	 *
	 * <p>
	 * Specifies a list of properties that should be excluded from {@link BeanMap#entrySet()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Exclude the 'city' and 'state' properties from the Address class.</jc>
	 * 	<ja>@Bean</ja>(excludeProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #xp()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesExcludes(Class, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesExcludes(String, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesExcludes(Map)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String excludeProperties() default "";

	/**
	 * Find fluent setters.
	 *
	 * <p>
	 * When <jk>true</jk>, fluent setters will be detected on beans.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Bean</ja>(findFluentSetters=<jk>true</jk>)
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
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link BeanConfig#findFluentSetters()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#findFluentSetters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean findFluentSetters() default false;

	/**
	 * Implementation class.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Bean</ja>(implClass=MyInterfaceImpl.<jk>class</jk>)
	 * 	<jk>public class</jk> MyInterface {...}
	 * <p>
	 *
	 * @return The annotation value.
	 */
	Class<?> implClass() default void.class;

	/**
	 * Bean property interceptor.
	 *
	 * <p>
	 * Bean interceptors can be used to intercept calls to getters and setters and alter their values in transit.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanInterceptor}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<? extends BeanInterceptor<?>> interceptor() default BeanInterceptor.Void.class;

	/**
	 * Identifies a class to be used as the interface class for this and all subclasses.
	 *
	 * <p>
	 * When specified, only the list of properties defined on the interface class will be used during serialization.
	 * Additional properties on subclasses will be ignored.
	 *
	 * <p class='bjava'>
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
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing class.
	 * It is ignored when the annotation is applied directly to classes.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

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
	 * <p class='bjava'>
	 * 	<jc>// Address class with only street/city/state properties (in that order).</jc>
	 * 	<ja>@Bean</ja>(properties=<js>"street,city,state"</js>)
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #p()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanProperties(Class, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanProperties(String, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanProperties(Map)}
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
	 * 	<ja>@Bean</ja>(propertyNamer=PropertyNamerDashedLC.<jk>class</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#propertyNamer(Class)}
	 * </ul>
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
	 * 	<ja>@Bean</ja>(readOnlyProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #ro()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesReadOnly(Class, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesReadOnly(String, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesReadOnly(Map)}
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
	 * Sort bean properties in alphabetical order.
	 *
	 * <p>
	 * When <jk>true</jk>, all bean properties will be serialized and access in alphabetical order.
	 * <br>Otherwise, the natural order of the bean properties is used which is dependent on the JVM vendor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Sort bean properties alphabetically during serialization.</jc>
	 * 	<ja>@Bean</ja>(sort=<jk>true</jk>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#sortProperties()}
	 * </ul>
	 *
	 * @return The annotation value.
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
	 * <p class='bjava'>
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
	 *
	 * @return The annotation value.
	 */
	Class<?> stopClass() default void.class;

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
	 * <p class='bjava'>
	 * 	<jc>// Use _type='mybean' to identify this bean.</jc>
	 * 	<ja>@Bean</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanDictionary(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
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
	 * <p class='bjava'>
	 * 	<jc>// Use 'type' instead of '_type' for bean names.</jc>
	 * 	<ja>@Bean</ja>(typePropertyName=<js>"type"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link BeanConfig#typePropertyName()}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#typePropertyName(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String typePropertyName() default "";

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
	 * 	<ja>@Bean</ja>(writeOnlyProperties=<js>"city,state"</js>})
	 * 	<jk>public class</jk> Address {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		{@link #wo()} is a shortened synonym for this value.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesWriteOnly(Class, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesWriteOnly(String, String)}
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#beanPropertiesWriteOnly(Map)}
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