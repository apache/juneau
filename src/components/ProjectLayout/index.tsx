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
import {PageMetadata} from '@docusaurus/theme-common';
import ProjectRail from '@site/src/components/ProjectRail';
import {projectBySlug} from '@site/src/data/projects';
import styles from './styles.module.css';

type Props = {slug?: string; children: ReactNode};

// Wraps an /about page body: the project rail beside the content, and for a project page its name, tagline, module
// list and Read-more links.  No slug means the overview, which supplies its own heading.  The MDX page wrapper already
// provides the page container and <main>, so this adds neither; every about page sets hide_table_of_contents.
export default function ProjectLayout({slug, children}: Props): ReactNode {
  const project = slug === undefined ? undefined : projectBySlug(slug);
  if (slug !== undefined && project === undefined) {
    // An error thrown during static generation fails `npm run build`, unlike a broken link.
    throw new Error(`ProjectLayout: no project with slug "${slug}" in src/data/projects.ts`);
  }
  return (
    <div className={styles.layout}>
      {project && <PageMetadata title={project.name} description={project.tagline} />}
      <ProjectRail current={slug} />
      <div className={styles.content}>
        {project && (
          <>
            <h1>{project.name}</h1>
            <p className={styles.lead}>{project.tagline}</p>
          </>
        )}
        {children}
        {project && (
          <>
            <p className={styles.modules}>
              <strong>Modules:</strong>{' '}
              {project.modules.map((m, i) => (
                <span key={m}>
                  {i > 0 && ', '}
                  <code>{m}</code>
                </span>
              ))}
            </p>
            <p className={styles.readMore}>
              {project.docs.map((d, i) => (
                <span key={d}>
                  {i > 0 && ' '}
                  <Link to={`/docs/topics/${d}/`}>{i === 0 ? 'Read more →' : `Also: ${d}`}</Link>
                </span>
              ))}
            </p>
          </>
        )}
      </div>
    </div>
  );
}
