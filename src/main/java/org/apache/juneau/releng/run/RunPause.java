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
package org.apache.juneau.releng.run;

/**
 * Why a run is waiting for a person: the {@code pause} block of {@code run.json}, written by {@code release.py} when a
 * step pauses and cleared when the next attempt starts.
 */
public class RunPause {

	/** The step that paused. */
	public String step;

	/** The one-line instruction {@code release.py} printed, ending in the resume command. */
	public String message;

	/** When it paused ({@code yyyyMMdd-HHmmss}). */
	public String at;
}
