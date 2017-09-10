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
package org.apache.juneau.transforms;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.transform.*;

/**
 * Transforms {@link Calendar Calendars} to {@link Long Longs} using {@code Calender.getTime().getTime()}.
 */
public class CalendarLongSwap extends PojoSwap<Calendar,Long> {

	/**
	 * Converts the specified {@link Calendar} to a {@link Long}.
	 */
	@Override /* PojoSwap */
	public Long swap(BeanSession session, Calendar o) {
		return o.getTime().getTime();
	}

	/**
	 * Converts the specified {@link Long} to a {@link Calendar}.
	 */
	@Override /* PojoSwap */
	@SuppressWarnings("unchecked")
	public Calendar unswap(BeanSession session, Long o, ClassMeta<?> hint) throws Exception {
		ClassMeta<? extends Calendar> tt;
		if (hint == null || ! hint.canCreateNewInstance())
			hint = session.getClassMeta(GregorianCalendar.class);
		tt = (ClassMeta<? extends Calendar>)hint;
		Calendar c = tt.newInstance();
		c.setTimeInMillis(o);
		return c;
	}
}
