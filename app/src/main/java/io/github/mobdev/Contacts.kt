package io.github.mobdev

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.database.getStringOrNull

data class Contact(val name: String?, val phoneNumber: String?, val email: String?)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")
    return contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    ).use { cursor: Cursor? ->
        if (cursor == null) return emptyList()
        buildList {
            while (cursor.moveToNext()) {
                val name = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val phoneNumber = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                val contactId = cursor.getStringOrNull(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                )
                val email = contactId?.let {
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(it),
                        null
                    )?.use { emailCursor ->
                        if (emailCursor.moveToFirst())
                            emailCursor.getStringOrNull(
                                emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                            )
                        else null
                    }
                }
                add(Contact(name, phoneNumber, email))
            }
        }
    }
}