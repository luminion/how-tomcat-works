package ex16.pyrmont.shutdownhook;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

/**
 * 关闭钩子
 *    用户关闭应用程序时候，需要做一个善后清理工作
 *    有时用户不会按照推荐方法关闭应用程序，会突然退出
 *
 *    在java中，虚拟机会对两类事件进行相应，然后执行关闭操作
 *      1.调用System.exit()，或程序的租后一个非守护线程退出时，正常退出
 *      2.用户突然强制虚拟机中断运行，例如ctrl+c或未关闭java程序退出系统
 *    虚拟机在关闭时，会经过以下两个阶段
 *      1.启动所有已经注册的关闭钩子，
 *        如果有的话，关闭钩子是先前已经通过Runtime类注册的线程，
 *        所有的关闭钩子会并发执行，直到完成任务
 *      2.虚拟机根据情况调用所有没有被调用过的终结器(finalizer)
 *
 *    此处着重说明关闭钩子的操作
 *        1.创建Thread类的一个子类
 *        2.实现你自己的run()方法，当应用程序(正常或突然)关闭时，调用此方法
 *        3.在应用程序中，实例化关闭钩子类
 *        4.实用当前Runtime类的addShutdownHook()方法注册关闭钩子
 *    此处不需要像其他现场调用关闭钩子的start()方法，虚拟机会在它运行其关闭序列时启动并执行关闭钩子
 *
 * org.apache.catalina.startup.Catalina.CatalinaShutdownHook类
 *    该类为org.apache.catalina.startup.Catalina的内部类(Catalina类为启动tomcat时用到的两个类)
 *    该类是Tomcat中的关闭钩子，Catalina的start()方法中会实例化该类并注册到Runtime中
 *
 */
public class MySwingApp extends JFrame {
  JButton exitButton = new JButton();
  JTextArea jTextArea1 = new JTextArea();
  String dir = System.getProperty("user.dir");
  String filename = "temp.txt";

  public MySwingApp() {
    //
    exitButton.setText("Exit");
    exitButton.setBounds(new Rectangle(304, 248, 76, 37));
    exitButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        exitButton_actionPerformed(e);
      }
    });
    this.getContentPane().setLayout(null);
    jTextArea1.setText("Click the Exit button to quit");
    jTextArea1.setBounds(new Rectangle(9, 7, 371, 235));
    this.getContentPane().add(exitButton, null);
    this.getContentPane().add(jTextArea1, null);
    this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    this.setBounds(0,0, 400, 330);
    this.setVisible(true);
    initialize();
  }

  private void initialize() {
    // 添加关闭的钩子(若未添加此钩子，不通过exit按钮关闭时，不会删除临时文件)
    SwingHook hook=new SwingHook();
    Runtime.getRuntime().addShutdownHook(hook);

    // create a temp file
    // 创建临时文件
    File file = new File(dir, filename);
    try {
      System.out.println("Creating temporary file");
      file.createNewFile();
    }
    catch (IOException e) {
      System.out.println("Failed creating temporary file.");
    }
  }
  
  private void shutdown() {
    // delete the temp file
    // 删除临时文件
    File file = new File(dir, filename);
    if (file.exists()) {
      System.out.println("Deleting temporary file.");
      file.delete();
    }
  }

  void exitButton_actionPerformed(ActionEvent e) {
    shutdown();
    System.exit(0);
  }


  private class SwingHook extends Thread{
    @Override
    public void run() {
      shutdown();
    }
  }


  public static void main(String[] args) {
    //创建临时文件并删除
    MySwingApp mySwingApp = new MySwingApp();
  }
}
