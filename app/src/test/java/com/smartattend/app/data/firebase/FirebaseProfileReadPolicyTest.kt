package com.smartattend.app.data.firebase

import com.google.firebase.firestore.Source
import org.junit.Assert.assertSame
import org.junit.Test

class FirebaseProfileReadPolicyTest {
    @Test
    fun authorizationProfileMustBeReadFromServer() {
        assertSame(Source.SERVER, FirebaseAuthenticationRepository.AUTHORIZED_PROFILE_SOURCE)
    }
}
