/* explains Tomcat's default container */
package ex04.pyrmont.startup;

import ex04.pyrmont.core.SimpleContainer;
import org.apache.catalina.connector.http.HttpConnector;

/**
 * 服务器启动类
 * Tomcat的默认连接器
 * Tomcat的连接器需要实现org.apache.catalina.Connector接口
 * 接口中最重要的4个方法为：
 *
 * setContainer(Container container)
 *    将连接器和某个servlet关联
 * getContainer()
 *    返回与当前连接器相关联的servlet
 * createRequest()
 *    创建请求对象
 * createResponse()
 *    创建响应对象
 *
 * HttpConnector是HTTP协议1.1的实现
 *  连接器的initialize()方法会创建serverSocket套接字
 *  连接器内部变量processors（栈），维护了一组处理器，避免了创建处理器的开销
 *      processors维护的处理器的数量由minProcessors和maxProcessors决定（默认5，20）
 *      初始创建minProcessors数量的处理器，当处理器超出处理范围时创建新的处理器
 *      若需要不断创建新的处理器，将maxProcessors设置为负数即可
 *      已创建的处理器数量由curProcessor记录
 *      此处的所有processor都实现了Runnable()接口，使其运行在自己的线程中，称为处理器线程
 *      连接器的newProcessor()方法负责创建新的处理器,创建后调用其start()方法使其运行在独立线程中
 *      当处理器线程启动时，会while循环调用处理器对象的await()方法处理业务逻辑
 *      连接器的recycle(Processor processor)方法负责将处理器对象入栈
 *
 *  连接器的主要业务逻辑运行在run()方法
 *    循环监听serverSocket，
 *    当有新的连接建立时，设置默认超时时间等默认值
 *    调用createProcessor()方法获取处理器 (并非直接创建，而是优先从processors处理器池中获取)
 *    调用处理器的assign(Socket socket)方法，并传入socket处理请求
 *       此时被调用的processor处理器对象上有两个线程在工作，处理器线程和连接器线程;
 *       连接器线程执行assign()方法
 *       处理器线程执行await()方法
 *       这两个方法均为synchronized同步方法
 *       线程之间通过使用while循环判断布尔变量available来执行
 *
 *       连接器线程：
 *       assign()方法被调用后，判断available，
 *       若为true，则代表正在执行任务，在此处wait()
 *       若为false，则代表未执行任务，此时：
 *          将socket对象赋值给processor的socket变量，将available改为true，使用notifyAll()唤醒正在等待的线程（实际上就是处理器线程）;
 *
 *       处理器线程：
 *       该线程在创建后，运行时
 *       run()方法会不断循环调用await()方法，await()方法会返回一个socket
 *       await()方法被调用后，判断available，
 *       若为false，则代表当前没有需要执行的业务，在此处wait()
 *       若为true，则代表当前有需要执行的业务，此时：
 *          将available改为false
 *          使用notifyAll()唤醒正在等待的线程（实际上就是连接器线程）
 *          将本类的socket变量的指向的对象返回给run()方法
 *          run()方法调用process()方法处理请求
 *          process()处理完毕后，调用连接器的recycle()方法将自己回收到连接器的连接池中
 *          .....唤醒其他需要通知的线程(例如监听器)
 *          run()方法再次循环调用await()方法.....
 *
 *       处理器的process()方法：
 *       布尔变量ok表示是否有错误发生
 *       布尔变量finishResponse表示是否应该调用Response接口的finishResponse()方法
 *       布尔变量keepAlive表示是否为持久连接
 *       http11表示是否为http1.1版本
 *       使用SocketInputStream来包装inputStream，该流接受一个指定大小缓冲区的参数（来自连接器）
 *       读取SocketInputStream中的所有数据
 *       调用parseConnection(),parseRequest(),parseHeaders()方法，对连接，请求、请求头进行封装
 *       （在解析的过程中会出现很多异常，任何一个异常都会将变量ok和finishResponse设置为false）
 *       完成解析后，处理器将request和response对象作为参数传入servlet容器的invoke()方法
 *       若finishResponse为true，则调用Response接口的finishResponse()方法
 *
 *       处理器的parseConnection()方法：
 *       用于解析连接，从套接字中获取internet地址，将其赋值给HttpRequestImpl对象，此外还检查是否使用代理，将socket对象赋值给request对象
 *
 *       处理器的parseRequest(),parseHeaders()方法：
 *       同ex03.pyrmont.connector.http.HttpProcessor类似
 *       不过parseHeaders()没有使用HttpHeader的字符串，而是使用DefaultHeader的字符数组，减少系统开销
 *       若所有的请求头都已经读取过了，则readHeader()方法不会再给HttpHeader示例设置name属性，此时就可以退出parseHeaders()方法
 *
 */
public final class Bootstrap {
  public static void main(String[] args) {
    // 创建连接器
    HttpConnector connector = new HttpConnector();
    SimpleContainer container = new SimpleContainer();
    connector.setContainer(container);
    try {
      connector.initialize();
      connector.start();

      // make the application wait until we press any key.
      System.in.read();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}