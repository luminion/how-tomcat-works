package ex10.pyrmont.startup;

import ex10.pyrmont.core.SimpleWrapper;
import ex10.pyrmont.core.SimpleContextConfig;
import ex10.pyrmont.realm.SimpleRealm;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.loader.WebappLoader;

/**
 * 安全与验证
 *    servlet容器是通过一个名为验证器的阀来支持安全限制的
 *    当servlet容器启动时，验证器阀被添加到context容器管道中
 *    调用wrapper阀时先调用验证器阀
 *    验证器阀调用Context容器的领域对象的authenticate()方法，传入用户名和密码进行校验
 *
 *
 * org.apache.catalina.Realm接口
 *    领域对象的实例，需要实现该接口
 *    领域对象是用来对用户进行身份验证的组件，对用户名和密码有效性进行判断
 *    领域对象保存了所有有效用户的用户名和密码，或会访问保存存储这些数据的存储器，这些数据的具体存储依赖于领域对象的具体实现
 *    在tomcat中，有效用户信息默认存储在tomcat-user.xml文件中，但是可以使用其他领域对象来实现针对其他资源验证用户，例如查询关系型数据库
 *    通常和一个context容器关联
 *    此接口有4个重载方法用于检验身份
 *       authenticate(String username, String credentials)
 *       authenticate(String username, byte[] credentials)
 *       authenticate(X509Certificate certs[])
 *       authenticate(String username, String digest, String nonce, String nc, String cnonce, String qop, String realm, String md5a2)
 *       通常使用第一个方法校验，此外，还有一个校验指定角色是否拥有权限的方法
 *       hasRole(Principal principal, String role)
 *
 * org.apache.catalina.realm.RealmBase抽象类
 *    该类即Realm接口的基本实现，
 *    此类还有几个具体的实现类作为Realm接口的实现：JAASRealm 、 JDBCRealm、JNDIRealm、MemoryRealm、UserDatabaseRealm等
 *    其中，默认情况下使用MemoryRealm的实例作为Realm的实现(也就是领域对象)
 *
 * org.apache.catalina.realm.GenericPrincipal类
 *    主体对象的实例
 *    该类为java.security.Principal接口的实现类，该接口可用于表示任何实体，例如个人、公司和登录 ID
 *    该类的两个构造函数：
 *      GenericPrincipal(Realm realm, String name, String password)
 *      GenericPrincipal(Realm realm, String name, String password, List roles)
 *    从构造函数可以看出，该类必须与一个领域Realm关联，且必须拥有一个名字和密码
 *    该用户对应角色列表是可选的，可以调用其hasRole()方法，并传入一个字符串形式的角色名来检查该主体是否拥有指定角色
 *        hasRole()方法在tomcat4中，不会识别*，在tomcat5中，*代表任何字符，传入*会直接识别为true
 *
 * org.apache.catalina.deploy.LoginConfig类
 *    登录配置类，该类为final修饰，不可被继承
 *    该类有两个构造器
 *      LoginConfig()
 *      LoginConfig(String authMethod, String realmName, String loginPage, String errorPage)
 *    方法：(同时包含setter)
 *      getAuthMethod()   获取认证方法
 *      getErrorPage()    获取错误页面
 *      getLoginPage()    获取登录页面
 *      getRealmName()    获取领域对象名称
 *    可以调用LoginConfig类的getRealmName()方法来获取领域对象的名字，
 *    可以调用LoginConfig类的getAuthMethod()获取认证的方法名：
 *        认证的方法名只能是以下：BASIC、DIGEST、FORM、CLIENT-CERT
 *        如果使用的是基于表单验证的身份验证方法，LoginConfig类还需要在loginPage属性和errorPage属性以字符串形式储存登录及错误页面url
 *
 *  org.apache.catalina.Authenticator接口
 *      验证器对应的接口，该接口内无任何方法，仅用于标记
 *
 *  org.apache.catalina.authenticator.AuthenticatorBase抽象类
 *      继承自ValveBase，实现了Authenticator接口和Lifecycle接口
 *      继承ValveBase后，该类也是一个阀，可以直接添加到Context上下文的管道中
 *      该类的子类：
 *          org.apache.catalina.authenticator.BasicAuthenticator
 *              基本的身份验证
 *          org.apache.catalina.authenticator.DigestAuthenticator
 *              基于信息摘要的身份验证
 *          org.apache.catalina.authenticator.FormAuthenticator
 *              基于表单的身份验证
 *          org.apache.catalina.authenticator.SSLAuthenticator
 *              基于SSL的身份验证
 *          org.apache.catalina.authenticator.NonLoginAuthenticator
 *              当用户没有指定验证方法名时，使用该类对来访者的身份进行验证
 *              该类只会检查安全限制，不会涉及用户身份的验证
 *      验证器的重要工作是对用户进行身份验证，而具体验证逻辑在AuthenticatorBase类中未给出：
 *        AuthenticatorBase类的invoke()方法会调用其未实现的方法authenticate()
 *            抽象方法authenticate()需要传入一个LoginConfig类实例
 *            LoginConfig类实例从AuthenticatorBase实例关联的Context容器的getLoginConfig()方法中获取
 *        所以，具体的实现逻辑是由具体实现根据LoginConfig的配置中获取的信息处理之后决定的
 *
 * 安装验证器阀
 *  在部署描述中，login-config元素仅能出现一次，login-config包含一个authMethod元素来指定身份验证方法
 *  也就是说，一个Context实例，只能有一个LoginConfig类、一个AuthenticatorBase类的子类(验证类)
 *        认证的方法名：               对应具体验证逻辑的实现类：
 *        BASIC                     org.apache.catalina.authenticator.BasicAuthenticator
 *        DIGEST                    org.apache.catalina.authenticator.DigestAuthenticator
 *        FORM                      org.apache.catalina.authenticator.FormAuthenticator
 *        CLIENT-CERT               org.apache.catalina.authenticator.SSLAuthenticator
 *        未设置(NONE)               org.apache.catalina.authenticator.NonLoginAuthenticator
 *  由于使用的验证器是在运行时才确定的，因此，该类时动态载入的，
 *  默认上下文StandardContext使用org.apache.catalina.startup.ContextConfig来进行Context实例的属性设置
 *  ContextConfig负责动态载入AuthenticatorBase的子类，实例化其对象，并将其作为一个阀安装到StandardContext中
 *
 * 该启动类应用程序的SImpleContextConfig有改动，作为实例化StandardContext的配置使用
 */
public final class Bootstrap1 {
  public static void main(String[] args) {

  //invoke: http://localhost:8080/Modern or  http://localhost:8080/Primitive

    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/myApp");
    context.setDocBase("myApp");
    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    context.addChild(wrapper1);
    context.addChild(wrapper2);
    // for simplicity, we don't add a valve, but you can add
    // valves to context or wrapper just as you did in Chapter 6

    Loader loader = new WebappLoader();
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");
    // add ContextConfig. This listener is important because it configures
    // StandardContext (sets configured to true), otherwise StandardContext
    // won't start

    // add constraint
    // 约束
    SecurityCollection securityCollection = new SecurityCollection();
    securityCollection.addPattern("/");
    securityCollection.addMethod("GET");

    // 安全策略
    SecurityConstraint constraint = new SecurityConstraint();
    constraint.addCollection(securityCollection);
    // 添加访问限制的角色
    constraint.addAuthRole("manager");

    // 登录配置
    LoginConfig loginConfig = new LoginConfig();
    loginConfig.setRealmName("Simple Realm");
    // add realm
    Realm realm = new SimpleRealm();

    // 关联容器和realm
    context.setRealm(realm);
    // 关联安全策略
    context.addConstraint(constraint);
    // 关联登录配置
    context.setLoginConfig(loginConfig);

    connector.setContainer(context);

    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) context).start();

      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) context).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}