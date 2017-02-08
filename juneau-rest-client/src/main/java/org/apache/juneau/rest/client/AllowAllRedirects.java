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

import org.apache.http.impl.client.*;

/**
 * Redirect strategy that allows for redirects on any request type, not just <code>GET</code> or <code>HEAD</code>.
 * <p>
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>This class is similar to <code>org.apache.http.impl.client.LaxRedirectStrategy</code>
 * 		in Apache HttpClient 4.2, but also allows for redirects on <code>PUTs</code> and <code>DELETEs</code>.
 * </ul>
 */
public class AllowAllRedirects extends DefaultRedirectStrategy {

   @Override /* DefaultRedirectStrategy */
   protected boolean isRedirectable(final String method) {
   	return true;
   }
}
