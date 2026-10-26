# Trails - Campsite Reservation System

Created by Team QUADLE for CMPUT 301 (Fall 2026).

Our project documentation, including the **Product Backlog**, **UI Mockups**, **Object-Oriented Analysis**, and **Meeting Logs**, is hosted entirely in our GitHub Wiki.

**[Click here to view the Trails Wiki](https://github.com/cmput301f26-quadle/quadle-trails/wiki)**

## How to run

1. Open the `Trails/` folder (not the repo root) in Android Studio.
2. Let Gradle sync finish.
3. Run the `app` configuration on an emulator or device running API 24 or higher.

The application id is `com.example.quadletrails` so it matches the Firebase project in `google-services.json`. The Kotlin package is `com.example.trails`.

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
