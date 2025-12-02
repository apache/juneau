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
package org.apache.juneau.commons.io;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

/**
 * A wrapper around an {@link OutputStream} that prevents the underlying stream from being closed.
 *
 * <p>
 * This class wraps an existing {@link OutputStream} and intercepts the {@link #close()} method,
 * making it a no-op (except for flushing). All other operations are delegated to the underlying
 * stream. This is useful when you need to pass a stream to code that will close it, but you
 * want to keep the underlying stream open for further use.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Prevents closing - {@link #close()} flushes but doesn't close the underlying stream
 * 	<li>Transparent delegation - all other operations pass through to the wrapped stream
 * 	<li>Useful for resource management - allows multiple consumers without premature closing
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Passing streams to APIs that close them, but you need to keep the stream open
 * 	<li>Multiple operations on the same stream where intermediate operations might close it
 * 	<li>Resource management scenarios where you control the stream lifecycle
 * 	<li>Wrapping system streams (System.out, System.err) that should not be closed
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Wrap a stream that should not be closed</jc>
 * 	FileOutputStream <jv>fos</jv> = <jk>new</jk> FileOutputStream(<js>"output.txt"</js>);
 * 	NoCloseOutputStream <jv>wrapper</jv> = <jk>new</jk> NoCloseOutputStream(<jv>fos</jv>);
 *
 * 	<jc>// Pass to code that might close it</jc>
 * 	<jv>someMethod</jv>(<jv>wrapper</jv>);  <jc>// May call close(), but fos remains open</jc>
 *
 * 	<jc>// Continue using the original stream</jc>
 * 	<jv>fos</jv>.write(<js>"more data"</js>.getBytes());
 * 	<jv>fos</jv>.close();  <jc>// Close when actually done</jc>
 * </p>
 *
 * <h5 class='section'>Important Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The {@link #close()} method flushes the stream but does not close the underlying stream
 * 	<li>You are responsible for closing the underlying stream when you're done with it
 * 	<li>This wrapper does not prevent resource leaks - ensure the underlying stream is eventually closed
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link NoCloseWriter} - Writer counterpart
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class NoCloseOutputStream extends OutputStream {

	private final OutputStream os;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new NoCloseOutputStream that wraps the specified OutputStream. The wrapper
	 * will prevent the underlying stream from being closed via the {@link #close()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FileOutputStream <jv>fos</jv> = <jk>new</jk> FileOutputStream(<js>"file.txt"</js>);
	 * 	NoCloseOutputStream <jv>wrapper</jv> = <jk>new</jk> NoCloseOutputStream(<jv>fos</jv>);
	 * </p>
	 *
	 * @param os The OutputStream to wrap. Must not be <jk>null</jk>.
	 */
	public NoCloseOutputStream(OutputStream os) {
		this.os = assertArgNotNull("os", os);
	}

	/**
	 * Flushes the stream but does not close the underlying OutputStream.
	 *
	 * <p>
	 * This method flushes any buffered data to the underlying stream but does not close it.
	 * The underlying stream remains open and can continue to be used after this method is called.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FileOutputStream <jv>fos</jv> = <jk>new</jk> FileOutputStream(<js>"file.txt"</js>);
	 * 	NoCloseOutputStream <jv>wrapper</jv> = <jk>new</jk> NoCloseOutputStream(<jv>fos</jv>);
	 * 	<jv>wrapper</jv>.close();  <jc>// Flushes but doesn't close fos</jc>
	 * 	<jv>fos</jv>.write(<js>"still works"</js>.getBytes());  <jc>// fos is still open</jc>
	 * </p>
	 *
	 * @throws IOException If an I/O error occurs while flushing.
	 */
	@Override /* Overridden from OutputStream */
	public void close() throws IOException {
		os.flush();
	}

	@Override /* Overridden from OutputStream */
	public void flush() throws IOException {
		os.flush();
	}

	@Override /* Overridden from OutputStream */
	public void write(byte[] b) throws IOException {
		assertArgNotNull("b", b);
		os.write(b);
	}

	@Override /* Overridden from OutputStream */
	public void write(byte[] b, int off, int len) throws IOException {
		assertArgNotNull("b", b);
		os.write(b, off, len);
	}

	@Override /* Overridden from OutputStream */
	public void write(int b) throws IOException {
		os.write(b);
	}
}