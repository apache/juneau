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
package org.apache.juneau.rest.helper;

import java.net.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.response.*;

/**
 * Convenience subclass of {@link SeeOther} for redirecting a response to the servlet root.
 */
@Response(description="Redirect to servlet root")
public class SeeOtherRoot extends SeeOther {

	/**
	 * Reusable instance.
	 */
	public static final SeeOtherRoot INSTANCE = new SeeOtherRoot();

	/**
	 * Constructor.
	 */
	public SeeOtherRoot() {
		super(URI.create("servlet:/"));
	}
}