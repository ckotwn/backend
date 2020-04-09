package life.catalogue.api.model;

import org.junit.Test;

import static org.junit.Assert.*;
import static life.catalogue.api.model.ColUser.Role.*;

public class ColUserTest {

  @Test
  public void hasRole() {
    ColUser u = new ColUser();
    for (ColUser.Role r : ColUser.Role.values()) {
      assertFalse(u.hasRole(r, null));
      assertFalse(u.hasRole(r.name(), 1));
      assertFalse(u.hasRole(r, 1));
    }
    u.addRole(USER);
    for (ColUser.Role r : ColUser.Role.values()) {
      if (r == USER) {
        assertTrue(u.hasRole(r, null));
        assertTrue(u.hasRole(r.name(), 1));
        assertTrue(u.hasRole(r, 1));
      } else {
        assertFalse(u.hasRole(r, null));
        assertFalse(u.hasRole(r.name(), 1));
        assertFalse(u.hasRole(r, 1));
      }
    }


    u.addRole(EDITOR);
    assertTrue(u.hasRole(USER, null));
    assertTrue(u.hasRole(USER.name(), 1));
    assertTrue(u.hasRole(USER, 1));

    assertTrue(u.hasRole(EDITOR, null));
    assertTrue(u.hasRole(EDITOR.name(), null));
    assertFalse(u.hasRole(EDITOR, 1));

    assertFalse(u.hasRole(ADMIN, null));
    assertFalse(u.hasRole(ADMIN.name(), 1));
    assertFalse(u.hasRole(ADMIN, 1));


    u.addDataset(1);
    assertTrue(u.hasRole(USER, null));
    assertTrue(u.hasRole(USER.name(), 1));
    assertTrue(u.hasRole(USER, 1));

    assertTrue(u.hasRole(EDITOR, null));
    assertTrue(u.hasRole(EDITOR.name(), null));
    assertTrue(u.hasRole(EDITOR, 1));
    assertFalse(u.hasRole(EDITOR, 2));

    assertFalse(u.hasRole(ADMIN, null));
    assertFalse(u.hasRole(ADMIN, 1));
    assertFalse(u.hasRole(ADMIN, 2));

    u.addDataset(2);
    assertTrue(u.hasRole(EDITOR, null));
    assertTrue(u.hasRole(EDITOR, 1));
    assertTrue(u.hasRole(EDITOR, 2));

    u.addRole(ADMIN);
    assertTrue(u.hasRole(ADMIN, null));
    assertTrue(u.hasRole(ADMIN, 1));
    assertTrue(u.hasRole(ADMIN, 2));
  }

  @Test
  public void isAuthorized() {
    ColUser u = new ColUser();
    assertFalse(u.isAuthorized(1));

    u.addDataset(1);
    assertFalse(u.isAuthorized(1));

    u.addRole(USER);
    assertFalse(u.isAuthorized(1));

    u.addRole(ADMIN);
    assertTrue(u.isAuthorized(1));

    u.removeRole(ADMIN);
    assertFalse(u.isAuthorized(1));

    u.addRole(EDITOR);
    assertTrue(u.isAuthorized(1));
    assertFalse(u.isAuthorized(2));

    u.addDataset(2);
    assertTrue(u.isAuthorized(2));
  }

  @Test
  public void isEditor() {
    ColUser u = new ColUser();
    assertFalse(u.isEditor(1));

    u.addDataset(1);
    assertFalse(u.isEditor(1));

    u.addRole(ADMIN);
    assertFalse(u.isEditor(1));

    u.addRole(EDITOR);
    assertTrue(u.isEditor(1));
    assertTrue(u.isEditor(1));
    assertFalse(u.isEditor(-1));
  }
}