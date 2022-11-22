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
package org.apache.juneau.rest.mock;

import java.io.*;
import org.apache.juneau.assertions.*;

/**
 * A capturing {@link PrintStream} that allows you to easily capture console output.
 *
 * <p>
 * Stores output into an internal {@link ByteArrayOutputStream}.  Note that this means you could run into memory
 * constraints if you heavily use this class.
 *
 * <p>
 * Typically used in conjunction with the {@link org.apache.juneau.rest.client.RestClient.Builder#console(PrintStream)} to capture console output for
 * testing purposes.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A simple REST API that echos a posted bean.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyRest <jk>extends</jk> BasicRestObject {
 * 		<ja>@RestPost</ja>(<js>"/bean"</js>)
 * 		<jk>public</jk> Bean postBean(<ja>@Content</ja> Bean <jv>bean</jv>) {
 * 			<jk>return</jk> <jv>bean</jv>;
 * 		}
 * 	}
 *
 *	<jc>// Our mock console.</jc>
 * 	MockConsole <jv>console</jv> = MockConsole.<jsm>create</jsm>();
 *
 *	<jc>// Make a call against our REST API and log the call.</jc>
 * 	MockRestClient
 * 		.<jsm>create</jsm>(MyRest.<jk>class</jk>)
 * 		.json5()
 * 		.logRequests(DetailLevel.<jsf>FULL</jsf>, Level.<jsf>SEVERE</jsf>)
 * 		.logToConsole()
 * 		.console(<jv>console</jv>)
 * 		.build()
 * 		.post(<js>"/bean"</js>, <jv>bean</jv>)
 * 		.run();
 *
 * 	<jv>console</jv>.assertContents().is(
 * 		<js>""</js>,
 * 		<js>"=== HTTP Call (outgoing) ======================================================"</js>,
 * 		<js>"=== REQUEST ==="</js>,
 * 		<js>"POST http://localhost/bean"</js>,
 * 		<js>"---request headers---"</js>,
 * 		<js>"	Accept: application/json5"</js>,
 * 		<js>"---request entity---"</js>,
 * 		<js>"	Content-Type: application/json5"</js>,
 * 		<js>"---request content---"</js>,
 * 		<js>"{f:1}"</js>,
 * 		<js>"=== RESPONSE ==="</js>,
 * 		<js>"HTTP/1.1 200 "</js>,
 * 		<js>"---response headers---"</js>,
 * 		<js>"	Content-Type: application/json"</js>,
 * 		<js>"---response content---"</js>,
 * 		<js>"{f:1}"</js>,
 * 		<js>"=== END ======================================================================="</js>,
 * 		<js>""</js>
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
public class MockConsole extends PrintStream {

	private static final ByteArrayOutputStream baos = new ByteArrayOutputStream();

	/**
	 * Constructor.
	 */
	public MockConsole() {
		super(baos);
	}

	/**
	 * Creator.
	 *
	 * @return A new {@link MockConsole} object.
	 */
	public static MockConsole create() {
		return new MockConsole();
	}

	/**
	 * Resets the contents of this buffer.
	 *
	 * @return This object.
	 */
	public synchronized MockConsole reset() {
		baos.reset();
		return this;
	}

	/**
	 * Allows you to perform fluent-style assertions on the contents of this buffer.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	MockConsole <jv>console</jv> = MockConsole.<jsf>create</jsf>();
	 *
	 * 	MockRestClient
	 * 		.<jsm>create</jsm>(MyRest.<jk>class</jk>)
	 * 		.console(<jv>console</jv>)
	 * 		.debug()
	 * 		.json5()
	 * 		.build()
	 * 		.get(<js>"/url"</js>)
	 * 		.run();
	 *
	 * 	<jv>console</jv>.assertContents().isContains(<js>"HTTP GET /url"</js>);
	 * </p>
	 *
	 * @return A new fluent-style assertion object.
	 */
	public synchronized FluentStringAssertion<MockConsole> assertContents() {
		return new FluentStringAssertion<>(toString(), this);
	}

	/**
	 * Allows you to perform fluent-style assertions on the size of this buffer.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	MockConsole <jv>console</jv> = MockConsole.<jsf>create</jsf>();
	 *
	 * 	MockRestClient
	 * 		.<jsm>create</jsm>(MyRest.<jk>class</jk>)
	 * 		.console(<jv>console</jv>)
	 * 		.debug()
	 * 		.json5()
	 * 		.build()
	 * 		.get(<js>"/url"</js>)
	 * 		.run();
	 *
	 * 	<jv>console</jv>.assertSize().isGreaterThan(0);
	 * </p>
	 *
	 * @return A new fluent-style assertion object.
	 */
	public synchronized FluentIntegerAssertion<MockConsole> assertSize() {
		return new FluentIntegerAssertion<>(baos.size(), this);
	}

	/**
	 * Returns the contents of this buffer as a string.
	 */
	@Override
	public String toString() {
		return baos.toString();
	}
}
