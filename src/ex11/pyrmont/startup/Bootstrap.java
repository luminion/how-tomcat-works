package ex11.pyrmont.startup;

//use StandardWrapper
import ex11.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;

/**
 * 使用StandardWrapper类（最小的servlet容器）
 *    该类是catalina中wrapper接口的标准实现
 *
 * 对于每个引入的http请求，tomcat的标准流程如下
 *    连接器创建request和response对象
 *    连接器Connector会调用与其关联的容器StandardContext的invoke()方法
 *    容器StandardContext的invoke()方法最后会调用与其绑定的管道StandardPipeline的invoke()方法
 *    管道StandardPipeline的invoke()方法调用依次管道中的阀的invokeNext()方法
 *    管道StandardPipeline中所有的阀都调用完后，会调用StandardPipeline管道基础阀StandardContextValve的invoke()方法
 *    基础阀StandardContextValve的invoke()方法会获取与其绑定的Context容器，
 *    基础阀StandardContextValve调用StandardContext容器的map()方法获取处理请求的StandardWrapper容器
 *        容器StandardContext容器的map方法会调用findMapper()方法(该方法由ContainerBse抽象类实现),获取与其绑定的StandardContextMapper映射器
 *        容器StandardContextMapper映射器会调用自身map()方法，根据路径寻找匹配的对应的StandardWrapper，并返回
 *    基础阀StandardContextValve基础阀获取StandardWrapperWrapper容器后，调用StandardWrapperWrapper容器的invoke()方法
 *
 *    容器StandardWrapper的invoke()方法最后会调用与其绑定的StandardPipeline管道的invoke()方法
 *    管道StandardPipeline的invoke()方法调用依次管道中的阀的invokeNext()方法
 *    管道StandardPipeline中所有的阀都调用完后，会调用Pipeline管道基础阀StandardWrapperValve的invoke()方法
 *    基础阀StandardWrapperValve的invoke()方法会调用Wrapper的allocate()方法获取Servlet实例
 *        容器StandardWrapper的allocate()方法判断是否已载入Servlet实例，已载入直接返回，未载入调用load()方法载入Servlet实例
 *        容器StandardWrapper的load()方法调用Servlet实例的init()方法
 *    基础阀StandardWrapperValue的invoke()方法会调用私有方法createFilterChain()创建过滤器链
 *        过滤器链调用过滤器的doFilter()方法
 *        过滤器链中过滤器调用完并全部通过后，调用Servlet实例的service()方法
 *        释放过滤器链
 *        调用wrapper的deallocate()方法，将servlet实例放回实例池（STM servlet，非STM servlet不会进行操作）
 *
 *
 *
 *
 * javax.servlet.SingleThreadModel接口
 *    servlet可以实现该接口，实现了该接口的servlet也被成为 STM servlet类
 *    实现该接口的目的是为了保证servlet实例一次只处理一个请求
 *    实现该接口后，可以保证不会同时有两个线程调用其service()方法，
 *      这一点由servlet容器通过对单一servlet实例的同步访问实现，
 *      或者维护一个servlet池，将每个新请求分派给一个空闲的servlet实例
 *    该接口并不能防止servlet访问共享资源造成的同步问题，例如访问类的静态变量或访问servlet作用域之外的类
 *    实现该接口并不代表servlet线程安全，因该接口仅保证在同一时刻只有一个线程在执行service方法，
 *      但是为了提高性能，servlet容器一般会创建多个servlet，
 *      也就是说，STM servlet实例的service方法会在多个STM servlet实例中并发执行
 *      当需要访问静态类变量或类以外的资源，就有可能引起同步问题
 *      同时，在servlet2.4之后，该接口已经被弃用
 *
 * org.apache.catalina.core.StandardWrapper
 *    该对象的主要任务是载入其所代表的servlet类，并实例化，
 *        同时该对象不能直接调用servlet的service方法，而是由基础阀StandardWrapperValue调用
 *    当第一次请求某个servlet类时，StandardWrapper载入servlet类
 *        由于StandardWrapper实例会动态载入类，所以StandardWrapper必须知道servlet类的完全限定类名，可以调用setServletClass()方法指定servlet类的完全限定名
 *    当StandardWrapperValue基础阀请求servlet实例是，StandardWrapper需要考虑servlet是否实现了SingleThreadModel接口
 *        没有实现SingleThreadModel接口的
 *            只会载入该类一次，并对所有请求返回该servlet类的同一个实例
 *            StandardWrapper实例不需要多个servlet实现，由程序员来负责同步对共享资源的访问
 *        对于一个STM servlet类（实现了SingleThreadModel接口）的
 *            StandardWrapper需要保证每个时刻只能有一个线程在执行STM servlet类的service()方法（synchronized）
 *            为了提升性能，StandardWrapper会维护一个STM servlet实例池
 *            Wrapper实例负责准备一个javax.servlet.servletConfig实例，后者在servlet实例内部可以获取
 *    分类servlet实例
 *        StandardWrapperValue的invoke方法调用了Wrapper实例的allocate()方法获取实例
 *        allocate()方法需要针对STM servlet和一般servlet处理不同
 *            布尔变量singleThreadModel用来表示该StandardWrapper实例表示的servlet类是否为STM servlet
 *            loadServlet()方法会检查其载入的servlet类是不是一个STM servlet类
 *            对于非STM servlet，使用wrapper对象的变量instance指代，为空时创建，非空每次都返回instance
 *            对于STM servlet，使用instancePool维护，
 *                  使用3个变量判断取用逻辑
 *                  nInstances        当前实例数量
 *                  countAllocated    已分配实例数量
 *                  maxInstances      最大实例数量
 *                  当countAllocated >= nInstances时，进入判断，否则跳过
 *                      当nInstances < maxInstances时，创建新实例
 *                      当nInstances >= maxInstances时，等待其他实例完成任务
 *                  从实例池中取出一个实例，并使countAllocated+1
 *     载入servlet类
 *        StandardWrapperValue的load()方法，调用loadServlet()方法，调用init()方法
 *        loadServlet()方法检查servlet对应的instance实例是否为STM servlet类，
 *            若是STM，且instance实例不为空，直接返回
 *            若instance为null或者是STM servlet类，则继续执行
 *        检查servlet是否为jsp页面，若是，获取该jsp页面代表的实际servlet类
 *            若从容器获取到了jsp页面对应的servlet，直接返回
 *            若未找到jsp页面的servlet，会使用servletClass的值对其进行载入
 *        获取载入器，进行servlet类的加载
 *        检查该servlet是否允许加载（安全检查）
 *        检查servlet是否为一个ContainerServlet容器，若是，将其添加到Context容器子容器中
 *        触发（生命周期）监听事件
 *
 * javax.servlet.ServletConfig接口
 *    StandardWrapper的loadServlet()方法在载入servlet类后，会调用该servlet实例的init()方法
 *    init()方法需要传入一个javax.servlet.ServletConfig实例作为参数
 *    StandardWrapper获取ServletConfig对象方式:
 *        StandardWrapper实现了javax.servlet.ServletConfig接口
 *        该接口有4个主要方法
 *        getInitParameterNames()           获取初始化参数名
 *        getInitParameter(String var1)     获取初始化参数
 *        getServletContext()               获取上下文
 *        getServletName()                  获取servlet名
 *    因StandardWrapper实现了ServletConfig接口
 *    因此在调用servlet的init()方法时，其可以将自己传入，
 *    但为了隐藏细节，StandardWrapper将自身包装成StandardWrapperFacade类的一个实例传入
 *
 * org.apache.catalina.core.StandardWrapperFacade类
 *    该类即StandardWrapper的包装类，对外部隐藏了StandardWrapper的细节
 *
 * org.apache.catalina.core.StandardWrapperValve类
 *    该类即StandardWrapper的基础阀，主要完成两个操作
 *    执行与该servlet实例关联的所有过滤器
 *    调用servlet实例的service方法
 *
 * org.apache.catalina.deploy.FilterDef类
 *    该类表示一个过滤器定义,其成员变量parameters指明了该过滤器的参数
 *
 * org.apache.catalina.core.ApplicationFilterConfig类
 *    该类实现了javax.servlet.FilterConfig接口
 *    用于管理web应用程序第1次启动时创建的所有过滤器实例
 *    其成员变量context表示web应用程序，filterDef表示一个过滤器
 *
 * org.apache.catalina.core.ApplicationFilterChain类
 *    该类实现了javax.servlet.FilterChain接口
 *    StandardWrapperValue的invoke()方法会创建一个该类的实例，并调用doFilter()方法
 *    ApplicationFilterChain类的doFilter()方法会将自身作为第三个参数传给过滤器的doFilter()方法
 *    在过滤器对象中，可以调用FilterChain对象的doFilter()方法，执行下一个过滤器，若没有调用doFilter，则不会调用后面的过滤器
 *
 */
public final class Bootstrap {
  public static void main(String[] args) {

  //invoke: http://localhost:8080/Modern or  http://localhost:8080/Primitive

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