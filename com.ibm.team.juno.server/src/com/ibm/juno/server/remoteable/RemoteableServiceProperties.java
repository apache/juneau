/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.remoteable;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Configurable properties for the {@link RemoteableServlet} class.
 * <p>
 * Properties can be set on the {@link RestServlet} class using the {@link RestResource#properties} or {@link RestMethod#properties} annotations.
 * <p>
 * These properties can also be passed in as servlet init parameters.
 * <p>
 * These properties are only valid at the class level, not the method level.  Setting them on {@link RestMethod#properties()} has no effect.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RemoteableServiceProperties {

	/**
	 * Only expose interfaces and methods annotated with {@link Remoteable @Remoteable} ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * When enabled, the {@link RemoteableServlet} class will only work with annotated remoteable interfaces and methods.
	 * Otherwise, all public methods can be executed through the service.
	 */
	public static final String REMOTEABLE_includeOnlyRemotableMethods = "RemoteableService.includeOnlyRemoteableMethods";
}
