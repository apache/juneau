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

/**
 * Utility classes and methods for the {@link MarshalledProp @MarshalledProp} annotation.
 *
 */
public class MarshalledPropAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MarshalledPropAnnotation() {}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.marshall.MarshallingContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private Class<?>[] dictionary = new Class[0];
		private String format = "";
		private String[] view = {};
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
		private Nulls nulls = Nulls.NOT_SET;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(MarshalledProp.class);
		}

		/**
		 * Instantiates a new {@link MarshalledProp @MarshalledProp} object initialized with this builder.
		 *
		 * @return A new {@link MarshalledProp @MarshalledProp} object.
		 */
		public MarshalledProp build() {
			return new Object(this);
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
		 * Sets the {@link MarshalledProp#dictionary()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dictionary(Class<?>...value) {
			dictionary = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#format()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(String value) {
			format = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#durationFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder durationFormat(DurationFormat value) {
			durationFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#periodFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder periodFormat(PeriodFormat value) {
			periodFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#calendarFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder calendarFormat(CalendarFormat value) {
			calendarFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#dateFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder dateFormat(DateFormat value) {
			dateFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#temporalFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder temporalFormat(TemporalFormat value) {
			temporalFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#timeZoneFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder timeZoneFormat(TimeZoneFormat value) {
			timeZoneFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#localeFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder localeFormat(LocaleFormat value) {
			localeFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#binaryFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder binaryFormat(BinaryFormat value) {
			binaryFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#enumFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder enumFormat(EnumFormat value) {
			enumFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#uuidFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder uuidFormat(UuidFormat value) {
			uuidFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#bitSetFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder bitSetFormat(BitSetFormat value) {
			bitSetFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#bigNumberFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder bigNumberFormat(BigNumberFormat value) {
			bigNumberFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#booleanFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder booleanFormat(BooleanFormat value) {
			booleanFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#floatFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder floatFormat(FloatFormat value) {
			floatFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#currencyFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder currencyFormat(CurrencyFormat value) {
			currencyFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#classFormat()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder classFormat(ClassFormat value) {
			classFormat = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#view()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder view(String...value) {
			view = value;
			return this;
		}

		/**
		 * Sets the {@link MarshalledProp#nulls()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 * @since 10.0.0
		 */
		public Builder nulls(Nulls value) {
			nulls = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements MarshalledProp {

		private final String[] description;
		private final Class<?>[] dictionary;
		private final String format;
		private final String[] view;
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
		private final Nulls nulls;

		Object(MarshalledPropAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			dictionary = copyOf(b.dictionary);
			format = b.format;
			view = copyOf(b.view);
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
			nulls = b.nulls;
		}

		@Override /* Overridden from MarshalledProp */
		public Class<?>[] dictionary() {
			return dictionary;
		}

		@Override /* Overridden from MarshalledProp */
		public String format() {
			return format;
		}

		@Override /* Overridden from MarshalledProp */
		public DurationFormat durationFormat() {
			return durationFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public PeriodFormat periodFormat() {
			return periodFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public CalendarFormat calendarFormat() {
			return calendarFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public DateFormat dateFormat() {
			return dateFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public TemporalFormat temporalFormat() {
			return temporalFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public TimeZoneFormat timeZoneFormat() {
			return timeZoneFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public LocaleFormat localeFormat() {
			return localeFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public BinaryFormat binaryFormat() {
			return binaryFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public EnumFormat enumFormat() {
			return enumFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public UuidFormat uuidFormat() {
			return uuidFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public BitSetFormat bitSetFormat() {
			return bitSetFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public BigNumberFormat bigNumberFormat() {
			return bigNumberFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public BooleanFormat booleanFormat() {
			return booleanFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public FloatFormat floatFormat() {
			return floatFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public CurrencyFormat currencyFormat() {
			return currencyFormat;
		}

		@Override /* Overridden from MarshalledProp */
		public ClassFormat classFormat() {
			return classFormat;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}

		@Override /* Overridden from MarshalledProp */
		public String[] view() {
			return view;
		}

		@Override /* Overridden from MarshalledProp */
		public Nulls nulls() {
			return nulls;
		}
	}

	/** Default value */
	public static final MarshalledProp DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
