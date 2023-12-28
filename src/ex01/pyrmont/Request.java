package ex01.pyrmont;

import java.io.InputStream;
import java.io.IOException;

public class Request {
  /**
   * 从serverSocket获取的输入流，用于解析数据
   */
  private InputStream input;
  /**
   * 请求uri路径
   */
  private String uri;

  /**
   * 创建请求对象，用于封装参数
   * @param input 输入流
   */
  public Request(InputStream input) {
    this.input = input;
  }

  /**
   * 获取请求数据
   * 并将请求转化为uri存储在本类uri变量中
   */
  public void parse() {
    // 从请求的InputStream中读取数据，并将其拼接为字符串
    StringBuffer request = new StringBuffer(2048);
    int i;
    byte[] buffer = new byte[2048];
    try {
      i = input.read(buffer);
    }
    catch (IOException e) {
      e.printStackTrace();
      i = -1;
    }
    for (int j=0; j<i; j++) {
      // 拼串
      request.append((char) buffer[j]);
    }
    // 请求的全部文字数据
    System.out.print(request.toString());
    // 转化uri
    uri = parseUri(request.toString());
  }

  /**
   * 获取请求的uri
   * uri存在于请求行的第一行中，
   * 获取请求的第一个空格和第二个空格
   * 这两个空格之间的数据就是uri
   * 格式：
   *
   * POST /examples/default.jsp HTTP/1.1
   * Accept: application/json,
   * Accept-Language: zh-cn
   * Accept-Encoding: gzip, deflate
   *
   * lastname=booty firstname=star
   *
   *
   * @param requestString 请求的所有字符串
   * @return uri
   */
  private String parseUri(String requestString) {
    int index1, index2;
    index1 = requestString.indexOf(' ');
    if (index1 != -1) {
      index2 = requestString.indexOf(' ', index1 + 1);
      if (index2 > index1)
        return requestString.substring(index1 + 1, index2);
    }
    return null;
  }

  public String getUri() {
    return uri;
  }

}