from pathlib import Path
import json
import joblib
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier, export_text
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    f1_score,
)

ROOT = Path(__file__).resolve().parent
DATASET = ROOT / "doctor_routing_training_data.csv"
ARTIFACTS = ROOT / "model_artifacts"
ARTIFACTS.mkdir(exist_ok=True)

FEATURES = [
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
]

CLASSES = [
    "General Practitioner",
    "Cardiology",
    "Dermatology",
    "Mental Health",
    "Dentistry",
]

df = pd.read_csv(DATASET)

X = df[FEATURES]
y = df["specialty"]

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y,
)

model = DecisionTreeClassifier(
    max_depth=8,
    min_samples_leaf=8,
    random_state=42,
    class_weight="balanced",
)

model.fit(X_train, y_train)

predictions = model.predict(X_test)

accuracy = accuracy_score(
    y_test,
    predictions,
)

macro_f1 = f1_score(
    y_test,
    predictions,
    average="macro",
)

print(f"Rows: {len(df)}")
print(f"Training rows: {len(X_train)}")
print(f"Testing rows: {len(X_test)}")
print(f"Accuracy: {accuracy:.4f}")
print(f"Macro F1: {macro_f1:.4f}")
print()
print(
    classification_report(
        y_test,
        predictions,
        labels=CLASSES,
        target_names=CLASSES,
        zero_division=0,
    )
)

joblib.dump(
    model,
    ARTIFACTS / "doctor_routing_tree.joblib",
)

report = classification_report(
    y_test,
    predictions,
    labels=CLASSES,
    target_names=CLASSES,
    output_dict=True,
    zero_division=0,
)

pd.DataFrame(report).T.to_csv(
    ARTIFACTS / "classification_report.csv"
)

matrix = confusion_matrix(
    y_test,
    predictions,
    labels=CLASSES,
)

pd.DataFrame(
    matrix,
    index=CLASSES,
    columns=CLASSES,
).to_csv(
    ARTIFACTS / "confusion_matrix.csv"
)

importance = pd.DataFrame(
    {
        "feature": FEATURES,
        "importance": model.feature_importances_,
    }
).sort_values(
    "importance",
    ascending=False,
)

importance.to_csv(
    ARTIFACTS / "feature_importance.csv",
    index=False,
)

rules = export_text(
    model,
    feature_names=FEATURES,
    decimals=3,
)

(ARTIFACTS / "decision_tree_rules.txt").write_text(
    rules,
    encoding="utf-8",
)

metadata = {
    "purpose": "Healthcare specialty routing only; not diagnosis",
    "dataset_type": "Synthetic educational routing dataset",
    "rows": len(df),
    "train_rows": len(X_train),
    "test_rows": len(X_test),
    "accuracy": accuracy,
    "macro_f1": macro_f1,
    "features": FEATURES,
    "classes": CLASSES,
    "random_state": 42,
    "model": "DecisionTreeClassifier",
    "max_depth": 8,
    "min_samples_leaf": 8,
}

(ARTIFACTS / "model_metadata.json").write_text(
    json.dumps(
        metadata,
        indent=2,
    ),
    encoding="utf-8",
)

fig, ax = plt.subplots(
    figsize=(8, 6)
)

ax.imshow(
    matrix
)

ax.set_xticks(
    range(
        len(CLASSES)
    ),
    CLASSES,
    rotation=35,
    ha="right",
)

ax.set_yticks(
    range(
        len(CLASSES)
    ),
    CLASSES,
)

ax.set_xlabel(
    "Predicted specialty"
)

ax.set_ylabel(
    "Actual specialty"
)

ax.set_title(
    "EHealthy Doctor Routing - Confusion Matrix"
)

for i in range(
    matrix.shape[0]
):
    for j in range(
        matrix.shape[1]
    ):
        ax.text(
            j,
            i,
            str(
                matrix[i, j]
            ),
            ha="center",
            va="center",
        )

fig.tight_layout()

fig.savefig(
    ARTIFACTS / "confusion_matrix.png",
    dpi=180,
)

print(
    "\nModel artifacts saved to:",
    ARTIFACTS,
)


# ------------------------------------------------------------
# Export the exact trained Decision Tree into Kotlin.
# Android therefore uses the trained model rules without needing
# scikit-learn or an online Python server.
# ------------------------------------------------------------

ANDROID_DIR = ROOT / "android"
ANDROID_DIR.mkdir(exist_ok=True)

tree = model.tree_
class_names = list(model.classes_)

def leaf_prediction(node):
    counts = tree.value[node][0]
    total = float(counts.sum())
    class_index = int(np.argmax(counts))
    confidence = float(counts[class_index] / total) if total else 0.0
    return class_names[class_index], confidence

reachable_nodes = []

def visit(node):
    reachable_nodes.append(node)
    if tree.children_left[node] != tree.children_right[node]:
        visit(int(tree.children_left[node]))
        visit(int(tree.children_right[node]))

visit(0)

def node_function(node):
    if tree.children_left[node] == tree.children_right[node]:
        label, confidence = leaf_prediction(node)
        return f"""    private fun node{node}(f: DoubleArray): ModelPrediction =
        ModelPrediction(
            specialty = {json.dumps(label)},
            confidence = {confidence:.8f}
        )
"""

    feature_index = int(tree.feature[node])
    threshold = float(tree.threshold[node])
    left = int(tree.children_left[node])
    right = int(tree.children_right[node])

    return f"""    private fun node{node}(f: DoubleArray): ModelPrediction {{
        return if (
            f[{feature_index}] <= {threshold:.8f}
        ) {{
            node{left}(f)
        }} else {{
            node{right}(f)
        }}
    }}
"""

feature_lines = "\n".join(
    f"            {json.dumps(feature)}," for feature in FEATURES
)

functions = "\n".join(
    node_function(node) for node in reachable_nodes
)

kotlin = f"""package ehealthy.connect.ml

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

object DoctorRecommendationClassifier {{

    const val FEATURE_COUNT: Int = {len(FEATURES)}

    val featureNames: List<String> =
        listOf(
{{feature_lines}}
        )

    fun predict(
        features: DoubleArray
    ): ModelPrediction {{

        require(
            features.size == FEATURE_COUNT
        ) {{
            "Expected $FEATURE_COUNT classifier features."
        }}

        return node0(
            features
        )
    }}

{{functions}}
}}
""".replace("{feature_lines}", feature_lines).replace("{functions}", functions)

(ANDROID_DIR / "DoctorRecommendationClassifier.kt").write_text(
    kotlin,
    encoding="utf-8",
)

print(
    "Android Decision Tree exported to:",
    ANDROID_DIR / "DoctorRecommendationClassifier.kt",
)
