/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tech.myic.mdnutils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;

/**
 *
 * @author jules
 */
public class Main {

    public static void main(String[] args) throws IOException {
        try (InputStream in = getSampleEmailInputStream("/home/jules/Downloads/success-mdn.eml")) {
            Message m = new DefaultMessageBuilder().parseMessage(in);
            try {
                if (MdnUtils.isMdnMessage(m)) {
                    System.out.println("Is MDN message");
                } else {
                    System.out.println("Is not MDN message");
                }
            } finally {
                m.dispose();
            }
        }

        try (InputStream in = getSampleEmailInputStream("/home/jules/Downloads/success-mdn.eml")) {
            Message m = new DefaultMessageBuilder().parseMessage(in);
            try {
                MdnData d = MdnUtils.getMdnData(m);

                String dt = d.getDisplayText();
                System.out.println("Display text: " + dt);

                String rua = d.getReportingUa();
                System.out.println("Reporting UA: " + rua);

                int rec = d.getRecipients().size();
                System.out.println("Recipient size: " + rec);

                Recipient s = d.getRecipients().get(0);
                System.out.println("Final recipient: " + s.getFinalRecipient());
                System.out.println("Disposition: " + s.getDisposition());

                List<HeaderValue> headerValues = d.getHeaderValues();

                System.out.println("Header size: " + headerValues.size());

                System.out.println("Header name: " + headerValues.get(0).getName());
                System.out.println("Header value: " + headerValues.get(0).getValue());
            } finally {
                m.dispose();
            }
        }
    }

    private static InputStream getSampleEmailInputStream(String name)
            throws IOException {
        InputStream in = new FileInputStream(name);

        if (in == null) {
            throw new IOException("Sample email not found: " + name);
        }

        return in;
    }
}
