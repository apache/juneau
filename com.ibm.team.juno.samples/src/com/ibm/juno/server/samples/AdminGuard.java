/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import com.ibm.juno.server.*;

/**
 * Sample guard that only lets administrators through.
 */
public class AdminGuard extends RestGuard {

	@Override /* RestGuard */
	public boolean isRequestAllowed(RestRequest req) {
		return true;
	}
}
