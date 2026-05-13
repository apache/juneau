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
import org.apache.juneau.MarshallingSession;
import org.apache.juneau.commons.bean.BeanProp;

/**
 * Used to tailor how bean properties get marshalled by the framework.
 *
 * <p>
 * The marshalling-only sibling of {@link BeanProp @BeanProp}.
 * Where {@code @BeanProp} (in <c>juneau-commons</c>) carries the bean-modeling attributes
 * (such as {@code name}, {@code ro}, {@code wo}, {@code type}, {@code params}, {@code elementType},
 * and {@code factory}), this annotation carries the wire-format-specific attributes used by serializers
 * and parsers: a per-property format string and a bean dictionary for polymorphic types.
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
	 * 	<li class='ja'>{@link Marshalled#dictionary()}
	 * 	<li class='ja'>{@link MarshalledConfig#dictionary()}
	 * 	<li class='ja'>{@link MarshalledConfig#dictionary_replace()}
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
	 * {@link MarshallingSession#convertToType(Object, Class)} but there is no guarantee that this will succeed.
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
}
