/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.server.RestServletProperties.*;

import java.util.*;

import com.ibm.juno.samples.addressbook.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.remoteable.*;

/**
 * Class showing the functionality of the RemoteableServlet class.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("serial")
@RestResource(
	path="/remoteable",
	messages="nls/SampleRemoteableServlet",
	properties={
		@Property(name=HTMLDOC_title, value="Remoteable Service Proxy API"),
		@Property(name=HTMLDOC_description, value="Sample class showing how to use remoteable proxies.  The list below are exposed services that can be retrieved using RestClient.getProxyInterface(Class)."),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.SampleRemoteableServlet)'}"),
		// Allow us to use method=POST from a browser.
		@Property(name=REST_allowMethodParam, value="*")
	},
	stylesheet="styles/devops.css"
)
public class SampleRemoteableServlet extends RemoteableServlet {

	AddressBook addressBook = new AddressBook();

	@Override /* RemoteableServlet */
	protected Map<Class<?>,Object> getServiceMap() throws Exception {
		Map<Class<?>,Object> m = new LinkedHashMap<Class<?>,Object>();

		// In this simplified example, we expose the same POJO service under two different interfaces.
		// One is IAddressBook which only exposes methods defined on that interface, and
		// the other is AddressBook itself which exposes all methods defined on the class itself.
		m.put(IAddressBook.class, addressBook);
		m.put(AddressBook.class, addressBook);
		return m;
	}
}
