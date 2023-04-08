package Zad1.Client;

import Zad1.AdminConsole.AdminClient;
import Zad1.AdminConsole.AdminGUI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class SubscriberGUI extends Application {
    SubscriberClient connection;
    SubscriberGUI controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GUI.fxml"));
        Parent root = loader.load();

        //podpinanie modelu
        this.controller = loader.getController();
        this.controller.connection = new SubscriberClient(
                Integer.parseInt(getParameters().getUnnamed().get(0)),
                this.controller.getUpdater()); // trzeba tak bo FXML utworzy osobną instancję tej klasy
        primaryStage.setTitle("Admin console");
        primaryStage.setScene(new Scene(root, 600, 500));
        primaryStage.show();
    }

    @Override
    public void stop(){
        this.controller.connection.stopListening();
    }

    public IUpdater getUpdater(){
        return (topics, messages) -> Platform.runLater(() -> {
            //update available topics
            if(topics != null) {
                this.availableTopicsListView.getItems().removeAll(this.availableTopicsListView.getItems());
                this.availableTopicsListView.getItems().addAll(topics);
                //unsubscribe from topics which are not available anymore
                List<String> copyOfSubscribedTopics = new ArrayList<>(this.subscribedTopicsListView.getItems());
                copyOfSubscribedTopics.removeAll(topics);
                this.subscribedTopicsListView.getItems().removeAll(copyOfSubscribedTopics);
            }
            //append messages
            if(messages != null) {
                this.messagesTextArea.setText(this.messagesTextArea.getText() + "\n" + String.join(" ", messages));
            }
        });
    }

    public ListView<String> availableTopicsListView;
    public ListView<String> subscribedTopicsListView;
    public TextArea messagesTextArea;

    private String selectedAvailableTopic;
    private String selectedSubscribedTopic;
    public void handleAvailableTopicListClick(){
        this.selectedAvailableTopic = this.availableTopicsListView.getSelectionModel().getSelectedItem();
    }
    public void handleSubscribedTopicListClick(){
        this.selectedSubscribedTopic = this.subscribedTopicsListView.getSelectionModel().getSelectedItem();
    }
    public void handleSubscribeButton(){
        if(this.selectedAvailableTopic != null && !this.subscribedTopicsListView.getItems().contains(this.selectedAvailableTopic)) {
            this.subscribedTopicsListView.getItems().add(this.selectedAvailableTopic);
            //wyślij wiadomość do serwera
            this.connection.subscribe(this.selectedAvailableTopic);
            this.selectedAvailableTopic = null;
        }
    }
    public void handleUnsubscribeButton(){
        if(this.selectedSubscribedTopic != null && this.subscribedTopicsListView.getItems().contains(this.selectedAvailableTopic)) {
            this.subscribedTopicsListView.getItems().remove(this.selectedSubscribedTopic);
            //wyślij wiadomość do serwera
            this.connection.unsubscribe(this.selectedSubscribedTopic);
            this.selectedSubscribedTopic = null;
        }
    }
}
