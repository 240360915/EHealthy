EHealthy Doctor Recommendation Classification Batch

WHAT THIS IMPLEMENTS

Health Profile / questionnaire
        ↓
Feature extraction
        ↓
Trained Decision Tree classifier
        ↓
Specialty recommendation
        ↓
Find Doctors
        ↓
Approved doctors matching the predicted specialty rank first

SPECIALTIES
- General Practitioner
- Cardiology
- Dermatology
- Mental Health
- Dentistry

ANDROID FILES

1. android/FindDoctorsScreen.kt
   Replace your current:
   ehealthy/connect/ui/patientDashboard/FindDoctors/FindDoctorsScreen.kt

2. android/DoctorRecommendationClassifier.kt
   NEW:
   ehealthy/connect/ml/DoctorRecommendationClassifier.kt

3. android/DoctorRecommendationEngine.kt
   NEW:
   ehealthy/connect/ml/DoctorRecommendationEngine.kt

No MainActivity.kt change is required.
No Supabase SQL is required for this feature.
No PatientRepository.kt replacement is required because the current repository
already contains getMyHealthProfile().

MODEL / ASSIGNMENT FILES

doctor_routing_training_data.csv
train_classifier.py
requirements.txt
MODEL_REPORT.md

model_artifacts/
- doctor_routing_tree.joblib
- classification_report.csv
- confusion_matrix.csv
- confusion_matrix.png
- feature_importance.csv
- decision_tree_rules.txt
- model_metadata.json

IMPORTANT DATASET DISCLOSURE

The supplied training CSV is SYNTHETIC EDUCATIONAL DATA created for this project.
Do not tell the lecturer that it is hospital/clinical data.

It exists so you can demonstrate a proper ML workflow:
- labelled dataset
- features and target class
- train/test split
- model training
- predictions
- accuracy
- precision
- recall
- F1-score
- confusion matrix
- exported trained model rules
- Android integration

CURRENT TEST PERFORMANCE
- 1,100 labelled examples
- 880 training examples
- 220 testing examples
- Accuracy: 88.18%
- Macro F1: 88.34%

HOW THE APP WORKS

1. Patient completes:
   Settings -> Health profile

2. The model reads routing signals from:
   - current symptoms
   - symptom severity
   - symptom duration
   - smoking status
   - chronic conditions
   - family medical history

3. Find Doctors shows:
   "Smart doctor recommendation"

4. The model outputs one specialty and a model confidence.

5. The screen clearly says:
   "This feature routes you to a healthcare specialty.
    It is not a medical diagnosis."

6. Only approved, non-deactivated doctors are shown.

7. Default Recommended ranking:
   - predicted-specialty match first
   - preferred-language match
   - years of experience
   - doctor name

8. Patient can tap:
   "Show recommended doctors"
   to see only doctors matching the predicted specialty.

HOW TO RE-TRAIN

From this folder:

pip install -r requirements.txt
python train_classifier.py

The script regenerates:
- evaluation metrics
- confusion matrix
- feature importance
- tree rules
- .joblib model
- android/DoctorRecommendationClassifier.kt

TEST EXAMPLES

Example 1:
Questionnaire:
"itchy red rash on my arms"
Expected routing:
Dermatology

Example 2:
Questionnaire:
"toothache and swollen gums"
Expected routing:
Dentistry

Example 3:
Questionnaire:
"chest pain and shortness of breath"
Expected routing:
Cardiology

Example 4:
Questionnaire:
"anxiety, panic attacks and I cannot sleep"
Expected routing:
Mental Health

Example 5:
Questionnaire:
"fever, cough, headache and body aches"
Expected routing:
General Practitioner

SAFETY / SCOPE
This is routing/recommendation only. It is not diagnosis.
For a production healthcare system, use clinically reviewed real-world data,
formal validation, bias testing and healthcare governance.
