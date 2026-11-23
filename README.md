# Cyberpunk TCG Companion

A small, offline-first Android college project for managing a physical card
collection and saved decks.

## Development build

The current development build includes the following screens:

- Home
- Collection
- My decks
- Scanner
- Settings

### Home

- Shortcuts open Collection and My decks while restoring the state of tabs.
- Shows the number of saved decks and the three most recently
  created decks: name, Draft status, Main and Legends counts, and previews of
  selected Legends. Tapping a deck opens its editor; View all decks opens the full
  list. Unsaved edits are not shown here.
- Total cards, Unique cards and Owned editions update as quantities change.

### Collection

- All cards opens the offline catalog on first visit. Owned shows the user's stash,
  which starts with zero copies.
- Choose edition opens the available printings. The +/− buttons save counts to Room
  immediately. Removing the last copy hides the printing from Owned.
- The two-column grid shows owned editions and quantities. Tapping a card opens its
  details. Search, filters and scroll position are restored when returning to the grid.
- The catalog and owned grid display local artwork and gameplay stats. Missing
  values appear as a dash and sort after known values in either direction.

Build an APK with an approved catalog using `./gradlew assembleDebug -PcatalogAssetsDir=/path/to/assets`.
The assets directory must contain `collection/catalog.json` and any artwork referenced
by that file under `collection/card-images/`. Debug builds also support the local catalog
in `local-assets/debug` when the property is omitted.

### My decks

- Creating a deck with a nonblank name, listing decks with Draft status
  and separate Main/Legends counts, opening the editor, and deleting with confirmation 
  are implemented features.
- Deck Editor allows renaming a deck and selecting or removing up to three owned Legends
  with distinct names.
- Add cards browses owned Units, Programs and Gear from the same Room catalog as Collection.
  The +/− buttons change quantities; reducing a quantity to zero removes the card.
  Cards sharing the same name and subtitle are limited to three copies in total. (Legal game rule)
  Adding a card requires a selected Legend of the same color and an available owned copy.
  Quantities from different printings of one card are added together.
- Shows the deck composition, RAM limits for all four colors, and warnings about
  the Legend count, Main size (40–50), colors and RAM limits overflow.
  Incomplete decks can be saved as Drafts.
- Save draft writes the working copy to Room. Using Back with
  unsaved edits offers a choice to discard changes or keep editing.
- Deck Preview shows the current working copy, with Legends separate from Main,
  copy counts and access to full card details. Previewing does not save changes.
- Saved decks survive app restarts. Collection changes update availability and
  warnings about missing copies while retaining saved deck entries.
- Share on a saved deck opens Android's share chooser with a plain-text deck list.

### Scanner

- The Scanner button opens `ScannerActivity` with an explicit Intent. CameraX requests
  camera permission and starts a rear-camera preview. The screen handles permission
  denial, opening app settings, missing cameras and retrying.
- Take photo captures one image as a temporary JPEG in the app's private cache.
  Repeated capture requests are blocked while a photo is being taken.
- After capture, the full image is displayed with its EXIF orientation applied.
  Retake deletes the previous photo and reopens the camera. Capture and image-reading
  errors are shown on screen.
- Scanner automatically recognizes Latin-script text in the captured photo and shows
  the full result with line breaks in the scrollable photo review. The ML Kit model
  is bundled with the app and works offline on first use. The review shows when no
  text is found and offers Retry if recognition fails.
- The photo review suggests up to five cards from the local catalog using their names
  and subtitles. Approximate matches are labeled Possible match. Select a card,
  choose an edition if needed, and confirm to add one copy.
- A completed photo, its recognized text and suggestions survive rotation and return
  from the background. Leaving Scanner discards the scan. CameraX releases the camera
  during photo review and when leaving Scanner, and stops it in the background.
  The next scanner session clears files left after process termination.
- The scanner warns in low light while leaving capture available. Find card manually
  opens the offline catalog from capture or photo review, including after permission
  denial or a camera error. Choose an edition and confirm to add one copy.

### Settings

- Supports Light, Dark, and Auto themes across the app, including the system bars.
- The selection is stored locally in SharedPreferences and restored after restart.
  Dark is the default.
- Auto uses the ambient light sensor and falls back to the system theme while
  waiting for a reading or when the sensor is unavailable.

## Architecture Perspective

### Phase 1

The app uses one Android module, `app`. `MainActivity` is the entry point and
hosts `CompanionApp`, which contains the top and bottom bars. `AppNavHost` defines
the Compose Navigation routes. Card and deck routes carry the selected record's ID.
`MainActivity` and `ScannerActivity` share the Material 3 theme and system bar setup.

The app follows a simple MVVM structure. Compose screens display state from
ViewModels and pass user actions back to them. When the state changes, Compose
updates the parts of the screen that use it.

- Collection and the deck browser share
  `CardBrowserViewModel` logic. Each keeps its own search, filters and sort order.
- `DecksViewModel` is shared by Home and the deck screens. The editor and Preview
  show the working copy while you edit. Save draft updates the version shown on
  Home and My decks. Each `Deck` stores Legend IDs and `DeckItem` records with
  card IDs and copy counts. Kotlin functions in `domain` calculate RAM limits
  and deck warnings.
- CameraX handles the preview and takes one photo at a time in `ScannerActivity`.
  The Activity's `ScannerViewModel` tracks capture, photo review, and manual
  confirmation. Confirming an edition updates the same Room collection as the main app.

#### State and storage

- **Decks** and their ordered Legends and Main entries are stored in separate Room
  tables linked to gameplay card IDs. Unsaved working copies stay in the ViewModel.
  Room's version 1-to-2 automatic migration adds the deck tables and keeps the collection.
- **Browsing state** uses Android's saved UI state. `SavedStateHandle` keeps the
  search, filters and sort order. Compose Navigation saves the back stack and
  Compose saves the grid position. Android can restore these when it recreates
  the same task after the app process ends.
- **Scanner photos** are temporary JPEGs in the app's private cache. A captured
  photo stays in review through rotation or a return from the background while
  the process is alive. Retake or leaving Scanner discards the photo. After a
  process restart, Scanner starts a new capture session and clears leftover files.
