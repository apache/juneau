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
package org.apache.juneau.rest;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Encapsulates java-method-level properties.
 *
 * <p>
 * These are properties specified on a REST resource method that extends the properties defined on {@link RestContextProperties}
 * and adds the following:
 * <ul>
 * 	<li class='ja'>{@link RestMethod#properties()}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.ConfigurableProperties}
 * </ul>
 * @deprecated Use {@link RequestAttributes}
 */
@SuppressWarnings("serial")
@Deprecated
public class RestMethodProperties extends ObjectMap {

	/**
	 * Constructor
	 *
	 * @param inner The inner properties.
	 */
	public RestMethodProperties(RestContextProperties inner) {
		setInner(inner);
	}

	/**
	 * Constructor
	 *
	 * @param inner The inner properties.
	 */
	public RestMethodProperties(RestMethodProperties inner) {
		setInner(inner);
	}
}
