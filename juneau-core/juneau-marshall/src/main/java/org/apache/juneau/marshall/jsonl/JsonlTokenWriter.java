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
package org.apache.juneau.marshall.jsonl;

import java.io.*;
import java.math.*;
import java.nio.charset.*;

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenWriter} surface for the JSONL / NDJSON
 * format.
 *
 * <p>
 * Wraps a {@link JsonTokenWriter} and appends a newline after every top-level value emit.  The
 * "top-level value" boundary is detected by tracking when an emit returns the cursor to
 * {@code depth == 0}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 */
@SuppressWarnings({
	"resource" // The writer's underlying Writer/OutputStream is owned by the caller via try-with-resources on the writer itself; Eclipse JDT flags the inner stream as unclosed but that's by design.
})
public class JsonlTokenWriter implements TokenWriter {

	private final JsonTokenWriter delegate;
	private final Writer out;
	private final Closeable owned;
	private int depth;
	private boolean closed;

	/**
	 * Constructor.
	 *
	 * @param out The writer to emit JSONL to.  Must not be <jk>null</jk>.
	 * @param settings The output-formatting settings.  Must not be <jk>null</jk>.
	 */
	public JsonlTokenWriter(Writer out, JsonTokenWriter.Settings settings) {
		this(out, settings, null);
	}

	/**
	 * Internal constructor that records an underlying {@link Closeable} the writer should close on
	 * shutdown (e.g. a file stream opened by {@link #forOutput(Object, JsonTokenWriter.Settings)}).
	 *
	 * @param out The writer to emit JSONL to.  Must not be <jk>null</jk>.
	 * @param settings The output-formatting settings.  Must not be <jk>null</jk>.
	 * @param owned A resource owned by this writer that should be closed on close, or <jk>null</jk> (no extra
	 * 	resource is closed).
	 */
	JsonlTokenWriter(Writer out, JsonTokenWriter.Settings settings, Closeable owned) {
		this.delegate = new JsonTokenWriter(out, settings);
		this.out = out;
		this.owned = owned;
	}

	/**
	 * Internal factory that mirrors {@link JsonTokenWriter#forOutput(Object, JsonTokenWriter.Settings)}
	 * so the per-format {@code tokenWriter(...)} factory can hand in any of the supported output
	 * types ({@link Writer}, {@link OutputStream}).
	 *
	 * @param output The output object.
	 * @param settings The output-formatting settings.
	 * @return A new {@link JsonlTokenWriter} writing to the resolved underlying writer.
	 * @throws IOException If the output type is not supported or could not be opened.
	 */
	public static JsonlTokenWriter forOutput(Object output, JsonTokenWriter.Settings settings) throws IOException {
		if (output == null)
			throw new IOException("Output cannot be null.");
		if (output instanceof Writer w)
			return new JsonlTokenWriter(w, settings);
		if (output instanceof OutputStream os)
			return new JsonlTokenWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), settings);
		throw new IOException("Cannot convert object of type " + output.getClass().getName() + " to a Writer.");
	}

	@Override public TokenWriter startObject() throws IOException { assertOpen(); delegate.startObject(); depth++; return this; }
	@Override public TokenWriter endObject() throws IOException { assertOpen(); delegate.endObject(); return endTopLevel(); }
	@Override public TokenWriter startArray() throws IOException { assertOpen(); delegate.startArray(); depth++; return this; }
	@Override public TokenWriter endArray() throws IOException { assertOpen(); delegate.endArray(); return endTopLevel(); }
	@Override public TokenWriter fieldName(String name) throws IOException { assertOpen(); delegate.fieldName(name); return this; }
	@Override public TokenWriter string(String value) throws IOException { assertOpen(); delegate.string(value); return scalarTopLevel(); }
	@Override public TokenWriter number(Number value) throws IOException { assertOpen(); delegate.number(value); return scalarTopLevel(); }
	@Override public TokenWriter number(long value) throws IOException { assertOpen(); delegate.number(value); return scalarTopLevel(); }
	@Override public TokenWriter number(double value) throws IOException { assertOpen(); delegate.number(value); return scalarTopLevel(); }
	@Override public TokenWriter number(BigDecimal value) throws IOException { assertOpen(); delegate.number(value); return scalarTopLevel(); }
	@Override public TokenWriter number(BigInteger value) throws IOException { assertOpen(); delegate.number(value); return scalarTopLevel(); }
	@Override public TokenWriter bool(boolean value) throws IOException { assertOpen(); delegate.bool(value); return scalarTopLevel(); }
	@Override public TokenWriter nil() throws IOException { assertOpen(); delegate.nil(); return scalarTopLevel(); }
	@Override public TokenWriter binary(byte[] value) throws IOException { assertOpen(); delegate.binary(value); return scalarTopLevel(); }

	@Override
	public TokenWriter object(Object value) throws IOException {
		assertOpen();
		// Walk through THIS writer's structural methods so the depth tracking + trailing-newline
		// logic fires correctly.  PojoWalker honors the WalkOptions configured on the underlying
		// delegate's Settings.
		PojoWalker.walk(this, value, delegate.settingsAccess().walk());
		return this;
	}

	@Override public boolean isStreaming() { return true; }
	@Override public void flush() throws IOException { out.flush(); }

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		try {
			delegate.close();
		} finally {
			if (owned != null)
				owned.close();
		}
	}

	/**
	 * Throws an {@link IOException} if this writer has been {@link #close() closed}.
	 *
	 * @throws IOException If this writer is closed.
	 */
	private void assertOpen() throws IOException {
		if (closed)
			throw new IOException("Token writer is closed.");
	}

	private TokenWriter endTopLevel() throws IOException {
		depth--;
		if (depth == 0)
			out.write('\n');
		return this;
	}

	private TokenWriter scalarTopLevel() throws IOException {
		if (depth == 0)
			out.write('\n');
		return this;
	}
}
