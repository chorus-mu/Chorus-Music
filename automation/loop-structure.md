# Chorus Music: Autonomous Loop Lifecycle & Worker Pipelines

## State Machine

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> SyncBuild: Trigger
    SyncBuild --> DeployAdb: Compile OK
    SyncBuild --> RemediateBuild: Compile Error
    DeployAdb --> ExecuteScenarios: App Running
    ExecuteScenarios --> AnalyzeLogs: Capture Crash/Glitch
    AnalyzeLogs --> GenerateTasks: Glitch Found
    AnalyzeLogs --> CycleComplete: Clean Run
    GenerateTasks --> WorkerAgent: Prioritize Issue
    WorkerAgent --> VerifyFix: Apply Patch
    VerifyFix --> SyncBuild: Re-test
    CycleComplete --> [*]
```

## Scenario Coverage Matrix
1. **App Launch & Theme**:
   - Launch in Material UI -> verify Monet theme, bottom nav items.
2. **Artist Full-Bleed Hero**:
   - Open artist page -> pull down for parallax zoom, test Play/Shuffle/Follow buttons.
3. **Lyrics Synchronization**:
   - Open full player -> switch to Lyrics view -> verify syllable glow, word bounce float, translation line display.
4. **AI DJ Set Builder**:
   - Queue sequence generation -> test Camelot wheel key adjacency and BPM pacing.

## Exit Criteria
- Zero runtime crashes over 100 navigation cycles.
- Zero unresolved syntax/lint errors in `:app:compileDebugKotlin`.
