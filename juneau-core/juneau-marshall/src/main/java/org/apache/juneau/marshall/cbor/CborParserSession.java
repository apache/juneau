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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.cbor.DataType.*;

import java.io.*;
import java.math.*;
import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link CborParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Cbor">CBOR Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"rawtypes", // Raw types necessary for generic type handling
	"unchecked", // Type erasure requires unchecked casts
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource"  // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class CborParserSession extends InputStreamParserSession implements TokenReadable, ArrayRecordReadable {

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParserSession.Builder<Builder> {

		final boolean nativeMode;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(CborParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.nativeMode = ctx.isNativeMode();
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

	private final boolean nativeMode;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected CborParserSession(Builder builder) {
		super(builder);
		this.nativeMode = builder.nativeMode;
	}

	/**
	 * Opens a low-level pull-parser cursor over a CBOR document, bound to this live session.
	 *
	 * <p>
	 * CBOR is already a token-shaped wire format (RFC 8949), so this cursor maps almost directly
	 * onto {@link CborInputStream#readDataType()}.  Indefinite-length containers
	 * (<c>0x9F</c> / <c>0xBF</c>) are handled by tracking a sentinel element-count of -1 on the
	 * container stack and emitting END_* on encountering the BREAK byte.
	 *
	 * <h5 class='section'>Builder properties honored:</h5>
	 * <c>autoCloseStreams</c>, <c>unbuffered</c>.
	 *
	 * <h5 class='section'>Builder properties NOT honored:</h5>
	 * <c>listener</c>, <c>debugOutputLines</c>, <c>consumes</c>, swaps, {@code @Schema}.
	 *
	 * @param input The input.
	 * @return A new {@link CborTokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@SuppressWarnings({
		"java:S2095" // ParserPipe lifecycle is transferred to the returned CborTokenReader, which closes it via its own close(); the caller owns the cursor via try-with-resources.
	})
	@Override /* TokenReadable */
	public TokenReader parseTokens(Object input) throws IOException {
		var pipe = new ParserPipe(input, isDebug(), isAutoCloseStreams(), isUnbuffered(), null);
		return new CborTokenReader(pipe, this).setNativeMode(nativeMode);
	}

	/**
	 * Streaming array-element {@link RecordReader} backed by {@link CborTokenReader}.
	 * Memory is O(1) in the array length &mdash; CBOR's indefinite-length array support
	 * (0x9F .. 0xFF) makes the wrapper trivial.
	 *
	 * @param input The input.
	 * @return A new element-streamed {@link RecordReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader parseArrayRecords(Object input) throws IOException {
		try {
			return StreamingArrayRecord.reader(parseTokens(input));
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	/*
	 * Workhorse method.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541"  // Single-threaded session contexts do not require synchronization
	})
	<T> T parseAnything(ClassMeta<?> eType, CborInputStream is, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

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
			return (T)o(parseAnything(eType.getElementType(), is, outer, pMeta));

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
			else if (dt == UINT || dt == NINT) {
				// Surface unsigned 64-bit by the target Java type (175fc GAP-1, aligned with the
				// proto-binary R5 decision): a BigInteger field carries the full magnitude (values
				// beyond signed long widen losslessly); any other type keeps the raw 64-bit bits as
				// a long (preserving the native int64 round-trip used by epoch/millis temporal and
				// duration values).
				o = sType.inner() == BigInteger.class ? is.readBigInteger() : (Object)is.readLong();
			}
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
				for (var i = 0; shouldContinueContainer(is, len, i); i++)
					jl.add(parseAnything(object(), is, outer, pMeta));
				o = jl;
			} else if (dt == MAP && sType.isObject()) {
				var jm = newGenericMap();
				for (var i = 0; shouldContinueContainer(is, len, i); i++)
					jm.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, jm, pMeta));
				o = cast(jm, pMeta, eType);
			}

			if (sType.isObject()) {
				// Do nothing.
			} else if (nn(builder) || sType.canCreateNewBean(outer)) {
				if (dt == MAP) {
					BeanMap m = builder == null ? newBeanMap(outer, sType.inner()) : toBeanMap(builder.create(this, eType));
					for (var i = 0; shouldContinueContainer(is, len, i); i++) {
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
					for (var i = 0; shouldContinueContainer(is, len, i); i++) {
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
					for (var i = 0; shouldContinueContainer(is, len, i); i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : newGenericList());
					for (var i = 0; shouldContinueContainer(is, len, i); i++)
						l.add(parseAnything(sType.getElementType(), is, l, pMeta));
					o = l;
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (sType.isArray() || sType.isArgs()) {
				if (dt == MAP) {
					var m = newGenericMap();
					for (var i = 0; shouldContinueContainer(is, len, i); i++)
						m.put((String)parseAnything(string(), is, outer, pMeta), parseAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (sType.isCollection() && sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : newGenericList());
					for (var i = 0; shouldContinueContainer(is, len, i); i++)
						l.add(parseAnything(sType.isArgs() ? sType.getArg(i) : sType.getElementType(), is, l, pMeta));
					o = toArray(sType, l);
				} else {
					throw new ParseException(this, "Invalid data type {0} encountered for parse type {1}", dt, sType);
				}
			} else if (dt == MAP) {
				var m = newGenericMap();
				for (var i = 0; shouldContinueContainer(is, len, i); i++)
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

	/**
	 * Returns whether the next iteration of a CBOR container loop should run.
	 *
	 * <p>
	 * For definite-length containers (<c>len &gt;= 0</c>) returns <jk>true</jk> while
	 * <c>i &lt; len</c>.  For indefinite-length containers (<c>len == -1</c>) peeks for the BREAK
	 * marker and returns <jk>false</jk> when found (consuming the BREAK byte), <jk>true</jk>
	 * otherwise.
	 */
	private static boolean shouldContinueContainer(CborInputStream is, long len, int i) throws IOException {
		if (len == -1)
			return !is.peekBreak();
		return i < len;
	}
}
