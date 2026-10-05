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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.channels.*;

import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * A client disconnecting mid-response is routine: it must not be recorded as the request's exception.
 */
class RestSession_ClientAbort_Test {

	@Rest
	public static class A_Resource {
		@RestGet(path="/foo")
		public String foo() {
			return "OK";
		}

		@RestGet(path="/abort")
		public String abort() throws IOException {
			throw new IOException("Broken pipe");
		}

		@RestGet(path="/fail")
		public String fail() throws IOException {
			throw new IOException("disk full");
		}
	}

	/** Mimics Tomcat's ClientAbortException (matched by simple name). */
	static class ClientAbortException extends IOException {
		private static final long serialVersionUID = 1L;
		ClientAbortException(Throwable cause) { super(cause); }
	}

	/** Mimics Jetty's EofException (matched by simple name). */
	static class EofException extends EOFException {
		private static final long serialVersionUID = 1L;
	}

	/** Response whose flushBuffer fails. */
	static class FlushFailsResponse extends MockServletResponse {
		private final IOException e;
		FlushFailsResponse(IOException e) { this.e = e; }
		@Override public void flushBuffer() throws IOException { throw e; }
	}

	/** Response whose body stream/writer are already dead, so rendering fails. */
	static class DeadBodyResponse extends MockServletResponse {
		private final IOException e;
		DeadBodyResponse(IOException e) { this.e = e; }
		@Override public jakarta.servlet.ServletOutputStream getOutputStream() throws IOException { throw e; }
		@Override public PrintWriter getWriter() throws IOException { throw e; }
	}

	private static Object dispatch(String path, MockServletResponse res) throws Exception {
		var resource = new A_Resource();
		var ctx = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
		var req = MockServletRequest.create("GET", path).header("Accept", "text/plain");
		ctx.execute(resource, req, res);
		return req.getAttribute("Exception");
	}

	@Test void a01_flushBrokenPipe_notRecorded() throws Exception {
		assertNull(dispatch("/foo", new FlushFailsResponse(new IOException("Broken pipe"))));
	}

	@Test void a02_flushWrappedClientAbortException_notRecorded() throws Exception {
		assertNull(dispatch("/foo", new FlushFailsResponse(new ClientAbortException(new IOException("Broken pipe")))));
	}

	@Test void a03_flushOtherIoFailure_stillRecorded() throws Exception {
		var e = dispatch("/foo", new FlushFailsResponse(new IOException("disk full")));
		assertBean(e, "class,message", "IOException,disk full");
	}

	@Test void a04_renderingBrokenPipe_notRecordedAndNoErrorResponseWritten() throws Exception {
		var res = new DeadBodyResponse(new ClientAbortException(new IOException("Broken pipe")));
		assertNull(dispatch("/foo", res));
		assertNotEquals(500, res.getStatus());
	}

	@Test void a04b_renderingBackendConnectionReset_stillRecorded() throws Exception {
		var e = dispatch("/foo", new DeadBodyResponse(new IOException("Connection reset")));
		assertBean(e, "class,message", "IOException,Connection reset");
	}

	@Test void a05_operationThrowsOtherIoFailure_stillHandledAs500() throws Exception {
		var res = new MockServletResponse();
		dispatch("/fail", res);
		assertEquals(500, res.getStatus());
	}

	//------------------------------------------------------------------------------------------------------------------
	// ClientAborts.isClientAbort
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_null() {
		assertFalse(ClientAborts.isClientAbort(null));
	}

	@Test void b02_clientAbortExceptionByName() {
		assertTrue(ClientAborts.isClientAbort(new ClientAbortException(new RuntimeException())));
	}

	@Test void b03_eof() {
		assertTrue(ClientAborts.isClientAbort(new EOFException()));
	}

	@Test void b04_closedChannel() {
		assertTrue(ClientAborts.isClientAbort(new ClosedChannelException()));
	}

	@Test void b05_messages() {
		assertTrue(ClientAborts.isClientAbort(new IOException("Broken pipe")));
		assertTrue(ClientAborts.isClientAbort(new IOException("Connection reset by peer")));
		assertTrue(ClientAborts.isClientAbort(new IOException("java.io.IOException: BROKEN PIPE")));
	}

	@Test void b05b_container_strict() {
		assertTrue(ClientAborts.isContainerClientAbort(new RuntimeException(new ClientAbortException(new IOException()))));
		assertTrue(ClientAborts.isContainerClientAbort(new EofException()));
		assertFalse(ClientAborts.isContainerClientAbort(new IOException("Connection reset")));
		assertFalse(ClientAborts.isContainerClientAbort(new IOException("Broken pipe")));
		assertFalse(ClientAborts.isContainerClientAbort(new EOFException()));
		assertFalse(ClientAborts.isContainerClientAbort(new ClosedChannelException()));
		assertFalse(ClientAborts.isContainerClientAbort(null));
	}

	@Test void b06_nonAbort() {
		assertFalse(ClientAborts.isClientAbort(new IOException("disk full")));
		assertFalse(ClientAborts.isClientAbort(new IOException()));
		// Message match is limited to IOExceptions.
		assertFalse(ClientAborts.isClientAbort(new RuntimeException("Broken pipe")));
	}

	@Test void b07_nestedCause() {
		assertTrue(ClientAborts.isClientAbort(new RuntimeException("x", new IllegalStateException(new IOException("Broken pipe")))));
	}

	@Test void b08_causeCycle_terminates() {
		var a = new RuntimeException("a");
		var b = new RuntimeException("b", a);
		a.initCause(b);
		assertFalse(ClientAborts.isClientAbort(a));
	}
}
