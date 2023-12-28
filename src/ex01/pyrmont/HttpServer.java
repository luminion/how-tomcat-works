package ex01.pyrmont;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

/**
 * 最基础的http服务器
 * 该服务器用于处理静态资源
 * 使用http1.1协议
 */
public class HttpServer {

  /**
   * WEB_ROOT是HTML和其他文件所在的目录。
   * 对于此属性，WEB_ROOT 是工作目录下的“webroot”目录。
   * 工作目录user.dir是文件系统中调用 java 命令的位置
   */
  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  +  "webroot";

  /**
   * 关闭系统的指定（链接）
   */
  private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

  /**
   * 是否循环监听请求(收到关闭指定后不再监听)
   */
  private boolean shutdown = false;

  public static void main(String[] args) {
    HttpServer server = new HttpServer();
    server.await();
  }

  public void await() {
    // 创建套接字
    ServerSocket serverSocket = null;
    int port = 8080;
    try {
      // 监听本地链接，请求的传入连接队列的最大长度指定为1
      serverSocket =  new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    // 当shutdown为false时，循环监听请求
    while (!shutdown) {
      Socket socket = null;
      InputStream input = null;
      OutputStream output = null;
      try {
        socket = serverSocket.accept();
        input = socket.getInputStream();
        output = socket.getOutputStream();

        // 创建ex01.pyrmont.Request请求对象，并转化参数
        Request request = new Request(input);
        request.parse();

        // 创建ex01.pyrmont.Request响应对象，并转化参数
        Response response = new Response(output);
        response.setRequest(request);
        response.sendStaticResource();

        // 关闭本次连接
        socket.close();

        // 检查是否为关闭连接，若是，将shutdown变量设置为true
        shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
      }
      catch (Exception e) {
        e.printStackTrace();
        continue;
      }
    }
  }
}
