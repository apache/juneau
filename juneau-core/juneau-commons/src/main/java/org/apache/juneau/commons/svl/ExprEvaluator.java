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

import java.lang.reflect.*;
import java.util.*;

/**
 * Evaluates a parsed {@link Expr} tree to a typed value.
 *
 * <p>
 * Java 17 has no pattern-matching <c>switch</c>, so the "exhaustive switch" over the sealed {@link Expr} hierarchy is an
 * <c>instanceof</c> chain ending in an <c>AssertionError</c>.
 *
 * <h5 class='section'>Semantics:</h5>
 * <ul>
 * 	<li>Operators dispatch to <c>$op.*</c> functions via {@link VarResolverSession#findFunction(String)}; a
 * 		<jk>null</jk> result means "unknown or denied" ({@link #denied}).
 * 	<li>Laziness (<c>?:</c>, <c>? :</c>, <c>and</c>, <c>or</c>) is decided here; the coercion is then delegated to the
 * 		operator function with already-evaluated values.
 * 	<li>Property access: {@link Map} key, record accessor, public getter / <c>isX</c> / public field (looked up once per
 * 		class and cached).  <c>class</c>, <c>getClass</c>, <c>declaringClass</c> and <c>getDeclaringClass</c> are blocked
 * 		(case-insensitively), and so are values of reflective type (Class, ClassLoader, Module, Method/Field/Constructor,
 * 		Thread, Runtime, ProcessBuilder) at the point they are read; <c>java.*</c> (and friends) targets are refused except {@link Map}, {@link Collection},
 * 		{@link CharSequence}, {@link Number}, {@link Boolean}, {@link Optional} and arrays (user-defined enums use the normal getter path), which expose a few fixed
 * 		properties (<c>size</c>, <c>empty</c>, <c>length</c>, <c>present</c>) through hand-written code, never reflection.
 * 	<li>Values of reflective type read through a path, index or element property are refused (an {@link ExprException} in
 * 		both modes).  Values of that type that arrive as a bare scope name, as an iterated element, or as a projected result
 * 		are an {@link ExprException} in strict mode and treated as absent (<jk>null</jk>, or skipped by a select) in lenient mode.
 * 		A getter that throws an {@link Error} (for example {@link OutOfMemoryError}) is never wrapped; a
 * 		{@link StackOverflowError} is reported as "expression too complex".
 * 	<li>{@code maxCollectionSize} is one budget per expression evaluation, shared by all nested select/project
 * 		element scopes: strict mode throws when an element beyond it would be evaluated, lenient mode stops.
 * 	<li>Iterating a {@link Map} with <c>.?[</c> / <c>.![</c> yields one element per entry, as a map with <c>key</c> and
 * 		<c>value</c> properties.
 * </ul>
 *
 * <h5 class='section'>Errors:</h5>
 * <p>
 * Every {@link RuntimeException} raised while evaluating a node (including those thrown by functions, vars, getters
 * and {@link Map} implementations) is converted to an {@link ExprException} carrying the innermost node position, the
 * source and the cause.  A {@link StackOverflowError} from a pathologically deep tree becomes
 * "expression too complex".  The parser does not cap AST depth for left-associative chains or long paths; the stack
 * overflow guard here is what bounds them.
 *
 * <h5 class='section'>Fall-through (for the expression segment):</h5>
 * <p>
 * Where the template-level behaviour for an unavailable function or operator is "emit the verbatim source" (lenient
 * mode), the evaluator throws {@link FallThrough}.  The caller (Task 2.5) must catch it and emit the fallthrough text.
 * In strict mode an {@link ExprException} is thrown instead.
 */
@SuppressWarnings({
	"java:S135", // Loops use continue and break for lenient-mode skips
	"java:S1192" // Duplicated literals are diagnostic message fragments that read more clearly inline
})
final class ExprEvaluator {

	/** Marker meaning "no such property" (distinct from a present <jk>null</jk> value). */
	static final Object MISSING = new Object();

	/**
	 * Signal thrown in lenient mode when a function, operator or var is unavailable (unknown or denied by the
	 * allowlist).  Carries no stack trace.
	 */
	static final class FallThrough extends RuntimeException {
		private static final long serialVersionUID = 1L;

		FallThrough(String message) {
			super(message, null, false, false);
		}
	}

	private ExprEvaluator() {}

	/**
	 * Evaluates an expression.
	 *
	 * @param e The tree.
	 * @param ctx The evaluation context.
	 * @return The typed result; <jk>null</jk> means absent.
	 * @throws ExprException On evaluation failure (always) or on denied/unknown functions in strict mode.
	 * @throws FallThrough On denied/unknown functions in lenient mode.
	 */
	static Object eval(Expr e, ExprContext ctx) {
		try {
			return node(e, ctx);
		} catch (StackOverflowError x) {
			throw new ExprException("expression too complex", ctx.source, e.pos(), x);
		}
	}

	private static Object node(Expr e, ExprContext ctx) {
		try {
			if (e instanceof Expr.Literal x) return x.v();
			if (e instanceof Expr.Ident x) return scopeValue(ctx.lookup(x.name(), x.pos()), x.pos(), ctx);
			if (e instanceof Expr.VarRef x) return varRef(x, ctx);
			if (e instanceof Expr.Call x) return call(x, ctx);
			if (e instanceof Expr.Unary x) return unary(x, ctx);
			if (e instanceof Expr.Binary x) return binary(x, ctx);
			if (e instanceof Expr.Ternary x) return ternary(x, ctx);
			if (e instanceof Expr.Elvis x) return elvis(x, ctx);
			if (e instanceof Expr.Path x) return path(x, ctx);
			if (e instanceof Expr.Index x) return index(x, ctx);
			if (e instanceof Expr.Select x) return iterate(x.target(), x.pred(), true, x.pos(), ctx);
			if (e instanceof Expr.Project x) return iterate(x.target(), x.map(), false, x.pos(), ctx);
		} catch (ExprException | FallThrough x) {
			throw x;
		} catch (RuntimeException x) {
			throw new ExprException(describe(x), ctx.source, e.pos(), x);
		}
		throw new AssertionError("Unhandled node " + e);
	}

	/** A value of reflective type read from a scope: strict mode throws, lenient mode treats it as absent. */
	private static Object scopeValue(Object v, int pos, ExprContext ctx) {
		if (v == null || ! forbiddenType(v))
			return v;
		if (ctx.strict)
			throw err("value of type " + v.getClass().getName() + " is not allowed", pos, ctx);
		return null;
	}

	private static String describe(Throwable x) {
		return x.getMessage() == null ? x.getClass().getSimpleName() : x.getMessage();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Functions
	//-----------------------------------------------------------------------------------------------------------------

	private static ExprException err(String msg, int pos, ExprContext ctx) {
		return new ExprException(msg, ctx.source, pos);
	}

	/** Looks up an operator function; denied/unknown fails per mode. */
	private static VarFunction fn(String name, int pos, ExprContext ctx) {
		var f = ctx.session.findFunction(name);
		if (f == null) {
			var msg = ctx.session.isFunctionDenied(name) ? "operator '" + name + "' is not permitted" : "operator '" + name + "' is not available (no such function is registered)";
			if (ctx.strict)
				throw err(msg, pos, ctx);
			throw new FallThrough(msg);
		}
		return f;
	}

	private static Object invoke(VarFunction f, int pos, ExprContext ctx, Object...args) {
		try {
			return f.invokeValue(ctx.session, Arrays.asList(args));
		} catch (ExprException | FallThrough x) {
			throw x;   // a function that renders a nested expression may signal either; never wrap them
		} catch (Exception x) {
			throw new ExprException(describe(x), ctx.source, pos, x);
		}
	}

	/** Plain function call.  Call-form <c>and/or/not</c> are ordinary public functions: eager, public coercion table. */
	private static Object call(Expr.Call c, ExprContext ctx) {
		var n = c.fn();
		var f = ctx.session.findFunction(n);
		if (f == null) {
			var msg = ctx.session.isFunctionDenied(n) ? "function '" + n + "' is not permitted" : "unknown function '" + n + "'";
			if (ctx.strict)
				throw err(msg, c.pos(), ctx);
			throw new FallThrough(msg);
		}
		var count = c.args().size();
		if (count < f.minArity() || count > f.maxArity())
			throw err("function '" + n + "' expected " + f.minArity() + ".." + (f.maxArity() == Integer.MAX_VALUE ? "any" : f.maxArity()) + " argument(s), got " + count, c.pos(), ctx);
		var args = new Object[count];
		for (var i = 0; i < count; i++)
			args[i] = node(c.args().get(i), ctx);
		return invoke(f, c.pos(), ctx, args);
	}

	private static Object unary(Expr.Unary u, ExprContext ctx) {
		var name = unaryName(u.op());
		if (name == null)
			throw err("unknown unary operator '" + u.op() + "'", u.pos(), ctx);
		var f = fn("$op." + name, u.pos(), ctx);
		return invoke(f, u.pos(), ctx, node(u.e(), ctx));
	}

	/** Maps a unary operator token to its <c>$op.*</c> suffix, or <jk>null</jk>. */
	static String unaryName(String op) {
		return switch (op) {
			case "-" -> "neg";
			case "+" -> "pos";
			case "not" -> "not";
			default -> null;
		};
	}

	/** Maps a binary operator token to its <c>$op.*</c> suffix, or <jk>null</jk>. */
	static String binaryName(String op) {
		return switch (op) {
			case "and" -> "and";
			case "or" -> "or";
			case "+" -> "add";
			case "-" -> "sub";
			case "*" -> "mul";
			case "/" -> "div";
			case "%" -> "mod";
			case "==" -> "eq";
			case "!=" -> "ne";
			case "<" -> "lt";
			case "<=" -> "le";
			case ">" -> "gt";
			case ">=" -> "ge";
			case "matches" -> "matches";
			default -> null;
		};
	}

	private static Object binary(Expr.Binary b, ExprContext ctx) {
		var o = b.op();
		if ("and".equals(o) || "or".equals(o))
			return andOr("and".equals(o), b.l(), b.r(), b.pos(), ctx);
		var name = binaryName(o);
		if (name == null)
			throw err("unknown operator '" + o + "'", b.pos(), ctx);
		var f = fn("$op." + name, b.pos(), ctx);
		return invoke(f, b.pos(), ctx, node(b.l(), ctx), node(b.r(), ctx));
	}

	/** Short-circuit <c>and</c> / <c>or</c>; the operator function coerces each operand. */
	private static Object andOr(boolean and, Expr l, Expr r, int pos, ExprContext ctx) {
		var f = fn(and ? "$op.and" : "$op.or", pos, ctx);
		boolean lb = (Boolean)invoke(f, pos, ctx, node(l, ctx), and ? Boolean.TRUE : Boolean.FALSE);
		if (and != lb)
			return lb;   // false and ... / true or ...
		return invoke(f, pos, ctx, lb, node(r, ctx));
	}

	private static Object ternary(Expr.Ternary t, ExprContext ctx) {
		var f = fn("$op.ternary", t.pos(), ctx);
		boolean chosen = (Boolean)invoke(f, t.pos(), ctx, node(t.c(), ctx), Boolean.TRUE, Boolean.FALSE);
		return node(chosen ? t.a() : t.b(), ctx);
	}

	private static Object elvis(Expr.Elvis el, ExprContext ctx) {
		var f = fn("$op.elvis", el.pos(), ctx);
		var l = node(el.l(), ctx);
		if (l != null)
			return invoke(f, el.pos(), ctx, l, null);
		return invoke(f, el.pos(), ctx, null, node(el.r(), ctx));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Variable references
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Resolves a <c>${...}</c> reference.
	 *
	 * <p>
	 * The body is read by {@link VarTemplateCompiler#translate(java.util.function.Function, String)}, exactly as a
	 * top-level template reads it: a registered prefix is a lookup, otherwise the whole body is the empty-prefix
	 * <c>P</c> shortcut (so <c>${myprop:fallback}</c> is a property lookup with a default), and the colon split
	 * depends on the var class.  A body that cannot be represented (a <c>,</c> in a key) is an error in strict mode and
	 * a fall-through in lenient mode.
	 */
	private static Object varRef(Expr.VarRef v, ExprContext ctx) {
		var raw = v.raw();
		var call = VarTemplateCompiler.translate(ctx.session::getVar, raw.substring(2, raw.length() - 1));
		if (call.error() != null) {
			if (ctx.strict)
				throw new ExprException(call.error(), ctx.source, v.pos());
			throw new FallThrough(call.error());
		}
		if (! ctx.session.enterExprVar()) {
			var msg = "Variable nested too deeply (max " + ctx.maxExprDepth + ")";
			if (ctx.strict)
				throw new ExprException(msg, ctx.source, v.pos());
			throw new FallThrough(msg);
		}
		try {
			return ctx.session.resolveValue(call.prefix(), call.body());
		} catch (VarResolverException x) {
			throw new ExprException(describe(x), ctx.source, v.pos(), x);
		} finally {
			ctx.session.exitExprVar();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Property access
	//-----------------------------------------------------------------------------------------------------------------

	/** The property is blocked outright (reflective escape hatch, or a value of reflective type). */
	private static final class Blocked extends RuntimeException {
		private static final long serialVersionUID = 1L;
		Blocked(String message) { super(message, null, false, false); }
	}

	/** A getter threw. */
	private static final class GetterFailed extends RuntimeException {
		private static final long serialVersionUID = 1L;
		GetterFailed(String message, Throwable cause) { super(message, cause, false, false); }
	}

	/** Per-class property tables (public zero-arg methods by name, public fields, record component names). */
	private static final class Props {
		final Map<String,Method> methods = new HashMap<>();
		final Map<String,Field> fields = new HashMap<>();
		final Set<String> recordNames = new HashSet<>();
	}

	/** For a method declared by a non-public class, finds the same method on a public superclass or interface (or returns it unchanged). */
	private static Method publicVersion(Class<?> c, Method m) {
		if (Modifier.isPublic(m.getDeclaringClass().getModifiers()))
			return m;
		for (var t = c; t != null; t = t.getSuperclass()) {
			if (t != c && Modifier.isPublic(t.getModifiers()))
				try { return t.getMethod(m.getName()); } catch (NoSuchMethodException x) { /* try next */ }
			for (var i : t.getInterfaces()) {
				var r = publicInterfaceMethod(i, m.getName());
				if (r != null)
					return r;
			}
		}
		return m;
	}

	private static Method publicInterfaceMethod(Class<?> i, String name) {
		if (Modifier.isPublic(i.getModifiers()))
			try { return i.getMethod(name); } catch (NoSuchMethodException x) { /* try super-interfaces */ }
		for (var s : i.getInterfaces()) {
			var r = publicInterfaceMethod(s, name);
			if (r != null)
				return r;
		}
		return null;
	}

	// Note: LinkageError (e.g. NoClassDefFoundError) from getMethods()/getFields() deliberately propagates; it is an Error, not a property problem.
	private static final ClassValue<Props> PROPS = new ClassValue<>() {
		@Override protected Props computeValue(Class<?> c) {
			var p = new Props();
			for (var m : c.getMethods()) {
				var mn = m.getName();
				if (m.getParameterCount() == 0 && ! Modifier.isStatic(m.getModifiers()) && m.getReturnType() != void.class
						&& ! "getClass".equals(mn) && ! "getDeclaringClass".equals(mn))
					p.methods.putIfAbsent(mn, publicVersion(c, m));
			}
			for (var f : c.getFields())
				if (! Modifier.isStatic(f.getModifiers()))
					p.fields.putIfAbsent(f.getName(), f);
			if (c.isRecord())
				for (var rc : c.getRecordComponents())
					p.recordNames.add(rc.getName());
			return p;
		}
	};

	private static boolean blockedName(String name) {
		return "class".equalsIgnoreCase(name) || "getClass".equalsIgnoreCase(name) || "declaringClass".equalsIgnoreCase(name)
			|| "getDeclaringClass".equalsIgnoreCase(name);
	}

	private static boolean forbiddenType(Object v) {
		return v instanceof Class || v instanceof ClassLoader || v instanceof Module || v instanceof java.lang.reflect.AccessibleObject
			|| v instanceof Thread || v instanceof ThreadGroup || v instanceof Runtime || v instanceof ProcessBuilder || v instanceof Process;
	}

	private static boolean isSafeJava(Class<?> c) {
		return c.isArray() || Map.class.isAssignableFrom(c) || Collection.class.isAssignableFrom(c) || CharSequence.class.isAssignableFrom(c)
			|| Number.class.isAssignableFrom(c) || c == Boolean.class || c == Optional.class;
	}

	private static boolean isJdk(Class<?> c) {
		var n = c.getName();
		return n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("jdk.") || n.startsWith("sun.") || n.startsWith("com.sun.");
	}

	/**
	 * Property lookup for element scopes.
	 *
	 * @return The value, or {@link #MISSING} if the element genuinely has no such property (the caller then falls back to
	 * 	the outer scope).  A throwing getter yields <jk>null</jk> (lenient) or an {@link ExprException} (strict); it never
	 * 	falls back to the outer scope.
	 */
	static Object elementProp(Object o, String name, ExprContext ctx, int pos) {
		if (o == null)
			return MISSING;
		if (blockedName(name))
			throw err("access to '" + name + "' is not allowed", pos, ctx);
		var c = o.getClass();
		Object v;
		try {
			if (o instanceof Map<?,?> m && m.containsKey(name))
				v = m.get(name);
			else if (isSafeJava(c))
				v = safeProp(o, name);
			else if (isJdk(c))
				return MISSING;
			else
				v = beanProp(o, name);
			if (forbiddenType(v))
				throw new Blocked("value of type " + v.getClass().getName() + " is not allowed");
		} catch (Blocked x) {
			throw err(x.getMessage(), pos, ctx);
		} catch (GetterFailed x) {
			if (ctx.strict)
				throw new ExprException(x.getMessage(), ctx.source, pos, x.getCause());
			return null;
		}
		return v;
	}

	/** Reads a property of a non-JDK object (record accessor, getter, <c>isX</c>, or public field). */
	private static Object beanProp(Object o, String name) {
		var p = PROPS.get(o.getClass());
		Method m = null;
		if (p.recordNames.contains(name))
			m = p.methods.get(name);
		if (m == null) {
			var cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			m = p.methods.get("get" + cap);
			if (m == null) {
				var b = p.methods.get("is" + cap);
				if (b != null && (b.getReturnType() == boolean.class || b.getReturnType() == Boolean.class))
					m = b;
			}
		}
		try {
			Object v;
			if (m != null)
				v = m.invoke(o);
			else {
				var f = p.fields.get(name);
				if (f == null)
					return MISSING;
				v = f.get(o);
			}
			if (forbiddenType(v))
				throw new Blocked("value of type " + v.getClass().getName() + " is not allowed");
			return v;
		} catch (IllegalAccessException x) {
			// A public member of a non-public class with no public declaration to call through: present but unreachable
			// (we never call setAccessible).  This is NOT "absent", so element scopes must not fall back to the outer scope.
			throw new GetterFailed("property '" + name + "' is not accessible", null);
		} catch (InvocationTargetException x) {
			var cause = x.getCause();
			if (cause instanceof Error err)
				throw err;   // OutOfMemoryError, LinkageError, ...; a StackOverflowError is converted at the entry point
			throw new GetterFailed("property '" + name + "' threw " + cause, cause);
		}
	}

	private static Object safeProp(Object o, String name) {
		if (o instanceof Map<?,?> m) {
			if (m.containsKey(name)) return m.get(name);
			return switch (name) { case "size" -> m.size(); case "empty" -> m.isEmpty(); default -> MISSING; };
		}
		if (o instanceof Collection<?> c)
			return switch (name) { case "size" -> c.size(); case "empty" -> c.isEmpty(); default -> MISSING; };
		if (o instanceof CharSequence s)
			return switch (name) { case "length" -> s.length(); case "empty" -> s.isEmpty(); default -> MISSING; };
		if (o instanceof Optional<?> p)
			return switch (name) { case "present" -> p.isPresent(); case "empty" -> p.isEmpty(); default -> MISSING; };
		if (o.getClass().isArray())
			return "length".equals(name) ? (Object)Array.getLength(o) : MISSING;
		return MISSING;
	}

	private static Object path(Expr.Path p, ExprContext ctx) {
		var t = node(p.target(), ctx);
		if (t == null) {
			if (p.nullSafe() || ! ctx.strict)
				return null;
			throw err("cannot read property '" + p.name() + "' of null", p.pos(), ctx);
		}
		var name = p.name();
		if (blockedName(name))
			throw err("access to '" + name + "' is not allowed", p.pos(), ctx);
		var c = t.getClass();
		Object v;
		try {
			if (isSafeJava(c))
				v = safeProp(t, name);
			else if (isJdk(c))
				throw err("property access on type " + c.getName() + " is not allowed", p.pos(), ctx);
			else
				v = beanProp(t, name);
			if (forbiddenType(v))
				throw new Blocked("value of type " + v.getClass().getName() + " is not allowed");
		} catch (Blocked x) {
			throw err(x.getMessage(), p.pos(), ctx);
		} catch (GetterFailed x) {
			if (ctx.strict)
				throw new ExprException(x.getMessage(), ctx.source, p.pos(), x.getCause());
			return null;
		}
		if (v == MISSING) {
			if (ctx.strict)
				throw err("no property '" + name + "' on " + c.getSimpleName(), p.pos(), ctx);
			return null;
		}
		return v;
	}

	/** Map lookup; an integral numeric key also matches an equal-valued Long or Integer key. */
	private static Object mapGet(Map<?,?> m, Object k, boolean[] found) {
		if (m.containsKey(k)) { found[0] = true; return m.get(k); }
		if (k instanceof Number n && n.doubleValue() == n.longValue()) {
			var l = n.longValue();
			var alts = l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE ? new Object[]{ Long.valueOf(l), Integer.valueOf((int)l) } : new Object[]{ Long.valueOf(l) };
			for (var a : alts)
				if (! a.equals(k) && m.containsKey(a)) { found[0] = true; return m.get(a); }
		}
		return null;
	}

	private static Object index(Expr.Index x, ExprContext ctx) {
		var t = node(x.target(), ctx);
		if (t == null) {
			if (x.nullSafe() || ! ctx.strict)
				return null;
			throw err("cannot index null", x.pos(), ctx);
		}
		var k = node(x.idx(), ctx);
		if (t instanceof Map<?,?> m) {
			var found = new boolean[1];
			var v = mapGet(m, k, found);
			if (found[0]) {
				if (forbiddenType(v))
					throw err("value of type " + v.getClass().getName() + " is not allowed", x.pos(), ctx);
				return v;
			}
			if (ctx.strict)
				throw err("no key '" + k + "'", x.pos(), ctx);
			return null;
		}
		var isList = t instanceof List;
		if (isList || t.getClass().isArray()) {
			if (! (k instanceof Number n) || n.doubleValue() != n.longValue())
				throw err("index must be an integer but was " + k, x.pos(), ctx);
			var i = n.longValue();
			var size = isList ? ((List<?>)t).size() : Array.getLength(t);
			if (i < 0 || i >= size) {
				if (ctx.strict)
					throw err("index " + i + " out of range 0.." + (size - 1), x.pos(), ctx);
				return null;
			}
			var v = isList ? ((List<?>)t).get((int)i) : Array.get(t, (int)i);
			if (forbiddenType(v))
				throw err("value of type " + v.getClass().getName() + " is not allowed", x.pos(), ctx);
			return v;
		}
		if (t instanceof CharSequence cs) {
			if (! (k instanceof Number n) || n.doubleValue() != n.longValue())
				throw err("index must be an integer but was " + k, x.pos(), ctx);
			var i = n.longValue();
			var size = cs.length();
			if (i < 0 || i >= size) {
				if (ctx.strict)
					throw err("index " + i + " out of range 0.." + (size - 1), x.pos(), ctx);
				return null;
			}
			return String.valueOf(cs.charAt((int)i));
		}
		throw err("cannot index " + t.getClass().getSimpleName(), x.pos(), ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Select / project
	//-----------------------------------------------------------------------------------------------------------------

	private static Object iterate(Expr target, Expr body, boolean select, int pos, ExprContext ctx) {
		// Allowlist check first, before touching the target.
		fn(select ? "$op.select" : "$op.project", pos, ctx);
		var t = node(target, ctx);
		if (t == null) {
			if (! ctx.strict)
				return null;
			throw err("cannot iterate null", pos, ctx);
		}
		if (ctx.depth + 1 > ctx.maxExprDepth)
			throw err("expression nested too deeply (max " + ctx.maxExprDepth + ")", pos, ctx);
		Iterator<?> it;
		if (t instanceof Collection<?> c)
			it = c.iterator();
		else if (t.getClass().isArray()) {
			var n = Array.getLength(t);
			it = new Iterator<Object>() {
				int i;
				@Override public boolean hasNext() { return i < n; }
				@Override public Object next() {
					if (i >= n)
						throw new NoSuchElementException();
					return Array.get(t, i++);
				}
			};
		} else if (t instanceof Map<?,?> m) {
			var ei = m.entrySet().iterator();
			it = new Iterator<Object>() {
				@Override public boolean hasNext() { return ei.hasNext(); }
				@Override public Object next() {
					if (! ei.hasNext())
						throw new NoSuchElementException();
					var en = ei.next();
					var em = new LinkedHashMap<String,Object>();
					em.put("key", en.getKey());
					em.put("value", en.getValue());
					return em;
				}
			};
		} else
			throw err("cannot iterate " + t.getClass().getSimpleName(), pos, ctx);

		List<Object> out = new ArrayList<>();
		while (it.hasNext()) {
			if (ctx.budget[0] >= ctx.maxCollectionSize) {
				if (ctx.strict)
					throw err("collection iteration exceeds maxCollectionSize " + ctx.maxCollectionSize, pos, ctx);
				break;
			}
			ctx.budget[0]++;
			var el = it.next();
			if (forbiddenType(el)) {
				if (ctx.strict)
					throw err("value of type " + el.getClass().getName() + " is not allowed", pos, ctx);
				if (! select)
					out.add(null);
				continue;   // lenient: a reflective-type element is treated as absent
			}
			var r = scopeValue(node(body, ctx.element(el)), body.pos(), ctx);
			if (select) {
				if (truth(r, body.pos(), ctx))
					out.add(el);
			} else
				out.add(r);
		}
		return out;
	}

	/** Predicate coercion: Boolean or "true"/"false"; anything else is an error in strict mode and false in lenient mode. */
	private static boolean truth(Object r, int pos, ExprContext ctx) {
		if (r instanceof Boolean b) return b;
		if (r instanceof String s) {
			if ("true".equalsIgnoreCase(s)) return true;
			if ("false".equalsIgnoreCase(s)) return false;
		}
		if (ctx.strict)
			throw err("selection predicate must be boolean but was " + (r == null ? "null" : r.getClass().getSimpleName()), pos, ctx);
		return false;
	}
}
