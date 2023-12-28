package ex02.pyrmont;

import java.io.IOException;

/**
 * 静态资源处理类
 */
public class StaticResourceProcessor {

  public void process(Request request, Response response) {
    try {
      response.sendStaticResource();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}