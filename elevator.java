import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Represents the direction an elevator or request is traveling.
 * IDLE is used when the elevator is stationary or a request has the same source and destination floor.
 */
public enum Direction {
    UP, DOWN, IDLE
}

/**
 * Represents the operational state of an elevator.
 * MOVING means the elevator is actively processing a request; IDLE means it has no pending work.
 */
public enum ElevatorState {
    MOVING, IDLE
}

/**
 * Represents a passenger's elevator request — where they are and where they want to go.
 * The direction is automatically derived from the source and destination floors at construction time.
 */
public class Request {
    private final int sourceFloor;       // Floor where the passenger is waiting
    private final int destinationFloor;  // Floor the passenger wants to reach
    private Direction direction;         // Travel direction derived from source/destination

    /**
     * Creates a new request and automatically determines the travel direction.
     * If destination > source, direction is UP.
     * If destination < source, direction is DOWN.
     * If equal, direction is IDLE (no movement needed).
     */
    public Request(int sourceFloor, int destinationFloor) {
        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
        if (destinationFloor > sourceFloor) {
            this.direction = Direction.UP;
        } else if (destinationFloor < sourceFloor) {
            this.direction = Direction.DOWN;
        } else {
            this.direction = Direction.IDLE;
        }
    }

    public int getSourceFloor() { return sourceFloor; }
    public int getDestinationFloor() { return destinationFloor; }
    public Direction getDirection() { return direction; }
}

/**
 * Represents a single elevator unit in the building.
 * Each elevator maintains its own queue of requests and processes them sequentially.
 * On creation, the elevator starts at floor 1 in an IDLE state with an empty request queue.
 */
public class Elevator {
    private int elevatorId;          // Unique identifier for this elevator
    private int currentFloor;        // The floor the elevator is currently on
    private ElevatorState state;     // Whether the elevator is moving or idle
    private List<Request> requests;  // Queue of pending requests to be fulfilled
    private Direction direction;     // Current travel direction of the elevator

    /**
     * Initializes the elevator at floor 1 with no pending requests.
     */
    public Elevator(int elevatorId) {
        this.elevatorId = elevatorId;
        this.currentFloor = 1;
        this.state = ElevatorState.IDLE;
        this.requests = new ArrayList<>();
    }

    /**
     * Adds a new request to this elevator's queue.
     * If the elevator was idle, it transitions to MOVING state.
     * Null requests are silently ignored.
     */
    public void addRequest(Request request) {
        if (request == null) {
            return;
        }
        requests.add(request);
        // Transition from idle to moving as soon as the first request arrives
        if (state == ElevatorState.IDLE) {
            state = ElevatorState.MOVING;
        }
    }

    public int getCurrentFloor() { return currentFloor; }
    public ElevatorState getState() { return state; }

    /**
     * Returns the direction this elevator needs to travel to reach the next queued request.
     * Compares the next request's source floor against the elevator's current floor.
     * Returns IDLE if there are no pending requests.
     */
    public Direction getDirection() {
        if (requests.isEmpty()) return Direction.IDLE;
        Request next = requests.get(0);
        return next.getSourceFloor() > currentFloor ? Direction.UP : Direction.DOWN;
    }

    public int getElevatorId() {
        return elevatorId;
    }

    public void setElevatorState(ElevatorState state) {
        this.state = state;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    /**
     * Processes the next request in the queue.
     * The elevator first moves to the passenger's source floor to pick them up,
     * then moves to their destination floor to drop them off.
     * Once the queue is empty, the elevator returns to IDLE state.
     */
    public void processNextRequest() {
        if (requests.isEmpty()) {
            state = ElevatorState.IDLE;
            return;
        }

        Request nextRequest = requests.remove(0);

        // Move to the floor where the passenger is waiting
        System.out.println("Elevator " + elevatorId + " moving from floor " + currentFloor + " to floor " + nextRequest.getSourceFloor());
        currentFloor = nextRequest.getSourceFloor();

        // Pick up the passenger and travel to their destination
        System.out.println("Elevator " + elevatorId + " moving from floor " + currentFloor + " to floor " + nextRequest.getDestinationFloor());
        currentFloor = nextRequest.getDestinationFloor();

        // No more work to do — mark elevator as idle
        if (requests.isEmpty()) {
            state = ElevatorState.IDLE;
        }
    }
}

/**
 * Strategy interface for selecting which elevator should handle a given request.
 * Implementations can use different algorithms — e.g. nearest elevator, least loaded, round-robin.
 * Swapping strategies at runtime is supported by ElevatorController.
 */
public interface ElevatorStrategy {
    Elevator selectElevator(List<Elevator> elevators, Request request);
}

/**
 * Selects the elevator closest (by floor distance) to the request's source floor.
 * Only considers elevators that are idle or already traveling in the same direction as the request,
 * avoiding the inefficiency of redirecting an elevator that is heading the other way.
 */
public class NearestElevatorStrategy implements ElevatorStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        Elevator nearest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            // Skip elevators moving in the opposite direction — assigning to them wastes time
            if (elevator.getState() == ElevatorState.IDLE || elevator.getDirection() == request.getDirection()) {
                int distance = Math.abs(elevator.getCurrentFloor() - request.getSourceFloor());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = elevator;
                }
            }
        }

        return nearest;
    }
}

/**
 * Central controller that manages all elevators in the building.
 * Implemented as a thread-safe singleton so only one controller exists per JVM instance.
 * Delegates elevator selection to a pluggable strategy, defaulting to NearestElevatorStrategy.
 */
public class ElevatorController {
    private List<Elevator> elevators;   // All elevators registered with this controller
    private ElevatorStrategy strategy;  // Current strategy for picking which elevator handles a request

    // Volatile ensures the instance is visible across threads without full synchronization on every read
    private static volatile ElevatorController instance;

    /**
     * Returns the singleton instance, creating it on the first call.
     * Uses double-checked locking to be safe under concurrent access.
     */
    public static ElevatorController getInstance() {
        if (instance == null) {
            synchronized (ElevatorController.class) {
                if (instance == null) {
                    instance = new ElevatorController();
                }
            }
        }
        return instance;
    }

    // Private constructor enforces singleton — callers must use getInstance()
    private ElevatorController() {
        this.elevators = new ArrayList<>();
        this.strategy = new NearestElevatorStrategy();
    }

    // Registers a new elevator with this controller so it can receive requests
    public void addElevator(Elevator elevator) {
        elevators.add(elevator);
    }

    // Replaces the current selection strategy — allows runtime switching (Strategy pattern)
    public void setElevatorStrategy(ElevatorStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Routes an incoming request to the most suitable elevator using the current strategy.
     * If no eligible elevator is found (all moving in the opposite direction), logs a message.
     */
    public void requestElevator(Request request) {
        Elevator selectedElevator = strategy.selectElevator(elevators, request);
        if (selectedElevator != null) {
            selectedElevator.addRequest(request);
        } else {
            System.out.println("No available elevator for request from floor " + request.getSourceFloor() + " to floor " + request.getDestinationFloor());
        }
    }

    /**
     * Drives all elevators to completion — each elevator keeps processing requests until idle.
     * This is a synchronous simulation; in a real system this would run on background threads.
     */
    public void processAllRequests() {
        for (Elevator elevator : elevators) {
            while (elevator.getState() == ElevatorState.MOVING) {
                elevator.processNextRequest();
            }
        }
    }
}


/*
    Example usage:

    ElevatorController controller = ElevatorController.getInstance();

    controller.addElevator(new Elevator(1));
    controller.addElevator(new Elevator(2));
    controller.addElevator(new Elevator(3));

    // Default strategy — NearestElevator
    controller.requestElevator(new Request(1, 7));
    controller.requestElevator(new Request(3, 10));
    controller.requestElevator(new Request(5, 2));

    controller.processAllRequests();

    // Swap strategy at runtime — Strategy pattern!
    controller.setElevatorStrategy(new NearestElevatorStrategy());
*/
