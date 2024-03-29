package game;

import gameUtil.ShapeHolder;
import gameUtil.AliveTroop;
import gameUtil.BuildingName;
import gameUtil.Spell;
import gameUtil.Troop;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import mainMenu.MenuController;
import player.Bot;
import player.Client;
import player.Player;
import player.Status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameController implements EventHandler<MouseEvent> {
    private TimerTask timerTask;
    private GameModel gameModel;
    private Player player2;
    private Client client1;
    private Client client2 = null;
    private Client client3 = null;
    private Client client4 = null;
    private ArrayList<Button> listedButtons;
    private Timer timer;
    private final int FRAMES_PER_SECOND = 10;
    public static long frameTimeInMilliseconds;
    private GameView gameView;
    @FXML private Button card4Button;
    @FXML private Button card3Button;
    @FXML private Button card2Button;
    @FXML private Button card1Button;
    @FXML private AnchorPane firstLayer;
    @FXML private AnchorPane secondLayerPane;
    @FXML private Label timerLabel;
    @FXML private Label blueCrownNumber;
    @FXML private Label redCrownNumber;
    @FXML private Label componentUsernameLabel;
    @FXML private ProgressBar elixirBar;
    @FXML private ImageView nextCardImage;
    @FXML private ImageView elixirImage;
    @FXML private Label elixirCount;

    /**
     * this is a constructor
     */
    public GameController() {
    }

    /**
     * this method initialize the gui
     */
    public void initialize() {
    }

    /**
     * this method initialize game
     * @param player1 is the player which is in direct contact with the game
     * @param player2 is the second player which might be a bot or a real player
     */
    public void initialize(Client player1, Player player2) {
        player1.getStatus().resetLists();
        player2.getStatus().resetLists();
        client1 = player1;
        this.player2 = player2;
        var status1 = player1.getStatus();
        var status2 = player2.getStatus();
        Thread timerThread = new Thread(() -> {
            int time = 180;
            int counter1 = 0;
            int counter2 = 0;
            while (time > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (counter1 == (time > 60 ? 2 : 1)) {
                    status1.increaseElixirs();
                    Platform.runLater(() -> elixirCount.setText(String.valueOf(status1.getElixirs())));
                    counter1 = 0;
                }
                if (counter2 == (time > 60 ? 2 : 1)) {
                    status2.increaseElixirs();
                    counter2 = 0;
                }
                if (status1.getElixirs() < 10)
                    counter1++;
                if (status2.getElixirs() < 10)
                    counter2++;
                time--;
                int finalTime = time;
                Platform.runLater(() -> timerLabel.setText(finalTime /60 + ":" + String.format("%02d", finalTime %60)));
                if (time < 60) {
                    Platform.runLater(() -> timerLabel.setStyle("-fx-text-fill: #ff0000"));
                    Platform.runLater(() -> timerLabel.setStyle("-fx-font-weight: bold"));
                    Platform.runLater(() -> timerLabel.setStyle("-fx-font-size: 22"));
                }
            }
            Platform.runLater(() -> indicateTheWinner(player2));
        });
        Thread elixirThread = new Thread(() -> {
            while (true) {
                try {
                        Platform.runLater(() -> {
                        elixirImage.setAccessibleText(String.valueOf(status1.getElixirs()));
                        elixirBar.setProgress(status1.getElixirs()/10.0);
                    });
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread kingWatcher = new Thread(() -> {
            while (client1.isConnected()) {
                try {Thread.sleep(400);
                } catch (InterruptedException e) {e.printStackTrace();}
                if (!isKingAlive(client1)) {
                    Platform.runLater(() -> indicateTheWinner(player2));
                    break;
                }
                if (!isKingAlive(player2)) {
                    Platform.runLater(() -> indicateTheWinner(player2));
                    break;
                }
            }
        });
        Thread crownCounter = new Thread(() -> {
            while (client1.isConnected()) {
                Platform.runLater(() -> {
                    blueCrownNumber.setText(String.valueOf(gameModel.getAllyCrown()));
                    redCrownNumber.setText(String.valueOf(gameModel.getEnemyCrown()));
                });
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {e.printStackTrace();}
            }
        });
        crownCounter.start();
        kingWatcher.start();
        timerThread.start();
        elixirThread.start();
        if (player2 instanceof Bot) {
            ((Bot) player2).connectToGame();
            if (((Bot)player2).isSmart())
                ((Bot)player2).playSmart();
            else
                ((Bot)player2).playNormally();
        }
        nextCardImage.setFitHeight(60);
        nextCardImage.setFitWidth(45);
        componentUsernameLabel.setText(status2.getUsername());
        secondLayerPane = new AnchorPane();
        secondLayerPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0)");
        gameModel = new GameModel();
        gameModel.initialize(status1, player2);
        gameView = new GameView();
        gameView.initialize(gameModel);
        secondLayerPane.getChildren().add(gameView);
        listedButtons = new ArrayList<>();
        listedButtons.add(card1Button);
        listedButtons.add(card2Button);
        listedButtons.add(card3Button);
        listedButtons.add(card4Button);
        for (int i = 0; i < 4; i++)
            setButtonImage(listedButtons.get(i), gameModel.getPlayersStatus()[0].getCardsDeskInUse().get(4 + i).getCardAddress());
        firstLayer.getChildren().add(gameView);
        startTimer();

        EventHandler<ActionEvent> cardButtonAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Platform.runLater(() -> {
                    gameModel.setStack((Button) actionEvent.getTarget(),
                            gameModel.getPlayersStatus()[0].getCardsDeskInUse().get(4 + listedButtons.indexOf((Button) actionEvent.getTarget())));
                    if (!firstLayer.getChildren().contains(ShapeHolder.getTopRectangle()))
                        firstLayer.getChildren().add(ShapeHolder.getTopRectangle());
                    status2.getAliveAllyTroops().forEach(troop -> {
                        if (troop.getCard().getName().equals(BuildingName.ARCHER_TOWER)) {
                            if (!firstLayer.getChildren().contains(ShapeHolder.getRightRectangle()) &&
                                    troop.getLocation().getX() > 172)
                                firstLayer.getChildren().add(ShapeHolder.getRightRectangle());
                            else if (!firstLayer.getChildren().contains(ShapeHolder.getLeftRectangle()) &&
                                    troop.getLocation().getX() < 172)
                                firstLayer.getChildren().add(ShapeHolder.getLeftRectangle());
                        }
                    });
                });
            }
        };
        listedButtons.forEach(button -> button.setOnAction(cardButtonAction));
    }

    /**
     * this method initialize game
     * @param player1 is the player which is in direct contact with the game
     * @param clients is the list of all players teammate and components
     */
    public void initialize(Client player1, Client...clients) {
        this.client1 = player1;
        this.client2 = clients[0];
        this.client3 = clients[1];
        this.client4 = clients[2];
        this.player2 = clients[0];
        client1.getStatus().resetListsFor2On2();
        client2.getStatus().resetListsFor2On2();
        client3.getStatus().resetListsFor2On2();
        client4.getStatus().resetListsFor2On2();
        var status1 = player1.getStatus();
        var status2 = player2.getStatus();
        var status3 = client3.getStatus();
        var status4 = client4.getStatus();
        Thread timerThread = new Thread(() -> {
            int time = 180;
            int counter1 = 0;
            int counter2 = 0;
            while (time > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (counter1 == (time > 60 ? 2 : 1)) {
                    status1.increaseElixirs();
                    Platform.runLater(() -> elixirCount.setText(String.valueOf(status1.getElixirs())));
                    counter1 = 0;
                }
                if (counter2 == (time > 60 ? 2 : 1)) {
                    status2.increaseElixirs();
                    counter2 = 0;
                }
                if (status1.getElixirs() < 10)
                    counter1++;
                if (status2.getElixirs() < 10)
                    counter2++;
                time--;
                int finalTime = time;
                Platform.runLater(() -> timerLabel.setText(finalTime /60 + ":" + String.format("%02d", finalTime %60)));
                if (time < 60) {
                    Platform.runLater(() -> timerLabel.setStyle("-fx-text-fill: #ff0000"));
                    Platform.runLater(() -> timerLabel.setStyle("-fx-font-weight: bold"));
                    Platform.runLater(() -> timerLabel.setStyle("-fx-font-size: 22"));
                }
            }
            Platform.runLater(() -> indicateTheWinner(client3, client4, client2));
        });
        Thread elixirThread = new Thread(() -> {
            while (true) {
                try {
                    Platform.runLater(() -> {
                        elixirImage.setAccessibleText(String.valueOf(status1.getElixirs()));
                        elixirBar.setProgress(status1.getElixirs()/10.0);
                    });
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread kingWatcher = new Thread(() -> {
            while (client1.isConnected()) {
                try {Thread.sleep(400);
                } catch (InterruptedException e) {e.printStackTrace();}
                if (!isKingAlive(client1)) {
                    Platform.runLater(() -> indicateTheWinner(client3, client4, client2));
                    break;
                }
                if (!isKingAlive(client3)) {
                    Platform.runLater(() -> indicateTheWinner(client3, client4, client2));
                    break;
                }
            }
        });
        Thread crownCounter = new Thread(() -> {
            while (client1.isConnected()) {
                Platform.runLater(() -> {
                    blueCrownNumber.setText(String.valueOf(gameModel.getAllyCrown()));
                    redCrownNumber.setText(String.valueOf(gameModel.getEnemyCrown()));
                });
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {e.printStackTrace();}
            }
        });
        crownCounter.start();
        kingWatcher.start();
        timerThread.start();
        elixirThread.start();
        nextCardImage.setFitHeight(60);
        nextCardImage.setFitWidth(45);
        componentUsernameLabel.setText(status4.getUsername() + " & " + status3.getUsername());
        secondLayerPane = new AnchorPane();
        secondLayerPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0)");
        gameModel = new GameModel();
        gameModel.initialize(status1, client3);
        gameView = new GameView();
        gameView.initialize(gameModel);
        secondLayerPane.getChildren().add(gameView);
        listedButtons = new ArrayList<>();
        listedButtons.add(card1Button);
        listedButtons.add(card2Button);
        listedButtons.add(card3Button);
        listedButtons.add(card4Button);
        for (int i = 0; i < 4; i++)
            setButtonImage(listedButtons.get(i), gameModel.getPlayersStatus()[0].getCardsDeskInUse().get(4 + i).getCardAddress());
        firstLayer.getChildren().add(gameView);
        startTimer();

        EventHandler<ActionEvent> cardButtonAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Platform.runLater(() -> {
                    gameModel.setStack((Button) actionEvent.getTarget(),
                            gameModel.getPlayersStatus()[0].getCardsDeskInUse().get(4 + listedButtons.indexOf((Button) actionEvent.getTarget())));
                    if (!firstLayer.getChildren().contains(ShapeHolder.getTopRectangle()))
                        firstLayer.getChildren().add(ShapeHolder.getTopRectangle());
                    status2.getAliveAllyTroops().forEach(troop -> {
                        if (troop.getCard().getName().equals(BuildingName.ARCHER_TOWER)) {
                            if (!firstLayer.getChildren().contains(ShapeHolder.getRightRectangle()) &&
                                    troop.getLocation().getX() > 172)
                                firstLayer.getChildren().add(ShapeHolder.getRightRectangle());
                            else if (!firstLayer.getChildren().contains(ShapeHolder.getLeftRectangle()) &&
                                    troop.getLocation().getX() < 172)
                                firstLayer.getChildren().add(ShapeHolder.getLeftRectangle());
                        }
                    });
                });
            }
        };
        listedButtons.forEach(button -> button.setOnAction(cardButtonAction));
    }

    /**
     * this method indicates the winner
     */
    private void indicateTheWinner(Player...players) {
        client1.disconnect();
        timerTask.cancel();
        timer.cancel();
        timer.purge();
        if (player2 instanceof Bot) {
            ((Bot)player2).disconnectFromGame();
        }
        int bluePoint = Integer.parseInt(blueCrownNumber.getText());
        int redPoint = Integer.parseInt(redCrownNumber.getText());
        String allyName = client1.getStatus().getUsername();
        String enemyName = players[0].getStatus().getUsername();
        if (players.length > 2) {
            allyName += " & " + players[2].getStatus().getUsername();
            enemyName += " & " + players[1].getStatus().getUsername();
        }
        if (!isKingAlive(client1)) {
            client1.getStatus().addRecord(allyName + "(" + bluePoint + ")" + "   LOST  " + "(" + 3 + ")" + enemyName);
            client1.getStatus().increaseXP(200);
            client1.getStatus().increaseTrophy(20);
        } else if (!isKingAlive(players[0])) {
            client1.getStatus().addRecord(allyName + "(" + 3 + ")" + "   WON  " + "(" + redPoint + ")" + enemyName);
            client1.getStatus().increaseXP(70);
        } else if (bluePoint > redPoint) {
            client1.getStatus().addRecord(allyName + "("  + bluePoint + ")" + "   WON   " + "(" + redPoint + ")" + enemyName);
            client1.getStatus().increaseXP(200);
            client1.getStatus().increaseTrophy(20);
        } else if (bluePoint < redPoint) {
            client1.getStatus().addRecord(allyName + "("  + bluePoint + ")" + "   LOST   " + "(" + redPoint + ")" + enemyName);
            client1.getStatus().increaseXP(70);
        } else {
            if (getTotalHPOfTowers(client1.getStatus()) > getTotalHPOfTowers(players[2].getStatus())) {
                client1.getStatus().addRecord(allyName + "("  + bluePoint + ")" + "   WON   " + "(" + redPoint + ")" + enemyName);
                client1.getStatus().increaseXP(200);
            } else if(getTotalHPOfTowers(client1.getStatus()) < getTotalHPOfTowers(players[2].getStatus())) {
                client1.getStatus().addRecord(allyName + "("  + bluePoint + ")" + "   LOST   " + "(" + redPoint + ")" + enemyName);
                client1.getStatus().increaseXP(70);
            } else {
                client1.getStatus().addRecord(allyName + "("  + bluePoint + ")" + "   DRAW   " + "(" + redPoint + ")" + enemyName);
                client1.getStatus().increaseXP(135);
            }
        }
        try {
            client1.sendCommand("<SAVE> " + toString(client1.getStatus()));
        } catch (IOException e) {e.printStackTrace();}
        client1.getStatus().setEnemyStatus(null);
        goToMainMenu();
    }

    /** this method switch from game to main menu. */
    private void goToMainMenu() {
        try {
            Stage playerStage = (Stage) blueCrownNumber.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader();
            Pane root = (Pane) loader.load(getClass().getResource("/mainMenu/menu.fxml").openStream());
            MenuController menuController = loader.getController();
            menuController.initialize(client1);
            Scene scene = new Scene(root);
            playerStage.setScene(scene);
            playerStage.setTitle("Clash Royal");
            playerStage.setResizable(false);
            playerStage.show();
            if (player2 instanceof Bot)
                ((Bot) player2).disconnectFromGame();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * this method is for checking which players' king is dead
     * @param player is the player whose players are going to be checked
     * @return true if king is alive else false
     */
    private boolean isKingAlive(Player player) {
        AtomicBoolean isKingAlive = new AtomicBoolean(false);
        player.getStatus().getAliveAllyTroops().forEach(troop -> {
            if (troop.getCard().getName().equals(BuildingName.KING_TOWER)) {
                isKingAlive.set(true);
            }
        });
        return isKingAlive.get();
    }

    /**
     * this method calculate total hp of the towers of a client
     * @param status is the status of that client
     * @return total number of hp
     */
    private int getTotalHPOfTowers(Status status) {
        AtomicInteger sum = new AtomicInteger();
        status.getAliveAllyTroops().forEach((troop) -> {
            if (troop.getCard().getName().equals(BuildingName.ARCHER_TOWER) ||
                troop.getCard().getName().equals(BuildingName.KING_TOWER))
                sum.addAndGet(troop.getHP());
        });
        return sum.get();
    }

    /**
     * this method set an image for a button
     * @param cardButton is the card linked to the button
     * @param imageAddress is address of the image of the card
     */
    private void setButtonImage(Button cardButton, String imageAddress) {
        Image image = new Image(imageAddress);
        ImageView view = new ImageView(image);
        view.setFitHeight(80);
        view.setFitWidth(80);
        cardButton.setGraphic(view);
    }

    /**
     * this method handles all key events
     * @param mouseEvent is a mouse event
     */
    @Override
    public void handle(MouseEvent mouseEvent) {
        Point2D mouseLocation = new Point2D(mouseEvent.getSceneX(), mouseEvent.getSceneY());
        if (mouseEvent.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
            if (gameModel.getStackedButton() != null &&
                gameModel.getStackedCard() != null &&
                (GameModel.isValidCoordination(mouseLocation, player2.getStatus().getAliveAllyTroops()) ||
                gameModel.getStackedCard() instanceof Spell) &&
                gameModel.getStackedCard().getCost() <= client1.getStatus().getElixirs()) {
                firstLayer.getChildren().remove(ShapeHolder.getLeftRectangle());
                firstLayer.getChildren().remove(ShapeHolder.getRightRectangle());
                firstLayer.getChildren().remove(ShapeHolder.getTopRectangle());
                var cardDeskInUse = gameModel.getPlayersStatus()[0].getCardsDeskInUse();
                var aliveAllyTroops = gameModel.getPlayersStatus()[0].getAliveAllyTroops();
                var stackedCard = gameModel.getStackedCard();
                for (int i = 0; i < (stackedCard instanceof Troop ? ((Troop) stackedCard).getCount() : 1); i++) {
                    if (i % 2 == 0)
                        aliveAllyTroops.add(new AliveTroop(gameModel.getStackedCard(),
                                            new Point2D(mouseLocation.getX() + 20 * i, mouseLocation.getY())));
                    if (i % 2 != 0)
                        aliveAllyTroops.add(new AliveTroop(gameModel.getStackedCard(),
                                            new Point2D(mouseLocation.getX(), mouseLocation.getY() + 20 * i)));
                }
                cardDeskInUse.remove(stackedCard);
                cardDeskInUse.add(0, gameModel.getStackedCard());
                for (int i = 0; i < 4; i++)
                    setButtonImage(listedButtons.get(i), cardDeskInUse.get(4 + i).getCardAddress());
                nextCardImage.setImage(new Image(cardDeskInUse.get(3).getCardAddress()));
                client1.getStatus().decreaseElixirs(gameModel.getStackedCard().getCost());
                if (player2 instanceof Client && client3 == null && client4 == null) {
                    try {
                        client1.sendCommand("<PLAY> "
                                + mouseLocation.getX() + " "
                                + mouseLocation.getY() + " "
                                + toString(gameModel.getStackedCard()));
                        //AudioClip clip = new AudioClip(gameModel.getStackedCard().getTone());
                        //clip.play();
                    } catch (IOException e) {e.printStackTrace();}
                } else if ( client2 != null &&
                            client3 != null &&
                            client4 != null) {
                    try {
                        client1.sendCommand("<PLAY_ALLY> "
                                + mouseLocation.getX() + " "
                                + mouseLocation.getY() + " "
                                + toString(gameModel.getStackedCard()));

                        client1.sendCommand("<PLAY_ENEMY> "
                                + mouseLocation.getX() + " "
                                + mouseLocation.getY() + " "
                                + toString(gameModel.getStackedCard()));
                    } catch (IOException ex) {ex.printStackTrace();}
                }
                gameModel.resetStack();
            }
        }
    }

    /** Write the object to a Base64 string. */
    private static String toString( Serializable o ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Schedules the model to update based on the timer.
     */
    private void startTimer() {
        this.timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                if (client1.isConnected())
                    Platform.runLater(() -> update());
            }
        };
        frameTimeInMilliseconds = (long)(1000.0 / FRAMES_PER_SECOND);
        this.timer.schedule(timerTask, 0, frameTimeInMilliseconds);
    }

    /**
     * updates the game
     */
    private void update() {
        gameModel.step();
        gameView.update(gameModel);
    }
}
