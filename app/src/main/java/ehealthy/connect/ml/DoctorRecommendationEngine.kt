package ehealthy.connect.ml

import ehealthy.connect.data.patient.PatientHealthProfile
import java.util.Locale

data class DoctorRecommendation(
    val specialty: String,
    val confidence: Double,
    val reason: String,
    val matchedSignals: List<String>
)

object DoctorRecommendationEngine {

    fun recommend(
        profile: PatientHealthProfile
    ): DoctorRecommendation? {

        val symptoms =
            profile.current_symptoms
                ?.trim()
                .orEmpty()

        if (
            symptoms.isBlank()
        ) {
            return null
        }

        val extracted =
            extract(
                profile
            )

        val prediction =
            DoctorRecommendationClassifier
                .predict(
                    extracted.features
                )

        return DoctorRecommendation(
            specialty =
                prediction.specialty,
            confidence =
                prediction.confidence
                    .coerceIn(
                        0.0,
                        1.0
                    ),
            reason =
                reasonFor(
                    prediction.specialty,
                    extracted.matchedSignals
                ),
            matchedSignals =
                extracted.matchedSignals
        )
    }


    fun specialtyMatches(
        doctorSpecialty: String,
        recommendedSpecialty: String
    ): Boolean {

        val doctor =
            doctorSpecialty
                .lowercase(
                    Locale.ROOT
                )

        return when (
            recommendedSpecialty
        ) {

            "Cardiology" ->
                doctor.contains(
                    "cardio"
                )

            "Dermatology" ->
                doctor.contains(
                    "dermat"
                ) ||
                        doctor.contains(
                            "skin"
                        )

            "Mental Health" ->
                doctor.contains(
                    "mental"
                ) ||
                        doctor.contains(
                            "psych"
                        ) ||
                        doctor.contains(
                            "counsell"
                        ) ||
                        doctor.contains(
                            "counsel"
                        )

            "Dentistry" ->
                doctor.contains(
                    "dent"
                ) ||
                        doctor.contains(
                            "oral"
                        )

            "General Practitioner" ->
                doctor.contains(
                    "general"
                ) ||
                        doctor.contains(
                            "family"
                        ) ||
                        doctor.contains(
                            "primary"
                        ) ||
                        doctor == "gp"

            else ->
                doctor.equals(
                    recommendedSpecialty,
                    ignoreCase =
                        true
                )
        }
    }


    fun languageMatches(
        doctorLanguages: String?,
        preferredLanguage: String?
    ): Boolean {

        val preferred =
            preferredLanguage
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return false

        return doctorLanguages
            .orEmpty()
            .contains(
                preferred,
                ignoreCase =
                    true
            )
    }


    private data class ExtractedFeatures(
        val features: DoubleArray,
        val matchedSignals: List<String>
    )


    private fun extract(
        profile: PatientHealthProfile
    ): ExtractedFeatures {

        val symptomText =
            listOfNotNull(
                profile.current_symptoms,
                profile.chronic,
                profile.family_medical_history
            )
                .joinToString(
                    " "
                )
                .lowercase(
                    Locale.ROOT
                )

        val signals =
            mutableListOf<String>()


        fun containsAny(
            vararg words: String
        ): Boolean {

            return words.any {
                symptomText.contains(
                    it
                )
            }
        }


        fun flag(
            label: String,
            vararg words: String
        ): Double {

            val matched =
                containsAny(
                    *words
                )

            if (
                matched
            ) {
                signals +=
                    label
            }

            return if (
                matched
            ) {
                1.0
            } else {
                0.0
            }
        }


        val chestPain =
            flag(
                "chest pain",
                "chest pain",
                "chest pressure",
                "tight chest",
                "chest tightness"
            )

        val palpitations =
            flag(
                "palpitations",
                "palpitation",
                "racing heart",
                "heart racing",
                "irregular heartbeat",
                "fast heartbeat"
            )

        val shortnessOfBreath =
            flag(
                "shortness of breath",
                "shortness of breath",
                "difficulty breathing",
                "breathless",
                "breathing difficulty"
            )

        val dizzinessFainting =
            flag(
                "dizziness/fainting",
                "dizzy",
                "dizziness",
                "faint",
                "fainting",
                "lightheaded"
            )


        val skinRash =
            flag(
                "skin rash",
                "rash",
                "hives",
                "skin eruption"
            )

        val itching =
            flag(
                "itching",
                "itchy",
                "itch"
            )

        val acne =
            flag(
                "acne",
                "pimple",
                "pimples"
            )

        val skinLesion =
            flag(
                "skin lesion",
                "skin lesion",
                "mole",
                "skin growth",
                "skin spot",
                "eczema",
                "psoriasis"
            )


        val anxiety =
            flag(
                "anxiety",
                "anxiety",
                "anxious",
                "worry",
                "worried"
            )

        val lowMood =
            flag(
                "low mood",
                "depressed",
                "depression",
                "low mood",
                "sadness",
                "hopeless"
            )

        val panic =
            flag(
                "panic",
                "panic attack",
                "panic attacks"
            )

        val sleepProblem =
            flag(
                "sleep problem",
                "insomnia",
                "cannot sleep",
                "can't sleep",
                "poor sleep",
                "sleep problem"
            )


        val toothPain =
            flag(
                "tooth pain",
                "tooth pain",
                "toothache",
                "painful tooth"
            )

        val gumProblem =
            flag(
                "gum problem",
                "bleeding gum",
                "bleeding gums",
                "gum pain",
                "swollen gums",
                "gum swelling"
            )

        val mouthSwelling =
            flag(
                "mouth swelling",
                "mouth swelling",
                "facial swelling",
                "swollen mouth"
            )

        val jawPain =
            flag(
                "jaw pain",
                "jaw pain",
                "painful jaw"
            )


        val fever =
            flag(
                "fever",
                "fever",
                "high temperature",
                "temperature"
            )

        val cough =
            flag(
                "cough",
                "cough",
                "coughing"
            )

        val headache =
            flag(
                "headache",
                "headache",
                "migraine",
                "head pain"
            )

        val stomachPain =
            flag(
                "stomach pain",
                "stomach pain",
                "abdominal pain",
                "tummy pain",
                "abdominal cramps"
            )

        val nausea =
            flag(
                "nausea",
                "nausea",
                "nauseous",
                "vomit",
                "vomiting"
            )

        val bodyAches =
            flag(
                "body aches",
                "body ache",
                "body aches",
                "muscle pain",
                "muscle aches",
                "general pain"
            )


        val smokingText =
            profile.smoking_status
                .orEmpty()
                .lowercase(
                    Locale.ROOT
                )

        val smoker =
            if (
                listOf(
                    "yes",
                    "current",
                    "daily",
                    "smoke",
                    "smoker"
                ).any {
                    smokingText.contains(
                        it
                    )
                }
            ) {

                signals +=
                    "smoking history"

                1.0

            } else {

                0.0
            }


        val historyText =
            listOfNotNull(
                profile.chronic,
                profile.family_medical_history
            )
                .joinToString(
                    " "
                )
                .lowercase(
                    Locale.ROOT
                )

        val cardiacHistory =
            if (
                listOf(
                    "heart",
                    "cardiac",
                    "hypertension",
                    "high blood pressure",
                    "cholesterol"
                ).any {
                    historyText.contains(
                        it
                    )
                }
            ) {

                signals +=
                    "heart/blood-pressure history"

                1.0

            } else {

                0.0
            }


        val severityHigh =
            if (
                (
                        profile
                            .symptom_severity
                            ?: 0
                        ) >=
                7
            ) {

                signals +=
                    "higher symptom severity"

                1.0

            } else {

                0.0
            }


        val durationLong =
            if (
                isLongDuration(
                    profile.symptom_duration
                )
            ) {

                signals +=
                    "longer symptom duration"

                1.0

            } else {

                0.0
            }


        val vector =
            doubleArrayOf(
                chestPain,
                palpitations,
                shortnessOfBreath,
                dizzinessFainting,
                skinRash,
                itching,
                acne,
                skinLesion,
                anxiety,
                lowMood,
                panic,
                sleepProblem,
                toothPain,
                gumProblem,
                mouthSwelling,
                jawPain,
                fever,
                cough,
                headache,
                stomachPain,
                nausea,
                bodyAches,
                smoker,
                cardiacHistory,
                severityHigh,
                durationLong
            )


        return ExtractedFeatures(
            features =
                vector,
            matchedSignals =
                signals
                    .distinct()
                    .take(
                        5
                    )
        )
    }


    private fun isLongDuration(
        raw: String?
    ): Boolean {

        val value =
            raw
                ?.trim()
                ?.lowercase(
                    Locale.ROOT
                )
                ?: return false

        if (
            value.isBlank()
        ) {
            return false
        }


        if (
            listOf(
                "week",
                "weeks",
                "month",
                "months",
                "year",
                "years",
                "chronic",
                "ongoing",
                "long time"
            ).any {
                value.contains(
                    it
                )
            }
        ) {
            return true
        }


        val match =
            Regex(
                """(\d+)\s*day"""
            )
                .find(
                    value
                )

        val days =
            match
                ?.groupValues
                ?.getOrNull(
                    1
                )
                ?.toIntOrNull()


        return (
                days
                    ?: 0
                ) >=
                7
    }


    private fun reasonFor(
        specialty: String,
        signals: List<String>
    ): String {

        val signalText =
            if (
                signals.isEmpty()
            ) {

                "the answers in your current health questionnaire"

            } else {

                signals.joinToString(
                    ", "
                )
            }


        return when (
            specialty
        ) {

            "Cardiology" ->
                "The routing model found cardiovascular-related questionnaire signals, including $signalText."

            "Dermatology" ->
                "The routing model found skin-related questionnaire signals, including $signalText."

            "Mental Health" ->
                "The routing model found mental-health-related questionnaire signals, including $signalText."

            "Dentistry" ->
                "The routing model found dental/oral-health questionnaire signals, including $signalText."

            else ->
                "The routing model found general-care questionnaire signals, including $signalText."
        }
    }
}
