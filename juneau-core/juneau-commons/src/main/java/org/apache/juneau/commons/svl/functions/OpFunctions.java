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
package org.apache.juneau.commons.svl.functions;

import java.math.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.svl.*;

/**
 * Reserved {@code $op.*} operator functions targeted by the compiled expression grammar.
 *
 * <p>
 * Each operator is one {@link VarFunction} registered <i>only</i> under its reserved {@code $op.} name (never
 * under a plain name).  All are deniable via an allowlist like any other function.  Arguments arrive already
 * evaluated; short-circuiting ({@code and}, {@code or}, {@code elvis}, {@code ternary}) is the evaluator's job.
 *
 * <h2 class='section'>Coercion rules (SpEL-style):</h2>
 * <ul>
 * 	<li>Numeric tower: {@code Byte/Short/Integer/Long} stay {@code long} (overflow detected with {@code Math.*Exact}
 * 		and promoted to {@link BigDecimal}); any {@code Double/Float} operand gives {@code double} (this wins over
 * 		{@code BigDecimal}); otherwise any {@code BigDecimal}/{@code BigInteger} gives {@link BigDecimal}.
 * 	<li>{@code add} concatenates when either operand is a {@link String}; a {@code null} operand renders as
 * 		{@code "null"}.  {@code sub/mul/div/mod/neg/pos} are numeric only.
 * 	<li>{@code div} of two integral values stays integral only when exact, otherwise {@code double}.  Integral (and
 * 		BigDecimal) division or modulo by zero is an error; by {@code 0.0} follows IEEE 754.
 * 	<li>{@code eq/ne} compare numbers numerically ({@code 1 == 1.0}), otherwise {@link Objects#equals}.
 * 	<li>{@code lt/le/gt/ge} compare numbers numerically or same-class {@link Comparable}s; anything else is an
 * 		error.  A {@code null} operand is an error in strict mode and {@code false} in lenient mode.
 * 	<li>{@code and/or/not/ternary} accept {@link Boolean} or the strings {@code "true"}/{@code "false"}, matched
 * 		case-insensitively (consistent with {@code BooleanFunctions}, whose parser also uses case-insensitive
 * 		matching but additionally accepts 1/yes/on and 0/no/off, which operators deliberately do not).  {@code and}
 * 		and {@code or} validate <i>both</i> operands before combining.
 * 	<li>{@code Long.MIN_VALUE / -1} overflows and is promoted to {@link BigDecimal}; {@code Long.MIN_VALUE % -1} is {@code 0}.
 * 	<li>NaN is unordered: {@code eq} is false, {@code ne} true, and all relational operators false.  {@code 0.0 == -0.0}.
 * 		Long-versus-double comparison is exact (no precision loss).
 * 	<li>{@code add} string concatenation renders doubles via {@link ArithmeticFunctions#render(double)} (integral
 * 		values drop {@code .0}) and BigDecimal via {@code toPlainString()}.
 * 	<li>{@code elvis} is null-only: {@code false}, {@code 0} and {@code ""} are kept.
 * 	<li>{@code matches} uses a bounded shared pattern cache.  There is <b>no match timeout</b>: ReDoS exposure is
 * 		bounded only by the configured limits and the function allowlist.
 * </ul>
 */
@SuppressWarnings({
	"java:S3776" // Numeric comparison and arithmetic dispatch over every numeric kind in one place
})
public final class OpFunctions {

	private OpFunctions() {}

	/** All function classes in this category. */
	@SuppressWarnings({
		"java:S2386", // ALL is an immutable compile-time registry; exposed as an array for the cross-package/varargs functions(...) API.
		"unchecked" // Cast is safe: type verified by caller context.
	})
	public static final Class<? extends VarFunction>[] ALL = new Class[] {
		Add.class, Sub.class, Mul.class, Div.class, Mod.class, Eq.class, Ne.class, Lt.class, Le.class, Gt.class, Ge.class, And.class, Or.class, Not.class, Neg.class, Pos.class, Elvis.class, Ternary.class, Matches.class, Select.class, Project.class
	};

	/** Base class for operator functions. */
	abstract static class Op implements VarFunction {
		private final String name;
		private final int arity;

		Op(String name, int arity) {
			this.name = name;
			this.arity = arity;
		}

		@Override public String name() { return name; }
		@Override public int minArity() { return arity; }
		@Override public int maxArity() { return arity; }

		abstract Object apply(VarResolverSession s, Object a, Object b, Object c);

		@Override
		public Object invokeValue(VarResolverSession session, List<Object> args) {
			if (args.size() != arity)
				throw new VarResolverException("Operator '%s' requires %s argument(s) but got %s", name, arity, args.size());
			return apply(session, args.get(0), arity > 1 ? args.get(1) : null, arity > 2 ? args.get(2) : null);
		}

		@Override
		public String invoke(VarResolverSession session, List<Object> args) {
			var r = invokeValue(session, args);
			return r == null ? "" : r.toString();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static String typeOf(Object o) {
		return o == null ? "null" : o.getClass().getSimpleName();
	}

	private static boolean isLong(Object o) {
		return o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte;
	}

	private static boolean isDouble(Object o) {
		return o instanceof Double || o instanceof Float;
	}

	private static boolean isBig(Object o) {
		return o instanceof BigDecimal || o instanceof BigInteger;
	}

	private static boolean isNum(Object o) {
		return isLong(o) || isDouble(o) || isBig(o);
	}

	@SuppressWarnings({
		"java:S2111" // The exact binary expansion of the double is intended here
	})
	private static BigDecimal big(Object o) {
		if (o instanceof BigDecimal x) return x;
		if (o instanceof BigInteger x) return new BigDecimal(x);
		if (isDouble(o)) return new BigDecimal(((Number)o).doubleValue());  // exact binary expansion; callers exclude NaN/Infinity
		return BigDecimal.valueOf(((Number)o).longValue());
	}

	private static Number num(String op, Object o) {
		if (!isNum(o))
			throw new VarResolverException("Operator '%s' requires a numeric operand but got %s", op, typeOf(o));
		return (Number)o;
	}

	static Object add(Object a, Object b) {
		if (a instanceof String || b instanceof String)
			return str(a) + str(b);
		return arith("+", a, b);
	}

	static Object arith(String op, Object a, Object b) {
		var x = num(op, a);
		var y = num(op, b);
		if (isDouble(x) || isDouble(y))
			return dbl(op, x.doubleValue(), y.doubleValue());
		if (isLong(x) && isLong(y))
			return lng(op, x.longValue(), y.longValue());
		return bd(op, big(x), big(y));
	}

	private static Object dbl(String op, double x, double y) {
		return switch (op) {
			case "+" -> x + y;
			case "-" -> x - y;
			case "*" -> x * y;
			case "/" -> x / y;
			default -> x % y;
		};
	}

	private static VarResolverException zero(String op) {
		return new VarResolverException("Operator '%s' %s by zero", op, "/".equals(op) ? "division" : "modulo");
	}

	private static Object lng(String op, long x, long y) {
		try {
			switch (op) {
				case "+": return Math.addExact(x, y);
				case "-": return Math.subtractExact(x, y);
				case "*": return Math.multiplyExact(x, y);
				case "/":
					if (y == 0)
						throw zero(op);
					if (x == Long.MIN_VALUE && y == -1) throw new ArithmeticException();
					return x / y;
				default:
					if (y == 0)
						throw zero(op);
					return x % y;
			}
		} catch (ArithmeticException e) {
			return bd(op, BigDecimal.valueOf(x), BigDecimal.valueOf(y));
		}
	}

	private static Object bd(String op, BigDecimal x, BigDecimal y) {
		switch (op) {
			case "+": return (x.add(y));
			case "-": return (x.subtract(y));
			case "*": return (x.multiply(y));
			case "/":
				if (y.signum() == 0)
					throw zero(op);
				return (x.divide(y, MathContext.DECIMAL128));
			default:
				if (y.signum() == 0)
					throw zero(op);
				return (x.remainder(y));
		}
	}

	static Object neg(Object a) {
		var x = num("-", a);
		if (isDouble(x)) return -x.doubleValue();
		if (isLong(x)) {
			var l = x.longValue();
			return l == Long.MIN_VALUE ? BigDecimal.valueOf(l).negate() : (Object)(-l);
		}
		return big(x).negate();
	}

	/** Returned by {@link #numCmp} when either operand is NaN (no ordering exists). */
	private static final int UNORDERED = 2;

	/**
	 * Numeric comparison: -1, 0, 1, or {@link #UNORDERED}.
	 *
	 * <p>
	 * Two doubles compare with primitive semantics (so {@code 0.0 == -0.0}).  A double against an integral or
	 * BigDecimal value compares exactly through {@link BigDecimal} (no long-to-double precision loss);
	 * infinities are ordered by sign without conversion.
	 */
	private static int numCmp(Number x, Number y) {
		if (isDouble(x) && isDouble(y)) {
			var dx = x.doubleValue();
			var dy = y.doubleValue();
			if (Double.isNaN(dx) || Double.isNaN(dy)) return UNORDERED;
			if (dx < dy) return -1;
			return dx > dy ? 1 : 0;
		}
		if (isDouble(x) || isDouble(y)) {
			var d = (isDouble(x) ? x : y).doubleValue();
			if (Double.isNaN(d)) return UNORDERED;
			if (Double.isInfinite(d)) {
				var sign = d > 0 ? 1 : -1;
				return isDouble(x) ? sign : -sign;
			}
		}
		if (isLong(x) && isLong(y))
			return Long.compare(x.longValue(), y.longValue());
		return big(x).compareTo(big(y));
	}

	static boolean eq(Object a, Object b) {
		if (isNum(a) && isNum(b))
			return numCmp((Number)a, (Number)b) == 0;
		return Objects.equals(a, b);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static boolean rel(VarResolverSession s, String op, Object a, Object b) {
		if (a == null || b == null) {
			if (s != null && s.isStrict())
				throw new VarResolverException("Operator '%s' cannot compare null operand", op);
			return false;
		}
		int c;
		if (isNum(a) && isNum(b))
			c = numCmp((Number)a, (Number)b);
		else if (a.getClass() == b.getClass() && a instanceof Comparable x)
			c = x.compareTo(b);
		else
			throw new VarResolverException("Operator '%s' cannot compare %s with %s", op, typeOf(a), typeOf(b));
		if (c == UNORDERED)
			return false;
		return switch (op) {
			case "<" -> c < 0;
			case "<=" -> c <= 0;
			case ">" -> c > 0;
			default -> c >= 0;
		};
	}

	static boolean bool(String op, Object o) {
		if (o instanceof Boolean b) return b;
		if (o instanceof String s) {
			if ("true".equalsIgnoreCase(s)) return true;
			if ("false".equalsIgnoreCase(s)) return false;
		}
		throw new VarResolverException("Operator '%s' requires a boolean operand but got %s", op, typeOf(o));
	}

	/** Validates both operands (no short-circuit; that is the evaluator's job) then combines them. */
	static boolean both(String op, Object a, Object b, boolean and) {
		var x = bool(op, a);
		var y = bool(op, b);
		return and ? x && y : x || y;
	}

	/** Scalar string form used by {@code add} concatenation; shares {@link ArithmeticFunctions#render(double)}. */
	static String str(Object o) {
		if (isDouble(o)) return ArithmeticFunctions.render(((Number)o).doubleValue());
		if (o instanceof BigDecimal x) return x.toPlainString();
		return String.valueOf(o);
	}

	static boolean matches(Object s, Object regex) {
		if (!(s instanceof CharSequence cs))
			throw new VarResolverException("Operator 'matches' requires a string subject but got %s", typeOf(s));
		if (!(regex instanceof String r))
			throw new VarResolverException("Operator 'matches' requires a string regex but got %s", typeOf(regex));
		try {
			return RegexFunctions.PATTERN_CACHE.get(r).matcher(cs).matches();
		} catch (PatternSyntaxException e) {
			throw new VarResolverException(e, "Operator 'matches' has an invalid regex '%s': %s", r, e.getDescription());
		}
	}

	/** {@code $op.add}. */
	public static class Add extends Op {
		/** Constructor. */
		public Add() { super("$op.add", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return add(a, b); }
	}

	/** {@code $op.sub}. */
	public static class Sub extends Op {
		/** Constructor. */
		public Sub() { super("$op.sub", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return arith("-", a, b); }
	}

	/** {@code $op.mul}. */
	public static class Mul extends Op {
		/** Constructor. */
		public Mul() { super("$op.mul", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return arith("*", a, b); }
	}

	/** {@code $op.div}. */
	public static class Div extends Op {
		/** Constructor. */
		public Div() { super("$op.div", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return arith("/", a, b); }
	}

	/** {@code $op.mod}. */
	public static class Mod extends Op {
		/** Constructor. */
		public Mod() { super("$op.mod", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return arith("%", a, b); }
	}

	/** {@code $op.eq}. */
	public static class Eq extends Op {
		/** Constructor. */
		public Eq() { super("$op.eq", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return eq(a, b); }
	}

	/** {@code $op.ne}. */
	public static class Ne extends Op {
		/** Constructor. */
		public Ne() { super("$op.ne", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return !eq(a, b); }
	}

	/** {@code $op.lt}. */
	public static class Lt extends Op {
		/** Constructor. */
		public Lt() { super("$op.lt", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return rel(s, "<", a, b); }
	}

	/** {@code $op.le}. */
	public static class Le extends Op {
		/** Constructor. */
		public Le() { super("$op.le", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return rel(s, "<=", a, b); }
	}

	/** {@code $op.gt}. */
	public static class Gt extends Op {
		/** Constructor. */
		public Gt() { super("$op.gt", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return rel(s, ">", a, b); }
	}

	/** {@code $op.ge}. */
	public static class Ge extends Op {
		/** Constructor. */
		public Ge() { super("$op.ge", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return rel(s, ">=", a, b); }
	}

	/** {@code $op.and}. */
	public static class And extends Op {
		/** Constructor. */
		public And() { super("$op.and", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return both("and", a, b, true); }
	}

	/** {@code $op.or}. */
	public static class Or extends Op {
		/** Constructor. */
		public Or() { super("$op.or", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return both("or", a, b, false); }
	}

	/** {@code $op.not}. */
	public static class Not extends Op {
		/** Constructor. */
		public Not() { super("$op.not", 1); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return !bool("not", a); }
	}

	/** {@code $op.neg}. */
	public static class Neg extends Op {
		/** Constructor. */
		public Neg() { super("$op.neg", 1); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return neg(a); }
	}

	/** {@code $op.pos}. */
	public static class Pos extends Op {
		/** Constructor. */
		public Pos() { super("$op.pos", 1); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return num("+", a); }
	}

	/** {@code $op.elvis}. */
	public static class Elvis extends Op {
		/** Constructor. */
		public Elvis() { super("$op.elvis", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return a != null ? a : b; }
	}

	/** {@code $op.ternary}. */
	public static class Ternary extends Op {
		/** Constructor. */
		public Ternary() { super("$op.ternary", 3); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return bool("?:", a) ? b : c; }
	}

	/** {@code $op.matches}. */
	public static class Matches extends Op {
		/** Constructor. */
		public Matches() { super("$op.matches", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { return matches(a, b); }
	}

	/** Registered stub for the evaluator-only {@code $op.select} operator.
	 *
	 * <p>
	 * Exists so the name can be allowed or denied by an allowlist.  The evaluator must call
	 * {@code session.findFunction("$op.select")} and fail like a denied function before iterating; invoking it
	 * directly always throws {@link UnsupportedOperationException}. */
	public static class Select extends Op {
		/** Constructor. */
		public Select() { super("$op.select", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { throw new UnsupportedOperationException("evaluator-only"); }
	}

	/** Registered stub for the evaluator-only {@code $op.project} operator.
	 *
	 * <p>
	 * Exists so the name can be allowed or denied by an allowlist.  The evaluator must call
	 * {@code session.findFunction("$op.project")} and fail like a denied function before iterating; invoking it
	 * directly always throws {@link UnsupportedOperationException}. */
	public static class Project extends Op {
		/** Constructor. */
		public Project() { super("$op.project", 2); }
		@Override Object apply(VarResolverSession s, Object a, Object b, Object c) { throw new UnsupportedOperationException("evaluator-only"); }
	}
}
