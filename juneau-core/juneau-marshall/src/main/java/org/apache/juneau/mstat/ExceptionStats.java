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
package org.apache.juneau.mstat;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.annotation.*;

/**
 * Represents an entry in {@link ExceptionStore}.
 */
@Bean(properties="hash,count,exceptionClass,message,stackTrace,causedBy", fluentSetters=true)
public class ExceptionStats extends ExceptionInfo implements Comparable<ExceptionStats> {
	private final AtomicInteger count = new AtomicInteger(0);
	private transient long timeout = -1;

	public static ExceptionStats create() {
		return new ExceptionStats();
	}

	/**
	 * Returns the number of times this stack trace was encountered.
	 *
	 * @return The number of times this stack trace was encountered.
	 */
	public int getCount() {
		return count.intValue();
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionStats count(int value) {
		count.set(value);
		return this;
	}

	/**
	 * Increments the occurrence count of this exception.
	 *
	 * @return This object (for method chaining).
	 */
	public ExceptionStats increment() {
		count.incrementAndGet();
		return this;
	}

	/**
	 * TODO
	 *
	 * @param value TODO
	 * @return This object (for method chaining).
	 */
	public ExceptionStats timeout(long value) {
		this.timeout = value;
		return this;
	}

	/**
	 * TODO
	 *
	 * @return TODO
	 */
	public boolean isExpired() {
		return timeout >= 0 && System.currentTimeMillis() > timeout;
	}


	@Override
	public ExceptionStats hash(String value) {
		super.hash(value);
		return this;
	}

	@Override
	public ExceptionStats exceptionClass(String value) {
		super.exceptionClass(value);
		return this;
	}

	@Override
	public ExceptionStats message(String value) {
		super.message(value);
		return this;
	}

	@Override
	public ExceptionStats stackTrace(List<StackTraceElement> value) {
		super.stackTrace(value);
		return this;
	}

	@Override
	public ExceptionStats causedBy(ExceptionInfo value) {
		super.causedBy(value);
		return this;
	}



	@Override /* Comparable */
	public int compareTo(ExceptionStats o) {
		return Integer.compare(o.getCount(), getCount());
	}

	@Override /* Object */
	public ExceptionStats clone() {
		try {
			return (ExceptionStats) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
