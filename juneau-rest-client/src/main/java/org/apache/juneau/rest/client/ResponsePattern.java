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
package org.apache.juneau.rest.client;

import java.io.*;
import java.util.regex.*;

/**
 * Used to find regular expression matches in REST responses made through {@link RestCall}.
 * <p>
 * Response patterns are applied to REST calls through the {@link RestCall#addResponsePattern(ResponsePattern)} method.
 *
 * <h6 class='topic'>Example:</h6>
 * This example shows how to use a response pattern finder to find and capture patterns for <js>"x=number"</js> and <js>"y=string"</js>
 * 	from a response body.
 * <p>
 * <p class='bcode'>
 * 	<jk>final</jk> List&lt;Number&gt; xList = <jk>new</jk> ArrayList&lt;Number&gt;();
 * 	<jk>final</jk> List&lt;String&gt; yList = <jk>new</jk> ArrayList&lt;String&gt;();
 *
 * 	restClient.doGet(<jsf>URL</jsf>)
 * 		.addResponsePattern(
 * 			<jk>new</jk> ResponsePattern(<js>"x=(\\d+)"</js>) {
 * 				<ja>@Override</ja>
 * 				<jk>public void</jk> onMatch(RestCall restCall, Matcher m) <jk>throws</jk> RestCallException {
 * 					xList.add(Integer.<jsm>parseInt</jsm>(m.group(1)));
 * 				}
 * 				<ja>@Override</ja>
 * 				<jk>public void</jk> onNoMatch(RestCall restCall) <jk>throws</jk> RestCallException {
 * 					<jk>throw new</jk> RestCallException(<js>"No X's found!"</js>);
 * 				}
 * 			}
 * 		)
 * 		.addResponsePattern(
 * 			<jk>new</jk> ResponsePattern(<js>"y=(\\S+)"</js>) {
 * 				<ja>@Override</ja>
 * 				<jk>public void</jk> onMatch(RestCall restCall, Matcher m) <jk>throws</jk> RestCallException {
 * 					yList.add(m.group(1));
 * 				}
 * 				<ja>@Override</ja>
 * 				<jk>public void</jk> onNoMatch(RestCall restCall) <jk>throws</jk> RestCallException {
 * 					<jk>throw new</jk> RestCallException(<js>"No Y's found!"</js>);
 * 				}
 * 			}
 * 		)
 * 		.run();
 * </p>
 * <p>
 * <h5 class='notes'>Important Notes:</h5>
 * <ol class='notes'>
 * 	<li><p>
 * 		Using response patterns does not affect the functionality of any of the other methods
 * 		used to retrieve the response such as {@link RestCall#getResponseAsString()} or {@link RestCall#getResponse(Class)}.<br>
 * 		HOWEVER, if you want to retrieve the entire text of the response from inside the match methods,
 * 		use {@link RestCall#getCapturedResponse()} since this method will not absorb the response for those other methods.
 * 	</p>
 * 	<li><p>
 * 		Response pattern methods are NOT executed if a REST exception occurs during the request.
 * 	</p>
 * 	<li><p>
 * 		The {@link RestCall#successPattern(String)} and {@link RestCall#failurePattern(String)} methods use instances of
 * 		this class to throw {@link RestCallException RestCallExceptions} when success patterns are not found or failure patterns
 * 		are found.
 * 	</p>
 * 	<li><p>
 * 		{@link ResponsePattern} objects are reusable and thread-safe.
 * 	</p>
 * </ol>
 */
public abstract class ResponsePattern {

	private Pattern pattern;

	/**
	 * Constructor.
	 *
	 * @param pattern Regular expression pattern.
	 */
	public ResponsePattern(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	void match(RestCall rc) throws RestCallException {
		try {
			Matcher m = pattern.matcher(rc.getCapturedResponse());
			boolean found = false;
			while (m.find()) {
				onMatch(rc, m);
				found = true;
			}
			if (! found)
				onNoMatch(rc);
		} catch (IOException e) {
			throw new RestCallException(e);
		}
	}

	/**
	 * Returns the pattern passed in through the constructor.
	 *
	 * @return The pattern passed in through the constructor.
	 */
	protected String getPattern() {
		return pattern.pattern();
	}

	/**
	 * Instances can override this method to handle when a regular expression pattern matches
	 * 	on the output.
	 * <p>
	 * This method is called once for every pattern match that occurs in the response text.
	 *
	 * @param rc The {@link RestCall} that this pattern finder is being used on.
	 * @param m The regular expression {@link Matcher}.  Can be used to retrieve group matches in the pattern.
	 * @throws RestCallException Instances can throw an exception if a failure condition is detected.
	 */
	public void onMatch(RestCall rc, Matcher m) throws RestCallException {}

	/**
	 * Instances can override this method to handle when a regular expression pattern doesn't match on the output.
	 *
	 * @param rc The {@link RestCall} that this pattern finder is being used on.
	 * @throws RestCallException Instances can throw an exception if a failure condition is detected.
	 */
	public void onNoMatch(RestCall rc) throws RestCallException {}
}
