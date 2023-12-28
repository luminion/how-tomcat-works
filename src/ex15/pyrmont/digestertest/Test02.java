package ex15.pyrmont.digestertest;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.digester.Digester;

public class Test02 {

  public static void main(String[] args) {
    String path = System.getProperty("user.dir") + File.separator  + "etc";
    File file = new File(path, "employee2.xml");
    Digester digester = new Digester();
    // add rules
    digester.addObjectCreate("employee", "ex15.pyrmont.digestertest.Employee");
    digester.addSetProperties("employee");

    digester.addObjectCreate("employee/office", "ex15.pyrmont.digestertest.Office");
    // 添加employee中的office属性
    digester.addSetProperties("employee/office");
    // 添加关联employee和office关联的方法，方法名为addOffice
    digester.addSetNext("employee/office", "addOffice");

    digester.addObjectCreate("employee/office/address", "ex15.pyrmont.digestertest.Address");
    // 添加office中的address属性
    digester.addSetProperties("employee/office/address");
    // 添加关联office和address关联的方法，方法名为setAddress
    digester.addSetNext("employee/office/address", "setAddress"); 
    try {
      Employee employee = (Employee) digester.parse(file);
      ArrayList offices = employee.getOffices();
      Iterator iterator = offices.iterator();
      System.out.println("-------------------------------------------------");
      while (iterator.hasNext()) {
        Office office = (Office) iterator.next();
        Address address = office.getAddress();
        System.out.println(office.getDescription());
        System.out.println("Address : " + 
          address.getStreetNumber() + " " + address.getStreetName());
        System.out.println("--------------------------------");
      }
      
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }  
}
