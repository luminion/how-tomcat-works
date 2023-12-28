package ex02.pyrmont;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * 同ex01.pyrmont.HttpServer类
 * 引入了处理器processor的概念
 * 由processor对静态资源处理和servlet处理进行判断，并将由对应servlet处理
 */
public class HttpServer1 {

  /**
   * 关闭指令
   */
  private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

  /**
   * 是否循环监听请求(收到关闭指定后不再监听)
   */
  private boolean shutdown = false;

  public static void main(String[] args) {
    HttpServer1 server = new HttpServer1();
    server.await();
  }

  public void await() {
    // 创建套接字
    ServerSocket serverSocket = null;
    int port = 8080;
    try {
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

        // 创建ex02.pyrmont.Request请求对象，并转化参数
        Request request = new Request(input);
        request.parse();

        // 创建ex02.pyrmont.Request响应对象，并转化参数
        Response response = new Response(output);
        response.setRequest(request);

        // 根据请求路径判断请求的是静态资源还是servlet
        if (request.getUri().startsWith("/servlet/")) {
          ServletProcessor1 processor = new ServletProcessor1();
          // servlet处理器处理
          processor.process(request, response);
        }
        else {
          // 静态请求处理器处理
          StaticResourceProcessor processor = new StaticResourceProcessor();
          processor.process(request, response);
        }

        // 关闭连接
        socket.close();
        // 检查是否为关闭连接，若是，将shutdown变量设置为true
        shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
      }
      catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }
}
