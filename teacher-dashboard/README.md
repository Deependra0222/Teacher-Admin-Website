# SmartAttend Teacher Dashboard

This folder is a local-only demonstration of the teacher workflow. It is not synchronized with the Android app and does not use Firebase until you explicitly add and configure the Firebase Web SDK.

Open `index.html` directly for the offline demo, or from this folder run `python -m http.server 8080` and visit `http://localhost:8080`. Use the prefilled demo account, start/stop a session, correct a record with a reason, and export CSV.

For Firebase later, create a Web app in the same Firebase project, copy `firebase-config.example.js` to the ignored `firebase-config.js`, add the official modular SDK, and replace the local arrays/localStorage with Auth and Firestore calls protected by `../firestore.rules`. Never place a service-account private key in this folder.
