package ex03.pyrmont.connector.http;
import java.io.File;

/**
 * 常量类
 */
public final class Constants {
  /**
   * 静态资源和servlet存放的根路径
   */
  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  +  "webroot";
  /**
   * 包名
   */
  public static final String Package = "ex03.pyrmont.connector.http";
  /**
   * 超时时间
   */
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
  /**
   * 闲置数
   */
  public static final int PROCESSOR_IDLE = 0;
  /**
   * 启用的连接器个数
   */
  public static final int PROCESSOR_ACTIVE = 1;
}
