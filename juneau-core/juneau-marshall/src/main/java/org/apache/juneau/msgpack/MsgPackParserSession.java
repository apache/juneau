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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackParser}.
 * 
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused against multiple inputs.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class MsgPackParserSession extends InputStreamParserSession {

	/**
	 * Create a new session using properties specified in the context.
	 * 
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime session arguments.
	 */
	protected MsgPackParserSession(MsgPackParser ctx, ParserSessionArgs args) {
		super(ctx, args);
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
		try (MsgPackInputStream is = new MsgPackInputStream(pipe)) {
			return parseAnything(type, is, getOuter(), null);
		}
	}

	/*
	 * Workhorse method.
	 */
	private <T> T parseAnything(ClassMeta<?> eType, MsgPackInputStream is, Object outer, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = object();
		PojoSwap<T,Object> swap = (PojoSwap<T,Object>)eType.getPojoSwap(this);
		BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;
		setCurrentClass(sType);

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
				o = trim(is.readString());
			else if (dt == BIN)
				o = is.readBinary();
			else if (dt == ARRAY && sType.isObject()) {
				ObjectList ol = new ObjectList(this);
				for (int i = 0; i < length; i++)
					ol.add(parseAnything(object(), is, outer, pMeta));
				o = ol;
			} else if (dt == MAP && sType.isObject()) {
				ObjectMap om = new ObjectMap(this);
				for (int i = 0; i < length; i++)
					om.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, om, pMeta));
				o = cast(om, pMeta, eType);
			}

			if (sType.isObject()) {
				// Do nothing.
			} else if (sType.isBoolean() || sType.isCharSequence() || sType.isChar() || sType.isNumber()) {
				o = convertToType(o, sType);
			} else if (sType.isMap()) {
				if (dt == MAP) {
					Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(this));
					for (int i = 0; i < length; i++) {
						Object key = parseAnything(sType.getKeyType(), is, outer, pMeta);
						ClassMeta<?> vt = sType.getValueType();
						Object value = parseAnything(vt, is, m, pMeta);
						setName(vt, value, key);
						m.put(key, value);
					}
					o = m;
				} else {
					throw new ParseException(loc(is), "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (builder != null || sType.canCreateNewBean(outer)) {
				if (dt == MAP) {
					BeanMap m = builder == null ? newBeanMap(outer, sType.getInnerClass()) : toBeanMap(builder.create(this, eType));
					for (int i = 0; i < length; i++) {
						String pName = parseAnything(string(), is, m.getBean(false), null);
						BeanPropertyMeta bpm = m.getPropertyMeta(pName);
						if (bpm == null) {
							if (pName.equals(getBeanTypePropertyName(eType)))
								parseAnything(string(), is, null, null);
							else
								onUnknownProperty(is.getPipe(), pName, m, 0, is.getPosition());
						} else {
							ClassMeta<?> cm = bpm.getClassMeta();
							Object value = parseAnything(cm, is, m.getBean(false), bpm);
							setName(cm, value, pName);
							bpm.set(m, pName, value);
						}
					}
					o = builder == null ? m.getBean() : builder.build(this, m.getBean(), eType);
				} else {
					throw new ParseException(loc(is), "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.canCreateNewInstanceFromString(outer) && dt == STRING) {
				o = sType.newInstanceFromString(outer, o == null ? "" : o.toString());
			} else if (sType.canCreateNewInstanceFromNumber(outer) && dt.isOneOf(INT, LONG, FLOAT, DOUBLE)) {
				o = sType.newInstanceFromNumber(this, outer, (Number)o);
			} else if (sType.isCollection()) {
				if (dt == MAP) {
					ObjectMap m = new ObjectMap(this);
					for (int i = 0; i < length; i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (
						sType.canCreateNewInstance(outer)
						? (Collection)sType.newInstance()
						: new ObjectList(this)
					);
					for (int i = 0; i < length; i++)
						l.add(parseAnything(sType.getElementType(), is, l, pMeta));
					o = l;
				} else {
					throw new ParseException(loc(is), "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.isArray() || sType.isArgs()) {
				if (dt == MAP) {
					ObjectMap m = new ObjectMap(this);
					for (int i = 0; i < length; i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (
						sType.isCollection() && sType.canCreateNewInstance(outer)
						? (Collection)sType.newInstance()
						: new ObjectList(this)
					);
					for (int i = 0; i < length; i++)
						l.add(parseAnything(sType.isArgs() ? sType.getArg(i) : sType.getElementType(), is, l, pMeta));
					o = toArray(sType, l);
				} else {
					throw new ParseException(loc(is), "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (dt == MAP) {
				ObjectMap m = new ObjectMap(this);
				for (int i = 0; i < length; i++)
					m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
				if (m.containsKey(getBeanTypePropertyName(eType)))
					o = cast(m, pMeta, eType);
				else
					throw new ParseException(loc(is), "Class ''{0}'' could not be instantiated.  Reason: ''{1}''",
						sType.getInnerClass().getName(), sType.getNotABeanReason());
			} else {
				throw new ParseException(loc(is), "Invalid data type {0} encountered for parse type {1}", dt, sType);
			}
		}

		if (swap != null && o != null)
			o = swap.unswap(this, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private ObjectMap loc(MsgPackInputStream is) {
		return getLastLocation().append("position", is.getPosition());
	}
}
