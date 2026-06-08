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
package org.apache.juneau.rest.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.rest.server.validation.*;
import org.junit.jupiter.api.*;

import jakarta.validation.constraints.*;

/**
 * Direct unit tests for {@link BeanValidator}, exercising the entrypoint contract independent of the
 * REST argument-resolver wiring covered elsewhere.
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"unused"     // REST op method parameters are required by the framework dispatch signature even when not referenced in the body.
})
class BeanValidator_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: isValidationRequested — recognizes @Valid (and equivalents) by FQN, ignores non-validation annotations.
	// -----------------------------------------------------------------------------------------------------------------

	static class A_Holder {
		void noMarker(String x) { /* intentionally empty */ }
		void jakartaValid(@jakarta.validation.Valid String x) { /* intentionally empty */ }
		void deprecated(@Deprecated String x) { /* intentionally empty */ }
	}

	@Test
	void a01_noMarker_returnsFalse() throws Exception {
		var pi = paramInfo("noMarker", 0);
		assertFalse(BeanValidator.isValidationRequested(pi));
	}

	@Test
	void a02_jakartaValid_returnsTrue() throws Exception {
		var pi = paramInfo("jakartaValid", 0);
		assertTrue(BeanValidator.isValidationRequested(pi));
	}

	@Test
	void a03_nonValidationAnnotation_returnsFalse() throws Exception {
		var pi = paramInfo("deprecated", 0);
		assertFalse(BeanValidator.isValidationRequested(pi));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: validate(null, ...) is a hard no-op — Jakarta's Validator.validate(null) throws IAE, and we don't want to
	// surface that as a 500 when the upstream arg resolver already handled the "missing body" case as a 400.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_nullBean_returnsNull() {
		// Null bean + null beanStore — even when the default validator can be resolved successfully, a null bean
		// short-circuits before any provider lookup.
		assertNull(BeanValidator.validate((Object)null, null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Happy / sad path through the default JVM-wide validator. This implicitly verifies that the test classpath
	// carries a provider (Hibernate Validator + Glassfish Expressly) — if either is missing the test will fail loudly
	// with a missing-provider degradation rather than a constraint check.
	// -----------------------------------------------------------------------------------------------------------------

	static class C_Bean {
		@NotNull
		String name;
	}

	@Test
	void c01_validate_validBean_returnsSame() {
		var b = new C_Bean();
		b.name = "alice";
		assertSame(b, BeanValidator.validate(b, null));
	}

	@Test
	void c02_validate_violatingBean_throwsValidationException() {
		var b = new C_Bean();  // name is null — violates @NotNull
		var ex = assertThrows(ValidationException.class, () -> BeanValidator.validate(b, null));
		assertEquals(1, ex.getViolations().size());
		assertEquals("name", ex.getViolations().get(0).getPath());
		assertEquals("NotNull", ex.getViolations().get(0).getConstraint());
		// invalidValue is omitted by default — guarded by ValidationViolation, not BeanValidator, but worth pinning
		// here to catch regressions where the dispatcher accidentally populates it from the ConstraintViolation.
		assertNull(ex.getViolations().get(0).getInvalidValue());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: ValidationException carries the immutable copy contract.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_getViolations_isUnmodifiable() {
		var ex = new ValidationException(List.of(new ValidationViolation("x", "msg", "NotNull")));
		assertThrows(UnsupportedOperationException.class, () -> ex.getViolations().add(null));
	}

	@Test
	void d02_copyViolations_isMutableAndIndependent() {
		var ex = new ValidationException(List.of(new ValidationViolation("x", "msg", "NotNull")));
		var copy = ex.copyViolations();
		copy.clear();
		assertEquals(1, ex.getViolations().size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------------------------------------------------

	private static ParameterInfo paramInfo(String methodName, int index) throws Exception {
		Method m = null;
		for (var mm : A_Holder.class.getDeclaredMethods()) {
			if (mm.getName().equals(methodName)) {
				m = mm;
				break;
			}
		}
		assertNotNull(m, "method " + methodName + " not found");
		return ParameterInfo.of(m.getParameters()[index]);
	}
}
