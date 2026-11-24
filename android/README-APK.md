# Banat Bot Console — Android APK Guide

Kasama sa `android/` folder ang ready-to-build Android app na may **In-App Facebook Login** (`FB Login` button). Kapag nag-login ang user sa Facebook sa loob mismo ng APK, kusang kinukuha ng app ang `c_user` at `xs` cookies at ipinapasa sa Banat Console—hindi na kailangan mag-export o mag-paste ng JSON cookies.

## Paraan 1: Build APK gamit ang GitHub (Walang Android Studio sa PC)

1. I-download ang project code ZIP sa dashboard (**Download Project Code (.zip)**) at i-upload/push sa isang GitHub repository.
2. Sa GitHub repo mo, buksan ang **Actions** tab → piliin ang **Build Android APK** → i-click ang **Run workflow**.
3. Pagkatapos ng build (~2 minutes), i-download ang **`banat-bot-console-apk`** artifact at i-install ang `app-debug.apk` sa Android phone mo.

## Paraan 2: Build APK gamit ang Android Studio (PC)

1. I-install ang **Android Studio** at Android SDK (API 34).
2. Buksan ang Android Studio → **Open** → piliin ang **`android/`** folder ng project na ito.
3. Hintaying matapos ang Gradle sync.
4. Sa menu bar: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Makikita ang generated `.apk` sa `android/app/build/outputs/apk/debug/app-debug.apk`.

## Paano Gamitin ang APK

1. Buksan ang **Banat Bot Console** app sa phone.
2. Kung iba ang server URL mo, pindutin ang **Server** button sa itaas at ilagay ang live URL ng iyong hosted Banat Bot server.
3. Mag-sign in sa Admin Console.
4. Pindutin ang **FB Login** sa itaas (o **Login Facebook sa App** button sa Controls tab) at mag-login sa Facebook account ng bot.
5. Kusang makukuha ang session at automatic na mag-o-online ang bot—pwede mo nang gamitin ang **Commands & GC** tab (**Auto Target User** at **Fast Attack**).
