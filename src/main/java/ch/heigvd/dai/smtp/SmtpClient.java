package src.main.java.ch.heigvd.dai.smtp;

import src.main.java.ch.heigvd.dai.model.mail.Message;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class SmtpClient implements ISmtpClient {
    private static final Logger LOG = Logger.getLogger(SmtpClient.class.getName());
    private PrintWriter writer;
    private BufferedReader reader;
    private final String smtpServerAddr;
    private final int smtpServerPort;

    public SmtpClient(String smtpServerAddr, int smtpServerPortport) {
        this.smtpServerAddr = smtpServerAddr;
        this.smtpServerPort =smtpServerPortport;
    }

    private void readHeaderFromServer() throws IOException {
        String line = reader.readLine();
        if (!line.startsWith("250")) {
            throw new IOException("SMTP error: " + line);
        }
        while (line.startsWith("250-")){
            line = reader.readLine();
            LOG.info(line);
        }
    }

    private void readFromServer() throws IOException {
        String line = reader.readLine();
        LOG.info(line);
    }

    private void writeToServer(String line) throws IOException {
        writer.write(line + "\r\n");
        writer.flush();
    }


    @Override
    public void sendMessage(Message message) throws IOException {

        LOG.info("Sending message via SMTP");
        Socket socket = new Socket(smtpServerAddr, smtpServerPort);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        String line;

        readFromServer();

        // EHLO
        writeToServer("EHLO localhost");

        readHeaderFromServer();

        // MAIL FROM
        writeToServer("MAIL FROM:"+message.getFrom());

        readFromServer();

        // RCPT TO
        for (String to : message.getTo()) {
            writeToServer("RCPT TO:" + to);
            line = reader.readLine();
            LOG.info(line);
        }

        // START DATA
        writeToServer("DATA");

        readFromServer();

        writeToServer("Content-Type: text/plain; charset=utf-8");
        writeToServer("From: " + message.getFrom());

        writer.write("To: " + message.getTo()[0]);
        for (int i = 1; i < message.getTo().length; ++i) {
            writer.write(", " + message.getTo()[i]);
        }
        writer.write("\r\n");
        writer.flush();

        LOG.info("Subject: " + message.getBody());
        writeToServer("Subject: =?utf8?B?" + Base64.getEncoder().encodeToString(message.getSubject().getBytes()) + "?=");

        writer.write("\r\n");

        LOG.info(message.getBody());
        writer.write(message.getBody() + "\r\n");
        writeToServer(".");

        // END DATA

        readFromServer();

        writeToServer("QUIT");

        writer.close();
        reader.close();
        socket.close();
    }
}
