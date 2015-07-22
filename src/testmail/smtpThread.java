package testmail;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Administrator on 2015/7/21.
 */
public class smtpThread extends Thread{
    @Override
    public void run() {
        try {
            ServerSocket smtpserver = new ServerSocket(25);
            Socket socket ;
            while(true){
                socket = smtpserver.accept();
                SmtpServer receive = new SmtpServer(socket) ;
                receive.start() ;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
