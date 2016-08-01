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
 * Transforms {@link Date Dates} to {@link Long Longs}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class DateLongFilter extends PojoFilter<Date,Long> {

	/**
	 * Converts the specified {@link Date} to a {@link Long}.
	 */
	@Override /* PojoFilter */
	public Long filter(Date o) {
		return o.getTime();
	}

	/**
	 * Converts the specified {@link Long} to a {@link Date}.
	 */
	@Override /* PojoFilter */
	public Date unfilter(Long o, ClassMeta<?> hint) throws ParseException {
		Class<?> c = (hint == null ? java.util.Date.class : hint.getInnerClass());
		if (c == java.util.Date.class)
			return new java.util.Date(o);
		if (c == java.sql.Date.class)
			return new java.sql.Date(o);
		if (c == java.sql.Time.class)
			return new java.sql.Time(o);
		if (c == java.sql.Timestamp.class)
			return new java.sql.Timestamp(o);
		throw new ParseException("DateLongFilter is unable to narrow object of type ''{0}''", c);
	}
}
