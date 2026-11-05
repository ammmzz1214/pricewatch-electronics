# Presentation layer — contract notes

All presentation files live under
`app/src/main/java/com/pricewatch/electronics/presentation/` and are written
against the foundation that exists in this project (domain models, repositories,
use cases, `core.designsystem`, `core.common.Money/UrlValidator/Result`,
`data.providers.ProviderRegistry`, `data.local.SiteSeedData`). No foundation
file was modified.

## Screen signatures (must match `app/Navigation.kt` exactly)

| Screen | Signature |
|---|---|
| `home.HomeScreen` | `(onNavigateToProduct: (Long) -> Unit, onNavigateToAddProduct: () -> Unit, onNavigateToSettings: () -> Unit)` |
| `products.MyProductsScreen` | `(onNavigateToProduct: (Long) -> Unit, onNavigateToAddProduct: () -> Unit)` |
| `products.AddProductScreen` | `(onProductCreated: (Long) -> Unit, onNavigateBack: () -> Unit)` |
| `products.ProductDetailScreen` | `(productId: Long, onNavigateBack: () -> Unit)` |
| `sites.SitesScreen` | `(onNavigateToSite: (Long) -> Unit)` |
| `sites.SiteDetailScreen` | `(siteId: Long, onNavigateBack: () -> Unit)` |
| `history.HistoryScreen` | `(onNavigateToProduct: (Long) -> Unit)` |
| `settings.SettingsScreen` | `()` |

IDs are `Long` everywhere (`Product.id`, `Site.id`). `Product` carries no
`siteName` — ViewModels resolve `siteId -> name` from `SiteRepository`
(`HomeViewModel.siteNames`, `ProductsViewModel.siteNames`).

## Design-system reuse (no duplication)

- Theme, colours, spacing, shapes, typography: `core.designsystem`
  (`PriceWatchTheme`, `PriceWatchColors`, `PriceWatchSpacing`,
  `PriceWatchShapes`). The earlier `presentation/theme/PriceWatchTheme.kt`
  draft was **deleted** — it duplicated the foundation theme.
- `presentation/components/StatCard`, `GoldProgressBall`, `ErrorView`
  delegate to the design-system `StatCard`, `LoadingGoldBall`, `ErrorState`.
- `presentation/components/ProductCard` is a separate implementation (not a
  duplicate of the design-system one): it adds the price-difference text,
  target-reached badge, status chip and an optional delete button.
- Money formatting/parsing uses `core.common.Money` (`format`,
  `fromDecimalString`, `percentageChange`, `isValidPrice`); URL validation
  uses `core.common.UrlValidator`; `Result<T>` is `core.common.Result`.

## Check Now — what is real

`presentation/util/PriceCheckRunner` runs a real check per product:
`SiteRepository.getSite` -> `ProviderRegistry.providerFor(site)` ->
`PriceProvider.fetchCurrentPrice(url)` -> `ProductRepository
.recordSuccessfulCheck / recordFailedCheck`. Progress is real
completed/total; duplicate runs are guarded in `HomeViewModel`.

Honest limitation: today every seeded site resolves to
`ManualPriceProvider` (eBay is partial support and also declines automated
fetching), so Check Now records `CHECK_FAILED` with the provider's message
and **never overwrites the last known price** (enforced in
`ProductRepositoryImpl`). When a verified live provider lands, the same
code path records real price changes and history — no presentation change
needed. The UI says this plainly on Add Product and in the Sites screens.

## Known seams / follow-ups for other agents (not presentation work)

1. **Background worker does not exist yet.** `SettingsViewModel` persists the
   monitoring interval (15/30/60/120), toggles and alert preferences in Room
   (`MonitoringSettings`). The worker agent should read
   `MonitoringSettings.intervalMinutes / monitoringEnabled` and schedule
   `PERIODIC_WORK_NAME` accordingly. Presentation deliberately references no
   worker class, so it compiles without one.
2. **Theme preference.** `MonitoringSettings` has no theme field, so the
   theme (Dark / System) is persisted in DataStore
   (`presentation/settings/ThemePreferences.kt`). `MainActivity` currently
   always applies the dark `PriceWatchTheme`; to honour System it should
   read `observeThemePreference()` and pass the flag through. The stored
   value is correct either way and the app stays dark (the brand default).
3. **Edit product.** The NavHost has no edit route, so editing is an
   in-place dialog on `ProductDetailScreen` (name, price, target,
   monitoring toggle). A manual price edit is recorded via
   `recordSuccessfulCheck(..., source = "manual_edit")` so history stays
   truthful.
4. **Support email placeholder.** `SettingsScreen` uses
   `support@pricewatch-electronics.co.uk` for Contact Support. Confirm or
   replace this address before release.
5. **Site favicons.** `SitesScreen.SiteLogo` loads favicons from
   `https://www.google.com/s2/favicons?domain=<domain>&sz=64` (Google's
   public favicon service) with a monogram tile behind it. Replace with
   bundled logos if offline use is required.
6. **HomeScreen replacement.** A partial 85-line `HomeScreen.kt` from the
   foundation pass was replaced by the complete dashboard required by this
   task (stats, sort/filter, Check Now progress, alerts dialog) using the
   same NavHost signature.

## Invariants enforced in presentation

- Category is locked to `Category.ELECTRONICS` — no category picker exists.
- Subcategories: all 18 `Subcategory` enum values, via `displayName`.
- Sites: the 51 seeded sites come from `SiteRepository.observeSites()`;
  the UI never hardcodes the list.
- Money is integer minor units everywhere; no `Double` price storage.
- No `TODO`, no dead buttons: every button navigates, calls a ViewModel,
  opens a dialog, or shows a snackbar. Delete flows always confirm first.
- No APK build is claimed: this layer was written and cross-checked
  against the foundation sources, but no Gradle build was run in this VM.
