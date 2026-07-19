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
package org.apache.juneau.marshall.msgpack;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.marshall.msgpack.DataType.*;

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
 * Session object that lives for the duration of a single use of {@link MsgPackParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MessagePackSupport">MessagePack Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"rawtypes", // Raw types necessary for generic type handling
	"unchecked", // Type erasure requires unchecked casts
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource"  // Closeable resources are owned by the caller's parser session; Eclipse JDT @Owning warning is by design.
})
public class MsgPackParserSession extends InputStreamParserSession implements TokenReadable, ArrayRecordReadable {

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
		protected Builder(MsgPackParser ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.nativeMode = ctx.isNativeMode();
		}

		@Override
		public MsgPackParserSession build() {
			return new MsgPackParserSession(this);
		}

	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder create(MsgPackParser ctx) {
		return new Builder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * Maximum databind parse-recursion depth.
	 *
	 * <p>
	 * The databind {@link #readAnything(ClassMeta, MsgPackInputStream, Object, BeanPropertyMeta) readAnything}
	 * path recurses once per nesting level; this bound makes an adversarial deeply-nested document fail with a
	 * {@link ParseException} instead of a {@link StackOverflowError}.  The token-cursor path is iterative and
	 * unaffected.
	 */
	private static final int MAX_PARSE_DEPTH = 1000;

	private final boolean nativeMode;

	private int parseDepth;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MsgPackParserSession(Builder builder) {
		super(builder);
		this.nativeMode = builder.nativeMode;
	}

	/**
	 * Opens a low-level pull-parser cursor over a MessagePack document, bound to this live session.
	 *
	 * <h5 class='section'>Builder properties honored:</h5>
	 * <c>autoCloseStreams</c>, <c>unbuffered</c>.
	 *
	 * <h5 class='section'>Builder properties NOT honored:</h5>
	 * <c>listener</c>, <c>debugOutputLines</c>, <c>consumes</c>, swaps, {@code @Schema}.
	 *
	 * @param input The input.
	 * @return A new {@link MsgPackTokenReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@SuppressWarnings({
		"java:S2095" // ParserPipe lifecycle is transferred to the returned MsgPackTokenReader, which closes it via its own close(); the caller owns the cursor via try-with-resources.
	})
	@Override /* TokenReadable */
	public TokenReader readTokens(Object input) throws IOException {
		var pipe = new ParserPipe(input, isDebug(), isAutoCloseStreams(), isUnbuffered(), null);
		return new MsgPackTokenReader(pipe, this).setNativeMode(nativeMode);
	}

	/**
	 * Streaming array-element {@link RecordReader} backed by {@link MsgPackTokenReader}.
	 *
	 * <p>
	 * Memory is O(1) in the array length &mdash; the wrapper opens the outer MessagePack array via
	 * the token cursor and binds one element at a time, exactly mirroring the CBOR cursor.  The
	 * returned cursor reports {@link RecordReader#isStreaming()} == <jk>true</jk>.
	 *
	 * @param input The input.
	 * @return A new element-streamed {@link RecordReader}.
	 * @throws IOException If a problem occurred opening the underlying input.
	 */
	@Override /* ArrayRecordReadable */
	public RecordReader readArrayRecords(Object input) throws IOException {
		try {
			return StreamingArrayRecord.reader(readTokens(input));
		} catch (ParseException e) {
			throw new IOException(e);
		}
	}

	/*
	 * Workhorse entry point.  Wraps {@link #readAnything0} with a recursion-depth guard so that an
	 * adversarial deeply-nested document fails with a {@link ParseException} instead of a
	 * {@link StackOverflowError}.
	 */
	<T> T readAnything(ClassMeta<?> eType, MsgPackInputStream is, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {
		if (++parseDepth > MAX_PARSE_DEPTH) {
			parseDepth--;
			throw new ParseException(this, "Maximum parse depth exceeded (%s).", MAX_PARSE_DEPTH);
		}
		try {
			return readAnything0(eType, is, outer, pMeta);
		} finally {
			parseDepth--;
		}
	}

	/*
	 * Workhorse method.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for this specific logic
		"java:S6541"  // Single-threaded session contexts do not require synchronization
	})
	private <T> T readAnything0(ClassMeta<?> eType, MsgPackInputStream is, Object outer, BeanPropertyMeta pMeta) throws IOException, ParseException, ExecutableException {

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
			return (T)o(readAnything(eType.getElementType(), is, outer, pMeta));

		setCurrentClass(sType);

		Object o = null;
		DataType dt = is.readDataType();
		var rawLength = is.readLength();
		// G8: array32/map32/str32/bin32/ext32 lengths are 32-bit unsigned (up to 2^32-1); a value in
		// [2^31, 2^32-1] would truncate to a negative int.  Java containers/arrays cannot exceed int range.
		if (rawLength > Integer.MAX_VALUE)
			throw new ParseException(this, "MessagePack length %s exceeds the maximum supported size of %s.", rawLength, Integer.MAX_VALUE);
		int length = (int)rawLength;

		if (dt != DataType.NULL) {
			if (dt == BOOLEAN)
				o = is.readBoolean();
			else if (dt == INT)
				o = is.readInt();
			else if (dt == LONG) {
				var l = is.readLong();
				// G3: a uint64 above Long.MAX_VALUE comes back as a negative signed long.  Mirror the
				// protobuf R5 cross-format decision: a long target keeps the raw bits, a BigInteger target
				// carries the full unsigned magnitude (lossless widen).
				if (is.isUint64() && sType.inner() == BigInteger.class)
					o = new BigInteger(Long.toUnsignedString(l));
				else
					o = l;
			} else if (dt == FLOAT)
				o = is.readFloat();
			else if (dt == DOUBLE)
				o = is.readDouble();
			else if (dt == STRING)
				o = trim(is.readString());
			else if (dt == BIN)
				o = is.readBinary();
			else if (dt == EXT)
				// G1: always consume the ext payload (mirrors the token cursor) and surface it as byte[].
				// Without this branch the payload bytes are left in the stream, desyncing the next element.
				o = is.readBinary();
			else if (dt == ARRAY && sType.isObject()) {
				var jl = newGenericList();
				for (var i = 0; i < length; i++)
					jl.add(readAnything(object(), is, outer, pMeta));
				o = jl;
			} else if (dt == MAP && sType.isObject()) {
				var jm = newGenericMap();
				for (var i = 0; i < length; i++)
					jm.put((String)readAnything(string(), is, outer, pMeta), readAnything(object(), is, jm, pMeta));
				o = cast(jm, pMeta, eType);
			}

			if (sType.isObject()) {
				// Do nothing.
			} else if (nn(builder) || sType.canCreateNewBean(outer)) {
				if (dt == MAP) {
					BeanMap m = builder == null ? newBeanMap(outer, sType.inner()) : toBeanMap(builder.create(this, eType));
					for (var i = 0; i < length; i++) {
						String pName = readAnything(string(), is, m.getBean(false), null);
						var bpm = m.getPropertyMeta(pName);
						if (bpm == null) {
							if (pName.equals(getBeanTypePropertyName(eType)))
								readAnything(string(), is, null, null);
							else
								onUnknownProperty(pName, m, readAnything(string(), is, null, null));
						} else {
							var cm = (ClassMeta<?>) bpm.getBeanInfo();
							Object value = readAnything(cm, is, m.getBean(false), bpm);
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
					throw new ParseException(this, "Invalid data type %s encountered for parse type %s", dt, sType);
				}
			} else if (sType.isMap()) {
				if (dt == MAP) {
					Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : newGenericMap(sType));
					for (var i = 0; i < length; i++) {
						Object key = readAnything(sType.getKeyType(), is, outer, pMeta);
						var vt = sType.getValueType();
						Object value = readAnything(vt, is, m, pMeta);
						setName(vt, value, key);
						m.put(key, value);
					}
					o = m;
				} else {
					throw new ParseException(this, "Invalid data type %s encountered for parse type %s", dt, sType);
				}
			} else if (sType.isBoolean() || sType.isCharSequence() || sType.isChar() || sType.isNumber() || sType.isByteArray()) {
				// Merged scalar tier: one wire-type (BOOLEAN/INT/LONG/FLOAT/DOUBLE/STRING/BIN) covers
				// many scalar Java types — the read already happened above and convertToType narrows.
				o = convertToType(o, sType);
			} else if (sType.isDate()) {
				o = readDate(String.valueOf(o), sType);
			} else if (sType.isCalendar()) {
				o = readCalendar(String.valueOf(o), sType);
			} else if (sType.isTemporal()) {
				o = readTemporal(String.valueOf(o), sType);
			} else if (sType.isDuration()) {
				o = readDuration(String.valueOf(o));
			} else if (sType.isPeriod()) {
				o = readPeriod(String.valueOf(o));
			} else if (sType.canCreateNewInstanceFromString(outer) && dt == STRING) {
				o = sType.newInstanceFromString(outer, o == null ? "" : o.toString());
			} else if (sType.isCollection()) {
				if (dt == MAP) {
					var m = newGenericMap();
					for (var i = 0; i < length; i++)
						m.put((String)readAnything(string(), is, outer, pMeta), readAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : newGenericList());
					for (var i = 0; i < length; i++)
						l.add(readAnything(sType.getElementType(), is, l, pMeta));
					o = l;
				} else {
					throw new ParseException(this, "Invalid data type %s encountered for parse type %s", dt, sType);
				}
			} else if (sType.isArray() || sType.isArgs()) {
				if (dt == MAP) {
					var m = newGenericMap();
					for (var i = 0; i < length; i++)
						m.put((String)readAnything(string(), is, outer, pMeta), readAnything(object(), is, m, pMeta));
					o = cast(m, pMeta, eType);
				} else if (dt == ARRAY) {
					Collection l = (sType.isCollection() && sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance() : newGenericList());
					for (var i = 0; i < length; i++)
						l.add(readAnything(sType.isArgs() ? sType.getArg(i) : sType.getElementType(), is, l, pMeta));
					o = toArray(sType, l);
				} else {
					throw new ParseException(this, "Invalid data type %s encountered for parse type %s", dt, sType);
				}
			} else if (dt == MAP) {
				var m = newGenericMap();
				for (var i = 0; i < length; i++)
					m.put((String)readAnything(string(), is, outer, pMeta), readAnything(object(), is, m, pMeta));
				if (m.containsKey(getBeanTypePropertyName(eType)))
					o = cast(m, pMeta, eType);
				else if (nn(sType.getProxyInvocationHandler()))
					o = newBeanMap(outer, sType.inner()).load(m).getBean();
				else
					throw new ParseException(this, "Class '%s' could not be instantiated.  Reason: '%s'", cn(sType), sType.getNotABeanReason());
			} else {
				throw new ParseException(this, "Invalid data type %s encountered for parse type %s", dt, sType);
			}
		}

		if (nn(swap) && nn(o))
			o = unswap(swap, o, eType);

		if (nn(outer))
			setParent(eType, o, outer);

		return (T)o;
	}

	@Override /* Overridden from ParserSession */
	protected <T> T doRead(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		try (MsgPackInputStream is = new MsgPackInputStream(pipe)) {
			return readAnything(type, is, getOuter(), null);
		}
	}
}
