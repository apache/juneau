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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Rest info variable resolver.
 *
 * <p>
 * The format for this var is <js>"$RI{key1[,key2...]}"</js>.
 *
 * <p>
 * Used to resolve values returned by {@link RestRequest#getInfoProvider()}..
 * <br>When multiple keys are used, returns the first non-null/empty value.
 *
 * <p>
 * The possible values are:
 * <ul>
 * 	<li><js>"contact"</js> - Value returned by {@link Info#getContact()}
 * 	<li><js>"description"</js> - Value returned by {@link RestInfoProvider#getDescription(RestRequest)}
 * 	<li><js>"externalDocs"</js> - Value returned by {@link Swagger#getExternalDocs()}
 * 	<li><js>"license"</js> - Value returned by {@link Info#getLicense()}
 * 	<li><js>"methodDescription"</js> - Value returned by {@link RestInfoProvider#getMethodDescription(Method,RestRequest)}
 * 	<li><js>"methodSummary"</js> - Value returned by {@link RestInfoProvider#getMethodSummary(Method,RestRequest)}
 * 	<li><js>"siteName"</js> - Value returned by {@link RestInfoProvider#getSiteName(RestRequest)}
 * 	<li><js>"tags"</js> - Value returned by {@link Swagger#getTags()}
 * 	<li><js>"termsOfService"</js> - Value returned by {@link Info#getTermsOfService()}
 * 	<li><js>"title"</js> - See {@link RestInfoProvider#getTitle(RestRequest)}
 * 	<li><js>"version"</js> - See {@link Info#getVersion()}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	String title = restRequest.resolveVars(<js>"$RI{title}"</js>);
 * 	String titleOrDescription = restRequest.resolveVars(<js>"$RI{title,description}"</js>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		This variable resolver requires that a {@link RestRequest} object be set as a context object on the resolver
 * 		or a session object on the resolver session.
 * 	<li>
 * 		For security reasons, nested and recursive variables are not resolved.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.SvlVariables">Overview &gt; juneau-rest-server &gt; SVL Variables</a>
 * </ul>
 */
public class RestInfoVar extends MultipartResolvingVar {

	/**
	 * The name of the session or context object that identifies the {@link RestRequest} object.
	 */
	public static final String SESSION_req = "req";


	/** The name of this variable. */
	public static final String NAME = "RI";

	/**
	 * Constructor.
	 */
	public RestInfoVar() {
		super(NAME);
	}

	@Override /* Var */
	protected boolean allowNested() {
		return false;
	}

	@Override /* Var */
	protected boolean allowRecurse() {
		return false;
	}

	@Override /* Parameter */
	public String resolve(VarResolverSession session, String key) throws RestException, InternalServerError {
		try {
			RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req, true);
			Swagger swagger = req.getSwagger();
			RestInfoProvider rip = req.getInfoProvider();
			WriterSerializer s = SimpleJsonSerializer.DEFAULT;
			char c = StringUtils.charAt(key, 0);
			if (c == 'c') {
				if ("contact".equals(key)) {
					Contact x = swagger.getInfo().getContact();
					return x == null ? null : s.toString(x);
				}
			} else if (c == 'd') {
				if ("description".equals(key))
					return rip.getDescription(req);
			} else if (c == 'e') {
				if ("externalDocs".equals(key)) {
					ExternalDocumentation x = swagger.getExternalDocs();
					return x == null ? null : s.toString(x);
				}
			} else if (c == 'l') {
				if ("license".equals(key)) {
					License x = swagger.getInfo().getLicense();
					return x == null ? null : s.toString(x);
				}
			} else if (c == 'm') {
				if ("methodDescription".equals(key))
					return rip.getMethodDescription(req.getJavaMethod(), req);
				if ("methodSummary".equals(key))
					return rip.getMethodSummary(req.getJavaMethod(), req);
			} else if (c == 's') {
				if ("siteName".equals(key))
					return rip.getSiteName(req);
			} else if (c == 't') {
				if ("tags".equals(key)) {
					List<Tag> x = swagger.getTags();
					return x == null ? null : s.toString(x);
				} else if ("termsOfService".equals(key)) {
					return swagger.getInfo().getTermsOfService();
				} else if ("title".equals(key)) {
					return swagger.getInfo().getTitle();
				}
			} else if (c == 'v') {
				if ("version".equals(key))
					return swagger.getInfo().getVersion();
			}
			return null;
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}
}