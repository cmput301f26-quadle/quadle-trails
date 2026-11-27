# Trails - Campsite Reservation System

Created by Team QUADLE for CMPUT 301 (Fall 2026).

Our project documentation, including the **Product Backlog**, **UI Mockups**, **Object-Oriented Analysis**, and **Meeting Logs**, is hosted entirely in our GitHub Wiki.

**[Click here to view the Trails Wiki](https://github.com/cmput301f26-quadle/quadle-trails/wiki)**

## How to run

1. Open the `Trails/` folder (not the repo root) in Android Studio.
2. Let Gradle sync finish.
3. Run the `app` configuration on an emulator or device running API 24 or higher.

The application id is `com.example.quadletrails` so it matches the Firebase project in `google-services.json`. The Kotlin package is `com.example.trails`.

## Testing as different users

Run the **debug** build to show **Choose your testing role** immediately on a fresh launch.
Tap **Camper**, **Owner**, or **Admin** to enter as the corresponding sample user below.
The selection survives rotation and backgrounding. To choose again, open
**Settings → Change testing role**, which restarts the screen stack before showing the picker.

| User | Roles | Opens in |
|---|---|---|
| Priya Sharma | Camper | Camper (Browse) |
| Daniel Whitehorse | Owner, Camper | Owner (Hosting) |
| Morgan Lee | Admin, Owner, Camper | Admin (Admin home) |

**Switch testing role (no code changes needed):**
1. Tap the **Settings** tab (gear icon) → **Change testing role**.
2. Tap **Camper**, **Owner**, or **Admin**.
3. The app opens that role's home screen using its sample profile.

The older sample-user controls in Device profile are also restricted to debug builds.

**Switch mode (US 03.11.01):** as Morgan or Daniel, go to **Settings → Profile**, tap the mode pill at the top right (e.g. **Admin ▾**), pick a mode and tap **Switch to …**. Users only see the roles they hold, so Priya cannot switch.

**Profile stories (US 01.07.03, 01.08.01, 01.09.01):** on **Settings → Profile**, edit name, email or phone and tap **Save Changes**, or tap **Delete Profile** and confirm. Device profile shows the masked device identifier.

**Data:** users, campsites, lotteries, waitlists, reservations, notifications and messages are stored in Firestore, so profile changes are kept when the app restarts. Ratings and reports still use in-memory sample data. Users are identified by device with Firebase anonymous sign-in (US 01.09.01): release builds always use it, and the testing build offers it as **This device · Firebase sign-in** next to the sample users. Sample users are also signed in to Firebase in the background, so every session has `request.auth`.

## Debug and release builds

In Android Studio, open **Build Variants** and select **debug** for the `app` module,
then press Run. Debug enables `BuildConfig.TESTING_TOOLS`, adds `-testing` to the
version name, and shows the startup role picker and Settings shortcut.

From the `Trails/` directory, with a device or emulator connected:
```
./gradlew installDebug
```
Open Trails on the device after installation.

Select **release** to build without the testing popup or sample-user controls.
To create an unsigned release APK:
```
./gradlew assembleRelease
```
The APK is under `app/build/outputs/apk/release/`. To install or distribute a release,
use Android Studio's **Generate Signed App Bundle or APK** action and your release
keystore; no release signing credentials are configured in this repository.

Both variants currently use the same application ID and Firebase configuration.
They are not separate installed apps or separate data environments. A differently
signed release cannot replace a debug installation without uninstalling it first
(which removes local app data).

**Release identity:** a release build signs the device in with Firebase anonymous
authentication and keeps its profile in Firestore. A new user starts as a camper and can
become a host in the app; the Admin role is granted by the team in the Firebase console.
Ratings and reports still use in-memory data in both variants, and Firestore is still in
test mode: the `users` security rules are written in `doc/firestore-users.rules` but not published.

**Run the tests** (from the `Trails/` folder, with an emulator running for the second command):
```
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

## Who owns what

Paths are relative to `Trails/app/src/main/java/com/example/trails/`.

| Name | GitHub | Category | User stories | Files / folders |
|---|---|---|---|---|
| Neaz | NeazMZ06 | Search & Discovery | 01.01.01–01.01.05 | `ui/discovery/` |
| Ibrahim | ibr2309 | Campsite Management | 02.01.01–02.01.04, 02.03.01 | `ui/campsite/`, Firestore `CampsiteRepository` |
| Skanda | skandarajeev | Lottery Setup | 02.04.01–02.04.07 | `ui/lottery/`, Firestore `LotteryRepository` |
| Chitkarsh | chitkarshrathi | Waitlist | 01.01.06, 01.02.01, 01.02.02, 02.09.01, 02.11.01 | `ui/waitlist/` (incl. `JoinLotterySection`, `RulesAgreementDialog`), Firestore `WaitlistRepository` |
| Aarav | aarav-kapoor-cs | Identity, Profile & Roles | 01.07.03, 01.08.01, 01.09.01, 03.11.01 | `ui/profile/`, `data/fake/CurrentUser.kt`, Firestore `UserRepository` |
| Zainab | zsaghir | Admin, Ratings & Policy | 01.11.01, 01.13.01, 03.04.01, 03.05.01 | `ui/admin/` (incl. `RatingSection`), Firestore `RatingRepository` |

Files marked OWNER: shared can be changed by anyone via a small PR reviewed by any teammate.

## AI usage

Files where AI helped are marked with an `// AI assistance:` line in their header.
