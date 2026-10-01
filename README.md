# TrueTag 🏷️⚡

> **Point your camera. See the real price. No surprises.**

In the United States, sales tax is calculated and added at checkout, meaning the shelf sticker price is never what you actually pay at the cash register. Furthermore, tax rates vary wildly across state, county, and city boundaries (e.g., from 0% in Oregon to over 10.25% in Chicago).

**TrueTag** solves this everyday problem using on-device optical character recognition (ML Kit) paired with an instant, completely offline local tax database to calculate the true checkout price on the fly.

---

## 🚀 The 60-Second Demo Path (Shipaton Walkthrough)

1. **Splash Screen**: Watch the animated logo seamlessly morph from a rounded price tag into a crisp clarity checkmark.
2. **Scan Shelf Tag**: Point camera at a price tag or tap **Demo Scan** (`Coffee Beans ($12.99)`).
3. **Smart Price Insight**: See the animated green flash + checkmark, then watch the result bottom sheet spring up with:
   - Shelf Tag: `$12.99`
   - Estimated Tax: `+$1.33 (10.25%)`
   - Real Checkout Price: `=$14.32`
   - **Smart Insight**: *"🟢 Good deal — 14% below typical average"*
4. **Live Location Breakdown**: Tap the top location chip (`📍 Chicago, IL · Cook County · 10.25%`) to reveal the detailed breakout:
   - State Tax: `6.25%`
   - Cook County Tax: `1.75%`
   - Chicago City Tax: `2.25%`
   - Expandable *"Why this rate?"* explainer.
5. **Add to Cart & Budget Bar**: Add items to cart and watch the live **Budget Mode** progress bar (`$23.40 / $50.00`). Watch the total rolling counter dynamically roll up!
6. **AI Shopping Money Coach**: Tap the floating Money Coach AI bot bubble:
   - Ask: *"Is this a good deal?"* or *"How much tax am I paying today?"*
   - Context-aware bot analyzes current cart, local tax, and budget with streaming text reveal.
7. **Receipt Scanner**: Toggle scan mode to **Store Receipt** and tap **Demo Scan Receipt**. ML Kit extracts line items and auto-splits the subtotal and sales tax! Tap **Save to Trips**.
8. **Tax Tracker (Pro)**: View monthly sales tax summary (`$47.82`), animated weekly bar chart, and the *"If you lived in Oregon, you'd have saved $47.82"* comparison card.
9. **Gamification & Profile**: Check unlocked badges (*First Scan*, *10 Items Scanned*, *Stayed Under Budget*, *Receipt Master*), weekly streak counter, and tap a badge for a celebration confetti burst!

---

## 🌟 Comprehensive Feature Set

### 1. Real GPS + Smart Location Layer
- `FusedLocationProviderClient` retrieves current GPS location.
- Reverse geocoding provides street address, city, state, ZIP, and county.
- Live chip on main screen: `📍 Chicago, IL · Cook County · 10.25%`.
- Tap chip to open the **Location Detail Sheet** with statutory breakout rates (State, County, City) and *"Why this rate?"* explainer.
- Manual location override and search dialog covering 70+ US cities.
- Caches last known location for zero-delay instant reopen.
- Friendly animated prompt if GPS is disabled.

### 2. Context-Aware AI Money Coach Assistant
- Floating chat bubble on every screen.
- The assistant is a dedicated shopping money coach with live context of:
  - Active cart contents and live checkout total
  - Current location and tax rate breakdown
  - User's budget and spent amount
  - Past shopping history from local database
- Quick question chips (*"Is this a deal?"*, *"Total with tax?"*, *"Cheaper option: $4.99 vs $6.49?"*, *"Am I over budget?"*).
- Typing indicator with 3 bouncing dots and 30ms word-by-word streaming reveal.
- Works 100% offline with zero external network required.

### 3. Smart Price Insights
- Evaluates scanned items against a bundled local reference database (`price_reference.csv`).
- Displays instant feedback:
  - 🟢 *Good deal — 12% below average for this item*
  - 🟡 *Fair price — in line with typical*
  - 🔴 *High — you'd pay less at Target*
- Includes benchmark store comparison.

### 4. Budget Mode
- Set shopping budget in settings (default `$50.00`).
- Progress bar under top bar and cart total (`$23.40 / $50.00`).
- Color shifts from Green (<75%) to Yellow (75%-99%) to Red (>=100%).
- Gentle haptic warning and shake animation when budget is exceeded.

### 5. Monthly Tax Tracker (Pro)
- Hero stat: *"You paid $47.82 in sales tax this month."*
- Animated weekly bar chart growing upward on load.
- *"If you lived in Oregon, you'd have saved $47.82"* comparison card.
- Breakdown of tax paid by store (Target, Trader Joe's, Costco).
- **Export to CSV** button to share tax records via Android Share Sheet.

### 6. Saved Trips
- List of recorded trips with store name, date, item count, total price, and tax.
- Tap a trip to view all individual item prices and taxes.
- Long-press / dropdown menu to **Rename**, **Duplicate**, **Delete**, or **Share** receipt summary.

### 7. Receipt Scanner
- Dual scan modes: **Shelf Tag** and **Store Receipt**.
- Extracts line items, subtotal, and tax.
- Auto-splits total into pre-tax and sales tax using local statutory rate.
- One-tap save to your shopping trips and Tax Tracker.

### 8. Quick Access Widget Preview
- Glance-style widget card showing live cart total and tax breakdown.
- Instant shortcut to open scanner.

### 9. Notifications
- Configurable daily reminder (*"Don't forget to check the real price 💰"*).
- Store proximity nudge option.

### 10. Gamification & Badges
- Annual tax tracked counter.
- Badges: *First Scan*, *10 Items Scanned*, *Stayed Under Budget*, *Receipt Master*.
- Day streak tracker.
- Custom Canvas confetti particle burst on badge unlock and milestones.

### 11. Judge Demo Mode (Shipaton Ready)
- Built-in toggle in Profile/Settings:
  - Fakes 5 pre-loaded scans with realistic items
  - Fakes Chicago GPS coordinates
  - Fakes saved trip history
  - Provides instant Money Coach AI answers
  - Unlocks Pro tier to remove all judging friction!

### 12. Polish, Animations & Haptics
- Animated splash screen with tag morphing into a checkmark.
- Corner brackets gently pulse (`1.0 → 1.04 → 1.0` scale, 1.8s ease-in-out).
- Green detection flash + 300ms animated checkmark path drawing.
- Spring physics on bottom sheets (`dampingRatio = 0.75f, stiffness = 300f`).
- Staggered sequence for price lines (120ms apart).
- Rolling animated counter for cart checkout total.
- Native haptic and audio feedback on scan success, budget warning, and button clicks.

---

## 🏗️ Architecture

- **Architecture**: MVVM with ViewModel, StateFlow, and Repository pattern.
- **Local Persistence**: **Room Database** (`androidx.room`) with reactive `Flow` queries for Cart items, Trips, Chat history, and User stats.
- **Camera & OCR**: CameraX (`camera2`, `view`, `lifecycle`) + Google ML Kit Text Recognition running entirely on-device.
- **Location**: `play-services-location` `FusedLocationProviderClient` with `Geocoder` reverse geocoding.
- **Monetization**: RevenueCat Purchases SDK (`purchases:8.2.1`).
- **UI & Motion**: 100% Jetpack Compose with Material Design 3, custom brand green `#00C853`, and rounded 20dp cards.

---

## 🚀 Running the App

```bash
# Build debug APK
gradle assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```
