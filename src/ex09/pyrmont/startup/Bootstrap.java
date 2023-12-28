package ex09.pyrmont.startup;

import ex09.pyrmont.core.SimpleWrapper;
import ex09.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;

/**
 * session管理
 *    tomcat使用org.apache.catalina.Manager接口来管理session
 *    session管理器需要与一个Context容器关联，负责创建、更新、销毁session对象
 *    servlet实例可以通过调用javax.servlet.http.HttpServletRequest的getSession()方法获取一个session对象
 *    在Catalina的默认连接器中，org.apache.catalina.connector.HttpRequestBase类实现HttpServletRequest接口
 *    可以用来获取session对象（获取关联的Context容器，从Context容器获取session管理器，然后从session管理器获取session）
 *
 *
 *    默认情况下， session管理器将session存放在内存中，在tomcat中，其也可以将session对象持久化到文件储存器或通过jdbc写入到数据库
 *    org.apache.catalina.session包下存放了许多与session相关的类
 *
 *    在servlet编程方面中，Session对象由javax.servlet.http.HttpSession接口表示
 *    在tomcat中的标准实现是org.apache.catalina.session.StandardSession类
 *    为了安全，管理器并不会直接将StandardSession交给servlet使用
 *    而是使用一个外观类，org.apache.catalina.session.StandardSessionFacade
 *    在管理器内部，session管理器会使用另一个外观类：org.apache.catalina.Session接口
 *
 *    org.apache.catalina.Session接口是作为Catalina的外观类使用的
 *    其标准实现为org.apache.catalina.session.StandardSession
 *    session对象总是存在于session管理器中的
 *    可以使用session的getManager()和setManager()方法将session和管理器关联
 *    对于某个session来说，在于其Session管理器相关联的某个Context容器内，该session对象有一个唯一标识符
 *    可以通过setId()和setId()方法来访问该标识符
 *    session管理器会调用getLastAccessedTime方法，根据返回值判断一个session对象的有效性
 *    session管理器会调用setValid()方法来设置或重置session对象的有效性
 *    每当访问一个session实例时，会调用其access()方法来修改session对象的最后访问时间
 *    最后，session管理器会调用session对象的expire()方法将其设置为过期
 *    也可以通过getSession()方法获取一个经过session外观类包装的HttpSession对象
 *
 * org.apache.catalina.session.StandardSession类
 *    该类是Session接口的标准实现
 *    实现了javax.servlet.http.HttpSession接口、org.apache.catalina.Session接口、java.lang.Serializable接口
 *    该类的构造函数接收一个session管理器的实例，迫使其构造时与某个session管理器关联
 *    该类的某些变量带了transient关键字，指定无法实例化
 *        在tomcat5中，该类一些私有变量被修改为受保护的，每个变量对应一个getter和setter
 *    该类的getSession()方法会通过传入一个自身实例来创建一个StandardSessionFacade外观类，并将其返回
 *    若session管理器中某个session对象在某个时间长度内未被访问时，会被设置为过期，时间长度由变量maxInactiveInterval决定
 *    设置过期的方式是调用session的expire()方法实现
 *
 *
 * org.apache.catalina.Manager接口
 *    session管理器的定义，所有的session管理系都需要实现该接口的功能
 *    该接口有以下方法
 *    getContainer()和setContainer()      以关联容器和session管理器
 *    getMaxInactiveInterval()和getMaxInactiveInterval()     设置session过期时间，单位为秒
 *    createSession()   来创建一个session实例
 *    add()             将session实例添加到实例池中
 *    remove()          从实例池中移除一个session
 *    load()            从持久化介质中加载session
 *    unload()          将session存储到持久化介质中
 *
 * org.apache.catalina.ManagerBase抽象类
 *    提供了Manager接口常见功能的实现，其有两个子类，StandardManager、PersistentManagerBase
 *    该抽象类提供了以下方法：
 *    generateSessionId()     构建一个新的唯一sessionId
 *    findSession(String id)  查找指定id的session并返回
 *    findSession()           返回所有的session数组
 *
 * org.apache.catalina.StandardManager类
 *    该类会将实例存放在内存中，
 *    当catalina关闭时，其会将当前内存中所有session对象存储到文件中
 *    当再次启动catalina时，又会将这些session对象重新载入到内存中
 *    该类实现了Lifecycle接口，使其可以由其关联的Context容器来启动或关闭
 *        其中stop()方法，会调用其unload()方法，将对象序列化到一个名为"SESSION.ser"的文件中
 *        每个Context容器都会产生一个该文件
 *        该文件位于环境变量CATALINA_HOME指定的目录下的work目录中
 *    在该类中，销毁已经失效的session是由一个单独的线程来完成的
 *    为此，该类还实现了Runnable接口，在run()方法中进行session的销毁
 *    在run()方法中，线程会休眠maxInactiveInterval属性对应的时间，之后检查是否有需要销毁的session，若有则销毁
 *        注：
 *          在tomcat5中该类不再实现Runnable，改由backGroundProcess()方法调用
 *          这个方法被org.apache.catalina.core.ContainerBase抽象类中的一个专用线程池周期性的调用，
 *          ContainerBase是StandardContext的父类，
 *          ContainerBase的内部类ContainerBackgroundProcessor实现了Runnable接口
 *
 * org.apache.catalina.PersistentManagerBase抽象类
 *    该类的子类，会将session对象储存到辅助存储器中，在catalina中，该类有两个子类：
 *        org.apache.catalina.session.PersistentManager
 *        org.apache.catalina.session.DistributedManager
 *    该类与StandardManager主要区别在于储存形式的区别
 *    该类有一个org.apache.catalina.Store接口的变量，用于表示储存形式
 *    在该持久化session管理器中，session对象可以备份，也可以换出
 *        当备份一个对象时，该session对象会被复制到储存器中，原对象留在内存中
 *            当空闲时间超过maxIdleBackup后悔被备份
 *            可将该值设置为负数防止备份
 *        当换出一个对象时，该session对象会被移动到储存器中，原对象从内存中删除
 *            当前活动的session对象超过了上限数，或该session对象闲置过长时间时，为了节省内存，会将其换出
 *            上限数由变量maxActiveSessions指定
 *            闲置时间由minIdleSwap和maxIdleSwap指定
 *            可以通过将以上的值设置为负数防止session被换出
 *    同样，该类在tomcat4中实现了Runnable接口用一个专用线程来进行备份换出，tomcat5中改为backGroundProcess()方法调用
 *    该类的findSession() 方法与之前有区别，该方法会先在session池中查找，然后在持久化介质中查找
 *
 * org.apache.catalina.session.PersistentManager类
 *    相比PersistentManagerBase抽象类，仅多出两个属性，未添加新方法
 *
 * org.apache.catalina.session.DistributedManager类
 *    该类用于两个或多个节点的集群服务器
 *    当复制session对象时，该类实例会向其他节点发送消息，此外，集群中的节点也必须能够接收到其他节点发送的消息
 *    发送和接收消息通过org.apache.catalina.cluster包下的接口和类进行统一
 *        其中：
 *            ClusterSender接口用于发送消息
 *            ClusterReceiver接口用于接收消息
 *    该类在使用createSession()方法创建session对象时，便会向其他节点发送消息
 *    同时，该类实现了Runnable接口，使一个专门的线程来检查Session对象是否过期，并接受其他集群节点上的消息
 *
 * org.apache.catalina.Store接口
 *    储存器，是为session管理器提供持久化存储器的一个组件
 *    该接口中比较重要的方法是
 *    load()      从持久化介质中加载session
 *    save()      将session保存到持久化介质中
 *    key()       以字符串数组形式返回所有session对象的标识符
 *
 * org.apache.catalina.session.StoreBase抽象类
 *    实现了org.apache.catalina.Store
 *    提供了一些基本功能，但未实现load()和save()方法，因这两个方法和储存介质有关
 *    该类有两个子类：FileStore和JDBCStore
 *    在tomcat4中，StoreBase使用另一个线程周期性地检查session对象，从活动的集合中移除过期的session对象
 *    在tomcat4中，不再使用专用线程，改用其关联的PersistentManagerBase实例的backGroundProcess()方法
 *
 * org.apache.catalina.session.FileStore类
 *    该类将session对象存储到某个文件中
 *    文件名以session对象的标识符+".session"构成
 *    可以使用setDirectory()方法改变储存的目录
 * org.apache.catalina.session.JDBCStore类
 *    该类将session文件存储到数据库中
 *    需要调用setDriverName()和setConnectionURL()方法设置驱动和连接
 *
 *
 * 相比ex08，使用的SimpleWrapperValue有所些许不同
 * request对象需要关联Context容器，否则无法获取安全管理器，也无法获取session
 */
public final class Bootstrap {
  public static void main(String[] args) {

    //invoke: http://localhost:8080/myApp/Session

    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    // 该servlet位于myApp/WEB-INF/classes/SessionServlet.class

    wrapper1.setName("Session");
    wrapper1.setServletClass("SessionServlet");

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/myApp");
    context.setDocBase("myApp");

    context.addChild(wrapper1);

    // context.addServletMapping(pattern, name);
    // note that we must use /myApp/Session, not just /Session
    // because the /myApp section must be the same as the path, so the cookie will
    // be sent back.
    // 添加映射，注：此处的映射与ex08不同
    context.addServletMapping("/myApp/Session", "Session");
    // add ContextConfig. This listener is important because it configures
    // StandardContext (sets configured to true), otherwise StandardContext
    // won't start
    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    // here is our loader
    Loader loader = new WebappLoader();
    // associate the loader with the Context
    context.setLoader(loader);

    connector.setContainer(context);

    // add a Manager
    Manager manager = new StandardManager();
    context.setManager(manager);

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