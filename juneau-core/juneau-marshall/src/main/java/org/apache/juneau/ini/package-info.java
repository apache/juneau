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

/**
 * INI format serializer and parser for Apache Juneau.
 *
 * <p>
 * INI is a classic configuration file format with sections (<c>[sectionName]</c>) and key-value pairs.
 * Beans map to INI with simple properties as key-value pairs and nested beans as named sections.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	String ini = IniSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 * 	MyBean bean = IniParser.<jsf>DEFAULT</jsf>.parse(<jv>ini</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Or use the marshaller:</jc>
 * 	String ini = Ini.<jsf>of</jsf>(<jv>myBean</jv>);
 * 	MyBean bean = Ini.<jsf>to</jsf>(<jv>ini</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/IniBasics">INI Basics</a>
 * </ul>
 */
package org.apache.juneau.ini;
