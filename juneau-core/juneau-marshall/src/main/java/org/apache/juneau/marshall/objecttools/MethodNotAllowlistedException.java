/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.marshall.objecttools;

import org.apache.juneau.marshall.*;

/**
 * Thrown by {@link ObjectIntrospector} when an attempt is made to invoke a method that has not been
 * explicitly allow-listed.
 *
 * <p>
 * As of Juneau 10.0, {@link ObjectIntrospector} is secure-by-default: reflective method dispatch is refused
 * unless the caller has explicitly allow-listed the methods that may be invoked.  This closes off
 * reflective-invoke-over-the-wire exposure (e.g. a REST endpoint that feeds untrusted method/argument strings
 * straight into an introspector).
 *
 * <h5 class='section'>How to fix:</h5><ul>
 * 	<li>
 * 		Allow-list the specific method(s) you intend to expose:
 * 		<p class='bjava'>
 * 			ObjectIntrospector.<jsm>create</jsm>(<jv>o</jv>).allow(MyBean.<jk>class</jk>, <js>"getName"</js>).invokeMethod(...);
 * 		</p>
 * 	<li>
 * 		Or, for trusted in-process callers only, restore the pre-10.0 behavior of allowing any public,
 * 		non-deprecated method:
 * 		<p class='bjava'>
 * 			ObjectIntrospector.<jsm>create</jsm>(<jv>o</jv>).allowAll().invokeMethod(...);
 * 		</p>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ObjectIntrospector}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ObjectTools">Object Tools</a>
 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110" // Deep inheritance inherent to the exception hierarchy
})
public class MethodNotAllowlistedException extends BasicRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message Message.
	 * @param args Message arguments.
	 */
	public MethodNotAllowlistedException(String message, Object...args) {
		super(message, args);
	}

	@Override /* Overridden from BasicRuntimeException */
	public MethodNotAllowlistedException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}
}
