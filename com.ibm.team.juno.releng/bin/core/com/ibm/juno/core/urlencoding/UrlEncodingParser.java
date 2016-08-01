/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2013, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import static com.ibm.juno.core.urlencoding.UonParserProperties.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.utils.*;

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
 * 	<li>{@link UonParserProperties}
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
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

	private <T> T parseAnything(ClassMeta<T> nt, UonParserContext ctx, ParserReader r, Object outer, Object name) throws ParseException {

		BeanContext bc = ctx.getBeanContext();
		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoFilter<T,Object> filter = (PojoFilter<T,Object>)nt.getPojoFilter();
		ClassMeta<?> ft = nt.getFilteredClassMeta();

		try {
			int c = r.peek();
			if (c == '?')
				r.read();

			Object o;

			if (ft.isObject()) {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(ctx, r, m, bc.string(), bc.object());
				o = m.cast();
			} else if (ft.isMap()) {
				Map m = (ft.canCreateNewInstance() ? (Map)ft.newInstance() : new ObjectMap(bc));
				o = parseIntoMap(ctx, r, m, ft.getKeyType(), ft.getValueType());
			} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
				ObjectMap m = new ObjectMap(bc);
				parseIntoMap(ctx, r, m, string(), object());
				o = ft.newInstanceFromObjectMap(outer, m);
			} else if (ft.canCreateNewBean(outer)) {
				BeanMap m = bc.newBeanMap(outer, ft.getInnerClass());
				m = parseIntoBeanMap(ctx, r, m);
				o = m == null ? null : m.getBean();
			} else {
				// It could be a non-bean with _class attribute.
				ObjectMap m = new ObjectMap(bc);
				ClassMeta<Object> valueType = object();
				parseIntoMap(ctx, r, m, string(), valueType);
				if (m.containsKey("_class"))
					o = m.cast();
				else if (m.containsKey("_value"))
					o = ctx.getBeanContext().convertToType(m.get("_value"), ft);
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
						throw new ParseException("Class ''{0}'' could not be instantiated as application/x-www-form-urlencoded.  Reason: ''{1}''", ft, ft.getNotABeanReason());
					throw new ParseException("Malformed application/x-www-form-urlencoded input for class ''{0}''.", ft);
				}
			}

			if (filter != null && o != null)
				o = filter.unfilter(o, nt);

			if (outer != null)
				setParent(nt, o, outer);

			if (name != null)
				setName(nt, o, name);

			return (T)o;

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ParseException("Error occurred trying to parse into class ''{0}''", ft).initCause(e);
		}
	}

	private <K,V> Map<K,V> parseIntoMap(UonParserContext ctx, ParserReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType) throws ParseException, IOException {

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
					Object attr = parseAttr(r, true, ctx);
					currAttr = ctx.getBeanContext().convertToType(attr, keyType);
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
						V value = convertAttrToType(m, "", valueType);
						m.put(currAttr, value);
						if (c == -1)
							return m;
						state = S1;
					} else  {
						// For performance, we bypass parseAnything for string values.
						V value = (V)(valueType.isString() ? super.parseString(r.unread(), true, ctx) : super.parseAnything(valueType, ctx, r.unread(), null, m, true, null));

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
			throw new ParseException("Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException("Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException("Dangling '=' found in object entry");
		if (state == S4)
			throw new ParseException("Could not find end of object.");

		return null; // Unreachable.
	}

	private <T> BeanMap<T> parseIntoBeanMap(UonParserContext ctx, ParserReader r, BeanMap<T> m) throws ParseException, IOException {
		int line = r.getLine();
		int column = r.getColumn();

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
					currAttr = parseAttrName(r, true);
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
									onUnknownProperty(ctx, currAttr, m, currAttrLine, currAttrCol);
								}
							} else {
								try {
									// In cases of "&foo=", create an empty instance of the value if createable.
									// Otherwise, leave it null.
									ClassMeta<?> cm = pMeta.getClassMeta();
									if (cm.canCreateNewInstance())
										pMeta.set(m, cm.newInstance());
								} catch (Exception e) {
									throw new ParseException(e);
								}
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
									m.put(currAttr, parseAnything(object(), ctx, r.unread(), null, m.getBean(false), true, currAttr));
								} else {
									onUnknownProperty(ctx, currAttr, m, currAttrLine, currAttrCol);
									parseAnything(object(), ctx, r.unread(), null, m.getBean(false), true, null); // Read content anyway to ignore it
								}
							} else {
								if (shouldUseExpandedParams(pMeta, ctx)) {
									ClassMeta cm = pMeta.getClassMeta();
									Object value = parseAnything(cm.getElementType(), ctx, r.unread(), pMeta, m.getBean(false), true, currAttr);
									pMeta.add(m, value);
								} else {
									Object value = parseAnything(pMeta.getClassMeta(), ctx, r.unread(), pMeta, m.getBean(false), true, currAttr);
									pMeta.set(m, value);
								}
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
			throw new ParseException(line, column, "Could not find attribute name on object.");
		if (state == S2)
			throw new ParseException(line, column, "Could not find '=' following attribute name on object.");
		if (state == S3)
			throw new ParseException(line, column, "Could not find value following '=' on object.");
		if (state == S4)
			throw new ParseException(line, column, "Could not find end of object.");

		return null; // Unreachable.
	}

	/**
	 * Returns true if the specified bean property should be expanded as multiple key-value pairs.
	 */
	private final boolean shouldUseExpandedParams(BeanPropertyMeta<?> pMeta, UonParserContext ctx) {
		ClassMeta cm = pMeta.getClassMeta();
		if (cm.isArray() || cm.isCollection()) {
			if (ctx.isExpandedParams())
				return true;
			if (pMeta.getBeanMeta().getClassMeta().getUrlEncodingMeta().isExpandedParams())
				return true;
		}
		return false;
	}

	/**
	 * Parse a URL query string into a simple map of key/value pairs.
	 *
	 * @param qs The query string to parse.
	 * @return A sorted {@link TreeMap} of query string entries.
	 * @throws IOException
	 */
	public Map<String,String[]> parseIntoSimpleMap(String qs) throws IOException {

		Map<String,String[]> m = new TreeMap<String,String[]>();

		if (StringUtils.isEmpty(qs))
			return m;

		UonParserReader r = new UonParserReader(qs, true);

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

	private Object[] parseArgs(UonParserContext ctx, ParserReader r, ClassMeta<?>[] argTypes) throws ParseException {
		// TODO - This can be made more efficient.
		BeanContext bc = ctx.getBeanContext();
		ClassMeta<TreeMap<Integer,String>> cm = bc.getMapClassMeta(TreeMap.class, Integer.class, String.class);
		TreeMap<Integer,String> m = parseAnything(cm, ctx, r, ctx.getOuter(), null);
		Object[] vals = m.values().toArray(new Object[m.size()]);
		if (vals.length != argTypes.length)
			throw new ParseException("Argument lengths don't match.  vals={0}, argTypes={1}", vals.length, argTypes.length);
		for (int i = 0; i < vals.length; i++) {
			String s = String.valueOf(vals[i]);
			vals[i] = super.parseAnything(argTypes[i], ctx, ctx.getUrlEncodingParserReader(new StringReader(s), s.length()), null, ctx.getOuter(), true, null);
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
		UonParserContext uctx = (UonParserContext)createContext();
		uctx.decodeChars = false;
		UonParserReader r = uctx.getUrlEncodingParserReader(wrapReader(in), in.length());
		return super.parseAnything(type, uctx, r, null, null, true, null);
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
	protected <T> T doParse(Reader in, int estimatedSize, ClassMeta<T> type, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		type = ctx.getBeanContext().normalizeClassMeta(type);
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		T o = parseAnything(type, uctx, r, ctx.getOuter(), null);
		return o;
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(Reader in, int estimatedSize, ClassMeta<?>[] argTypes, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		Object[] a = parseArgs(uctx, r, argTypes);
		return a;
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(Reader in, int estimatedSize, Map<K,V> m, Type keyType, Type valueType, ParserContext ctx) throws ParseException, IOException {
		UonParserContext uctx = (UonParserContext)ctx;
		UonParserReader r = uctx.getUrlEncodingParserReader(in, estimatedSize);
		if (r.peek() == '?')
			r.read();
		m = parseIntoMap(uctx, r, m, ctx.getBeanContext().getClassMeta(keyType), ctx.getBeanContext().getClassMeta(valueType));
		return m;
	}

	@Override /* Parser */
	public UrlEncodingParser setProperty(String property, Object value) throws LockedException {
		checkLock();
		if (! upp.setProperty(property, value))
			if (! uep.setProperty(property, value))
				super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser setProperties(ObjectMap properties) throws LockedException {
		for (Map.Entry<String,Object> e : properties.entrySet())
			setProperty(e.getKey(), e.getValue());
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public UrlEncodingParser addFilters(Class<?>...classes) throws LockedException {
		super.addFilters(classes);
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
		c.upp = upp.clone();
		c.uep = uep.clone();
		return c;
	}
}
