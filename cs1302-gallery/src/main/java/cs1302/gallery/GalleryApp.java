package cs1302.gallery;

import java.net.http.HttpClient;

import cs1302.gallery.SearchBar;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.TilePane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.text.*;
import javafx.stage.StageStyle;
import javafx.scene.control.ProgressBar;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.lang.Thread;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import cs1302.gallery.ItunesResponse;
import javafx.event.Event;
import java.util.ArrayList;
import java.util.Timer;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.lang.*;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.scene.text.Font;
import java.lang.Math;
import javafx.util.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/*


CHECKLIST:

- make progress bar update accordingly
   + adjust labels accordingly

0;136;0c- make randomization function
   + pause and play button event handler

-


 */



/**
 * Represents an iTunes Gallery App.
 */
public class GalleryApp extends Application {

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    // the itunes api url
    private static final String ITUNES_API = "https://itunes.apple.com/search";

    private Stage stage;
    private Scene scene;



    private VBox root;
    private TilePane mainDisplay;
    private SearchBar searchBar;

    private Image defaultImg;
    private HBox messageBar;
    private Text messageContents;

    private HBox progressBox;
    private Text claimText;
    private ProgressBar progressBar;

    private ImageView[] imageViews = new ImageView[20];
    private ArrayList<String> imageUrls = new ArrayList<String>();
    ArrayList<String> tempList = new ArrayList<String>();

    private EventHandler<ActionEvent> playPauseHandler;
    private EventHandler<ActionEvent> getImagesHandler;
    private String uri;
    private int tempCount = 1;
    private boolean isPlaying;

    private Timeline timeline = new Timeline();
    private KeyFrame keyFrame;
    private boolean isDefault = true;

    protected ItunesResponse itunesResponse;
    protected ItunesResult result;

    /**
     * Represents the event handler for the randomization function of the {@code playPause} button.
     *
     */
    public void randomizeFunction() {

        EventHandler<ActionEvent> handler = (e -> {


            int randomNum = (int) (Math.random() * (this.tempList.size()));

            int randomNum2 = (int) (Math.random() * (this.imageViews.length));

            String url = this.tempList.get(randomNum);


            if (url != null) {

                this.imageViews[randomNum2].setImage(new Image(url));
                this.imageViews[randomNum2].setFitHeight(100.0);
                this.imageViews[randomNum2].setFitWidth(100.0);

            }

            if (this.imageUrls.contains(this.tempList.get(randomNum))) {

                this.tempList.remove(this.tempList.get(randomNum));

            } // if


        });

        this.keyFrame = new KeyFrame(Duration.seconds(2), handler);
        this.timeline = new Timeline();
        this.timeline.setCycleCount(Timeline.INDEFINITE);
        this.timeline.getKeyFrames().add(this.keyFrame);
        this.timeline.play();

    } // randomizeFunction


    /**
     * Creates and immediately starts a new daemon thread that executes
     * {@code target.run()}. This method will return immediately to its caller.
     * From the Brief Introduction to Java Threads tutorial.
     *
     * @param target the object whose {@code run} method is invoked when this thread starts.
     *
     */
    public void runNow(Runnable target) {

        Thread t = new Thread(target);
        t.setDaemon(true);
        t.start();

    } // runNow

    /**
     * Builds the main display.
     *
     */
    public void buildDisplay() {

        // if less than 21 urls are found
        if (this.imageUrls.size() < 21) {

            this.updatePBar(1.0);

            Runnable task = () -> {

                Exception iae = new IllegalArgumentException();
                Alert alert = new Alert(AlertType.ERROR);

                if (this.tempCount == 1) {


                    this.searchBar.playPause.setDisable(true);

                }

                this.messageContents.setText("Last attempt to get images failed...");

                alert.setContentText("URI: " + uri + "\n" +
                    "Exception: " + iae.toString() + ": " + this.imageUrls.size()
                    + " distinct results found," + " but 21 or more are needed.");

//                this.searchBar.playPause.setDisable(true);
                alert.showAndWait();




                this.updatePBar(1.0);


            };

            Platform.runLater(task);


            return;

        } else {

            int count = 0;

            while (count < 20) {

                String url = this.imageUrls.get(count);

                this.imageViews[count].setImage(new Image(url));
                this.imageViews[count].setFitWidth(100);
                this.imageViews[count].setFitHeight(100);

                count++;

            } // while

        } // if


    } // buildDisplay


    /**
     * Requests, recieves, parses the JSON response body, and stores the URLs
     * in an {@code ArrayList<String>} object.
     *
     */
    public void requestJSON() {

        try {
            this.updatePBar(0.0);
            this.updatePBar(0.0);
            this.imageUrls.clear();
            tempList.clear();
            String term = URLEncoder.encode(this.searchBar.urlField.getText(),
                StandardCharsets.UTF_8);
            String media = URLEncoder.encode(this.searchBar.dropDown.getSelectionModel().
                getSelectedItem().toString(), StandardCharsets.UTF_8);
            String limit = URLEncoder.encode("200", StandardCharsets.UTF_8);
            String query = String.format("?term=%s&media=%s&limit=%s", term, media, limit);
            this.uri = ITUNES_API + query;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();
            // send request / receive response in the form of a String
            HttpResponse<String> response = HTTP_CLIENT.<String>
                send(request, BodyHandlers.ofString());
            // ensure the request is okay
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            } // if
            String body = response.body().trim();
            // parse the JSON-formatted string using GSON
            this.itunesResponse = GSON
                .fromJson(body, ItunesResponse.class);

            // add image urls to ArrayList for storage
            for (int i = 0; i < itunesResponse.results.length; i++) {

                this.updatePBar(1.0 * i / itunesResponse.results.length);
                Thread.sleep(40);
                this.result = itunesResponse.results[i];
                this.tempList.add(result.artworkUrl100);

            } // for

            // checks for identical urls
            for (int i = 0; i < tempList.size(); i++) {

                if (!this.imageUrls.contains(this.tempList.get(i))) {
                    this.imageUrls.add(tempList.get(i));
                } // if
                this.updatePBar(1.0);
            } // for


        } catch (IOException | InterruptedException e) {

            System.err.println(e);
            e.printStackTrace();
        } // try



    } // requestResponse



    /**
     * Constructs a {@code GalleryApp} object}.
     */
    public GalleryApp() {

        this.stage = null;
        this.scene = null;
        this.root = new VBox();

        this.defaultImg = new Image("file:resources/default.png");
        this.searchBar = new SearchBar();
        this.messageBar = new HBox();
        this.messageContents = new Text("Type in a term, select a media type, "
        + "then click the button.");
        this.messageContents.setTextAlignment(TextAlignment.LEFT);
        this.messageBar.getChildren().addAll(messageContents);
        this.mainDisplay = new TilePane();

        int count = 0;

        while (count < 20) {

            this.imageViews[count] = new ImageView(this.defaultImg);
            this.mainDisplay.getChildren().add(imageViews[count]);
            count++;

        } // while

        // making bottom container with progress bar
        this.progressBox = new HBox();
        this.claimText = new Text("Images provided by iTunes Search API.");
        this.progressBar = new ProgressBar(0.0);
        this.progressBar.setPrefWidth(250);

        HBox.setHgrow(progressBar, Priority.ALWAYS);

        // adds all children to progress box
        this.progressBox.getChildren().addAll(progressBar, claimText);

        // adds all children to root container
        this.root.getChildren().addAll(searchBar, messageBar, mainDisplay, progressBox);

        this.searchBar.playPause.setDisable(true);

    } // GalleryApp


    /**
     * Updates the progress bar through use of a lambda expression.
     *
     * @param amount the amount to increment
     */
    public void updatePBar(final double amount) {


        Platform.runLater(() -> this.progressBar.setProgress(amount));

    } // updatePBar

    /**
     * Represents the functionality of the {@code getImages} button.
     *
     */
    public void getImagesButton() {

        try {


            if (this.searchBar.playPause.getText().equals("Play")) {

                this.tempCount++;

            } // if


            this.searchBar.playPause.setDisable(true);
            this.searchBar.getImages.setDisable(true);
            this.timeline.pause();
            this.requestJSON();
            //this.searchBar.playPause.setText("Play");
            this.messageContents.setText("Getting images...");
            this.searchBar.playPause.setDisable(false);
            this.searchBar.getImages.setDisable(false);
            this.messageContents.setText(this.uri);
            this.messageContents.setFont(new Font(12));

            Runnable task = () -> {

                //this.searchBar.playPause.setText("Play");
                this.buildDisplay();

            };


            this.searchBar.playPause.setText("Play");
            Platform.runLater(task);

            this.tempCount++;
            this.timeline.pause();



        } catch (IllegalArgumentException iae) {

            System.out.println(iae.toString());

        } // try

    } // getImagesButton


    /**
     * Represents the functionality of the {@code playPause} button.
     *
     */
    public void playPauseFunction() {

        this.tempCount++;

        if (this.tempCount % 2 == 0.0) {

            this.isPlaying = true;

        } // if

        if (this.tempCount % 2 != 0.0) {

            this.isPlaying = false;


        } // if

        Platform.runLater(() -> {

            if (this.isPlaying) {

                this.searchBar.playPause.setText("Pause");

                this.timeline.play();

            } // if

            if (!this.isPlaying) {

                this.searchBar.playPause.setText("Play");

                this.timeline.pause();

                return;

            } // if


        });

        if (this.isPlaying) {

            randomizeFunction();

        } // if


    } // randomChange


    /** {@inheritDoc} */
    @Override
    public void init() {


        System.out.println("init() called");


        // event handler for the Get Images button
        this.getImagesHandler = e -> {


            Thread t = new Thread(() -> getImagesButton());

            this.searchBar.playPause.setText("Play");
            this.tempCount++;
            t.setDaemon(true);
            t.start();

        };

        this.searchBar.getImages.setOnAction(this.getImagesHandler);

        // event handler for the Play/Pause button
        this.searchBar.playPause.setOnAction(e -> this.playPauseFunction());


    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.stage.initStyle(StageStyle.DECORATED);
        this.scene = new Scene(this.root);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("GalleryApp!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start


    /** {@inheritDoc} */
    @Override
    public void stop() {
        // feel free to modify this method
        System.out.println("stop() called");
    } // stop

} // GalleryApp
