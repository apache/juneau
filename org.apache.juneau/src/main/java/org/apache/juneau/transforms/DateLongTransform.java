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
package org.apache.juneau.transforms;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;

/**
 * Transforms {@link Date Dates} to {@link Long Longs}.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class DateLongTransform extends PojoTransform<Date,Long> {

	/**
	 * Converts the specified {@link Date} to a {@link Long}.
	 */
	@Override /* PojoTransform */
	public Long transform(Date o) {
		return o.getTime();
	}

	/**
	 * Converts the specified {@link Long} to a {@link Date}.
	 */
	@Override /* PojoTransform */
	public Date normalize(Long o, ClassMeta<?> hint) throws ParseException {
		Class<?> c = (hint == null ? java.util.Date.class : hint.getInnerClass());
		if (c == java.util.Date.class)
			return new java.util.Date(o);
		if (c == java.sql.Date.class)
			return new java.sql.Date(o);
		if (c == java.sql.Time.class)
			return new java.sql.Time(o);
		if (c == java.sql.Timestamp.class)
			return new java.sql.Timestamp(o);
		throw new ParseException("DateLongTransform is unable to narrow object of type ''{0}''", c);
	}
}
