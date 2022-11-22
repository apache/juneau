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

import org.apache.juneau.common.internal.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Rest info variable resolver.
 *
 * <p>
 * The format for this var is <js>"$RS{key1[,key2...]}"</js>.
 *
 * <p>
 * Used to resolve values returned by {@link RestRequest#getSwagger()}..
 * <br>When multiple keys are used, returns the first non-null/empty value.
 *
 * <p>
 * The possible values are:
 * <ul>
 * 	<li><js>"contact"</js> - Value returned by {@link Info#getContact()}
 * 	<li><js>"description"</js> - Value returned by {@link Info#getDescription()}
 * 	<li><js>"externalDocs"</js> - Value returned by {@link Swagger#getExternalDocs()}
 * 	<li><js>"license"</js> - Value returned by {@link Info#getLicense()}
 * 	<li><js>"operationDescription"</js> - Value returned by {@link Operation#getDescription()}
 * 	<li><js>"operationSummary"</js> - Value returned by {@link Operation#getSummary()}
 * 	<li><js>"siteName"</js> - Value returned by {@link Info#getSiteName()}
 * 	<li><js>"tags"</js> - Value returned by {@link Swagger#getTags()}
 * 	<li><js>"termsOfService"</js> - Value returned by {@link Info#getTermsOfService()}
 * 	<li><js>"title"</js> - See {@link Info#getTitle()}
 * 	<li><js>"version"</js> - See {@link Info#getVersion()}
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>title</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$RS{title}"</js>);
 * 	String <jv>titleOrDescription</jv> = <jv>restRequest</jv>.getVarResolver().resolve(<js>"$RS{title,description}"</js>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This variable resolver requires that a {@link RestRequest} bean be available in the session bean store.
 * 	<li class='note'>
 * 		For security reasons, nested and recursive variables are not resolved.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class RequestSwaggerVar extends MultipartResolvingVar {

	/** The name of this variable. */
	public static final String NAME = "RS";

	/**
	 * Constructor.
	 */
	public RequestSwaggerVar() {
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

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) throws BasicHttpException, InternalServerError {
		try {
			RestRequest req = session.getBean(RestRequest.class).orElseThrow(InternalServerError::new);
			Optional<Swagger> swagger = req.getSwagger();
			WriterSerializer s = Json5Serializer.DEFAULT;
			Optional<Operation> methodSwagger = req.getOperationSwagger();
			char c = StringUtils.charAt(key, 0);
			if (c == 'c') {
				if ("contact".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getContact()).map(StringUtils::stringify).orElse(null);
			} else if (c == 'd') {
				if ("description".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getDescription()).orElse(null);
			} else if (c == 'e') {
				if ("externalDocs".equals(key))
					return swagger.map(Swagger::getExternalDocs).map(ExternalDocumentation::toString).orElse(null);
			} else if (c == 'l') {
				if ("license".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getLicense()).map(StringUtils::stringify).orElse(null);
			} else if (c == 'o') {
				if ("operationDescription".equals(key))
					return methodSwagger.map(Operation::getDescription).orElse(null);
				if ("operationSummary".equals(key))
					return methodSwagger.map(Operation::getSummary).orElse(null);
			} else if (c == 'r') {
				if ("siteName".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getSiteName()).orElse(null);
			} else if (c == 't') {
				if ("tags".equals(key))
					return swagger.map(Swagger::getTags).map(x -> s.toString(x)).orElse(null);
				if ("termsOfService".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getTermsOfService()).orElse(null);
				if ("title".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getTitle()).orElse(null);
			} else if (c == 'v') {
				if ("version".equals(key))
					return swagger.map(Swagger::getInfo).map(x -> x == null ? null : x.getVersion()).orElse(null);
			}
			return null;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}