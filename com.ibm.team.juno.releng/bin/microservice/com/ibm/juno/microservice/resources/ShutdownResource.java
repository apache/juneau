/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.microservice.resources;

import com.ibm.juno.microservice.Resource;
import com.ibm.juno.server.annotation.RestMethod;
import com.ibm.juno.server.annotation.RestResource;

/**
 * Provides the capability to shut down this REST microservice through a REST call.
 */
@RestResource(
	path="/shutdown",
	label="Shut down this resource"
)
public class ShutdownResource extends Resource {
	
	private static final long serialVersionUID = 1L;

	/** 
	 * [GET /] - Shutdown this resource. 
	 * 
	 * @return The string <js>"OK"</js>.
	 * @throws Exception 
	 */
	@RestMethod(name="GET", path="/", description="Show contents of config file.")
	public String shutdown() throws Exception {
		new Thread(
			new Runnable() {
				@Override /* Runnable */
				public void run() {
					try {
						Thread.sleep(1000);
					System.exit(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		).start();
		return "OK";
	}
}
