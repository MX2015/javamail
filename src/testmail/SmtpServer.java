package testmail;

import util.UtilBase64;
import util.UtilDate;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Administrator on 2015/7/21.
 */
public class SmtpServer extends Thread {
    Properties userlist = new Properties();  //用户信息配置文件
    //smtp邮件服务器的规则
    private static final String GET_HELO = "HELO";
    private static final String GET_MAIL = "MAIL";
    private static final String GET_EHLO = "EHLO";
    private static final String GET_RCPT = "RCPT";
    private static final String GET_DATA = "DATA";
    private final static String GET_QUIT = "QUIT";
    private final static String GET_AUTH = "AUTH";
    private static final String GET_POINT = ".";

    private String path = "d:\\mail\\";//文件的存放位置

    private Account FromAccount;//发件人
    private Account ToAccount;//收件人
    private File ToFile;//收件人目录

    //数据通信用的流
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;


    public SmtpServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            String userlistpath = System.getProperty("user.dir") + File.separator + "user.properties";
            InputStream is = new FileInputStream(userlistpath);
            userlist.load(is);
            is.close();

            socket.setSoTimeout(30000);//超时
            //读和写
            br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
            //服务器发送欢迎信息给客户端
            sendWelcomeMessage();
            //
            while (parseCommand(readCommandLine())) {
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //发送欢迎信息给客户端
    private boolean sendWelcomeMessage() {
        //220 统一规定
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("220").append(" ").append(InetAddress.getLocalHost()).append("\t" + "WELCOME");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        sendMessage(sb.toString());
        return true;
    }

    //发送消息
    public void sendMessage(String msg) {
        System.out.println("s:" + msg);
        pw.println(msg);
        pw.flush();
    }

    //读取一行信息
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

    //校验信息
    private boolean parseCommand(String command) {
        boolean returnValue = true;
        if (command == null)//为空直接返回
            return false;

        String argument = null;//信息的后部分

        int spaceIndex = command.indexOf(" "); //helo 19

        if (spaceIndex > 0) {
            argument = command.substring(spaceIndex + 1);
            command = command.substring(0, spaceIndex);
        }

        if (command.equalsIgnoreCase(GET_HELO))
            doHELO(argument);
        else if (command.equalsIgnoreCase(GET_EHLO)) {
            doEHLO(argument);
        } else if (command.equalsIgnoreCase(GET_MAIL)) {
            doMail(argument);
        } else if (command.equalsIgnoreCase(GET_RCPT))
            doRCPT(argument);

        else if (command.equalsIgnoreCase(GET_DATA)) {
            doData(argument);
        } else if (command.equalsIgnoreCase(GET_QUIT)) {
            doQuit(argument);
            returnValue = false;
        } else if (command.equalsIgnoreCase(GET_AUTH)) {
            doAUTH(argument);
        }
        return returnValue;
    }

    private void doAUTH(String argument) {
//            System.out.println(GET_AUTH);
        if (argument != null) {
            if (argument.equalsIgnoreCase("login")) {  //处理anth login  请求
                FromAccount = new Account();
                try {
                    sendMessage("334 " + UtilBase64.encryptBASE64("username:".getBytes()));
                    String mailaddress = new String(UtilBase64.decryptBASE64(br.readLine().trim()));  //解密name
                    System.out.println("mailaddress"+mailaddress);
                    int index = mailaddress.indexOf("@");
                    FromAccount.name = mailaddress.substring(0, index); // 取出<@之间的，即用户名
                    FromAccount.dir = mailaddress.substring(index + 1); // 取出@>之间的，即域名
                    sendMessage("334 " + UtilBase64.encryptBASE64("password:".getBytes()));
                    FromAccount.password = new String(UtilBase64.decryptBASE64(br.readLine().trim()));  //解密name
//                        System.out.println("name:" + fromuser.name);
//                        System.out.println("password:" + fromuser.password);
                    //用户账户密码判断
                    if (userlist.getProperty(FromAccount.name).equals(FromAccount.password)) {
                        sendMessage("235 Authentication successful");
                    } else {
                        sendMessage("535 Authentication failed");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doEHLO(String argument) {
        sendMessage("250-localhost");
        sendMessage("250-PIPELINING");
        sendMessage("250-SIZE 73400320");
        sendMessage("250-AUTH LOGIN PLAIN");
        sendMessage("250-AUTH=LOGIN");
        sendMessage("250-MAILCOMPRESS");
        sendMessage("250-8BITMIME");
        sendMessage("250 STARTTLS");
    }

    //服务器返回helo信息
    private void doHELO(String argument) {
        StringBuffer responseBuffer = new StringBuffer();
        responseBuffer.append("250 Welcome!").append(" ").append(argument);
        sendMessage(responseBuffer.toString());
    }

    public void doMail(String argument) {
        FromAccount = new Account();
        StringBuffer responseBuffer = new StringBuffer();
        if (argument == null) {
            sendMessage("501 the wrong argument");
        }
        int index1 = argument.indexOf("<");
        int index2 = argument.indexOf("@");
        int index3 = argument.indexOf(">");                             //邮箱<875729140@qq.com>
        FromAccount.name = argument.substring(index1 + 1, index2); // 取出<@之间的 用户名 875729140
        FromAccount.dir = argument.substring(index2 + 1, index3); // 取出@>之间的 qq.com
        responseBuffer.append("250 OK!");
        sendMessage(responseBuffer.toString());
    }

    //发送邮件 指定的邮箱  rcpt <875729140@qq.com>
    private void doRCPT(String argument) {
        ToAccount = new Account();
        StringBuffer responseBuffer = new StringBuffer();
        if (argument == null) {
            sendMessage("501 the wrong argument");
        }
        int index1 = argument.indexOf("<");
        int index2 = argument.indexOf("@");
        int index3 = argument.indexOf(">");
        ToAccount.name = argument.substring(index1 + 1, index2);
        ToAccount.dir = argument.substring(index2 + 1, index3);
        ToFile = new File(path + "\\" + ToAccount.name + File.separator);//接收者目录
        if (!ToFile.exists()) ToFile.mkdirs();//没有就创建一个接受者目录
        responseBuffer.append("250 OK!");
        sendMessage(responseBuffer.toString());
    }

    // 书写邮件正文 data
    private void doData(String argument) {
        sendMessage("354 Start mail input,End data with .");
        File tofile = new File(path + ToAccount.name + File.separator + UtilDate.getOrderNum() + ".txt");  //生成该邮件文件
        PrintWriter filepw = null;
        try {
            filepw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tofile), "utf-8"), true);
            filepw.println("From: " + FromAccount.name + "@" + FromAccount.dir);
            filepw.println("To: " + ToAccount.name + "@" + ToAccount.dir);
            filepw.println("Date: " + new Date());
            String line = br.readLine().trim();
            while (!line.equals(GET_POINT)) {  //接受邮件数据 不为点是继续书写 当一行只有一个点是 退出
                filepw.println(line);
                line = br.readLine().trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (filepw != null) {
                filepw.close();
            }
        }
        sendMessage("250 Ok");
    }


    //退出 quit
    private void doQuit(String argument) {
        StringBuffer responseBuffer = new StringBuffer();
        responseBuffer.append("221 ").append("GET_QUIT").append("\tOK")
                .append("\tGoodBye!");
        sendMessage(responseBuffer.toString());
    }

    class Account {
        public String name;//账号
        public String password;
        public String dir;//邮箱
    }
}

