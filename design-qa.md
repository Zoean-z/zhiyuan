# Main Screen Design QA

## Target and evidence
- Reference: `C:\Users\Lenovo\.codex\generated_images\019fd675-e582-7803-ae7d-e9f7a490434b\exec-1cd958d8-5294-47b8-abaf-b2dabe2092e9.png`
- Implementation: `_verify/main-home-final.png`
- Side-by-side comparison: `_verify/main-reference-comparison.png`
- Viewport: 1440 x 1024
- State: logged-in text query with recognized conditions, non-empty stable results, two-column school cards, and live backend data.

## Visual review
- P0: none.
- P1: none.
- P2: live university data and actions differ from the static mock, as required by the real interfaces; school marks use the existing icon library instead of fabricated logos.
- Layout: sidebar, top bar, query area, risk tabs, card grid, spacing, borders, and typography match the selected restrained blue-white direction.
- Usability: no duplicate empty state; no visible overlap, clipping, or horizontal overflow at the target viewport.

## Interaction review
- Text query submits successfully, renders parsed conditions, risk groups, and the backend AI summary.
- Score query supports school-first and major-first modes.
- School-first opens the major detail drawer; major-first exposes add-to-plan actions.
- Switching query type or priority mode does not display an incompatible previous result.
- New results automatically activate the first non-empty risk group.

final result: passed
