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
package org.apache.juneau.rest.server.converter;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.rest.server.*;

/**
 * Converter for enablement of {@link ObjectIntrospector} support on response objects returned by a
 * <c>@RestOp</c>-annotated method.
 *
 * <p>
 * When enabled, public methods can be called on objects returned through the {@link RestResponse#setContent(Object)}
 * method.
 *
 * <p>
 * Note that opening up public methods for calling through a REST interface can be dangerous, and should be done with
 * caution.
 *
 * <p>
 * 	<b>Security - deny-by-default:</b> As of Juneau 10.0, {@link ObjectIntrospector} is secure-by-default and
 * 	denies reflective method dispatch unless the caller has explicitly allow-listed the target method(s).  This
 * 	converter is <b>off by default</b> as shipped &mdash; with no {@link IntrospectableSettings} bean registered,
 * 	every <c>invokeMethod</c> request is refused with an HTTP 500 ({@link MethodNotAllowlistedException} wrapped
 * 	in {@link InternalServerError}).  A resource opts specific methods in <i>explicitly</i> by registering an
 * 	{@link IntrospectableSettings} bean in its bean store:
 * </p>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=Introspectable.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> IntrospectableSettings introspectableSettings() {
 * 			<jk>return</jk> IntrospectableSettings.<jsm>create</jsm>()
 * 				.allow(MyBean.<jk>class</jk>, <js>"getName"</js>, <js>"getAge"</js>)
 * 				.build();
 * 		}
 * 	}
 * </p>
 * <p>
 * 	For a trusted resource that needs the pre-10.0 behavior of dispatching to any public, non-deprecated method,
 * 	call {@link IntrospectableSettings.Builder#allowAll() allowAll()} instead &mdash; but never do this on a
 * 	resource whose response objects expose methods you wouldn't want an arbitrary caller to invoke, since the
 * 	method name and arguments always come from untrusted request query parameters.  See
 * 	{@link IntrospectableSettings} for the full builder API.
 * </p>
 *
 * <p>
 * Java methods are invoked by passing in the following URL parameters:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<c>&amp;invokeMethod</c> - The Java method name, optionally with arguments if necessary to
 * 		differentiate between methods.
 * 	<li>
 * 		<c>&amp;invokeArgs</c> - The arguments as an array.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ObjectIntrospector} - Additional information on introspection of POJO methods.
 * 	<li class='jc'>{@link IntrospectableSettings} - Per-resource allow-list configuration for this converter.
 * 	<li class='ja'>{@link RestOp#converters()} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 */
public class Introspectable implements RestConverter {

	/**
	 * Swagger parameters for this converter.
	 */
	public static final String SWAGGER_PARAMS = """
		{in:'query',name:'invokeMethod',description:' The Java method name, optionally with arguments if necessary to differentiate between methods.',examples:{example:'toString'}},
		{in:'query',name:'invokeArgs',description:'The arguments as an array.',examples:{example:'foo,bar'}}
		""";

	@Override /* Overridden from RestConverter */
	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this only borrows a bean and must not close it.
	})
	public Object convert(RestRequest req, Object o) throws InternalServerError {
		String method = req.getQueryParam("invokeMethod").orElse(null);
		String args = req.getQueryParam("invokeArgs").orElse(null);
		if (method == null)
			return o;
		try {
			MarshallingSession bs = req.getMarshallingSession();
			var swap = bs.getClassMetaForObject(o).getSwap(bs);
			if (nn(swap))
				o = swap.swap(bs, o);
			var settings = req.getContext().getBeanStore().getBean(IntrospectableSettings.class).orElse(IntrospectableSettings.DEFAULT);
			return ObjectIntrospector.create(o, Json5Parser.DEFAULT).allow(settings.asFilter()).invokeMethod(method, args);
		} catch (Exception e) {
			return new InternalServerError(e, "Error occurred trying to invoke method: %s", localizedMessage(e));
		}
	}
}
