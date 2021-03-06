package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OfflineClientSessionModel;
import org.keycloak.models.OfflineUserSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.services.managers.ClientManager;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractModelTest {

    @Test
    public void persistUser() {
        RealmModel realm = realmManager.createRealm("original");
        KeycloakSession session = realmManager.getSession();
        UserModel user = session.users().addUser(realm, "user");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");
        assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

        RealmModel searchRealm = realmManager.getRealm(realm.getId());
        UserModel persisted = session.users().getUserByUsername("user", searchRealm);

        assertEquals(user, persisted);

        searchRealm = realmManager.getRealm(realm.getId());
        UserModel persisted2 =  session.users().getUserById(user.getId(), searchRealm);
        assertEquals(user, persisted2);

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(UserModel.LAST_NAME, "last-name");
        List<UserModel> search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");

        attributes.clear();
        attributes.put(UserModel.EMAIL, "email");
        search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");

        attributes.clear();
        attributes.put(UserModel.LAST_NAME, "last-name");
        attributes.put(UserModel.EMAIL, "email");
        search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");
    }
    
    @Test
    public void webOriginSetTest() {
        RealmModel realm = realmManager.createRealm("original");
        ClientModel client = realm.addClient("user");

        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client.addWebOrigin("origin-1");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.addWebOrigin("origin-2");
        Assert.assertEquals(2, client.getWebOrigins().size());

        client.removeWebOrigin("origin-2");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.removeWebOrigin("origin-1");
        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client = realm.addClient("oauthclient2");

        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client.addWebOrigin("origin-1");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.addWebOrigin("origin-2");
        Assert.assertEquals(2, client.getWebOrigins().size());

        client.removeWebOrigin("origin-2");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.removeWebOrigin("origin-1");
        Assert.assertTrue(client.getWebOrigins().isEmpty());

    }

    @Test
    public void testUserRequiredActions() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = session.users().addUser(realm, "user");

        Assert.assertTrue(user.getRequiredActions().isEmpty());

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        String id = realm.getId();
        commit();
        realm = realmManager.getRealm(id);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));

        user.addRequiredAction(RequiredAction.VERIFY_EMAIL.name());
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(2, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP.name());
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        user.removeRequiredAction(RequiredAction.VERIFY_EMAIL.name());
        user = session.users().getUserByUsername("user", realm);

        Assert.assertTrue(user.getRequiredActions().isEmpty());
    }

    @Test
    public void testUserMultipleAttributes() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = session.users().addUser(realm, "user");
        UserModel userNoAttrs = session.users().addUser(realm, "user-noattrs");

        user.setSingleAttribute("key1", "value1");
        List<String> attrVals = new ArrayList<>(Arrays.asList( "val21", "val22" ));
        user.setAttribute("key2", attrVals);

        commit();

        // Test read attributes
        realm = realmManager.getRealmByName("original");
        user = session.users().getUserByUsername("user", realm);

        attrVals = user.getAttribute("key1");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("value1", attrVals.get(0));
        Assert.assertEquals("value1", user.getFirstAttribute("key1"));

        attrVals = user.getAttribute("key2");
        Assert.assertEquals(2, attrVals.size());
        Assert.assertTrue(attrVals.contains("val21"));
        Assert.assertTrue(attrVals.contains("val22"));

        attrVals = user.getAttribute("key3");
        Assert.assertTrue(attrVals.isEmpty());
        Assert.assertNull(user.getFirstAttribute("key3"));

        Map<String, List<String>> allAttrVals = user.getAttributes();
        Assert.assertEquals(2, allAttrVals.size());
        Assert.assertEquals(allAttrVals.get("key1"), user.getAttribute("key1"));
        Assert.assertEquals(allAttrVals.get("key2"), user.getAttribute("key2"));

        // Test remove and rewrite attribute
        user.removeAttribute("key1");
        user.setSingleAttribute("key2", "val23");

        commit();

        realm = realmManager.getRealmByName("original");
        user = session.users().getUserByUsername("user", realm);
        Assert.assertNull(user.getFirstAttribute("key1"));
        attrVals = user.getAttribute("key2");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("val23", attrVals.get(0));
    }

    @Test
    public void testSearchByUserAttribute() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user1 = session.users().addUser(realm, "user1");
        UserModel user2 = session.users().addUser(realm, "user2");
        UserModel user3 = session.users().addUser(realm, "user3");

        user1.setSingleAttribute("key1", "value1");
        user1.setSingleAttribute("key2", "value21");

        user2.setSingleAttribute("key1", "value1");
        user2.setSingleAttribute("key2", "value22");

        user3.setSingleAttribute("key2", "value21");

        commit();

        List<UserModel> users = session.users().searchForUserByUserAttribute("key1", "value1", realm);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.contains(user1));
        Assert.assertTrue(users.contains(user2));

        users = session.users().searchForUserByUserAttribute("key2", "value21", realm);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.contains(user1));
        Assert.assertTrue(users.contains(user3));

        users = session.users().searchForUserByUserAttribute("key2", "value22", realm);
        Assert.assertEquals(1, users.size());
        Assert.assertTrue(users.contains(user2));

        users = session.users().searchForUserByUserAttribute("key3", "value3", realm);
        Assert.assertEquals(0, users.size());
    }

    @Test
    public void testServiceAccountLink() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        ClientModel client = realm.addClient("foo");

        UserModel user1 = session.users().addUser(realm, "user1");
        user1.setFirstName("John");
        user1.setLastName("Doe");

        UserModel user2 = session.users().addUser(realm, "user2");
        user2.setFirstName("John");
        user2.setLastName("Doe");

        // Search
        Assert.assertNull(session.users().getUserByServiceAccountClient(client));
        List<UserModel> users = session.users().searchForUser("John Doe", realm);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.contains(user1));
        Assert.assertTrue(users.contains(user2));

        // Link service account
        user1.setServiceAccountClientLink(client.getId());

        commit();

        // Search and assert service account user not found
        realm = realmManager.getRealmByName("original");
        UserModel searched = session.users().getUserByServiceAccountClient(client);
        Assert.assertEquals(searched, user1);
        users = session.users().searchForUser("John Doe", realm);
        Assert.assertEquals(1, users.size());
        Assert.assertFalse(users.contains(user1));
        Assert.assertTrue(users.contains(user2));

        users = session.users().getUsers(realm, false);
        Assert.assertEquals(1, users.size());
        Assert.assertFalse(users.contains(user1));
        Assert.assertTrue(users.contains(user2));

        users = session.users().getUsers(realm, true);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.contains(user1));
        Assert.assertTrue(users.contains(user2));

        Assert.assertEquals(2, session.users().getUsersCount(realm));

        // Remove client
        new ClientManager(realmManager).removeClient(realm, client);
        commit();

        // Assert service account removed as well
        realm = realmManager.getRealmByName("original");
        Assert.assertNull(session.users().getUserByUsername("user1", realm));
    }

    @Test
    public void testOfflineSessionsRemoved() {
        RealmModel realm = realmManager.createRealm("original");
        ClientModel fooClient = realm.addClient("foo");
        ClientModel barClient = realm.addClient("bar");

        UserModel user1 = session.users().addUser(realm, "user1");
        UserModel user2 = session.users().addUser(realm, "user2");

        addOfflineUserSession(realm, user1, "123", "something1");
        addOfflineClientSession(realm, user1, "456", "123", fooClient.getId(), "something2");
        addOfflineClientSession(realm, user1, "789", "123", barClient.getId(), "something3");

        addOfflineUserSession(realm, user2, "2123", "something4");
        addOfflineClientSession(realm, user2, "2456", "2123", fooClient.getId(), "something5");

        commit();

        // Searching by clients
        Assert.assertEquals(2, session.users().getOfflineClientSessionsCount(realm, fooClient));
        Assert.assertEquals(1, session.users().getOfflineClientSessionsCount(realm, barClient));

        Collection<OfflineClientSessionModel> clientSessions = session.users().getOfflineClientSessions(realm, fooClient, 0, 10);
        Assert.assertEquals(2, clientSessions.size());
        clientSessions = session.users().getOfflineClientSessions(realm, fooClient, 0, 1);
        OfflineClientSessionModel cls = clientSessions.iterator().next();
        assertSessionEquals(cls, "456", "123", fooClient.getId(), user1.getId(), "something2");
        clientSessions = session.users().getOfflineClientSessions(realm, fooClient, 1, 1);
        cls = clientSessions.iterator().next();
        assertSessionEquals(cls, "2456", "2123", fooClient.getId(), user2.getId(), "something5");

        clientSessions = session.users().getOfflineClientSessions(realm, barClient, 0, 10);
        Assert.assertEquals(1, clientSessions.size());
        cls = clientSessions.iterator().next();
        assertSessionEquals(cls, "789", "123", barClient.getId(), user1.getId(), "something3");

        realm = realmManager.getRealmByName("original");
        realm.removeClient(barClient.getId());

        commit();

        realm = realmManager.getRealmByName("original");
        user1 = session.users().getUserByUsername("user1", realm);
        Assert.assertEquals("something1", session.users().getOfflineUserSession(realm, user1, "123").getData());
        Assert.assertEquals("something2", session.users().getOfflineClientSession(realm, user1, "456").getData());
        Assert.assertNull(session.users().getOfflineClientSession(realm, user1, "789"));

        realm.removeClient(fooClient.getId());

        commit();

        realm = realmManager.getRealmByName("original");
        user1 = session.users().getUserByUsername("user1", realm);
        Assert.assertNull(session.users().getOfflineClientSession(realm, user1, "456"));
        Assert.assertNull(session.users().getOfflineClientSession(realm, user1, "789"));
        Assert.assertNull(session.users().getOfflineUserSession(realm, user1, "123"));
        Assert.assertEquals(0, session.users().getOfflineUserSessions(realm, user1).size());
        Assert.assertEquals(0, session.users().getOfflineClientSessions(realm, user1).size());
    }

    private void addOfflineUserSession(RealmModel realm, UserModel user, String userSessionId, String data) {
        OfflineUserSessionModel model = new OfflineUserSessionModel();
        model.setUserSessionId(userSessionId);
        model.setData(data);
        session.users().addOfflineUserSession(realm, user, model);
    }

    private void addOfflineClientSession(RealmModel realm, UserModel user, String clientSessionId, String userSessionId, String clientId, String data) {
        OfflineClientSessionModel model = new OfflineClientSessionModel();
        model.setClientSessionId(clientSessionId);
        model.setUserSessionId(userSessionId);
        model.setUserId(user.getId());
        model.setClientId(clientId);
        model.setData(data);
        session.users().addOfflineClientSession(realm, model);
    }

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getUsername(), actual.getUsername());
        Assert.assertEquals(expected.getCreatedTimestamp(), actual.getCreatedTimestamp());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());

        String[] expectedRequiredActions = expected.getRequiredActions().toArray(new String[expected.getRequiredActions().size()]);
        Arrays.sort(expectedRequiredActions);
        String[] actualRequiredActions = actual.getRequiredActions().toArray(new String[actual.getRequiredActions().size()]);
        Arrays.sort(actualRequiredActions);

        Assert.assertArrayEquals(expectedRequiredActions, actualRequiredActions);
    }

    private static void assertSessionEquals(OfflineClientSessionModel cls, String expectedClientSessionId, String expectedUserSessionId,
                                     String expectedClientId, String expectedUserId, String expectedData) {
        Assert.assertEquals(cls.getData(), expectedData);
        Assert.assertEquals(cls.getClientSessionId(), expectedClientSessionId);
        Assert.assertEquals(cls.getUserSessionId(), expectedUserSessionId);
        Assert.assertEquals(cls.getUserId(), expectedUserId);
        Assert.assertEquals(cls.getClientId(), expectedClientId);
    }

}

