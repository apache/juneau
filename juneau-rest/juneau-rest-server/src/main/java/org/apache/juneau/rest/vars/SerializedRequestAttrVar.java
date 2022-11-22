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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Serialized request attribute variable resolver.
 *
 * <p>
 * The format for this var is <js>"$SA{contentType,key[,defaultValue]}"</js>.
 *
 * <p>
 * This variable resolver requires that a {@link RestRequest} bean be available in the session bean store.
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class SerializedRequestAttrVar extends StreamedVar {

	/** The name of this variable. */
	public static final String NAME = "SA";

	/**
	 * Constructor.
	 */
	public SerializedRequestAttrVar() {
		super(NAME);
	}

	@Override /* Var */
	public void resolveTo(VarResolverSession session, Writer w, String key) throws Exception {
		int i = key.indexOf(',');
		if (i == -1)
			throw new RuntimeException("Invalid format for $SA var.  Must be of the format $SA{contentType,key[,defaultValue]}");
		String[] s2 = split(key);
		RestRequest req = session.getBean(RestRequest.class).orElseThrow(InternalServerError::new);
		Object o = req.getAttribute(key).orElse(key);
		Serializer s = req.getOpContext().getSerializers().getSerializer(s2[0]);
		if (s != null)
			s.serialize(w, o);
	}

	@Override  /* Var */
	protected boolean allowNested() {
		return false;
	}

	@Override  /* Var */
	protected boolean allowRecurse() {
		return false;
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}
