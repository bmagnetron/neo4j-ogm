/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.persistence.examples.locking;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.domain.locking.Location;
import org.neo4j.ogm.domain.locking.PowerUser;
import org.neo4j.ogm.domain.locking.User;
import org.neo4j.ogm.exception.OptimisticLockingException;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;

/**
 * @author Frantisek Hartman
 */
public class NodeOptimisticLockingTest extends MultiDriverTestClass {

    private static SessionFactory sessionFactory;

    private Session session;

    @BeforeClass
    public static void setUpClass() {
        sessionFactory = new SessionFactory(driver, "org.neo4j.ogm.domain.locking");
    }

    @Before
    public void setUp() throws Exception {
        session = sessionFactory.openSession();
        session.purgeDatabase();
    }

    @Test
    public void whenSaveNewNodeThenSetVersionToZero() {
        User frantisek = new User("Frantisek");

        // version set in the entity
        session.save(frantisek);
        assertThat(frantisek.getVersion()).isEqualTo(0L);

        // version set in the graph
        session.clear();
        User loaded = session.load(User.class, frantisek.getId());
        assertThat(loaded.getVersion()).isEqualTo(0L);
    }

    @Test
    public void givenNodeWhenUpdateNodeThenIncrementVersion() {
        User frantisek = new User("Frantisek");

        session.save(frantisek);
        frantisek.setName("Frantisek Hartman");
        session.save(frantisek);

        // version updated
        assertThat(frantisek.getVersion()).isEqualTo(1L);

        session.clear();
        User loaded = session.load(User.class, frantisek.getId());
        assertThat(loaded.getName()).isEqualTo("Frantisek Hartman");
        assertThat(loaded.getVersion()).isEqualTo(1L);
    }

    @Test
    public void givenNodeWithWrongVersionWhenSaveNodeThenFailWithOptimisticLockingException() {
        User frantisek = new User("Frantisek");
        session.save(frantisek);
        frantisek.setName("Frantisek Hartman");
        session.save(frantisek);

        User wrongVersion = new User("Frantisek");
        wrongVersion.setId(frantisek.getId());
        wrongVersion.setVersion(0L);

        assertThatThrownBy(() -> session.save(wrongVersion))
            .isInstanceOf(OptimisticLockingException.class)
            .hasMessageContaining("Entity with type='[User]'")
            .hasMessageContaining("id='" + frantisek.getId() + "' had incorrect version 0");
    }

    @Test
    public void saveOnNonExistingEntityShouldFailWithOptimisticLockingException() {
        User frantisek = new User("Frantisek");
        session.save(frantisek);

        // someone else deletes node
        session.delete(frantisek);

        frantisek.setName("Frantisek Hartman");

        // save should throw exception
        assertThatThrownBy(() -> session.save(frantisek))
            .isInstanceOf(OptimisticLockingException.class);

        // and node should not exist
        Collection<User> users = session.loadAll(User.class);
        assertThat(users).isEmpty();
    }

    @Test
    public void givenNodeWhenDeleteThenNodeIsDeleted() {
        // This is normal delete, but should still work with optimistic locking
        User frantisek = new User("Frantisek");
        session.save(frantisek);

        session.delete(frantisek);

        Collection<User> users = session.loadAll(User.class);
        assertThat(users).isEmpty();
    }

    @Test
    public void givenNodeWithWrongVersionWhenDeleteThenThrowOptimisticLockingException() {
        User frantisek = new User("Frantisek");
        session.save(frantisek);

        frantisek.setVersion(1L);

        assertThatThrownBy(() -> session.delete(frantisek))
            .isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    public void shouldWorkWithInheritedVersionField() {
        PowerUser frantisek = new PowerUser("Frantisek");
        session.save(frantisek);

        assertThat(frantisek.getVersion()).isEqualTo(0L);

        frantisek.setName("Frantisek Hartman");
        session.save(frantisek);

        assertThat(frantisek.getVersion()).isEqualTo(1L);
    }

    @Test
    public void shouldWorkWithCustomVersionFieldName() {
        Location london = new Location("London");
        session.save(london);

        assertThat(london.getCustomVersion()).isEqualTo(0L);

        london.setName("Greater London");
        session.save(london);

        assertThat(london.getCustomVersion()).isEqualTo(1L);
    }

}
