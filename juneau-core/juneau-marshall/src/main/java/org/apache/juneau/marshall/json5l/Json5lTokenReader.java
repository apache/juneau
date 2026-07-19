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
package org.apache.juneau.marshall.json5l;

import java.io.*;

import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenReader} surface for the JSON5L format.
 *
 * <p>
 * Subclasses {@link Json5TokenReader} to inherit the JSON5 dialect relaxations (single-quoted and
 * bare-identifier strings/field names, trailing commas, missing values, comments) and adds JSONL's
 * flat top-level sequencing: each line's value is emitted in turn at {@link #getDepth() depth} 0
 * and {@link #next()} returns {@link TokenType#END_OF_STREAM} only when the input is exhausted.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>The cursor is a true O(1)-memory streaming cursor ({@link #isStreaming()} == <jk>true</jk>).
 * </ul>
 */
@SuppressWarnings({
	"resource" // The cursor's underlying ParserPipe is owned by the caller via try-with-resources on the cursor itself; Eclipse JDT flags the inner pipe as unclosed but that's by design.
})
public class Json5lTokenReader extends Json5TokenReader {

	/**
	 * Constructor with default settings.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public Json5lTokenReader(ParserPipe pipe) throws IOException {
		super(pipe);
	}

	/**
	 * Constructor used by {@link Json5lParserSession#readTokens(Object)} to plumb the calling
	 * session through so that {@link #read(Class)} can delegate to the JSON5L databind path.
	 *
	 * @param pipe The parser input pipe.  Must not be <jk>null</jk>.
	 * @param settings The cursor-level settings.  Must not be <jk>null</jk>.
	 * @param session The {@link Json5lParserSession} for {@link #read(Class)} delegation, or
	 * 	<jk>null</jk> to disable {@code read}.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public Json5lTokenReader(ParserPipe pipe, Settings settings, Json5lParserSession session) throws IOException {
		super(pipe, settings, session);
	}

	@Override /* JsonTokenReader */
	protected void afterValue() {
		// At depth 0, a top-level value just completed and we want the next next() / read() to
		// consume the next line's value (instead of transitioning to S05_end as plain JSON would).
		// Inside a container the parent's logic is still correct.
		if (depth == 0)
			state = S00_expectValue;
		else
			super.afterValue();
	}
}
