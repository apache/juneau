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
package org.apache.juneau.http.remote;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.lang.reflect.*;

import org.apache.juneau.reflect.*;

/**
 * Contains the meta-data about a Java method on a remote class.
 *
 * <p>
 * Captures the information in {@link Remote @Remote} annotations for caching and reuse.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
 * </ul>
 */
public class RrpcInterfaceMethodMeta {

	private final String url, path;
	private final Method method;

	/**
	 * Constructor.
	 *
	 * @param restUrl The absolute URL of the REST interface backing the interface proxy.
	 * @param m The Java method.
	 */
	public RrpcInterfaceMethodMeta(final String restUrl, Method m) {
		this.method = m;
		this.path =  m.getName() + '/' + getMethodArgsSignature(m);
		this.url = trimSlashes(restUrl) + '/' + urlEncode(path);
	}

	/**
	 * Given a Java method, returns the arguments signature.
	 *
	 * @param m The Java method.
	 * @param full Whether fully-qualified names should be used for arguments.
	 * @return The arguments signature for the specified method.
	 */
	private static String getMethodArgsSignature(Method m) {
		StringBuilder sb = new StringBuilder(128);
		Class<?>[] pt = m.getParameterTypes();
		if (pt.length == 0)
			return "";
		sb.append('(');
		for (int i = 0; i < pt.length; i++) {
			ClassInfo pti = ClassInfo.of(pt[i]);
			if (i > 0)
				sb.append(',');
			pti.appendFullName(sb);
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Returns the absolute URL of the REST interface invoked by this Java method.
	 *
	 * @return The absolute URL of the REST interface, never <jk>null</jk>.
	 */
	public String getUri() {
		return url;
	}

	/**
	 * Returns the HTTP path of this method.
	 *
	 * @return
	 * 	The HTTP path of this method relative to the parent interface.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>Never has leading or trailing slashes.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the underlying Java method that this metadata is about.
	 *
	 * @return
	 * 	The underlying Java method that this metadata is about.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Method getJavaMethod() {
		return method;
	}
}
