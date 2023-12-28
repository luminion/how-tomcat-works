package ex14.pyrmont.startup;

import ex14.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;

/**
 * 服务器组件和服务组件
 *
 *      顶级组件：
 *          Server      表示一个Tomcat实例，Server代表整个catalina servlet容器；包含一个或多个service子容器。主要是用来管理容器下各个Service组件的生命周期
 *          Service     代表Tomcat中一组提供服务、处理请求的组件（包括多个Connector和一个Container）
 *      连接器：
 *          Connector   连接器，客户端连接到Tomcat容器的服务点
 *      容器组件：
 *          Container   容器的父接口，用于封装和管理Servlet，以及具体处理Request请求，该容器的设计用的是典型的责任链的设计模式
 *              Engine  引擎，表示可运行的Catalina的servlet引擎实例，并且包含了servlet容器的核心功能。
 *              Host    虚拟主机，作用就是运行多个应用，它负责安装和展开这些应用，并且标识这个应用以便能够区分它们
 *              Context 上下文，代表 Servlet 的 Context，它具备了 Servlet 运行的基本环境，它表示Web应用程序本身
 *              Wrapper 包装器，代表一个 Servlet，它负责管理一个 Servlet，包括的 Servlet 的装载、初始化、执行以及资源回收
 *      嵌套组件：
 *           Valve    阀门：类似于Servlet规范中定义的过滤器，用来拦截请求并在将其转至目标之前进行某种处理操作。
 *           Logger   日志记录器：用于记录组件内部的状态信息
 *           Loader   类加载器：负责加载、解释Java类编译后的字节码
 *           Realm    领域：用于用户的认证和授权
 *           Executor 执行器：执行器组件允许您配置一个共享的线程池，以供连接器使用(从tomcat 6.0.11版本开始)
 *           Listener 监听器：监听已注册组件的生命周期。
 *           Manager  会话管理器：用于实现http会话管理的功能
 *           Cluster  集群：专用于配置Tomcat集群的元素(可用于Engine和Host容器中)

 * org.apache.catalina.Server接口
 *    该接口标识Catalina的整个servlet引擎，囊括了所有组件
 *      服务器组件不再需要单独对连接器和容器进行启动/关闭
 *      其原理为启动时服务器组件时，其会启动其中所有的组件，然后无限期等待关闭命令
 *    服务器组件使用了另一个组件(服务组件)来包含其他组件，如一个容器组件一个/多个连接器组件
 *
 * org.apache.catalina.core.StandardServer类
 *    Server接口的标准实现
 *    该类中许多方法都与server.xml中的服务器配置相关
 *    一个服务器组件可以有0个或多个服务组件
 *    该类提供了addService()方法，removeService()和findService()方法的实现
 *    该类有4个与生命周期相关的方法，分别为initialize()、start()、stop()、await()
 *    和其他组件一样，可以初始化并启动服务器组件
 *    可以使用await()方法使其一直阻塞，直到其从指定端口上接受到关闭命令
 *    当await()方法返回时，会运行stop()方法关闭其下所有子组件
 *
 * org.apache.catalina.Service接口
 *    服务组件是该接口的实例，一个服务组件可以包含一个servlet容器(Container)和多个连接器(Connector)
 *    可以自由地将连接器实例添加到服务组件中，所有连接器都会与这个servlet容器相关联
 *
 * org.apache.catalina.core.StandardService类
 *    Service接口的标准实现
 *    该类的initialize()方法用于初始化所有添加到其中的连接器
 *    该类同时也实现了Lifecycle接口，该类的start()方法可以启动连接器和所有servlet容器
 *    该类的实例中，有两种组件
 *      分别是连接器(Connector)和servlet容器(Container)
 *      servlet容器只有一个，连接器可以有多个，多个连接器使tomcat可以为不同的请求协议提供服务
 *      例如，一个连接器处理http请求，另一个处理https请求
 *    该类的container变量指向一个Container接口的实例
 *    该类的Connectors[] 指向所有连接器的引用
 *      在该类中，与服务相关联的servlet容器，被传给每个连接器对象的setContainer()方法，以实现关联关系
 *      实用addConnector()或removeConnector()方法添加或移除连接器
 *    该类与生命周期有关的方法
 *      包括从lifecycle接口中实现的start()、stop()方法
 *      initialize()方法，该方法会调用服务组件中所有连接器的initialize()方法
 */
public final class Bootstrap {
  public static void main(String[] args) {

    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();

    Wrapper wrapper1 = new StandardWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new StandardWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/app1");
    context.setDocBase("app1");

    context.addChild(wrapper1);
    context.addChild(wrapper2);

    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    Host host = new StandardHost();
    host.addChild(context);
    host.setName("localhost");
    host.setAppBase("webapps");

    Loader loader = new WebappLoader();
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    Engine engine = new StandardEngine();
    engine.addChild(host);
    engine.setDefaultHost("localhost");

    Service service = new StandardService();
    service.setName("Stand-alone Service");
    Server server = new StandardServer();
    server.addService(service);
    service.addConnector(connector);

    // StandardService class's setContainer will call all its connector's setContainer method
    // StandardService 类的 setContainer 将调用其所有连接器的 setContainer 方法
    service.setContainer(engine);

    // Start the new server
    if (server instanceof Lifecycle) {
      try {
        // 初始化
        server.initialize();
        // 开始
        ((Lifecycle) server).start();
        // 开始监听关闭指令
        // 此时主线程会进入await()方法中创建ServerSocket并一直循环接收指令
        // 当指令为非关闭指令时，会在控制台输出指令
        // 当指令为关闭指令时，会跳出await()方法中的循环，关闭套接字，回到此处继续往下执行
        server.await();
        // the program waits until the await method returns,
        // i.e. until a shutdown command is received.
      }
      catch (LifecycleException e) {
        e.printStackTrace(System.out);
      }
    }

    // Shut down the server
    // 关闭服务器
    if (server instanceof Lifecycle) {
      try {
        ((Lifecycle) server).stop();
      }
      catch (LifecycleException e) {
        e.printStackTrace(System.out);
      }
    }
  }
}