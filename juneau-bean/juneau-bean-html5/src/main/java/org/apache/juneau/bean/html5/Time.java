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
package org.apache.juneau.bean.html5;

import org.apache.juneau.marshall.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#the-time-element">&lt;time&gt;</a>
 * element.
 *
 * <p>
 * The time element represents a specific point in time or a duration. It provides a machine-readable
 * way to mark up dates and times in HTML, making it easier for search engines, screen readers, and
 * other tools to understand and process temporal information. The element can contain human-readable
 * text while providing a machine-readable datetime attribute.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple date</jc>
 * 	Time <jv>date</jv> = <jsm>time</jsm>(<js>"January 15, 2024"</js>)
 * 		.datetime(<js>"2024-01-15"</js>);
 *
 * 	<jc>// Date and time</jc>
 * 	Time <jv>datetime</jv> = <jsm>time</jsm>(<js>"2:30 PM on January 15, 2024"</js>)
 * 		.datetime(<js>"2024-01-15T14:30:00Z"</js>);
 *
 * 	<jc>// Relative time</jc>
 * 	Time <jv>relative</jv> = <jsm>time</jsm>(<js>"yesterday"</js>)
 * 		.datetime(<js>"2024-01-15"</js>);
 *
 * 	<jc>// Duration</jc>
 * 	Time <jv>duration</jv> = <jsm>time</jsm>(<js>"2 hours and 30 minutes"</js>)
 * 		.datetime(<js>"PT2H30M"</js>);
 *
 * 	<jc>// Time with styling</jc>
 * 	Time <jv>styled</jv> = <jsm>time</jsm>(<js>"Event Date: January 15, 2024"</js>)
 * 		.datetime(<js>"2024-01-15"</js>)
 * 		.class_(<js>"event-date"</js>);
 *
 * 	<jc>// Time with timezone</jc>
 * 	Time <jv>timezone</jv> = <jsm>time</jsm>(<js>"2:30 PM EST on January 15, 2024"</js>)
 * 		.datetime(<js>"2024-01-15T14:30:00-05:00"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#time() time()}
 * 		<li class='jm'>{@link HtmlBuilder#time(Object...) time(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "time")
public class Time extends HtmlElementMixed<Time> {

	/**
	 * Creates an empty {@link Time} element.
	 */
	public Time() {}

	/**
	 * Creates a {@link Time} element with the specified {@link Time#children(Object[])} nodes.
	 *
	 * @param children The {@link Time#children(Object[])} nodes. Must not be <jk>null</jk>.
	 */
	public Time(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/text-level-semantics.html#attr-time-datetime">datetime</a>
	 * attribute.
	 *
	 * <p>
	 * Specifies the machine-readable value of the time element. This provides a standardized
	 * format for the date and time that can be processed by computers.
	 *
	 * <p>
	 * The value should be a valid date-time string in ISO 8601 format.
	 *
	 * @param value The machine-readable date and time value. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Time datetime(String value) {
		attr("datetime", value);
		return this;
	}
}