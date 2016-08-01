/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.microservice.resources;

import com.ibm.juno.microservice.ResourceGroup;
import com.ibm.juno.server.annotation.RestResource;

/**
 * Sample root REST resource.
 * 
 * @author James Bognar (jbognar@us.ibm.com)
 */
@RestResource(
	path="/",
	label="Sample Root Resource",
	description="This is a sample router page",
	children={ConfigResource.class,LogsResource.class}
)
public class SampleRootResource extends ResourceGroup {
	private static final long serialVersionUID = 1L;
}
