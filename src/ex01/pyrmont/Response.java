package ex01.pyrmont;

import java.io.OutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

/*
  HTTP Response = Status-Line
    *(( general-header | response-header | entity-header ) CRLF)
    CRLF
    [ message-body ]
    Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
*/

/**
 * http1.1
 * 响应报文类
 *
 * 响应示例：
 *
 * HTTP/1.1 200 OK
 * Date: Sat, 31 Dec 2005 23:59:59 GMT
 * Content-Type: text/html;charset=ISO-8859-1
 * Content-Length: 122
 *
 * ＜html＞
 * ＜head＞
 * ＜title＞Work Homepage＜/title＞
 * ＜/head＞
 * ＜body＞
 * ＜!-- body goes here --＞
 * ＜/body＞
 * ＜/html＞
 */
public class Response {

  private static final int BUFFER_SIZE = 1024;
  /**
   * ,用于获取uri
   */
  Request request;
  /**
   * 从serverSocket获取的输出流
   */
  OutputStream output;

  /**
   * 创建响应对象，用于向客户端输出信息
   * @param output 输出流
   */
  public Response(OutputStream output) {
    this.output = output;
  }

  /**
   * 指定该响应对应的请求
   * @param request  ex01.pyrmont.Request请求对象
   */
  public void setRequest(Request request) {
    this.request = request;
  }

  /**
   * 检查指定请求路径中指定的文件是否存在
   * 若文件存在，则读取文件并输出
   * 若文件不存在，输出错误页面
   * @throws IOException  io
   */
  public void sendStaticResource() throws IOException {
    byte[] bytes = new byte[BUFFER_SIZE];
    FileInputStream fis = null;
    try {
      // 从uri中获取对应的本地文件
      File file = new File(HttpServer.WEB_ROOT, request.getUri());
      if (file.exists()) {
        // 文件存在，输出文件
        fis = new FileInputStream(file);
        int ch = fis.read(bytes, 0, BUFFER_SIZE);
        while (ch!=-1) {
          output.write(bytes, 0, ch);
          ch = fis.read(bytes, 0, BUFFER_SIZE);
        }
      }
      else {
        // file not found 文件不存在，输出错误信息 \r\n 为CRLF符号，用于指定换行
        String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
          "Content-Type: text/html\r\n" +
          "Content-Length: 23\r\n" +
          "\r\n" +
          "<h1>File Not Found</h1>";
        output.write(errorMessage.getBytes());
      }
    }
    catch (Exception e) {
      // thrown if cannot instantiate a File object 发生异常
      System.out.println(e.toString() );
    }
    finally {
      if (fis!=null)
        fis.close();
    }
  }
}