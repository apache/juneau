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
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/edits.html#the-del-element">&lt;del&gt;</a>
 * element.
 *
 * <p>
 * The del element represents text that has been deleted from a document. It is typically rendered
 * with a strikethrough effect to indicate that the content has been removed. The cite and datetime
 * attributes can be used to provide information about when and why the deletion occurred.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.bean.html5.HtmlBuilder.*;
 *
 * 	<jc>// Simple deleted text</jc>
 * 	Del <jv>del1</jv> = <jsm>del</jsm>(<js>"This text has been removed."</js>);
 *
 * 	<jc>// Deleted text with citation and timestamp</jc>
 * 	Del <jv>del2</jv> = <jsm>del</jsm>()
 * 		.cite(<js>"https://example.com/revision-log"</js>)
 * 		.datetime(<js>"2024-01-15T10:30:00Z"</js>)
 * 		.text(<js>"Outdated information removed"</js>);
 *
 * 	<jc>// Deleted text with reason</jc>
 * 	Del <jv>del3</jv> = <jsm>del</jsm>()
 * 		.cite(<js>"https://example.com/corrections"</js>)
 * 		.datetime(<js>"2024-01-15"</js>)
 * 		.text(<js>"Incorrect statement"</js>)
 * 		.title(<js>"Removed due to factual error"</js>);
 * </p>
 *
 * <p>
 * The following convenience methods are provided for constructing instances of this bean:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link HtmlBuilder}
 * 	<ul class='javatree'>
 * 		<li class='jm'>{@link HtmlBuilder#del() del()}
 * 		<li class='jm'>{@link HtmlBuilder#del(Object...) del(Object...)}
 * 	</ul>
 * </ul>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 */
@Marshalled(typeName = "del")
public class Del extends HtmlElementMixed<Del> {

	/**
	 * Creates an empty {@link Del} element.
	 */
	public Del() {}

	/**
	 * Creates a {@link Del} element with the specified {@link Del#children(Object[])} node.
	 *
	 * @param children The {@link Del#children(Object[])} node. Must not be <jk>null</jk>.
	 */
	public Del(Object...children) {
		children(children);
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/edits.html#attr-mod-cite">cite</a> attribute.
	 *
	 * <p>
	 * Specifies the URL of a document that explains the reason for the deletion.
	 * This provides context and justification for the edit.
	 *
	 * <p>
	 * The URL should point to a document that explains why the content was deleted.
	 *
	 * @param value The URL explaining the reason for the deletion. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Del cite(String value) {
		attr("cite", value);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/edits.html#attr-mod-datetime">datetime</a> attribute.
	 *
	 * <p>
	 * Specifies the date and time when the content was deleted. Used for tracking
	 * the history of document changes.
	 *
	 * <p>
	 * The value should be a valid date-time string in ISO 8601 format.
	 *
	 * @param value The date and time when the content was deleted. Can be <jk>null</jk> to unset the attribute.
	 * @return This object.
	 */
	public Del datetime(String value) {
		attr("datetime", value);
		return this;
	}

}