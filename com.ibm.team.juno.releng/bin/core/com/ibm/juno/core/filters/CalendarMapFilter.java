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
 * Transforms {@link Calendar Calendars} to {@link Map Maps} of the format <code>{_class:String,value:long}</code>.
 * <p>
 * 	TODO:  This class does not handle timezones correctly when parsing {@code GregorianCalendar} objects.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("rawtypes")
public class CalendarMapFilter extends PojoFilter<Calendar,Map> {

	/**
	 * Converts the specified {@link Calendar} to a {@link Map}.
	 */
	@Override /* PojoFilter */
	public Map filter(Calendar o) {
		ObjectMap m = new ObjectMap();
		m.put("time", o.getTime().getTime());
		m.put("timeZone", o.getTimeZone().getID());
		return m;
	}

	/**
	 * Converts the specified {@link Map} to a {@link Calendar}.
	 */
	@Override /* PojoFilter */
	@SuppressWarnings("unchecked")
	public Calendar unfilter(Map o, ClassMeta<?> hint) throws ParseException {
		ClassMeta<? extends Calendar> tt;
		try {
			if (hint == null || ! hint.canCreateNewInstance())
				hint = getBeanContext().getClassMeta(GregorianCalendar.class);
			tt = (ClassMeta<? extends Calendar>)hint;
			long time = Long.parseLong(o.get("time").toString());
			String timeZone = o.get("timeZone").toString();
			Date d = new Date(time);
			Calendar c = tt.newInstance();
			c.setTime(d);
			c.setTimeZone(TimeZone.getTimeZone(timeZone));
			return c;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
