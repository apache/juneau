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
package org.apache.juneau.rest.rrpc;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;

/**
 * A session for a single HTTP request against an RRPC Java method.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a>
 * </ul>
 */
public class RrpcRestOpSession extends RestOpSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param ctx The context of the RRPC Java Method.
	 * @param session The REST session creating this session.
	 * @return A new builder.
	 */
	public static Builder create(RrpcRestOpContext ctx, RestSession session) {
		return new Builder(ctx, session);

	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends RestOpSession.Builder {

		RrpcRestOpContext ctx;

		/**
		 * Constructor.
		 *
		 * @param ctx The context object of the RRPC Java method.
		 * @param session The REST session.
		 */
		public Builder(RrpcRestOpContext ctx, RestSession session) {
			super(ctx, session);
			this.ctx = ctx;
		}

		@Override
		public RrpcRestOpSession build() {
			return new RrpcRestOpSession(this);
		}

	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final RrpcRestOpContext ctx;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected RrpcRestOpSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
	}

	@Override
	public void run() throws Throwable {

		super.run();

		RestRequest req = getRequest();
		RestResponse res = getResponse();
		RestSession session = getRestSession();

		final Object o = res.hasContent() ? res.getContent(Object.class) : null;

		if ("GET".equals(session.getMethod())) {
			res.setContent(ctx.getMeta().getMethodsByPath().keySet());
			return;

		} else if ("POST".equals(session.getMethod())) {
			String pip = session.getUrlPath().getPath();
			if (pip.indexOf('/') != -1)
				pip = pip.substring(pip.lastIndexOf('/')+1);
			pip = urlDecode(pip);
			RrpcInterfaceMethodMeta rmm = ctx.getMeta().getMethodMetaByPath(pip);
			if (rmm != null) {
				Method m = rmm.getJavaMethod();
				try {
					// Parse the args and invoke the method.
					Parser p = req.getContent().getParserMatch().get().getParser();
					Object[] args = null;
					if (m.getGenericParameterTypes().length == 0)
						args = new Object[0];
					else {
						try (Closeable in = p.isReaderParser() ? req.getReader() : req.getInputStream()) {
							args = p.parseArgs(in, m.getGenericParameterTypes());
						}
					}
					res.setContent(m.invoke(o, args));
					return;
				} catch (BasicHttpException e) {
					throw e;
				} catch (Exception e) {
					throw new InternalServerError(e);
				}
			}
		}
		throw new NotFound();
	}
}
