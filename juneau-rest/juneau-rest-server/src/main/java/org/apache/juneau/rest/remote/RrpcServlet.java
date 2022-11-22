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
package org.apache.juneau.rest.remote;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;

/**
 * Abstract class for defining Remote Interface Services.
 *
 * <p>
 * Remote Interface Services are POJOs whose methods can be invoked remotely through proxy interfaces.
 *
 * <p>
 * To implement a remote interface service, developers must simply subclass from this class and implement the
 * {@link #getServiceMap()} method that maps java interfaces to POJO instances.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.RestRpc">REST/RPC</a> * </ul>
 */
@SuppressWarnings({"serial","javadoc"})
public abstract class RrpcServlet extends BasicRestServlet {

	private final Map<String,RrpcInterfaceMeta> serviceMap = new ConcurrentHashMap<>();

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the list of interfaces to their implementation objects.
	 *
	 * <p>
	 * This class is called often and not cached, so any caching should occur in the subclass if necessary.
	 *
	 * @return The service map.
	 * @throws Exception Any exception.
	 */
	protected abstract Map<Class<?>,Object> getServiceMap() throws Exception;

	//-----------------------------------------------------------------------------------------------------------------
	// REST methods
	//-----------------------------------------------------------------------------------------------------------------

	@RestGet(
		path="/",
		summary="List of available remote interfaces",
		description="Shows a list of the interfaces registered with this remote interface servlet."
	)
	public List<LinkString> getInterfaces() throws Exception {
		List<LinkString> l = new LinkedList<>();
		for (Class<?> c : getServiceMap().keySet())
			l.add(new LinkString(c.getName(), "servlet:/{0}", urlEncode(c.getName())));
		return l;
	}

	@RestGet(
		path="/{javaInterface}",
		summary="List of available methods on interface",
		description="Shows a list of all the exposed methods on an interface."
	)
	@HtmlDocConfig(
		nav="<h5>Interface:  $RP{javaInterface}</h5>"
	)
	public Collection<LinkString> listMethods(
			@Path("javaInterface") @Schema(description="Java interface name") String javaInterface
		) throws Exception {

		List<LinkString> l = list();
		for (String s : getMethods(javaInterface).keySet())
			l.add(new LinkString(s, "servlet:/{0}/{1}", urlEncode(javaInterface), urlEncode(s)));
		return l;
	}

	@RestGet(
		path="/{javaInterface}/{javaMethod}",
		summary="Form entry for interface method call",
		description="Shows a form entry page for executing a remote interface method."
	)
	@HtmlDocConfig(
		nav={
			"<h5>Interface:  $RP{javaInterface}</h5>",
			"<h5>Method:  $RP{javaMethod}</h5>"
		}
	)
	public Div showEntryForm(
			@Path("javaInterface") @Schema(description="Java interface name") String javaInterface,
			@Path("javaMethod") @Schema(description="Java method name") String javaMethod
		) throws NotFound, Exception {

		// Find the method.
		RrpcInterfaceMethodMeta rmm = getMethods(javaInterface).get(javaMethod);
		if (rmm == null)
			throw new NotFound("Method not found");

		Table t = table();

		Type[] types = rmm.getJavaMethod().getGenericParameterTypes();
		if (types.length == 0) {
			t.child(tr(td("No arguments").colspan(3).style("text-align:center")));
		} else {
			t.child(tr(th("Index"),th("Type"),th("Value")));
			for (int i = 0; i < types.length; i++) {
				String type = Mutaters.toString(types[i]);
				t.child(tr(td(i), td(type), td(input().name(String.valueOf(i)).type("text"))));
			}
		}

		t.child(
			tr(
				td().colspan(3).style("text-align:right").children(
					types.length == 0 ? null : button("reset", "Reset"),
					button("button","Cancel").onclick("window.location.href='/'"),
					button("submit", "Submit")
				)
			)
		);

		return div(form().id("form").action("request:/").method(POST).children(t));
	}

	@RestPost(
		path="/{javaInterface}/{javaMethod}",
		summary="Invoke an interface method",
		description="Invoke a Java method by passing in the arguments as an array of serialized objects.\nThe returned object is then serialized to the response.",
		swagger=@OpSwagger(
			parameters= {
				"{",
					"in: 'body',",
					"description: 'Serialized array of Java objects',",
					"schema: {",
						"type': 'array'",
					"},",
					"examples: {",
						"'application/json+lax': '[\\'foo\\', 123, true]'",
					"}",
				"}"
			},
			responses= {
				"200:{ description:'The return object serialized', schema:{type:'any'},example:{foo:123} }",
			}
		)
	)
	@HtmlDocConfig(
		nav= {
			"<h5>Interface:  $RP{javaInterface}</h5>",
			"<h5>Method:  $RP{javaMethod}</h5>"
		}
	)
	public Object invoke(
			Reader r,
			ReaderParser p,
			@Header("Content-Type") ContentType contentType,
			@Path("javaInterface") @Schema(description="Java interface name") String javaInterface,
			@Path("javaMethod") @Schema(description="Java method name") String javaMethod
		) throws UnsupportedMediaType, NotFound, Exception {

		// Find the parser.
		if (p == null)
			throw new UnsupportedMediaType("Could not find parser for media type ''{0}''", contentType);
		RrpcInterfaceMeta rim = getInterfaceClass(javaInterface);

		// Find the service.
		Object service = getServiceMap().get(rim.getJavaClass());
		if (service == null)
			throw new NotFound("Service not found");

		// Find the method.
		RrpcInterfaceMethodMeta rmm = getMethods(javaInterface).get(javaMethod);
		if (rmm == null)
			throw new NotFound("Method not found");

		// Parse the args and invoke the method.
		java.lang.reflect.Method m = rmm.getJavaMethod();
		Object[] params = p.parseArgs(r, m.getGenericParameterTypes());
		return m.invoke(service, params);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private Map<String,RrpcInterfaceMethodMeta> getMethods(String javaInterface) throws Exception {
		return getInterfaceClass(javaInterface).getMethodsByPath();
	}

	/**
	 * Return the <c>Class</c> given it's name if it exists in the services map.
	 */
	private RrpcInterfaceMeta getInterfaceClass(String javaInterface) throws NotFound, Exception {
		RrpcInterfaceMeta rm = serviceMap.get(javaInterface);
		if (rm == null) {
			for (Class<?> c : getServiceMap().keySet()) {
				if (c.getName().equals(javaInterface)) {
					rm = new RrpcInterfaceMeta(c, null);
					serviceMap.put(javaInterface, rm);
					return rm;
				}
			}
			throw new NotFound("Interface class not found");
		}
		return rm;
	}
}
