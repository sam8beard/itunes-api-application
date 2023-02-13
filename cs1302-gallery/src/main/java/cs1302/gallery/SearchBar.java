package cs1302.gallery;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
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
import javafx.scene.control.ComboBox;


/**
 * Represents a search bar.
 */
public class SearchBar extends HBox {

    TextField urlField;
    Button playPause;
    Button getImages;
    ComboBox dropDown;
    Label urlLabel;
    String[] searchChoices = new String[] {
        "movie",
        "podcast",
        "music",
        "musicVideo",
        "audioBook",
        "shortFilm",
        "tvShow",
        "software",
        "ebook",
        "all"
    };

    /**
     * Constructs a {@code SearchBar}.
     */
    public SearchBar() {

        super(8);
        this.searchChoices = searchChoices;
        this.urlLabel = new Label("Search: ");
        this.urlField = new TextField();
        this.playPause = new Button("Play");
        this.playPause.setPrefWidth(50);
        this.playPause.setPrefHeight(10);
        this.playPause.setFont(new Font(10.5));
        this.getImages = new Button("Get Images");
        this.dropDown = new ComboBox<String>();
        this.setComboBox();

        this.setSpacing(5);

        this.getChildren().addAll(this.playPause, this.urlLabel, this.urlField, this.dropDown,
            this.getImages);



    } // SearchBar



    /**
     * Builds the {@code ComboBox} for this search bar.
     */
    public void setComboBox() {

        this.dropDown.getItems().addAll(searchChoices);
        this.dropDown.getSelectionModel().select(2);

    } // setComboBox


} // SearchBar
