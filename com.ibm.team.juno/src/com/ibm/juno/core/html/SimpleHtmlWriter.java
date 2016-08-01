/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import java.io.*;

/**
 * Utility class for creating custom HTML.
 * <p>
 * Example:
 * <p class='bcode'>
 * 	String table = <jk>new</jk> SimpleHtmlWriter().sTag(<js>"table"</js>).sTag(<js>"tr"</js>).sTag(<js>"td"</js>).append(<js>"hello"</js>).eTag(<js>"td"</js>).eTag(<js>"tr"</js>).eTag(<js>"table"</js>).toString();
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SimpleHtmlWriter extends HtmlSerializerWriter {

	/**
	 * Constructor.
	 */
	public SimpleHtmlWriter() {
		super(new StringWriter(), true, false, '\'', null, null);
	}

	@Override /* Object */
	public String toString() {
		return out.toString();
	}
}
