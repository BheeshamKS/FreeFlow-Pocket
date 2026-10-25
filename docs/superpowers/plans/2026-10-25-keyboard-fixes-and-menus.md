# FreeFlow Keyboard Fixes & Feature Menus Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the space button unresponsive tap bug, revamp the top bar and keyboard emoji buttons to open a full Samsung-style Emoji Menu with real-time emoji search, add a full Clipboard Menu sheet with cards, unclutter the bottom action row to match reference screenshots, verify every single button behavior, and build an installable APK for user testing.

**Architecture:** MVI architecture for `FreeFlowIMEService` with an expanded `KeyboardViewState` (`KEYBOARD`, `EMOJI_PICKER`, `EMOJI_SEARCH`, `CLIPBOARD`). The unified 10-column keyboard grid enforces strict proportional sizing. Pointer input in `Keycap` separates tap vs drag gestures to ensure zero-latency space insertion while retaining smooth cursor scrubbing. An in-memory/DataStore clipboard manager tracks recent copies, and a comprehensive emoji dataset with keyword search powers the emoji search bar.

**Tech Stack:** Kotlin 2.1.0, Jetpack Compose (BOM 2025.02.00), Material 3, Android InputMethodService, Android ClipboardManager, Coroutines & StateFlow, JUnit 4.

---

## Reference Analysis
Based on the 5 uploaded reference screenshots:
1. **Screenshot 1 (Main Keyboard):** Top bar with `[😊 (orange dot)]`, `[A 文]`, `[📋]`, `[⚙️]`, `[•••]`. Number row (1-0), QWERTY row, Home row (centered A-L), Shift row (Shift, Z-M, Backspace), Bottom action row (`[!#1]`, `[😊]`, `[English (UK)]`, `[.]`, `[🔍]`), and a clean bottom chin with `⌵` chevron at bottom-right.
2. **Screenshot 2 (Emoji Picker):** Top bar with `[⌨️]` return-to-keyboard icon, tabs (`😊` selected pill, GIFs, `:-)`, etc.). Category bar below with `🔍` Search button, `|` divider, and 9 category icons (`🕒`, `😊`, `🐶`, `🍴`, `🏠`, `🏀`, `📱`, `⁉️`, `🚩`). 8-column emoji grid. Bottom chin with `⌵`.
3. **Screenshot 3 & 4 (Emoji Search):** Search query input with cursor and `✕` clear button. When empty, displays "No recent searches". When query typed (e.g. "happy"), displays horizontal scrollable strip of matching emojis. Full QWERTY keyboard underneath to type search queries. Bottom chin with `⌵`.
4. **Screenshot 5 (Clipboard View):** Header with `[⌨️]` return-to-keyboard icon, `| Clipboard`, `[📌]` pin icon, `[🗑️]` clear icon. 3-column grid of rounded dark cards displaying clipboard text clips. Tapping a card pastes into active field. Bottom chin with `⌵`.

---

## Tasks

### Task 1: Fix Space Button Tap & Drag Gesture in `Keycap.kt`
- [ ] Fix pointer input in `Keycap.kt` so tapping the spacebar triggers `onKeyPress(key)` immediately, while horizontal dragging scrolls the cursor via `onCursorDrag(deltaX)`.
- [ ] Ensure tactile haptics, keypress sound, and Keys Cafe bloom animations fire reliably.

### Task 2: State Architecture & Key Routing in `FreeFlowIMEService.kt` and `KeyModel.kt`
- [ ] Introduce `KeyboardViewState`: `KEYBOARD`, `EMOJI_PICKER`, `EMOJI_SEARCH`, `CLIPBOARD`.
- [ ] Handle `KeyType.EMOJI` so clicking the bottom emoji key opens the Emoji Picker instead of typing "Emoji".
- [ ] Update `Smartbar.kt` so tapping the top emoji icon opens the Emoji Picker without committing any emoji or switching to word suggestions.
- [ ] Update `Smartbar.kt` so tapping the clipboard icon opens the Clipboard Menu without automatically pasting.
- [ ] Support dynamic `EditorInfo.imeOptions` on the Enter/Action key (search icon `🔍` with blue accent tint `#5AB4FF` when action is Search).

### Task 3: Unclutter Bottom Action Row & Perfect 10-Column Alignment
- [ ] Align all keyboard rows to a strict 10.0f weight budget:
  - Row 1 (Numbers): 10 keys x 1.0f = 10.0f
  - Row 2 (QWERTY): 10 keys x 1.0f = 10.0f
  - Row 3 (Home): 0.5f spacer + 9 keys x 1.0f + 0.5f spacer = 10.0f (home row nicely centered)
  - Row 4 (Shift row): 1.3f Shift + 7 keys x 1.0f + 1.7f Backspace = 10.0f
  - Row 5 (Action row): 1.3f `[!#1]` + 1.0f `[😊]` + 5.4f `[Space]` + 1.0f `[.]` + 1.3f `[Action]` = 10.0f
- [ ] Refine bottom bar padding and down chevron `⌵` position to ensure a clean, uncluttered finish matching Screenshot 1.

### Task 4: Implement Samsung-Style Emoji Menu View (`EmojiPickerView.kt`)
- [ ] Create `EmojiRepository` with curated categories matching reference (`🕒` Recents, `😊` Smileys, `🐶` Animals, `🍴` Food, `🏠` Travel, `🏀` Activities, `📱` Objects, `⁉️` Symbols, `🚩` Flags).
- [ ] Implement `EmojiPickerView`:
  - Top header with `[⌨️]` button to return to keyboard, tab pills with highlighted `😊` pill.
  - Category bar with `🔍` Search button, `|` divider, and category icons.
  - 8-column emoji grid (`GridCells.Fixed(8)`).
  - Tapping an emoji commits text (`ic.commitText(emoji, 1)`), updates recents, and triggers haptics.
  - Bottom bar with down chevron `⌵`.

### Task 5: Implement Emoji Search View (`EmojiSearchView.kt`)
- [ ] Implement `EmojiSearchView` matching Screenshots 3 & 4:
  - Top search bar with live query, cursor, and `✕` clear/close button.
  - "No recent searches" state when query is empty.
  - Real-time filtered horizontal emoji strip when query is typed (e.g. "happy" -> 😊 😃 😀 😁 😆 🤣 😂 🥹 ...).
  - Embedded QWERTY keyboard underneath for search typing (handling characters, space, backspace, and clear).
  - Tapping any search result emoji commits it to `ic.commitText(emoji, 1)`.

### Task 6: Implement Clipboard Sheet View (`ClipboardView.kt`)
- [ ] Create `ClipboardHistoryManager` to track clipboard items from Android `ClipboardManager`.
- [ ] Implement `ClipboardView` matching Screenshot 5:
  - Header: `[⌨️]` button, `| Clipboard`, `[📌]` pin toggle, `[🗑️]` clear clips button.
  - 3-column grid of rounded cards (`#18181C` background, `#2A2A32` border, 12dp corners).
  - Tapping a card commits its text (`ic.commitText(clipText, 1)`) and fires haptics.
  - Bottom bar with down chevron `⌵`.

### Task 7: Comprehensive Button Behavior Verification & Unit Tests
- [ ] Write unit tests for all keyboard buttons, layouts, shift states, space tap/drag, emoji routing, clipboard history, and emoji search filtering.
- [ ] Test every single key: 1-0, q-p, a-l, z-m, shift, backspace, space, period, action/search, symbols toggle, topbar buttons, hide chevron.

### Task 8: Build Installable APK & Output Artifact
- [ ] Compile release or debug APK via `./gradlew assembleDebug`.
- [ ] Copy output APK to `build_outputs/freeflow-pocket.apk`.
- [ ] Verify APK file integrity and provide direct path for installation.
