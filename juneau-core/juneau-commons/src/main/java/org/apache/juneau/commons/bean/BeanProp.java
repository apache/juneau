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

import java.lang.annotation.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.inject.*;

/**
 * Annotation that can be applied to bean fields, getter/setter methods, and constructor parameters
 * to control bean introspection and property modeling.
 *
 * <p>
 * The bean-modeling sibling of {@code @MarshalledProp}.
 * Where {@code @MarshalledProp} (in <c>juneau-marshall</c>) carries the marshalling-only attributes
 * (such as {@code dictionary}, {@code format}, and {@code properties}), this annotation carries the
 * attributes that describe the bean property itself — its name, accessibility (read/write only),
 * concrete type, generic type parameters, element type, and the bean factory used to instantiate
 * its value.
 *
 * <p>
 * Splitting the annotations lets a property participate in the bean model (for example, to declare
 * its name or read-only semantics) without dragging in any marshalling concerns, and lets the
 * bean-modeling layer live in <c>juneau-commons</c> independent of <c>juneau-marshall</c>.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean fields, getter methods, and setter methods.
 * 	<li>Constructor parameters.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BeanpAnnotation">@BeanProp Annotation</a>
 * </ul>
 */
@Documented
@Target({ FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
@Inherited
public @interface BeanProp {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * Short, concise summary of the exposed API.
	 *
	 * <p>
	 * Intended as a brief, single-line description suitable for AI/LLM consumption, compact documentation,
	 * or any context where brevity matters. See {@link org.apache.juneau.commons.Schema#summary()}
	 * for the canonical definition; this field is the bean-property-level counterpart.
	 *
	 * @return The annotation value.
	 * @since 10.0.0
	 */
	String summary() default "";

	/**
	 * Element type for streaming/consuming bean properties.
	 *
	 * <p>
	 * Specifies the element type for properties of type {@link Stream},
	 * {@link BeanSupplier},
	 * {@link BeanConsumer}, or
	 * {@link BeanChannel} when type erasure prevents
	 * the framework from inferring the generic type argument at runtime.
	 *
	 * <p>
	 * This attribute also supports:
	 * <ul>
	 * 	<li><b>Narrowing</b> - Specify a more specific subtype than the declared type
	 * 	<li><b>Concrete implementation</b> - Specify a concrete class for an abstract/interface element type
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> OrderCollection {
	 * 		<jc>// Stream property - element type cannot be inferred at runtime due to erasure</jc>
	 * 		<ja>@BeanProp</ja>(elementType=Order.<jk>class</jk>)
	 * 		<jk>public</jk> Stream&lt;Order&gt; getOrders() { ... }
	 *
	 * 		<jc>// BeanChannel with concrete impl specified instead of abstract element type</jc>
	 * 		<ja>@BeanProp</ja>(elementType=ConcreteItem.<jk>class</jk>)
	 * 		<jk>public</jk> BeanChannel&lt;AbstractItem&gt; getItems() { ... }
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanSupplier}
	 * 	<li class='jc'>{@link BeanConsumer}
	 * 	<li class='jc'>{@link BeanChannel}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?> elementType() default void.class;

	/**
	 * Bean factory class for this property's value.
	 *
	 * <p>
	 * Specifies a {@link BeanFactory} class to use when instantiating
	 * the value of this specific bean property, overriding the class-level {@link BeanType#factory()} if present.
	 *
	 * <p>
	 * When a factory class is specified, the framework resolves it in the following order:
	 * <ol>
	 * 	<li>Look up the factory class in the configured {@link BeanStore}
	 * 	<li>Attempt direct instantiation via no-arg constructor or {@code getInstance()} static method
	 * 	<li>Throw {@link IllegalArgumentException} if both fail
	 * </ol>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@BeanProp</ja>(factory=ItemChannelFactory.<jk>class</jk>, elementType=Item.<jk>class</jk>)
	 * 		<jk>public</jk> BeanChannel&lt;Item&gt; getItems() { ... }
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jc'>{@link BeanFactory}
	 * 	<li class='ja'>{@link BeanType#factory()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw BeanFactory type required for annotation attribute declaration
	})
	Class<? extends org.apache.juneau.commons.function.BeanFactory> factory() default org.apache.juneau.commons.function.BeanFactory.Void.class;

	/**
	 * Identifies the name of the property.
	 *
	 * <p>
	 * Normally, this is automatically inferred from the field name or getter method name of the property.
	 * However, this property can be used to assign a different property name from the automatically inferred value.
	 *
	 * <h5 class='topic'>Dynamic beans</h5>
	 * <p>
	 * On a <strong>field</strong> whose type is not a {@link Map}, <js>"*"</js> does not create a dynamic property:
	 * the property name is the field name (same as omitting {@code name}/{@code value}), while other attributes on
	 * this annotation still apply.
	 * </p>
	 * <p>
	 * The bean property named <js>"*"</js> is the designated "dynamic property" which allows for "extra" bean
	 * properties not otherwise defined.
	 *
	 * <p>
	 * The following examples show how to define dynamic bean properties.
	 * <p class='bjava'>
	 * 	<jc>// Option #1 - A simple public Map field.
	 * 	// The field name can be anything.</jc>
	 * 	<jk>public class</jk> BeanWithDynaField {
	 *
	 * 		<ja>@BeanProp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; <jf>extraStuff</jf> = <jk>new</jk> LinkedHashMap&lt;&gt;();
	 * 	}
	 *
	 * 	<jc>// Option #2 - Getters and setters.
	 * 	// Method names can be anything.
	 * 	// Getter must return a Map with String keys.
	 * 	// Setter must take in two arguments.</jc>
	 * 	<jk>public class</jk> BeanWithDynaMethods {
	 *
	 * 		<ja>@BeanProp</ja>(name=<js>"*"</js>)
	 * 		<jk>public</jk> Map&lt;String,Object&gt; getMyExtraStuff() {
	 * 			...
	 * 		}
	 *
	 * 		<ja>@BeanProp</ja>(name=<js>"*"</js>)
	 * 		<jk>public void</jk> setAnExtraField(String <jv>name</jv>, Object <jv>value</jv>) {
	 * 			...
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <div class='info'>
	 * 		Note that the {@link Name @Name} annotation can also be used for identifying a property name.
	 * </div>
	 *
	 * @return The annotation value.
	 */
	String name() default "";

	/**
	 * For bean properties of maps and collections, this annotation can be used to identify the class types of the
	 * contents of the bean property object when the generic parameter types are interfaces or abstract classes.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type with String keys and Integer values.</jc>
	 * 		<ja>@BeanProp</ja>(type=HashMap.<jk>class</jk>, params={String.<jk>class</jk>,Integer.<jk>class</jk>})
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] params() default {};

	/**
	 * Identifies a property as read-only.
	 *
	 * <p>
	 * Serializers will serialize such properties as usual, but parsers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@BeanProp</ja>(ro=<js>"true"</js>)
	 * 		<jk>public float</jk> <jf>price</jf>;
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<b>Java Records:</b> Marking record components as read-only is not supported during parsing.
	 * 		Because records are immutable, all components must be provided to the canonical constructor.
	 * 		Read-only components will be serialized as usual, but the parser will be unable to instantiate the
	 * 		record if the read-only component values are missing from the input.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String ro() default "";

	/**
	 * Identifies a specialized class type for the property.
	 *
	 * <p>
	 * Normally this can be inferred through reflection of the field type or getter return type.
	 * However, you'll want to specify this value if you're parsing beans where the bean property class is an interface
	 * or abstract class to identify the bean type to instantiate.
	 * Otherwise, you may cause an {@link InstantiationException} when trying to set these fields.
	 *
	 * <p>
	 * This property must denote a concrete bean class with a no-arg constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Identify concrete map type.</jc>
	 * 		<ja>@BeanProp</ja>(type=HashMap.<jk>class</jk>)
	 * 		<jk>public</jk> Map <jf>p1</jf>;
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	Class<?> type() default void.class;

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * The following annotations are equivalent:
	 *
	 * <p class='bjava'>
	 * 	<ja>@BeanProp</ja>(name=<js>"foo"</js>)
	 *
	 * 	<ja>@BeanProp</ja>(<js>"foo"</js>)
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";

	/**
	 * Identifies a property as write-only.
	 *
	 * <p>
	 * Parsers will parse such properties as usual, but serializers will silently ignore them.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 * 		<ja>@BeanProp</ja>(wo=<js>"true"</js>)
	 * 		<jk>public float</jk> <jf>price</jf>;
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String wo() default "";
}
