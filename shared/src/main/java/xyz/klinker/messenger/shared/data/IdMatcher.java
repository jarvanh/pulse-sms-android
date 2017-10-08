package xyz.klinker.messenger.shared.data;

import xyz.klinker.messenger.shared.data.model.Conversation;

public class IdMatcher {
    public String fiveLetter;
    public String sevenLetter;
    public String sevenLetterNoFormatting;
    public String eightLetter;
    public String eightLetterNoFormatting;
    public String tenLetter;

    public IdMatcher(String fiveLetter, String sevenLetter, String sevenLetterNoFormatting, String eightLetter, String eightLetterNoFormatting, String tenLetter) {
        this.fiveLetter = fiveLetter;
        this.sevenLetter = sevenLetter;
        this.sevenLetterNoFormatting = sevenLetterNoFormatting;
        this.eightLetter = eightLetter;
        this.eightLetterNoFormatting = eightLetterNoFormatting;
        this.tenLetter = tenLetter;
    }

    public String getDefault() {
        return eightLetterNoFormatting;
    }

    public String[] getAllMatchers() {
        return new String[] { fiveLetter, sevenLetter, sevenLetterNoFormatting, eightLetter, eightLetterNoFormatting, tenLetter };
    }

    public String getWhereClause() {
        return Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " +
                Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=? OR " +
                Conversation.COLUMN_ID_MATCHER + "=? OR " + Conversation.COLUMN_ID_MATCHER + "=?";
    }
}
