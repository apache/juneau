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

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.validation.*;
import jakarta.validation.constraints.*;

/**
 * Coverage for user-defined Jakarta Bean Validation constraint annotations.
 *
 * <p>
 * Verifies that Juneau's integration delegates entirely to the underlying {@code jakarta.validation}
 * engine for constraint discovery &mdash; meaning a user can ship their own {@code @Constraint}
 * annotation backed by a {@code ConstraintValidator} and have it picked up alongside the built-in
 * {@code jakarta.validation.constraints.*} family with no Juneau-specific glue.
 */
@SuppressWarnings({
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RestValidation_CustomValidator_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Custom @Sku constraint + validator. The validator just enforces a fixed regex; the point of the
	// test isn't the constraint itself but to prove user-defined annotations participate in the
	// Juneau-dispatched validation pass.
	// -----------------------------------------------------------------------------------------------------------------

	@Target({ ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@Constraint(validatedBy = SkuValidator.class)
	public @interface Sku {

		String message() default "must match SKU-NNNN format";

		Class<?>[] groups() default {};

		Class<? extends jakarta.validation.Payload>[] payload() default {};
	}

	public static class SkuValidator implements ConstraintValidator<Sku, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value != null && value.matches("SKU-\\d{4}");
		}
	}

	public static class Order {

		@Sku
		public String sku;

		@Min(1)
		public int qty;
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A {

		@RestPost("/order")
		public String create(@Valid @Content Order o) {
			return "ok:" + o.sku;
		}
	}

	@Test
	void a01_customConstraint_violation_isReported() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		// SKU regex requires SKU-NNNN; "BAD" fails the custom validator. Also verifies the constraint
		// surfaces by its simple-name (`Sku`) in the errors[] payload, matching how built-in constraints
		// like NotBlank are reported.
		a.post("/order", "{\"sku\":\"BAD\",\"qty\":1}")
			.contentType("application/json")
			.run()
			.assertStatus(400)
			.assertContent().isContains("\"status\":400", "\"errors\":[", "\"sku\"", "Sku");
	}

	@Test
	void a02_customConstraint_satisfied_reachesHandler() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.post("/order", "{\"sku\":\"SKU-1234\",\"qty\":1}")
			.contentType("application/json")
			.run()
			.assertStatus(200)
			.assertContent("\"ok:SKU-1234\"");
	}
}
