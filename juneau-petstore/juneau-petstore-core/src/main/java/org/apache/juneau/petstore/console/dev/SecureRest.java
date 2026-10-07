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
package org.apache.juneau.petstore.console.dev;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;

import org.apache.juneau.petstore.console.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * Secure: an open console page demonstrating the bearer-token guarded {@link SecureApiRest}.
 *
 * <p>
 * The page's "Try it" card calls {@code /console/dev/secure/api/whoami} from the browser with no token, the demo
 * token or a wrong token, and shows the status and body inline.  The demo tokens come from
 * {@code StubBearerTokenValidator}: {@code petstore-user} (alice) and {@code petstore-admin} (admin).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bftl'>
 * 	&lt;@page tab="dev/secure" init=["/console/dev/secure/secure.js"]&gt;
 * 	&lt;@card id="try" title="Try it"&gt;
 * 	&lt;button type="button" data-secure-call="petstore-user"&gt;With the demo token&lt;/button&gt;
 * 	&lt;output id="secure-result"&gt;&lt;/output&gt;
 * 	&lt;/@card&gt;
 * 	&lt;/@page&gt;
 * </p>
 */
@Rest(path="/secure", title="Secure", children={SecureApiRest.class})
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
	"resource" // The response writer is owned by the servlet container; Eclipse JDT @Owning warning is by design.
})
public class SecureRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** @return The page. */
	@RestGet(path="/")
	public View page() {
		return FreemarkerView.of("secure.ftlh");
	}

	/**
	 * Serves the page-local script.
	 *
	 * @param res The response.
	 * @throws IOException If the bundled script can't be read.
	 */
	@RestGet(path="/secure.js")
	public void script(RestResponse res) throws IOException {
		try (var in = SecureRest.class.getResourceAsStream("secure.js")) {
			if (in == null)
				throw new IllegalStateException("Missing classpath resource 'secure.js' beside " + SecureRest.class.getName());
			res.setContentType("text/javascript;charset=utf-8");
			res.getWriter().write(new String(in.readAllBytes(), UTF_8));
		}
	}
}
