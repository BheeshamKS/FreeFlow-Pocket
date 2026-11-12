## Summary

<!-- Provide a concise description of the purpose, scope, and context of this change. -->

- **Feature/Fix**: 
- **Context/Issue**: 

---

## UI Unslop & Design System Audit

<!-- For UI/UX changes, ensure alignment with FreeFlow's Graphite AMOLED design system. -->

- [ ] **Design System Compliance**: Uses standardized theme tokens (`DarkRaised`, `DarkCard`, `DarkCardBorder`, `SurfaceDark`) rather than hardcoded hex colors.
- [ ] **Contrast Compliance**: Primary action buttons/badges enforce accessible text and icon contrast (e.g. `OnPrimaryBlue` on `PrimaryBlue` with >9.4:1 contrast ratio, never pure white on bright blue).
- [ ] **Typography & Glyphs**: Removed unicode badge glyphs (e.g., `✓`, `✖`, `〰`) and emoji decoration from headers, status indicators, and action labels.
- [ ] **Clean Motion & Overlays**: Restrained, hardware-accelerated animations without loud neon glows, excessive drop shadows, or unstyled borders.

---

## Test Coverage & Impact

<!-- Describe the unit tests, instrumented tests, or manual test scenarios covered. -->

- [ ] Unit tests added or updated
- [ ] Compose UI / screen tests verified
- [ ] No regression to IME keyboard lifecycle, overlay HUD lifecycle, or audio streaming

---

## Verification Checklist

Please ensure the following commands run cleanly before requesting review:

- [ ] `JAVA_HOME=... ./gradlew compileDebugKotlin` passes with 0 errors
- [ ] `JAVA_HOME=... ./gradlew lintDebug` passes without critical issues
- [ ] `JAVA_HOME=... ./gradlew test` passes all unit tests
- [ ] Tested on target Android device or emulator (API 26+)
