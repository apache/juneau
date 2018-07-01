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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Identifies Java methods on a resource/servlet class that get invoked during particular lifecycle events of
 * the servlet or REST call.
 *
 * <p>
 * For example, if you want to add an initialization method to your resource:
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(...)
 * 	<jk>public class</jk> MyResource  {
 *
 * 		<jc>// Our database.</jc>
 * 		<jk>private</jk> Map&lt;Integer,Object&gt; <jf>myDatabase</jf>;
 *
 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
 * 		<jk>public void</jk> initMyDatabase(RestContextBuilder builder) <jk>throws</jk> Exception {
 * 			<jf>myDatabase</jf> = <jk>new</jk> LinkedHashMap&lt;&gt;();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Or if you want to intercept REST calls:
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(...)
 * 	<jk>public class</jk> MyResource {
 *
 * 		<jc>// Add a request attribute to all incoming requests.</jc>
 * 		<ja>@RestHook</ja>(<jsf>PRE_CALL</jsf>)
 * 		<jk>public void</jk> onPreCall(RestRequest req) {
 * 			req.setAttribute(<js>"foo"</js>, <js>"bar"</js>);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * The hook events can be broken down into two categories:
 * <ul class='spaced-list'>
 * 	<li>Resource lifecycle events:
 * 		<ul>
 * 			<li>{@link HookEvent#INIT INIT} - Right before initialization.
 * 			<li>{@link HookEvent#POST_INIT POST_INIT} - Right after initialization.
 * 			<li>{@link HookEvent#POST_INIT_CHILD_FIRST POST_INIT_CHILD_FIRST} - Right after initialization, but run child methods first.
 * 			<li>{@link HookEvent#DESTROY DESTROY} - Right before servlet destroy.
 * 		</ul>
 * 	<li>REST call lifecycle events:
 * 		<ul>
 * 			<li>{@link HookEvent#START_CALL START_CALL} - At the beginning of a REST call.
 * 			<li>{@link HookEvent#PRE_CALL PRE_CALL} - Right before the <ja>@RestMethod</ja> method is invoked.
 * 			<li>{@link HookEvent#POST_CALL POST_CALL} - Right after the <ja>@RestMethod</ja> method is invoked.
 * 			<li>{@link HookEvent#END_CALL END_CALL} - At the end of the REST call after the response has been flushed.
 * 		</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.LifecycleHooks">Overview &gt; juneau-rest-server &gt; Lifecycle Hooks</a>
 * </ul>
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
public @interface RestHook {

	/**
	 * The lifecycle event.
	 */
	HookEvent value();
}
