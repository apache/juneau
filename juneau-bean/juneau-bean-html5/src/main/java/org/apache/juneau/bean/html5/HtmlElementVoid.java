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

import static org.apache.juneau.marshall.xml.XmlFormat.*;

import org.apache.juneau.marshall.xml.*;

/**
 * A subclass of HTML elements that have no content or end tags.
 *
 * <p>
 * See <a href="https://www.w3.org/TR/html51/syntax.html#void-elements">void elements</a>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHtml5">juneau-bean-html5</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@Xml(format = VOID)
@SuppressWarnings("java:S119")  // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
public abstract class HtmlElementVoid<SELF extends HtmlElementVoid<SELF>> extends HtmlElement<SELF> {
}
