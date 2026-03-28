import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Abstract base class representing a vehicle in the parking lot.
 * Concrete subclasses (Car, Bike, Truck) extend this class.
 */
public abstract class Vehicle{
    /**
     * Enum representing the supported vehicle types.
     */
    public enum VehicleType{
        CAR,
        TRUCK,
        BIKE

    }
    private String licensePlate;
    private VehicleType vehicleType;

    //constructor

    public Vehicle(String licensePlate, VehicleType vehicleType)
    {
        this.licensePlate=licensePlate; //assigning the license plate to the instance variable
        this.vehicleType=vehicleType; //assigning the vehicle type to the instance variable
    }
    //Getters
    public String getLicensePlate()
    {
        return licensePlate;
    }
    public VehicleType getVehicleType()
    {
        return vehicleType;
    }




}

/** Concrete Vehicle subclass representing a car. */
class Car extends Vehicle
{
    public Car( String LicensePlate)
    {
        super(LicensePlate, VehicleType.CAR);
    }
}

/** Concrete Vehicle subclass representing a bike. */
class Bike extends Vehicle
{
    public Bike( String LicensePlate)
    {
        super(LicensePlate, VehicleType.BIKE);
    }
}

/** Concrete Vehicle subclass representing a truck. */
class Truck extends Vehicle
{
    public Truck( String LicensePlate)
    {
        super(LicensePlate, VehicleType.TRUCK);
    }
}

/**
 * Factory class for creating Vehicle instances.
 * Encapsulates the instantiation logic so callers don't depend on concrete types.
 */
public class VehicleFactory
{
    /**
     * Creates and returns a Vehicle of the specified type.
     *
     * @param type         vehicle type string ("CAR", "BIKE", or "TRUCK"), case-insensitive
     * @param licensePlate license plate for the vehicle
     * @return a new Vehicle instance of the matching subclass
     * @throws IllegalArgumentException if type or licensePlate is null, or type is unrecognized
     */
    public static Vehicle createVehicle(String type, String licensePlate)
    {
        if(type==null || licensePlate==null){
        throw new IllegalArgumentException("Type or license plate cannot be null");
        }

        switch (type.toUpperCase())
        {
            case "CAR":
                return new Car(licensePlate);
            case "BIKE":
                return new Bike(licensePlate);
            case "TRUCK":
                return new Truck(licensePlate);
            default:
                throw new IllegalArgumentException("Invalid vehicle type: " + type);
        }


    }
}

/**
 * Represents a single parking spot in the lot.
 * Each spot is typed to accept only a specific VehicleType.
 */
public class ParkingSpot
{
    private int spotId;
    private Vehicle.VehicleType spotType;
    private boolean isAvailable;

    private Vehicle currentVehicle;

    //constructor
    public ParkingSpot(int spotId, Vehicle.VehicleType spotType)
    {
        this.spotId=spotId;
        this.spotType=spotType;
        isAvailable=true;      // spot starts empty
        currentVehicle=null;
    }

    /** Returns true if the spot currently has no vehicle assigned. */
    public boolean isAvailable()
    {
        return this.isAvialable;
    }

    /**
     * Assigns a vehicle to this spot.
     *
     * @param v the vehicle to park
     * @return true if assignment succeeded; false if the spot is taken,
     *         the vehicle is null, or the vehicle type doesn't match the spot type
     */
    public boolean assignVehicle(Vehicle v)
    {

        if(isAvailable==false|| v==null || v.getVehicleType()!=spotType)
        {
            return false;
        }
        this.currentVehicle=v;
        this.isAvailable=false;
        return true;

    }

    /** Clears the current vehicle from this spot, marking it available again. */
    public void removeVehicle()
    {
        this.currentVehicle=null;
        this.isAvailable=true;
    }

    public int getSpotId()
    {
        return this.spotId;
    }

    public Vehicle.VehicleType getSpotType()
    {
        return spotType;
    }

    public Vehicle getCurrentVehicle()
    {
        return this.currentVehicle;
    }


}

/**
 * Represents a single floor in the parking lot, containing a collection of ParkingSpots.
 */
public class ParkingFloor{
    private int floorID;
    private List<ParkingSpot> spots;

    public ParkingFloor(int floorId)
    {
        this.floorID=floorId;
        this.spots=new ArrayList<>();


    }

    /**
     * Adds a spot to this floor. Null spots are silently ignored.
     *
     * @param spot the ParkingSpot to add
     */
    public void addSpot(ParkingSpot spot)
    {
        if(spot!=null)
        {
            spots.add(spot);
        }
    }

    /**
     * Finds and returns the first available spot on this floor that matches the given vehicle type.
     *
     * @param type the VehicleType to search for
     * @return an available ParkingSpot, or null if none found
     */
    public ParkingSpot getAvParkingSpot(Vehicle.VehicleType type)
    {
        for(ParkingSpot spot:this.spots)
        {
            if(spot.isAvailable()==true && spot.getSpotType()==type)
            {
                return spot;
            }
        }
        return null;
    }

    public int getFloorID()
    {
        return floorID;
    }

    public List<ParkingSpot> getSpots()
    {
        return spots;
    }


}

/**
 * Singleton representing the entire parking lot.
 * Manages multiple ParkingFloors and provides lot-wide spot lookup.
 *
 * Uses double-checked locking for thread-safe lazy initialization.
 */
public class  ParkingLot
{
    private static volatile ParkingLot instance;

    /**
     * Returns the single ParkingLot instance, creating it on first call.
     * Thread-safe via double-checked locking with a volatile field.
     */
    public static ParkingLot getInstance()
    {
        if(instance==null)
        {
            syncronized(ParkingLot.class)
            {
                if(instance==null)
                {
                    instance=new ParkingLot();
                }
            }
        }
        return instance;
    }

    private List<ParkingFloor> floors;

    /** Private constructor — use getInstance() to obtain the singleton. */
    private ParkingLot()
    {
        this.floors=new ArrayList<>();
    }

    /**
     * Adds a floor to the parking lot. Null floors are silently ignored.
     *
     * @param floor the ParkingFloor to add
     */
    public void addFloor(ParkingFloor floor)
    {
        if(floor!=null){
        floors.add(floor);
        }
    }


    /**
     * Searches all floors in order and returns the first available spot
     * that matches the requested vehicle type.
     *
     * @param type the VehicleType to find a spot for
     * @return an available ParkingSpot, or null if the lot is full for that type
     */
    public ParkingSpot getParkingSpot(Vehicle.VehicleType type)
    {
        for( ParkingFloor floor: floors)
        {
            ParkingSpot spot=floor.getAvParkingSpot(type);
            if(spot!=null)
            {
                return spot;
            }

        }
        return null;
    }

    public List<ParkingFloor> getFloors()
    {
        return floors;
    }



}



/**
 * Strategy interface for pricing.
 * Implementations define the per-hour rate for a specific vehicle type.
 */
public interface PricingStrategy
{
    /**
     * Calculates the total parking cost.
     *
     * @param hours number of hours parked
     * @return total cost in rupees
     */
    double calcCost(long hours);
}


/** PricingStrategy for bikes — ₹30/hour. */
public class BikePricing implements PricingStrategy
{
    private static final int RATE=30;
    @Override

    public double calcCost(long hours)
    {
        return hours*RATE;
    }


}

/** PricingStrategy for cars — ₹50/hour. */
public class CarPricing implements PricingStrategy
{
    private static final int RATE=50;
    @Override

    public double calcCost(long hours)
    {
        return hours*RATE;
    }


}

/** PricingStrategy for trucks — ₹80/hour. */
public class TruckPricing implements PricingStrategy
{
    private static final int RATE=80;
    @Override

    public double calcCost(long hours)
    {
        return hours*RATE;
    }


}

/**
 * Represents a parking ticket issued when a vehicle enters the lot.
 * Built using the Builder pattern to handle the mix of required and optional fields.
 */
public class ParkingTicket
{
    private String ticketId;
    private Vehicle vehicle;
    private ParkingSpot spot;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;       // set when the vehicle exits
    private PricingStrategy pricingStrategy;
    private double amountPaid;            // set after payment

    /** Private constructor — use Builder to create instances. */
    private ParkingTicket(Builder builder)
    {
          this.ticketId = builder.ticketId;
        this.vehicle = builder.vehicle;
        this.spot = builder.spot;
        this.entryTime = builder.entryTime;
        this.exitTime = builder.exitTime;
        this.pricingStrategy = builder.pricingStrategy;
        this.amountPaid = builder.amountPaid;
    }

    public String getTicketId() { return ticketId; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSpot getSpot() { return spot; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public PricingStrategy getPricingStrategy() { return pricingStrategy; }
    public double getAmountPaid() { return amountPaid; }


    /**
     * Builder for ParkingTicket.
     * Required fields are set via the constructor; optional fields (exitTime, amountPaid)
     * are set via fluent setters before calling build().
     */
    public static class Builder
    {
        // Required fields
        private String ticketId;
        private Vehicle vehicle;
        private ParkingSpot spot;
        private LocalDateTime entryTime;
        private PricingStrategy pricingStrategy;

        // Optional fields
        private LocalDateTime exitTime;
        private double amountPaid;


          public Builder(String ticketId,
                       Vehicle vehicle,
                       ParkingSpot spot,
                       LocalDateTime entryTime,
                       PricingStrategy pricingStrategy) {

            if (ticketId == null || vehicle == null || spot == null
                || entryTime == null || pricingStrategy == null) {
                throw new IllegalArgumentException("Required fields cannot be null");
            }

            this.ticketId = ticketId;
            this.vehicle = vehicle;
            this.spot = spot;
            this.entryTime = entryTime;
            this.pricingStrategy = pricingStrategy;
                       }

            // Optional setters — return this for method chaining

            public Builder setExitTime(LocalDateTime exitTime) {
            this.exitTime = exitTime;
            return this;
        }
            public Builder setAmountPaid(double amountPaid) {
            this.amountPaid = amountPaid;
            return this;
        }

        /** Constructs and returns the ParkingTicket. */
        public ParkingTicket build() {
            return new ParkingTicket(this);
        }





    }


}

/**
 * Strategy interface for payment processing.
 * Implementations handle a specific payment method (cash, card, UPI).
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


