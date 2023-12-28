package ex08.pyrmont.startup;

import ex08.pyrmont.core.SimpleWrapper;
import ex08.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.naming.resources.ProxyDirContext;

/**
 * 载入器
 * 在tomcat中，需要使用自定义载入器来加载类
 *
 * 为什么使用自定义类加载器
 *    因servlet容器不应该完全信任它正在运行的servlet类
 *    若类似ex02.pyrmont包的demo应用程序使用系统类的载入器载入某个servlet所使用的全部类
 *    那么servlet能访问所有的类，包括java虚拟机环境比那辆CLASSPATH指定路径下所有的类和库，非常危险
 *    servlet应该只允许载入WEB-INF/classes目录及其子目录下的类
 *
 * 在Catalina中，载入器是org.apache.catalina.Loader接口的实例
 *    使用自定义类载入器的另外一个原因，为了提供自动重载的功能，
 *    即当WEB-INF/classes目录或WEB-INF/lib目录下的类发生变化是，web应用程序会重新载入这些类
 *    在tomcat的载入器的实现中，需要一个额外线程不断检查servlet类和其他类文件的时间戳
 *    若要支持自动重载功能，则载入器必须实现org.apache.catalina.loader.Reloader接口
 *    因tomcat内的类加载器仅能加载WEB-INF/classes目录及其子目录，目录外的类无法加载，
 *    所以tomcat中载入器指的是web应用程序加载器，而不仅仅指类加载器
 *
 * 术语：
 *   repository 仓库    表示要载入的类存放位置（本地路径）
 *   resource   资源    值一个类在容器中的DirContext对象，其文件根路径就是上下文的文件根路径(资源路径，应用程序中的访问url)
 *
 * 载入器必须实现org.apache.catalina.Loader接口
 * tomcat的载入器通常会与一个Context级别容器关联，
 *      如果Context的一个或多个servlet类被修改了,载入器也可以支持对类的自动重载
 *      这样程序员只需要重新编译servlet类，并将其重新载入即可，不用重启tomcat
 *    此外，Loader接口还添加了一些方法来对仓库的集合进行操作
 *    默认目录为WEB-INF/classes 和 WEB-INF/lib
 *    setReloadable()     设置是否支持自动重载
 *    getReloadable()     获取是否支持自动重载
 *    getDelegate()       是否适用双亲委派机制
 *    setDelegate()       设置是否适用双亲委派
 *    modified()          重载来支持类自动重载(默认禁用了自动重载)
 *        在载入器的实现中，如果仓库中的一个类或多个类被修改了，那么modified()必须返回true，才能提供重载的支持
 *        但载入器自身并不能自动重载，他会调用Context容器接口中的reload()方法来实现，
 *        因此，想要启动Context容器的自动重载功能，需要在server.xml文件中添加一个Context元素:
 *        <Context path="/myApp" docBase="myApp" debug="0" reloadable="true"/>
 *
 * Catalina使用org.apache.catalina.loader.WebappLoader类作为Loader接口的实现
 *    WebappLoader会创建一个org.apache.catalina.loader.WebappClassLoader类的实例作为其类载入器
 *    WebappClassLoader继承自java.net.URLClassLoader类
 *
 *    WebappLoader是和Context容器关联的载入器，
 *    WebappClassLoader是WebappLoader内的变量，是用来加载类的类载入器
 *
 * 为了支持类的重新加载功能
 * 类载入器需要实现org.apache.catalina.loader.Reloader接口
 *    modified()          当仓库中的某个类被修改的时候，该方法会返回true
 *    addRepository()     添加仓库
 *    findRepositories()  获取仓库列表
 *
 * WebappLoader类
 *    该类实现了Loader、Lifecycle、PropertyChangeListener、Runnable接口
 *    该类使用org.apache.catalina.loader.WebappClassLoader类的实例作为类载入器
 *    该类实现了Lifecycle接口，可以由相关的容器或组件来启动
 *    该类实现了Runnable接口，
 *        可以指定一个其他线程来不断调用其modified()方法，
 *        如果modified()返回true
 *        WebappLoader的实例会通知其关联的servlet容器（在此处为Context）
 *        然后由容器(Context)完成类的重新载入（调用容器自身的reload()方法）
 *
 *        当调用WebappLoader的start()方法时，会完成以下工作
 *          创建一个类载入器
 *              为了完成类的载入，WebappLoader会在内部创建使用一个类载入器，
 *                Loader接口仅提供了getClassLoader()方法，未提供setClassLoader()方法，
 *                因此，无法实例化一个新的类载入器，再将其复制给WebappLoader，
 *                为了程序的灵活性，
 *                WebappLoader使用了getLoaderClass()和setLoaderClass()方法来获取或改变其私有变量loaderClass的值
 *                loaderClass指明了类载入器的名字，默认情况，该值为org.apache.catalina.loader.WebappClassLoader
 *                也可以继承WebappClassLoader来实现自己的类载入器，然后使用setLoaderClass()方法强制WebappLoader使用自定义载入器
 *                否则，WebappLoader启动时会调用createClassLoader()方法创建默认类加载器
 *          设置仓库
 *              使用addRepository()方法，将仓库添加到类加载器中
 *                WEB-INF/classes目录被传入到类载入器的addRepository()方法中
 *                WEB-INF/lib目录被传入到类载入器的setJarPath()方法中
 *              如此，类加载器就能在类目录和库目录中载入相关类
 *          设置类路径
 *              调用WebappLoader的setClassPath()方法
 *              该方法会在servlet上下文(Context)中为JSP编译器设置一个字符串形式的属性来指明类路径信息
 *          设置访问权限
 *              调用WebappLoader的setPermissions();
 *              若运行tomcat时使用了安全管理器，该方法会为类载入器设置访问相关目录的权限
 *              例如：只能访问WEB-INF/classes和WEB-INF/lib目录
 *              若未使用安全管理器，则该方法会直接返回
 *          启动一个新线程来执行类的自动载入
 *              WebappLoader支持自动重载功能（如果某些类被重新编译了，那么这个类会重新载入，无需重启tomcat），
 *              为了实现此目的，WebappLoader需要一个线程周期性的检查每个资源的时间戳
 *              检查的间隔时间由变量checkInterval决定(秒)，默认15
 *              此处创建一个新线程，将自身传入新线程中（因自身实现了Runnable接口），并启动新线程
 *              在自身的run()方法中,
 *              每隔一段时间调用WebappClassLoader的modified()方法，检查是否有资源被修改
 *              若被修改，调用自身的notifyContext()方法，进行类的重载工作
 *                  notifyContext()方法不会直接调用Context接口的reload()方法，
 *                  而是会实例化一个内部类WebappContextNotifier
 *                  并创建一个新线程，将WebappContextNotifier传入其中，随后启动线程
 *                  WebappContextNotifier的run()方法会调用Context的reload()方法进行类的重载
 *                  让重载类的任务在另一个线程中完成
 *
 *                  注意：
 *                      在tomcat5中，检查类被修改的任务改为由org.apache.catalina.core.StandardContext的backgroundProcess()方法完成
 *                      这个方法被org.apache.catalina.core.ContainerBase抽象类中的一个专用线程池周期性的调用，
 *                      ContainerBase是StandardContext的父类，
 *                      ContainerBase的内部类ContainerBackgroundProcessor实现了Runnable接口
 *
 * WebappClassLoader类
 *    web应用程序中负责载入类的类载入器
 *    该类设置参考了安全和性能两方面，
 *        例如：
 *           缓存之前已经载入的类提升性能
 *           缓存加载失败的类的名字，当再次请求同一个类时，抛出ClassNotFoundException，不再重新查找
 *           考虑到安全，WebappClassLoader不允许载入某些类
 *              这些类的名字存放在一个字符串数组triggers中
 *              默认只有一个元素：javax.servlet.Servlet
 *           某些特殊包及其子包下的类也是不允许载入的，也不会将任务委托给系统类加载器去执行
 *              这些包的名字存放在一个字符串数组packageTriggers中
 *              默认包含：
 *                javax
 *                org.xml.sax
 *                org.w3c.dom
 *                org.apache.xerces
 *                org.apache.xalan
 *    该类的实例会在指定会在仓库列表和jar文件中搜索需要载入的类
 *    为了达到更好的性能，会缓存已经载入的类，下次再使用该类时，直接从缓存中获取
 *    缓存可以在本地执行，即可以由WebappClassLoader来管理所加载并缓存的类
 *    此外，java.lang.Classloader类会维护一个Vector对象，保存已经载入的类，防止这些类在不使用时被垃圾回收
 *    在这种情况下，其缓存是由父类来管理的
 *
 *    每个由载入的类，都视为"资源"，"资源"是org.apache.catalina.loader.ResourceEntry的实例
 *    ResourceEntry会保存其代表的class文件的字节流，最后一次修改日期，Manifest信息(如果来自jar文件)等
 *    所有被缓存的资源都会缓存在WebappClassLoader的变量resourceEntries中
 *    所有未找到的资源都会缓存在WebappClassLoader的变量notFoundResources中
 *
 *    载入类
 *    loadClass()方法
 *    载入类时，遵循以下规则
 *        先检查本地缓存
 *        若本地缓存无，则检查上一级缓存（java.lang.Classloader的findLoadedClass()方法）
 *        若两个缓存都没有，则使用系统的类载入器进行加载，防止覆盖jdk的类
 *        若启用了安全管理器，则检查是否允许载入该类，若为禁止，抛出ClassNotFoundException
 *        若打开标志位delegate，或者待载入的类是属于包触发器的类，则调用父载入器来载入相关类，若父载入器为null，则使用系统的类载入器
 *        若还是未找到指定类，抛出ClassNotFoundException异常
 */
public final class Bootstrap {
  public static void main(String[] args) {

    //invoke: http://localhost:8080/Modern or  http://localhost:8080/Primitive
    // 将catalina.base属性设置为用户的根目录
    System.setProperty("catalina.base", System.getProperty("user.dir"));

    //连接器和wrapper容器
    Connector connector = new HttpConnector();
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    // 应用程序上下文容器 StandardContext是tomcat中的Context的标准实现
    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/myApp");
    context.setDocBase("myApp");

    context.addChild(wrapper1);
    context.addChild(wrapper2);

    // context.addServletMapping(pattern, name);
    // 请求路径映射
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    // add ContextConfig. This listener is important because it configures
    // StandardContext (sets configured to true), otherwise StandardContext
    // won't start
    // 添加上下文配置。这个监听器很重要，
    // 因为它配置了StandardContext
    // 为此上下文设置“正确配置”标志为true，才能正常启动。
    // 正确配置默认为false，检测到致命配置错误时，启动侦听器设置也会为false，
    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);



    // here is our loader
    // 加载器
    Loader loader = new WebappLoader();
    // associate the loader with the Context
    // 关联加载器和容器
    context.setLoader(loader);

    // 关联连接器和应用程序
    connector.setContainer(context);

    //启动
    try {
      connector.initialize();
      // 启动连接器
      ((Lifecycle) connector).start();
      // 启动容器
      ((Lifecycle) context).start();

      // now we want to know some details about WebappLoader
      // 获取加载器使用的类加载器相关信息
      WebappClassLoader classLoader = (WebappClassLoader) loader.getClassLoader();
      // 打印资源文档根目录
      System.out.println("Resources' docBase: " + ((ProxyDirContext)classLoader.getResources()).getDocBase());
      // 获取仓库列表
      String[] repositories = classLoader.findRepositories();
      for (int i=0; i<repositories.length; i++) {
        // 打印仓库路径
        System.out.println("  repository: " + repositories[i]);
      }

      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) context).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}