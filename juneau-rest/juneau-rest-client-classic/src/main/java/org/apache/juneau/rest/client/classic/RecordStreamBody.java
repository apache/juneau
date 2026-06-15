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
package org.apache.juneau.rest.client.classic;

import java.util.function.*;

import org.apache.juneau.marshall.stream.*;

/**
 * Wrapper that lets a caller pass a {@code Consumer<RecordWriter>} or
 * {@code Consumer<TokenWriter>} as the body of a {@code @RemoteOp} request.
 *
 * <p>
 * The framework resolves the matching serializer for the request's {@code Content-Type},
 * opens a writer of the appropriate kind, and hands it to the caller's consumer.  When the
 * consumer returns, the writer is closed and the request body is dispatched.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<ja>@Remote</ja>
 * 	<jk>public interface</jk> MyApi {
 *
 * 		<ja>@RemotePost</ja>(<js>"/bulk-upload"</js>)
 * 		<jk>void</jk> upload(<ja>@Content</ja> RecordStreamBody <jv>body</jv>);
 * 	}
 *
 * 	<jc>// Caller</jc>
 * 	<jv>api</jv>.upload(RecordStreamBody.<jsm>record</jsm>(<jv>w</jv> -&gt; {
 * 		<jk>for</jk> (Bean <jv>b</jv> : <jv>source</jv>())
 * 			<jv>w</jv>.write(<jv>b</jv>);
 * 	}));
 * </p>
 */
public final class RecordStreamBody {

	/**
	 * Creates a body that streams via a {@link RecordWriter} (whole-value record cursor).
	 *
	 * @param consumer The callback that emits records.  Must not be <jk>null</jk>.
	 * @return A new body wrapper.
	 */
	public static RecordStreamBody record(Consumer<RecordWriter> consumer) {
		return new RecordStreamBody(consumer, RecordWriter.class);
	}

	/**
	 * Creates a body that streams via a {@link TokenWriter} (fine-grained structural cursor).
	 *
	 * @param consumer The callback that emits structural events.  Must not be <jk>null</jk>.
	 * @return A new body wrapper.
	 */
	public static RecordStreamBody token(Consumer<TokenWriter> consumer) {
		return new RecordStreamBody(consumer, TokenWriter.class);
	}

	private final Consumer<?> consumer;
	private final Class<?> writerKind;

	private RecordStreamBody(Consumer<?> consumer, Class<?> writerKind) {
		this.consumer = consumer;
		this.writerKind = writerKind;
	}

	/** @return The caller's consumer (typed as Consumer&lt;RecordWriter&gt; or Consumer&lt;TokenWriter&gt;). */
	public Consumer<?> getConsumer() { return consumer; }

	/** @return Either {@code RecordWriter.class} or {@code TokenWriter.class}. */
	public Class<?> getWriterKind() { return writerKind; }
}
