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
package org.apache.juneau.rest.beans;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;

/**
 * Convenience subclass of {@link SeeOther} for redirecting a response to the servlet root.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.UtilityBeans">Utility Beans</a>
 * </ul>
 */
@Response @Schema(description="Redirect to servlet root")
public class SeeOtherRoot extends SeeOther {

	/**
	 * Reusable instance.
	 */
	public static final SeeOtherRoot INSTANCE = new SeeOtherRoot();

	/**
	 * Constructor.
	 */
	public SeeOtherRoot() {
		super();
		setLocation("servlet:/");
	}

	/**
	 * Constructor with no redirect.
	 * <p>
	 * Used for end-to-end interfaces.
	 *
	 * @param content Message to send as the response.
	 */
	public SeeOtherRoot(String content) {
		super();
		setLocation("servlet:/");
		setContent(content);
	}
}