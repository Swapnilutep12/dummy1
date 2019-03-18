package edu.utep.cs.mypricewatcher.file;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/** Sample file helper for feasibility; in practice, store in the external storage. */
public class FileHelper extends pricewatcher.model.FileHelper {
    private static final String filename = "items.txt";
    private Context context;

    public FileHelper(Context context) {
        this.context = context;
    }

    public Reader reader() {
        try (InputStream stream = context.openFileInput(filename)) {
             return new InputStreamReader(stream);
        } catch (IOException e) {
            e.printStackTrace(); // FIXME
        }
        return null;
    }

    public Writer writer() {
        try (OutputStream stream = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            return new OutputStreamWriter(stream);
        } catch (IOException e) {
            e.printStackTrace(); // FIXME
        }
        return null;
    }
}
