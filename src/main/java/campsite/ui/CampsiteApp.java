package campsite.ui;

import campsite.model.Booking;
import campsite.model.BookingRequest;
import campsite.model.Campsite;
import campsite.model.CampsiteManager;
import campsite.model.CampingArea;
import campsite.model.CustomList;
import campsite.model.Guest;
import campsite.model.Plot;
import campsite.model.PlotMatch;
import campsite.model.PlotType;
import campsite.model.PowerPreference;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class CampsiteApp extends Application {
    private static final Path DATA_FILE = Path.of("campsite-data.bin");

    private CampsiteManager manager = new CampsiteManager();
    private final ListView<Campsite> campsiteList = new ListView<>();
    private final ListView<CampingArea> areaList = new ListView<>();
    private final ListView<Plot> plotList = new ListView<>();
    private final ListView<Booking> bookingList = new ListView<>();
    private final TextArea output = new TextArea();
    private final Label status = new Label("Ready");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Campsite Management System");
        stage.setMinWidth(1100);
        stage.setMinHeight(650);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(createManagementTab());
        tabs.getTabs().add(createBookingTab());
        tabs.getTabs().add(createSystemTab());

        output.setEditable(false);
        output.setWrapText(true);
        output.setPromptText("Choose an action to see information here.");
        VBox.setVgrow(output, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setTop(new Label("Campsite Management System"));
        root.setCenter(tabs);
        root.setBottom(new VBox(6, new Label("Results and notifications"), output, status));
        BorderPane.setMargin(tabs, new Insets(12, 0, 12, 0));

        stage.setScene(new Scene(root, 1250, 760));
        stage.show();
        refreshCampsites();
        if (java.nio.file.Files.exists(DATA_FILE)) {
            try {
                manager = CampsiteManager.load(DATA_FILE);
                refreshCampsites();
                showStatus("Loaded saved data from " + DATA_FILE);
            } catch (IOException exception) {
                showError("Could not load saved campsite data: " + exception.getMessage());
            }
        }
    }


    
    private Tab createManagementTab() {
        VBox actions = new VBox(8,
                actionButton("Add campsite", this::addCampsite),
                actionButton("Add camping area", this::addArea),
                actionButton("Add plot / pitch", this::addPlot),
                actionButton("Add guest", this::addGuest),
                actionButton("Remove plot / pitch", this::removePlot));
        actions.setPadding(new Insets(10));
        actions.setPrefWidth(190);

        HBox drillDown = new HBox(10,
                listPanel("Campsites", campsiteList),
                listPanel("Camping areas", areaList),
                listPanel("Plots / pitches", plotList),
                listPanel("Bookings", bookingList));
        campsiteList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> refreshAreas(newValue));
        areaList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> refreshPlots(newValue));
        plotList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showPlotDetails(newValue));
        bookingList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showBookingDetails(newValue));

        BorderPane content = new BorderPane();
        content.setLeft(actions);
        content.setCenter(drillDown);
        BorderPane.setMargin(drillDown, new Insets(10));
        return new Tab("Manage and browse", content);
    }

    private Tab createBookingTab() {
        VBox actions = new VBox(8,
                actionButton("Add booking", this::addBooking),
                actionButton("Cancel booking", this::cancelBooking),
                actionButton("View campsite bookings", this::viewCampsiteBookings),
                actionButton("View all pending bookings", () ->
                        output.setText(manager.allPendingBookings())),
                actionButton("View guest record", this::viewGuest),
                actionButton("Smart find a holiday", this::smartFind));
        actions.setPadding(new Insets(12));
        return new Tab("Bookings and guests", actions);
    }

    private Tab createSystemTab() {
        VBox actions = new VBox(8,
                actionButton("Save data", this::saveData),
                actionButton("Load data", this::loadData),
                actionButton("Load sample data", this::loadSampleData),
                actionButton("Reset all data", this::resetData),
                new Label("Data file: " + DATA_FILE.toAbsolutePath()));
        actions.setPadding(new Insets(12));
        return new Tab("Save, load and reset", actions);
    }

    private VBox listPanel(String title, ListView<?> list) {
        VBox panel = new VBox(6, new Label(title), list);
        panel.setMinWidth(170);
        HBox.setHgrow(panel, Priority.ALWAYS);
        VBox.setVgrow(list, Priority.ALWAYS);
        return panel;
    }

    private Button actionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> runSafely(action));
        return button;
    }




    /// add campsite, area, plot, guest, booking
    private void addCampsite() {
        InputForm form = new InputForm("Add campsite");
        form.add("Campsite ID (positive number)");
        form.add("Name");
        form.add("Address");
        form.add("Stars (1-5)");
        form.add("Contact information");
        form.add("Facilities (comma separated)");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        Campsite campsite = manager.addCampsite(Integer.parseInt(values.get(0)),
                values.get(1), values.get(2), Integer.parseInt(values.get(3)),
                values.get(4), CampsiteManager.parseFacilities(values.get(5)));
        refreshCampsites();
        showStatus("Added " + campsite);
    }

    private void addArea() {
        InputForm form = new InputForm("Add camping area");
        form.add("Campsite ID");
        form.add("Area identifier");
        form.add("Description");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        CampingArea area = manager.addArea(Integer.parseInt(values.get(0)),
                values.get(1), values.get(2));
        refreshCampsites();
        showStatus("Added area " + area.identifier());
    }

    private void addPlot() {
        InputForm form = new InputForm("Add plot or pitch");
        form.add("Campsite ID");
        form.add("Camping area identifier");
        form.add("Plot identifier");
        form.add("Type (EMPTY_PITCH, BUNGALOW, APARTMENT, STATIC_MOBILE_HOME, OTHER)");
        form.add("Has power? (yes/no; static dwellings must have power)");
        form.add("Bedrooms (0 if none)");
        form.add("Beds (0 if none)");
        form.add("Size in square metres");
        form.add("Facilities (comma separated)");
        form.add("Cost per night (€)");
        form.add("Photo URL (optional)");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        boolean hasPower = parseYesNo(values.get(4));
        Plot plot = manager.addPlot(Integer.parseInt(values.get(0)), values.get(1),
                values.get(2), PlotType.fromText(values.get(3)), hasPower,
                Integer.parseInt(values.get(5)), Integer.parseInt(values.get(6)),
                Double.parseDouble(values.get(7)),
                CampsiteManager.parseFacilities(values.get(8)),
                Double.parseDouble(values.get(9)), values.get(10));
        refreshCampsites();
        showStatus("Added plot " + plot.identifier());
    }

    private void addGuest() {
        InputForm form = new InputForm("Add guest");
        form.add("Guest ID (positive number)");
        form.add("Name");
        form.add("Date of birth (YYYY-MM-DD)");
        form.add("Address");
        form.add("Telephone");
        form.add("Email");
        form.add("Additional notes");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        Guest guest = manager.addGuest(Integer.parseInt(values.get(0)), values.get(1),
                LocalDate.parse(values.get(2)), values.get(3), values.get(4),
                values.get(5), values.get(6));
        showStatus("Added guest " + guest);
    }

    private void addBooking() {
        InputForm form = bookingForm("Add booking");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        BookingRequest request = readRequest(values, 3);
        Booking booking = manager.addBooking(Integer.parseInt(values.get(0)),
                values.get(1), Integer.parseInt(values.get(2)), request);
        refreshCampsites();
        showStatus("Booking " + booking.id() + " assigned to plot " + booking.plotId());
    }





// cancel booking, view bookings, view guest, smart find
    private void cancelBooking() {
        InputForm form = new InputForm("Cancel booking");
        form.add("Booking ID");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        int bookingId = Integer.parseInt(values.get(0));
        if (!manager.cancelBooking(bookingId)) {
            throw new IllegalArgumentException("Booking was not found or is no longer pending.");
        }
        refreshCampsites();
        showStatus("Cancelled booking " + bookingId);
    }

    private void viewCampsiteBookings() {
        InputForm form = new InputForm("View campsite bookings");
        form.add("Campsite ID");
        CustomList<String> values = form.show();
        if (values != null) {
            output.setText(manager.pendingBookingsForCampsite(
                    Integer.parseInt(values.get(0))));
        }
    }

    private void viewGuest() {
        InputForm form = new InputForm("View guest record");
        form.add("Guest ID");
        CustomList<String> values = form.show();
        if (values != null) {
            output.setText(manager.guestRecord(Integer.parseInt(values.get(0))));
        }
    }

    private void smartFind() {
        InputForm form = new InputForm("Smart find a camping holiday");
        form.add("Guest ID");
        form.add("Start date (YYYY-MM-DD)");
        form.add("End date (YYYY-MM-DD)");
        form.add("Preferred type (blank for any)");
        form.add("Number of guests");
        form.add("Power (ANY, REQUIRED, NOT_REQUIRED)");
        form.add("Minimum plot size (m²)");
        form.add("Minimum bedrooms");
        form.add("Minimum beds");
        form.add("Maximum cost per night (€)");
        form.add("Minimum campsite stars (1-5)");
        form.add("Required campsite facilities (comma separated)");
        form.add("Required plot facilities (comma separated)");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        int guestId = Integer.parseInt(values.get(0));
        CustomList<String> campsiteFacilities =
                CampsiteManager.parseFacilities(values.get(11));
        CustomList<String> plotFacilities =
                CampsiteManager.parseFacilities(values.get(12));
        BookingRequest request = readRequest(values, 1, campsiteFacilities, plotFacilities);
        PlotMatch match = manager.findSmartPlot(request, Integer.parseInt(values.get(10)),
                campsiteFacilities, plotFacilities);
        if (match == null) {
            output.setText("No campsite and plot match all requested criteria.");
            showStatus("Smart find found no available match.");
            return;
        }

        String suggestion = "Suggested holiday:\n"
                + match.campsite() + "\nArea: " + match.area().identifier()
                + "\nPlot: " + match.plot() + "\nDates: "
                + request.startDate() + " to " + request.endDate()
                + "\nBook this suggestion for guest " + guestId + "?";
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, suggestion,
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Smart find result");
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Booking booking = manager.bookSmartChoice(match, guestId, request);
            refreshCampsites();
            output.setText(suggestion + "\n\nCreated booking " + booking.id() + ".");
            showStatus("Smart find created booking " + booking.id());
        } else {
            output.setText(suggestion + "\n\nNo booking was created.");
        }
    }

    private void removePlot() {
        InputForm form = new InputForm("Remove plot or pitch");
        form.add("Campsite ID");
        form.add("Camping area identifier");
        form.add("Plot identifier");
        CustomList<String> values = form.show();
        if (values == null) {
            return;
        }
        int campsiteId = Integer.parseInt(values.get(0));
        String areaId = values.get(1);
        String plotId = values.get(2);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove plot " + plotId + "? Pending bookings will be reassigned if possible.",
                ButtonType.YES, ButtonType.NO);
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        int cancelledCount = manager.removePlot(campsiteId, areaId, plotId);
        refreshCampsites();
        showStatus("Removed plot " + plotId + "; "
                + cancelledCount + " booking(s) could not be reassigned and were cancelled.");
    }

    private void saveData() {
        try {
            manager.save(DATA_FILE);
            showStatus("Saved all campsite data to " + DATA_FILE);
        } catch (IOException exception) {
            showError("Could not save campsite data: " + exception.getMessage());
        }
    }



    private void loadSampleData() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "This replaces the current campsite data and saves the sample data to disk. Continue?",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Load sample data?");
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        CampsiteManager sample = CampsiteManager.createSampleData();
        try {
            sample.save(DATA_FILE);
        } catch (IOException exception) {
            showError("Could not save sample campsite data: " + exception.getMessage());
            return;
        }
        manager = sample;
        refreshCampsites();
        LocalDate start = LocalDate.now().plusDays(30);
        output.setText("Sample data is ready. Try Smart Find with:\n"
                + "Guest ID: 5001\n"
                + "Start date: " + start + "\n"
                + "End date: " + start.plusDays(5) + "\n"
                + "Preferred type: BUNGALOW\n"
                + "Guests: 4; Power: ANY\n"
                + "Minimum size: 40; Bedrooms: 2; Beds: 4\n"
                + "Maximum cost per night: 200; Minimum stars: 4\n"
                + "Campsite facilities: Pool\n"
                + "Plot facilities: Kitchen, Wi-Fi\n\n"
                + "The suggested result should be Pine Ridge Resort, plot PINE-02.");
        showStatus("Sample data loaded and saved to " + DATA_FILE);
    }

    private void loadData() {
        try {
            manager = CampsiteManager.load(DATA_FILE);
            refreshCampsites();
            showStatus("Loaded all campsite data from " + DATA_FILE);
        } catch (IOException exception) {
            showError("Could not load campsite data: " + exception.getMessage());
        }
    }

    private void resetData() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "This removes all data from memory and deletes the saved data file if it exists.",
                ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText("Reset all system data?");
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        manager.reset();
        refreshCampsites();
        output.clear();
        try {
            java.nio.file.Files.deleteIfExists(DATA_FILE);
            showStatus("All system data and the saved data file have been cleared.");
        } catch (IOException exception) {
            showError("Memory was cleared, but the saved data file could not be deleted: "
                    + exception.getMessage());
        }
    }

    private InputForm bookingForm(String title) {
        InputForm form = new InputForm(title);
        form.add("Campsite ID");
        form.add("Camping area identifier");
        form.add("Guest ID");
        form.add("Start date (YYYY-MM-DD)");
        form.add("End date (YYYY-MM-DD)");
        form.add("Preferred type (blank for any)");
        form.add("Number of guests");
        form.add("Power (ANY, REQUIRED, NOT_REQUIRED)");
        form.add("Minimum plot size (m²)");
        form.add("Minimum bedrooms");
        form.add("Minimum beds");
        form.add("Maximum cost per night (€)");
        return form;
    }

    private BookingRequest readRequest(CustomList<String> values, int firstRequestField) {
        return readRequest(values, firstRequestField, new CustomList<>(), new CustomList<>());
    }

    private BookingRequest readRequest(CustomList<String> values, int firstRequestField,
            CustomList<String> campsiteFacilities, CustomList<String> plotFacilities) {
        String typeText = values.get(firstRequestField + 2).trim();
        PlotType type = typeText.isEmpty() ? null : PlotType.fromText(typeText);
        PowerPreference power = PowerPreference.valueOf(
                values.get(firstRequestField + 4).trim().toUpperCase(Locale.ROOT));
        return new BookingRequest(
                LocalDate.parse(values.get(firstRequestField)),
                LocalDate.parse(values.get(firstRequestField + 1)),
                type,
                Integer.parseInt(values.get(firstRequestField + 3)),
                power,
                Double.parseDouble(values.get(firstRequestField + 5)),
                Integer.parseInt(values.get(firstRequestField + 6)),
                Integer.parseInt(values.get(firstRequestField + 7)),
                Double.parseDouble(values.get(firstRequestField + 8)),
                campsiteFacilities, plotFacilities);
    }

    private void refreshCampsites() {
        campsiteList.getItems().clear();
        for (Campsite campsite : manager.campsites()) {
            campsiteList.getItems().add(campsite);
        }
        refreshAreas(campsiteList.getSelectionModel().getSelectedItem());
    }

    private void refreshAreas(Campsite campsite) {
        areaList.getItems().clear();
        plotList.getItems().clear();
        bookingList.getItems().clear();
        if (campsite != null) {
            for (CampingArea area : campsite.areas()) {
                areaList.getItems().add(area);
            }
            output.setText("Campsite " + campsite.id() + ": " + campsite.name()
                    + "\nAddress: " + campsite.address()
                    + "\nStars: " + campsite.stars()
                    + "\nContact: " + campsite.contact()
                    + "\nFacilities: " + join(campsite.facilities()));
        } else {
            output.clear();
        }
    }

    private void refreshPlots(CampingArea area) {
        plotList.getItems().clear();
        bookingList.getItems().clear();
        if (area != null) {
            for (Plot plot : area.plots()) {
                plotList.getItems().add(plot);
            }
            output.setText("Camping area: " + area.identifier()
                    + "\nDescription: " + area.description());
        }
    }

    private void showPlotDetails(Plot plot) {
        if (plot == null) {
            return;
        }
        bookingList.getItems().clear();
        for (Booking booking : plot.bookings()) {
            bookingList.getItems().add(booking);
        }
        output.setText("Plot: " + plot.identifier()
                + "\nType: " + plot.type().displayName()
                + "\nPower: " + (plot.hasPower() ? "Yes" : "No")
                + "\nBedrooms: " + plot.bedrooms()
                + "\nBeds: " + plot.beds()
                + "\nSize: " + plot.size() + " m²"
                + "\nCost: €" + plot.costPerNight() + " per night"
                + "\nFacilities: " + join(plot.facilities())
                + "\nPhoto: " + plot.photoUrl()
                + "\nBookings stored on this plot: " + plot.bookings().size());
    }

    private void showBookingDetails(Booking booking) {
        if (booking == null) {
            return;
        }
        Guest guest = manager.findGuest(booking.guestId());
        output.setText("Booking: " + booking.id()
                + "\nStatus: " + booking.status()
                + "\nGuest: " + (guest == null ? booking.guestId() : guest.name())
                + "\nCampsite ID: " + booking.campsiteId()
                + "\nCamping area: " + booking.areaId()
                + "\nPlot: " + booking.plotId()
                + "\nDates: " + booking.startDate() + " to " + booking.endDate()
                + "\nGuests: " + booking.request().guestCount()
                + "\nNightly budget: €" + booking.request().budgetPerNight());
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException exception) {
            showError(exception.getMessage());
        }
    }

    private boolean parseYesNo(String answer) {
        if (answer.equalsIgnoreCase("yes")) {
            return true;
        }
        if (answer.equalsIgnoreCase("no")) {
            return false;
        }
        throw new IllegalArgumentException("Enter yes or no for the power field.");
    }

    private String join(CustomList<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(value);
        }
        return result.length() == 0 ? "None" : result.toString();
    }

    private void showStatus(String message) {
        status.setText(message);
        status.setStyle("-fx-text-fill: #176b35;");
    }

    private void showError(String message) {
        String error = message == null || message.isBlank() ? "Please check the entered data." : message;
        status.setText(error);
        status.setStyle("-fx-text-fill: #a32121;");
        Alert alert = new Alert(Alert.AlertType.ERROR, error, ButtonType.OK);
        alert.setHeaderText("Action could not be completed");
        alert.showAndWait();
    }

    private static final class InputForm {
        private final Dialog<CustomList<String>> dialog = new Dialog<>();
        private final GridPane fields = new GridPane();
        private final CustomList<TextField> inputs = new CustomList<>();
        private int row;

        private InputForm(String title) {
            dialog.setTitle(title);
            dialog.setHeaderText(title);
            fields.setHgap(10);
            fields.setVgap(8);
            fields.setPadding(new Insets(10));
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            ScrollPane scrollPane = new ScrollPane(fields);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefViewportHeight(480);
            dialog.getDialogPane().setContent(scrollPane);
            dialog.setResultConverter(button -> button == ButtonType.OK ? readFields() : null);
        }

        private void add(String label) {
            TextField input = new TextField();
            input.setPrefColumnCount(28);
            fields.add(new Label(label), 0, row);
            fields.add(input, 1, row);
            inputs.add(input);
            row++;
        }

        private CustomList<String> show() {
            return dialog.showAndWait().orElse(null);
        }

        private CustomList<String> readFields() {
            CustomList<String> values = new CustomList<>();
            for (TextField input : inputs) {
                values.add(input.getText().trim());
            }
            return values;
        }
    }
}
