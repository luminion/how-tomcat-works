package ex03.pyrmont.startup;

import ex03.pyrmont.connector.http.HttpConnector;

/**
 * 服务器启动类
 * 引入了连接器的概念，
 * 由连接器接受请求，
 * 接受请求后将请求交给处理器处理
 * 如此分开执行可提高效率
 */
public final class Bootstrap {
  public static void main(String[] args) {
    // 创建连接器
    HttpConnector connector = new HttpConnector();
    connector.start();
  }
}