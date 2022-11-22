// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.swap;

import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.xml.datatype.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.swaps.*;

/**
 * Maintain the list of default swaps used by all serializers and parsers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class DefaultSwaps {

	private static final Map<Class<?>,ObjectSwap<?,?>> SWAPS = new ConcurrentHashMap<>();
	static {
		SWAPS.put(Enumeration.class, new EnumerationSwap());
		SWAPS.put(Iterator.class, new IteratorSwap());
		SWAPS.put(Locale.class, new LocaleSwap());
		SWAPS.put(Class.class, new ClassSwap());
		SWAPS.put(Calendar.class, new TemporalCalendarSwap.IsoOffsetDateTime());
		SWAPS.put(Date.class, new TemporalDateSwap.IsoLocalDateTime());
		SWAPS.put(Instant.class, new TemporalSwap.IsoInstant());
		SWAPS.put(ZonedDateTime.class, new TemporalSwap.IsoOffsetDateTime());
		SWAPS.put(LocalDate.class, new TemporalSwap.IsoLocalDate());
		SWAPS.put(LocalDateTime.class, new TemporalSwap.IsoLocalDateTime());
		SWAPS.put(LocalTime.class, new TemporalSwap.IsoLocalTime());
		SWAPS.put(OffsetDateTime.class, new TemporalSwap.IsoOffsetDateTime());
		SWAPS.put(OffsetTime.class, new TemporalSwap.IsoOffsetTime());
		SWAPS.put(StackTraceElement.class, new StackTraceElementSwap());
		SWAPS.put(Year.class, new TemporalSwap.IsoYear());
		SWAPS.put(YearMonth.class, new TemporalSwap.IsoYearMonth());
		SWAPS.put(Temporal.class, new TemporalSwap.IsoInstant());
		SWAPS.put(TimeZone.class, new TimeZoneSwap());
		SWAPS.put(XMLGregorianCalendar.class, new XMLGregorianCalendarSwap());
		SWAPS.put(ZoneId.class, new ZoneIdSwap());
		SWAPS.put(MatchResult.class, new MatchResultSwap());
	}

	/**
	 * Find the default ObjectSwap for the specified class.
	 *
	 * @param ci The class to find the swap for.
	 * @return The matched swap, or <jk>null</jk> if it couldn't be found.
	 */
	public static ObjectSwap<?,?> find(ClassInfo ci) {
		ClassInfo ci2 = ci.getAnyParent(x -> SWAPS.get(x.inner()) != null);
		return ci2 == null ? null : SWAPS.get(ci2.inner());
	}
}
