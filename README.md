# PRICEWATCH ELECTRONICS

**Track smarter. Buy at the right price.**

Native Android app (Kotlin, Jetpack Compose, Material 3, Clean Architecture + MVVM,
Room, Hilt, WorkManager, Retrofit + OkHttp). Electronics only — one top-level
category, 18 subcategories, 51 UK sites in the directory.

## Project status

Foundation + data/domain layers, presentation screens, WorkManager
worker/scheduler, NotificationHelper, and unit tests
(MoneyTest, PriceChangeTest, SiteDirectoryTest) are complete.
The project has **not** been compiled in this environment (no Android SDK /
Gradle build was run here) and **no APK/AAB has been built**. Do not claim a
successful build until `./gradlew assembleDebug` passes in Android Studio.

## Build

Prerequisites: Android Studio (Hedgehog or newer), JDK 17, Android SDK 34.

```bash
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # release AAB/APK (needs signing, below)
./gradlew testDebugUnitTest    # unit tests
```

`gradlew` / `gradlew.bat` are not included in this snapshot: run
`gradle wrapper` once in Android Studio, or copy the wrapper from a new
Android Studio project. `gradle-wrapper.properties` targets Gradle 8.7.

## Configuration

- `applicationId` com.pricewatch.electronics, `compileSdk`/`targetSdk` 34,
  `minSdk` 26, `versionCode` 1, `versionName` 1.0.0. BuildConfig is enabled;
  the version shown on Home comes from `BuildConfig.VERSION_NAME`.
- Money is stored as integer minor units (pence), never `Double`.
- Prices are GBP by default.

## Release signing (never commit secrets)

Create an untracked `keystore.properties` at the project root:

```properties
storeFile=/absolute/path/to/pricewatch-release.keystore
storePassword=<store password>
keyAlias=pricewatch
keyPassword=<key password>
```

Generate a keystore with:

```bash
keytool -genkeypair -v -keystore pricewatch-release.keystore \
  -alias pricewatch -keyalg RSA -keysize 2048 -validity 10000
```

`app/build.gradle.kts` reads this file only when it exists. Never commit
`*.keystore`, `*.jks`, `keystore.properties`, passwords or API secrets
(see `.gitignore`).

## Sites — honest integration statuses

Exactly 51 sites are seeded on first launch (`data/local/SiteSeedData.kt`).
No site is marked `LIVE_INTEGRATION`: no live integration has been implemented
and verified. eBay UK is `PARTIAL_SUPPORT`; every other site is `MANUAL_URL`
— the user saves the product URL and updates the price manually. Comparison
platforms (PriceSpy, PriceRunner, Idealo, Google Shopping, Kelkoo, Trolley)
are directories, not retailers. Respect each site's terms, rate limits,
`robots.txt` and anti-bot restrictions; never bypass auth or CAPTCHA.

## Failed-check rule (mandatory)

If a website request fails, the last known price is **never** overwritten,
no price-history record is inserted, and no increase/decrease is reported.
The attempt is marked `CHECK_FAILED`, failure details + timestamp are stored,
and the previous successful price and timestamp are preserved. Implemented in
`ProductRepositoryImpl.recordFailedCheck()` / `recordSuccessfulCheck()`.

## Background monitoring

WorkManager periodic work, unique name `pricewatch_periodic_check`, network
constraint + battery-not-low, exponential backoff, intervals 15/30/60/120 min.
15 minutes is the WorkManager minimum and execution is inexact.

## Structure

```
com.pricewatch.electronics
  app/            PriceWatchApp (Hilt, notification channels, WorkManager config),
                  MainActivity, Navigation (home, products, sites, history,
                  settings, add_product, product_detail/{id}, site_detail/{id})
  core/common     Result, Money (minor units), UrlValidator, Constants
  core/designsystem  Colours (#0A0F1D/#111827/#172033/#D4AF37/#00B4D8…),
                  Type, Theme, Spacing/Shapes, components (gold button,
                  StatCard, ProductCard, LoadingGoldBall, Empty/ErrorState)
  core/network    ApiService, PriceProvider interface + ManualPriceProvider
  data/local      Room entities, DAOs, PriceWatchDatabase (v1), Mappers,
                  SiteSeedData (51 sites)
  data/repositories  Product/History/Site/Settings/CheckLog implementations
  data/providers  ProviderRegistry (manual fallback today)
  domain/models   Product, PriceHistory, Site, CheckProgress, enums
                  (Category ELECTRONICS only; 18 subcategories)
  domain/repositories  Repository interfaces (incl. checkAllNow progress flow)
  domain/usecases Add/Delete/GetProducts, GetPriceHistory, ValidateProduct,
                  CalculatePriceChange
  di/             DatabaseModule, NetworkModule, AppModule (Hilt)
  workers/        PriceCheckWorker, WorkScheduler
```

## Known limitations

- No live website integration is verified; automated checking falls back to
  manual price updates for all 51 sites today.
- `PriceWatchDatabase.MIGRATION_1_2` is a documented placeholder for a future
  v2 bump; the shipped schema is v1 and no migration is applied.
- No Gradle build has been run from this workspace; compile in Android Studio
  and fix any errors the parallel presentation work introduces (notably the
  screen signatures in `Navigation.kt` vs. the screens being written
  concurrently, and `Product.id` being `Long` while one screen passes it as
  a `String`).
