/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import java.util.*;

import org.apache.http.client.utils.*;
import org.apache.http.message.*;

/**
 * Convenience class for setting date headers in RFC2616 format.
 * <p>
 * Equivalent to the following code:
 * <p class='bcode'>
 * 	Header h = <jk>new</jk> Header(name, DateUtils.<jsm>formatDate</jsm>(value));
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class DateHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a date request property in RFC2616 format.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	public DateHeader(String name, Date value) {
		super(name, DateUtils.formatDate(value));
	}
}
