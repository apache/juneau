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
package org.apache.juneau.rest.vars;

import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * Localized string variable resolver.
 *
 * <p>
 * The format for this var is <js>"$L{key[,args...]}"</js>.
 *
 * <p>
 * This variable resolver requires that a {@link RestRequest} bean be available in the session bean store.
 *
 * <p>
 * Values are pulled from the {@link RestRequest#getMessage(String,Object[])} method.
 * These in turn are pulled from the resource bundle associated with the servlet class where the request was made.
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class LocalizationVar extends MultipartVar {

	/** The name of this variable. */
	public static final String NAME = "L";

	/**
	 * Constructor.
	 */
	public LocalizationVar() {
		super(NAME);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String[] args) {
		if (args.length > 0) {
			String key = args[0];
			String[] a = (args.length > 1) ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
			Messages messages = null;
			if (session.getBean(RestRequest.class).isPresent())
				messages = session.getBean(RestRequest.class).get().getMessages();
			if (messages == null)
				messages = session.getBean(Messages.class).orElseThrow(InternalServerError::new);
			return messages.getString(key, (Object[])a);
		}
		return "";
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(Messages.class).isPresent() || session.getBean(RestRequest.class).isPresent();
	}
}
