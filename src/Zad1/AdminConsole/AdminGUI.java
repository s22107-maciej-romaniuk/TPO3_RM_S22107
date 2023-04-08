package Zad1.AdminConsole;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.util.List;

public class AdminGUI extends Application {
    private AdminClient connection;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GUI.fxml"));
        Parent root = loader.load();
        ((AdminGUI) loader.getController()).connection = new AdminClient(Integer.parseInt(getParameters().getUnnamed().get(0))); // trzeba tak bo FXML utworzy osobną instancję tej klasy
        primaryStage.setTitle("Admin console");
        primaryStage.setScene(new Scene(root, 600, 500));
        primaryStage.show();
    }

    @Override
    public void stop() {

    }

    public ListView<String> topicListView;
    public TextField topicToAddField;
    public TextField selectedTopicField;
    public TextArea messageTextArea;
    public Button addTopicButton;
    public Button removeTopicButton;
    public Button sendMessageButton;

    public void handleTopicListClick(){
        this.selectedTopicField.setText(this.topicListView.getSelectionModel().getSelectedItem());
    }

    public void handleAddTopicButton(){
        if(this.topicToAddField.getText() != null &&!this.topicToAddField.getText().isEmpty()) {
            List<String> topicList = this.topicListView.getItems();
            String topicToAdd = this.topicToAddField.getText().toUpperCase();
            if(!topicList.contains(topicToAdd)) {
                topicList.add(topicToAdd);
                connection.sendTopicList(topicList);
            }
        }
    }

    public void handleRemoveTopicButton(){
        if(this.selectedTopicField.getText() != null &&!this.selectedTopicField.getText().isEmpty()) {
            List<String> topicList = this.topicListView.getItems();
            if(topicList.remove(this.selectedTopicField.getText())) {
                connection.sendTopicList(topicList);
            }
        }
    }

    public void handleSendMessageButton(){
        connection.sendMessage(this.selectedTopicField.getText(), this.messageTextArea.getText());
    }
}
