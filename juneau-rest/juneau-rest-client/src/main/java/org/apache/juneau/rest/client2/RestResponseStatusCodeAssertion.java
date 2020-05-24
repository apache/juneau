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
package org.apache.juneau.rest.client2;

import org.apache.juneau.utils.*;

/**
 * Provides the ability to perform fluent-style assertions on the response status code.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	MyBean bean = client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertStatusCode().equals(200)
 * 		.getBody().as(MyBean.<jk>class</jk>);
 * </p>
 */
public class RestResponseStatusCodeAssertion extends FluentIntAssertion<RestResponse> {

	/**
	 * Constructor.
	 *
	 * @param value The status code.
	 * @param returns The response object.
	 */
	public RestResponseStatusCodeAssertion(int value, RestResponse returns) {
		super(value, returns);
	}
}
