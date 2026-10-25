/*
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
 */

import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import {GROUPS, PROJECTS} from '@site/src/data/projects';
import styles from './styles.module.css';

export default function ProjectStrip(): ReactNode {
  return (
    <section className={`juneau-project-strip ${styles.strip}`}>
      <div className="container">
        <h2 className={styles.title}>The Juneau projects</h2>
        <div className={styles.columns}>
          {GROUPS.map((g) => (
            <div key={g.id}>
              <h3 className={styles.groupLabel}>{g.label}</h3>
              <ul className={styles.items}>
                {PROJECTS.filter((p) => p.group === g.id).map((p) => (
                  <li key={p.slug}>
                    <Link to={`/about/${p.slug}/`}>{p.name}</Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
