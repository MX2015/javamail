package testmail;

public class Main {

    public static void main(String[] args) {
        System.out.println("POP3Server Begin!");
        POP3Server POP3Server = new POP3Server();
        POP3Server.start();
        System.out.println("SMTPServer Begin!");
        smtpThread SMTPServer = new smtpThread();
        SMTPServer.start();
    }
}
