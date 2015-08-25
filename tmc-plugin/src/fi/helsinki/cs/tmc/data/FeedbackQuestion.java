package fi.helsinki.cs.tmc.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedbackQuestion {
    private static final Pattern intRangeRegex =
            Pattern.compile("^intrange\\[(-?\\d+)\\.\\.(-?\\d+)\\]$");

    private int id;
    private String question;
    private String kind;

    public FeedbackQuestion() {}

    public FeedbackQuestion(int id, String question, String kind) {
        this.id = id;
        this.question = question;
        this.kind = kind;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public boolean isText() {
        return kind.equals("text");
    }

    public boolean isIntRange() {
        return intRangeMatcher().matches();
    }

    public int getIntRangeMin() {
        Matcher matcher = intRangeMatcher();
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new IllegalStateException("Not an intrange");
        }
    }

    public int getIntRangeMax() {
        Matcher matcher = intRangeMatcher();
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        } else {
            throw new IllegalStateException("Not an intrange");
        }
    }

    private Matcher intRangeMatcher() {
        return intRangeRegex.matcher(kind);
    }
}
