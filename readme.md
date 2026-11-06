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

There is no sign-in. The app runs as one of three realistic sample users, so every role can be tested on one device.

| User | Roles | Opens in |
|---|---|---|
| Priya Sharma | Camper | Camper (Browse) — the default |
| Daniel Whitehorse | Owner, Camper | Owner (Hosting) |
| Morgan Lee | Admin, Owner, Camper | Admin (Admin home) |

**Switch user (no code changes needed):**
1. Tap the **Settings** tab (gear icon) → **Profile**.
2. Tap the green **"Signed in with this device"** card to open **Device profile**.
3. Scroll to **"Testing: run as a sample user"** and tap Priya, Daniel or Morgan.
4. The app restarts as that user, on their home screen.

**Switch mode (US 03.11.01):** as Morgan or Daniel, go to **Settings → Profile**, tap the mode pill at the top right (e.g. **Admin ▾**), pick a mode and tap **Switch to …**. Users only see the roles they hold, so Priya cannot switch.

**Profile stories (US 01.07.03, 01.08.01, 01.09.01):** on **Settings → Profile**, edit name, email or phone and tap **Save Changes**, or tap **Delete Profile** and confirm. Device profile shows the masked device identifier.

**Data:** campsites, lotteries and waitlists are stored in Firestore. Users and ratings use in-memory sample data for this checkpoint, so profile changes reset when the app restarts. Firebase anonymous sign-in and Firestore storage for users are built and will be switched on in Part 4.

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
