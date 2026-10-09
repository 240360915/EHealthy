# EHealthy Doctor Recommendation Classification Model

## Purpose

This model is a **healthcare specialty routing classifier**. It uses structured signals extracted from the patient's saved EHealthy health questionnaire to recommend a type of healthcare professional. It does **not** diagnose a condition and it does not replace a clinician.

## Model

- Algorithm: **Decision Tree Classifier**
- Classes: General Practitioner, Cardiology, Dermatology, Mental Health, Dentistry
- Input features: 26
- Dataset rows: 1100
- Training rows: 880
- Test rows: 220
- Split: 80% training / 20% testing
- Random state: 42
- Maximum tree depth: 8
- Minimum samples per leaf: 8

## Dataset disclosure

`doctor_routing_training_data.csv` is a **synthetic educational routing dataset created for this EHealthy project**. It is not a clinical dataset and must not be represented as one. The patterns were designed to demonstrate the machine-learning workflow required by the project: feature engineering, classification, train/test evaluation, model export, and application integration.

The Android application therefore presents the output as a **specialty recommendation**, not a medical diagnosis.

## Test results

- **Accuracy:** 88.18%
- **Macro F1-score:** 88.34%

### Per-class results

| Specialty | Precision | Recall | F1-score |
|---|---:|---:|---:|
| General Practitioner | 0.714 | 0.909 | 0.800 |
| Cardiology | 0.889 | 0.909 | 0.899 |
| Dermatology | 0.955 | 0.955 | 0.955 |
| Mental Health | 0.951 | 0.886 | 0.918 |
| Dentistry | 0.971 | 0.750 | 0.846 |

## How EHealthy uses the model

1. The patient completes the Health Profile questionnaire.
2. Android extracts binary routing features from current symptoms, chronic/family history, smoking status, symptom severity and duration.
3. The exported Decision Tree predicts one specialty and returns the leaf confidence.
4. Find Doctors displays the recommendation with a clear **not a diagnosis** notice.
5. Only **approved, non-deactivated doctors** remain in the directory.
6. The default Recommended sort places doctors matching the predicted specialty first, then considers preferred-language match and years of experience.
7. The patient remains free to browse all doctors or apply the recommended-specialty filter.

## Confusion matrix

The exact matrix is included as:

- `model_artifacts/confusion_matrix.csv`
- `model_artifacts/confusion_matrix.png`

## Reproducibility

Run:

```bash
pip install -r requirements.txt
python train_classifier.py
```

The script reproduces the train/test evaluation, model artifacts and Android `DoctorRecommendationClassifier.kt`.

## Important limitation

This proof-of-concept uses synthetic training data. A production healthcare recommender would require clinically reviewed labels, representative real-world data, bias/safety evaluation, governance, monitoring, and professional validation before medical deployment.
