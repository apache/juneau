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
package org.apache.juneau.rest.server.console;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.html5.*;
import org.junit.jupiter.api.*;

/**
 * Phase 4 gate: {@link TagHtmlRender}'s domain-resolution behavior.
 */
class TagHtmlRender_Test extends TestBase {

	private final TagHtmlRender render = new TagHtmlRender();

	//-----------------------------------------------------------------------------------------------------------------
	// @TagDomain default-domain trap
	//-----------------------------------------------------------------------------------------------------------------

	/** No {@code @TagDomain} on this type - deliberately named so an implementation using {@code getSimpleName()} would be tempted to (wrongly) resolve "priority". */
	enum Priority { LOW, MEDIUM, HIGH }

	@Test void a01_noTagDomainAnnotation_defaultsToStatus_notGetSimpleName() {
		var span = (Span) render.getContent(null, Priority.LOW);
		assertEquals("tag status low", span.getAttr(String.class, "class"),
			() -> "expected default domain 'status', not the enum's simple name 'priority'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @TagDomain explicit-domain gate
	//-----------------------------------------------------------------------------------------------------------------

	@TagDomain(domain="priority")
	enum ExplicitPriority { LOW, MEDIUM, HIGH }

	@Test void a02_explicitTagDomainAnnotation_resolves() {
		var span = (Span) render.getContent(null, ExplicitPriority.HIGH);
		assertEquals("tag priority high", span.getAttr(String.class, "class"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getDeclaringClass() regression (S2): anonymous-constant-body enum constants
	//-----------------------------------------------------------------------------------------------------------------

	@TagDomain(domain="x")
	enum WithAnonymousBody {
		A { @Override public String label() { return "a-label"; } },
		B;
		public String label() { return "plain"; }
	}

	/**
	 * {@code A}'s runtime class is an anonymous subclass of {@code WithAnonymousBody} (because it overrides
	 * {@code label()}). {@code getDeclaringClass()} always returns {@code WithAnonymousBody} itself, so both
	 * constants resolve identically.
	 *
	 * <p>
	 * <b>Correction to the plan's RED-proof premise (recorded here, not silently dropped):</b> the plan asserted
	 * this test would fail (RED) against a {@code value.getClass().getAnnotation(TagDomain.class)}-based
	 * implementation because {@code @Inherited} supposedly "does not extend across enum-constant-body
	 * subclassing". A standalone JDK 17 reflection probe run during development disproves this: {@code A}'s
	 * anonymous class's {@code getSuperclass()} <b>is</b> {@code WithAnonymousBody}, so {@code @Inherited}'s
	 * superclass walk finds the annotation there too &mdash; {@code getClass()} would have passed this exact test
	 * as well, given that {@link TagDomain} is {@code @Inherited}. This test therefore is NOT a RED/GREEN
	 * regression trap the way Phase 1/2's identifier guards are; it is a plain correctness assertion for
	 * {@link TagHtmlRender#domain(Enum)}'s actual (and still-preferred, see that method's javadoc) implementation
	 * choice.
	 */
	@Test void a03_anonymousConstantBody_stillResolvesTypeLevelAnnotation() {
		var spanA = (Span) render.getContent(null, WithAnonymousBody.A);
		var spanB = (Span) render.getContent(null, WithAnonymousBody.B);
		assertEquals("tag x a", spanA.getAttr(String.class, "class"));
		assertEquals("tag x b", spanB.getAttr(String.class, "class"));
	}

	@Test void a04_anonymousBodyConstant_runtimeClassDiffersFromDeclaringClass_sanityCheck() {
		// Sanity check that A really does have an anonymous runtime class distinct from the enum type - otherwise
		// (a03) would not actually be exercising the bug this gate guards against.
		assertNotEquals(WithAnonymousBody.class, WithAnonymousBody.A.getClass());
		assertEquals(WithAnonymousBody.class, WithAnonymousBody.A.getDeclaringClass());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Null-safety
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a05_nullValue_returnsNull_noNpe() {
		assertNull(render.getContent(null, null));
	}
}
