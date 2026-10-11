package com.smartattend.app.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseSdkInitializationTest {
    @Test
    fun packagedConfigurationInitializesDefaultAppAndRequestedSdks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = FirebaseApp.getInstance()
        val auth = FirebaseAuth.getInstance(app)
        val firestore = FirebaseFirestore.getInstance(app)

        assertFalse(app.options.applicationId.isBlank())
        assertFalse(app.options.projectId.orEmpty().isBlank())
        assertNotNull(auth)
        assertNotNull(firestore)
        assertEquals(app.name, auth.app.name)
        assertEquals(app.name, firestore.app.name)
        assertEquals("com.smartattend.app", context.packageName)
    }
}
