package com.example.engine

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class PhoneContactItem(
    val id: String,
    val name: String,
    val number: String,
    val source: ContactSource, // CONTACT_BOOK, RECENT_CALL_LOG, LIVE_INPUT
    val callType: String? = null, // INCOMING, OUTGOING, MISSED
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

enum class ContactSource {
    CONTACT_BOOK,
    RECENT_CALL_LOG,
    LIVE_INPUT
}

object PhoneCallManager {

    fun hasPermissions(context: Context): Boolean {
        val contactsGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val callLogGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        return contactsGranted && callLogGranted
    }

    /**
     * Reads saved contacts from the Android Contacts Provider.
     * Never returns hardcoded pre-defined numbers.
     */
    fun fetchSavedContacts(context: Context): List<PhoneContactItem> {
        val contactsList = mutableListOf<PhoneContactItem>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) else ""
                    val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Contact" else "Contact"
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""

                    val cleanNum = number.replace("\\s".toRegex(), "")
                    if (cleanNum.isNotEmpty() && !seenNumbers.contains(cleanNum)) {
                        seenNumbers.add(cleanNum)
                        contactsList.add(
                            PhoneContactItem(
                                id = "ct_$id",
                                name = name,
                                number = number,
                                source = ContactSource.CONTACT_BOOK
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return contactsList
    }

    /**
     * Reads recent incoming/outgoing/missed calls from the Android Call Log.
     * Never returns hardcoded pre-defined numbers.
     */
    fun fetchRecentCallLogs(context: Context): List<PhoneContactItem> {
        val callLogs = mutableListOf<PhoneContactItem>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        try {
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (it.moveToNext() && count < 30) {
                    val id = if (idIdx != -1) it.getString(idIdx) else ""
                    val rawName = if (nameIdx != -1) it.getString(nameIdx) else null
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "Unknown" else "Unknown"
                    val name = if (!rawName.isNullOrBlank()) rawName else "Caller ($number)"
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                    val typeInt = if (typeIdx != -1) it.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val duration = if (durIdx != -1) it.getInt(durIdx) else 0

                    val typeStr = when (typeInt) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Blocked"
                        else -> "Call"
                    }

                    callLogs.add(
                        PhoneContactItem(
                            id = "cl_$id",
                            name = name,
                            number = number,
                            source = ContactSource.RECENT_CALL_LOG,
                            callType = typeStr,
                            timestamp = date,
                            durationSeconds = duration
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return callLogs
    }

    /**
     * Checks if a phone number exists in the device's saved address book.
     * Used for Zero-Interference Contact Whitelist mode.
     */
    fun isContactWhitelisted(context: Context, number: String): Boolean {
        if (number.isBlank() || number == "Unknown") return false
        val cleanNumber = number.replace("[^0-9+]".toRegex(), "")
        if (cleanNumber.length < 5) return false

        val savedContacts = fetchSavedContacts(context)
        return savedContacts.any { contact ->
            val savedClean = contact.number.replace("[^0-9+]".toRegex(), "")
            savedClean.endsWith(cleanNumber.takeLast(8)) || cleanNumber.endsWith(savedClean.takeLast(8))
        }
    }

    /**
     * Terminates an active call on the device using TelecomManager / Telephony system services.
     */
    fun endActiveCall(context: Context): Boolean {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    @Suppress("DEPRECATION")
                    val result = telecomManager?.endCall() ?: false
                    android.util.Log.i("PhoneCallManager", "telecomManager.endCall() returned: $result")
                    if (result) return true
                }
            }

            // Fallback via reflection on TelephonyManager ITelephony for legacy / customized ROMs
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val getITelephony = telephonyManager?.javaClass?.getDeclaredMethod("getITelephony")
            getITelephony?.isAccessible = true
            val iTelephony = getITelephony?.invoke(telephonyManager)
            val endCallMethod = iTelephony?.javaClass?.getDeclaredMethod("endCall")
            endCallMethod?.isAccessible = true
            val success = endCallMethod?.invoke(iTelephony) as? Boolean ?: false
            android.util.Log.i("PhoneCallManager", "ITelephony.endCall() returned: $success")
            return success
        } catch (e: Exception) {
            android.util.Log.w("PhoneCallManager", "Could not programmatically disconnect call: ${e.message}")
            return false
        }
    }
}
