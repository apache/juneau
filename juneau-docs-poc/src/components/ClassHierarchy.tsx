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

import React from 'react';
import styles from './ClassHierarchy.module.css';

interface ClassNode {
  name: string;
  type: 'interface' | 'class' | 'abstract' | 'method' | 'field' | 'annotation' | 'enum' | 'private-method' | 'private-field' | 'annotation-method';
  link?: string;
  description?: string;
  children?: ClassNode[];
}

interface ClassHierarchyProps {
  nodes: ClassNode[];
}

const getCssClass = (type: string) => {
  switch (type) {
    case 'interface':
      return 'jic'; // Java interface
    case 'class':
      return 'jc'; // Java class
    case 'abstract':
      return 'jac'; // Java abstract class
    case 'method':
      return 'jm'; // Java method
    case 'field':
      return 'jf'; // Java field
    case 'annotation':
      return 'ja'; // Java annotation
    case 'enum':
      return 'je'; // Java enum
    case 'private-method':
      return 'jmp'; // Java private method
    case 'private-field':
      return 'jfp'; // Java private field
    case 'annotation-method':
      return 'jma'; // Java annotation method
    default:
      return 'jc';
  }
};

const ClassTreeNode: React.FC<{ node: ClassNode; level: number }> = ({ node, level }) => {
  const cssClass = getCssClass(node.type);
  return (
    <li className={cssClass}>
      {node.link ? (
        <a href={node.link}>
          {node.name}
        </a>
      ) : (
        node.name
      )}
      {node.description && (
        <span> - {node.description}</span>
      )}
      {node.children && node.children.length > 0 && (
        <ul>
          {node.children.map((child, index) => (
            <ClassTreeNode key={index} node={child} level={level + 1} />
          ))}
        </ul>
      )}
    </li>
  );
};

export const ClassHierarchy: React.FC<ClassHierarchyProps> = ({ nodes }) => {
  return (
    <div className="javatreec">
      <ul>
        {nodes.map((node, index) => (
          <ClassTreeNode key={index} node={node} level={0} />
        ))}
      </ul>
    </div>
  );
};

export default ClassHierarchy;
