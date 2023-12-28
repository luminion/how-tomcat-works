package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 请求连接器
 * 用于等待请求的连接
 * 当请求连接后，
 * 将请求交给ex03.pyrmont.connector.http.HttpProcessor处理器处理
 * 这样在请求被servlet处理器不会进行解析，可以提高连接效率
 *
 */
public class HttpConnector implements Runnable {

  boolean stopped;
  private String scheme = "http";

  public String getScheme() {
    return scheme;
  }

  public void run() {
    ServerSocket serverSocket = null;
    int port = 8080;
    try {
      serverSocket =  new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    while (!stopped) {
      // Accept the next incoming connection from the server socket
      Socket socket = null;
      try {
        socket = serverSocket.accept();
      }
      catch (Exception e) {
        continue;
      }
      // Hand this socket off to an HttpProcessor
      HttpProcessor processor = new HttpProcessor(this);
      processor.process(socket);
    }
  }

  public void start() {
    Thread thread = new Thread(this);
    thread.start();
  }
}