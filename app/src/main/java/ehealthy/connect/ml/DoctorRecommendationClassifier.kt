package ehealthy.connect.ml

/**
 * AUTO-GENERATED from the project's trained scikit-learn DecisionTreeClassifier.
 *
 * This model ROUTES a questionnaire to a healthcare specialty.
 * It is not a diagnostic model.
 */
data class ModelPrediction(
    val specialty: String,
    val confidence: Double
)

object DoctorRecommendationClassifier {

    const val FEATURE_COUNT: Int = 26

    val featureNames: List<String> =
        listOf(
            "chest_pain",
            "palpitations",
            "shortness_of_breath",
            "dizziness_fainting",
            "skin_rash",
            "itching",
            "acne",
            "skin_lesion",
            "anxiety",
            "low_mood",
            "panic",
            "sleep_problem",
            "tooth_pain",
            "gum_problem",
            "mouth_swelling",
            "jaw_pain",
            "fever",
            "cough",
            "headache",
            "stomach_pain",
            "nausea",
            "body_aches",
            "smoker",
            "cardiac_history",
            "severity_high",
            "duration_long",
        )

    fun predict(
        features: DoubleArray
    ): ModelPrediction {

        require(
            features.size == FEATURE_COUNT
        ) {
            "Expected $FEATURE_COUNT classifier features."
        }

        return node0(
            features
        )
    }

    private fun node0(f: DoubleArray): ModelPrediction {
        return if (
            f[12] <= 0.50000000
        ) {
            node1(f)
        } else {
            node50(f)
        }
    }

    private fun node1(f: DoubleArray): ModelPrediction {
        return if (
            f[4] <= 0.50000000
        ) {
            node2(f)
        } else {
            node41(f)
        }
    }

    private fun node2(f: DoubleArray): ModelPrediction {
        return if (
            f[0] <= 0.50000000
        ) {
            node3(f)
        } else {
            node32(f)
        }
    }

    private fun node3(f: DoubleArray): ModelPrediction {
        return if (
            f[8] <= 0.50000000
        ) {
            node4(f)
        } else {
            node23(f)
        }
    }

    private fun node4(f: DoubleArray): ModelPrediction {
        return if (
            f[16] <= 0.50000000
        ) {
            node5(f)
        } else {
            node16(f)
        }
    }

    private fun node5(f: DoubleArray): ModelPrediction {
        return if (
            f[2] <= 0.50000000
        ) {
            node6(f)
        } else {
            node13(f)
        }
    }

    private fun node6(f: DoubleArray): ModelPrediction {
        return if (
            f[9] <= 0.50000000
        ) {
            node7(f)
        } else {
            node10(f)
        }
    }

    private fun node7(f: DoubleArray): ModelPrediction {
        return if (
            f[5] <= 0.50000000
        ) {
            node8(f)
        } else {
            node9(f)
        }
    }

    private fun node8(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "General Practitioner",
            confidence = 0.51908397
        )

    private fun node9(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dermatology",
            confidence = 0.95833333
        )

    private fun node10(f: DoubleArray): ModelPrediction {
        return if (
            f[11] <= 0.50000000
        ) {
            node11(f)
        } else {
            node12(f)
        }
    }

    private fun node11(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 0.83333333
        )

    private fun node12(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 1.00000000
        )

    private fun node13(f: DoubleArray): ModelPrediction {
        return if (
            f[1] <= 0.50000000
        ) {
            node14(f)
        } else {
            node15(f)
        }
    }

    private fun node14(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 0.76923077
        )

    private fun node15(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 1.00000000
        )

    private fun node16(f: DoubleArray): ModelPrediction {
        return if (
            f[18] <= 0.50000000
        ) {
            node17(f)
        } else {
            node22(f)
        }
    }

    private fun node17(f: DoubleArray): ModelPrediction {
        return if (
            f[17] <= 0.50000000
        ) {
            node18(f)
        } else {
            node21(f)
        }
    }

    private fun node18(f: DoubleArray): ModelPrediction {
        return if (
            f[21] <= 0.50000000
        ) {
            node19(f)
        } else {
            node20(f)
        }
    }

    private fun node19(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "General Practitioner",
            confidence = 0.78260870
        )

    private fun node20(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "General Practitioner",
            confidence = 1.00000000
        )

    private fun node21(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "General Practitioner",
            confidence = 1.00000000
        )

    private fun node22(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "General Practitioner",
            confidence = 1.00000000
        )

    private fun node23(f: DoubleArray): ModelPrediction {
        return if (
            f[9] <= 0.50000000
        ) {
            node24(f)
        } else {
            node31(f)
        }
    }

    private fun node24(f: DoubleArray): ModelPrediction {
        return if (
            f[10] <= 0.50000000
        ) {
            node25(f)
        } else {
            node30(f)
        }
    }

    private fun node25(f: DoubleArray): ModelPrediction {
        return if (
            f[11] <= 0.50000000
        ) {
            node26(f)
        } else {
            node29(f)
        }
    }

    private fun node26(f: DoubleArray): ModelPrediction {
        return if (
            f[25] <= 0.50000000
        ) {
            node27(f)
        } else {
            node28(f)
        }
    }

    private fun node27(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 0.75000000
        )

    private fun node28(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 0.77777778
        )

    private fun node29(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 0.93333333
        )

    private fun node30(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 1.00000000
        )

    private fun node31(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 1.00000000
        )

    private fun node32(f: DoubleArray): ModelPrediction {
        return if (
            f[1] <= 0.50000000
        ) {
            node33(f)
        } else {
            node40(f)
        }
    }

    private fun node33(f: DoubleArray): ModelPrediction {
        return if (
            f[2] <= 0.50000000
        ) {
            node34(f)
        } else {
            node39(f)
        }
    }

    private fun node34(f: DoubleArray): ModelPrediction {
        return if (
            f[3] <= 0.50000000
        ) {
            node35(f)
        } else {
            node38(f)
        }
    }

    private fun node35(f: DoubleArray): ModelPrediction {
        return if (
            f[25] <= 0.50000000
        ) {
            node36(f)
        } else {
            node37(f)
        }
    }

    private fun node36(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 0.75000000
        )

    private fun node37(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 0.50000000
        )

    private fun node38(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 0.88888889
        )

    private fun node39(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 1.00000000
        )

    private fun node40(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 1.00000000
        )

    private fun node41(f: DoubleArray): ModelPrediction {
        return if (
            f[5] <= 0.50000000
        ) {
            node42(f)
        } else {
            node49(f)
        }
    }

    private fun node42(f: DoubleArray): ModelPrediction {
        return if (
            f[6] <= 0.50000000
        ) {
            node43(f)
        } else {
            node48(f)
        }
    }

    private fun node43(f: DoubleArray): ModelPrediction {
        return if (
            f[24] <= 0.50000000
        ) {
            node44(f)
        } else {
            node47(f)
        }
    }

    private fun node44(f: DoubleArray): ModelPrediction {
        return if (
            f[25] <= 0.50000000
        ) {
            node45(f)
        } else {
            node46(f)
        }
    }

    private fun node45(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dermatology",
            confidence = 0.66666667
        )

    private fun node46(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dermatology",
            confidence = 0.91666667
        )

    private fun node47(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dermatology",
            confidence = 0.37500000
        )

    private fun node48(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dermatology",
            confidence = 1.00000000
        )

    private fun node49(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dermatology",
            confidence = 1.00000000
        )

    private fun node50(f: DoubleArray): ModelPrediction {
        return if (
            f[2] <= 0.50000000
        ) {
            node51(f)
        } else {
            node64(f)
        }
    }

    private fun node51(f: DoubleArray): ModelPrediction {
        return if (
            f[9] <= 0.50000000
        ) {
            node52(f)
        } else {
            node63(f)
        }
    }

    private fun node52(f: DoubleArray): ModelPrediction {
        return if (
            f[16] <= 0.50000000
        ) {
            node53(f)
        } else {
            node62(f)
        }
    }

    private fun node53(f: DoubleArray): ModelPrediction {
        return if (
            f[4] <= 0.50000000
        ) {
            node54(f)
        } else {
            node61(f)
        }
    }

    private fun node54(f: DoubleArray): ModelPrediction {
        return if (
            f[13] <= 0.50000000
        ) {
            node55(f)
        } else {
            node60(f)
        }
    }

    private fun node55(f: DoubleArray): ModelPrediction {
        return if (
            f[24] <= 0.50000000
        ) {
            node56(f)
        } else {
            node57(f)
        }
    }

    private fun node56(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dentistry",
            confidence = 1.00000000
        )

    private fun node57(f: DoubleArray): ModelPrediction {
        return if (
            f[15] <= 0.50000000
        ) {
            node58(f)
        } else {
            node59(f)
        }
    }

    private fun node58(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dentistry",
            confidence = 0.77777778
        )

    private fun node59(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dentistry",
            confidence = 1.00000000
        )

    private fun node60(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dentistry",
            confidence = 1.00000000
        )

    private fun node61(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Dentistry",
            confidence = 0.50000000
        )

    private fun node62(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "General Practitioner",
            confidence = 0.50000000
        )

    private fun node63(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Mental Health",
            confidence = 0.62500000
        )

    private fun node64(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = "Cardiology",
            confidence = 0.66666667
        )

}

