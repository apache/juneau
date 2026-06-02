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
import java.math.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.bean.*;

/**
 * Annotation that can be applied to classes to control how they are marshalled.
 *
 * <p>
 * Carries the marshalling-only attributes for a class — bean-modeling attributes (such as
 * {@code properties}, {@code excludeProperties}, {@code findFluentSetters}, {@code interfaceClass},
 * {@code stopClass}, {@code propertyNamer}, {@code unsorted}, {@code factory}, etc.) live on
 * {@link BeanType @BeanType} in <c>juneau-commons</c>.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Bean classes and parent interfaces.
 * 	<li>Non-bean marshalled classes.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link MarshalledApply @MarshalledApply}.
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
@SuppressWarnings({
	"java:S1452"  // Wildcard required - Class<? extends BeanInterceptor<?>> for interceptor definition
})
public @interface Marshalled {

	/**
	 * Specifies the marshalling strategy for this type.
	 *
	 * <p>
	 * Defaults to {@link MarshalledAs#DETECT} (auto-detection).
	 * Use {@link MarshalledAs#STRING} to serialize via {@code toString()} and deserialize via
	 * {@code fromString}/{@code valueOf}/single-String constructor — equivalent to the former
	 * {@code MarshallingStringSwap} pattern.
	 *
	 * @return The marshalling strategy.
	 */
	MarshalledAs as() default MarshalledAs.DETECT;

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
	 * or any context where brevity matters. See {@link Schema#summary()}
	 * for the canonical definition; this field is the type-level counterpart.
	 *
	 * @return The annotation value.
	 * @since 9.5.0
	 */
	String summary() default "";

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary for all properties in this class and all subclasses.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link MarshalledProp#dictionary()}
	 * 	<li class='ja'>{@link MarshalledConfig#dictionary()}
	 * 	<li class='ja'>{@link MarshalledConfig#dictionary_replace()}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder<?>#beanDictionary(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary() default {};

	/**
	 * Calendar wire format override.
	 *
	 * @return The annotation value.
	 */
	CalendarFormat calendarFormat() default CalendarFormat.NOT_SET;

	/**
	 * Date wire format override.
	 *
	 * @return The annotation value.
	 */
	DateFormat dateFormat() default DateFormat.NOT_SET;

	/**
	 * Duration wire format override.
	 *
	 * @return The annotation value.
	 */
	DurationFormat durationFormat() default DurationFormat.NOT_SET;

	/**
	 * Locale wire format override.
	 *
	 * @return The annotation value.
	 */
	LocaleFormat localeFormat() default LocaleFormat.NOT_SET;

	/**
	 * Temporal wire format override.
	 *
	 * @return The annotation value.
	 */
	TemporalFormat temporalFormat() default TemporalFormat.NOT_SET;

	/**
	 * Time-zone wire format override.
	 *
	 * @return The annotation value.
	 */
	TimeZoneFormat timeZoneFormat() default TimeZoneFormat.NOT_SET;

	/**
	 * Binary wire format override for <code><jk>byte</jk>[]</code> values.
	 *
	 * @return The annotation value.
	 */
	BinaryFormat binaryFormat() default BinaryFormat.NOT_SET;

	/**
	 * Enum wire format override.
	 *
	 * @return The annotation value.
	 */
	EnumFormat enumFormat() default EnumFormat.NOT_SET;

	/**
	 * UUID wire format override.
	 *
	 * @return The annotation value.
	 */
	UuidFormat uuidFormat() default UuidFormat.NOT_SET;

	/**
	 * Big-number wire format override for {@link BigInteger} / {@link BigDecimal} values.
	 *
	 * @return The annotation value.
	 */
	BigNumberFormat bigNumberFormat() default BigNumberFormat.NOT_SET;

	/**
	 * Boolean wire format override for {@link Boolean} / <code><jk>boolean</jk></code> values.
	 *
	 * @return The annotation value.
	 */
	BooleanFormat booleanFormat() default BooleanFormat.NOT_SET;

	/**
	 * Float / Double non-finite wire format override for {@link Float} / {@link Double} values.
	 *
	 * @return The annotation value.
	 */
	FloatFormat floatFormat() default FloatFormat.NOT_SET;

	/**
	 * Currency wire format override for {@link Currency} values.
	 *
	 * @return The annotation value.
	 */
	CurrencyFormat currencyFormat() default CurrencyFormat.NOT_SET;

	/**
	 * Class wire format override for {@link Class} values.
	 *
	 * @return The annotation value.
	 */
	ClassFormat classFormat() default ClassFormat.NOT_SET;

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
	 * 			<li>A static method annotated with {@link Example @Example} with zero arguments or one {@link MarshallingSession} argument.
	 * 			<li>A static method with name <c>example</c> with no arguments or one {@link MarshallingSession} argument.
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
	 * Period wire format override.
	 *
	 * @return The annotation value.
	 */
	PeriodFormat periodFormat() default PeriodFormat.NOT_SET;

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
	 * 	<ja>@Marshalled</ja>(typeName=<js>"mybean"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder<?>#beanDictionary(Class...)}
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
	 * The default value if not specified is <js>"_type"</js>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Use 'type' instead of '_type' for bean names.</jc>
	 * 	<ja>@Marshalled</ja>(typePropertyName=<js>"type"</js>)
	 * 	<jk>public class</jk> MyBean {...}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link MarshalledConfig#typePropertyName()}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder<?>#typePropertyName(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String typePropertyName() default "";
}
