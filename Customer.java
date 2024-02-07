import java.util.concurrent.atomic.AtomicInteger;

class Customer {
    private static AtomicInteger customerCount = new AtomicInteger(0);
    private int customerID;
    private double arrivalTime;
    private int processingTime;
    private int itemsInCart;

    public Customer(double arrivalTime, int itemsInCart) {
        this.customerID = customerCount.incrementAndGet();
        this.arrivalTime = arrivalTime;
        this.itemsInCart = itemsInCart;
        this.processingTime = calculateProcessingTime();
    }

    public int getCustomerID() {
        return customerID;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public int getItemsInCart() {
        return itemsInCart;
    }

    private int calculateProcessingTime() {
        return itemsInCart * 2;
    }
}

class RegularCustomer extends Customer {
    public RegularCustomer(double arrivalTime, int itemsInCart) {
        super(arrivalTime, itemsInCart);
    }
}

class ExpressCustomer extends Customer {
    public ExpressCustomer(double arrivalTime, int itemsInCart) {
        super(arrivalTime, itemsInCart);
    }
}