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
 * Provides the ability to perform fluent-style assertions on response headers.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the content type header is provided.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).exists();
 *
 * 	<jc>// Validates the content type is JSON.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).equals(<js>"application/json"</js>);
 *
 * 	<jc>// Validates the content type is JSON using test predicate.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).passes(x -&gt; x.equals(<js>"application/json"</js>));
 *
 * 	<jc>// Validates the content type is JSON by just checking for substring.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).contains(<js>"json"</js>);
 *
 * 	<jc>// Validates the content type is JSON using regular expression.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>);
 *
 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
 * </p>
 *
 * <p>
 * The assertion test returns the original response object allowing you to chain multiple requests like so:
 * <p class='bcode w800'>
 * 	<jc>// Validates the header and converts it to a bean.</jc>
 * 	MediaType mediaType = client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertHeader(<js>"Content-Type"</js>).exists()
 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>)
 * 		.getHeader(<js>"Content-Type"</js>).as(MediaType.<jk>class</jk>);
 * </p>
 */
public class RestResponseHeaderAssertion extends FluentStringAssertion<RestResponse> {

	/**
	 * Constructor
	 *
	 * @param text The header value as text or <jk>null</jk> if it didn't exist.
	 * @param returns The response object.
	 */
	public RestResponseHeaderAssertion(String text, RestResponse returns) {
		super(text, returns);
	}
}
