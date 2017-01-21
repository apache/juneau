// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.remoteable;

import org.apache.juneau.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Configurable properties for the {@link RemoteableServlet} class.
 * <p>
 * Properties can be set on the {@link RestServlet} class using the {@link RestResource#properties} or {@link RestMethod#properties} annotations.
 * <p>
 * These properties can also be passed in as servlet init parameters.
 * <p>
 * These properties are only valid at the class level, not the method level.  Setting them on {@link RestMethod#properties()} has no effect.
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
