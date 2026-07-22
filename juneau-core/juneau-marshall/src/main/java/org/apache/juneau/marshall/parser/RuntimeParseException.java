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
package org.apache.juneau.marshall.parser;

/**
 * Unchecked wrapper used by the
 * {@link org.apache.juneau.marshall.stream.RecordReader#iterator(Class) iterator(...)} /
 * {@link org.apache.juneau.marshall.stream.RecordReader#stream(Class) stream(...)} views to surface the checked
 * {@link java.io.IOException} / {@link ParseException} cause
 * through the {@link java.util.Iterator} / {@link java.util.stream.Stream} API.
 *
 * <p>
 * Carries the original typed cause so pipeline consumers can recover it via
 * {@link #getCause()} and react meaningfully, rather than catching a bare
 * {@link RuntimeException}.
 */
public class RuntimeParseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param cause The underlying checked exception (typically {@link java.io.IOException} or
	 * 	{@link ParseException}).
	 * 	<br>Can be <jk>null</jk> (the exception is created with no cause and no message).
	 */
	public RuntimeParseException(Throwable cause) {
		super(cause);
	}
}
