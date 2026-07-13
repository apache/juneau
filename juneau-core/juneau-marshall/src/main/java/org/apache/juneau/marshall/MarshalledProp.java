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
package org.apache.juneau.marshall;

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
	 * Declares the named views this property belongs to.
	 *
	 * <p>
	 * When an active view is selected (via
	 * {@link MarshallingContext.Builder#activeView(String)} or the per-call session
	 * override), this property is included only when its declared view set contains the active view name.
	 *
	 * <p>
	 * If this member is empty (the default), the property follows the default-view-inclusion policy:
	 * by default an untagged property is included under every active view (matching Jackson's
	 * {@code DEFAULT_VIEW_INCLUSION} behavior). The policy can be flipped via
	 * {@link MarshallingContext.Builder#disableDefaultViewInclusion()}.
	 *
	 * <p>
	 * Multiple view names are supported — a property tagged {@code view={"summary","detail"}} is included
	 * when <em>either</em> {@code "summary"} or {@code "detail"} is the active view (union semantics).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>public class</jk> MyBean {
	 *
	 * 		<jc>// Included in all views (untagged = default inclusion)</jc>
	 * 		<jk>public</jk> String <jf>id</jf>;
	 *
	 * 		<jc>// Included only in the "summary" and "detail" views</jc>
	 * 		<ja>@MarshalledProp</ja>(view={<js>"summary"</js>, <js>"detail"</js>})
	 * 		<jk>public</jk> String <jf>name</jf>;
	 *
	 * 		<jc>// Included only in the "detail" view</jc>
	 * 		<ja>@MarshalledProp</ja>(view=<js>"detail"</js>)
	 * 		<jk>public</jk> String <jf>description</jf>;
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link MarshallingContext.Builder#activeView(String)}
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ViewProjection">View-based Projection</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 * @since 10.0.0
	 */
	String[] view() default {};

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
	 * 	<li class='jm'>{@link MarshallingContext.Builder#beanDictionary(Class...)}
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
	 * BitSet wire format override for {@link BitSet} property values.
	 *
	 * @return The annotation value.
	 */
	BitSetFormat bitSetFormat() default BitSetFormat.NOT_SET;

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

	/**
	 * Per-property null-coercion policy applied when a {@code null} JSON-equivalent value reaches this property during
	 * parsing.
	 *
	 * <p>
	 * Juneau's analog of Jackson's {@code @JsonSetter(nulls=…)}.  When the parser encounters an explicit {@code null}
	 * (or an absent {@link Optional}) for this property, the configured policy decides whether to:
	 * <ul>
	 * 	<li>{@link Nulls#LEAVE LEAVE} (default) — set the property to {@code null} (or, for an {@link Optional}-typed
	 * 		property, to {@link Optional#empty()}).
	 * 	<li>{@link Nulls#EMPTY EMPTY} — substitute the type's "empty" value (empty {@link String}/{@link Collection}/
	 * 		{@link Map}, primitive default for primitives, {@code Optional.empty()} for {@link Optional}).
	 * 	<li>{@link Nulls#DEFAULT DEFAULT} — substitute the bean-constructed default for the property (i.e. the value the
	 * 		property holds on a fresh no-arg-constructed instance of the bean).  When no reference instance can be built,
	 * 		falls back to {@link Nulls#LEAVE LEAVE}.
	 * 	<li>{@link Nulls#SKIP SKIP} — do not call the setter at all, leaving any pre-existing value in place.
	 * </ul>
	 *
	 * <p>
	 * For an {@link Optional}-typed property, {@code EMPTY}/{@code DEFAULT} resolve to {@link Optional#empty()} —
	 * never a bare {@code null} inside an {@link Optional}.  The same contract applies to {@link OptionalInt},
	 * {@link OptionalLong}, and {@link OptionalDouble}.
	 *
	 * <p>
	 * When this member is {@link Nulls#NOT_SET NOT_SET} (the default), the context-level default configured on
	 * {@link org.apache.juneau.marshall.parser.Parser.Builder#nulls(Nulls)} applies.
	 *
	 * @return The annotation value.
	 * @since 10.0.0
	 */
	Nulls nulls() default Nulls.NOT_SET;
}
