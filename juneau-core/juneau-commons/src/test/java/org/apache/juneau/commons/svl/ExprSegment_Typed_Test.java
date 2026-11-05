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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests that {@code #{...}} expressions use a function's typed {@link VarFunction#invokeValue} result
 * (formerly exercised through the removed {@code ScriptSegment}).
 */
class ExprSegment_Typed_Test extends TestBase {

	/** Stub whose legacy invoke returns a String and whose invokeValue returns a Long sum. */
	public static class SumFn implements VarFunction {
		@Override public String name() { return "sum"; }
		@Override public int minArity() { return 2; }
		@Override public int maxArity() { return 2; }
		@Override public String invoke(VarResolverSession session, List<Object> args) {
			return "legacy:" + args.get(0) + "," + args.get(1);
		}
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) {
			return Long.parseLong(args.get(0).toString()) + Long.parseLong(args.get(1).toString());
		}
	}

	@Test void a01_typedResultFeedsOperators() {
		var vr = VarResolver.create().defaultFunctions().functions(new SumFn()).build();
		// The typed Long (3) flows into arithmetic; a String "legacy:1,2" could not.
		assertEquals("4", vr.resolve("#{sum(1,2) + 1}"));
	}

	@Test void a02_renderedResultIsTypedValueStringified() {
		var vr = VarResolver.create().functions(new SumFn()).build();
		assertEquals("3", vr.compile("#{sum(1,2)}").resolve(vr.createSession()));
	}
}
