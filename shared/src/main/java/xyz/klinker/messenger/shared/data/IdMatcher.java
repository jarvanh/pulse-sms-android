package xyz.klinker.messenger.shared.data;

import xyz.klinker.messenger.shared.data.model.Conversation;

public class IdMatcher {
    public String fiveLetter;
    public String sevenLetter;
    public String eightLetter;
    public String tenLetter;

    public IdMatcher(String fiveLetter, String sevenLetter, String eightLetter, String tenLetter) {
        this.fiveLetter = fiveLetter;
        this.sevenLetter = sevenLetter;
        this.eightLetter = eightLetter;
        this.tenLetter = tenLetter;
    }

    public String getDefault() {
        return eightLetter;
    }

    public String[] getAllMatchers() {
        return new String[] { fiveLetter, sevenLetter, eightLetter, tenLetter };
    }

    public String getWhereClause() {
        return Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " +
                Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?";
    }
}
