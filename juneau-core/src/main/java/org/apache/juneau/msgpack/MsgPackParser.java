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
package org.apache.juneau.msgpack;

import static org.apache.juneau.msgpack.DataType.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Parses a MessagePack stream into a POJO model.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Content-Type</code> types: <code>octal/msgpack</code>
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link MsgPackParserContext}
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes({"octal/msgpack"})
public final class MsgPackParser extends InputStreamParser {

	/** Default parser, all default settings.*/
	public static final MsgPackParser DEFAULT = new MsgPackParser().lock();

	/**
	 * Workhorse method.
	 */
	private <T> T parseAnything(MsgPackParserSession session, ClassMeta<T> nt, MsgPackInputStream is, Object outer) throws Exception {

		BeanContext bc = session.getBeanContext();
		if (nt == null)
			nt = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)nt.getPojoSwap();
		ClassMeta<?> ft = nt.getSerializedClassMeta();
		session.setCurrentClass(ft);

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
			else if (dt == ARRAY && ft.isObject()) {
				ObjectList ol = new ObjectList(bc);
				for (int i = 0; i < length; i++)
					ol.add(parseAnything(session, object(), is, outer));
				o = ol;
			} else if (dt == MAP && ft.isObject()) {
				ObjectMap om = new ObjectMap(bc);
				for (int i = 0; i < length; i++)
					om.put(parseAnything(session, string(), is, outer), parseAnything(session, object(), is, om));
				o = om.cast();
			}

			if (ft.isObject()) {
				// Do nothing.
			} else if (ft.isBoolean() || ft.isCharSequence() || ft.isChar() || ft.isNumber()) {
				o = bc.convertToType(o, ft);
			} else if (ft.isMap()) {
				if (dt == MAP) {
					Map m = (ft.canCreateNewInstance(outer) ? (Map)ft.newInstance(outer) : new ObjectMap(bc));
					for (int i = 0; i < length; i++) {
						Object key = parseAnything(session, ft.getKeyType(), is, outer);
						ClassMeta<?> vt = ft.getValueType();
						Object value = parseAnything(session, vt, is, m);
						setName(vt, value, key);
						m.put(key, value);
					}
					o = m;
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, ft);
				}
			} else if (ft.canCreateNewInstanceFromObjectMap(outer)) {
				ObjectMap m = new ObjectMap(bc);
				for (int i = 0; i < length; i++)
					m.put(parseAnything(session, string(), is, outer), parseAnything(session, object(), is, m));
				o = ft.newInstanceFromObjectMap(outer, m);
			} else if (ft.canCreateNewBean(outer)) {
				if (dt == MAP) {
					BeanMap m = bc.newBeanMap(outer, ft.getInnerClass());
					for (int i = 0; i < length; i++) {
						String pName = parseAnything(session, string(), is, m.getBean(false));
						BeanPropertyMeta bpm = m.getPropertyMeta(pName);
						if (bpm == null) {
							if (pName.equals("_class"))
								parseAnything(session, bc.string(), is, null);
							else
								onUnknownProperty(session, pName, m, 0, is.getPosition());
						} else {
							ClassMeta<?> cm = bpm.getClassMeta();
							Object value = parseAnything(session, cm, is, m.getBean(false));
							setName(cm, value, pName);
							bpm.set(m, value);
						}
					}
					o = m.getBean();
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, ft);
				}
			} else if (ft.canCreateNewInstanceFromString(outer) && dt == STRING) {
				o = ft.newInstanceFromString(outer, o == null ? "" : o.toString());
			} else if (ft.canCreateNewInstanceFromNumber(outer) && dt.isOneOf(INT, LONG, FLOAT, DOUBLE)) {
				o = ft.newInstanceFromNumber(outer, (Number)o);
			} else if (ft.isCollection()) {
				if (dt == MAP) {
					ObjectMap m = new ObjectMap(bc);
					for (int i = 0; i < length; i++)
						m.put(parseAnything(session, string(), is, outer), parseAnything(session, object(), is, m));
					o = m.cast();
				} else if (dt == ARRAY) {
					Collection l = (ft.canCreateNewInstance(outer) ? (Collection)ft.newInstance() : new ObjectList(bc));
					for (int i = 0; i < length; i++)
						l.add(parseAnything(session, ft.getElementType(), is, l));
					o = l;
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, ft);
				}
			} else if (ft.isArray()) {
				if (dt == MAP) {
					ObjectMap m = new ObjectMap(bc);
					for (int i = 0; i < length; i++)
						m.put(parseAnything(session, string(), is, outer), parseAnything(session, object(), is, m));
					o = m.cast();
				} else if (dt == ARRAY) {
					Collection l = (ft.isCollection() && ft.canCreateNewInstance(outer) ? (Collection)ft.newInstance() : new ObjectList(bc));
					for (int i = 0; i < length; i++)
						l.add(parseAnything(session, ft.getElementType(), is, l));
					o = bc.toArray(ft, l);
				} else {
					throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, ft);
				}
			} else if (dt == MAP) {
				ObjectMap m = new ObjectMap(bc);
				for (int i = 0; i < length; i++)
					m.put(parseAnything(session, string(), is, outer), parseAnything(session, object(), is, m));
				if (m.containsKey("_class"))
					o = m.cast();
				else
					throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", ft.getInnerClass().getName(), ft.getNotABeanReason());
			} else {
				throw new ParseException(session, "Invalid data type {0} encountered for parse type {1}", dt, ft);
			}
		}

		if (transform != null && o != null)
			o = transform.unswap(o, nt, bc);

		if (outer != null)
			setParent(nt, o, outer);

		return (T)o;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public MsgPackParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer) {
		return new MsgPackParserSession(getContext(MsgPackParserContext.class), getBeanContext(), input, op, javaMethod, outer);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		MsgPackParserSession s = (MsgPackParserSession)session;
		type = s.getBeanContext().normalizeClassMeta(type);
		MsgPackInputStream is = s.getInputStream();
		T o = parseAnything(s, type, is, s.getOuter());
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
