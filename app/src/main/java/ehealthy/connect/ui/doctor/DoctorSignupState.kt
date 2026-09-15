// DoctorSignupState.kt
package ehealthy.connect.ui.doctor

object DoctorSignupState {
    var email: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var userId: String = ""
    var isGoogleSignup: Boolean = false

    fun reset() {
        email = ""
        firstName = ""
        lastName = ""
        userId = ""
        isGoogleSignup = false
    }
}