import java.util.Random;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Snake {
    private int head;
    private int tail;

    public Snake(int head, int tail) {
        if (tail >= head)
            throw new IllegalArgumentException("Tail must be less than head.");
        this.head = head;
        this.tail = tail;
    }

    public int getHead() { return head; }
    public int getTail() { return tail; }
}

public class Ladder {
    private int bottom;
    private int top;

    public Ladder(int bottom, int top) {
        if (bottom >= top)
            throw new IllegalArgumentException("Bottom must be less than top.");
        this.bottom = bottom;
        this.top = top;
    }

    public int getBottom() { return bottom; }
    public int getTop() { return top; }
}

public class Player {
    private final String name;
    private final String playerId;
    private int position;

    public Player(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
        this.position = 0;
    }

    public String getName() { return name; }
    public String getPlayerId() { return playerId; }
    public int getPosition() { return position; }

    public void move(int steps) {
        if (position + steps > 100) {
            System.out.println(name + " needs exact roll to win. Stay at " + position);
            return;
        }
        position += steps;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

public interface Dice {
    int roll();
}

public class StandardDice implements Dice {
    private final Random random;

    public StandardDice() {
        this.random = new Random();
    }

    @Override
    public int roll() {
        return random.nextInt(6) + 1;
    }
}

public class LoadedDice implements Dice {
    @Override
    public int roll() {
        return 6; // always rolls 6 — for testing
    }
}

public class Board {
    private Map<Integer, Integer> snakes;
    private Map<Integer, Integer> ladders;

    public Board() {
        this.snakes = new HashMap<>();
        this.ladders = new HashMap<>();
    }

    public void addSnake(Snake snake) {
        snakes.put(snake.getHead(), snake.getTail());
    }

    public void addLadder(Ladder ladder) {
        ladders.put(ladder.getBottom(), ladder.getTop());
    }

    public int getNewPosition(int position) {
        if (snakes.containsKey(position) && ladders.containsKey(position))
            throw new IllegalStateException("Both snake and ladder at same position!");

        if (snakes.containsKey(position)) {
            System.out.println("Snake! Sliding down from " + position + " to " + snakes.get(position));
            return snakes.get(position);
        }

        if (ladders.containsKey(position)) {
            System.out.println("Ladder! Climbing up from " + position + " to " + ladders.get(position));
            return ladders.get(position);
        }

        return position;
    }
}

public class Game {
    private static volatile Game instance;

    // Private constructor — Singleton
    private Game() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.dice = new StandardDice(); // default dice
        this.currentPlayerIndex = 0;
    }

    public static Game getInstance() {
        if (instance == null) {
            synchronized (Game.class) {
                if (instance == null) {  // double checked locking
                    instance = new Game();
                }
            }
        }
        return instance;
    }

    private Board board;
    private List<Player> players;
    private Dice dice;
    private int currentPlayerIndex;

    public void addPlayer(Player player) {
        if (player != null) players.add(player);
    }

    public void addSnake(Snake snake) {
        board.addSnake(snake);
    }

    public void addLadder(Ladder ladder) {
        board.addLadder(ladder);
    }

    public void setDice(Dice dice) {
        this.dice = dice; // Strategy pattern — swap at runtime
    }

    public void playTurn() {
        if (isGameOver()) {
            System.out.println("Game is already over!");
            return;
        }

        Player currentPlayer = players.get(currentPlayerIndex);
        int roll = dice.roll();
        System.out.println(currentPlayer.getName() + " rolled: " + roll);

        // Move player
        currentPlayer.move(roll);

        // Check snake or ladder
        int newPosition = board.getNewPosition(currentPlayer.getPosition());
        currentPlayer.setPosition(newPosition);

        System.out.println(currentPlayer.getName() + " is now at position " + currentPlayer.getPosition());

        // Check win
        if (currentPlayer.getPosition() == 100) {
            System.out.println("🎉 " + currentPlayer.getName() + " wins!");
            return;
        }

        // Next player's turn
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public boolean isGameOver() {
        return players.stream().anyMatch(p -> p.getPosition() == 100);
    }
}