# Chorus Music: Autonomous Overnight Test & Glitch Repair Orchestrator

## Overview
This orchestration engine runs continuous autonomous testing, UI layout glitch detection, and automatic code remediation for **Chorus Music** (`pushkar.chorus.music`).

## Architecture & Responsibilities
1. **Device Sensor & ADB Runner**: Deploys the latest debug APK to attached Android hardware or local emulator, navigates key routes (Home, Explore, Library, Artist, Player, Lyrics, Settings).
2. **Visual & UI Validator**: Analyzes layout hierarchy, overscroll behavior, Material 3 UI rendering, and syllable animation frame-rate.
3. **Glitch Classifier**: Ingests crash reports, UI clipped elements, and broken constraints into structured task items.
4. **Autonomous Fixer**: Modifies source files, runs compilation checks via Gradle (`./gradlew :app:compileDebugKotlin`), verifies fixes, and logs outcome.
5. **State Ledger**: Persists progress and test coverage in `.planning/STATE.md`.

## Execution Protocol
```powershell
powershell -ExecutionPolicy Bypass -File .\automation\run-overnight-loop.ps1 -MaxIterations 20
```

## Safeguards
- **Strict Verification Gate**: Code edits must pass clean Gradle build before acceptance.
- **Fail-safe Revert**: If 3 consecutive repair attempts on an issue fail, changes are reverted cleanly.
