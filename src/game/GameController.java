package game;

import gameUtil.AliveTroop;
import gameUtil.BuildingName;
import gameUtil.Troop;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
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
    private GameModel gameModel;
    private Client client1;
    private Player player2;
    private ArrayList<Button> listedButtons;
    private Timer timer;
    private final int FRAMES_PER_SECOND = 10;
    public static long frameTimeInMilliseconds;
    private GameView gameView;
    private boolean isGameFinished = false;
    @FXML private Button card4Button;
    @FXML private Button card3Button;
    @FXML private Button card2Button;
    @FXML private Button card1Button;
    @FXML private AnchorPane firstLayer;
    @FXML private AnchorPane secondLayerPane;
    @FXML private Button tower1;
    @FXML private Button tower2;
    @FXML private Button tower3;
    @FXML private Button tower4;
    @FXML private Button king1;
    @FXML private Button king2;
    @FXML private Label timerLabel;
    @FXML private Label blueCrownNumber;
    @FXML private Label redCrownNumber;
    @FXML private Label componentUsernameLabel;
    @FXML private ProgressBar elixirBar;
    @FXML private ImageView nextCardImage;
    @FXML private ImageView elixirImage;

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
     */
    public void initialize(Client player1, Player player2) {
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
                if (counter1 == 2) {
                    status1.increaseElixirs();
                    counter1 = 0;
                }
                if (counter2 == 2) {
                    status2.increaseElixirs();
                    counter2 = 0;
                }
                if (status1.getElixirs() < 10)
                    counter1++;
                if (status2.getElixirs() < 10)
                    counter2++;
                time--;
                int finalTime = time;
                Platform.runLater(() -> timerLabel.setText(finalTime /60 + ":" + finalTime %60));
            }
            indicateTheWinner();
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
        Thread deadKing = new Thread(() -> {
            AtomicBoolean isKingAlive = new AtomicBoolean(false);
           client1.getStatus().getAliveAllyTroops().forEach(troop -> {
               if (troop.getCard().getName().equals(BuildingName.KING_TOWER))
                   isKingAlive.set(true);
           });
           if (!isKingAlive.get()) {
               indicateTheWinner();
           }
            player2.getStatus().getAliveAllyTroops().forEach(troop -> {
                if (troop.getCard().getName().equals(BuildingName.KING_TOWER))
                    isKingAlive.set(true);
            });
            if (!isKingAlive.get()) {
                indicateTheWinner();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        deadKing.start();
        timerThread.start();
        elixirThread.start();
        nextCardImage.setFitHeight(60);
        nextCardImage.setFitWidth(45);
        componentUsernameLabel.setText(status2.getUsername());
        secondLayerPane = new AnchorPane();
        secondLayerPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0)");
        gameModel = new GameModel();
        gameModel.initialize(status1, status2);
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
        client1 = new Client();
        client1.setStatus(status1);
        firstLayer.getChildren().add(gameView);
        startTimer();

        EventHandler<ActionEvent> cardButtonAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                gameModel.setStack((Button)actionEvent.getTarget(),
                gameModel.getPlayersStatus()[0].getCardsDeskInUse().get(4 + listedButtons.indexOf((Button)actionEvent.getTarget())));
            }
        };
        listedButtons.forEach(button -> button.setOnAction(cardButtonAction));
    }

    /**
     * this method indicates the winner
     */
    private void indicateTheWinner() {
        timer.cancel();
        if (player2 instanceof Bot) {
            ((Bot)player2).disconnectFromGame();
        }
        isGameFinished = true;
        int bluePoint = Integer.parseInt(blueCrownNumber.getText());
        int redPoint = Integer.parseInt(redCrownNumber.getText());
        if (!isKingAlive(client1)) {
            client1.getStatus().addRecord(client1.getStatus().getUsername() + "(" + bluePoint + ")" + "   WON   " + "(" + 3 + ")" + player2.getStatus().getUsername());
            client1.getStatus().increaseXP(200);
        }
        if (!isKingAlive(player2)) {
            client1.getStatus().addRecord(client1.getStatus().getUsername() + "(" + 3 + ")" + "   LOST   " + "(" + redPoint + ")" + player2.getStatus().getUsername());
            client1.getStatus().increaseXP(70);
        }
        if (bluePoint > redPoint) {
            client1.getStatus().addRecord(client1.getStatus().getUsername() + "("  + bluePoint + ")" + "   WON   " + "(" + redPoint + ")" + player2.getStatus().getUsername());
            client1.getStatus().increaseXP(200);
        } else if (bluePoint < redPoint) {
            client1.getStatus().addRecord(client1.getStatus().getUsername() + "("  + bluePoint + ")" + "   LOST   " + "(" + redPoint + ")" + player2.getStatus().getUsername());
            client1.getStatus().increaseXP(70);
        } else {
            if (getTotalHPOfTowers(client1.getStatus()) > getTotalHPOfTowers(player2.getStatus())) {
                client1.getStatus().addRecord(client1.getStatus().getUsername() + "("  + bluePoint + ")" + "   WON   " + "(" + redPoint + ")" + player2.getStatus().getUsername());
                client1.getStatus().increaseXP(200);
            } else if(getTotalHPOfTowers(client1.getStatus()) < getTotalHPOfTowers(player2.getStatus())) {
                client1.getStatus().addRecord(client1.getStatus().getUsername() + "("  + bluePoint + ")" + "   LOST   " + "(" + redPoint + ")" + player2.getStatus().getUsername());
                client1.getStatus().increaseXP(70);
            } else {
                client1.getStatus().addRecord(client1.getStatus().getUsername() + "("  + bluePoint + ")" + "   DRAW   " + "(" + redPoint + ")" + player2.getStatus().getUsername());
                client1.getStatus().increaseXP(135);
            }
        }
        try {
            client1.sendCommand("<SAVE> " + toString(client1.getStatus()));
        } catch (IOException e) {
            e.printStackTrace();
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
        System.out.println("mouse clicked first");
        System.out.println(gameModel.getStackedButton());
        System.out.println(gameModel.getStackedCard());
        if (mouseEvent.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
            if (gameModel.getStackedButton() != null &&
                gameModel.getStackedCard() != null &&
                GameModel.isValidCoordination(mouseLocation, player2.getStatus().getAliveAllyTroops()) &&
                gameModel.getStackedCard().getCost() <= client1.getStatus().getElixirs()) {
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
                gameModel.resetStack();
                if (player2 instanceof Client) {
                    try {
                        client1.sendCommand("<PLAY> " + mouseLocation.getX() + " "
                                                      + mouseLocation.getY() + " "
                                                      + toString(gameModel.getStackedCard()));
                    } catch (IOException e) {e.printStackTrace();}
                }
            }
        }
        mouseEvent.consume();
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
        TimerTask timerTask = new TimerTask() {
            public void run() {
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


    /**
     * this is a getter
     * @return true if game is finished otherwise false
     */
    public boolean isGameFinished() {
        return isGameFinished;
    }
}
