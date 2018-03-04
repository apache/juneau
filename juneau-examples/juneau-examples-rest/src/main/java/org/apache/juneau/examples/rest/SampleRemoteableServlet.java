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
package org.apache.juneau.examples.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import java.util.*;
import java.util.Map;

import org.apache.juneau.dto.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.examples.addressbook.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.remoteable.*;

/**
 * Class showing the functionality of the RemoteableServlet class.
 */
@SuppressWarnings("serial")
@RestResource(
	path="/remoteable",
	messages="nls/SampleRemoteableServlet",
	title="Remoteable Service Proxy API",
	description="Sample class showing how to use remoteable proxies.",
	htmldoc=@HtmlDoc(
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		}
	),
	// Allow us to use method=POST from a browser.
	allowedMethodParams="*"
)
public class SampleRemoteableServlet extends RemoteableServlet {

	private final AddressBook addressBook;
	
	public SampleRemoteableServlet() {
		addressBook = new AddressBook();
		addressBook.init();
	}

	@Override /* RemoteableServlet */
	protected Map<Class<?>,Object> getServiceMap() throws Exception {
		Map<Class<?>,Object> m = new LinkedHashMap<>();

		// In this simplified example, we expose the same POJO service under two different interfaces.
		// One is IAddressBook which only exposes methods defined on that interface, and
		// the other is AddressBook itself which exposes all methods defined on the class itself.
		m.put(IAddressBook.class, addressBook);
		m.put(AddressBook.class, addressBook);
		return m;
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods to provide aside messages
	//-----------------------------------------------------------------------------------------------------------------
	
	@Override /* RemoteableServlet */
	@RestMethod(name=GET, path="/",
		htmldoc=@HtmlDoc(
			aside={
				"<div style='max-width:400px;min-width:200px' class='text'>",
				"	<p>Shows how to use the <code>RemoteableServlet</code> class to define RPC-style remoteable interfaces using REST as a protocol.</p>",
				"	<p>Remoteable proxies are retrieved on the client side using <code>RestClient.getInterfaceProxy(Class)</code>.</p>",
				"	<p>Methods are invoked using POSTs of serialized arrays of objects and the returned value is marshalled back as a response.</p>",
				"	<p>This page shows the list of keys returned by the getServiceMap() method which returns a Map&lt;Class,Object&gt; which maps interfaces to objects.</p>",
				"	<p>This example shows the differences between using interfaces (preferred) and classes as keys.</p>",
				"</div>"
			}
		)
	)
	public List<LinkString> getInterfaces(RestRequest req) throws Exception {
		return super.getInterfaces(req);
	}

	@Override /* RemoteableServlet */
	@RestMethod(name=GET, path="/{javaInterface}", summary="List of available methods on $RP{javaInterface}.",
		htmldoc=@HtmlDoc(
			aside={
				"<div style='max-width:400px;min-width:200px' class='text'>",
				"	<p>Shows the list of methods defined on this interface.</p>",
				"	<p>Links take you to a form-entry page for testing.</p>",
				"</div>"
			}
		)
	)
	public Collection<LinkString> listMethods(RestRequest req, @Path("javaInterface") String javaInterface) throws Exception {
		return super.listMethods(req, javaInterface);
	}

	@Override /* RemoteableServlet */
	@RestMethod(name=GET, path="/{javaInterface}/{javaMethod}", summary="Form entry for method $RP{javaMethod} on interface $RP{javaInterface}",
		htmldoc=@HtmlDoc(
			aside={
				"<div style='max-width:400px;min-width:200px' class='text'>",
				"	<p>A rudimentary form-entry page.</p>",
				"	<p>When using this form, arguments are passed as a URL-encoded FORM post.</p>",
				"	<p>However, other formats such as JSON and XML can also be posted.</p>",
				"</div>"
			}
		)
	)
	public Div showEntryForm(RestRequest req, @Path("javaInterface") String javaInterface, @Path("javaMethod") String javaMethod) throws Exception {
		return super.showEntryForm(req, javaInterface, javaMethod);
	} 
}
