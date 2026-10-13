package ehealthy.connect.data.doctor

import ehealthy.connect.data.patient.PatientHealthProfile
import ehealthy.connect.data.patient.PhysicalVisitInvoice
import ehealthy.connect.data.patient.PhysicalVisitRequest
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


object DoctorPatientCareRepository {


    private val supabase =
        SupabaseClientProvider.client


    /*
     * ---------------------------------------------------------
     * PATIENT HEALTH PROFILE
     * ---------------------------------------------------------
     *
     * The doctor does NOT directly query the patients table.
     *
     * Supabase must verify that the logged-in doctor is actually
     * connected to this patient through an appointment.
     */
    suspend fun getPatientHealthProfile(
        appointmentId: String
    ): Result<PatientHealthProfile> =
        runCatching {

            require(
                appointmentId.isNotBlank()
            ) {
                "Appointment ID is required."
            }

            supabase.auth.awaitInitialization()

            val parameters =
                buildJsonObject {

                    put(
                        "p_appointment_id",
                        appointmentId
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function =
                        "get_patient_health_profile_for_doctor",
                    parameters =
                        parameters
                )
                .decodeList<PatientHealthProfile>()
                .firstOrNull()
                ?: error(
                    "Patient health profile could not be found."
                )
        }


    /*
     * ---------------------------------------------------------
     * PHYSICAL VISIT REQUESTS
     * ---------------------------------------------------------
     *
     * Loads the physical follow-up requests connected to
     * this appointment/patient.
     */
    suspend fun getPhysicalVisitRequests(
        appointmentId: String
    ): Result<List<PhysicalVisitRequest>> =
        runCatching {

            require(
                appointmentId.isNotBlank()
            ) {
                "Appointment ID is required."
            }

            val parameters =
                buildJsonObject {

                    put(
                        "p_appointment_id",
                        appointmentId
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function =
                        "doctor_get_physical_visit_requests",
                    parameters =
                        parameters
                )
                .decodeList<PhysicalVisitRequest>()
        }


    /*
     * ---------------------------------------------------------
     * PHYSICAL VISIT INVOICES
     * ---------------------------------------------------------
     */
    suspend fun getPhysicalVisitInvoices(
        appointmentId: String
    ): Result<List<PhysicalVisitInvoice>> =
        runCatching {

            require(
                appointmentId.isNotBlank()
            ) {
                "Appointment ID is required."
            }

            val parameters =
                buildJsonObject {

                    put(
                        "p_appointment_id",
                        appointmentId
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function =
                        "doctor_get_physical_visit_invoices",
                    parameters =
                        parameters
                )
                .decodeList<PhysicalVisitInvoice>()
        }

    suspend fun acceptPhysicalVisitRequest(
        requestId: String,
        doctorNote: String = ""
    ): Result<Unit> =
        runCatching {

            require(requestId.isNotBlank()) {
                "Request ID is required."
            }

            val parameters =
                buildJsonObject {
                    put(
                        "p_request_id",
                        requestId
                    )

                    put(
                        "p_status",
                        "accepted"
                    )

                    put(
                        "p_doctor_note",
                        doctorNote
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function =
                        "doctor_decide_physical_visit_request",
                    parameters =
                        parameters
                )
        }


    suspend fun declinePhysicalVisitRequest(
        requestId: String,
        doctorNote: String = ""
    ): Result<Unit> =
        runCatching {

            require(requestId.isNotBlank()) {
                "Request ID is required."
            }

            val parameters =
                buildJsonObject {
                    put(
                        "p_request_id",
                        requestId
                    )

                    put(
                        "p_status",
                        "declined"
                    )

                    put(
                        "p_doctor_note",
                        doctorNote
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function =
                        "doctor_decide_physical_visit_request",
                    parameters =
                        parameters
                )
        }

    suspend fun issuePhysicalVisitInvoice(
        requestId: String,
        serviceDescription: String,
        amountMinor: Int,
        proposedDate: String,
        proposedTime: String,
        location: String,
        notes: String = ""
    ): Result<Unit> =
        runCatching {

            require(requestId.isNotBlank()) {
                "Physical visit request ID is required."
            }

            require(serviceDescription.trim().length >= 3) {
                "Please enter the service being provided."
            }

            require(amountMinor > 0) {
                "Invoice amount must be greater than zero."
            }

            require(proposedDate.isNotBlank()) {
                "Please provide a visit date."
            }

            require(proposedTime.isNotBlank()) {
                "Please provide a visit time."
            }

            require(location.trim().length >= 3) {
                "Please provide the physical visit location."
            }

            val parameters =
                buildJsonObject {

                    put(
                        "p_request_id",
                        requestId
                    )

                    put(
                        "p_service_description",
                        serviceDescription.trim()
                    )

                    put(
                        "p_amount_minor",
                        amountMinor
                    )

                    put(
                        "p_currency",
                        "ZAR"
                    )

                    put(
                        "p_proposed_date",
                        proposedDate.trim()
                    )

                    put(
                        "p_proposed_time",
                        proposedTime.trim()
                    )

                    put(
                        "p_location",
                        location.trim()
                    )

                    put(
                        "p_notes",
                        notes.trim()
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function =
                        "doctor_issue_physical_visit_invoice",
                    parameters =
                        parameters
                )
        }
}