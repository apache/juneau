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
package org.apache.juneau;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;

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

	/**
	 * Locale wire format override.
	 *
	 * @return The annotation value.
	 */
	LocaleFormat localeFormat() default LocaleFormat.NOT_SET;

	/**
	 * Period wire format override.
	 *
	 * @return The annotation value.
	 */
	PeriodFormat periodFormat() default PeriodFormat.NOT_SET;

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
	 * Binary wire format override for <code><jk>byte</jk>[]</code> property values.
	 *
	 * @return The annotation value.
	 */
	BinaryFormat binaryFormat() default BinaryFormat.NOT_SET;

	/**
	 * Enum wire format override for {@link Enum} property values.
	 *
	 * @return The annotation value.
	 */
	EnumFormat enumFormat() default EnumFormat.NOT_SET;

	/**
	 * UUID wire format override for {@link UUID} property values.
	 *
	 * @return The annotation value.
	 */
	UuidFormat uuidFormat() default UuidFormat.NOT_SET;

	/**
	 * Big-number wire format override for {@link BigInteger} / {@link BigDecimal} property values.
	 *
	 * @return The annotation value.
	 */
	BigNumberFormat bigNumberFormat() default BigNumberFormat.NOT_SET;

	/**
	 * Boolean wire format override for {@link Boolean} / <code><jk>boolean</jk></code> property values.
	 *
	 * @return The annotation value.
	 */
	BooleanFormat booleanFormat() default BooleanFormat.NOT_SET;

	/**
	 * Float / Double non-finite wire format override for {@link Float} / {@link Double} property values.
	 *
	 * @return The annotation value.
	 */
	FloatFormat floatFormat() default FloatFormat.NOT_SET;

	/**
	 * Currency wire format override for {@link Currency} property values.
	 *
	 * @return The annotation value.
	 */
	CurrencyFormat currencyFormat() default CurrencyFormat.NOT_SET;

	/**
	 * Class wire format override for {@link Class} property values.
	 *
	 * @return The annotation value.
	 */
	ClassFormat classFormat() default ClassFormat.NOT_SET;
}
