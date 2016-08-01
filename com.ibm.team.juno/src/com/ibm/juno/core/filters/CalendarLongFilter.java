/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;

/**
 * Transforms {@link Calendar Calendars} to {@link Long Longs} using {@code Calender.getTime().getTime()}.
 * <p>
 * 	TODO:  This class does not handle timezones correctly when parsing {@code GregorianCalendar} objects.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class CalendarLongFilter extends PojoFilter<Calendar,Long> {

	/**
	 * Converts the specified {@link Calendar} to a {@link Long}.
	 */
	@Override /* PojoFilter */
	public Long filter(Calendar o) {
		return o.getTime().getTime();
	}

	/**
	 * Converts the specified {@link Long} to a {@link Calendar}.
	 */
	@Override /* PojoFilter */
	@SuppressWarnings("unchecked")
	public Calendar unfilter(Long o, ClassMeta<?> hint) throws ParseException {
		ClassMeta<? extends Calendar> tt;
		try {
			if (hint == null || ! hint.canCreateNewInstance())
				hint = getBeanContext().getClassMeta(GregorianCalendar.class);
			tt = (ClassMeta<? extends Calendar>)hint;
			Calendar c = tt.newInstance();
			c.setTimeInMillis(o);
			return c;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
