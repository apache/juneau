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
package org.apache.juneau.marshall.swaps;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Thin delegating swap for top-level {@code byte[]} values in textual serializers.
 *
 * <p>
 * Used as a fall-back swap by {@link DefaultSwaps} for root-level / map-value /
 * collection-element {@code byte[]} payloads that are not bean properties. Delegates to whichever
 * {@link BinaryFormat} is configured on the
 * {@link MarshallingContext}.
 *
 * <p>Disabled for:
 * <ul>
 * 	<li>Binary serializers / parsers with a native byte-array wire type —
 * 		gated by {@link OutputStreamSerializerSession#hasNativeBytes()} /
 * 		{@link InputStreamParserSession#hasNativeBytes()}.  Sessions that return {@code true}
 * 		(MsgPack / CBOR / BSON) emit / consume native byte sequences and skip this swap.  Sessions
 * 		that return {@code false} (Parquet, binary RDF) have no native byte-array primitive and
 * 		still run this swap to produce / consume the configured text wire form.
 * 	<li>CSV serializers / parsers ({@link CsvSerializerSession} / {@link CsvParserSession}) — CSV exposes its
 * 		own per-cell {@code byteArrayFormat} setting via {@link CsvByteArrayCellFormat}.
 * 	<li>{@code null}-session lookups (i.e. {@link ClassMeta}-level conversion plumbing) — keeps the
 * 		String-to-{@code byte[]} fallback in {@code BasicConverter} / {@code MarshallingContext.findConversion}
 * 		on the simple {@link String#getBytes(java.nio.charset.Charset)} path for non-binary inputs.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Swaps">Swap Basics</a>
 * </ul>
 */
public class BinarySwap extends StringSwap<byte[]> {

	@Override /* Overridden from ObjectSwap */
	public int match(MarshallingSession session) {
		// Null sessions arrive from ClassMeta-level conversion lookup paths.  Returning 0 keeps the
		// default String-to-byte[] conversion (UTF-8 getBytes) intact for callers that aren't actually
		// trying to encode/decode a wire-format binary payload.
		if (session == null)
			return 0;
		// Binary serializers / parsers with a native byte-array wire type handle byte[] natively — skip
		// this swap so the underlying BSON / CBOR / MsgPack writer emits the raw byte sequence and the
		// corresponding parser consumes it.  Binary serializers without a native byte-array wire type
		// (Parquet, binary RDF) report hasNativeBytes()==false and still run this swap to produce /
		// consume the configured text wire form (HEX / BASE64 / etc.) per OQ 10 (b).
		if (session instanceof OutputStreamSerializerSession oss && oss.hasNativeBytes())
			return 0;
		if (session instanceof InputStreamParserSession ips && ips.hasNativeBytes())
			return 0;
		// CSV defers byte[] cell formatting to its own CsvByteArrayCellFormat-driven cell serializer/parser.
		if (session instanceof CsvSerializerSession || session instanceof CsvParserSession)
			return 0;
		// OpenAPI part serializers/parsers drive byte[] cell encoding from the {@code format} schema attribute
		// (BYTE / BINARY / BINARY_SPACED).  Skipping this swap leaves the raw byte[] in place so the
		// OpenAPI session can apply the schema-directed encoding directly.
		if (session instanceof OpenApiSerializerSession || session instanceof OpenApiParserSession)
			return 0;
		// NOT_SET means "no wire format configured" — leave byte[] in its native language form so the
		// surrounding serializer falls back to its built-in array representation (e.g. JSON array).
		if (session.getMarshallingContext().getBinaryFormat() == BinaryFormat.NOT_SET)
			return 0;
		return super.match(session);
	}

	@Override /* Overridden from ObjectSwap */
	public String swap(MarshallingSession session, byte[] o) {
		return session.getMarshallingContext().getBinaryFormat().format(o);
	}

	@Override /* Overridden from ObjectSwap */
	public byte[] unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
		return session.getMarshallingContext().getBinaryFormat().parse(o);
	}
}
