/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.urlencoding;

import static org.apache.juneau.urlencoding.UonParserContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parses URL-encoded text into POJO models.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Parses URL-Encoded text (e.g. <js>"foo=bar&baz=bing"</js>) into POJOs.
 * <p>
 * 	Expects parameter values to be in UON notation.
 * <p>
 * 	This parser uses a state machine, which makes it very fast and efficient.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link UonParserContext}
 * 	<li>{@link BeanContext}
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked", "hiding" })
@Consumes("application/x-www-form-urlencoded")
public class UrlEncodingParser extends UonParser {

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser().lock();

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT_WS_AWARE = new UrlEncodingParser().setProperty(UON_whitespaceAware, true).lock();

	/**
	 * Constructor.
	 */
	public UrlEncodingParser() {
		setProperty(UON_decodeChars, true);
	}

	private <T> T parseAnything(UrlEncodingParserSession session, ClassMeta<T> nt, ParserReader r, Object outer) throws Exception {

		BeanContext bc = session.getBeanContext();
		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)nt.getPojoSwap();
		ClassMeta<?> ft = nt.getTransformedClassMeta();

		int c = r.peek();
		if (c == '?')
			r.read();

		Object o;

		if (ft.isObject()) {
			ObjectMap m = new ObjectMap(bc);
			parseIntoMap(session, r, m, bc.string(), bc.object());
			o = m.cast();
		} else if (ft.isMap()) {
			Map m = (ft.canCreateNewInstance() ? (Map)ft.newInstance() : new ObjectMap(bc));
			o = parseIntoMap(session, r, m, ft.getKeyType(), ft.getValueType());
		} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
			ObjectMap m = new ObjectMap(bc);
			parseIntoMap(session, r, m, string(), object());
			o = ft.newInstanceFromObjectMap(outer, m);
		} else if (ft.canCreateNewBean(outer)) {
			BeanMap m = bc.newBeanMap(outer, ft.getInnerClass());
			m = parseIntoBeanMap(session, r, m);
			o = m == null ? null : m.getBean();
		} else {
			// It could be a non-bean with _class attribute.
			ObjectMap m = new ObjectMap(bc);
			ClassMeta<Object> valueType = object();
			parseIntoMap(session, r, m, string(), valueType);
			if (m.containsKey("_class"))
				o = m.cast();
			else if (m.containsKey("_value"))
				o = session.getBeanContext().convertToType(m.get("_value"), ft);
			else if (ft.isCollection()) {
				// ?1=foo&2=bar...
				Collection c2 = ft.canCreateNewInstance() ? (Collection)ft.newInstance() : new ObjectList(bc);
				Map<Integer,Object> t = new TreeMap<Integer,Object>();
				for (Map.Entry<String,Object> e : m.entrySet()) {
					String k = e.getKey();
					if (StringUtils.isNumeric(k))
						t.put(Integer.valueOf(k), bc.convertToType(e.getValue(), ft.getElementType()));
				}
				c2.addAll(t.values());
				o = c2;
			} else {
				if (ft.getNotABeanReason() != null)
					throw new ParseException(session, "Class ''{0}'' could not be instantiated as application/x-www-form-urlencoded.  Reason: ''{1}''", ft, ft.getNotABeanReason());
				throw new ParseException(session, "Malformed application/x-www-form-urlencoded input for class ''{0}''.", ft);
			}
		}

		if (transform != null && o != null)
			o = transform.unswap(o, nt);

		if (outer != null)
			setParent(nt, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(UonParserSession session, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType) throws Exception {

		if (keyType == null)
			keyType = (ClassMeta<K>)string();

		int c = r.peek();
		if (c == -1)
			return m;

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for & or end.
		boolean isInEscape = false;

		int state = S1;
		K currAttr = null;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == -1)
						return m;
					r.unread();
					Object attr = parseAttr(session, r, true);
					currAttr = session.trim(session.getBeanContext().convertToType(attr, keyType));
					state = S2;
					c = 0; // Avoid isInEscape if c was '\'
				} else if (state == S2) {
					if (c == '\u0002')
						state = S3;
					else if (c == -1 || c == '\u0001') {
						m.put(currAttr, null);
						if (c == -1)
							return m;
						state = S1;
					}
				} else if (state == S3) {
					if (c == -1 || c == '\u0001') {
						V value = convertAttrToType(session, m, "", valueType);
						m.put(currAttr, value);
						if (c == -1)
							return m;
						state = S1;
					} else  {
						// For performance, we bypass parseAnything for string values.
						V value = (V)(valueType.isString() ? super.parseString(session, r.unread(), true) : super.parseAnything(session, valueType, r.unread(), m, true));

						// If we already encountered this parameter, turn it into a list.
						if (m.containsKey(currAttr) && valueType.isObject()) {
							Object v2 = m.get(currAttr);
							if (! (v2 instanceof ObjectList)) {
								v2 = new ObjectList(v2);
								m.put(currAttr, (V)v2);
							}
							((ObjectList)v2).add(value);
						} else {
							m.put(currAttr, value);
						}
						state = S4;
						c = 0; // Avoid isInEscape if c was '\'
					}
				} else if (state == S4) {
					if (c == '\u0001')
						state = S1;
					else if (c == -1) {
						return m;
					}
				}
			}
			isInEscape = (c == '\\' && ! isInEscape);
		}
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(session, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(session, "Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException(session, "Could not find end of object.");

		return null; // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap(UrlEncodingParserSession session, ParserReader r, BeanMap<T> m) throws Exception {

		int c = r.peek();
		if (c == -1)
			return m;

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName end, looking for =.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Looking for , or }
		boolean isInEscape = false;

		int state = S1;
		String currAttr = "";
		int currAttrLine = -1, currAttrCol = -1;
		while (c != -1) {
			c = r.read();
			if (! isInEscape) {
				if (state == S1) {
					if (c == -1) {
						return m;
					}
					r.unread();
					currAttrLine= r.getLine();
					currAttrCol = r.getColumn();
					currAttr = parseAttrName(session, r, true);
					if (currAttr == null)  // Value was '%00'
						return null;
					state = S2;
				} else if (state == S2) {
					if (c == '\u0002')
						state = S3;
					else if (c == -1 || c == '\u0001') {
						m.put(currAttr, null);
						if (c == -1)
							return m;
						state = S1;
					}
				} else if (state == S3) {
					if (c == -1 || c == '\u0001') {
						if (! currAttr.equals("_class")) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								if (m.getMeta().isSubTyped()) {
									m.put(currAttr, "");
								} else {
									onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
								}
							} else {
								session.setCurrentProperty(pMeta);
								// In cases of "&foo=", create an empty instance of the value if createable.
								// Otherwise, leave it null.
								ClassMeta<?> cm = pMeta.getClassMeta();
								if (cm.canCreateNewInstance())
									pMeta.set(m, cm.newInstance());
								session.setCurrentProperty(null);
							}
						}
						if (c == -1)
							return m;
						state = S1;
					} else {
						if (! currAttr.equals("_class")) {
							BeanPropertyMeta pMeta = m.getPropertyMeta(currAttr);
							if (pMeta == null) {
								if (m.getMeta().isSubTyped()) {
									Object value = parseAnything(session, object(), r.unread(), m.getBean(false), true);
									m.put(currAttr, value);
								} else {
									onUnknownProperty(session, currAttr, m, currAttrLine, currAttrCol);
									parseAnything(session, object(), r.unread(), m.getBean(false), true); // Read content anyway to ignore it
								}
							} else {
								session.setCurrentProperty(pMeta);
								if (session.shouldUseExpandedParams(pMeta)) {
									ClassMeta et = pMeta.getClassMeta().getElementType();
									Object value = parseAnything(session, et, r.unread(), m.getBean(false), true);
									setName(et, value, currAttr);
									pMeta.add(m, value);
								} else {
									ClassMeta<?> cm = pMeta.getClassMeta();
									Object value = parseAnything(session, cm, r.unread(), m.getBean(false), true);
									setName(cm, value, currAttr);
									pMeta.set(m, value);
								}
								session.setCurrentProperty(null);
							}
						}
						state = S4;
					}
				} else if (state == S4) {
					if (c == '\u0001')
						state = S1;
					else if (c == -1) {
						return m;
					}
				}
			}
			isInEscape = (c == '\\' && ! isInEscape);
		}
		if (state == S1)
			throw new ParseException(session, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(session, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(session, "Could not find value following '=' on object.");
		if (state == S4)
			throw new ParseException(session, "Could not find end of object.");

		return null; // Unreachable.
	}

	/**
	 * Parse a URL query string into a simple map of key/value pairs.
	 *
	 * @param qs The query string to parse.
	 * @return A sorted {@link TreeMap} of query string entries.
	 * @throws Exception
	 */
	public Map<String,String[]> parseIntoSimpleMap(String qs) throws Exception {

		Map<String,String[]> m = new TreeMap<String,String[]>();

		if (StringUtils.isEmpty(qs))
			return m;

		UonReader r = new UonReader(qs, true);

		final int S1=1; // Looking for attrName start.
		final int S2=2; // Found attrName start, looking for = or & or end.
		final int S3=3; // Found =, looking for valStart.
		final int S4=4; // Found valStart, looking for & or end.

		try {
			int c = r.peek();
			if (c == '?')
				r.read();

			int state = S1;
			String currAttr = null;
			while (c != -1) {
				c = r.read();
				if (state == S1) {
					if (c != -1) {
						r.unread();
						r.mark();
						state = S2;
					}
				} else if (state == S2) {
					if (c == -1) {
						add(m, r.getMarked(), null);
					} else if (c == '\u0001') {
						m.put(r.getMarked(0,-1), null);
						state = S1;
					} else if (c == '\u0002') {
						currAttr = r.getMarked(0,-1);
						state = S3;
					}
				} else if (state == S3) {
					if (c == -1 || c == '\u0001') {
						add(m, currAttr, "");
					} else {
						if (c == '\u0002')
							r.replace('=');
						r.unread();
						r.mark();
						state = S4;
					}
				} else if (state == S4) {
					if (c == -1) {
						add(m, currAttr, r.getMarked());
					} else if (c == '\u0001') {
						add(m, currAttr, r.getMarked(0,-1));
						state = S1;
					} else if (c == '\u0002') {
						r.replace('=');
					}
				}
			}
		} finally {
			r.close();
		}

		return m;
	}

	private static void add(Map<String,String[]> m, String key, String val) {
		boolean b = m.containsKey(key);
		if (val == null) {
			if (! b)
				m.put(key, null);
		} else if (b && m.get(key) != null) {
			m.put(key, ArrayUtils.append(m.get(key), val));
		} else {
			m.put(key, new String[]{val});
		}
	}

	private Object[] parseArgs(UrlEncodingParserSession session, ParserReader r, ClassMeta<?>[] argTypes) throws Exception {
		// TODO - This can be made more efficient.
		BeanContext bc = session.getBeanContext();
		ClassMeta<TreeMap<Integer,String>> cm = bc.getMapClassMeta(TreeMap.class, Integer.class, String.class);
		TreeMap<Integer,String> m = parseAnything(session, cm, r, session.getOuter());
		Object[] vals = m.values().toArray(new Object[m.size()]);
		if (vals.length != argTypes.length)
			throw new ParseException(session, "Argument lengths don't match.  vals={0}, argTypes={1}", vals.length, argTypes.length);
		for (int i = 0; i < vals.length; i++) {
			String s = String.valueOf(vals[i]);
			vals[i] = super.parseAnything(session, argTypes[i], new UonReader(s, false), session.getOuter(), true);
		}

		return vals;
	}

	/**
	 * Parses a single query parameter value into the specified class type.
	 *
	 * @param in The input query string value.
	 * @param type The class type of the object to create.
	 * @return A new instance of the specified type.
	 * @throws ParseException
	 */
	public <T> T parseParameter(CharSequence in, ClassMeta<T> type) throws ParseException {
		if (in == null)
			return null;
		UonParserSession session = createParameterContext(in);
		try {
			UonReader r = session.getReader();
			return super.parseAnything(session, type, r, null, true);
		} catch (ParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException(session, e);
		} finally {
			session.close();
		}
	}

	/**
	 * Parses a single query parameter value into the specified class type.
	 *
	 * @param in The input query string value.
	 * @param type The class type of the object to create.
	 * @return A new instance of the specified type.
	 * @throws ParseException
	 */
	public <T> T parseParameter(CharSequence in, Class<T> type) throws ParseException {
		return parseParameter(in, getBeanContext().getClassMeta(type));
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public UrlEncodingParserSession createSession(Object input, ObjectMap properties, Method javaMethod, Object outer) {
		return new UrlEncodingParserSession(getContext(UrlEncodingParserContext.class), getBeanContext(), input, properties, javaMethod, outer);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		UrlEncodingParserSession s = (UrlEncodingParserSession)session;
		type = s.getBeanContext().normalizeClassMeta(type);
		UonReader r = s.getReader();
		T o = parseAnything(s, type, r, s.getOuter());
		return o;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<?>[] argTypes) throws Exception {
		UrlEncodingParserSession uctx = (UrlEncodingParserSession)session;
		UonReader r = uctx.getReader();
		Object[] a = parseArgs(uctx, r, argTypes);
		return a;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		UrlEncodingParserSession s = (UrlEncodingParserSession)session;
		UonReader r = s.getReader();
		if (r.peek() == '?')
			r.read();
		m = parseIntoMap(s, r, m, s.getBeanContext().getClassMeta(keyType), s.getBeanContext().getClassMeta(valueType));
		return m;
	}

	@Override /* Parser */
	public UrlEncodingParser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser addTransforms(Class<?>...classes) throws LockedException {
		super.addTransforms(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> UrlEncodingParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public UrlEncodingParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public UrlEncodingParser clone() {
		UrlEncodingParser c = (UrlEncodingParser)super.clone();
		return c;
	}
}
