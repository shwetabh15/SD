
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents the current state of a physical book copy in the library.
 * AVAILABLE — on the shelf and ready to be borrowed.
 * BORROWED   — currently checked out by a member.
 * RESERVED   — held for a member who requested it in advance.
 * LOST       — reported missing; cannot be issued until recovered.
 */
public enum BookStatus {
    AVAILABLE,
    BORROWED,
    RESERVED,
    LOST
}

/**
 * Represents the logical definition of a book — title, author, and ISBN.
 * This is not a physical copy; it describes the book itself.
 * Multiple BookItem instances can share the same Book data (i.e. multiple copies of one title).
 */
public class Book {
    private final String isbn;    // Internationally unique identifier for this book's edition
    private final String title;
    private final String author;
    private BookStatus status;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return this.title;
    }
}

/**
 * Represents a specific physical copy of a book held by the library.
 * Each copy has its own ID, status, and due date that are tracked independently
 * from other copies of the same title.
 */
class BookItem extends Book {
    private final String copyId;   // Unique identifier for this particular physical copy
    private BookStatus status;     // Current availability status of this copy
    Date dueDate;                  // The date this copy must be returned; set when the copy is borrowed

    /**
     * Creates a new physical copy, marked AVAILABLE by default.
     */
    public BookItem(String isbn, String title, String author, String copyId) {
        super(isbn, title, author);
        this.copyId = copyId;
        this.status = BookStatus.AVAILABLE;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public BookStatus getStatus() {
        return status;
    }

    public String getCopyId() { return copyId; }
}

/**
 * Strategy interface defining the borrowing privileges for a membership tier.
 * Each tier (Basic, Premium, VIP) provides its own implementation with different limits and fees.
 * Using a strategy here lets Member behavior vary by tier without any conditional logic in Member itself.
 */
public interface MembershipStrategy {
    int getMaxBooksAllowed();      // Maximum number of books the member may have checked out simultaneously
    int getBorrowDurationDays();   // Number of days a member can keep a book before it becomes overdue
    double getLateFeePerDay();     // Fine amount charged per day for each day past the due date
}

/**
 * Basic membership tier — tightest borrowing limit, shortest loan window, highest late fee.
 * Intended for occasional or new library members.
 */
class BasicMembership implements MembershipStrategy {
    @Override
    public int getMaxBooksAllowed() { return 3; }

    @Override
    public int getBorrowDurationDays() { return 7; }

    @Override
    public double getLateFeePerDay() { return 10; }
}

/**
 * Premium membership tier — moderate limits and a reduced late fee compared to Basic.
 */
class PremiumMembership implements MembershipStrategy {
    @Override
    public int getMaxBooksAllowed() { return 5; }

    @Override
    public int getBorrowDurationDays() { return 14; }

    @Override
    public double getLateFeePerDay() { return 5; }
}

/**
 * VIP membership tier — highest borrowing limit, longest loan window, and lowest late fee.
 * Intended for frequent or long-standing library members.
 */
class VIPMembership implements MembershipStrategy {
    @Override
    public int getMaxBooksAllowed() { return 10; }

    @Override
    public int getBorrowDurationDays() { return 30; }

    @Override
    public double getLateFeePerDay() { return 1; }
}

/**
 * Factory that instantiates the correct MembershipStrategy from a plain string type name.
 * Centralizes membership creation so callers never reference concrete tier classes directly.
 * Throws IllegalArgumentException if the type string does not match a known tier.
 */
class MembershipFactory {
    public static MembershipStrategy getMembership(String type) {
        switch (type.toLowerCase()) {
            case "basic":   return new BasicMembership();
            case "premium": return new PremiumMembership();
            case "vip":     return new VIPMembership();
            default: throw new IllegalArgumentException("Unknown membership type: " + type);
        }
    }
}

/**
 * Base class for all people who interact with the library system.
 * Holds the identity fields common to both members and librarians.
 */
class User {
    String userId;   // Unique identifier assigned to this user in the system
    String name;

    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}

/**
 * A registered library member who can borrow and return book copies.
 * Borrowing rules — how many books, for how long, and at what fine rate — are
 * fully governed by the member's MembershipStrategy, resolved at construction time.
 */
class Member extends User {
    private final MembershipStrategy membershipStrategy;  // Encapsulates this member's tier privileges
    List<BookItem> issuedBooks = new ArrayList<>();        // Physical copies currently checked out by this member

    /**
     * Creates a member and resolves their membership tier from the provided type string.
     */
    public Member(String userId, String name, String membershipType) {
        super(userId, name);
        this.membershipStrategy = MembershipFactory.getMembership(membershipType);
    }

    public MembershipStrategy getMembershipStrategy() {
        return membershipStrategy;
    }

    /**
     * Attempts to issue a book copy to this member.
     * Two preconditions must hold before the book is issued:
     *   1. The member has not reached their tier's concurrent borrow limit.
     *   2. The requested copy's status is AVAILABLE.
     * On success, marks the copy BORROWED and calculates a due date based on the tier's loan duration.
     * Returns true if the book was successfully issued, false if either precondition failed.
     */
    public boolean issueBook(BookItem book) {
        // Enforce the membership tier's maximum concurrent borrow limit
        if (issuedBooks.size() >= membershipStrategy.getMaxBooksAllowed()) {
            System.out.println("Cannot issue more books. Limit reached.");
            return false;
        }
        // Only copies with AVAILABLE status can be borrowed
        if (book.getStatus() != BookStatus.AVAILABLE) {
            System.out.println("Book is not available for issue.");
            return false;
        }

        book.setStatus(BookStatus.BORROWED);

        // Set the due date by advancing today by the tier's allowed loan duration
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, membershipStrategy.getBorrowDurationDays());
        book.dueDate = calendar.getTime();

        System.out.println(name + " issued book " + book.getCopyId());
        return true;
    }

    /**
     * Processes the return of a borrowed book copy.
     * Verifies the copy was actually issued to this member, then marks it AVAILABLE.
     * If the return is past the due date, calculates a fine based on the number of overdue
     * days multiplied by the tier's daily late fee rate and prints the amount owed.
     */
    public void returnBook(BookItem book) {
        // Reject returns for copies that were never issued to this member
        if (!issuedBooks.contains(book)) {
            System.out.println("This book was not issued to " + name);
            return;
        }

        book.setStatus(BookStatus.AVAILABLE);
        issuedBooks.remove(book);
        System.out.println(name + " returned book " + book.getCopyId());

        // Calculate a late fine if the book is returned after its due date
        double fine = 0;
        Date currentDate = new Date();
        if (currentDate.after(book.dueDate)) {
            long daysLate = (currentDate.getTime() - book.dueDate.getTime()) / (1000 * 60 * 60 * 24);
            fine = daysLate * membershipStrategy.getLateFeePerDay();
        }

        issuedBooks.remove(book);

        if (fine > 0) {
            System.out.println("Late return. Fine: " + fine);
        }
    }
}

/**
 * A library staff member responsible for managing the physical book catalog.
 * Unlike a Member, a Librarian does not borrow books — they add and remove copies from the Library.
 * All catalog mutations go through the Library singleton to ensure thread safety.
 */
public class Librarian extends User {

    public Librarian(String userId, String name) {
        super(userId, name);
    }

    /**
     * Adds a new physical copy to the library's catalog and confirms the addition.
     */
    public void addBook(Library library, BookItem book) {
        library.addBook(book);
        System.out.println("Book " + book.getCopyId() + " added to library.");
    }

    /**
     * Removes a physical copy from the library's catalog and confirms the removal.
     */
    public void removeBook(Library library, BookItem book) {
        library.removeBook(book);
        System.out.println("Book " + book.getCopyId() + " removed from library.");
    }
}

/**
 * The central catalog that holds all physical book copies in the library.
 * Implemented as a thread-safe singleton — only one Library instance exists per JVM.
 * All mutating operations (add, remove) are synchronized to prevent race conditions
 * when multiple threads issue or return books concurrently.
 */
class Library {
    // Volatile ensures the instance reference is visible across threads without full synchronization on every read
    private static volatile Library instance;

    // Private constructor enforces singleton — callers must go through getInstance()
    private Library() {}

    /**
     * Returns the singleton Library instance, creating it on the first call.
     * Uses double-checked locking so initialization is safe under concurrent access
     * while avoiding synchronization overhead on subsequent calls.
     */
    public static Library getInstance() {
        if (instance == null) {
            synchronized (Library.class) {
                if (instance == null) {
                    instance = new Library();
                }
            }
        }
        return instance;
    }

    private List<BookItem> books = new ArrayList<>();  // Master list of all physical copies in the library

    // Adds a copy to the catalog; synchronized to prevent concurrent modification
    public synchronized void addBook(BookItem book) {
        books.add(book);
    }

    // Removes a copy from the catalog; synchronized to prevent concurrent modification
    public synchronized void removeBook(BookItem book) {
        books.remove(book);
    }

    // Returns the full list of copies; synchronized so callers always see a consistent snapshot
    public synchronized List<BookItem> getBooks() {
        return books;
    }

    /**
     * Searches for all copies whose title matches the given string (case-insensitive).
     * Returns an empty list if no matches are found.
     */
    public List<BookItem> searchBooksByTitle(String title) {
        List<BookItem> results = new ArrayList<>();
        for (BookItem book : books) {
            if (book.getTitle().equalsIgnoreCase(title)) {
                results.add(book);
            }
        }
        return results;
    }
}

/*
    -------------------------
    Example usage
    -------------------------

    // Get the single Library instance
    Library library = Library.getInstance();

    // Librarian adds book copies to the catalog
    Librarian librarian = new Librarian("L001", "Alice");
    BookItem copy1 = new BookItem("978-0-06-112008-4", "To Kill a Mockingbird", "Harper Lee", "COPY-001");
    BookItem copy2 = new BookItem("978-0-7432-7356-5", "1984", "George Orwell", "COPY-002");
    librarian.addBook(library, copy1);
    librarian.addBook(library, copy2);

    // Create members with different membership tiers
    Member basicMember   = new Member("M001", "Bob",   "basic");    // max 3 books, 7 days, $10/day fine
    Member premiumMember = new Member("M002", "Carol", "premium");  // max 5 books, 14 days, $5/day fine
    Member vipMember     = new Member("M003", "Dave",  "vip");      // max 10 books, 30 days, $1/day fine

    // Member borrows a book
    basicMember.issueBook(copy1);   // succeeds — copy1 is AVAILABLE
    basicMember.issueBook(copy1);   // fails — copy1 is now BORROWED

    // Member returns the book; fine is calculated if returned after due date
    basicMember.returnBook(copy1);

    // Search the catalog by title (case-insensitive)
    List<BookItem> results = library.searchBooksByTitle("1984");

    // Librarian removes a damaged copy from the catalog
    librarian.removeBook(library, copy2);
*/
