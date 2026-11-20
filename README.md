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

- Uses CameraX: opening the tab requests CAMERA permission and starts a rear-camera
  preview. Handles permission denial, opening app settings, missing cameras and retrying.
- Take photo captures one image as a temporary JPEG in the app's private cache.
  Repeated capture requests are blocked while a photo is being taken.
- After capture, the full image is displayed with its EXIF orientation applied.
  Retake deletes the previous photo and reopens the camera. Capture and image-reading
  errors are shown on screen.
- A completed photo survives rotation and moving the app to the background.
  Leaving Scanner through another tab or Back resets the photo. The camera is
  released during photo review and when leaving the screen; lifecycle handling
  stops it in the background. Files left after the process ends are cleaned up
  the next time Scanner opens.
- OCR, card matching, manual confirmation and adding cards to the collection are
  not implemented yet. Scanner runs inside MainActivity; a separate ScannerActivity
  and an explicit Intent to open it have not been added yet.

### Settings

- Supports manual Light/Dark switching across the app, including the system bars.
- The selection is stored locally in SharedPreferences and restored after restart.
  Dark is the default.
- Auto mode and ambient light sensor integration are not implemented yet.

## Architecture Perspective

### Phase 1

The current implementation uses one Android module, `app`. Its only activity,
`MainActivity`, is the entry point. It applies the Material 3 theme and hosts
`CompanionApp`, which contains the top and bottom bars. `AppNavHost` defines the
Compose Navigation routes. Card and deck routes carry the selected record's ID.

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
- Scanner is a tab in the same activity. CameraX handles the camera preview and
  takes one photo at a time. `ScannerViewModel` tracks the capture and the photo
  being reviewed.

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
