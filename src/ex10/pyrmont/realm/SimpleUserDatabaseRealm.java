package ex10.pyrmont.realm;
// modification of org.apache.catalina.realm.UserDatabaseRealm

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.catalina.Group;
import org.apache.catalina.Role;
import org.apache.catalina.User;
import org.apache.catalina.UserDatabase;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.catalina.users.MemoryUserDatabase;

/**
 * 该领域对象将用户凭证保存在系统变量中UserDatabase的位置
 * 也就是主启动类bootstrap2指定的conf/tomcat-users.xml文件
 * 该类会读取文件的内容，然后将其载入内存，根据读取的内容进行验证
 *
 */
public class SimpleUserDatabaseRealm extends RealmBase {

  protected UserDatabase database = null;
  protected static final String name = "SimpleUserDatabaseRealm";

  protected String resourceName = "UserDatabase";

  public Principal authenticate(String username, String credentials) {
    // Does a user with this username exist?
    // 从数据源中获取用户
    User user = database.findUser(username);
    if (user == null) {
      return (null);
    }

    // Do the credentials specified by the user match?
    // FIXME - Update all realms to support encoded passwords
    // 用户指定的凭据是否匹配？ 编码后进行比较
    boolean validated = false;
    if (hasMessageDigest()) {
      // Hex hashes should be compared case-insensitive
      validated = (digest(credentials).equalsIgnoreCase(user.getPassword()));
    }
    else {
      validated = (digest(credentials).equals(user.getPassword()));
    }
    if (!validated) {
      return null;
    }

    // 提取权限
    ArrayList combined = new ArrayList();
    Iterator roles = user.getRoles();
    while (roles.hasNext()) {
      Role role = (Role) roles.next();
      String rolename = role.getRolename();
      if (!combined.contains(rolename)) {
        combined.add(rolename);
      }
    }
    Iterator groups = user.getGroups();
    while (groups.hasNext()) {
      Group group = (Group) groups.next();
      roles = group.getRoles();
      while (roles.hasNext()) {
        Role role = (Role) roles.next();
        String rolename = role.getRolename();
        if (!combined.contains(rolename)) {
          combined.add(rolename);
        }
      }
    }
    return (new GenericPrincipal(this, user.getUsername(),
      user.getPassword(), combined));
  }

  // ------------------------------------------------------ Lifecycle Methods


    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
  protected Principal getPrincipal(String username) {
    return (null);
  }

  protected String getPassword(String username) {
    return null;
  }

  protected String getName() {
    return this.name;
  }

  /**
   * 根据指定路径创建数据源
   * @param path
   */
  public void createDatabase(String path) {
    // 创建UserDatabase的具体实现，
    // 将所有定义的用户、组和角色加载到内存数据结构中，
    // 并使用指定的 XML 文件进行持久存储
    database = new MemoryUserDatabase(name);
    //设置MemoryUserDatabase的文件路径
    ((MemoryUserDatabase) database).setPathname(path);
    try {
      // 解析文件
      database.open();
    }
    catch (Exception e)  {
    }
  }
}