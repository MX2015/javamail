package testmail;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Created by Methol on 2015-07-21.
 * POP3服务器
 */
public class POP3Server extends Thread {
    Properties userlist = new Properties();  //用户信息配置文件

    public void run() {
        try {
            String userlistpath = System.getProperty("user.dir") + File.separator + "user.properties";
            InputStream is = new FileInputStream(userlistpath);
            userlist.load(is);
            is.close();
            ServerSocket popserver = new ServerSocket(110);
            Socket socket;
            while (true) {
                socket = popserver.accept();
                POP3Session receive = new POP3Session(socket);
                receive.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class POP3Session extends Thread {

        private static final String POP_USER = "USER";
        private static final String POP_PASS = "PASS";
        private static final String POP_STAT = "STAT";
        private static final String POP_LIST = "LIST";
        private static final String POP_RETR = "RETR";
        private static final String POP_DELE = "DELE";
        private final static String POP_QUIT = "QUIT";
        private final static String POP_OK = "+OK ";
        private final static String POP_ERR = "-ERR ";
        private final static String POP_UIDL = "UIDL";
        private final static String POP_CAPA = "CAPA";

        private String path = "d:\\mail\\";//存放邮件的路径
        private PrintWriter pw;
        private BufferedReader br;
        private Socket socket;

        private String name;
        private String password;

        private File[] file;  //存放收件箱所有邮件

        public POP3Session(Socket socket) {
            this.socket = socket;
        }

        //服务器发出的命令
        public void sendMessage(String message) {
            System.out.println("s:" + message);
            pw.println(message);
            pw.flush();//刷新流
        }

        //读取客户端的发来的命令
        public String readCommandLine() throws IOException {
            while (true) {
                String commandLine = br.readLine();
                if (commandLine != null) {
                    commandLine = commandLine.trim();
                    System.out.println("c:" + commandLine);
                }
                return commandLine;
            }
        }

        //欢迎信息
        public boolean sendWelcomeMessage() {
            sendMessage("+OK POP3 server ready");
            return true;
        }

        private boolean reciveCommand(String command) {
            boolean returnValue = true;
            if (command == null)
                return false;
            String argument = null;//空格 后一部分
            int spaceIndex = command.indexOf(" ");

            if (spaceIndex > 0) {
                argument = command.substring(spaceIndex + 1);
                command = command.substring(0, spaceIndex);//空格的前一部分
            }
            if (command.equalsIgnoreCase(POP_USER)) {
                doUSER(argument);
            } else if (command.equalsIgnoreCase(POP_PASS)) {
                doPass(argument);
            } else if (command.equalsIgnoreCase(POP_STAT)) {
                doStat();
            } else if (command.equalsIgnoreCase(POP_LIST)) {
                doList(argument);
            } else if (command.equalsIgnoreCase(POP_DELE)) {
                doDele(argument);
            } else if (command.equalsIgnoreCase(POP_RETR)) {
                if (argument == null) {
                    sendMessage(POP_ERR + " Mail number is required...");
                } else {
                    doRetr(argument);
                }
            } else if (command.equalsIgnoreCase(POP_QUIT)) {
                doQuit();
                return false;
            } else if (command.equalsIgnoreCase(POP_UIDL)) {
                doUIDL();
            }
//            else if (command.equalsIgnoreCase(POP_CAPA)) {
//                doCAPA();
//            }
            else {
                sendMessage(POP_OK);
                sendMessage(".");
            }
            return returnValue;
        }

        private void doUSER(String argument) {//user name user 123
            name = argument;
            password = userlist.getProperty(name);//通过名字找到密码来确定是否有这个账号
            if (password == null) {
                sendMessage(POP_ERR + "User can't Find");
            } else {
                sendMessage(POP_OK + name + " is a user");
            }
        }

        private void doPass(String argument) {
            if (password.equals(argument)) {
                sendMessage(POP_OK);
            } else {
                sendMessage(POP_ERR);
            }
        }

        private void doStat() {//邮箱中邮件的个数 以及大小  1 100
            File fileMail = new File(path + name + File.separator);
            if (!fileMail.exists()) {
                sendMessage(POP_ERR);
            } else {
                file = fileMail.listFiles();
                long size = 0;
                for (int i = 0; i < file.length; i++) {
                    size = size + file[i].length();
                }
                sendMessage(POP_OK + file.length + " " + size);  //返回文件总大小和总数量
            }
        }

        private void doList(String argument) {
            //先判断file数组是不是空的
            if (file == null) {
                File fileMail = new File(path + name + File.separator);
                if (!fileMail.exists()) {
                    sendMessage(POP_ERR);
                } else {
                    file = fileMail.listFiles();
                }
            }
            if (argument == null) {//list 查看全部文件
                sendMessage(POP_OK + " ");
                int len = file.length;
                for (int i = 0; i < len; i++)
                    sendMessage(i + 1 + " " + file[i].length());
                sendMessage(".");
            } else {// list 1  遍历第一封邮件
                int index = Integer.parseInt(argument);
                sendMessage(POP_OK + index + " " + file[index - 1].length());
            }
        }

        //查看邮件内容 Retr 1
        private void doRetr(String argument) {
            int index = Integer.parseInt(argument) - 1;
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(file[index]));
                String s = null;
                sendMessage(POP_OK + " ");
                while ((s = br.readLine()) != null) {
                    sendMessage(s);
                }
                sendMessage(".");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void doDele(String argument) {
            int index = Integer.parseInt(argument) - 1;
            File delefile = file[index];
            if (delefile.delete()) {
                sendMessage(POP_OK + "message " + argument + " deleted");
            } else {
                sendMessage(POP_ERR + "message " + argument + " already deleted");
            }
        }

        private void doQuit() {
            sendMessage(POP_OK + "ByeBye");
        }

        //返回唯一的标识 1 88888888888格式来确定是否有新的邮件需要接受
        private void doUIDL() {
            if (file == null) {
                File fileMail = new File(path + name + File.separator);
                if (!fileMail.exists()) {
                    sendMessage(POP_ERR);
                } else {
                    file = fileMail.listFiles();
                }
            }
            sendMessage(POP_OK + " ");
            for (int i = 0; i < file.length; i++) {
                sendMessage(i + 1 + " " + file[i].getName());
            }
            sendMessage(".");
        }

        private void doCAPA() {
            sendMessage(POP_OK);
            sendMessage("USER");
            sendMessage("UIDL");
            sendMessage(".");
        }

        public void run() {
            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
                sendWelcomeMessage();
                while (reciveCommand(readCommandLine())) {
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                    pw.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
