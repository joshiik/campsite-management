# Campsite Management System

A JavaFX campsite manager built around custom ADTs and a singly linked list. The
model is separate from the interface, so the main data is not stored in JavaFX
controls.

## Requirements

- JDK 21 or later
- Maven 3.9 or later

## Run

On Windows, with Maven on `PATH`, run:

```text
run.bat
```

`run.bat` compiles the application and launches JavaFX. Run the unit tests with:

```text
mvn test
```

The application saves and loads `campsite-data.bin` in its working directory.
The **Save data** and **Load data** buttons control persistence; saved data is
also loaded automatically when the application starts and the file exists.
Use **Load sample data** on the system tab to replace the current data with
example campsites, plots and guests for trying Smart Find. The sample data is
saved automatically; the application asks for confirmation before replacing
existing data.
Reset clears both the in-memory system and that saved file.
The Windows launcher uses a relative classpath so the application also runs
when the project folder contains non-English characters.

Bookings require enough beds for the requested guest count in static
accommodation. Empty pitches do not have a built-in guest limit because the
assignment does not define one. Smart find filters by every requested
requirement, then prefers the highest campsite star rating and the lower nightly
price.

## Main parts

- `CustomList<T>` is the project's generic singly linked list.
- The classes in `campsite.model` define campsites, areas, plots, guests,
  booking preferences and bookings. `CampsiteManager` contains the application
  rules, availability checks, smart search and persistence.
- `CampsiteApp` is the JavaFX interface. It calls the model rather than using
  controls as the source of the application's data.
- Unit tests in `src/test/java` cover list behavior, validation, booking
  selection and conflicts, cancellation, plot removal, smart search and saving.

All primary model collections use the custom linked list rather than Java
collection classes or regular arrays.
