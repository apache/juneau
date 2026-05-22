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

import org.apache.juneau.commons.bean.BeanConfig;
import org.apache.juneau.commons.http.MediaType;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.swap.*;

/**
 * Annotation for specifying marshalling-only config properties defined in {@link MarshallingContext} and {@link MarshallingTraverseContext}.
 *
 * <p>
 * Used primarily for specifying marshalling configuration properties on REST classes and methods.
 *
 * <p>
 * This annotation is the marshalling-only sibling of {@code @BeanConfig}. After the Phase 3 split,
 * bean-modeling attributes (visibility settings, fluent-setter detection, property naming, not-bean
 * exclusions, etc.) live on {@link BeanConfig @BeanConfig} in
 * {@code juneau-commons}. Attributes that affect wire format, type discriminators, swap classes,
 * locale/media-type/timezone, and debug mode stay here on {@code @MarshalledConfig}.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply(MarshalledConfigAnnotation.Applier.class)
@SuppressWarnings({
	"java:S100" // Annotation methods use underscore suffix to avoid Java keyword conflicts
})
public @interface MarshalledConfig {

	/**
	 * Debug mode.
	 *
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link org.apache.juneau.MarshallingTraverseContext.Builder#detectRecursions()}.
	 * </ul>
	 *
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.Context.Builder#debug()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String debug() default "";

	/**
	 * Bean dictionary.
	 *
	 * <p>
	 * The list of classes that make up the bean dictionary in this bean context.
	 *
	 * <p>
	 * A dictionary is a name/class mapping used to find class types during parsing when they cannot be inferred
	 * through reflection.
	 * <br>The names are defined through the {@link Marshalled#typeName() @Marshalled(typeName)} annotation defined on the bean class.
	 * <br>For example, if a class <c>Foo</c> has a type-name of <js>"myfoo"</js>, then it would end up serialized
	 * as <js>"{_type:'myfoo',...}"</js>.
	 *
	 * <p>
	 * This setting tells the parsers which classes to look for when resolving <js>"_type"</js> attributes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Marshalled#dictionary()}
	 * 	<li class='ja'>{@link MarshalledProp#dictionary()}
	 * 	<li class='ja'>{@link MarshalledConfig#dictionary_replace()}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#beanDictionary(Class...)}
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BeanDictionaryBasics">Bean Dictionary Basics</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary() default {};

	/**
	 * Replace bean dictionary.
	 *
	 * <p>
	 * Same as {@link #dictionary()} but replaces any existing value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Marshalled#dictionary()}
	 * 	<li class='ja'>{@link MarshalledProp#dictionary()}
	 * 	<li class='ja'>{@link MarshalledConfig#dictionary()}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#beanDictionary(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] dictionary_replace() default {};

	/**
	 * Locale.
	 *
	 * <p>
	 * Specifies the default locale for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingSession.Builder#locale(Locale)}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#locale(Locale)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String locale() default "";

	/**
	 * Media type.
	 *
	 * <p>
	 * Specifies the default media type value for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingSession.Builder#mediaType(MediaType)}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#mediaType(MediaType)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String mediaType() default "";

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	/**
	 * Java object swaps.
	 *
	 * <p>
	 * Swaps are used to "swap out" non-serializable classes with serializable equivalents during serialization,
	 * and "swap in" the non-serializable class during parsing.
	 *
	 * <p>
	 * An example of a swap would be a <c>Calendar</c> object that gets swapped out for an ISO8601 string.
	 *
	 * <p>
	 * Multiple swaps can be associated with a single class.
	 * <br>When multiple swaps are applicable to the same class, the media type pattern defined by
	 * {@link ObjectSwap#forMediaTypes()} or {@link Swap#mediaTypes() @Swap(mediaTypes)} are used to come up with the best match.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#swaps(Class...)}
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapBasics">Swap Basics</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/PerMediaTypeSwaps">Per-media-type Swaps</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OneWaySwaps">One-way Swaps</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapAnnotation">@Swap Annotation</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AutoSwaps">Auto-detected swaps</a>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SurrogateClasses">Surrogate Classes</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] swaps() default {};

	/**
	 * Replace Java object swap classes.
	 *
	 * <p>
	 * Same as {@link #swaps()} but replaces any existing value.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#swaps(Class...)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] swaps_replace() default {};

	/**
	 * Calendar wire format.
	 *
	 * @return The annotation value.
	 */
	CalendarFormat calendarFormat() default CalendarFormat.NOT_SET;

	/**
	 * Date wire format.
	 *
	 * @return The annotation value.
	 */
	DateFormat dateFormat() default DateFormat.NOT_SET;

	/**
	 * Duration wire format.
	 *
	 * @return The annotation value.
	 */
	DurationFormat durationFormat() default DurationFormat.NOT_SET;

	/**
	 * Locale wire format.
	 *
	 * @return The annotation value.
	 */
	LocaleFormat localeFormat() default LocaleFormat.NOT_SET;

	/**
	 * Period wire format.
	 *
	 * @return The annotation value.
	 */
	PeriodFormat periodFormat() default PeriodFormat.NOT_SET;

	/**
	 * Temporal wire format.
	 *
	 * @return The annotation value.
	 */
	TemporalFormat temporalFormat() default TemporalFormat.NOT_SET;

	/**
	 * Time-zone wire format.
	 *
	 * @return The annotation value.
	 */
	TimeZoneFormat timeZoneFormat() default TimeZoneFormat.NOT_SET;

	/**
	 * Binary wire format for <code><jk>byte</jk>[]</code> values.
	 *
	 * @return The annotation value.
	 */
	BinaryFormat binaryFormat() default BinaryFormat.NOT_SET;

	/**
	 * Enum wire format.
	 *
	 * @return The annotation value.
	 */
	EnumFormat enumFormat() default EnumFormat.NOT_SET;

	/**
	 * UUID wire format.
	 *
	 * @return The annotation value.
	 */
	UuidFormat uuidFormat() default UuidFormat.NOT_SET;

	/**
	 * Big-number wire format for {@link BigInteger} / {@link BigDecimal} values.
	 *
	 * @return The annotation value.
	 */
	BigNumberFormat bigNumberFormat() default BigNumberFormat.NOT_SET;

	/**
	 * Boolean wire format for {@link Boolean} / <code><jk>boolean</jk></code> values.
	 *
	 * @return The annotation value.
	 */
	BooleanFormat booleanFormat() default BooleanFormat.NOT_SET;

	/**
	 * Float / Double non-finite wire format for {@link Float} / {@link Double} values.
	 *
	 * @return The annotation value.
	 */
	FloatFormat floatFormat() default FloatFormat.NOT_SET;

	/**
	 * Currency wire format for {@link Currency} values.
	 *
	 * @return The annotation value.
	 */
	CurrencyFormat currencyFormat() default CurrencyFormat.NOT_SET;

	/**
	 * Class wire format for {@link Class} values.
	 *
	 * @return The annotation value.
	 */
	ClassFormat classFormat() default ClassFormat.NOT_SET;

	/**
	 * Time zone.
	 *
	 * <p>
	 * Specifies the default timezone for serializer and parser sessions.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingSession.Builder#timeZone(TimeZone)}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#timeZone(TimeZone)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String timeZone() default "";

	/**
	 * Bean type property name.
	 *
	 * <p>
	 * This specifies the name of the bean property used to store the dictionary name of a bean type so that the
	 * parser knows the data type to reconstruct.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Default value: <js>"_type"</js>.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='ja'>{@link Marshalled#typePropertyName()}
	 * 	<li class='jm'>{@link org.apache.juneau.MarshallingContext.Builder#typePropertyName(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String typePropertyName() default "";
}
