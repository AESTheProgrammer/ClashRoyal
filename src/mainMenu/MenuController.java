package mainMenu;

import game.GameController;
import gameUtil.Card;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import login.LoginController;
import player.Bot;
import player.Client;
import player.Status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;

public class MenuController {
    private MenuModel menuModel;
    private HashMap<Button, Card> linkedButtons;

    @FXML private TextArea historyTextArea;
    @FXML private Button twoOnTwoButton;
    @FXML private ProgressBar xpBar;
    @FXML private Button battleButton;
    @FXML private Button normalBotButton;
    @FXML private Button smartBotButton;
    @FXML private TabPane mainMenuTabPane;
    @FXML private Button button1;
    @FXML private Button button2;
    @FXML private Button button3;
    @FXML private Button button4;
    @FXML private Button button5;
    @FXML private Button button6;
    @FXML private Button button7;
    @FXML private Button button8;
    @FXML private Button button9;
    @FXML private Button button10;
    @FXML private Button button11;
    @FXML private Button button12;
    @FXML private Button logoutButton;
    @FXML private Label usernameLabel;
    @FXML private Label trophyLabel;
    @FXML private Label levelLabel;


    /**
     * this is a constructor
     */
    public MenuController() {

    }

    /**
     * this method initialize the model and controller and view of the gui for the user
     */
    public void initialize() {
    }

    /**
     * this method initialize the menu controller
     * @param client is the player object which holds clients attributes
     * @throws IOException is the exception
     */
    public void initialize(Client client) throws IOException {
        //Media media = new Media(new File("login.wav").toURI().toString());
        //MediaPlayer player = new MediaPlayer(media);
        //player.play();
        String history = "";
        for (String string : client.getStatus().getHistory())
            history += string + "\n";
        historyTextArea.setText(history + "\n");
        historyTextArea.setStyle("-fx-font-size: 1.8em;");
        historyTextArea.setDisable(true);
        Status clientStatus = client.getStatus();
        ArrayList<Button> buttonsList = new ArrayList<>();
        Collections.addAll(buttonsList, button1, button2, button3, button4, button5, button6, button7, button8, button9, button10, button11, button12);
        linkedButtons = new HashMap<>();
        menuModel = new MenuModel();
        menuModel.setStatus(clientStatus);
        usernameLabel.setText(menuModel.getStatus().getUsername());
        trophyLabel.setText(String.valueOf(menuModel.getStatus().getTrophy()));
        levelLabel.setText(String.valueOf(menuModel.getStatus().getLevel()));
        xpBar.setProgress(menuModel.getStatus().getXP());

        ArrayList<Card> cardsNotInUse = (ArrayList<Card>) menuModel.getStatus().getCards().clone();
        cardsNotInUse.removeAll(menuModel.getStatus().getCardsDeskInUse());
        int i = 0;
        for (var card : menuModel.getStatus().getCardsDeskInUse())
            linkedButtons.put(buttonsList.get(i++), card);
        for (var card : cardsNotInUse)
            linkedButtons.put(buttonsList.get(i++), card);
        for (var button : linkedButtons.keySet()) {
            setButtonImage(linkedButtons.get(button).getCardAddress(), button);
        }

        EventHandler<ActionEvent> logoutButtonAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    Stage stage = (Stage) button1.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader();
                    Pane root = (Pane) loader.load(getClass().getResource("/login/login.fxml").openStream());
                    LoginController loginController = loader.getController();
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.setTitle("Clash Royal");
                    stage.setResizable(false);
                    stage.show();
                } catch (IOException ex) {ex.printStackTrace();}
            }
        };
        logoutButton.setOnAction(logoutButtonAction);

        EventHandler<ActionEvent> botButtonOnClick = new EventHandler<>() {
            public void handle(ActionEvent e) {
                try {
                    client.connect();
                    menuModel.getStatus().getCardsDeskInUse().clear();
                    linkedButtons.remove(button9);
                    linkedButtons.remove(button10);
                    linkedButtons.remove(button11);
                    linkedButtons.remove(button12);
                    menuModel.getStatus().getCardsDeskInUse().addAll(linkedButtons.values());
                    Stage playerStage = (Stage) button1.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader();
                    Pane root = loader.load(getClass().getResource("/game/game.fxml").openStream());
                    GameController gameController = loader.getController();
                    if (e.getTarget().equals(normalBotButton))
                        gameController.initialize(client, new Bot(new Status("EasyBot"), false));
                    else if (e.getTarget().equals(smartBotButton))
                        gameController.initialize(client, new Bot(new Status("SmartBot"), true));
                    Scene scene = new Scene(root);
                    playerStage.setScene(scene);
                    playerStage.setTitle("Clash Royal");
                    root.setOnMouseClicked(gameController);
                    playerStage.setResizable(false);
                    playerStage.show();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        };
        normalBotButton.setOnAction(botButtonOnClick);
        smartBotButton.setOnAction(botButtonOnClick);

        EventHandler<ActionEvent> battleButtonEvent = new EventHandler<>() {
            String s;
            public void handle(ActionEvent e) {
                try {
                    Thread thread = new Thread(() -> {
                        while (!(s = client.getLastRespond()).contains("<READY>")) {
                            System.out.println(s);
                            try {Thread.sleep(100);
                            } catch (InterruptedException interruptedException) {interruptedException.printStackTrace();}
                        }
                    });
                    thread.start();
                    client.sendCommand("FIND_COMPONENT_1");
                    thread.join();
                    client.connect();
                    menuModel.getStatus().getCardsDeskInUse().clear();
                    linkedButtons.remove(button9);
                    linkedButtons.remove(button10);
                    linkedButtons.remove(button11);
                    linkedButtons.remove(button12);
                    menuModel.getStatus().getCardsDeskInUse().addAll(linkedButtons.values());
                    Stage playerStage = (Stage) button1.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader();
                    Pane root = loader.load(getClass().getResource("/game/game.fxml").openStream());
                    GameController gameController = loader.getController();
                    Status status = (Status)fromString(s.split(" ", 2)[1]);
                    Client componentClient = new Client();
                    componentClient.setStatus(status);
                    gameController.initialize(client, componentClient);
                    Scene scene = new Scene(root);
                    playerStage.setScene(scene);
                    playerStage.setTitle("Clash Royal");
                    root.setOnMouseClicked(gameController);
                    playerStage.setResizable(false);
                    playerStage.show();
                } catch (IOException | InterruptedException | ClassNotFoundException ioException) {ioException.printStackTrace();}
            }
        };
        battleButton.setOnAction(battleButtonEvent);

        EventHandler<ActionEvent> twoOnTwoButtonEvent = new EventHandler<>() {
            String s;
            public void handle(ActionEvent e) {
                try {
                    String[] stateStrings = new String[3];
                    Thread thread = new Thread(() -> {
                        int i = 0;
                        while (i < 3) {
                            while (!(s = client.getLastRespond()).contains("<READY1>")) {
                                try { Thread.sleep(50);
                                } catch (InterruptedException interruptedException) {interruptedException.printStackTrace();}
                            }
                            System.out.println(s);
                            stateStrings[i] = s;
                            i++;
                            while (!(s = client.getLastRespond()).contains("<READY2>")) {
                                try { Thread.sleep(50);
                                } catch (InterruptedException interruptedException) {interruptedException.printStackTrace();}
                            }
                            System.out.println(s);
                            stateStrings[i] = s;
                            i++;
                            while (!(s = client.getLastRespond()).contains("<READY3>")) {
                                try { Thread.sleep(50);
                                } catch (InterruptedException interruptedException) {interruptedException.printStackTrace();}
                            }
                            System.out.println(s);
                            stateStrings[i] = s;
                            i++;
                        }
                    });
                    thread.start();
                    client.sendCommand("FIND_COMPONENT_2");
                    thread.join();
                    client.connect();
                    menuModel.getStatus().getCardsDeskInUse().clear();
                    linkedButtons.remove(button9);
                    linkedButtons.remove(button10);
                    linkedButtons.remove(button11);
                    linkedButtons.remove(button12);
                    menuModel.getStatus().getCardsDeskInUse().addAll(linkedButtons.values());
                    Stage playerStage = (Stage) button1.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader();
                    Pane root = loader.load(getClass().getResource("/game/game.fxml").openStream());
                    GameController gameController = loader.getController();
                    Status[] statusList = new Status[3];
                    for (int i = 0; i < 3; i++) {
                        statusList[i] = (Status)fromString(stateStrings[i].split(" ")[1]);
                    }
                    Client teamMate = new Client();
                    teamMate.setStatus(statusList[0]);
                    Client component1 = new Client();
                    component1.setStatus(statusList[1]);
                    Client component2 = new Client();
                    component2.setStatus(statusList[2]);

                    gameController.initialize(client, teamMate, component1, component2);
                    Scene scene = new Scene(root);
                    playerStage.setScene(scene);
                    playerStage.setTitle("Clash Royal");
                    root.setOnMouseClicked(gameController);
                    playerStage.setResizable(false);
                    playerStage.show();
                } catch (IOException | InterruptedException | ClassNotFoundException ioException) {ioException.printStackTrace();}
            }
        };
        twoOnTwoButton.setOnAction(twoOnTwoButtonEvent);

        mainMenuTabPane.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
                menuModel.resetStacked();
            }
        });

        EventHandler<ActionEvent> cardButtonsAction = new EventHandler<>() {
            public void handle(ActionEvent e) {
                if (menuModel.getStackedCard() == null) {
                    menuModel.setStackedCard(linkedButtons.get((Button) e.getTarget()));
                    menuModel.setStackedButton((Button)e.getTarget());
                } else {
                    Button clicked = (Button)e.getTarget();
                    Card card = linkedButtons.get((Button)e.getTarget());
                    linkedButtons.put(menuModel.getStackedButton(), card);
                    linkedButtons.put(clicked, menuModel.getStackedCard());
                    setButtonImage(menuModel.getStackedCard().getCardAddress(), clicked);
                    setButtonImage(card.getCardAddress(), menuModel.getStackedButton());
                    menuModel.resetStacked();
                }
            }
        };
        buttonsList.forEach(btn -> btn.setOnAction(cardButtonsAction));
    }

    /**
     * this method set an image for a button
     * @param imageAddress is the address of the image which will be put on the button
     * @param button is the button which will be covered  with that image
     */
    private void setButtonImage(String imageAddress, Button button) {
        Image image = new Image(imageAddress);
        ImageView view = new ImageView(image);
        view.setFitHeight(140);
        view.setFitWidth(140);
        button.setGraphic(view);
    }

    /** Read the object from a Base64 string. */
    private Object fromString( String s ) throws IOException ,
            ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(  data ) );
        Object o  = ois.readObject();
        ois.close();
        return o;
    }
}
