/*
 * Copyright 2018 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm;

import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.Collections;

import io.realm.entities.AllJavaTypes;
import io.realm.entities.AllTypes;
import io.realm.entities.OsCreateTestObject;
import io.realm.entities.PrimaryKeyAsLong;
import io.realm.internal.ColumnInfo;
import io.realm.internal.OsObject;
import io.realm.internal.RealmObjectProxy;
import io.realm.internal.Row;
import io.realm.internal.Table;
import io.realm.internal.objectstore.OsObjectBuilder;
import io.realm.rule.TestRealmConfigurationFactory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Ideally all of the functionality is tested through the code generated by the annotation processor
 * So these tests are mostly here to isolate the behaviour from the more complex proxy classes.
 */
@RunWith(AndroidJUnit4.class)
public class OsObjectBuilderTests {
    @Rule
    public final TestRealmConfigurationFactory configFactory = new TestRealmConfigurationFactory();
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private Realm realm;

    @Before
    public void setUp() {
        realm = Realm.getInstance(configFactory.createConfiguration());
    }

    @After
    public void tearDown() {
        if (realm != null && !realm.isClosed()) {
            realm.close();
        }
    }

    @Test
    public void createOrUpdate_newObject() {
        realm.beginTransaction();

        Table table = realm.getTable(OsCreateTestObject.class);
        OsObjectBuilder builder = new OsObjectBuilder(table);
        builder.addInteger(OsCreateTestObject.FIELD_ID, 1);
        builder.addString(OsCreateTestObject.FIELD_STRING, "foo");
        OsCreateTestObject obj = realm.createObject(OsCreateTestObject.class, 42);
        builder.addObject(OsCreateTestObject.FIELD_OBJECT, (RealmObjectProxy) obj);
        Row row = builder.createNewObject();

        OsCreateTestObject managedObject = convertToManagedObject(OsCreateTestObject.class, realm, row);
        assertEquals(1, managedObject.getFieldId());
        assertEquals("foo", managedObject.getFieldString());
        assertEquals(obj, managedObject.getFieldObject());
    }

    @Test
    public void createOrUpdate_updateExistingObject() {

    }

    @Test
    public void createOrUpdate_updateTypeWithNoPrimaryKeyThrows() {

    }

    @Test
    public void createOrUpdate_updateObjectWithNoPrimaryKeyValueThrows() {

    }

    private <T extends RealmModel> T convertToManagedObject(Class<T> clazz, Realm realm, Row row) {
        // Prepare ObjectContext before creating the Proxy
        final BaseRealm.RealmObjectContext objectContext = BaseRealm.objectContext.get();
        ColumnInfo columnInfo = realm.getSchema().getColumnInfo(clazz);
        objectContext.set(realm, row, columnInfo, false, Collections.emptyList());

        // Instantiate the proxy class using the prepared Object Context
        String proxyClassName = clazz.getCanonicalName().replace(".", "_") + "RealmProxy";
        try {
            Class<?> proxyClass = Class.forName("io.realm." + proxyClassName);
            Constructor<?> constructor = proxyClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            //noinspection unchecked
            return (T) constructor.newInstance();
        } catch (Exception e) {
            fail("Could not create proxy class: " + e.toString());
        }

        throw new IllegalStateException();
    }
}