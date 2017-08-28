package xyz.klinker.messenger.shared.data.model;

import android.database.Cursor;

import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DatabaseSQLiteHelper;
import xyz.klinker.messenger.encryption.EncryptionUtils;

/**
 * Data object for holding information about a contact
 */
public class Contact implements DatabaseSQLiteHelper.DatabaseTable {

    public static final String TABLE = "contact";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_COLOR_DARK = "color_dark";
    public static final String COLUMN_COLOR_LIGHT = "color_light";
    public static final String COLUMN_COLOR_ACCENT = "color_accent";
    public static final String COLUMN_ID_MATCHER = "id_matcher"; // created in database v9

    private static final String DATABASE_CREATE = "create table if not exists " +
            TABLE + " (" +
            COLUMN_ID + " integer primary key, " +
            COLUMN_PHONE_NUMBER + " varchar(255) not null, " +
            COLUMN_ID_MATCHER + " text not null, " +
            COLUMN_NAME + " varchar(255) not null, " +
            COLUMN_COLOR + " integer not null, " +
            COLUMN_COLOR_DARK + " integer not null, " +
            COLUMN_COLOR_LIGHT + " integer not null, " +
            COLUMN_COLOR_ACCENT + " integer not null" +
            ");";

    public long id;
    public String phoneNumber;
    public String idMatcher;
    public String name;
    public ColorSet colors = new ColorSet();

    public Contact() {

    }

    public Contact(ContactBody body) {
        this.phoneNumber = body.phoneNumber;
        this.name = body.name;
        this.colors.color = body.color;
        this.colors.colorDark = body.colorDark;
        this.colors.colorLight = body.colorLight;
        this.colors.colorAccent = body.colorAccent;
    }

    @Override
    public String getCreateStatement() {
        return DATABASE_CREATE;
    }

    @Override
    public String getTableName() {
        return TABLE;
    }

    @Override
    public String[] getIndexStatements() {
        return new String[0];
    }

    @Override
    public void fillFromCursor(Cursor cursor) {
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String column = cursor.getColumnName(i);

            if (column.equals(COLUMN_ID)) {
                this.id = cursor.getLong(i);
            } else if (column.equals(COLUMN_PHONE_NUMBER)) {
                this.phoneNumber = cursor.getString(i);
            } else if (column.equals(COLUMN_ID_MATCHER)) {
                this.idMatcher = cursor.getString(i);
            } else if (column.equals(COLUMN_NAME)) {
                this.name = cursor.getString(i);
            } else if (column.equals(COLUMN_COLOR)) {
                this.colors.color = cursor.getInt(i);
            } else if (column.equals(COLUMN_COLOR_DARK)) {
                this.colors.colorDark = cursor.getInt(i);
            } else if (column.equals(COLUMN_COLOR_LIGHT)) {
                this.colors.colorLight = cursor.getInt(i);
            } else if (column.equals(COLUMN_COLOR_ACCENT)) {
                this.colors.colorAccent = cursor.getInt(i);
            }
        }
    }

    @Override
    public void encrypt(EncryptionUtils utils) {
        this.phoneNumber = utils.encrypt(this.phoneNumber);
        this.name = utils.encrypt(this.name);
    }

    @Override
    public void decrypt(EncryptionUtils utils) {
        try {
            this.phoneNumber = utils.decrypt(this.phoneNumber);
            this.name = utils.decrypt(this.name);
        } catch (Exception e) {

        }
    }

}