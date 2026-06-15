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

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Reference implementation of the public {@link TokenReader} surface for the JSONL / NDJSON
 * format.
 *
 * <p>
 * JSONL is a sequence of independent JSON values separated by newlines.  There is no root
 * container.  This cursor models that as a flat top-level sequence at {@link #getDepth() depth} 0:
 * each line's tokens are emitted in turn, and {@link #next()} returns {@link TokenType#END_OF_STREAM}
 * only when the underlying input is exhausted.
 *
 * <p>
 * The most ergonomic way to consume a JSONL file is via {@link #read(Class)}, which binds one
 * line to a POJO per call:
 * <p class='bjava'>
 * 	<jk>try</jk> (TokenReader <jv>r</jv> = Jsonl.<jsf>DEFAULT</jsf>.parseTokens(<jv>file</jv>)) {
 * 		<jk>while</jk> (<jv>r</jv>.canRead()) {
 * 			MyRecord <jv>rec</jv> = <jv>r</jv>.read(MyRecord.<jk>class</jk>);
 * 			process(<jv>rec</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * 	<li>The cursor is a true O(1)-memory streaming cursor ({@link #isStreaming()} == <jk>true</jk>).
 * </ul>
 */
@SuppressWarnings({
	"resource" // The cursor's underlying ParserPipe is owned by the caller via try-with-resources on the cursor itself; Eclipse JDT flags the inner pipe as unclosed but that's by design.
})
public class JsonlTokenReader extends JsonTokenReader {

	/**
	 * Constructor with default settings.
	 *
	 * @param pipe The parser input pipe to read from.  Must not be <jk>null</jk>.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public JsonlTokenReader(ParserPipe pipe) throws IOException {
		super(pipe);
	}

	/**
	 * Constructor used by {@link JsonlParserSession#parseTokens(Object)} to plumb the calling
	 * session through.
	 *
	 * @param pipe The parser input pipe.  Must not be <jk>null</jk>.
	 * @param settings The cursor-level settings.  Must not be <jk>null</jk>.
	 * @param session The {@link JsonlParserSession} for {@link #read(Class)} delegation, or
	 * 	<jk>null</jk> to disable {@code read}.
	 * @throws IOException If a problem occurred opening the underlying reader.
	 */
	public JsonlTokenReader(ParserPipe pipe, Settings settings, JsonlParserSession session) throws IOException {
		super(pipe, settings, session);
	}

	@Override /* JsonTokenReader */
	protected void afterValue() {
		// At depth 0, a top-level value just completed and we want the next next() / read()
		// to consume the next line's value (instead of transitioning to S05_end as plain JSON
		// would).  Inside a container the parent's logic (S03_expectCommaOrEndObject /
		// S04_expectCommaOrEndArray) is still correct.
		if (depth == 0)
			state = S00_expectValue;
		else
			super.afterValue();
	}
}
