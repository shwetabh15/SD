// ============================================================
//  Uber-like Ride Sharing System — Low Level Design
//  Patterns used: Singleton, Strategy, Decorator, OCP
// ============================================================

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


// ============================================================
//  ENUMS
// ============================================================

/**
 * Represents the lifecycle of a Ride.
 *
 * Transitions:
 *   REQUESTED → ACCEPTED (driver assigned)
 *             → CANCELLED (no driver found)
 *   ACCEPTED  → IN_PROGRESS (ride starts)
 *   IN_PROGRESS → COMPLETED (ride ends)
 *   Any state → FULL (shared ride reached max capacity)
 */
enum RideStatus {
    REQUESTED,    // Ride created, waiting for a driver
    ACCEPTED,     // Driver has been assigned
    IN_PROGRESS,  // Ride is currently ongoing
    COMPLETED,    // Ride has ended successfully
    CANCELLED,    // No driver found or ride was aborted
    FULL          // Shared ride: no more riders can join
}

/**
 * Vehicle tiers available on the platform.
 * Each tier has a different per-km fare rate (MICRO < MINI < PRIME).
 */
enum VehicleType {
    MICRO,   // Cheapest, smallest capacity
    MINI,    // Mid-tier
    PRIME    // Premium, most expensive
}


// ============================================================
//  LOCATION
// ============================================================

/**
 * Represents a geographic point using latitude and longitude.
 *
 * Note: Distance is calculated using Euclidean formula for simplicity.
 * In production, use the Haversine formula for real geo-distance.
 */
class Location {

    private final double latitude;
    private final double longitude;

    public Location(double latitude, double longitude) {
        this.latitude  = latitude;
        this.longitude = longitude;
    }

    public double getLatitude()  { return latitude; }
    public double getLongitude() { return longitude; }

    /**
     * Computes straight-line (Euclidean) distance between two locations.
     * Acceptable for LLD; replace with Haversine for real-world use.
     *
     * @param other The target location
     * @return Euclidean distance between this and other
     */
    public double calculateDistance(Location other) {
        double dLat = this.latitude  - other.latitude;
        double dLon = this.longitude - other.longitude;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}


// ============================================================
//  USER INTERFACE
// ============================================================

/**
 * Common abstraction for any participant on the platform (Driver or Rider).
 * Enforces that every user has a settable location.
 */
interface User {
    void setLocation(Location location);
}


// ============================================================
//  DRIVER
// ============================================================

/**
 * Represents a driver registered on the platform.
 *
 * A driver:
 * - Has a fixed VehicleType (cannot be changed post-registration)
 * - Tracks availability — set to false while on a ride
 * - Maintains a rating (simplified: direct set, not averaged)
 */
class Driver implements User {

    private final String      name;
    private final int         driverID;
    private final VehicleType vehicleType;

    private Location location;
    private boolean  isAvailable;
    private double   rating;

    public Driver(String name, VehicleType vehicleType, int driverID) {
        this.name        = name;
        this.vehicleType = vehicleType;
        this.driverID    = driverID;
        this.isAvailable = true;   // Available by default on registration
        this.rating      = 5.0;    // Start with full rating
    }

    /** Updates the driver's current GPS position. */
    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Updates driver rating.
     * TODO: Replace with running weighted average:
     *       newRating = (oldRating * rideCount + rating) / (rideCount + 1)
     */
    public void updateRating(double newRating) {
        this.rating = newRating;
    }

    /** Marks the driver as available or unavailable for new rides. */
    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String      getName()       { return name; }
    public int         getDriverID()   { return driverID; }
    public VehicleType getVehicleType(){ return vehicleType; }
    public Location    getLocation()   { return location; }
    public double      getRating()     { return rating; }
    public boolean     isAvailable()   { return isAvailable; }
}


// ============================================================
//  RIDER
// ============================================================

/**
 * Represents a passenger requesting a ride.
 *
 * The rider's location is updated to their dropoff point
 * once the ride is completed.
 */
class Rider implements User {

    private final String name;
    private final int    riderID;
    private Location     location;

    public Rider(String name, int riderID) {
        this.name    = name;
        this.riderID = riderID;
    }

    /** Updates the rider's current position (e.g., on ride completion). */
    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    public String   getName()    { return name; }
    public int      getRiderID() { return riderID; }
    public Location getLocation(){ return location; }
}


// ============================================================
//  PRICING STRATEGY  (Strategy Pattern)
// ============================================================

/**
 * Strategy interface for fare calculation.
 *
 * Allows the pricing model to be swapped at runtime without
 * modifying RideService or Ride — follows Open/Closed Principle.
 *
 * Known implementations:
 * - NormalPricingStrategy  (base fare + distance × rate)
 * - SurgePricingStrategy   (decorates any strategy with a multiplier)
 */
interface PricingStrategy {
    /**
     * Calculates the total fare for a trip.
     *
     * @param pickup      Ride's shared pickup location
     * @param dropoff     This specific rider's dropoff location
     * @param vehicleType The vehicle tier used for the ride
     * @return Calculated fare in currency units
     */
    double calculateFare(Location pickup, Location dropoff, VehicleType vehicleType);
}

/**
 * Standard pricing: a fixed base fare plus a per-km rate that varies by vehicle tier.
 *
 * Formula: fare = BASE_FARE + (distance × perKmRate)
 *
 * Rates:
 *   MICRO → ₹10/km
 *   MINI  → ₹15/km
 *   PRIME → ₹20/km
 */
class NormalPricingStrategy implements PricingStrategy {

    private static final double BASE_FARE         = 5.0;
    private static final double PER_KM_RATE_MICRO = 10.0;
    private static final double PER_KM_RATE_MINI  = 15.0;
    private static final double PER_KM_RATE_PRIME = 20.0;

    @Override
    public double calculateFare(Location pickup, Location dropoff, VehicleType vehicleType) {
        double distance  = pickup.calculateDistance(dropoff);
        double perKmRate;

        switch (vehicleType) {
            case MICRO: perKmRate = PER_KM_RATE_MICRO; break;
            case MINI:  perKmRate = PER_KM_RATE_MINI;  break;
            case PRIME: perKmRate = PER_KM_RATE_PRIME; break;
            default:    perKmRate = PER_KM_RATE_MINI;  // Fallback
        }

        return BASE_FARE + (distance * perKmRate);
    }
}

/**
 * Surge pricing decorator — wraps any PricingStrategy and applies a multiplier.
 *
 * This is a Decorator Pattern: it extends behavior of the wrapped strategy
 * without altering it. You can even chain surges if needed.
 *
 * Example:
 *   PricingStrategy surge = new SurgePricingStrategy(new NormalPricingStrategy(), 1.5);
 *   // Applies 1.5× surge on top of normal fares.
 */
class SurgePricingStrategy implements PricingStrategy {

    private final PricingStrategy basePricing;
    private final double          surgeMultiplier;

    /**
     * @param basePricing      The underlying strategy to wrap
     * @param surgeMultiplier  Multiplier applied to base fare (e.g., 1.5 = 50% surge)
     */
    public SurgePricingStrategy(PricingStrategy basePricing, double surgeMultiplier) {
        this.basePricing     = basePricing;
        this.surgeMultiplier = surgeMultiplier;
    }

    @Override
    public double calculateFare(Location pickup, Location dropoff, VehicleType vehicleType) {
        // Delegate to base strategy, then scale up
        return basePricing.calculateFare(pickup, dropoff, vehicleType) * surgeMultiplier;
    }
}


// ============================================================
//  DRIVER MATCHING STRATEGY  (Strategy Pattern)
// ============================================================

/**
 * Strategy interface for assigning a driver to a ride.
 *
 * Decouples the matching algorithm from RideService.
 * Alternative strategies could include: highest-rated, random, zone-based, etc.
 */
interface DriverMatchingStrategy {
    /**
     * Selects the best available driver for the given ride.
     *
     * @param drivers All registered drivers on the platform
     * @param ride    The ride needing a driver
     * @return Best matching Driver, or null if none available
     */
    Driver findDriver(List<Driver> drivers, Ride ride);
}

/**
 * Matches the ride to the nearest available driver with the correct vehicle type.
 *
 * Complexity: O(n) where n = number of registered drivers.
 */
class NearestDriverMatchingStrategy implements DriverMatchingStrategy {

    @Override
    public Driver findDriver(List<Driver> drivers, Ride ride) {
        Driver nearestDriver = null;
        double minDistance   = Double.MAX_VALUE;

        for (Driver driver : drivers) {
            // Only consider available drivers with the matching vehicle type
            if (!driver.isAvailable() || driver.getVehicleType() != ride.getVehicleType()) {
                continue;
            }

            double distance = driver.getLocation().calculateDistance(ride.getPickupLocation());
            if (distance < minDistance) {
                minDistance   = distance;
                nearestDriver = driver;
            }
        }

        return nearestDriver; // null if no suitable driver found
    }
}


// ============================================================
//  RIDE
// ============================================================

/**
 * Represents a single ride, which may be shared by multiple riders.
 *
 * Key responsibilities:
 * 1. Track all riders and their individual dropoff locations
 * 2. Recalculate per-rider fares proportionally whenever a new rider joins
 * 3. Manage ride lifecycle (status transitions)
 *
 * ── Shared Ride Fare Logic ──────────────────────────────────
 * Each rider's fare is proportional to their individual distance.
 *
 * Example: Rider A (distance=4) and Rider B (distance=1)
 *   rawA = BASE + 4 × rate = 45,  rawB = BASE + 1 × rate = 15
 *   totalFare = 60
 *   fareA = (45/60) × 60 = 45,  fareB = (15/60) × 60 = 15
 *
 * Result: Each rider effectively pays their own raw fare.
 * Sharing does NOT reduce individual cost here — for split discounts,
 * use totalFare = max(rawFares) and split proportionally.
 * ────────────────────────────────────────────────────────────
 */
class Ride {

    private final int            rideID;
    private final int            maxCapacity;
    private final Location       pickupLocation;   // Shared pickup point for all riders
    private final VehicleType    vehicleType;
    private final PricingStrategy pricingStrategy;

    private RideStatus            status;
    private Driver                driver;
    private double                totalFare;

    // Per-rider data — LinkedHashMap preserves insertion order
    private final List<Rider>             riders;
    private final Map<Rider, Location>    riderDropoffs;  // Rider → their own dropoff
    private final Map<Rider, Double>      riderFares;     // Rider → their computed fare share

    public Ride(int maxCapacity, Location pickupLocation, VehicleType vehicleType,
                int rideID, PricingStrategy pricingStrategy) {
        this.maxCapacity      = maxCapacity;
        this.pickupLocation   = pickupLocation;
        this.vehicleType      = vehicleType;
        this.rideID           = rideID;
        this.pricingStrategy  = pricingStrategy;
        this.riders           = new ArrayList<>();
        this.riderDropoffs    = new LinkedHashMap<>();
        this.riderFares       = new LinkedHashMap<>();
        this.totalFare        = 0.0;
        this.status           = RideStatus.REQUESTED;
        this.driver           = null;
    }

    /**
     * Adds a rider to this shared ride with their individual dropoff location.
     *
     * After adding, fares are recalculated for all current riders.
     * If the ride reaches maxCapacity, status is updated to FULL.
     *
     * @param rider   The rider to add
     * @param dropoff The rider's destination
     * @return true if successfully added, false if ride is already full
     */
    public boolean addRider(Rider rider, Location dropoff) {
        if (riders.size() >= maxCapacity) {
            return false;  // Ride is at capacity — cannot accept more riders
        }

        riders.add(rider);
        riderDropoffs.put(rider, dropoff);
        recalculateFares();  // Rebalance all fares with the new rider included

        if (riders.size() == maxCapacity) {
            status = RideStatus.FULL;  // Signal that no more riders can join
        }

        return true;
    }

    /**
     * Recalculates each rider's fare share every time the pool changes.
     *
     * Algorithm:
     *   1. Compute each rider's "raw fare" = what they'd pay alone (pickup → their dropoff)
     *   2. totalFare = sum of all raw fares
     *   3. Each rider's share = (their raw fare / totalFare) × totalFare = their raw fare
     *
     * This is proportional splitting — riders with longer trips pay more.
     * The math reduces to: each rider pays exactly their own raw fare.
     *
     * Note: For a discount model, cap totalFare at max(rawFares) + some buffer,
     * then split proportionally — everyone saves vs riding alone.
     */
    private void recalculateFares() {
        Map<Rider, Double> rawFares = new LinkedHashMap<>();
        double sumRawFares = 0.0;

        // Step 1: Each rider's hypothetical solo fare (pickup → their dropoff)
        for (Rider rider : riders) {
            double raw = pricingStrategy.calculateFare(
                pickupLocation, riderDropoffs.get(rider), vehicleType
            );
            rawFares.put(rider, raw);
            sumRawFares += raw;
        }

        // Step 2: Total is the sum of all individual raw fares
        this.totalFare = sumRawFares;

        // Step 3: Proportional share — short-distance riders pay less
        for (Rider rider : riders) {
            double proportion = rawFares.get(rider) / sumRawFares;
            riderFares.put(rider, proportion * totalFare);
            // Simplifies to: riderFares.put(rider, rawFares.get(rider));
            // Kept verbose to show the proportional intent clearly
        }
    }

    /**
     * Returns the computed fare for a specific rider.
     * Returns 0.0 if rider is not part of this ride.
     */
    public double getFareForRider(Rider rider) {
        return riderFares.getOrDefault(rider, 0.0);
    }

    /**
     * Simple equal-split fare across all riders.
     * Use this when all riders share a single flat fare instead of proportional distance.
     *
     * @return Per-rider equal share
     */
    public double splitFare() {
        if (riders.isEmpty()) return totalFare;
        return totalFare / riders.size();
    }

    /**
     * Assigns a driver to this ride and transitions status to ACCEPTED.
     * Called by RideService after a successful driver match.
     */
    public void setDriver(Driver driver) {
        this.driver = driver;
        this.status = RideStatus.ACCEPTED;
    }

    public void      setStatus(RideStatus status) { this.status = status; }
    public RideStatus getStatus()                  { return status; }
    public Driver    getDriver()                   { return driver; }
    public List<Rider> getRiders()                 { return riders; }
    public Location  getPickupLocation()           { return pickupLocation; }
    public VehicleType getVehicleType()            { return vehicleType; }
    public double    getTotalFare()                { return totalFare; }
    public int       getRideID()                   { return rideID; }

    /** Returns a specific rider's dropoff location. */
    public Location getDropoffLocation(Rider rider) {
        return riderDropoffs.get(rider);
    }
}


// ============================================================
//  RIDE SERVICE  (Singleton + Orchestrator)
// ============================================================

/**
 * Central service that manages the entire ride lifecycle.
 *
 * Responsibilities:
 * - Driver registration
 * - Ride request orchestration (create → match → confirm)
 * - Ride completion and driver release
 * - Strategy injection (pricing and matching are swappable at runtime)
 *
 * Singleton: Only one RideService instance exists per JVM.
 * Uses double-checked locking for thread-safe lazy initialization.
 *
 * ── Extending the system ────────────────────────────────────
 * To add surge pricing:
 *   rideService.setPricingStrategy(new SurgePricingStrategy(new NormalPricingStrategy(), 1.5));
 *
 * To add a new matching algorithm:
 *   rideService.setMatchingStrategy(new HighestRatedDriverStrategy());
 * ────────────────────────────────────────────────────────────
 */
class RideService {

    // volatile ensures the instance reference is always read from main memory,
    // not from a CPU cache — critical for correctness in double-checked locking
    private static volatile RideService instance;

    private final List<Driver> drivers;
    private final List<Ride>   rides;

    private PricingStrategy       pricingStrategy;    // Defaults to NormalPricingStrategy
    private DriverMatchingStrategy matchingStrategy;  // Defaults to NearestDriverMatchingStrategy

    /** Private constructor — enforces Singleton; sets sensible defaults. */
    private RideService() {
        this.drivers          = new ArrayList<>();
        this.rides            = new ArrayList<>();
        this.pricingStrategy  = new NormalPricingStrategy();
        this.matchingStrategy = new NearestDriverMatchingStrategy();
    }

    /**
     * Returns the global RideService instance.
     *
     * Thread-safe via double-checked locking:
     * - First null-check avoids unnecessary synchronization after init
     * - Synchronized block + second null-check prevents race conditions during init
     */
    public static RideService getInstance() {
        if (instance == null) {                          // First check (no lock)
            synchronized (RideService.class) {
                if (instance == null) {                  // Second check (with lock)
                    instance = new RideService();
                }
            }
        }
        return instance;
    }

    /**
     * Registers a driver with the platform.
     * Driver must have a location set before rides can be matched to them.
     */
    public void registerDriver(Driver driver) {
        drivers.add(driver);
    }

    /**
     * Replaces the active pricing strategy at runtime.
     * All subsequent rides will use the new strategy.
     * Existing rides retain their original pricing (set at creation time).
     */
    public void setPricingStrategy(PricingStrategy strategy) {
        this.pricingStrategy = strategy;
    }

    /**
     * Replaces the active driver matching strategy at runtime.
     * Next call to requestRide() will use the new matching algorithm.
     */
    public void setMatchingStrategy(DriverMatchingStrategy strategy) {
        this.matchingStrategy = strategy;
    }

    /**
     * Core method: creates a ride, adds the first rider, finds a driver, and confirms.
     *
     * Flow:
     *   1. Create a Ride with current pricing strategy
     *   2. Add rider with their dropoff
     *   3. Match a driver using current matching strategy
     *   4a. If driver found → assign, mark driver unavailable, persist ride, return ride
     *   4b. If no driver   → cancel ride, return null
     *
     * @param rider        The requesting rider
     * @param riderDropoff The rider's destination
     * @param maxCapacity  Max riders allowed (1 = private, >1 = shared)
     * @param pickup       Shared pickup location
     * @param type         Required vehicle type
     * @return Confirmed Ride, or null if no driver available
     */
    public Ride requestRide(Rider rider, Location riderDropoff,
                             int maxCapacity, Location pickup, VehicleType type) {
        int  rideId = rides.size() + 1;
        Ride ride   = new Ride(maxCapacity, pickup, type, rideId, pricingStrategy);

        // Register the first rider (fare is calculated immediately)
        ride.addRider(rider, riderDropoff);

        // Attempt to find a suitable driver
        Driver driver = matchingStrategy.findDriver(drivers, ride);

        if (driver != null) {
            ride.setDriver(driver);       // Status → ACCEPTED
            driver.setAvailable(false);   // Prevent double-booking
            rides.add(ride);
            System.out.println("Ride " + rideId + " accepted by driver: " + driver.getName());
            return ride;
        } else {
            // No driver found — cancel immediately
            ride.setStatus(RideStatus.CANCELLED);
            System.out.println("No drivers available. Ride " + rideId + " cancelled.");
            return null;
        }
    }

    /**
     * Completes a ride:
     * - Marks ride as COMPLETED
     * - Releases the driver back into the available pool
     * - Updates each rider's location to their respective dropoff
     * - Prints fare breakdown per rider and the total
     *
     * @param ride The ride to complete
     */
    public void completeRide(Ride ride) {
        ride.setStatus(RideStatus.COMPLETED);

        // Free the driver for future rides
        ride.getDriver().setAvailable(true);

        // Update each rider's location and print their individual fare
        for (Rider rider : ride.getRiders()) {
            rider.setLocation(ride.getDropoffLocation(rider));
            System.out.printf("Rider %-10s fare: %.2f%n",
                rider.getName(), ride.getFareForRider(rider));
        }

        System.out.printf("Ride %d completed. Total fare: %.2f%n",
            ride.getRideID(), ride.getTotalFare());
    }
}