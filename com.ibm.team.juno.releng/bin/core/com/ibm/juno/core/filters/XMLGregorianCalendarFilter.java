/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2013, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.filters;

import javax.xml.datatype.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Transforms {@link XMLGregorianCalendar XMLGregorianCalendars} to ISO8601 date-time {@link String Strings}.
 * <p>
 * 	Objects are converted to strings using {@link XMLGregorianCalendar#toXMLFormat()}.
 * <p>
 * 	Strings are converted to objects using {@link DatatypeFactory#newXMLGregorianCalendar(String)}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class XMLGregorianCalendarFilter extends PojoFilter<XMLGregorianCalendar,String> {

	private DatatypeFactory dtf;

	/**
	 * Constructor.
	 */
	public XMLGregorianCalendarFilter() {
		try {
			this.dtf = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the specified <code>XMLGregorianCalendar</code> to a {@link String}.
	 */
	@Override /* PojoFilter */
	public String filter(XMLGregorianCalendar b) throws SerializeException {
		return b.toXMLFormat();
	}

	/**
	 * Converts the specified {@link String} to an <code>XMLGregorianCalendar</code>.
	 */
	@Override /* PojoFilter */
	public XMLGregorianCalendar unfilter(String s, ClassMeta<?> hint) throws ParseException {
		if (StringUtils.isEmpty(s))
			return null;
		return dtf.newXMLGregorianCalendar(s);
	}
}