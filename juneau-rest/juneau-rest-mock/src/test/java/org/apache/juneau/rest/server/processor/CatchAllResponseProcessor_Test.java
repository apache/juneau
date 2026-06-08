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
package org.apache.juneau.rest.server.processor;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Marker-conformance tests for {@link CatchAllResponseProcessor}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class CatchAllResponseProcessor_Test extends TestBase {

	/** {@link SerializedPojoProcessor} must implement {@link CatchAllResponseProcessor}. */
	@Test void a01_serializedPojoProcessor_isCatchAll() {
		// Instantiate via builder so BeanStore wiring is exercised.
		var chain = ResponseProcessorList.create(new BasicBeanStore(null))
			.add(SerializedPojoProcessor.class)
			.build()
			.toArray();
		assertEquals(1, chain.length);
		assertInstanceOf(CatchAllResponseProcessor.class, chain[0]);
	}

	/** {@link SerializedPojoProcessor} must also implement {@link ResponseProcessor}. */
	@Test void a02_serializedPojoProcessor_isResponseProcessor() {
		assertInstanceOf(ResponseProcessor.class, new SerializedPojoProcessor());
	}

	/** Default-chain scan: exactly one processor implements {@link CatchAllResponseProcessor}
	 * in the baseline default configuration (only {@link SerializedPojoProcessor}). */
	@Test void a03_defaultChain_exactlyOneCatchAll() {
		var chain = ResponseProcessorList.create(new BasicBeanStore(null))
			.add(
				AsyncResponseProcessor.class,
				ReaderProcessor.class,
				InputStreamProcessor.class,
				ThrowableProcessor.class,
				ProblemDetailsProcessor.class,
				HttpResponseProcessor.class,
				HttpResourceProcessor.class,
				HttpBodyProcessor.class,
				ResponseBeanProcessor.class,
				PlainTextPojoProcessor.class,
				SerializedPojoProcessor.class
			)
			.build()
			.toArray();

		var catchAllCount = 0;
		for (var p : chain)
			if (p instanceof CatchAllResponseProcessor)
				catchAllCount++;

		assertEquals(1, catchAllCount,
			"Default chain should have exactly 1 CatchAllResponseProcessor (SerializedPojoProcessor)");
	}

	/** {@link SerializedPojoProcessor} is the last entry in the default chain
	 * (all other processors are type-guarded; only SerializedPojoProcessor accepts any POJO). */
	@Test void a04_serializedPojoProcessor_isLastInDefaultChain() {
		var chain = ResponseProcessorList.create(new BasicBeanStore(null))
			.add(
				AsyncResponseProcessor.class,
				ReaderProcessor.class,
				InputStreamProcessor.class,
				ThrowableProcessor.class,
				ProblemDetailsProcessor.class,
				HttpResponseProcessor.class,
				HttpResourceProcessor.class,
				HttpBodyProcessor.class,
				ResponseBeanProcessor.class,
				PlainTextPojoProcessor.class,
				SerializedPojoProcessor.class
			)
			.build()
			.toArray();

		assertInstanceOf(SerializedPojoProcessor.class, chain[chain.length - 1],
			"SerializedPojoProcessor should be the last entry in the default chain");
	}
}
