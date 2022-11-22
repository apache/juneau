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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.msgpack.DataType.*;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link MsgPackParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.MsgPackDetails">MessagePack Details</a>
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class MsgPackParserSession extends InputStreamParserSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(MsgPackParser ctx) {
		return new Builder(ctx);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends InputStreamParserSession.Builder {

		MsgPackParser ctx;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(MsgPackParser ctx) {
			super(ctx);
			this.ctx = ctx;
		}

		@Override
		public MsgPackParserSession build() {
			return new MsgPackParserSession(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder javaMethod(Method value) {
			super.javaMethod(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder outer(Object value) {
			super.outer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schema(HttpPartSchema value) {
			super.schema(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.parser.ParserSession.Builder */
		public Builder schemaDefault(HttpPartSchema value) {
			super.schemaDefault(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MsgPackParserSession(Builder builder) {
		super(builder);
	}

	@Override /* ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (MsgPackInputStream is = new MsgPackInputStream(pipe)) {
			return parseAnything(type, is, getOuter(), null);
		}
	}

	/*
	 * Workhorse method.
	 */
	private <T> T parseAnything(ClassMeta<?> eType, MsgPackInputStream is, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		if (eType == null)
			eType = object();
		ObjectSwap<T,Object> swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		BuilderSwap<T,Object> builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (builder != null)
			sType = builder.getBuilderClassMeta(this);
		else if (swap != null)
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)optional(parseAnything(eType.getElementType(), is, outer, pMeta));

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
				JsonList jl = new JsonList(this);
				for (int i = 0; i < length; i++)
					jl.add(parseAnything(object(), is, outer, pMeta));
				o = jl;
			} else if (dt == MAP && sType.isObject()) {
				JsonMap jm = new JsonMap(this);
				for (int i = 0; i < length; i++)
					jm.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, jm, pMeta));
				o = cast(jm, pMeta, eType);
			}

			if (sType.isObject()) {
				// Do nothing.
			} else if (sType.isBoolean() || sType.isCharSequence() || sType.isChar() || sType.isNumber() || sType.isByteArray()) {
				o = convertToType(o, sType);
			} else if (sType.isMap()) {
				if (dt == MAP) {
					Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
					for (int i = 0; i < length; i++) {
						Object key = parseAnything(sType.getKeyType(), is, outer, pMeta);
						ClassMeta<?> vt = sType.getValueType();
						Object value = parseAnything(vt, is, m, pMeta);
						setName(vt, value, key);
						m.put(key, value);
					}
					o = m;
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
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
								onUnknownProperty(pName, m, parseAnything(string(), is, null, null));
						} else {
							ClassMeta<?> cm = bpm.getClassMeta();
							Object value = parseAnything(cm, is, m.getBean(false), bpm);
							setName(cm, value, pName);
							try {
								bpm.set(m, pName, value);
							} catch (BeanRuntimeException e) {
								onBeanSetterException(pMeta, e);
								throw e;
							}
						}
					}
					o = builder == null ? m.getBean() : builder.build(this, m.getBean(), eType);
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.canCreateNewInstanceFromString(outer) && dt == STRING) {
				o = sType.newInstanceFromString(outer, o == null ? "" : o.toString());
			} else if (sType.isCollection()) {
				if (dt == MAP) {
					JsonMap m = new JsonMap(this);
					for (int i = 0; i < length; i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (
						sType.canCreateNewInstance(outer)
						? (Collection)sType.newInstance()
						: new JsonList(this)
					);
					for (int i = 0; i < length; i++)
						l.add(parseAnything(sType.getElementType(), is, l, pMeta));
					o = l;
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.isArray() || sType.isArgs()) {
				if (dt == MAP) {
					JsonMap m = new JsonMap(this);
					for (int i = 0; i < length; i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (
						sType.isCollection() && sType.canCreateNewInstance(outer)
						? (Collection)sType.newInstance()
						: new JsonList(this)
					);
					for (int i = 0; i < length; i++)
						l.add(parseAnything(sType.isArgs() ? sType.getArg(i) : sType.getElementType(), is, l, pMeta));
					o = toArray(sType, l);
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (dt == MAP) {
				JsonMap m = new JsonMap(this);
				for (int i = 0; i < length; i++)
					m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
				if (m.containsKey(getBeanTypePropertyName(eType)))
					o = cast(m, pMeta, eType);
				else if (sType.getProxyInvocationHandler() != null)
					o = newBeanMap(outer, sType.getInnerClass()).load(m).getBean();
				else
					throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''",
						sType.getInnerClass().getName(), sType.getNotABeanReason());
			} else {
				throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
			}
		}

		if (swap != null && o != null)
			o = unswap(swap, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}
}
