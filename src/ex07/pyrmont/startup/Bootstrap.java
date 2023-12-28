package ex07.pyrmont.startup;

import ex07.pyrmont.core.SimpleContext;
import ex07.pyrmont.core.SimpleContextLifecycleListener;
import ex07.pyrmont.core.SimpleContextMapper;
import ex07.pyrmont.core.SimpleLoader;
import ex07.pyrmont.core.SimpleWrapper;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;

/**
 * 相比ex06.pyrmont
 * 添加了日志记录器
 * 仅两个类发生了变化，分别是SimpleContext和Bootstrap
 * 日志的相关类在org.apache.catalina.logger包下
 *
 * Logger接口
 *    log()           记录日志
 *    setContainer()  设置容器
 *    getContainer()  获取关联的容器
 *
 * LoggerBase抽象类
 *    tomcat5的LoggerBase与Mbeans接口整合了(暂不考虑)，比较负责
 *    此处单指tomcat4的LoggerBase类，其实现了除log()方法外的所有方法
 *    该类中使用int类型变量verbosity变量指定日志等级，默认为ERROR(Error为Logger接口的公共静态变量，值为1)
 *    可使用
 *        setVerbosity(int verbosity)
 *        setVerbosityLevel(String verbosity)方法设置等级，
 *        setVerbosityLevel()可设置的值包括FATAL、ERROR、WARNING、INFORMATION、DEBUG
 *    该类重载了log()方法：
 *        log(Exception exception, String msg)
 *        log(String msg, Throwable throwable)
 *        log(String message, int verbosity)
 *        log(String message, Throwable throwable, int verbosity)
 *
 * Tomcat提供的3种日志记录器（均继承自LoggerBase）
 *
 * SystemOutLogger      一般输出日志
 *    提供了log(String msg)方法的实现，接收到的每条消息都会通过System.out.println()输出到控制台
 *
 * SystemErrorLogger    错误信息日志
 *    与SystemOutLogger类似，区别为接收到的消息会通过System.err.println()输出
 *
 * FileLogger           将日志写入文件
 *    相比前两个输出日志，该类较复杂
 *    其将从servlet容器中接收到的日志消息写到一个文件中，并选择是否为每条消息添加时间戳
 *    当该类被首次实例化时，会创建一个日志文件，文件名包含了当前的日期变化，若日期发生了变化会创建一个新文件
 *    并将所有消息写入到文件中
 *    使用该类时，可以在日志文件的名称中添加前缀和后缀
 *    在tomcat4中，该类实现了Lifecycle接口
 *    在tomcat5中，改为由LoggerBase实现Lifecycle
 *    该类在使用stop()方法时，会关闭打开的流
 *
 */
public final class Bootstrap {
  public static void main(String[] args) {

    Connector connector = new HttpConnector();

    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Loader loader = new SimpleLoader();

    Context context = new SimpleContext();
    context.addChild(wrapper1);
    context.addChild(wrapper2);

    Mapper mapper = new SimpleContextMapper();
    mapper.setProtocol("http");

    LifecycleListener listener = new SimpleContextLifecycleListener();
    ((Lifecycle) context).addLifecycleListener(listener);

    context.addMapper(mapper);
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    // ------ add logger --------
    System.setProperty("catalina.base", System.getProperty("user.dir"));
    FileLogger logger = new FileLogger();
    logger.setPrefix("FileLog_");
    logger.setSuffix(".txt");
    logger.setTimestamp(true);
    logger.setDirectory("webroot");
    context.setLogger(logger);

    //---------------------------

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