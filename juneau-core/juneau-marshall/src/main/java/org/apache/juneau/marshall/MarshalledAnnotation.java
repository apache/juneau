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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.*;

/**
 * Utility classes and methods for the {@link Marshalled @Marshalled} annotation.
 *
 */
public class MarshalledAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MarshalledAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private MarshalledAs as = MarshalledAs.DETECT;
		private String[] description = {};
		private Class<?>[] dictionary = new Class[0];
		private Class<?> implClass = void.class;
		private Class<? extends BeanInterceptor<?>> interceptor = BeanInterceptor.Void.class;
		private String example = "";
		private String summary = "";
		private String typeName = "";
		private String typePropertyName = "";
		private DurationFormat durationFormat = DurationFormat.NOT_SET;
		private PeriodFormat periodFormat = PeriodFormat.NOT_SET;
		private CalendarFormat calendarFormat = CalendarFormat.NOT_SET;
		private DateFormat dateFormat = DateFormat.NOT_SET;
		private TemporalFormat temporalFormat = TemporalFormat.NOT_SET;
		private TimeZoneFormat timeZoneFormat = TimeZoneFormat.NOT_SET;
		private LocaleFormat localeFormat = LocaleFormat.NOT_SET;
		private BinaryFormat binaryFormat = BinaryFormat.NOT_SET;
		private EnumFormat enumFormat = EnumFormat.NOT_SET;
		private UuidFormat uuidFormat = UuidFormat.NOT_SET;
		private BitSetFormat bitSetFormat = BitSetFormat.NOT_SET;
		private BigNumberFormat bigNumberFormat = BigNumberFormat.NOT_SET;
		private BooleanFormat booleanFormat = BooleanFormat.NOT_SET;
		private FloatFormat floatFormat = FloatFormat.NOT_SET;
		private CurrencyFormat currencyFormat = CurrencyFormat.NOT_SET;
		private ClassFormat classFormat = ClassFormat.NOT_SET;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Marshalled.class);
		}

		/**
		 * Instantiates a new {@link Marshalled @Marshalled} object initialized with this builder.
		 *
		 * @return A new {@link Marshalled @Marshalled} object.
		 */
		public Marshalled build() {
			return new Object(this);
		}

		/**
		 * Sets the {@link Marshalled#as()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder as(MarshalledAs value) {
			as = value;
			return this;
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#durationFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder durationFormat(DurationFormat value) {
			durationFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#example()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder example(String value) {
			example = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#periodFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder periodFormat(PeriodFormat value) {
			periodFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#calendarFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder calendarFormat(CalendarFormat value) {
			calendarFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#dateFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dateFormat(DateFormat value) {
			dateFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#temporalFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder temporalFormat(TemporalFormat value) {
			temporalFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#timeZoneFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder timeZoneFormat(TimeZoneFormat value) {
			timeZoneFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#localeFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder localeFormat(LocaleFormat value) {
			localeFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#binaryFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder binaryFormat(BinaryFormat value) {
			binaryFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#enumFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder enumFormat(EnumFormat value) {
			enumFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#uuidFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uuidFormat(UuidFormat value) {
			uuidFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#bitSetFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder bitSetFormat(BitSetFormat value) {
			bitSetFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#bigNumberFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder bigNumberFormat(BigNumberFormat value) {
			bigNumberFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#booleanFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder booleanFormat(BooleanFormat value) {
			booleanFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#floatFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder floatFormat(FloatFormat value) {
			floatFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#currencyFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder currencyFormat(CurrencyFormat value) {
			currencyFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#classFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder classFormat(ClassFormat value) {
			classFormat = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#implClass()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder implClass(Class<?> value) {
			implClass = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#interceptor()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder interceptor(Class<? extends BeanInterceptor<?>> value) {
			interceptor = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#summary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder summary(String value) {
			summary = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#typeName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder typeName(String value) {
			typeName = value;
			return this;
		}

		/**
		 * Sets the {@link Marshalled#typePropertyName()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder typePropertyName(String value) {
			typePropertyName = value;
			return this;
		}
	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Marshalled {

		private final MarshalledAs as;
		private final String[] description;
		private final Class<? extends BeanInterceptor<?>> interceptor;
		private final Class<?> implClass;
		private final Class<?>[] dictionary;
		private final String example;
		private final String summary;
		private final String typeName;
		private final String typePropertyName;
		private final DurationFormat durationFormat;
		private final PeriodFormat periodFormat;
		private final CalendarFormat calendarFormat;
		private final DateFormat dateFormat;
		private final TemporalFormat temporalFormat;
		private final TimeZoneFormat timeZoneFormat;
		private final LocaleFormat localeFormat;
		private final BinaryFormat binaryFormat;
		private final EnumFormat enumFormat;
		private final UuidFormat uuidFormat;
		private final BitSetFormat bitSetFormat;
		private final BigNumberFormat bigNumberFormat;
		private final BooleanFormat booleanFormat;
		private final FloatFormat floatFormat;
		private final CurrencyFormat currencyFormat;
		private final ClassFormat classFormat;

		Object(MarshalledAnnotation.Builder b) {
			super(b);
			as = b.as;
			description = copyOf(b.description);
			dictionary = copyOf(b.dictionary);
			example = b.example;
			implClass = b.implClass;
			interceptor = b.interceptor;
			summary = b.summary;
			typeName = b.typeName;
			typePropertyName = b.typePropertyName;
			durationFormat = b.durationFormat;
			periodFormat = b.periodFormat;
			calendarFormat = b.calendarFormat;
			dateFormat = b.dateFormat;
			temporalFormat = b.temporalFormat;
			timeZoneFormat = b.timeZoneFormat;
			localeFormat = b.localeFormat;
			binaryFormat = b.binaryFormat;
			enumFormat = b.enumFormat;
			uuidFormat = b.uuidFormat;
			bitSetFormat = b.bitSetFormat;
			bigNumberFormat = b.bigNumberFormat;
			booleanFormat = b.booleanFormat;
			floatFormat = b.floatFormat;
			currencyFormat = b.currencyFormat;
			classFormat = b.classFormat;
		}

		@Override /* Overridden from Marshalled */
		public MarshalledAs as() {
			return as;
		}

		@Override /* Overridden from Marshalled */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Overridden from Marshalled */
		public DurationFormat durationFormat() {
			return durationFormat;
		}

		@Override /* Overridden from Marshalled */
		public String example() {
			return example;
		}

		@Override /* Overridden from Marshalled */
		public PeriodFormat periodFormat() {
			return periodFormat;
		}

		@Override /* Overridden from Marshalled */
		public CalendarFormat calendarFormat() {
			return calendarFormat;
		}

		@Override /* Overridden from Marshalled */
		public DateFormat dateFormat() {
			return dateFormat;
		}

		@Override /* Overridden from Marshalled */
		public TemporalFormat temporalFormat() {
			return temporalFormat;
		}

		@Override /* Overridden from Marshalled */
		public TimeZoneFormat timeZoneFormat() {
			return timeZoneFormat;
		}

		@Override /* Overridden from Marshalled */
		public LocaleFormat localeFormat() {
			return localeFormat;
		}

		@Override /* Overridden from Marshalled */
		public BinaryFormat binaryFormat() {
			return binaryFormat;
		}

		@Override /* Overridden from Marshalled */
		public EnumFormat enumFormat() {
			return enumFormat;
		}

		@Override /* Overridden from Marshalled */
		public UuidFormat uuidFormat() {
			return uuidFormat;
		}

		@Override /* Overridden from Marshalled */
		public BitSetFormat bitSetFormat() {
			return bitSetFormat;
		}

		@Override /* Overridden from Marshalled */
		public BigNumberFormat bigNumberFormat() {
			return bigNumberFormat;
		}

		@Override /* Overridden from Marshalled */
		public BooleanFormat booleanFormat() {
			return booleanFormat;
		}

		@Override /* Overridden from Marshalled */
		public FloatFormat floatFormat() {
			return floatFormat;
		}

		@Override /* Overridden from Marshalled */
		public CurrencyFormat currencyFormat() {
			return currencyFormat;
		}

		@Override /* Overridden from Marshalled */
		public ClassFormat classFormat() {
			return classFormat;
		}

		@Override /* Overridden from Marshalled */
		public Class<?> implClass() {
			return implClass;
		}

		@Override /* Overridden from Marshalled */
		public Class<? extends BeanInterceptor<?>> interceptor() {
			return interceptor;
		}

		@Override /* Overridden from Marshalled */
		public String summary() {
			return summary;
		}

		@Override /* Overridden from Marshalled */
		public String typeName() {
			return typeName;
		}

		@Override /* Overridden from Marshalled */
		public String typePropertyName() {
			return typePropertyName;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Marshalled DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
