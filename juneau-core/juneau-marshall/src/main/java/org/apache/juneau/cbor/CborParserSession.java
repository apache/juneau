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
package org.apache.juneau.cbor;

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.cbor.DataType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.commons.bean.BeanMap;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Session object that lives for the duration of a single use of {@link CborParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"rawtypes", // Raw types necessary for generic type handling
	"unchecked", // Type erasure requires unchecked casts
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class CborParserSession extends InputStreamParserSession {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParserSession.Builder<Builder> {

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(CborParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
		}

		@Override
		public CborParserSession build() {
			return new CborParserSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(CborParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CborParserSession(Builder builder) {
		super(builder);
	}

	/*
	 * Workhorse method.
	 */
	@SuppressWarnings({
		"resource",   // is is caller-owned; this method does not close it
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541"  // Single-threaded session contexts do not require synchronization
	})
	private <T> T parseAnything(ClassMeta<?> eType, CborInputStream is, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

		if (eType == null)
			eType = object();
		var swap = (ObjectSwap<T,Object>)eType.getSwap(this);
		var builder = (BuilderSwap<T,Object>)eType.getBuilderSwap(this);
		ClassMeta<?> sType = null;
		if (nn(builder))
			sType = builder.getBuilderClassMeta(this);
		else if (nn(swap))
			sType = swap.getSwapClassMeta(this);
		else
			sType = eType;

		if (sType.isOptional())
			return (T)opt(parseAnything(eType.getElementType(), is, outer, pMeta));

		setCurrentClass(sType);

		Object o = null;
		DataType dt = is.readDataType();
		long len = is.readLength();

		// Handle CBOR semantic tags: skip tag, parse following data item
		while (dt == TAG) {
			dt = is.readDataType();
			len = is.readLength();
		}

		if (dt != NULL) {
			if (dt == BOOLEAN)
				o = is.readBoolean();
			else if (dt == UINT || dt == NINT)
				o = is.readLong();
			else if (dt == FLOAT) {
				// Prefer double for conversion; single-precision if target is Float
				if (sType.isFloat() && !sType.isDouble())
					o = is.readFloat();
				else
					o = is.readDouble();
			} else if (dt == STRING)
				o = trim(is.readString());
			else if (dt == BINARY)
				o = is.readBinary();
			else if (dt == ARRAY && sType.isObject()) {
				var jl = newGenericList();
				for (var i = 0; i < len; i++)
					jl.add(parseAnything(object(), is, outer, pMeta));
				o = jl;
			} else if (dt == MAP && sType.isObject()) {
				var jm = newGenericMap();
				for (var i = 0; i < len; i++)
					jm.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, jm, pMeta));
				o = cast(jm, pMeta, eType);
			}

			if (sType.isObject()) {
				// Do nothing.
			} else if (nn(builder) || sType.canCreateNewBean(outer)) {
				if (dt == MAP) {
					BeanMap m = builder == null ? newBeanMap(outer, sType.inner()) : toBeanMap(builder.create(this, eType));
					for (var i = 0; i < len; i++) {
						String pName = parseAnything(string(), is, m.getBean(false), null);
						var bpm = m.getPropertyMeta(pName);
						if (bpm == null) {
							if (pName.equals(getBeanTypePropertyName(eType)))
								parseAnything(string(), is, null, null);
							else
								onUnknownProperty(pName, m, parseAnything(string(), is, null, null));
						} else {
							var cm = (ClassMeta<?>) bpm.getBeanInfo();
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
			} else if (sType.isMap()) {
				if (dt == MAP) {
					Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
					for (var i = 0; i < len; i++) {
						Object key = parseAnything(sType.getKeyType(), is, outer, pMeta);
						var vt = sType.getValueType();
						Object value = parseAnything(vt, is, m, pMeta);
						setName(vt, value, key);
						m.put(key, value);
					}
					o = m;
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.isBoolean() || sType.isCharSequence() || sType.isChar() || sType.isNumber() || sType.isByteArray()) {
				// Merged scalar tier: one CBOR data type (BOOLEAN/UINT/NINT/FLOAT/STRING/BINARY) covers
				// many scalar Java types — the read already happened above and convertToType narrows.
				o = convertToType(o, sType);
			} else if (sType.isDate()) {
				o = parseDate(String.valueOf(o), sType);
			} else if (sType.isCalendar()) {
				o = parseCalendar(String.valueOf(o), sType);
			} else if (sType.isTemporal()) {
				o = parseTemporal(String.valueOf(o), sType);
			} else if (sType.isDuration()) {
				o = parseDuration(String.valueOf(o));
			} else if (sType.isPeriod()) {
				o = parsePeriod(String.valueOf(o));
			} else if (sType.canCreateNewInstanceFromString(outer) && dt == STRING) {
				o = sType.newInstanceFromString(outer, o == null ? "" : o.toString());
			} else if (sType.isCollection()) {
				if (dt == MAP) {
					var m = newGenericMap();
					for (var i = 0; i < len; i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : newGenericList());
					for (var i = 0; i < len; i++)
						l.add(parseAnything(sType.getElementType(), is, l, pMeta));
					o = l;
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.isArray() || sType.isArgs()) {
				if (dt == MAP) {
					var m = newGenericMap();
					for (var i = 0; i < len; i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (sType.isCollection() && sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : newGenericList());
					for (var i = 0; i < len; i++)
						l.add(parseAnything(sType.isArgs() ? sType.getArg(i) : sType.getElementType(), is, l, pMeta));
					o = toArray(sType, l);
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (dt == MAP) {
				var m = newGenericMap();
				for (var i = 0; i < len; i++)
					m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
				if (m.containsKey(getBeanTypePropertyName(eType)))
					o = cast(m, pMeta, eType);
				else if (nn(sType.getProxyInvocationHandler()))
					o = newBeanMap(outer, sType.inner()).load(m).getBean();
				else
					throw new ParseException(this, "Class ''{0}'' could not be instantiated.  Reason: ''{1}''", cn(sType), sType.getNotABeanReason());
			} else if (dt == UNDEFINED || dt == SIMPLE) {
				// Treat as null for unknown simple values
				o = null;
			} else {
				throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
			}
		}

		if (nn(swap) && nn(o))
			o = unswap(swap, o, eType);

		if (nn(outer))
			setParent(eType, o, outer);

		return (T)o;
	}

	@Override /* Overridden from ParserSession */
	protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (CborInputStream is = new CborInputStream(pipe)) {
			return parseAnything(type, is, getOuter(), null);
		}
	}
}
