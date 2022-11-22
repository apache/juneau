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
package org.apache.juneau.swaps;

import static java.time.temporal.ChronoField.*;
import static java.time.temporal.TemporalQueries.*;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

/**
 * Wraps a {@link TemporalAccessor} to provide default values wherever possible instead of throwing unsupported field exceptions.
 *
 * <p>
 * If working correctly, any <c>TemporalAccessor</c> returned by the {@link DateTimeFormatter#parse(CharSequence)} method
 * should be able to be passed to any <code>Temporal.from(TemporalAccessor)</code> static method (such as {@link ZonedDateTime#from(TemporalAccessor)}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Swaps">Swaps</a>
 * </ul>
 */
public class DefaultingTemporalAccessor implements TemporalAccessor {

	private final TemporalAccessor inner;
	private final ZoneId zoneId;
	private ZonedDateTime zdt;

	/**
	 * Constructor.
	 *
	 * @param inner The temporal accessor being wrapped.
	 * @param zoneId The default zone ID if it's not specified in the accessor.
	 */
	public DefaultingTemporalAccessor(TemporalAccessor inner, ZoneId zoneId) {
		this.inner = inner;
		this.zoneId = zoneId;
	}

	@Override /* TemporalAccessor */
	public boolean isSupported(TemporalField field) {
		return inner.isSupported(field);
	}

	@Override /* TemporalAccessor */
	@SuppressWarnings("unchecked")
	public <R> R query(TemporalQuery<R> query) {
    	R r = inner.query(query);

    	if (r != null)
    		return r;

    	if (query == zone() || query == zoneId())
    		return (R)zoneId;

    	if (query == localTime()) {

    		if (isSupported(INSTANT_SECONDS))
    			return (R)zdt().toLocalTime();

    		int hour = 0;
    		if (isSupported(HOUR_OF_DAY))
    			hour = iget(HOUR_OF_DAY);
    		else if (isSupported(HOUR_OF_AMPM))
    			hour = iget(HOUR_OF_AMPM) + 12 * iget(AMPM_OF_DAY);

    		int minute = isSupported(MINUTE_OF_HOUR) ? iget(MINUTE_OF_HOUR) : 0;
    		int second = isSupported(SECOND_OF_MINUTE) ? iget(SECOND_OF_MINUTE) : 0;
    		int nano = isSupported(NANO_OF_SECOND) ? iget(NANO_OF_SECOND) : 0;

    		return (R)LocalTime.of(hour, minute, second, nano);
    	}

    	if (query == localDate()) {

    		if (isSupported(INSTANT_SECONDS))
    			return (R)zdt().toLocalDate();

    		int year = isSupported(YEAR) ? iget(ChronoField.YEAR) : 1970;
    		int month = isSupported(MONTH_OF_YEAR) ? iget(MONTH_OF_YEAR) : 1;
    		int dayOfMonth = isSupported(DAY_OF_MONTH) ? iget(DAY_OF_MONTH) : 1;

    		return (R)LocalDate.of(year, Month.of(month), dayOfMonth);
    	}

    	if (query == offset()) {
    		return (R)zoneId.getRules().getOffset(zdt().toInstant());
    	}

    	return null;
	}

	@Override /* TemporalAccessor */
	public long getLong(TemporalField field) {

		if (isSupported(field))
			return inner.getLong(field);

		if (field == INSTANT_SECONDS)
			return zdt().toEpochSecond();

		if (field == EPOCH_DAY)
			return zdt().toLocalDate().toEpochDay();

		if (field == YEAR) {
			if (isSupported(INSTANT_SECONDS))
	    		return zdt().toLocalDate().getYear();
			return 1970;
		}

		if (field == MONTH_OF_YEAR) {
			if (isSupported(INSTANT_SECONDS))
	    		return zdt().toLocalDate().getMonthValue();
			return 1;
		}

		return 0;
	}

	@Override /* TemporalAccessor */
	public int get(TemporalField field) {
		if (inner.isSupported(field))
			return inner.get(field);
		return (int)getLong(field);
	}

	private int iget(TemporalField field) {
		return inner.get(field);
	}

	private ZonedDateTime zdt() {
		if (zdt == null)
			zdt = ZonedDateTime.from(this);
		return zdt;
	}
}
