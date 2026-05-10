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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import com.viaoa.converter.OAConv;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.datasource.objectcache.ObjectCacheIterator;
import com.viaoa.filter.OAFilter;
import com.viaoa.graph.OAGraph;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.object.OAObjectCacheService;
import com.viaoa.graph.service.object.OAObjectInfoService;
import com.viaoa.graph.sibling.OASiblingHelper;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;

/**
 * REST-based {@link com.viaoa.datasource.OADataSource} implementation for OA clients.
 * <p>
 * {@code OADataSourceRestClient} enables any OA application to access a remote
 * OADataSource through JSON/HTTP using an {@link OADataSourceRestInterface}.
 * It fully supports CRUD operations, queries, and iterative result streaming.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Transparent remote CRUD and query operations over REST.</li>
 *   <li>JSON serialization via {@link com.viaoa.json.OAJson}.</li>
 *   <li>Fallback to local {@link com.viaoa.datasource.objectcache.ObjectCacheIterator}
 *       when applicable.</li>
 *   <li>Supports {@link com.viaoa.filter.OAFilter} for client-side filtering.</li>
 * </ul>
 *
 */
public class OADataSourceRestClient extends OADataSource {
	
	/**
	 * Cache of class-support results returned by the remote API.
	 * Maps a Class to a Boolean indicating whether the remote data source
	 * supports storage/query operations for that class.
	 */
	private Hashtable hashClass = new Hashtable();
	
	/**
	 * Reference to the remote REST API used for all CRUD, query,
	 * and metadata operations.
	 */
	private OADataSourceRestInterface restAPI;

	/**
	 * Constructs a new REST-based data source client.
	 *
	 * If the provided package is null, defaults to {@link OASync#ObjectPackage}.
	 * Stores the REST interface used for remote communication.
	 *
	 * @param packagex the package that identifies the object namespace, or null to use the default
	 * @param restAPI the remote REST interface used for all server communication
	 */
	public OADataSourceRestClient(Package packagex, OADataSourceRestInterface restAPI) {
		if (packagex == null) {
			// qqqqqq ?? packagex = OASync.ObjectPackage;
		}
		this.restAPI = restAPI;
	}

	/**
	 * Returns the remote REST API interface used for server communication.
	 *
	 * @return the configured {@link OADataSourceRestInterface}
	 */
	public OADataSourceRestInterface getRestAPI() {
		return restAPI;
	}

	/**
	 * Tracks whether the assign-ID-on-create setting was explicitly requested locally.
	 */
	private boolean bCalledGetAssignIdOnCreate;

	/**
	 * Locally cached result of whether IDs should be assigned on object creation.
	 */
	private boolean bGetAssignIdOnCreate;

	/**
	 * Explicitly sets whether the client should assign IDs when creating objects.
	 *
	 * @param b true to assign IDs locally, false otherwise
	 */
	public void setAssignIdOnCreate(boolean b) {
		bCalledGetAssignIdOnCreate = true;
		bGetAssignIdOnCreate = b;
	}

	/**
	 * Returns whether IDs should be assigned upon creation.
	 * If not previously cached, queries the remote API and caches the result.
	 *
	 * @return true if ID assignment on create is enabled, false otherwise
	 */
	@Override
	public boolean getAssignIdOnCreate() {
		if (bCalledGetAssignIdOnCreate) {
			return bGetAssignIdOnCreate;
		}
		verifyConnection();
		bGetAssignIdOnCreate = getRestAPI().getAssignIdOnCreate();
		bCalledGetAssignIdOnCreate = true;
		return bGetAssignIdOnCreate;
	}

	/**
	 * Determines whether the remote data source is reachable and operational.
	 *
	 * @return true if the remote source reports itself as available
	 */
	@Override
	public boolean isAvailable() {
		verifyConnection();
		return getRestAPI().isAvailable();
	}

	/**
	 * Cache that stores maximum lengths of properties by generated key
	 * "<className>-<propertyName>". Prevents repeated remote lookups.
	 */
	private final Map<String, Integer> hmMax = new HashMap<String, Integer>();

	/**
	 * Returns the maximum length of a property. Uses a cache keyed by
	 * class and property name; if not available, queries the remote API.
	 *
	 * @param c the class declaring the property
	 * @param propertyName the name of the property
	 * @return the maximum allowed length of the property
	 */
	@Override
	public int getMaxLength(Class c, String propertyName) {
		String key = (c.getName() + "-" + propertyName).toUpperCase();
		Integer ix = hmMax.get(key);
		if (ix != null) {
			return ix.intValue();
		}

		int iResult;
		verifyConnection();
		int max = getRestAPI().getMaxLength(c, propertyName);
		hmMax.put(key, max);
		return max;
	}

	/**
	 * Manually sets and caches a maximum property length value.
	 *
	 * @param c the declaring class
	 * @param propertyName the property name
	 * @param length the maximum allowed length
	 */
	public void setMaxLength(Class c, String propertyName, int length) {
		if (c == null || propertyName == null) {
			return;
		}
		String key = (c.getName() + "-" + propertyName).toUpperCase();
		hmMax.put(key, Integer.valueOf(length));
	}

	/**
	 * Ensures that a REST API connection is available.
	 * Throws a RuntimeException if no remote interface is configured.
	 */
	protected void verifyConnection() {
		if (getRestAPI() == null) {
			throw new RuntimeException("OADataSourceClient connection is not set");
		}
	}

	//NOTE: this needs to see if any of "clazz" superclasses are supported
	/**
	 * Determines whether the remote data source supports the given class.
	 * Uses a local cache; may check local SelectAll hubs when a filter
	 * is provided, otherwise delegates to the remote API.
	 *
	 * @param clazz the class to test
	 * @param filter optional filter used to allow local fallback
	 * @return true if the class is supported
	 */
	@Override
	public boolean isClassSupported(Class clazz, OAFilter filter) {
		if (clazz == null) {
			return false;
		}

		Boolean B = (Boolean) hashClass.get(clazz);
		if (B != null) {
			return B.booleanValue();
		}

		if (filter != null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
			if (og.objectsInternal().callObjectCacheGetSelectAllHub(clazz) != null) {
				return true;
			}
		}

		verifyConnection();
		boolean b = getRestAPI().isClassSupported(clazz);

		hashClass.put(clazz, b);
		return b;
	}

	/**
	 * Inserts an object without processing its references.
	 * Delegates to the remote API, then resets object state flags.
	 *
	 * @param obj the object to insert
	 */
	@Override
	public void insertWithoutReferences(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().insertWithoutReferences(obj);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	/**
	 * Inserts the object on the remote data source.
	 * After the operation, clears new/deleted/changed state flags.
	 *
	 * @param obj the object to insert
	 */
	@Override
	public void insert(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().insert(obj);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	/**
	 * Updates an object on the remote data source with optional include/
	 * exclude property lists. Resets state flags after remote update.
	 *
	 * @param obj the object to update
	 * @param includeProperties list of properties to include, or null
	 * @param excludeProperties list of properties to exclude, or null
	 */
	@Override
	public void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
		if (obj == null) {
			return;
		}
		getRestAPI().update(obj, includeProperties, excludeProperties);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	/**
	 * Saves an object on the remote data source.
	 * After saving, resets new/deleted/changed state flags.
	 *
	 * @param obj the object to save
	 */
	@Override
	public void save(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().save(obj);
		obj.setNew(false);
		obj.setDeleted(false);
		obj.setChanged(false);
	}

	/**
	 * Deletes an object on the remote data source.
	 * Marks the object as deleted and clears other state flags.
	 *
	 * @param obj the object to delete
	 */
	@Override
	public void delete(OAObject obj) {
		if (obj == null) {
			return;
		}
		getRestAPI().delete(obj);
		obj.setNew(false);
		obj.setDeleted(true);
		obj.setChanged(false);
	}

	/**
	 * Deletes all instances of the given class on the remote data source.
	 *
	 * @param c the class whose objects should be removed
	 */
	@Override
	public void deleteAll(Class c) {
		if (c == null) {
			return;
		}
		getRestAPI().deleteAll(c);
	}

	/**
	 * Counts objects on the remote data source matching the specified criteria.
	 * Converts a where-object to its JSON single-part ID before delegating.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere the query where-clause
	 * @param params parameter values for the where-clause
	 * @param whereObject optional object used to derive a where-ID
	 * @param propertyFromWhereObject property name relating whereObject to selectClass
	 * @param extraWhere additional where-clause text
	 * @param max maximum count limit
	 * @return the resulting count of matching objects
	 */
	@Override
	public int count(Class selectClass,
			String queryWhere, Object[] params,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max) {
		Class whereClass = null;
		String whereId = null;
		if (whereObject != null) {
			whereId = OAJson.convertObjectKeyToJsonSinglePartId(whereObject.getObjectKey());
			whereClass = whereObject.getClass();
		}

		int cnt = getRestAPI().count(selectClass, queryWhere, params, whereClass, whereId, propertyFromWhereObject, extraWhere, max);
		return cnt;
	}

	/**
	 * Performs a passthru count directly on the remote data source.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere the where-clause to apply
	 * @param max an upper bound on the count
	 * @return the number of matching objects
	 */
	@Override
	public int countPassthru(Class selectClass, String queryWhere, int max) {
		int cnt = getRestAPI().countPassthru(selectClass, queryWhere, max);
		return cnt;
	}

	/**
	 * Tracks whether support for storage was previously queried.
	 */
	private boolean bCalledSupportsStorage;

	/**
	 * Cached result indicating whether the remote data source
	 * supports storage operations.
	 */
	private boolean bSupportsStorage;

	/**
	 * Returns whether the remote data source supports storage operations.
	 * If not previously queried, calls the remote API and caches the result.
	 *
	 * @return true if storage is supported
	 */
	@Override
	public boolean supportsStorage() {
		if (bCalledSupportsStorage) {
			return bSupportsStorage;
		}

		bSupportsStorage = getRestAPI().supportsStorage();
		bCalledSupportsStorage = true;
		return bSupportsStorage;
	}

	/**
	 * Selects objects matching the given criteria. If a filter is supplied
	 * and a local SelectAll hub exists, returns a local cache iterator.
	 * Otherwise performs a remote select, returning a MyIterator instance.
	 *
	 * @param selectClass the class to query
	 * @param queryWhere the where-clause
	 * @param params parameter values
	 * @param queryOrder ordering expression
	 * @param whereObject optional reference object used to compute a single-part ID
	 * @param propertyFromWhereObject property name describing the link from whereObject
	 * @param extraWhere additional where-clause text
	 * @param max maximum number of objects to return
	 * @param filter optional filter to apply locally
	 * @param bDirty whether to include dirty objects
	 * @return an iterator over the selected objects
	 */
	@Override
	public OADataSourceIterator select(Class selectClass,
			String queryWhere, Object[] params, String queryOrder,
			OAObject whereObject, String propertyFromWhereObject, String extraWhere,
			int max, OAFilter filter, boolean bDirty) {

		if (filter != null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(selectClass);
			if (og.objectsInternal().callObjectCacheGetSelectAllHub(selectClass) != null) {
				ObjectCacheIterator it = new ObjectCacheIterator(selectClass, filter);
				it.setMax(max);
				return it;
			}
		}

		Class whereClass = null;
		String whereId = null;
		if (whereObject != null) {
			whereId = OAJson.convertObjectKeyToJsonSinglePartId(whereObject.getObjectKey());
			whereClass = whereObject.getClass();
		}

		int selectId = getRestAPI().select(	selectClass,
											queryWhere, params, queryOrder,
											whereClass, whereId,
											propertyFromWhereObject, extraWhere, max, bDirty);

		return new MyIterator(selectClass, selectId, filter);
	}

	/**
	 * Executes a passthru select operation on the remote data source.
	 * Uses a local cache iterator when a filter and SelectAll hub exist,
	 * otherwise returns a MyIterator tied to a remote select operation.
	 *
	 * @param selectClass the class to query
	 * @param queryWhere the where-clause
	 * @param queryOrder ordering expression
	 * @param max maximum number of objects to return
	 * @param filter optional local filter
	 * @param bDirty whether to include dirty objects
	 * @return an iterator over the selected objects
	 */
	@Override
	public OADataSourceIterator selectPassthru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, OAFilter filter, boolean bDirty) {
		if (filter != null) {
			final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(selectClass);
			if (og.objectsInternal().callObjectCacheGetSelectAllHub(selectClass) != null) {
				ObjectCacheIterator it = new ObjectCacheIterator(selectClass, filter);
				it.setMax(max);
				return it;
			}
		}
		int selectId = getRestAPI().selectPassThru(selectClass, queryWhere, queryOrder, max, bDirty);
		return new MyIterator(selectClass, selectId, filter);
	}

	/**
	 * Executes an arbitrary command on the remote data source.
	 *
	 * @param command the command string
	 * @return the remote execution result
	 */
	@Override
	public Object execute(String command) {
		return getRestAPI().execute(command);
	}

	/**
	 * Requests that the remote data source assign an ID for the object.
	 * If the remote call returns a populated object, copies key values
	 * into the supplied object based on its key property order.
	 *
	 * @param obj the object needing an ID
	 */
	@Override
	public void assignId(OAObject obj) {
		if (obj == null) {
			return;
		}
		OAObject objx = getRestAPI().assignId(obj, obj.getClass());

		if (objx == null) {
			return;
		}

		OAObjectKey okx = objx.getObjectKey();

		final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(obj);
    	OAObjectInfo oi = og.objectsInternal().callObjectInfoGetOAObjectInfo(obj.getClass());

		Object[] ids = okx.getObjectIds();

		int cnt = -1;
		for (String id : oi.getKeyProperties()) {
			cnt++;
			if (cnt == ids.length) {
				break;
			}
			obj.setProperty(id, ids[cnt]);
		}
	}

	/**
	 * Determines whether a property value will be created for the given object.
	 * Delegates to the remote API and converts the result to a boolean.
	 *
	 * @param object the target object
	 * @param propertyName the property name being tested
	 * @return true if the property value will be created
	 */
	@Override
	public boolean willCreatePropertyValue(OAObject object, String propertyName) {
		Object obj = getRestAPI().willCreatePropertyValue(object, propertyName);
		boolean b = OAConv.toBoolean(obj);
		return b;
	}

	/**
	 * Iterator used to stream remote select results. Wraps a server-side
	 * select session identified by selectId and fetches objects in batches.
	 */
	class MyIterator implements OADataSourceIterator {
		/**
		 * The remote select session identifier returned from the server.
		 */
		final int selectId;

		/**
		 * The class of objects being iterated.
		 */
		final Class clazz;
		
		/**
		 * Optional key used when the iterator should return a specific object.
		 * When set, the next() method attempts to resolve the key locally.
		 */
		OAObjectKey key; // object to return
		
		/**
		 * Indicates whether the iterator should return the object referenced
		 * by the key field instead of reading from the cache buffer.
		 */
		boolean bKey;
		
		/**
		 * Batched results retrieved from the remote server. Each element is an object
		 * instance until a null entry is encountered, signaling the end of batch.
		 */
		OAObject[] cache;
		
		/**
		 * The current read position within the result cache array.
		 */
		int cachePos = 0;
		
		/**
		 * Optional filter used to evaluate whether cached results should be returned.
		 */
		OAFilter filter;
		
		/**
		 * Helper used for sibling navigation and read-ahead support during iteration.
		 */
		private OASiblingHelper siblingHelper;
		
		/**
		 * Hub used to store objects loaded during read-ahead, enabling
		 * sibling-based navigation across retrieved objects.
		 */
		private Hub<OAObject> hubReadAhead;

		/**
		 * Constructs the iterator for a remote select session. Initializes fields
		 * and immediately retrieves the first batch of results.
		 *
		 * @param c the class of objects being iterated
		 * @param selectId the remote select session identifier
		 * @param filter optional filter applied during iteration
		 */
		public MyIterator(Class c, int selectId, OAFilter filter) {
			this.clazz = c;
			this.selectId = selectId;
			this.filter = filter;
			getMoreFromServer();
		}

		/**
		 * Returns the sibling helper used to navigate between related objects.
		 *
		 * @return the sibling helper instance, or null if none initialized
		 */
		@Override
		public OASiblingHelper getSiblingHelper() {
			return siblingHelper;
		}

		/**
		 * Determines whether another object is available. Checks key mode,
		 * cached results, and fetches additional batches from the server
		 * as needed.
		 *
		 * @return true if another object can be returned
		 */
		public synchronized boolean hasNext() {
			if (key != null) {
				return (bKey);
			}

			for (;;) {
				if (cache == null) {
					break;
				}
				for (; cachePos < cache.length; cachePos++) {
					if (cache[cachePos] == null) {
						return false;
					}
					if (filter == null || filter.isUsed(cache[cachePos])) {
						return true;
					}
				}
				getMoreFromServer();
			}

			return false;
		}

		/**
		 * Retrieves the next batch of results from the remote server. Initializes
		 * sibling helper and hub on first use and populates the read-ahead hub.
		 */
		protected synchronized void getMoreFromServer() {
			cachePos = 0;
			cache = (OAObject[]) getRestAPI().next(selectId, clazz);
			if (cache == null || cache.length == 0) {
				cache = null;
				close();
				return;
			}
			if (siblingHelper == null) {
				this.hubReadAhead = new Hub<>();
				siblingHelper = new OASiblingHelper(this.hubReadAhead);
			}

			for (OAObject obj : cache) {
				if (obj == null) {
					break;
				}
				// the server will add the object to the session cache (server side) if it is not in a hub w/master
				/* qqq ??
				if (OAObjectHubDelegate.isInHubWithMaster((OAObject) obj)) {
					OAObjectCSDelegate.removeFromServerSideCache((OAObject) obj);
				}
				*/
				hubReadAhead.add(obj);
			}
		}

		/**
		 * Returns the next object from the iterator. Resolves key-based requests
		 * locally if key mode is active; otherwise returns the next cached object.
		 *
		 * @return the next available object, or null if none
		 */
		public synchronized Object next() {
			if (!hasNext()) {
				return null;
			}
			Object obj = null;
			if (key != null) {
				final OAGraphInternal og = (OAGraphInternal) OARuntime.graph(clazz);
				obj = og.objectsInternal().callObjectCacheGet(clazz, key);
				if (obj == null) {
					// not on this system, need to get from server
					//qqqqqqq todo:
					//qqqqqqq  OASyncDelegate.getRemoteServer(packagex).getObject(clazz, key);
				}
				bKey = false;
				return obj;
			}
			obj = cache[cachePos++];

			return obj;
		}

		/**
		 * Cancels the remote select session and closes iterator resources.
		 */
		public void remove() {
			getRestAPI().removeSelect(selectId);
			close();
		}

		/**
		 * Placeholder implementation. Always returns null.
		 *
		 * @return null
		 */
		@Override
		public String getQuery() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * Placeholder implementation. Always returns null.
		 *
		 * @return null
		 */
		@Override
		public String getQuery2() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * Clears internal resources such as the read-ahead hub and sibling helper.
		 */
		public void close() {
			if (hubReadAhead != null) {
				hubReadAhead.clear();
				hubReadAhead = null;
			}
			if (siblingHelper != null) {
				siblingHelper = null;
			}
		}

		/**
		 * Ensures that resources are released before garbage collection.
		 * Calls close() after superclass finalization.
		 *
		 * @throws Throwable if an exception occurs during finalization
		 */
		public void finalize() throws Throwable {
			super.finalize();
			close();
		}

	}

	/**
	 * Updates many-to-many link relationships on the remote data source.
	 * Converts master/add/remove objects to their respective classes and
	 * single-part IDs before delegating to the remote API.
	 *
	 * @param masterObject the master object whose links are being modified
	 * @param adds objects to add to the relationship
	 * @param removes objects to remove from the relationship
	 * @param propertyNameFromMaster the property name on the master representing the relationship
	 */
	@Override
	public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propertyNameFromMaster) {
		Class masterClass = null;
		String masterId = null;
		if (masterObject != null) {
			masterId = OAJson.convertObjectKeyToJsonSinglePartId(masterObject.getObjectKey());
			masterClass = masterObject.getClass();
		}

		Class addClass = null;
		if (adds != null && adds.length > 0) {
			addClass = adds[0].getClass();
		}

		Class removeClass = null;
		if (removes != null && removes.length > 0) {
			removeClass = removes[0].getClass();
		}

		getRestAPI().updateMany2ManyLinks(masterClass, masterId, adds, addClass, removes, removeClass, propertyNameFromMaster);
	}

	/**
	 * Retrieves the BLOB value for the specified property.
	 * Not implemented in this class; always throws a RuntimeException.
	 *
	 * @param obj the object whose property is requested
	 * @param propertyName the BLOB property name
	 * @return never returns normally
	 * @throws RuntimeException always thrown to indicate unsupported operation
	 */
	@Override
	public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
		throw new RuntimeException("not yet implemented");
	}

	/**
	 * Identifies this data source as a client-side implementation.
	 *
	 * @return true always
	 */
	@Override
	public boolean isClient() {
		return true;
	}
}
