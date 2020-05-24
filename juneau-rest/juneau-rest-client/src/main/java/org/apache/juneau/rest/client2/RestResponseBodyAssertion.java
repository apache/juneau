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
 * Provides the ability to perform fluent-style assertions on the body of HTTP responses.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Validates the response body equals the text "OK".</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().equals(<js>"OK"</js>);
 *
 * 	<jc>// Validates the response body contains the text "OK".</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().contains(<js>"OK"</js>);
 *
 * 	<jc>// Validates the response body passes a predicate test.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().passes(x -&gt; x.contains(<js>"OK"</js>));
 *
 * 	<jc>// Validates the response body matches a regular expression.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().matches(<js>".*OK.*"</js>);
 *
 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().matches(<js>".*OK.*"</js>,  <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
 *
 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
 * 	Pattern p = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
 * 	client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.assertBody().matches(p);
 * </p>
 *
 * <p>
 * The assertion test returns the original response object allowing you to chain multiple requests like so:
 * <p class='bcode w800'>
 * 	<jc>// Validates the response body matches a regular expression.</jc>
 * 	MyBean bean = client
 * 		.get(<jsf>URL</jsf>)
 * 		.run()
 * 		.cacheBody()  <jc>// Allows multiple calls to getBody() </jc>
 * 		.assertBody().matches(<js>".*OK.*"</js>);
 * 		.assertBody().doesNotMatch(<js>".*ERROR.*"</js>)
 * 		.getBody().as(MyBean.<jk>class</jk>);
 * </p>
 */
public class RestResponseBodyAssertion extends FluentStringAssertion<RestResponse> {

	/**
	 * Constructor
	 *
	 * @param text The HTTP body as text.
	 * @param returns The response object.
	 */
	public RestResponseBodyAssertion(String text, RestResponse returns) {
		super(text, returns);
	}
}
