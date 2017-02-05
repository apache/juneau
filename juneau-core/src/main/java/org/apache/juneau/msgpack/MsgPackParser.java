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
package org.apache.juneau.msgpack;

import static org.apache.juneau.msgpack.DataType.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.MediaType;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parses a MessagePack stream into a POJO model.
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>octal/msgpack</code>
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link MsgPackParserContext}
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes("octal/msgpack")
public final class MsgPackParser extends InputStreamParser {

	/** Default parser, all default settings.*/
	public static final MsgPackParser DEFAULT = new MsgPackParser().lock();

	/**
	 * Workhorse method.
	 */
	private <T> T parseAnything(MsgPackParserSession session, ClassMeta<T> eType, MsgPackInputStream is, Object outer, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();
		session.setCurrentClass(sType);
		BeanRegistry breg = (pMeta == null ? session.getBeanRegistry() : pMeta.getBeanRegistry());

		Object o = null;
		DataType dt = is.readDataType();
		int length = (int)is.readLength();

		if (dt != DataType.NULL) {
			if (dt == BOOLEAN)
				o = is.readBoolean();
			else if (dt == INT)
				o = is.readInt();
			else if (dt == LONG)
				o = is.readLong();
			else if (dt == FLOAT)
				o = is.readFloat();
			else if (dt == DOUBLE)
				o = is.readDouble();
			else if (dt == STRING)
				o = session.trim(is.readString());
			else if (dt == BIN)
				o = is.readBinary();
			else if (dt == ARRAY && sType.isObject()) {
				ObjectList ol = new ObjectList(session);
				for (int i = 0; i < length; i++)
					ol.add(parseAnything(session, object(), is, outer, pMeta));
				o = ol;
			} else if (dt == MAP && sType.isObject()) {
				ObjectMap om = new ObjectMap(session);
				for (int i = 0; i < length; i++)
					om.put(parseAnything(session, string(), is, outer, pMeta), parseAnything(session, object(), is, om, pMeta));
				o = breg.cast(om);
			}

			if (sType.isObject()) {
				// Do nothing.
			} else if (sType.isBoolean() || sType.isCharSequence() || sType.isChar() || sType.isNumber()) {
				o = session.convertToType(o, sType);
			} else if (sType.isMap()) {
				if (dt == MAP) {
					Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(session));
					for (int i = 0; i < length; i++) {
						Object key = parseAnything(session, sType.getKeyType(), is, outer, pMeta);
						ClassMeta<?> vt = sType.getValueType();
						Object value = parseAnything(session, vt, is, m, pMeta);
						setName(vt, value, key);
						m.put(key, value);
					}
					o = m;
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.canCreateNewInstanceFromObjectMap(outer)) {
				ObjectMap m = new ObjectMap(session);
				for (int i = 0; i < length; i++)
					m.put(parseAnything(session, string(), is, outer, pMeta), parseAnything(session, object(), is, m, pMeta));
				o = sType.newInstanceFromObjectMap(session, outer, m);
			} else if (sType.canCreateNewBean(outer)) {
				if (dt == MAP) {
					BeanMap m = session.newBeanMap(outer, sType.getInnerClass());
					for (int i = 0; i < length; i++) {
						String pName = parseAnything(session, string(), is, m.getBean(false), null);
						BeanPropertyMeta bpm = m.getPropertyMeta(pName);
						if (bpm == null) {
							if (pName.equals(session.getBeanTypePropertyName()))
								parseAnything(session, session.string(), is, null, null);
							else
								onUnknownProperty(session, pName, m, 0, is.getPosition());
						} else {
							ClassMeta<?> cm = bpm.getClassMeta();
							Object value = parseAnything(session, cm, is, m.getBean(false), bpm);
							setName(cm, value, pName);
							bpm.set(m, value);
						}
					}
					o = m.getBean();
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.canCreateNewInstanceFromString(outer) && dt == STRING) {
				o = sType.newInstanceFromString(outer, o == null ? "" : o.toString());
			} else if (sType.canCreateNewInstanceFromNumber(outer) && dt.isOneOf(INT, LONG, FLOAT, DOUBLE)) {
				o = sType.newInstanceFromNumber(session, outer, (Number)o);
			} else if (sType.isCollection()) {
				if (dt == MAP) {
					ObjectMap m = new ObjectMap(session);
					for (int i = 0; i < length; i++)
						m.put(parseAnything(session, string(), is, outer, pMeta), parseAnything(session, object(), is, m, pMeta));
					o = breg.cast(m);
				} else if (dt == ARRAY) {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : new ObjectList(session));
					for (int i = 0; i < length; i++)
						l.add(parseAnything(session, sType.getElementType(), is, l, pMeta));
					o = l;
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.isArray()) {
				if (dt == MAP) {
					ObjectMap m = new ObjectMap(session);
					for (int i = 0; i < length; i++)
						m.put(parseAnything(session, string(), is, outer, pMeta), parseAnything(session, object(), is, m, pMeta));
					o = breg.cast(m);
				} else if (dt == ARRAY) {
					Collection l = (sType.isCollection() && sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : new ObjectList(session));
					for (int i = 0; i < length; i++)
						l.add(parseAnything(session, sType.getElementType(), is, l, pMeta));
					o = session.toArray(sType, l);
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (dt == MAP) {
				ObjectMap m = new ObjectMap(session);
				for (int i = 0; i < length; i++)
					m.put(parseAnything(session, string(), is, outer, pMeta), parseAnything(session, object(), is, m, pMeta));
				if (m.containsKey(session.getBeanTypePropertyName()))
					o = breg.cast(m);
				else
					throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", sType.getInnerClass().getName(), sType.getNotABeanReason());
			} else {
				throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, sType);
			}
		}

		if (transform != null && o != null)
			o = transform.unswap(session, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public MsgPackParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new MsgPackParserSession(getContext(MsgPackParserContext.class), op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		MsgPackParserSession s = (MsgPackParserSession)session;
		type = s.normalizeClassMeta(type);
		MsgPackInputStream is = s.getInputStream();
		T o = parseAnything(s, type, is, s.getOuter(), null);
		return o;
	}

	@Override /* Parser */
	public MsgPackParser setProperty(String property, Object value) throws LockedException {
		super.setProperty(property, value);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackParser setProperties(ObjectMap properties) throws LockedException {
		super.setProperties(properties);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackParser addNotBeanClasses(Class<?>...classes) throws LockedException {
		super.addNotBeanClasses(classes);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackParser addBeanFilters(Class<?>...classes) throws LockedException {
		super.addBeanFilters(classes);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackParser addPojoSwaps(Class<?>...classes) throws LockedException {
		super.addPojoSwaps(classes);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackParser addToDictionary(Class<?>...classes) throws LockedException {
		super.addToDictionary(classes);
		return this;
	}

	@Override /* CoreApi */
	public <T> MsgPackParser addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		super.addImplClass(interfaceClass, implClass);
		return this;
	}

	@Override /* CoreApi */
	public MsgPackParser setClassLoader(ClassLoader classLoader) throws LockedException {
		super.setClassLoader(classLoader);
		return this;
	}

	@Override /* Lockable */
	public MsgPackParser lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public MsgPackParser clone() {
		try {
			return (MsgPackParser)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
