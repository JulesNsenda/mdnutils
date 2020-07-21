package tech.myic.mdnutils;

import java.util.LinkedList;
import java.util.List;

public class MdnData {

    private String displayText;
    private String reportingUa;
    private final List<HeaderValue> headerValues = new LinkedList<>();
    private final List<Recipient> recipients = new LinkedList<>();

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public List<HeaderValue> getHeaderValues() {
        return headerValues;
    }

    public String getReportingUa() {
        return reportingUa;
    }

    public void setReportingUa(String reportingUa) {
        this.reportingUa = reportingUa;
    }
}
