package com.example.services;

import org.apache.commons.io.IOUtil;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Properties;

import static javax.mail.Transport.send;

@Component
public class SendMessages{
    private String sender = "xian04@yahoo.com";
    private String subject = "Timesheet Report";
    private String bodyText = "Please see the attached file for the timesheet report.";
    private String bodyHtml = "<h1>Timesheet Report</h1><p>Please see the attached file for the timesheet report.</p>";

    public void sendReport(InputStream is, String emailAddress) throws IOException {
        byte[] fileContent = IOUtil.toByteArray(is);

        try {
            send(fileContent, emailAddress);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] attachment, String emailAddress) throws MessagingException, IOException {
        MimeMessage message = null;
        Session session = Session.getDefaultInstance(new Properties());
        message = new MimeMessage(session);

        message.setFrom(new InternetAddress(sender));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
        message.setSubject(subject, "UTF-8");

        MimeMultipart msgBody = new MimeMultipart("alternative");
        MimeBodyPart wrap = new MimeBodyPart();

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(bodyText, "text/plain; charset=UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(bodyHtml, "text/html; charset=UTF-8");

        msgBody.addBodyPart(textPart);
        msgBody.addBodyPart(htmlPart);

        wrap.setContent(msgBody);

        MimeMultipart msg = new MimeMultipart("mixed");
        message.setContent(msg);
        msg.addBodyPart(wrap);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        DataSource fds = new ByteArrayDataSource(attachment, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        attachmentPart.setDataHandler(new DataHandler(fds));

        String reportName = "Timesheet Report.xls";
        attachmentPart.setFileName(reportName);
        msg.addBodyPart(attachmentPart);

        try {
            System.out.println("Attempting to send mail through Amazon SES...");
            Region region = Region.US_WEST_2;
            SesClient client = SesClient.builder()
                    .region(region)
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);
            ByteBuffer buffer = ByteBuffer.wrap(outputStream.toByteArray());
            byte[] arr = new byte[buffer.remaining()];
            buffer.get(arr);

            SdkBytes data = SdkBytes.fromByteArray(arr);
            RawMessage rawMessage = RawMessage.builder().data(data).build();
            SendRawEmailRequest rawRequest = SendRawEmailRequest.builder().rawMessage(rawMessage).build();
            client.sendRawEmail(rawRequest);
        } catch (SesException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        System.out.println("Mail sent!");
    }
}
