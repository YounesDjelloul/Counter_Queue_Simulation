import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class CashCounter {
    protected Queue<Customer> customerQueue = new ArrayDeque<>();
    private AtomicInteger totalCustomersProcessed = new AtomicInteger(0);
    private AtomicInteger totalWaitingTime = new AtomicInteger(0);
    private int counterNumber;

    public CashCounter(int counterNumber) {
        this.counterNumber = counterNumber;
    }

    public synchronized void processCustomer(Customer customer) {
        totalCustomersProcessed.incrementAndGet();
        totalWaitingTime.addAndGet(customer.getProcessingTime());
        System.out.println("\n" + (customer instanceof ExpressCustomer ? "Express" : "Regular") +
                " Customer " + customer.getCustomerID() + " processed at Counter " + counterNumber);
    }

    public int getTotalCustomersProcessed() {
        return totalCustomersProcessed.get();
    }

    public int getTotalWaitingTime() {
        return totalWaitingTime.get();
    }

    public int getCounterNumber() {
        return counterNumber;
    }

    public synchronized int getQueueSize() {
        return customerQueue.size();
    }
}