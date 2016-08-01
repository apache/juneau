/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import java.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;

/**
 * Convenience class for constructing instances of <code>List&lt;NameValuePair&gt;</code>
 * 	for the {@link UrlEncodedFormEntity} class.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs()
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"j_username"</js>, user))
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"j_password"</js>, pw));
 * 	request.setEntity(<jk>new</jk> UrlEncodedFormEntity(params));
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class NameValuePairs extends LinkedList<NameValuePair> {

	private static final long serialVersionUID = 1L;

	/**
	 * Appends the specified pair to the end of this list.
	 *
	 * @param pair The pair to append to this list.
	 * @return This object (for method chaining).
	 */
	public NameValuePairs append(NameValuePair pair) {
		super.add(pair);
		return this;
	}
}
