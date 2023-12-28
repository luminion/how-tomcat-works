package ex16.pyrmont.shutdownhook;


/**
 * 关闭钩子的雏形
 * @program: Test
 * @packagename: ex16.pyrmont.shutdownhook
 * @author: booty
 * @date: 2021-11-17 14:20
 **/
public class ShutDownHookDemo {

    public static void main(String[] args) {
        ShutDownHookDemo demo =new ShutDownHookDemo();
        demo.start();
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void start(){
        System.out.println("Demo start");
        ShutdownHook hook=new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);
    }

    class ShutdownHook extends Thread{
        @Override
        public void run() {
            System.out.println("shutting down hook work");
        }
    }
}
