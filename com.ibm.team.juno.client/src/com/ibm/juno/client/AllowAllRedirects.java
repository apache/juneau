/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.client;

import org.apache.http.impl.client.*;

/**
 * Redirect strategy that allows for redirects on any request type, not just <code>GET</code> or <code>HEAD</code>.
 * <p>
 * Note:  This class is similar to <code>org.apache.http.impl.client.LaxRedirectStrategy</code>
 * 	in Apache HttpClient 4.2, but also allows for redirects on <code>PUTs</code> and <code>DELETEs</code>.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class AllowAllRedirects extends DefaultRedirectStrategy {

   @Override /* DefaultRedirectStrategy */
   protected boolean isRedirectable(final String method) {
   	return true;
   }
}
