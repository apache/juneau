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
package org.apache.juneau.rest.remoteable;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.exception.*;

/**
 * Abstract class for defining Remoteable services.
 * 
 * <p>
 * Remoteable services are POJOs whose methods can be invoked remotely through proxy interfaces.
 * 
 * <p>
 * To implement a remoteable service, developers must simply subclass from this class and implement the
 * {@link #getServiceMap()} method that maps java interfaces to POJO instances.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.RemoteableProxies">Overview &gt; juneau-rest-server &gt; Remoteable Proxies</a>
 * </ul>
 */
@SuppressWarnings("serial")
public abstract class RemoteableServlet extends BasicRestServlet {

	private final Map<String,Class<?>> classNameMap = new ConcurrentHashMap<>();

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the list of interfaces to their implementation objects.
	 * 
	 * <p>
	 * This class is called often and not cached, so any caching should occur in the subclass if necessary.
	 * 
	 * @return The service map.
	 * @throws Exception
	 */
	protected abstract Map<Class<?>,Object> getServiceMap() throws Exception;

	//--------------------------------------------------------------------------------
	// REST methods
	//--------------------------------------------------------------------------------

	/**
	 * [GET /] - Get the list of all remote interfaces.
	 * 
	 * @param req The HTTP servlet request.
	 * @return The list of links to the remote interfaces.
	 * @throws Exception
	 */
	@RestMethod(name=GET, path="/")
	public List<LinkString> getInterfaces(RestRequest req) throws Exception {
		List<LinkString> l = new LinkedList<>();
		boolean useAll = ! useOnlyAnnotated();
		for (Class<?> c : getServiceMap().keySet()) {
			if (useAll || getContext().getBeanContext().getClassMeta(c).isRemoteable())
				l.add(new LinkString(c.getName(), "{0}/{1}", req.getRequestURI(), urlEncode(c.getName())));
		}
		return l;
	}

	/**
	 * [GET /{javaInterface] - Get the list of all remoteable methods on the specified interface name.
	 * 
	 * @param req The HTTP servlet request.
	 * @param javaInterface The Java interface name.
	 * @return The methods defined on the interface.
	 * @throws Exception
	 */
	@RestMethod(name=GET, path="/{javaInterface}", summary="List of available methods on $RP{javaInterface}.")
	public Collection<LinkString> listMethods(RestRequest req, @Path("javaInterface") String javaInterface) throws Exception {
		List<LinkString> l = new ArrayList<>();
		for (String s : getMethods(javaInterface).keySet()) {
			l.add(new LinkString(s, "{0}/{1}", req.getRequestURI(), urlEncode(s)));
		}
		return l;
	}

	/**
	 * [GET /{javaInterface] - Get the list of all remoteable methods on the specified interface name.
	 * 
	 * @param req The HTTP servlet request.
	 * @param javaInterface The Java interface name.
	 * @param javaMethod The Java method name or signature.
	 * @return A simple form entry page for invoking a remoteable method.
	 * @throws NotFound 
	 * @throws Exception
	 */
	@RestMethod(name=GET, path="/{javaInterface}/{javaMethod}", summary="Form entry for method $RP{javaMethod} on interface $RP{javaInterface}")
	public Div showEntryForm(RestRequest req, @Path("javaInterface") String javaInterface, @Path("javaMethod") String javaMethod) throws NotFound, Exception {
		
		// Find the method.
		java.lang.reflect.Method m = getMethods(javaInterface).get(javaMethod);
		if (m == null)
			throw new NotFound("Method not found");

		Table t = table();
		
		Type[] types = m.getGenericParameterTypes();
		if (types.length == 0) {
			t.child(tr(td("No arguments").colspan(3).style("text-align:center")));
		} else {
			t.child(tr(th("Index"),th("Type"),th("Value")));
			for (int i = 0; i < types.length; i++) {
				String type = ClassUtils.toString(types[i]);
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

	/**
	 * [POST /{javaInterface}/{javaMethod}] - Invoke the specified service method.
	 * 
	 * @param req The HTTP request.
	 * @param javaInterface The Java interface name.
	 * @param javaMethod The Java method name or signature.
	 * @return The results from invoking the specified Java method.
	 * @throws UnsupportedMediaType 
	 * @throws NotFound 
	 * @throws Exception
	 */
	@RestMethod(name=POST, path="/{javaInterface}/{javaMethod}")
	public Object invoke(RestRequest req, @Path String javaInterface, @Path String javaMethod) throws UnsupportedMediaType, NotFound, Exception {

		// Find the parser.
		ReaderParser p = req.getBody().getReaderParser();
		if (p == null)
			throw new UnsupportedMediaType("Could not find parser for media type ''{0}''", req.getHeaders().getContentType());
		Class<?> c = getInterfaceClass(javaInterface);

		// Find the service.
		Object service = getServiceMap().get(c);
		if (service == null)
			throw new NotFound("Service not found");

		// Find the method.
		java.lang.reflect.Method m = getMethods(javaInterface).get(javaMethod);
		if (m == null)
			throw new NotFound("Method not found");

		// Parse the args and invoke the method.
		Object[] params = p.parseArgs(req.getReader(), m.getGenericParameterTypes());
		return m.invoke(service, params);
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	private boolean useOnlyAnnotated() {
		return getProperties().getBoolean(RemoteableServiceProperties.REMOTEABLE_includeOnlyRemotableMethods, false);
	}

	private Map<String,java.lang.reflect.Method> getMethods(String javaInterface) throws Exception {
		Class<?> c = getInterfaceClass(javaInterface);
		ClassMeta<?> cm = getContext().getBeanContext().getClassMeta(c);
		return (useOnlyAnnotated() ? cm.getRemoteableMethods() : cm.getPublicMethods());
	}

	/**
	 * Return the <code>Class</code> given it's name if it exists in the services map.
	 */
	private Class<?> getInterfaceClass(String javaInterface) throws NotFound, Exception {
		Class<?> c = classNameMap.get(javaInterface);
		if (c == null) {
			for (Class<?> c2 : getServiceMap().keySet())
				if (c2.getName().equals(javaInterface)) {
					classNameMap.put(javaInterface, c2);
					return c2;
				}
			throw new NotFound("Interface class not found");
		}
		return c;
	}
}
