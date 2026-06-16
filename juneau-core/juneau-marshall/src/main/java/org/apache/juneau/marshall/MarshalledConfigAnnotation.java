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

import java.util.*;

import org.apache.juneau.commons.http.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;

/**
 * Utility classes and methods for the {@link MarshalledConfig @MarshalledConfig} annotation.
 *
 */
public class MarshalledConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private MarshalledConfigAnnotation() {}

	/**
	 * Applies {@link MarshalledConfig} annotations to a {@link org.apache.juneau.marshall.MarshallingContext.Builder}.
	 */
	public static class Applier extends AnnotationApplier<MarshalledConfig,MarshallingContext.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Applier(VarResolverSession vr) {
			super(MarshalledConfig.class, MarshallingContext.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<MarshalledConfig> ai, MarshallingContext.Builder b) {
			MarshalledConfig a = ai.inner();

			// @formatter:off
			bool(a.debug()).ifPresent(b::debug);
			string(a.typePropertyName()).ifPresent(b::typePropertyName);
			string(a.locale()).map(Locale::forLanguageTag).ifPresent(b::locale);
			string(a.mediaType()).map(MediaType::of).ifPresent(b::mediaType);
			string(a.timeZone()).map(TimeZone::getTimeZone).ifPresent(b::timeZone);
			if (a.durationFormat() != DurationFormat.NOT_SET) b.durationFormat(a.durationFormat());
			if (a.periodFormat() != PeriodFormat.NOT_SET) b.periodFormat(a.periodFormat());
			if (a.calendarFormat() != CalendarFormat.NOT_SET) b.calendarFormat(a.calendarFormat());
			if (a.dateFormat() != DateFormat.NOT_SET) b.dateFormat(a.dateFormat());
			if (a.temporalFormat() != TemporalFormat.NOT_SET) b.temporalFormat(a.temporalFormat());
			if (a.timeZoneFormat() != TimeZoneFormat.NOT_SET) b.timeZoneFormat(a.timeZoneFormat());
			if (a.localeFormat() != LocaleFormat.NOT_SET) b.localeFormat(a.localeFormat());
			if (a.binaryFormat() != BinaryFormat.NOT_SET) b.binaryFormat(a.binaryFormat());
			if (a.enumFormat() != EnumFormat.NOT_SET) b.enumFormat(a.enumFormat());
			if (a.uuidFormat() != UuidFormat.NOT_SET) b.uuidFormat(a.uuidFormat());
			if (a.bitSetFormat() != BitSetFormat.NOT_SET) b.bitSetFormat(a.bitSetFormat());
			if (a.bigNumberFormat() != BigNumberFormat.NOT_SET) b.bigNumberFormat(a.bigNumberFormat());
			if (a.booleanFormat() != BooleanFormat.NOT_SET) b.booleanFormat(a.booleanFormat());
			if (a.floatFormat() != FloatFormat.NOT_SET) b.floatFormat(a.floatFormat());
			if (a.currencyFormat() != CurrencyFormat.NOT_SET) b.currencyFormat(a.currencyFormat());
			if (a.classFormat() != ClassFormat.NOT_SET) b.classFormat(a.classFormat());
			classes(a.dictionary()).ifPresent(b::beanDictionary);
			classes(a.dictionary_replace()).ifPresent(x -> { b.beanDictionary().clear(); b.beanDictionary(x);});
			classes(a.swaps()).ifPresent(b::swaps);
			classes(a.swaps_replace()).ifPresent(x -> { b.swaps().clear(); b.swaps(x);});
			// @formatter:on
		}
	}
}