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
 * Transforms {@link Calendar Calendars} to {@link Map Maps} of the format <code>{_class:String,value:long}</code>.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings("rawtypes")
public class CalendarMapSwap extends PojoSwap<Calendar,Map> {

	/**
	 * Converts the specified {@link Calendar} to a {@link Map}.
	 */
	@Override /* PojoSwap */
	public Map swap(Calendar o) {
		ObjectMap m = new ObjectMap();
		m.put("time", o.getTime().getTime());
		m.put("timeZone", o.getTimeZone().getID());
		return m;
	}

	/**
	 * Converts the specified {@link Map} to a {@link Calendar}.
	 */
	@Override /* PojoSwap */
	@SuppressWarnings("unchecked")
	public Calendar unswap(Map o, ClassMeta<?> hint, BeanContext bc) throws ParseException {
		ClassMeta<? extends Calendar> tt;
		try {
			if (hint == null || ! hint.canCreateNewInstance())
				hint = bc.getClassMeta(GregorianCalendar.class);
			tt = (ClassMeta<? extends Calendar>)hint;
			long time = Long.parseLong(o.get("time").toString());
			String timeZone = o.get("timeZone").toString();
			Date d = new Date(time);
			Calendar c = tt.newInstance();
			c.setTime(d);
			c.setTimeZone(TimeZone.getTimeZone(timeZone));
			return c;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}
}
