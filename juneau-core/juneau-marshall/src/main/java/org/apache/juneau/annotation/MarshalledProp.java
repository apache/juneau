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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import org.apache.juneau.commons.bean.BeanProp;

/**
 * Used to tailor how bean properties get marshalled by the framework.
 *
 * <p>
 * The marshalling-only sibling of {@link BeanProp @BeanProp}.
 * Where {@code @BeanProp} (in <c>juneau-commons</c>) carries the bean-modeling attributes
 * (such as {@code name}, {@code ro}, {@code wo}, {@code type}, {@code params}, {@code elementType},
 * and {@code factory}), this annotation carries the wire-format-specific attributes used by serializers
 * and parsers: a per-property format string, a bean dictionary for polymorphic types, and a list of
 * child properties to render.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Methods/Fields - Bean getters/setters and properties.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link MarshalledPropApply @MarshalledPropApply}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanProp}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshalledPropAnnotation">@MarshalledProp Annotation</a>
 * </ul>
 */
@Documented
@Target({ FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
@Inherited
public @interface MarshalledProp {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary for this bean property, used during
	 * polymorphic marshalling.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.Marshalled#dictionary()}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.MarshalledConfig#dictionary()}
	 * 	<li class='ja'>{@link org.apache.juneau.annotation.MarshalledConfig#dictionary_replace()}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#beanDictionary(Class...)}
	 * </ul>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property.
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary() default {};

	/**
	 * Specifies a String format for converting the bean property value to a formatted string.
	 *
	 * <p>
	 * Note that this is usually a one-way conversion during serialization.
	 *
	 * <p>
	 * During parsing, we will attempt to convert the value to the original form by using the
	 * {@link org.apache.juneau.MarshallingSession#convertToType(Object, Class)} but there is no guarantee that this will succeed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@MarshalledProp</ja>(format=<js>"$%.2f"</js>)
	 * 	<jk>public float</jk> <jf>price</jf>;
	 * </p>
	 *
	 * <p>
	 * This annotation can also be used on private fields of a property like so:
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<ja>@MarshalledProp</ja>(format=<js>"$%.2f"</js>)
	 * 		<jk>private float</jk> <jf>price</jf>;
	 *
	 * 		<jk>public float</jk> getPrice() {
	 * 			<jk>return</jk> <jf>price</jf>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String format() default "";

	/**
	 * Used to limit which child properties are rendered by the serializers.
	 *
	 * <p>
	 * Can be used on any of the following bean property types:
	 * <ul class='spaced-list'>
	 * 	<li>Beans - Only render the specified properties of the bean.
	 * 	<li>Maps - Only render the specified entries in the map.
	 * 	<li>Bean/Map arrays - Same, but applied to each element in the array.
	 * 	<li>Bean/Map collections - Same, but applied to each element in the collection.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyClass {
	 *
	 * 		<jc>// Only render 'f1' when serializing this bean property.</jc>
	 * 		<ja>@MarshalledProp</ja>(properties=<js>"f1"</js>)
	 * 		<jk>public</jk> MyChildClass <jf>x1</jf> = <jk>new</jk> MyChildClass();
	 * 	}
	 *
	 * 	<jk>public class</jk> MyChildClass {
	 * 		<jk>public int</jk> <jf>f1</jf> = 1;
	 * 		<jk>public int</jk> <jf>f2</jf> = 2;
	 * 	}
	 *
	 * 	<jc>// Renders "{x1:{f1:1}}"</jc>
	 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jk>new</jk> MyClass());
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String properties() default "";
}
