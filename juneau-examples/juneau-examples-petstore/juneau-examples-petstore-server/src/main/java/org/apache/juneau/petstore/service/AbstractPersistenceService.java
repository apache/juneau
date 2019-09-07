// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.petstore.service;

import java.util.*;

import javax.persistence.*;

import org.apache.juneau.pojotools.*;

/**
 * Superclass for DAOs that use the JPA entity manager.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class AbstractPersistenceService {

	private final EntityManagerFactory entityManagerFactory;

	/**
	 * Constructor.
	 */
	public AbstractPersistenceService() {
		entityManagerFactory = Persistence.createEntityManagerFactory("test");
	}

	/**
	 * Retrieves an entity manager session.
	 *
	 * @return The entity manager session.
	 */
	protected EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	/**
	 * Retrieves the specified JPA bean from the repository.
	 *
	 * @param em The entity manager to use to retrieve the bean.
	 * @param t The bean type to retrieve.
	 * @param id The primary key value.
	 * @return The JPA bean, or null if not found.
	 */
	protected <T> T find(EntityManager em, Class<T> t, Object id) {
		return em.find(t, id);
	}

	/**
	 * Same as {@link #find(EntityManager, Class, Object)} but uses a new entity manager.
	 *
	 * @param t The bean type to retrieve.
	 * @param id The primary key value.
	 * @return The JPA bean, or null if not found.
	 */
	protected <T> T find(Class<T> t, Object id) {
		return find(getEntityManager(), t, id);
	}

	/**
	 * Store the specified JPA bean in the repository.
	 *
	 * @param em The entity manager to use to store and merge the bean.
	 * @param t The bean to store.
	 * @return The merged JPA bean returned by the {@link EntityManager#merge(Object)} method, or null if the bean was null.
	 */
	protected <T> T merge(EntityManager em, T t) {
		if (t == null)
			return null;
		try {
			EntityTransaction et = em.getTransaction();
			et.begin();
			t = em.merge(t);
			et.commit();
			return t;
		} finally {
			em.close();
		}
	}

	/**
	 * Same as {@link #merge(EntityManager, Object)} but uses a new entity manager.
	 *
	 * @param t The bean to store.
	 * @return The merged JPA bean returned by the {@link EntityManager#merge(Object)} method, or null if the bean was null.
	 */
	protected <T> T merge(T t) {
		return merge(getEntityManager(), t);
	}

	/**
	 * Store the specified JPA beans in the repository.
	 *
	 * All values are persisted in the same transaction.
	 *
	 * @param em The entity manager to use to store and merge the beans.
	 * @param c The collection of beans to store.
	 * @return The merged JPA beans returned by the {@link EntityManager#merge(Object)} method.
	 */
	protected <T> Collection<T> merge(EntityManager em, Collection<T> c) {
		Collection<T> c2 = new ArrayList<>();
		try {
			EntityTransaction et = em.getTransaction();
			et.begin();
			for (T t : c)
				c2.add(em.merge(t));
			et.commit();
			return c2;
		} finally {
			em.close();
		}
	}

	/**
	 * Same as {@link #merge(EntityManager, Collection)} but uses a new entity manager.
	 *
	 * @param c The collection of beans to store.
	 * @return The merged JPA beans returned by the {@link EntityManager#merge(Object)} method.
	 */
	protected <T> Collection<T> merge(Collection<T> c) {
		return merge(getEntityManager(), c);
	}

	/**
	 * Remove the specified JPA bean from the repository.
	 *
	 * @param t The bean type to remove.
	 * @param id The primary key value.
	 */
	protected <T> void remove(Class<T> t, Object id) {
		EntityManager em = getEntityManager();
		remove(em, find(em, t, id));
	}

	/**
	 * Remove the specified JPA bean from the repository.
	 *
	 * @param em The entity manager used to retrieve the bean.
	 * @param t The bean to remove.  Can be null.
	 */
	protected <T> void remove(EntityManager em, T t) {
		if (t == null)
			return;
		try {
			EntityTransaction et = em.getTransaction();
			et.begin();
			em.remove(t);
			et.commit();
		} finally {
			em.close();
		}
	}

	/**
	 * Runs a JPA query and returns the results.
	 *
	 * @param <T> The bean type.
	 * @param em The entity manager to use to retrieve the beans.
	 * @param query The JPA query.
	 * @param t The bean type.
	 * @param searchArgs Optional search arguments.
	 * @return The results.
	 */
	protected <T> List<T> query(EntityManager em, String query, Class<T> t, SearchArgs searchArgs, PageArgs pageArgs) {
		TypedQuery<T> q = em.createQuery(query, t);
		if (pageArgs != null) {
			q.setMaxResults(pageArgs.getLimit() == 0 ? 100 : pageArgs.getLimit());
			q.setFirstResult(pageArgs.getPosition());
		}
		return em.createQuery(query, t).getResultList();
	}

	/**
	 * Same as {@link #query(EntityManager,String,Class,SearchArgs)} but uses a new entity manager.
	 *
	 * @param <T> The bean type.
	 * @param query The JPA query.
	 * @param t The bean type.
	 * @param searchArgs Optional search arguments.
	 * @return The results.
	 */
	protected <T> List<T> query(String query, Class<T> t, SearchArgs searchArgs, PageArgs pageArgs) {
		return query(getEntityManager(), query, t, searchArgs, pageArgs);
	}

	/**
	 * Runs a JPA parameterized query and returns the results.
	 *
	 * @param em The entity manager to use to retrieve the beans.
	 * @param query The JPA query.
	 * @param t The bean type.
	 * @param params The query parameter values.
	 * @return The results.
	 */
	protected <T> List<T> query(EntityManager em, String query, Class<T> t, Map<String,Object> params) {
		TypedQuery<T> tq = em.createQuery(query, t);
		for (Map.Entry<String,Object> e : params.entrySet()) {
			tq.setParameter(e.getKey(), e.getValue());
		}
		return tq.getResultList();
	}

	/**
	 * Same as {@link #query(EntityManager,String,Class,Map)} but uses a new entity manager.
	 *
	 * @param query The JPA query.
	 * @param t The bean type.
	 * @param params The query parameter values.
	 * @return The results.
	 */
	protected <T> List<T> query(String query, Class<T> t, Map<String,Object> params) {
		return query(getEntityManager(), query, t, params);
	}

	/**
	 * Runs a JPA update statement.
	 *
	 * @param em The entity manager to use to run the statement.
	 * @param query The JPA update statement.
	 * @return The number of rows modified.
	 */
	protected int update(EntityManager em, String query) {
		return em.createQuery(query).executeUpdate();
	}

	/**
	 * Same as {@link #update(EntityManager,String)} but uses a new entity manager.
	 *
	 * @param query The JPA update statement.
	 * @return The number of rows modified.
	 */
	protected int update(String query) {
		return update(getEntityManager(), query);
	}

	/**
	 * Runs a JPA parameterized update statement.
	 *
	 * @param em The entity manager to use to run the statement.
	 * @param query The JPA update statement.
	 * @param params The query parameter values.
	 * @return The number of rows modified.
	 */
	protected int update(EntityManager em, String query, Map<String,Object> params) {
		Query q = em.createQuery(query);
		for (Map.Entry<String,Object> e : params.entrySet()) {
			q.setParameter(e.getKey(), e.getValue());
		}
		return q.executeUpdate();
	}

	/**
	 * Same as {@link #update(EntityManager,String,Map)} but uses a new entity manager.
	 *
	 * @param query The JPA update statement.
	 * @param params The query parameter values.
	 * @return The number of rows modified.
	 */
	protected int update(String query, Map<String,Object> params) {
		return update(getEntityManager(), query, params);
	}
}
