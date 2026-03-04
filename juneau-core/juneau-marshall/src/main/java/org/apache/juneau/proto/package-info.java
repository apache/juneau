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
 * Protobuf Text Format serializer and parser for Apache Juneau.
 *
 * <p>
 * Implements the <a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format</a>
 * for the human-readable representation of structured data. This is the text format used for protobuf
 * configuration files and debugging, <b>NOT</b> the binary wire format.
 *
 * <p>
 * No <c>.proto</c> schema files are required—Java bean metadata provides the type information.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	String proto = ProtoSerializer.<jsf>DEFAULT</jsf>.serialize(myBean);
 * 	MyBean bean = ProtoParser.<jsf>DEFAULT</jsf>.parse(proto, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Or use the marshaller:</jc>
 * 	String proto = Proto.<jsm>of</jsm>(myBean);
 * 	MyBean bean = Proto.<jsm>to</jsm>(proto, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * 	<li class='link'><a class="doclink" href="https://protobuf.dev/reference/protobuf/textformat-spec">Protobuf Text Format Specification</a>
 * </ul>
 */
package org.apache.juneau.proto;
