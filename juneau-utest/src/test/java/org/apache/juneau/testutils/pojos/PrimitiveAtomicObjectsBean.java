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
package org.apache.juneau.testutils.pojos;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Test bean fields of type AtomicInteger and AtomicLong.
 * Note that Jena parsers cannot handle these types, so we only test non-Jena parsers.
 */
@SuppressWarnings({})
public class PrimitiveAtomicObjectsBean {

	// primitive objects
	public AtomicInteger poAtomicInteger;
	public AtomicLong poAtomicLong;

	// uninitialized primitive objects
	public AtomicInteger pouAtomicInteger;
	public AtomicLong pouAtomicLong;

	// primitive object arrays
	public AtomicInteger[][] poaAtomicInteger;
	public AtomicLong[][] poaAtomicLong;

	// primitive object arrays
	public AtomicInteger[][] poauAtomicInteger;
	public AtomicLong[][] poauAtomicLong;

	// Anonymous list of primitives (types not erased on objects
	public List<AtomicInteger[]> poalAtomicInteger;
	public List<AtomicLong[]> poalAtomicLong;

	// Regular list of primitives (types erased on objects)
	public List<AtomicInteger[]> polAtomicInteger;
	public List<AtomicLong[]> polAtomicLong;

	private PrimitiveAtomicObjectsBean init() {
		// primitive objects
		poAtomicInteger = new AtomicInteger(1);
		poAtomicLong = new AtomicLong(2);

		// primitive object arrays
		poaAtomicInteger = a(a(new AtomicInteger(1)),a(new AtomicInteger(2)),null);
		poaAtomicLong = a(a(new AtomicLong(1)),a(new AtomicLong(2)),null);

		// Anonymous list of primitives
		poalAtomicInteger = l(a(new AtomicInteger(1)),null);
		poalAtomicLong = l(a(new AtomicLong(1)),null);

		// Regular list of primitives
		polAtomicInteger = new ArrayList<>();
		polAtomicInteger.add(a(new AtomicInteger(1)));
		polAtomicInteger.add(null);
		polAtomicLong = new ArrayList<>();
		polAtomicLong.add(a(new AtomicLong(1)));
		polAtomicLong.add(null);

		return this;
	}

	public static PrimitiveAtomicObjectsBean get() {
		return new PrimitiveAtomicObjectsBean().init();
	}
}