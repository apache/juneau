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
 * Transforms {@link Date Dates} to {@link Map Maps} of the format <tt>{value:long}</tt>.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("rawtypes")
public class DateMapFilter extends PojoFilter<Date,Map> {

	/**
	 * Converts the specified {@link Date} to a {@link Map}.
	 */
	@Override /* PojoFilter */
	public Map filter(Date o) {
		ObjectMap m = new ObjectMap();
		m.put("time", o.getTime());
		return m;
	}

	/**
	 * Converts the specified {@link Map} to a {@link Date}.
	 */
	@Override /* PojoFilter */
	public Date unfilter(Map o, ClassMeta<?> hint) throws ParseException {
		Class<?> c = (hint == null ? java.util.Date.class : hint.getInnerClass());
		long l = Long.parseLong(((Map<?,?>)o).get("time").toString());
		if (c == java.util.Date.class)
			return new java.util.Date(l);
		if (c == java.sql.Date.class)
			return new java.sql.Date(l);
		if (c == java.sql.Time.class)
			return new java.sql.Time(l);
		if (c == java.sql.Timestamp.class)
			return new java.sql.Timestamp(l);
		throw new ParseException("DateMapFilter is unable to narrow object of type ''{0}''", c);
	}
}
