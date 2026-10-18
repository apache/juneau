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
package org.apache.juneau.rest.server.reactive;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.reactive.ReactiveResponseProcessor_Test.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ReactiveResponseProcessor} adapter discovery and dispatch using {@link ReactiveStreamsAdapter}
 * implementations registered through this module's test {@code META-INF/services} file.
 *
 * <p>
 * The registration file deliberately lists a class that does not exist between the two real adapters, so the
 * processor must skip the broken entry and still load the adapters around it.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // MockRestClient and its responses are in-memory test doubles that hold no OS resource; the shared client lives in a static field for the class lifetime.
})
class ReactiveResponseProcessor_Adapter_Test extends TestBase {

	/** Wrapper type recognised by {@link StreamAdapter}. */
	public static final class StreamBox {
		final Flow.Publisher<?> publisher;
		StreamBox(Flow.Publisher<?> publisher) { this.publisher = publisher; }
	}

	/** Wrapper type recognised by {@link SingleAdapter}. */
	public static final class SingleBox {
		final Object value;
		SingleBox(Object value) { this.value = value; }
	}

	/** Adapts {@link StreamBox} to a streaming {@link Adaptation}. */
	public static final class StreamAdapter implements ReactiveStreamsAdapter {
		@Override public boolean canAdapt(Object value) { return value instanceof StreamBox; }
		@Override public Adaptation adapt(Object value) { return Adaptation.stream(((StreamBox)value).publisher); }
	}

	/** Adapts {@link SingleBox} to a single-value {@link Adaptation}. */
	public static final class SingleAdapter implements ReactiveStreamsAdapter {
		@Override public boolean canAdapt(Object value) { return value instanceof SingleBox; }
		@Override public Adaptation adapt(Object value) { return Adaptation.single(CompletableFuture.completedFuture(((SingleBox)value).value)); }
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Registered adapters convert their types; everything else is left alone.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class A {
		@RestGet("/stream")
		public StreamBox stream(RestResponse res) {
			res.setContentType("application/x-ndjson");
			return new StreamBox(ListPublisher.of(new Pojo("a", 1), new Pojo("b", 2)));
		}

		@RestGet("/single")
		public SingleBox single() {
			return new SingleBox(new Pojo("only", 3));
		}

		@RestGet("/plain")
		public Pojo plain() {
			return new Pojo("plain", 4);
		}

		@RestGet("/nothing")
		public Object nothing() {
			return null;
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_adaptedStream_streamsFrames() throws Exception {
		var body = CA.get("/stream").run().assertStatus(200).getContent().asString();
		assertEquals("{\"name\":\"a\",\"value\":1}\n{\"name\":\"b\",\"value\":2}\n", body);
	}

	@Test void a02_adaptedSingle_serializedAsCompletedValue() throws Exception {
		CA.get("/single").accept("application/json").run().assertStatus(200)
			.assertContent().is("{\"name\":\"only\",\"value\":3}");
	}

	@Test void a03_unadaptedValue_passesThroughEveryAdapter() throws Exception {
		CA.get("/plain").accept("application/json").run().assertStatus(200)
			.assertContent().is("{\"name\":\"plain\",\"value\":4}");
	}

	@Test void a04_nullReturn_notClaimedByProcessor() throws Exception {
		CA.get("/nothing").accept("application/json").run().assertStatus(200).assertContent().is("null");
	}
}
