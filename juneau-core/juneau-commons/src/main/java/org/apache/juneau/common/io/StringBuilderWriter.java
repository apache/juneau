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
package org.apache.juneau.common.io;

import static org.apache.juneau.common.utils.AssertionUtils.*;

import java.io.*;

/**
 * A {@link Writer} implementation that writes to a {@link StringBuilder} instead of a {@link StringBuffer}.
 *
 * <p>
 * This class is similar to {@link StringWriter}, but uses a {@link StringBuilder} instead of a
 * {@link StringBuffer} to avoid synchronization overhead. This makes it more efficient for
 * single-threaded use cases where thread-safety is not required.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>No synchronization overhead - uses {@link StringBuilder} instead of {@link StringBuffer}
 * 	<li>Efficient string building - optimized for single-threaded string construction
 * 	<li>Configurable initial capacity - can specify initial buffer size
 * 	<li>Wraps existing StringBuilder - can wrap an existing StringBuilder instance
 * 	<li>No-op close/flush - close and flush operations do nothing
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Building strings efficiently in single-threaded contexts
 * 	<li>Capturing output from APIs that require a Writer
 * 	<li>Converting Writer-based APIs to StringBuilder output
 * 	<li>Performance-critical string building where synchronization is not needed
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	StringBuilderWriter <jv>writer</jv> = <jk>new</jk> StringBuilderWriter();
 * 	<jv>writer</jv>.write(<js>"Hello"</js>);
 * 	<jv>writer</jv>.write(<js>" World"</js>);
 * 	String <jv>result</jv> = <jv>writer</jv>.toString();  <jc>// Returns "Hello World"</jc>
 *
 * 	<jc>// With initial capacity</jc>
 * 	StringBuilderWriter <jv>writer2</jv> = <jk>new</jk> StringBuilderWriter(1000);
 *
 * 	<jc>// Wrap existing StringBuilder</jc>
 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder();
 * 	StringBuilderWriter <jv>writer3</jv> = <jk>new</jk> StringBuilderWriter(<jv>sb</jv>);
 * 	<jv>writer3</jv>.write(<js>"test"</js>);
 * 	<jc>// sb now contains "test"</jc>
 * </p>
 *
 * <h5 class='section'>Comparison with StringWriter:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>StringWriter:</b> Uses {@link StringBuffer} (thread-safe, synchronized)
 * 	<li><b>StringBuilderWriter:</b> Uses {@link StringBuilder} (not thread-safe, faster)
 * 	<li><b>StringWriter:</b> Suitable for multi-threaded scenarios
 * 	<li><b>StringBuilderWriter:</b> Suitable for single-threaded scenarios where performance matters
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. It uses a {@link StringBuilder} internally, which is not
 * synchronized. If multiple threads need to write to the same StringBuilderWriter instance,
 * external synchronization is required.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonIO">juneau-common-io</a>
 * </ul>
 */
public class StringBuilderWriter extends Writer {

	private StringBuilder sb;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new StringBuilderWriter with the default initial capacity (16 characters).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	StringBuilderWriter <jv>writer</jv> = <jk>new</jk> StringBuilderWriter();
	 * 	<jv>writer</jv>.write(<js>"Hello"</js>);
	 * </p>
	 */
	public StringBuilderWriter() {
		sb = new StringBuilder();
		lock = null;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new StringBuilderWriter with the specified initial capacity. This can improve
	 * performance if you know approximately how large the resulting string will be, avoiding
	 * multiple buffer reallocations.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Pre-allocate buffer for known size</jc>
	 * 	StringBuilderWriter <jv>writer</jv> = <jk>new</jk> StringBuilderWriter(1000);
	 * 	<jv>writer</jv>.write(<js>"Large content..."</js>);
	 * </p>
	 *
	 * @param initialSize The initial capacity of the internal StringBuilder in characters.
	 *                    Must be non-negative.
	 * @throws IllegalArgumentException If <tt>initialSize</tt> is negative.
	 */
	public StringBuilderWriter(int initialSize) {
		assertArg(initialSize >= 0, "Argument 'initialSize' cannot be negative.");
		sb = new StringBuilder(initialSize);
		lock = null;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Creates a new StringBuilderWriter that wraps an existing StringBuilder. All writes to
	 * this writer will be appended to the provided StringBuilder. This is useful when you
	 * want to write to a StringBuilder that you already have a reference to.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	StringBuilder <jv>sb</jv> = <jk>new</jk> StringBuilder(<js>"Prefix: "</js>);
	 * 	StringBuilderWriter <jv>writer</jv> = <jk>new</jk> StringBuilderWriter(<jv>sb</jv>);
	 * 	<jv>writer</jv>.write(<js>"Suffix"</js>);
	 * 	String <jv>result</jv> = <jv>sb</jv>.toString();  <jc>// Returns "Prefix: Suffix"</jc>
	 * </p>
	 *
	 * @param sb The StringBuilder to wrap. Must not be <jk>null</jk>.
	 */
	public StringBuilderWriter(StringBuilder sb) {
		this.sb = assertArgNotNull("sb", sb);
		lock = null;
	}

	@Override /* Overridden from Writer */
	public StringBuilderWriter append(char c) {
		write(c);
		return this;
	}

	@Override /* Overridden from Writer */
	public StringBuilderWriter append(CharSequence csq) {
		if (csq == null)
			write("null");
		else
			write(csq.toString());
		return this;
	}

	@Override /* Overridden from Writer */
	public StringBuilderWriter append(CharSequence csq, int start, int end) {
		CharSequence cs = (csq == null ? "null" : csq);
		write(cs.subSequence(start, end).toString());
		return this;
	}

	@Override /* Overridden from Writer */
	public void close() throws IOException {}

	@Override /* Overridden from Writer */
	public void flush() {}

	@Override /* Overridden from Object */
	public String toString() {
		return sb.toString();
	}

	@Override /* Overridden from Writer */
	public void write(char cbuf[], int start, int length) {
		assertArgNotNull("cbuf", cbuf);
		sb.append(cbuf, start, length);
	}

	@Override /* Overridden from Writer */
	public void write(int c) {
		sb.appendCodePoint(c);
	}

	@Override /* Overridden from Writer */
	public void write(String str) {
		assertArgNotNull("str", str);
		sb.append(str);
	}

	@Override /* Overridden from Writer */
	public void write(String str, int off, int len) {
		assertArgNotNull("str", str);
		sb.append(str.substring(off, off + len));
	}
}