// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.svl;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * A var resolver session that combines a {@link VarResolver} with one or more session objects.
 *
 * <p>
 * Instances of this class are considered light-weight and fast to construct, use, and discard.
 *
 * <p>
 * This class contains the workhorse code for var resolution.
 *
 * <p>
 * Instances of this class are created through the {@link VarResolver#createSession()} and
 * {@link VarResolver#createSession(BeanStore)} methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not guaranteed to be thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public class VarResolverSession {

	private final VarResolver context;
	private final BeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param context
	 * 	The {@link VarResolver} context object that contains the {@link Var Vars} and context objects associated with
	 * 	that resolver.
	 * @param beanStore The bean store to use for resolving beans needed by vars.
	 *
	 */
	public VarResolverSession(VarResolver context, BeanStore beanStore) {
		this.context = context;
		this.beanStore = BeanStore.of(beanStore);
	}

	/**
	 * Resolve all variables in the specified string.
	 *
	 * @param s
	 * 	The string to resolve variables in.
	 * @return
	 * 	The new string with all variables resolved, or the same string if no variables were found.
	 * 	<br>Returns <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public String resolve(String s) {

		if (s == null || s.isEmpty() || (s.indexOf('$') == -1 && s.indexOf('\\') == -1))
			return s;

		// Special case where value consists of a single variable with no embedded variables (e.g. "$X{...}").
		// This is a common case, so we want an optimized solution that doesn't involve string builders.
		if (isSimpleVar(s)) {
			String var = s.substring(1, s.indexOf('{'));
			String val = s.substring(s.indexOf('{')+1, s.length()-1);
			Var v = getVar(var);
			if (v != null) {
				try {
					if (v.streamed) {
						StringWriter sw = new StringWriter();
						v.resolveTo(this, sw, val);
						return sw.toString();
					}
					s = v.doResolve(this, val);
					if (s == null)
						s = "";
					return (v.allowRecurse() ? resolve(s) : s);
				} catch (VarResolverException e) {
					throw e;
				} catch (Exception e) {
					throw new VarResolverException(e, "Problem occurred resolving variable ''{0}'' in string ''{1}''", var, s);
				}
			}
			return s;
		}

		try {
			return resolveTo(s, new StringWriter()).toString();
		} catch (IOException e) {
			throw asRuntimeException(e); // Never happens.
		}
	}

	/**
	 * Convenience method for resolving variables in arbitrary objects.
	 *
	 * <p>
	 * Supports resolving variables in the following object types:
	 * <ul>
	 * 	<li>{@link CharSequence}
	 * 	<li>Arrays containing values of type {@link CharSequence}.
	 * 	<li>Collections containing values of type {@link CharSequence}.
	 * 		<br>Collection class must have a no-arg constructor.
	 * 	<li>Maps containing values of type {@link CharSequence}.
	 * 		<br>Map class must have a no-arg constructor.
	 * </ul>
	 *
	 * @param <T> The value type.
	 * @param o The object.
	 * @return The same object if no resolution was needed, otherwise a new object or data structure if resolution was
	 * needed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> T resolve(T o) {
		if (o == null)
			return null;
		if (o instanceof CharSequence)
			return (T)resolve(o.toString());
		if (o.getClass().isArray()) {
			if (! containsVars(o))
				return o;
			Object o2 = Array.newInstance(o.getClass().getComponentType(), Array.getLength(o));
			for (int i = 0; i < Array.getLength(o); i++)
				Array.set(o2, i, resolve(Array.get(o, i)));
			return (T)o2;
		}
		if (o instanceof Collection) {
			try {
				Collection c = (Collection)o;
				if (! containsVars(c))
					return o;
				Collection c2 = c.getClass().getDeclaredConstructor().newInstance();
				c.forEach(x -> c2.add(resolve(x)));
				return (T)c2;
			} catch (VarResolverException e) {
				throw e;
			} catch (Exception e) {
				throw new VarResolverException(e, "Problem occurred resolving collection.");
			}
		}
		if (o instanceof Map) {
			try {
				Map m = (Map)o;
				if (! containsVars(m))
					return o;
				Map m2 = m.getClass().getDeclaredConstructor().newInstance();
				m.forEach((k,v) -> m2.put(k, resolve(v)));
				return (T)m2;
			} catch (VarResolverException e) {
				throw e;
			} catch (Exception e) {
				throw new VarResolverException(e, "Problem occurred resolving map.");
			}
		}
		return o;
	}

	private static boolean containsVars(Object array) {
		for (int i = 0; i < Array.getLength(array); i++) {
			Object o = Array.get(array, i);
			if (o instanceof CharSequence && o.toString().contains("$"))
				return true;
		}
		return false;
	}

	private static boolean containsVars(Collection<?> c) {
		Flag f = Flag.create();
		c.forEach(x -> {
			if (x instanceof CharSequence && x.toString().contains("$"))
				f.set();
		});
		return f.isSet();
	}

	private static boolean containsVars(Map<?,?> m) {
		Flag f = Flag.create();
		m.forEach((k,v) -> {
			if (v instanceof CharSequence && v.toString().contains("$"))
				f.set();
		});
		return f.isSet();
	}

	/*
	 * Checks to see if string is of the simple form "$X{...}" with no embedded variables.
	 * This is a common case, and we can avoid using StringWriters.
	 */
	private static boolean isSimpleVar(String s) {
		int S1 = 1;	   // Not in variable, looking for $
		int S2 = 2;    // Found $, Looking for {
		int S3 = 3;    // Found {, Looking for }
		int S4 = 4;    // Found }

		int length = s.length();
		int state = S1;
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (state == S1) {
				if (c == '$') {
					state = S2;
				} else {
					return false;
				}
			} else if (state == S2) {
				if (c == '{') {
					state = S3;
				} else if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a')) {   // False trigger "$X "
					return false;
				}
			} else if (state == S3) {
				if (c == '}')
					state = S4;
				else if (c == '{' || c == '$')
					return false;
			} else if (state == S4) {
				return false;
			}
		}
		return state == S4;
	}

	/**
	 * Resolves variables in the specified string and sends the output to the specified writer.
	 *
	 * <p>
	 * More efficient than first parsing to a string and then serializing to the writer since this method doesn't need
	 * to construct a large string.
	 *
	 * @param s The string to resolve variables in.
	 * @param out The writer to write to.
	 * @return The same writer.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Writer resolveTo(String s, Writer out) throws IOException {

		int S1 = 1;	   // Not in variable, looking for $
		int S2 = 2;    // Found $, Looking for {
		int S3 = 3;    // Found {, Looking for }

		int state = S1;
		boolean isInEscape = false;
		boolean hasInternalVar = false;
		boolean hasInnerEscapes = false;
		String varType = null;
		String varVal = null;
		int x = 0, x2 = 0;
		int depth = 0;
		int length = s.length();
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (state == S1) {
				if (isInEscape) {
					if (c == '\\' || c == '$') {
						out.append(c);
					} else {
						out.append('\\').append(c);
					}
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
				} else if (c == '$') {
					x = i;
					x2 = i;
					state = S2;
				} else {
					out.append(c);
				}
			} else if (state == S2) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					hasInnerEscapes = true;
					isInEscape = true;
				} else if (c == '{') {
					varType = s.substring(x+1, i);
					x = i;
					state = S3;
				} else if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a')) {  // False trigger "$X "
					if (hasInnerEscapes)
						out.append(unEscapeChars(s.substring(x, i+1), AS1));
					else
						out.append(s, x, i+1);
					x = i + 1;
					state = S1;
					hasInnerEscapes = false;
				}
			} else if (state == S3) {
				if (isInEscape) {
					isInEscape = false;
				} else if (c == '\\') {
					isInEscape = true;
					hasInnerEscapes = true;
				} else if (c == '{') {
					depth++;
					hasInternalVar = true;
				} else if (c == '}') {
					if (depth > 0) {
						depth--;
					} else {
						varVal = s.substring(x+1, i);
						Var r = getVar(varType);
						if (r == null) {
							if (hasInnerEscapes)
								out.append(unEscapeChars(s.substring(x2, i+1), AS2));
							else
								out.append(s, x2, i+1);
							x = i+1;
						} else {
							varVal = (hasInternalVar && r.allowNested() ? resolve(varVal) : varVal);
							try {
								if (r.streamed)
									r.resolveTo(this, out, varVal);
								else {
									String replacement = r.doResolve(this, varVal);
									if (replacement == null)
										replacement = "";
									// If the replacement also contains variables, replace them now.
									if (replacement.indexOf('$') != -1 && r.allowRecurse())
										replacement = resolve(replacement);
									out.append(replacement);
								}
							} catch (VarResolverException e) {
								throw e;
							} catch (Exception e) {
								throw new VarResolverException(e, "Problem occurred resolving variable ''{0}'' in string ''{1}''", varType, s);
							}
							x = i+1;
						}
						state = 1;
						hasInnerEscapes = false;
					}
				}
			}
		}
		if (isInEscape)
			out.append('\\');
		else if (state == S2)
			out.append('$').append(unEscapeChars(s.substring(x+1), AS1));
		else if (state == S3)
			out.append('$').append(varType).append('{').append(unEscapeChars(s.substring(x+1), AS2));
		return out;
	}

	private static final AsciiSet
		AS1 = AsciiSet.create("\\{"),
		AS2 = AsciiSet.create("\\${}")
	;

	/**
	 * Returns the bean from the registered bean store.
	 *
	 * @param <T> The value type.
	 * @param c The bean type.
	 * @return
	 * 	The bean.
	 * 	<br>Never <jk>null</jk>.
	 */
	public <T> Optional<T> getBean(Class<T> c) {
		Optional<T> t = beanStore.getBean(c);
		if (! t.isPresent())
			t = context.beanStore.getBean(c);
		return t;
	}

	/**
	 * Returns the {@link Var} with the specified name.
	 *
	 * @param name The var name (e.g. <js>"S"</js>).
	 * @return The {@link Var} instance, or <jk>null</jk> if no <c>Var</c> is associated with the specified name.
	 */
	protected Var getVar(String name) {
		Var v = this.context.getVarMap().get(name);
		return v != null && v.canResolve(this) ? v : null;
	}

	/**
	 * Resolves the specified strings in the string array.
	 *
	 * @param in The string array containing variables to resolve.
	 * @return An array with resolved strings.
	 */
	public String[] resolve(String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = resolve(in[i]);
		return out;
	}

	/**
	 * Adds a bean to this session.
	 *
	 * @param <T> The bean type.
	 * @param c The bean type.
	 * @param value The bean.
	 * @return This object.
	 */
	public <T> VarResolverSession bean(Class<T> c, T value) {
		beanStore.addBean(c, value);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return "var=" + this.context.getVarMap().keySet() + ", context.beanStore=" + this.context.beanStore + ", session.beanStore=" + beanStore;
	}
}
