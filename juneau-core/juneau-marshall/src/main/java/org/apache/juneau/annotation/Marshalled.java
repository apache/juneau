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

import org.apache.juneau.*;

/**
 * Annotation that can be applied to classes to control how they are marshalled.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link MarshalledApply @MarshalledApply}.
 * </ul>
 *
 * <p>
 * This annotation is typically only applied to non-bean classes.  The {@link Bean @Bean} annotation contains equivalent
 * functionality for bean classes.
 *
 */
@Documented
@Target({ METHOD, TYPE })
@Retention(RUNTIME)
@Inherited
@Repeatable(MarshalledAnnotation.Array.class)
public @interface Marshalled {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

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
	 * 	<ja>@Marshalled</ja>(example=<js>"{foo:'bar'}"</js>)
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
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
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
	 * Implementation class.
	 *
	 * <p>
	 * For interfaces and abstract classes this method can be used to specify an implementation class for the
	 * interface/abstract class so that instances of the implementation class are used when instantiated (e.g. during a
	 * parse).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Marshalled</ja>(implClass=MyInterfaceImpl.<jk>class</jk>)
	 * 	<jk>public class</jk> MyInterface {...}
	 * <p>
	 *
	 * @return The annotation value.
	 */
	Class<?> implClass() default void.class;

}