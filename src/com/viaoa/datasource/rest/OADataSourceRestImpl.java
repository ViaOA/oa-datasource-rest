/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.datasource.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

/**
 * Server-side implementation of {@link OADataSourceRestInterface}.
 * <p>
 * Delegates REST calls received by {@code OARestServlet} to the appropriate
 * local {@link com.viaoa.datasource.OADataSource} instance. It maintains
 * lightweight server-side iterators for active client queries.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Delegate all CRUD and query operations to the local data source.</li>
 *   <li>Maintain active iterators for paged result delivery.</li>
 *   <li>Convert JSON identity strings to {@link com.viaoa.object.OAObjectKey}.</li>
 * </ul>
 *
 * @since OA 4.0
 */
public class OADataSourceRestImpl implements OADataSourceRestInterface {
	
	/**
	 * Holds the default {@link OADataSource} instance used when no
	 * class-specific data source can be resolved.
	 */
	private OADataSource defaultDataSource;

	private static Logger LOG = Logger.getLogger(OADataSourceRestImpl.class.getName());

	/**
	 * Returns whether the underlying data source assigns identifiers when
	 * creating new objects.
	 *
	 * @return {@code true} if ID assignment on create is supported;
	 *         otherwise {@code false}.
	 */
	@Override
	public boolean getAssignIdOnCreate() {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return false;
		}
		return ds.getAssignIdOnCreate();
	}

	/**
	 * Determines whether the underlying data source is currently available.
	 *
	 * @return {@code true} if the data source is available; otherwise {@code false}.
	 */
	@Override
	public boolean isAvailable() {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return false;
		}
		return ds.isAvailable();
	}

	/**
	 * Convenience accessor that returns a default data source.
	 *
	 * @return the resolved default {@link OADataSource} or {@code null} if none.
	 */
	protected OADataSource getDataSource() {
		return getDataSource(null);
	}

	/**
	 * Resolves a data source for the specified class. Attempts lookup using the
	 * class first; falls back to cached default resolution.
	 *
	 * @param c the class used to resolve a data source, or {@code null} for default resolution
	 * @return the resolved {@link OADataSource}, or {@code null} if not found
	 */
	protected OADataSource getDataSource(Class c) {
		if (c != null) {
			OADataSource ds = OARuntime.datasource().get(c);
			if (ds != null) {
				return ds;
			}
		}
		if (defaultDataSource == null) {
			for (OADataSource ds  : OARuntime.datasource().getAll()) {
				defaultDataSource = ds;
				break;
			}
		}
		return defaultDataSource;
	}

	/**
	 * Retrieves the maximum allowed length for a property defined on a class
	 * according to the underlying data source.
	 *
	 * @param clazz the class owning the property
	 * @param propertyName the name of the property
	 * @return the maximum allowed length, or {@code 0} if unavailable
	 */
	@Override
	public int getMaxLength(Class clazz, String propertyName) {
		OADataSource ds = getDataSource(clazz);
		if (ds == null) {
			return 0;
		}

		int x = ds.getMaxLength(clazz, propertyName);
		return x;
	}

	/**
	 * Determines whether the underlying data source supports operations for
	 * the specified class.
	 *
	 * @param clazz the class to check
	 * @return {@code true} if the class is supported; otherwise {@code false}
	 */
	@Override
	public boolean isClassSupported(Class clazz) {
		OADataSource ds = getDataSource(clazz);
		if (ds == null) {
			return false;
		}
		return ds != null;
	}

	/**
	 * Inserts the given object into the data source without processing any of
	 * its reference properties.
	 *
	 * @param obj the object to insert
	 */
	@Override
	public void insertWithoutReferences(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.insertWithoutReferences(obj);
	}

	/**
	 * Inserts the specified object into the underlying data source.
	 *
	 * @param obj the object to insert
	 */
	@Override
	public void insert(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.insert(obj);
	}

	/**
	 * Updates the given object using the underlying data source. The
	 * include/exclude property lists are ignored by this implementation.
	 *
	 * @param obj the object to update
	 * @param includeProperties properties to include (unused)
	 * @param excludeProperties properties to exclude (unused)
	 */
	@Override
	public void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.update(obj);
	}

	/**
	 * Persists the specified object using the underlying data source. Performs
	 * either an insert or update based on the object's state.
	 *
	 * @param obj the object to save
	 */
	@Override
	public void save(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.save(obj);
	}

	/**
	 * Deletes the specified object from the underlying data source.
	 *
	 * @param obj the object to delete
	 */
	@Override
	public void delete(OAObject obj) {
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return;
		}
		ds.delete(obj);
	}

	/**
	 * Deletes all instances of the given class from the underlying data source.
	 *
	 * @param c the class whose objects should be removed
	 */
	@Override
	public void deleteAll(Class c) {
		OADataSource ds = getDataSource(c);
		if (ds == null) {
			return;
		}
		ds.deleteAll(c);
	}

	/**
	 * Counts matching objects using the underlying data source. Does not
	 * currently resolve the optional where-object.
	 *
	 * @param selectClass the target class
	 * @param queryWhere the WHERE clause
	 * @param params optional query parameters
	 * @param whereObjectClass unused where-object class
	 * @param whereKey unused where-object key
	 * @param propertyFromWhereObject linked-property name
	 * @param extraWhere additional WHERE conditions
	 * @param max maximum count
	 * @return the number of matching objects
	 */
	@Override
	public int count(Class selectClass, String queryWhere, Object[] params, Class whereObjectClass, String whereKey,
			String propertyFromWhereObject, String extraWhere, int max) {
		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return 0;
		}

		//qqqqqqqq todo: get where object (if whereObjectClass != null)
		OAObject objWhere = null;

		int x = ds.count(selectClass, queryWhere, params, objWhere, propertyFromWhereObject, extraWhere, max);
		return x;
	}

	/**
	 * Executes a pass-through count operation on the underlying data source.
	 *
	 * @param selectClass the target class
	 * @param queryWhere the WHERE clause
	 * @param max maximum count
	 * @return the number of matching objects
	 */
	@Override
	public int countPassthru(Class selectClass, String queryWhere, int max) {
		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return 0;
		}
		int x = ds.countPassthru(selectClass, queryWhere, max);
		return x;
	}

	/**
	 * Indicates whether the underlying data source supports persistent storage.
	 *
	 * @return {@code true} if storage is supported; otherwise {@code false}
	 */
	@Override
	public boolean supportsStorage() {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return false;
		}
		return ds.supportsStorage();
	}

	/**
	 * Counter used to assign unique identifiers for active server-side select
	 * iterators.
	 */
	private final AtomicInteger aiSelect = new AtomicInteger();

	/**
	 * Registry of active server-side iterators, keyed by select identifier.
	 * Used to support paged retrieval of query results.
	 */
	private ConcurrentHashMap<Integer, Iterator> hashIterator = new ConcurrentHashMap<Integer, Iterator>(); // used to store DB

	/**
	 * Executes a select query through the underlying data source and registers
	 * the resulting iterator for paged retrieval.
	 *
	 * @param selectClass target class
	 * @param queryWhere WHERE clause
	 * @param params query parameters
	 * @param queryOrderBy ORDER BY expression
	 * @param whereObjectClass optional class used to resolve an object filter
	 * @param whereKey key used for resolving the where-object
	 * @param propertyFromWhereObject property linking the where-object
	 * @param extraWhere additional WHERE clause
	 * @param max maximum results
	 * @param bDirty whether dirty objects should be included
	 * @return the identifier assigned to the created iterator
	 */
	@Override
	public int select(Class selectClass, String queryWhere, Object[] params, String queryOrderBy, Class whereObjectClass, String whereKey,
			String propertyFromWhereObject, String extraWhere, int max, boolean bDirty) {

		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return -1;
		}

		OAObject objWhere = null;
		if (whereObjectClass != null && whereKey != null) {
			OAObjectKey ok = OAJson.convertJsonSinglePartIdToObjectKey(whereObjectClass, whereKey);

			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(whereObjectClass);
			
			objWhere = (OAObject) og.objectsInternal().callObjectCacheGet(whereObjectClass, ok);
			if (objWhere == null) {
				OADataSource dsx = OARuntime.datasource().get(whereObjectClass);
				if (dsx != null) {
					objWhere = (OAObject) dsx.getObject(whereObjectClass, ok);
				}
			}
		}

		OADataSourceIterator iterator = ds.select(selectClass, queryWhere, params, 
			queryOrderBy, objWhere, propertyFromWhereObject,
			extraWhere, max, null, bDirty);

		int selectId = aiSelect.incrementAndGet();

		hashIterator.put(selectId, iterator);
		LOG.finer("add iterator, size=" + hashIterator.size());

		return selectId;
	}

	/**
	 * Executes a pass-through select operation on the underlying data source
	 * and registers the resulting iterator for paged retrieval.
	 *
	 * @param selectClass the class to query
	 * @param queryWhere the WHERE clause
	 * @param queryOrder the ORDER BY clause
	 * @param max maximum number of results
	 * @param bDirty whether dirty objects should be included
	 * @return an identifier for the created iterator, or {@code -1} if unsupported
	 */
	@Override
	public int selectPassThru(Class selectClass, String queryWhere, String queryOrder, int max, boolean bDirty) {
		OADataSource ds = getDataSource(selectClass);
		if (ds == null) {
			return -1;
		}

		OADataSourceIterator iterator = ds.select(selectClass, queryWhere, queryOrder, max, bDirty);

		int selectId = aiSelect.incrementAndGet();

		hashIterator.put(selectId, iterator);
		LOG.finer("add iterator, size=" + hashIterator.size());

		return selectId;
	}

	/**
	 * Executes a command directly on the underlying data source.
	 *
	 * @param command the command text
	 * @return the result returned by the underlying data source, or {@code -1} if not available
	 */
	@Override
	public Object execute(String command) {
		OADataSource ds = getDataSource();
		if (ds == null) {
			return -1;
		}
		Object obj = ds.execute(command);
		return obj;
	}

	/**
	 * Assigns an identifier to the given object using the underlying data source.
	 * The clazz argument is ignored; the object's class is used instead.
	 *
	 * @param obj the object to assign an ID to
	 * @param clazz unused class parameter
	 * @return the same object instance
	 */
	@Override
	public OAObject assignId(OAObject obj, Class<? extends OAObject> clazz) {
		if (obj == null) {
			return obj;
		}
		OADataSource ds = getDataSource(obj.getClass());
		if (ds == null) {
			return obj;
		}
		ds.assignId(obj);
		return obj;
	}

	/**
	 * Placeholder indicating whether the underlying data source will create a
	 * value for the specified property. Not yet implemented.
	 *
	 * @param object the object being evaluated
	 * @param propertyName the property to check
	 * @return always {@code false}
	 */
	@Override
	public boolean willCreatePropertyValue(OAObject object, String propertyName) {
		return false;
	}

	/**
	 * Updates many-to-many link relationships for the specified master object.
	 * This implementation does not yet perform any updates.
	 *
	 * @param masterClass the class of the master object
	 * @param masterId identifier of the master object
	 * @param adds objects to add to the relationship
	 * @param addClazz class of added objects
	 * @param removes objects to remove from the relationship
	 * @param removeClazz class of removed objects
	 * @param propertyNameFromMaster the relationship property name
	 */
	@Override
	public void updateMany2ManyLinks(Class masterClass, String masterId, OAObject[] adds, Class addClazz, OAObject[] removes,
			Class removeClazz, String propertyNameFromMaster) {
	}

	/**
	 * Retrieves up to 500 next elements from the iterator associated with the
	 * specified select identifier. Automatically removes the iterator if
	 * exhausted.
	 *
	 * @param selectId the identifier of the active iterator
	 * @param clazz unused parameter indicating expected class type
	 * @return an array of retrieved objects, or {@code null} if no iterator exists
	 */
	@Override
	public OAObject[] next(int selectId, Class clazz) {
		Iterator iterator = (Iterator) hashIterator.get(selectId);
		if (iterator == null) {
			return null;
		}

		ArrayList<Object> al = new ArrayList();
		for (int i = 0; i < 500; i++) {
			if (!iterator.hasNext()) {
				break;
			}
			Object obj = iterator.next();
			al.add(obj);
			/*
			if (obj instanceof OAObject) {
				OAObject oa = (OAObject) obj;
				this.setCached(oa);
			}
			*/
		}
		int x = al.size();
		if (x == 0) {
			removeSelect(selectId);
		}
		OAObject[] objs = new OAObject[x];
		if (x > 0) {
			al.toArray(objs);
		}
		return objs;
	}

	/**
	 * Removes the iterator associated with the given select identifier and
	 * performs a final call to {@link Iterator#remove()} on the iterator.
	 *
	 * @param selectId the identifier of the iterator to remove
	 */
	@Override
	public void removeSelect(int selectId) {
		Iterator iterator = (Iterator) hashIterator.get(selectId);
		if (iterator == null) {
			return;
		}
		iterator.remove();
		hashIterator.remove(selectId);
		LOG.finer("remove iterator, size=" + hashIterator.size());
	}

}
