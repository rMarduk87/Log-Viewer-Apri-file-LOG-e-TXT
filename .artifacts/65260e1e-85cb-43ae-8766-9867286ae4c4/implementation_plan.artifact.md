# Implementation Plan - Next/Prev Error Navigation

Add navigation buttons to jump between error logs when the user is not exclusively filtering by error.

## User Review Required

> [!IMPORTANT]
> I will define "no filter active" as the state where more than one `LogType` is selected, or specifically when the view is not restricted to `ERROR`. I'll add a "Show All" toggle or simply allow the buttons when `selectedTypes` contains types other than `ERROR`.

## Proposed Changes

### [LogViewModel] Navigation Logic

#### [MODIFY] [LogViewModel.kt](file:///C:/Users/Riccardo.Pezzolati/Log-Viewer-Apri-file-LOG-e-TXT/app/src/main/java/rpt/tool/logviewer_aprifilelogetxt/ui/log/LogViewModel.kt)
- Add `isFilterActive` helper: returns true if the current view is restricted to a subset of types.
- Add `findNextError(fromIndex: Int): Int?`: finds the index of the next `LogType.ERROR` in `displayedLines`.
- Add `findPrevError(fromIndex: Int): Int?`: finds the index of the previous `LogType.ERROR` in `displayedLines`.
- Modify `toggleType` to allow selecting all types easily.

### [UI] Navigation Buttons

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Riccardo.Pezzolati/Log-Viewer-Apri-file-LOG-e-TXT/app/src/main/java/rpt/tool/logviewer_aprifilelogetxt/MainActivity.kt)
- Add a navigation bar/row containing "Next Error" and "Prev Error" buttons.
- These buttons will be visible only when the user is NOT filtering by ERROR exclusively.
- Use `CoroutineScope` to call `listState.animateScrollToItem()`.

#### [MODIFY] [strings.xml](file:///C:/Users/Riccardo.Pezzolati/Log-Viewer-Apri-file-LOG-e-TXT/app/src/main/res/values/strings.xml)
- Add `next_error` and `prev_error` strings.
- Update `values-it/strings.xml`.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.

### Manual Verification
- Open a log file.
- Change filters to show more than just errors (e.g., select ALL).
- Use the Next/Prev buttons to jump between errors.
- Verify that the buttons are hidden or disabled when only errors are shown.
