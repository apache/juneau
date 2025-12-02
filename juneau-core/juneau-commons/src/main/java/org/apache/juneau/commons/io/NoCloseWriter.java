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
 * A wrapper around a {@link Writer} that prevents the underlying writer from being closed.
 *
 * <p>
 * This class wraps an existing {@link Writer} and intercepts the {@link #close()} method,
 * making it a no-op (except for flushing). All other operations are delegated to the underlying
 * writer. This is useful when you need to pass a writer to code that will close it, but you
 * want to keep the underlying writer open for further use.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Prevents closing - {@link #close()} flushes but doesn't close the underlying writer
 * 	<li>Transparent delegation - all other operations pass through to the wrapped writer
 * 	<li>Useful for resource management - allows multiple consumers without premature closing
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Passing writers to APIs that close them, but you need to keep the writer open
 * 	<li>Multiple operations on the same writer where intermediate operations might close it
 * 	<li>Resource management scenarios where you control the writer lifecycle
 * 	<li>Wrapping system writers that should not be closed
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Wrap a writer that should not be closed</jc>
 * 	FileWriter <jv>fw</jv> = <jk>new</jk> FileWriter(<js>"output.txt"</js>);
 * 	NoCloseWriter <jv>wrapper</jv> = <jk>new</jk> NoCloseWriter(<jv>fw</jv>);
 *
 * 	<jc>// Pass to code that might close it</jc>
 * 	<jv>someMethod</jv>(<jv>wrapper</jv>);  <jc>// May call close(), but fw remains open</jc>
 *
 * 	<jc>// Continue using the original writer</jc>
 * 	<jv>fw</jv>.write(<js>"more data"</js>);
 * 	<jv>fw</jv>.close();  <jc>// Close when actually done</jc>
 * </p>
 *
 * <h5 class='section'>Important Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The {@link #close()} method flushes the writer but does not close the underlying writer
 * 	<li>You are responsible for closing the underlying writer when you're done with it
 * 	<li>This wrapper does not prevent resource leaks - ensure the underlying writer is eventually closed
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link NoCloseOutputStream} - OutputStream counterpart
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class NoCloseWriter extends Writer {

	private final Writer w;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new NoCloseWriter that wraps the specified Writer. The wrapper will prevent
	 * the underlying writer from being closed via the {@link #close()} method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FileWriter <jv>fw</jv> = <jk>new</jk> FileWriter(<js>"file.txt"</js>);
	 * 	NoCloseWriter <jv>wrapper</jv> = <jk>new</jk> NoCloseWriter(<jv>fw</jv>);
	 * </p>
	 *
	 * @param w The Writer to wrap. Must not be <jk>null</jk>.
	 */
	public NoCloseWriter(Writer w) {
		this.w = assertArgNotNull("w", w);
	}

	@Override /* Overridden from Writer */
	public Writer append(char c) throws IOException {
		return w.append(c);
	}

	@Override /* Overridden from Writer */
	public Writer append(CharSequence csq) throws IOException {
		return w.append(csq);
	}

	@Override /* Overridden from Writer */
	public Writer append(CharSequence csq, int start, int end) throws IOException {
		return w.append(csq, start, end);
	}

	/**
	 * Flushes the writer but does not close the underlying Writer.
	 *
	 * <p>
	 * This method flushes any buffered data to the underlying writer but does not close it.
	 * The underlying writer remains open and can continue to be used after this method is called.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FileWriter <jv>fw</jv> = <jk>new</jk> FileWriter(<js>"file.txt"</js>);
	 * 	NoCloseWriter <jv>wrapper</jv> = <jk>new</jk> NoCloseWriter(<jv>fw</jv>);
	 * 	<jv>wrapper</jv>.close();  <jc>// Flushes but doesn't close fw</jc>
	 * 	<jv>fw</jv>.write(<js>"still works"</js>);  <jc>// fw is still open</jc>
	 * </p>
	 *
	 * @throws IOException If an I/O error occurs while flushing.
	 */
	@Override /* Overridden from Writer */
	public void close() throws IOException {
		w.flush();
	}

	@Override /* Overridden from Writer */
	public void flush() throws IOException {
		w.flush();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return w.toString();
	}

	@Override /* Overridden from Writer */
	public void write(char[] cbuf) throws IOException {
		assertArgNotNull("cbuf", cbuf);
		w.write(cbuf);
	}

	@Override /* Overridden from Writer */
	public void write(char[] cbuf, int off, int len) throws IOException {
		assertArgNotNull("cbuf", cbuf);
		w.write(cbuf, off, len);
	}

	@Override /* Overridden from Writer */
	public void write(int c) throws IOException {
		w.write(c);
	}

	@Override /* Overridden from Writer */
	public void write(String str) throws IOException {
		assertArgNotNull("str", str);
		w.write(str);
	}

	@Override /* Overridden from Writer */
	public void write(String str, int off, int len) throws IOException {
		assertArgNotNull("str", str);
		w.write(str, off, len);
	}
}