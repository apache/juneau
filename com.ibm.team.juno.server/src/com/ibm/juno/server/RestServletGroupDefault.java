/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.labels.*;

/**
 * Specialized subclass of {@link RestServletDefault} for showing "group" pages.
 * <p>
 * 	Group pages consist of simple lists of child resource URLs and their labels.
 * 	They're meant to be used as jumping-off points for child resources.
 * <p>
 * 	Child resources are specified using the {@link RestResource#children()} annotation.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@RestResource()
public abstract class RestServletGroupDefault extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Get child resources.
	 *
	 * @param req The HTTP request.
	 * @return The bean containing links to the child resources.
	 */
	@RestMethod(name="GET", path="/", description="Child resources")
	public ChildResourceDescriptions getChildren(RestRequest req) {
		return new ChildResourceDescriptions(this, req);
	}
}

