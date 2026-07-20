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
package org.apache.juneau.rest.server.arg;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;

/**
 * General exception due to a malformed Java parameter.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JavaMethodParameters">Java Method Parameters</a>
 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for ArgException hierarchy
})
public class ArgException extends InternalServerError {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param pi The parameter with the issue. Must not be <jk>null</jk>.
	 * @param msg The message.
	 * @param args The message args.
	 */
	public ArgException(ParameterInfo pi, String msg, Object...args) {
		super(f(msg, args) + " on parameter " + pi.getIndex() + " of method " + pi.getMethod().getNameFull() + ".");
	}
}
