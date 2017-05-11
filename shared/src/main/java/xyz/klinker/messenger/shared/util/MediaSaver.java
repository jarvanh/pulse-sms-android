package xyz.klinker.messenger.shared.util;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.model.Message;

public class MediaSaver {

    private static final int REQUEST_STORAGE_PERMISSION = 119;

    private Activity activity;
    private Context context;

    public MediaSaver(Activity activity) {
        this.activity = activity;
        this.context = activity;
    }

    public MediaSaver(Context context) {
        this.context = context;
    }

    public void saveMedia(long messageId) {
        DataSource source = DataSource.getInstance(context);
        source.open();
        Message message = source.getMessage(messageId);
        source.close();

        saveMedia(message);
    }

    public void saveMedia(Message message) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            saveMessage(message);
        } else {
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                saveMessage(message);
            }
        }
    }

    private void saveMessage(Message message) {
        final String directory = MmsSettings.get(context).saveDirectory;
        final String extension = MimeType.getExtension(message.mimeType);
        File dst = new File(directory,
                SimpleDateFormat.getDateTimeInstance().format(new Date(message.timestamp)) + extension);

        int count = 1;
        while (dst.exists()) {
            dst = new File(directory,
                    SimpleDateFormat.getDateTimeInstance().format(new Date(message.timestamp)) +
                            "-" + Integer.toString(count) + extension);
            count++;
        }

        try {
            dst.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (MimeType.isStaticImage(message.mimeType)) {
            try {
                Bitmap bmp = ImageUtils.getBitmap(context, message.data);
                FileOutputStream stream = new FileOutputStream(dst);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream);
                stream.close();

                bmp.recycle();

                ContentValues values = new ContentValues(3);

                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.Images.Media.MIME_TYPE, message.mimeType);
                values.put(MediaStore.MediaColumns.DATA, dst.getPath());

                context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.failed_to_save, Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                InputStream in = context.getContentResolver().openInputStream(Uri.parse(message.data));
                FileUtils.copy(in, dst);
                Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.failed_to_save, Toast.LENGTH_SHORT).show();
            }
        }
        
        updateMediaScanner(dst);
    }
    
    private void updateMediaScanner(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }
}
