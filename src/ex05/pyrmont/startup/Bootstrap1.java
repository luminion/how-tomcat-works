package ex05.pyrmont.startup;

import ex05.pyrmont.core.SimpleLoader;
import ex05.pyrmont.core.SimpleWrapper;
import ex05.pyrmont.valves.ClientIPLoggerValve;
import ex05.pyrmont.valves.HeaderLoggerValve;
import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;

/**
 * 仅包含一个servlet实例容器启动类
 * 使用了ex05.pyrmont.core包下的简单容器、简单上下文、简单管道等类
 * 核心为SimpleWrapper
 *
 * servlet容器概念
 * servlet容器是用来处理servlet资源，并为web客户端填充response对象的模块
 *
 * 所有容器都必须实现org.apache.catalina.Container接口
 * 然后使用连接器的setContainer()方法将容器传入连接器，
 * 连接器才能调用容器的invoke方法，
 * 容器使用invoke初始化servlet
 * servlet调用service处理业务逻辑
 *
 * tomcat中有4个级别的容器(对应同名接口)
 * 分别为：
 * Engine   表示整个Catalina Servlet引擎
 * Host     表示包含一个或多个Context容器的虚拟主机
 * Context  表示一个web应用程序，也就是上下文，一个Context可以有多个Wrapper
 * Wrapper  表示一个独立的servlet
 *
 * 部署功能性的Catalina并非必须包含4种容器，
 * 例如:
 * ex01中仅使用了一个wrapper
 * ex02中使用了一个Context和一个Wrapper
 * 本章（ex05）中不需要Host也不需要Wrapper
 *
 * 一个容器可以包含0个或多个子容器，Wrapper处于最底层，无法包含子容器
 * 可以使用Container接口的addChild()方法为当前容器添加子容器
 * 可以使用Container接口的removeChild()方法为当前容器移除子容器
 *
 * Container接口提供getter和setter方法将容器与组件关联，其中包括：
 * getLoader(),setLoader()
 * getLogger(),setLogger()
 * getManager(),setManager()
 * getRealm(),setRealm()
 * getResources(),setResources()
 * 在部署tomcat时，管理员可以通过编辑配置文件(server.xml)来决定使用那种容器(通过管道和阀的集合实现)
 *
 * 管道，及4个管道相关的接口：
 * Pipeline
 * Value
 * ValueContext
 * Contained
 *
 * 管道任务
 * 管道包含该servlet容器将要调用的任务，一个阀表示一个具体的执行任务，可以加入任意数量的阀
 * 阀分为基础阀和额外添加的阀，基础阀总是最后一个执行的
 * 阀的数量指额外添加的阀的数量（不包括基础阀）
 * 一个servlet容器可以有一条管道
 * 当调用容器的invoke()方法后，容器将任务交给管道处理，管道会依次调用其中的阀，直到所有的阀处理完成
 *      此处tomcat采用另一种实现方法，通过引入接口ValueContext来实现阀的遍历执行：
 *          当连接器调用容器的invoke()方法后，
 *          容器要执行的任务并没有硬编码在invoke()方法中
 *          而是调用容器对应管道的invoke()方法
 *          管道通过创建ValueContext接口实例来保证添加到其中所有的阀都必须至少执行一次
 *          ValueContext是作为管道的一个内部类来实现的
 *          因此ValueContext接口可以访问管道内的所有成员
 *          ValueContext接口最重要的方法时invokeNext()
 *          在ValueContext创建后，其会将自身传递给每个阀并执行阀
 *          因此每个阀执行完毕后都可以调用ValueContext的invokerNext()方法调用下一个阀
 *
 *       org.apache.catalina.core.StandardPipeline是servlet容器中Pipeline接口的实现
 *       该类有一个实现了ValueContext的内部类，名为StandardPipelineValveContext
 *       invokeNext()方法使用变量subscript标明正在调用的阀
 *       当第一调用invoke()方法时，subscript的值为0，stage为1，
 *       因此第一个阀会被调用（数组索引为0），管道中第一个阀接收ValueContext的实例，并调用其invokeNext()方法
 *       这时，subscript的值变为1，这样就会调用第二个阀
 *       最后一个阀调用时，subscript的值等于stage的值，便会调用基础阀
 *
 * Pipeline接口
 *    即管道，用于管理一组阀
 *    getBasic()       获取基础阀
 *    setBasic()       设置基础阀
 *    addValve()       添加阀（基础阀除外）
 *    getValves()      获取所有的阀（不包含基础阀）
 *    invoke()         依次调用管道中的阀
 *    removeValve()    移除管道中的阀
 *
 * Value接口
 *    即阀，被管道调用
 *    getInfo()     获取信息
 *    invoke()      调用当前阀
 *
 * ValueContext接口
 *    tomcat中，使用该接口来实现阀的遍历执行
 *    getInfo()     获取信息
 *    invokeNext()  调用下一个阀
 *
 * Contained接口
 *    阀可以选择实现该接口，用于关联上下文
 *    getContainer() 设置容器
 *    setContainer() 获取容器
 *
 *
 *
 * Wrapper接口
 *    wrapper级的servlet容器是一个独立的servlet实例，表示一个独立的servlet
 *    wrapper接口继承container接口，又添加了一些方法
 *    wrapper的实现类负责管理其基础servlet类的servlet生命周期
 *        即 init(),service(),destroy()等方法
 *    该容器不能添加子容器，添加时会抛出异常
 *    该接口中比较重要的方法：
 *    load()方法
 *        载入并初始化servlet类
 *    allocate()方法
 *        分配一个已初始化的servlet实例，且该方法还要考虑是否实现了SingleThread接口，
 * Context接口
 *    context接口表示一个应用程序的web应用程序，一个context可以有多个wrapper
 *    比较重要的方法：
 *    addWrapper()      添加一个wrapper对应的servlet实例
 *    createWrapper()   获取一个新创建的wrapper对应的servlet实例
 *
 *
 *
 *
 */
public final class Bootstrap1 {
  public static void main(String[] args) {

/* call by using http://localhost:8080/ModernServlet,
   but could be invoked by any name */
    // 创建连接器
    HttpConnector connector = new HttpConnector();
    // 创建容器
    Wrapper wrapper = new SimpleWrapper();
    // 指定容器对应的servlet名称
    wrapper.setServletClass("ModernServlet");
    // 用于加载servlet类的加载器
    Loader loader = new SimpleLoader();
    // 普通阀，用于输出请求头
    Valve valve1 = new HeaderLoggerValve();
    // 普通阀，用于输出客户端ip
    Valve valve2 = new ClientIPLoggerValve();

    // 将加载器和容器关联
    wrapper.setLoader(loader);
    // 向容器对应的管道中添加普通阀
    ((Pipeline) wrapper).addValve(valve1);
    ((Pipeline) wrapper).addValve(valve2);

    //设置连接器对应的容器
    connector.setContainer(wrapper);

    // 初始化启动连接器
    try {
      connector.initialize();
      connector.start();

      // make the application wait until we press a key.
      System.in.read();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}