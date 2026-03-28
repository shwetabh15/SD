
import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Represents the booking status of a seat.
 *
 * AVAILABLE — seat is open for booking.
 * LOCKED    — seat is temporarily held during an active booking attempt
 *             (prevents double-booking while payment is being processed).
 * BOOKED    — seat has been successfully reserved and paid for.
 */
public enum SeatStatus
{
    AVAILABLE,
    LOCKED,
    BOOKED
}

/**
 * Represents a movie available for screening.
 *
 * Holds metadata about the film: a unique ID, its title, runtime in minutes,
 * and genre. This is a simple value object — immutable after construction.
 */
public class Movie
{
    private String movieId;  // Unique identifier for the movie (e.g. "MOV-001")
    private String title;    // Display title (e.g. "Inception")
    private int duration;    // Runtime in minutes
    private String genre;    // Genre label (e.g. "Thriller", "Comedy")

    /**
     * @param movieId  Unique identifier for this movie.
     * @param title    Human-readable title.
     * @param duration Runtime in minutes.
     * @param genre    Genre category.
     */
    public Movie(String movieId, String title, int duration, String genre)
    {
        this.movieId=movieId;
        this.title=title;
        this.duration=duration;
        this.genre=genre;
    }

    public String getMovieId()
    {
        return this.movieId;
    }

    public String getTitle()
    {
        return title;
    }

     public int getDuration()
    {
        return duration;
    }
    public String getGenre()
    {
        return genre;
    }
}

/**
 * Abstract base class for all seat types in a theatre screen.
 *
 * Encapsulates the common attributes every seat has regardless of its tier:
 * a unique ID, a physical seat number visible to the customer, a booking
 * status, and a price. Concrete subclasses (RegularSeat, LuxurySeat) fix
 * the price for their tier and delegate everything else here.
 *
 * Thread-safety note: status mutations are performed under the seat's own
 * monitor in BookingManager to prevent race conditions when multiple users
 * try to book the same seat concurrently.
 */
public abstract class Seat
{
    private int seatId;            // Internal unique identifier for the seat
    private int seatNumber;        // Row/column label shown on the ticket
    private SeatStatus seatStatus; // Current availability state
    private double price;          // Ticket price in Indian Rupees (₹)

    /**
     * @param seatId     Internal seat identifier.
     * @param seatNumber Seat label shown to the customer.
     * @param seatStatus Initial status (typically AVAILABLE at show creation).
     * @param price      Ticket price in ₹.
     */
    public Seat(int seatId,int seatNumber,SeatStatus seatStatus,double price)
    {
        this.seatId=seatId;
        this.seatNumber=seatNumber;
        this.seatStatus=seatStatus;
        this.price=price;
    }

    public SeatStatus getStatus()
    {
        return seatStatus;
    }

    /** Updates the booking status. Called by BookingManager under synchronization. */
   public void setStatus(SeatStatus status)
    {
    this.seatStatus = status;
    }

    public int getSeatId()
    {
        return seatId;
    }

    public int getSeatNumber()
    {
        return seatNumber;
    }

    public double getSeatPrice()
    {
        return price;
    }
}

/**
 * Standard economy-tier seat priced at ₹200.
 *
 * Initialises as AVAILABLE. The price is a class-level constant so all
 * RegularSeat instances share the same fare without redundant per-object storage.
 */
class RegularSeat extends Seat {
    private static final double PRICE = 200.0;

    /**
     * @param seatId     Internal seat identifier.
     * @param seatNumber Seat label visible on the ticket.
     */
    public RegularSeat(int seatId, int seatNumber) {
        super(seatId, seatNumber, SeatStatus.AVAILABLE, PRICE);
    }
}

/**
 * Premium/luxury-tier seat priced at ₹500.
 *
 * Same structure as RegularSeat but with a higher fixed fare, representing
 * recliner or premium seats typically located in the back rows of the screen.
 */
class LuxurySeat extends Seat {
    private static final double PRICE = 500.0;

    /**
     * @param seatId     Internal seat identifier.
     * @param seatNumber Seat label visible on the ticket.
     */
    public LuxurySeat(int seatId, int seatNumber) {
        super(seatId, seatNumber, SeatStatus.AVAILABLE, PRICE);
    }
}


/**
 * Represents a physical screening room (auditorium) inside a Theatre.
 *
 * A Theatre can have multiple screens running different shows simultaneously.
 * totalSeats describes the physical capacity; the actual Seat objects for a
 * specific show are managed by the Show class, not here.
 */
public class Screen {
    private int screenId;      // Unique identifier within the theatre
    private String screenName; // Display name (e.g. "Screen 1", "IMAX")
    private int totalSeats;    // Physical seating capacity of this auditorium

    /**
     * @param screenId   Unique screen identifier.
     * @param screenName Human-readable name for this auditorium.
     * @param totalSeats Total number of seats the screen can accommodate.
     */
    public Screen(int screenId, String screenName, int totalSeats) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.totalSeats = totalSeats;
    }

    public int getScreenId() { return screenId; }
    public String getScreenName() { return screenName; }
    public int getTotalSeats() { return totalSeats; }
}

/**
 * Represents a single scheduled screening of a Movie on a Screen.
 *
 * A Show ties together the movie being played, the screen it plays on,
 * the date/time it starts, and the full list of seats for that screening.
 * Seats are populated at show-creation time and their statuses change as
 * customers book and cancel.
 */
public class Show
{
    private int showId;              // Unique identifier for this scheduled show
    private Movie movie;             // The movie being screened
    private Screen screen;           // The auditorium hosting the show
    private LocalDateTime showTime;  // Scheduled start date and time
    private List<Seat> seats;        // All seats associated with this show instance

    /**
     * Creates a new Show with an empty seat list. Call addSeat() to populate
     * it with RegularSeat / LuxurySeat instances before opening for booking.
     *
     * @param showId   Unique identifier for this show.
     * @param movie    The movie being screened.
     * @param screen   The auditorium where the show will run.
     * @param showTime The date and time the show starts.
     */
    public Show(int showId, Movie movie,Screen screen,LocalDateTime showTime)
    {
        this.showId=showId;
        this.movie=movie;
        this.screen=screen;
        this.showTime=showTime;
        this.seats=new ArrayList<>();
    }

    /**
     * Adds a seat to this show's seat inventory.
     * Null seats are silently ignored to protect list integrity.
     *
     * @param seat The seat to add; no-op if null.
     */
    public void addSeat(Seat seat)
    {
        if(seat!=null)
        {
            seats.add(seat);
        }
    }

    /**
     * Returns the first AVAILABLE seat found in the seat list, or null if
     * the show is sold out. Useful for quick single-seat lookup; to book
     * specific seats, iterate getSeats() and filter directly.
     *
     * @return An available Seat, or null if none exist.
     */
    public Seat getAvaiableSeat()
    {
        for(Seat seat:seats)
        {
            if(seat!=null && seat.getStatus()==SeatStatus.AVAILABLE)
            {
                return seat;
            }
        }
        return null;


    }

      public int getShowId()
    {
        return showId;
    }

    public Movie getMovie()
    {
        return movie;
    }

    public LocalDateTime getShowTime() { return showTime; }

    public Screen getScreen()
    {
        return screen;
    }

    public List<Seat> getSeats()
    {
        return seats;
    }




}



/**
 * Represents a cinema theatre that contains one or more screens.
 *
 * Theatre is the top-level venue entity. Each Theatre has a unique ID,
 * a display name, a city/area location, and a list of Screen objects.
 * Shows are scheduled on individual Screens inside the Theatre.
 */
public class Theatre
{
    private String theatreId;      // Unique identifier (e.g. "TH-BLR-001")
    private String theatreName;    // Display name (e.g. "PVR Cinemas, Koramangala")
    private String location;       // City or locality where the theatre is situated
    private List<Screen> screen;   // Screens/auditoriums inside this theatre

    /**
     * @param theatreId   Unique identifier for the theatre.
     * @param theatreName Human-readable theatre name.
     * @param location    City or locality where the theatre is located.
     */
    public Theatre(String theatreId, String theatreName, String location)
    {
        this.theatreId = theatreId;
        this.theatreName = theatreName;
        this.location = location;
        this.screen = new ArrayList<>();
    }

    /**
     * Adds a screen to this theatre's screen list. Null screens are ignored.
     *
     * @param screen The Screen to add.
     */
    public void addScreen(Screen screen)
    {
        if(screen!=null)
        {
            this.screen.add(screen);
        }
    }

    public String getTheatreId()
    {
        return theatreId;
    }

    public String getTheatreName()
    {
        return theatreName;
    }

    public String getLocation()
    {
        return location;
    }

    public List<Screen> getScreens()
    {
        return screen;
    }
}

/**
 * Represents a registered customer who can make bookings.
 *
 * Holds basic contact details. In a production system this would also store
 * authentication credentials and booking history, but here it acts as a
 * lightweight identity object that gets attached to a Booking.
 */
public class User
{
    private String userId; // Unique customer identifier (e.g. "USR-42")
    private String name;   // Customer's full name
    private String email;  // Email address for booking confirmation
    private String phone;  // Phone number for SMS alerts

    /**
     * @param userId Unique identifier for this user.
     * @param name   Customer's full name.
     * @param email  Email address.
     * @param phone  Contact phone number.
     */
    public User(String userId, String name, String email, String phone)
    {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}

/**
 * Lifecycle states of a Booking.
 *
 * PENDING   — booking has been initiated but payment not yet processed.
 * CONFIRMED — payment succeeded; seats are permanently reserved.
 * CANCELLED — booking was cancelled and seats released back to AVAILABLE.
 */
public enum BookingStatus
{
    PENDING,
    CONFIRMED,
    CANCELLED
}

/**
 * Strategy interface for payment processing.
 *
 * Different payment modes (cash, card, UPI) implement this interface,
 * allowing BookingManager to trigger payment without knowing the concrete
 * type — a classic application of the Strategy design pattern.
 */
public interface Payment
{
    /**
     * Processes a payment for the given amount.
     *
     * @param amount the amount to charge in rupees
     */
    void processPayment(double amount);
}

/** Payment implementation for cash transactions. */
public class CashPayment implements Payment
{
    @Override
    public void processPayment(double amount)
    {
         System.out.println("Processing cash payment of ₹" + amount);
    }
}

/** Payment implementation for card transactions. */
public class CardPayment implements Payment {

    @Override
    public void processPayment(double amount) {
        System.out.println("Processing card payment of ₹" + amount);
    }
}

/** Payment implementation for UPI transactions. */
public class UPIPayment implements Payment {

    @Override
    public void processPayment(double amount) {
        System.out.println("Processing UPI payment of ₹" + amount);
    }
}

/**
 * Immutable record of a completed seat reservation.
 *
 * Built using the Builder pattern to handle the mix of required and optional
 * fields cleanly, avoiding telescoping constructors. Once constructed, a
 * Booking cannot be mutated — cancellation is handled by BookingManager
 * updating the underlying Seat statuses directly.
 *
 * Design pattern: Builder
 */
public class Booking {
    // All fields are private and final — Booking is fully immutable after construction
    private final String bookingId;          // Unique booking reference (e.g. "BKG-1711612345678")
    private final User user;                 // Customer who made the booking
    private final Show show;                 // The show that was booked
    private final List<Seat> seats;          // Seats included in this booking
    private final LocalDateTime bookingTime; // Timestamp when the booking was created
    private final Payment payment;           // Payment strategy used to complete the booking
    private final double totalAmount;        // Total amount charged in ₹
    private final BookingStatus status;      // Current lifecycle status of the booking

    // Private constructor — only the inner Builder class can instantiate Booking
    private Booking(Builder builder) {
        this.bookingId = builder.bookingId;
        this.user = builder.user;
        this.show = builder.show;
        this.seats = builder.seats;
        this.bookingTime = builder.bookingTime;
        this.payment = builder.payment;
        this.totalAmount = builder.totalAmount;
        this.status = builder.status;
    }

    // Getters
    public String getBookingId() { return bookingId; }
    public User getUser() { return user; }
    public Show getShow() { return show; }
    public List<Seat> getSeats() { return seats; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public Payment getPayment() { return payment; }
    public double getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }

    /**
     * Builder for constructing a Booking in a readable, flexible way.
     *
     * Required fields are enforced in the constructor with a null check;
     * optional fields (payment, totalAmount, status) have safe defaults and
     * are set via fluent setter methods that return 'this' for chaining.
     *
     * Usage example:
     *   Booking b = new Booking.Builder(id, user, show, seats, time)
     *                   .setPayment(upi)
     *                   .setTotalAmount(700.0)
     *                   .setStatus(BookingStatus.CONFIRMED)
     *                   .build();
     */
    // Static nested Builder class
    public static class Builder {
        // Required fields — must be supplied at Builder construction time
        private final String bookingId;
        private final User user;
        private final Show show;
        private final List<Seat> seats;
        private final LocalDateTime bookingTime;

        // Optional fields with defaults
        private Payment payment = null;                       // No payment until explicitly set
        private double totalAmount = 0.0;                     // Defaults to zero
        private BookingStatus status = BookingStatus.PENDING; // Starts as PENDING

        /**
         * Validates and stores all required fields. Throws if any are null.
         *
         * @param bookingId   Unique booking reference string.
         * @param user        The customer making the booking.
         * @param show        The show being booked.
         * @param seats       The list of seats being reserved.
         * @param bookingTime The timestamp of the booking.
         */
        // Required fields in constructor
        public Builder(String bookingId,
                       User user,
                       Show show,
                       List<Seat> seats,
                       LocalDateTime bookingTime) {

            if (bookingId == null || user == null || show == null
                    || seats == null || bookingTime == null) {
                throw new IllegalArgumentException("Required fields cannot be null");
            }

            this.bookingId = bookingId;
            this.user = user;
            this.show = show;
            this.seats = seats;
            this.bookingTime = bookingTime;
        }

        // Optional setters — return this for chaining

        /** Sets the payment strategy used for this booking. */
        public Builder setPayment(Payment payment) {
            this.payment = payment;
            return this;
        }

        /** Sets the total amount charged for the booking in ₹. */
        public Builder setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /** Sets the booking lifecycle status (e.g. CONFIRMED, CANCELLED). */
        public Builder setStatus(BookingStatus status) {
            this.status = status;
            return this;
        }

        // Build — creates the immutable Booking object
        public Booking build() {
            return new Booking(this);
        }
    }
}

/**
 * Singleton service responsible for all booking and cancellation operations.
 *
 * BookingManager is the core orchestrator of the booking flow:
 *   1. Locks each requested seat atomically to prevent double-booking.
 *   2. Rolls back any partial locks if even one seat is unavailable.
 *   3. Marks all seats BOOKED, sums the prices, and processes payment.
 *   4. Returns an immutable, CONFIRMED Booking object on success.
 *
 * Design patterns used:
 *   - Singleton (thread-safe double-checked locking) — one instance manages
 *     all bookings, providing a single point of concurrency control.
 *   - Strategy (via Payment interface) — payment method is injected at call time.
 *   - Builder (via Booking.Builder) — creates the final immutable Booking.
 */
public class BookingManager
{
    // volatile ensures the reference is visible across threads after first write,
    // without requiring full synchronization on every subsequent read.
    private static volatile BookingManager instance;

    // Private constructor prevents external instantiation — only getInstance() creates the object
    private BookingManager() {}

    /**
     * Returns the single BookingManager instance, creating it on the first call.
     *
     * Uses double-checked locking (DCL) to avoid synchronization overhead
     * on every call after initialization. The outer null-check is the fast path
     * for already-initialized state; the inner null-check inside the synchronized
     * block prevents a race where two threads both pass the outer check before
     * the lock is acquired.
     *
     * @return The application-wide BookingManager instance.
     */
    public static BookingManager getInstance()
    {
        if (instance == null)                       // First check (no lock — fast path after init)
        {
            synchronized (BookingManager.class)     // Acquire class-level lock for initialization
            {
                if (instance == null)               // Second check (under lock — safe from race)
                {
                    instance = new BookingManager();
                }
            }
        }
        return instance;
    }

    /**
     * Attempts to book the requested seats for a user and process payment.
     *
     * Algorithm — all-or-nothing with per-seat locking:
     *   Phase 1 (Lock): Iterate over each requested seat and synchronize on
     *     that seat's monitor. If AVAILABLE, mark it LOCKED and track it.
     *     If any seat is not AVAILABLE, roll back by releasing all already-locked
     *     seats back to AVAILABLE, then return null to signal failure.
     *   Phase 2 (Confirm): All seats locked successfully — mark each BOOKED
     *     and accumulate the total price.
     *   Phase 3 (Pay): Invoke the payment strategy with the total amount.
     *   Phase 4 (Record): Build and return a CONFIRMED Booking object.
     *
     * Per-seat synchronization (synchronized(seat)) prevents two concurrent
     * threads from reserving the same seat simultaneously, without locking the
     * entire seat list and blocking bookings of unrelated seats.
     *
     * @param user    The customer requesting the booking.
     * @param show    The show to book seats for.
     * @param seats   The specific seats the customer wants to reserve.
     * @param payment The payment strategy to charge on success.
     * @return A CONFIRMED Booking on success, or null if any seat was unavailable.
     */
    public Booking bookSeats(User user, Show show, List<Seat> seats, Payment payment)
    {
        List<Seat> lockedSeats = new ArrayList<>();

        // Phase 1: Attempt to lock all requested seats one at a time.
        // If any seat is unavailable, abort and roll back all locks acquired so far.
        for (Seat seat : seats)
        {
            synchronized (seat)  // Lock on the individual seat to prevent concurrent status changes
            {
                if (seat.getStatus() == SeatStatus.AVAILABLE)
                {
                    seat.setStatus(SeatStatus.LOCKED);  // Temporarily hold the seat
                    lockedSeats.add(seat);
                }
                else
                {
                    // Rollback — release all seats locked so far before returning failure
                    for (Seat locked : lockedSeats)
                    {
                        synchronized (locked)
                        {
                            locked.setStatus(SeatStatus.AVAILABLE);  // Undo the temporary lock
                        }
                    }
                    return null;  // Booking failed: at least one seat was not available
                }
            }
        }

        // All seats locked successfully — mark as BOOKED
        // Phase 2: Confirm all locked seats as permanently BOOKED and sum the total price.
        double totalAmount = 0;
        for (Seat seat : lockedSeats)
        {
            synchronized (seat)
            {
                seat.setStatus(SeatStatus.BOOKED);   // Permanent reservation
                totalAmount += seat.getSeatPrice();  // Accumulate fare for each seat
            }
        }

        // Phase 3: Charge the customer using whichever payment strategy was provided.
        payment.processPayment(totalAmount);

        // Phase 4: Build and return the immutable Booking record.
        // Booking ID uses current epoch millis to ensure uniqueness across bookings.
        return new Booking.Builder(
                "BKG-" + System.currentTimeMillis(),  // Unique booking reference
                user,
                show,
                lockedSeats,
                LocalDateTime.now()
        )
                .setPayment(payment)
                .setTotalAmount(totalAmount)
                .setStatus(BookingStatus.CONFIRMED)
                .build();
    }

    /**
     * Cancels an existing booking by releasing all its seats back to AVAILABLE.
     *
     * Each seat is unlocked under its own monitor, consistent with the locking
     * strategy used during booking. After this call, the released seats become
     * immediately available for new bookings.
     *
     * Note: The Booking object is immutable and retains its CONFIRMED status
     * after cancellation. In a production system you would produce a new
     * Booking snapshot with status CANCELLED for audit purposes.
     *
     * @param booking The confirmed booking to cancel.
     */
    public void cancelBooking(Booking booking)
    {
        for (Seat seat : booking.getSeats())
        {
            synchronized (seat)
            {
                seat.setStatus(SeatStatus.AVAILABLE);  // Release seat back to the available pool
            }
        }
    }
}
