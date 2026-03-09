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
package org.apache.juneau.json;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.math.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.serializer.*;

/**
 * Session object for {@link JcsSerializer} that produces RFC 8785 canonical JSON.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 — JSON Canonicalization Scheme</a>
 * </ul>
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"java:S3776" // Cognitive complexity acceptable for ECMAScript number formatting and map serialization
})
public class JcsSerializerSession extends JsonSerializerSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializerSession.Builder {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JcsSerializer ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public JcsSerializerSession build() {
			return new JcsSerializerSession(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(JcsSerializer ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected JcsSerializerSession(Builder builder) {
		super(builder);
	}

	/**
	 * Converts a number to ECMAScript-compatible JSON string per RFC 8785.
	 *
	 * @param n The number.
	 * @return The serialized string.
	 * @throws SerializeException If the number is NaN or Infinity.
	 */
	public static String toEcmaNumber(Number n) {
		if (n instanceof Double d) {
			if (Double.isNaN(d))
				throw new SerializeException("NaN is not permitted in JCS (RFC 8785)");
			if (Double.isInfinite(d))
				throw new SerializeException("Infinity is not permitted in JCS (RFC 8785)");
			if (d == -0.0)
				return "0";
			return formatDouble(d);
		}
		if (n instanceof Float f) {
			if (Float.isNaN(f))
				throw new SerializeException("NaN is not permitted in JCS (RFC 8785)");
			if (Float.isInfinite(f))
				throw new SerializeException("Infinity is not permitted in JCS (RFC 8785)");
			if (f == -0.0f)
				return "0";
			return formatDouble(f.doubleValue());
		}
		if (n instanceof BigDecimal bd)
			return formatBigDecimal(bd);
		if (n instanceof BigInteger bi)
			return formatBigInteger(bi);
		if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
			var l = n.longValue();
			if (l == 0)
				return "0";
			return Long.toString(l);
		}
		return formatDouble(n.doubleValue());
	}

	private static final double ECMA_FIXED_LOW = 1e-6;
	private static final double ECMA_FIXED_HIGH = 1e21;

	private static String formatDouble(double d) {
		if (d == 0.0)
			return "0";
		var abs = Math.abs(d);
		// ECMAScript uses fixed notation for |n| in [1e-6, 1e21), scientific otherwise
		if (abs >= ECMA_FIXED_LOW && abs < ECMA_FIXED_HIGH) {
			var s = BigDecimal.valueOf(d).toPlainString();
			s = stripTrailingZeros(s);
			return s;
		}
		return formatDoubleScientific(d);
	}

	private static String stripTrailingZeros(String s) {
		if (s.contains(".")) {
			s = s.replaceAll("0+$", "");
			if (s.endsWith("."))
				s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String formatDoubleScientific(double d) {
		var s = Double.toString(d);
		var idxE = s.indexOf('E');
		if (idxE < 0)
			idxE = s.indexOf('e');
		if (idxE >= 0) {
			var mantissa = s.substring(0, idxE);
			var exp = s.substring(idxE + 1);
			var expNum = Integer.parseInt(exp);
			// ECMAScript: shorten mantissa (1.0 -> 1, 2.5 -> 2.5)
			mantissa = stripTrailingZeros(mantissa);
			if (mantissa.equals("1.0") || mantissa.equals("1"))
				mantissa = "1";
			else if (mantissa.endsWith(".0"))
				mantissa = mantissa.substring(0, mantissa.length() - 2);
			s = mantissa + 'e' + (expNum >= 0 ? "+" + expNum : String.valueOf(expNum));
		}
		return s;
	}

	private static String formatBigDecimal(BigDecimal bd) {
		try {
			var d = bd.doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d))
				throw new SerializeException("BigDecimal value exceeds IEEE 754 double range");
			return formatDouble(d);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new SerializeException("BigDecimal value exceeds IEEE 754 double range");
		}
	}

	private static String formatBigInteger(BigInteger bi) {
		try {
			var l = bi.longValueExact();
			return Long.toString(l);
		} catch (@SuppressWarnings("unused") ArithmeticException e) {
			throw new SerializeException("BigInteger value exceeds IEEE 754 safe integer range");
		}
	}

	/**
	 * JCS key comparison: UTF-16 code unit ordering per RFC 8785.
	 *
	 * <p>
	 * Null keys (e.g., from {@code HashMap} with null key) sort before non-null.
	 *
	 * @param a First string.
	 * @param b Second string.
	 * @return Comparison result (negative, zero, or positive).
	 */
	public static int jcsCompare(String a, String b) {
		if (a == null)
			return b == null ? 0 : -1;
		if (b == null)
			return 1;
		var len = Math.min(a.length(), b.length());
		for (var i = 0; i < len; i++) {
			var diff = Character.compare(a.charAt(i), b.charAt(i));
			if (diff != 0)
				return diff;
		}
		return Integer.compare(a.length(), b.length());
	}

	@Override /* Overridden from JsonSerializerSession */
	protected JsonWriter getJsonWriter(SerializerPipe out) {
		var output = out.getRawOutput();
		if (output instanceof JcsWriter w)
			return w;
		var w = new JcsWriter(out.getWriter(), isUseWhitespace(), getMaxIndent(), isEscapeSolidus(), getQuoteChar(), isSimpleAttrs(), isTrimStrings(), getUriResolver());
		out.setWriter(w);
		return w;
	}

	@SuppressWarnings({
		"rawtypes", // Raw types necessary for generic collection/map serialization
		"unchecked", // Type erasure requires unchecked casts in collection/map serialization
	})
	@Override /* Overridden from JsonSerializerSession - package visibility, override by making accessible */
	protected SerializerWriter serializeMap(JsonWriter out, Map m, ClassMeta<?> type) throws SerializeException {
		var keyType = type.getKeyType();
		var valueType = type.getValueType();

		var entries = new ArrayList<Map.Entry<?,?>>(m.entrySet());
		entries.sort((a, b) -> jcsCompare(toString(a.getKey()), toString(b.getKey())));

		var i = indent;
		out.w('{');

		var addComma = Flag.create();
		for (Map.Entry<?,?> x : entries) {
			Object value = x.getValue();
			Object key = generalize(x.getKey(), keyType);
			addComma.ifSet(() -> out.w(',').smi(i)).set();
			out.cr(i).attr(toString(key)).w(':').s(i);
			serializeAnything(out, value, valueType, (key == null ? null : toString(key)), null);
		}

		out.cre(i - 1).w('}');
		return out;
	}

	private static class BeanProp implements Map.Entry<String, Object> {
		final BeanPropertyMeta pMeta;
		final String key;
		final Object value;

		BeanProp(BeanPropertyMeta pMeta, String key, Object value) {
			this.pMeta = pMeta;
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() { return key; }

		@Override
		public Object getValue() { return value; }

		@Override
		public Object setValue(Object val) { throw new UnsupportedOperationException(); }
	}

	@Override /* Overridden from JsonSerializerSession */
	protected SerializerWriter serializeBeanMap(JsonWriter out, BeanMap<?> m, String typeName) throws SerializeException {
		var entries = new ArrayList<BeanProp>();
		Predicate<Object> checkNull = x -> isKeepNullProperties() || nn(x);

		m.forEachValue(checkNull, (pMeta, key, value, thrown) -> {
			var cMeta = pMeta.getClassMeta();
			if (nn(thrown))
				onBeanGetterException(pMeta, thrown);
			if (canIgnoreValue(cMeta, key, value))
				return;
			entries.add(new BeanProp(pMeta, key, value));
		});

		// Add _type property if present, then sort all by key using JCS UTF-16 comparison
		if (nn(typeName)) {
			var pm = m.getMeta().getTypeProperty();
			entries.add(new BeanProp(pm, pm.getName(), typeName));
		}
		entries.sort((a, b) -> jcsCompare(a.getKey(), b.getKey()));

		var i = indent;
		out.w('{');

		var addComma = Flag.create();
		for (var entry : entries) {
			addComma.ifSet(() -> out.append(',').smi(i)).set();
			out.cr(i).attr(entry.getKey()).w(':').s(i);
			serializeAnything(out, entry.getValue(), entry.pMeta.getClassMeta(), entry.getKey(), entry.pMeta);
		}

		out.cre(i - 1).w('}');
		return out;
	}
}
