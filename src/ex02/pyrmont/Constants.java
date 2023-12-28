package ex02.pyrmont;

import java.io.File;

/**
 * 系统常量
 *
 */
public class Constants {
  /**
   * WEB_ROOT是HTML和其他文件所在的目录。
   * 对于此属性，WEB_ROOT 是工作目录下的“webroot”目录。
   * 工作目录user.dir是文件系统中调用 java 命令的位置
   */
  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  +  "webroot";
}