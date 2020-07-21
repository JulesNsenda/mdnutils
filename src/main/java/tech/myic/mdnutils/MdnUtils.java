package tech.myic.mdnutils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;

public class MdnUtils {

    private MdnUtils() {
    }

    public static boolean isMdnMessage(Message message) {
        ContentTypeField contentTypeField = Optional.ofNullable(message.getHeader())
                .map(h -> h.getField("content-type"))
                .map(f -> f.getBody())
                .map(s -> Fields.contentType(s))
                .orElse(null);

        if (contentTypeField == null) {
            return false;
        }

        if (!"multipart/report".equals(contentTypeField.getMimeType())) {
            return false;
        }

        if (!"disposition-notification".equals(contentTypeField.getParameter("report-type"))) {
            return false;
        }

        return true;
    }

    public static MdnData getMdnData(Message message)
            throws IOException {
        if (!isMdnMessage(message)) {
            return null;
        }

        MdnData retv = new MdnData();

        Body body = message.getBody();

        if (body instanceof Multipart) {
            Multipart messageMultipart = (Multipart) body;

            boolean seenText = false;
            boolean seenReport = false;

            for (Entity entity : messageMultipart.getBodyParts()) {
                if (!seenText) {
                    seenText = true;
                    Body entityBody = entity.getBody();
                    try {
                        if (entityBody instanceof TextBody) {
                            StringWriter sw = new StringWriter();
                            try (Reader r = ((TextBody) entityBody).getReader()) {
                                char[] buf = new char[1024];
                                int rd;
                                while ((rd = r.read(buf)) >= 0) {
                                    if (rd > 0) {
                                        sw.write(buf, 0, rd);
                                    }
                                }
                            }
                            retv.setDisplayText(sw.toString());
                        }
                    } finally {
                        entity.getBody().dispose();
                    }
                } else if (!seenReport && "message/disposition-notification".equals(entity.getMimeType())) {
                    seenReport = true;

                    Body entityBody = entity.getBody();
                    if (entityBody == null) {
                        continue;
                    }

                    try {
                        if (entityBody instanceof BinaryBody) {
                            BinaryBody bb = (BinaryBody) entityBody;

                            try (BufferedReader r = new BufferedReader(new InputStreamReader(bb.getInputStream(), StandardCharsets.US_ASCII))) {
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                boolean endOfInput = false;
                                boolean endOfHeader = false;
                                boolean seenPerMessageHeader = false;
                                while (!endOfInput) {
                                    String line = r.readLine();
                                    if (line == null) {
                                        endOfHeader = true;
                                        endOfInput = true;
                                    } else if (line.trim().isEmpty()) {
                                        endOfHeader = true;
                                    } else {
                                        baos.write(line.getBytes(StandardCharsets.US_ASCII));
                                        baos.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                                    }

                                    if (endOfHeader) {
                                        Header h = new DefaultMessageBuilder().parseHeader(new ByteArrayInputStream(baos.toByteArray()));
                                        if (!seenPerMessageHeader) {
                                            seenPerMessageHeader = true;
                                            retv.setReportingUa(
                                                    Optional.ofNullable(h.getField("Reporting-UA"))
                                                            .map(Field::getBody)
                                                            .map(s -> DecoderUtil.decodeEncodedWords(s, StandardCharsets.UTF_8))
                                                            .orElse(null)
                                            );

                                            Recipient s = new Recipient();

                                            s.setDisposition(
                                                    Optional.ofNullable(h.getField("Disposition"))
                                                            .map(f -> DecoderUtil.decodeEncodedWords(f.getBody(), StandardCharsets.UTF_8))
                                                            .orElse(null)
                                            );
                                            s.setFinalRecipient(
                                                    Optional.ofNullable(h.getField("Final-Recipient"))
                                                            .map(f -> DecoderUtil.decodeEncodedWords(f.getBody(), StandardCharsets.UTF_8))
                                                            .orElse(null)
                                            );

                                            retv.getRecipients().add(s);
                                        }
                                        endOfHeader = false;
                                        baos = new ByteArrayOutputStream();
                                    }
                                }
                            }
                        }
                    } finally {
                        entityBody.dispose();
                    }
                } else if ("text/rfc822-headers".equals(entity.getMimeType())) {
                    Body entityBody = entity.getBody();
                    if (entityBody == null) {
                        continue;
                    }

                    try {
                        Header h;

                        if (entityBody instanceof SingleBody) {
                            SingleBody bb = (SingleBody) entityBody;

                            try (InputStream in = bb.getInputStream()) {
                                DefaultMessageBuilder mb = new DefaultMessageBuilder();
                                mb.setMimeEntityConfig(MimeConfig.PERMISSIVE);
                                h = mb.parseHeader(in);
                            }

                            for (Field f : h.getFields()) {
                                retv.getHeaderValues().add(new HeaderValue(f.getName().toLowerCase(Locale.US), DecoderUtil.decodeEncodedWords(f.getBody(), StandardCharsets.UTF_8)));
                            }
                        }
                    } finally {
                        entityBody.dispose();
                    }
                }
            }
        }
        return retv;
    }

}
