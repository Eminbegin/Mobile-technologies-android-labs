package ru.emink.contacts

import android.content.Context
import android.provider.ContactsContract

data class Contact(
    val name: String?,
    val phoneNumber: String?,
)

fun Context.fetchAllContacts(): List<Contact> {
    val columns = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )

    return contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        columns,
        null,
        null,
        null,
    ).use { cursor ->
        if (cursor == null) return emptyList()

        val nameColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

        return buildList {
            while (cursor.moveToNext()) {
                add(
                    Contact(
                        name = cursor.getString(nameColumn),
                        phoneNumber = cursor.getString(numberColumn),
                    ),
                )
            }
        }
    }
}
