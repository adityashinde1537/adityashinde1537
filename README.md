# CRESTO GPS Demo APK

This standalone Android app uses the phone's actual GPS and shows real-time coordinates, speed, trip distance, accuracy, and elapsed time. Start/Stop begins and ends a user-visible location foreground service. The Open Map button opens the most recent location in OpenStreetMap.

The app does not upload location to a server, and there is no remote bus tracker or student viewer in this basic demo.

Install the output debug APK, grant **Precise Location**, and enable GPS. Tracking stays active while the foreground service notification is shown.

Built with Android Gradle Plugin 8.7.3, Android SDK 35, Gradle 8.10.2, Java 17. APK is built automatically by the push workflow for this isolated branch. The repository's default branch is not changed.
