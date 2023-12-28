package ex15.pyrmont.digestertest;

import java.io.File;
import org.apache.commons.digester.Digester;

public class Test01 {

  public static void main(String[] args) {
    String path = System.getProperty("user.dir") + File.separator  + "etc";
    File file = new File(path, "employee1.xml");
    Digester digester = new Digester();
    // add rules
    // 创建的对象
    digester.addObjectCreate("employee", "ex15.pyrmont.digestertest.Employee");
    // 添加对象的名称属性
    digester.addSetProperties("employee");
    // 添加输出对象名称属性的方法
    digester.addCallMethod("employee", "printName");

    try {
      Employee employee = (Employee) digester.parse(file);
      System.out.println("First name : " + employee.getFirstName());
      System.out.println("Last name : " + employee.getLastName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }  
  
}
