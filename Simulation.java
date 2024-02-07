import java.util.*;
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

public class Simulation {
    private static volatile boolean processing = true;
    private static int totalCustomers;
    private static AtomicInteger customersArrived = new AtomicInteger(0);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the number of cash counters: ");
        int numCashCounters = scanner.nextInt();

        System.out.print("Enter the customer arrival rate (seconds): ");
        int customerArrivalRate = scanner.nextInt();

        System.out.print("Enter the total number of customers: ");
        totalCustomers = scanner.nextInt();

        CashCounter[] counters = new CashCounter[numCashCounters];
        for (int i = 0; i < numCashCounters; i++) {
            counters[i] = new CashCounter(i + 1);
        }

        List<Customer> expressCustomers = new ArrayList<>();
        List<Customer> regularCustomers = new ArrayList<>();

        double currentTime = System.currentTimeMillis();

        for (int i = 0; i < totalCustomers; i++) {
            System.out.print("Enter the number of items for Customer " + (i + 1) + ": ");
            int itemsInCart = scanner.nextInt();

            System.out.print("Enter customer type (1 for Regular, 2 for Express): ");
            int customerType = scanner.nextInt();

            Customer customer;
            if (customerType == 1) {
                customer = new RegularCustomer(currentTime, itemsInCart);
                regularCustomers.add(customer);
            } else if (customerType == 2) {
                customer = new ExpressCustomer(currentTime, itemsInCart);
                expressCustomers.add(customer);
            } else {
                System.out.println("Invalid customer type. Assuming Regular Customer.");
                customer = new RegularCustomer(currentTime, itemsInCart);
                regularCustomers.add(customer);
            }

            currentTime += customerArrivalRate * 1000;
        }

        scanner.close();

        Thread arrivalExpressThread = new Thread(() -> printExpressArrivals(counters, expressCustomers, customerArrivalRate));
        arrivalExpressThread.start();

        Thread arrivalRegularThread = new Thread(() -> printRegularArrivals(counters, regularCustomers, customerArrivalRate));
        arrivalRegularThread.start();

        Thread[] threads = new Thread[numCashCounters];
        for (int i = 0; i < numCashCounters; i++) {
            final int counterIndex = i;
            threads[i] = new Thread(() -> processCustomers(counters[counterIndex], counters));
            threads[i].start();
        }

        try {
            arrivalExpressThread.join();
            arrivalRegularThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        processing = false;

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\nSimulation Reporting:\n");
        for (CashCounter counter : counters) {
            double avgWaitingTime = counter.getTotalCustomersProcessed() != 0 ? (double) counter.getTotalWaitingTime() / counter.getTotalCustomersProcessed() : 0;
            System.out.printf("Counter %d: (%d) Customers Processed, Average Waiting Time - %.1f seconds%n",
                    counter.getCounterNumber(), counter.getTotalCustomersProcessed(), avgWaitingTime);
        }
    }


    private static void processCustomers(CashCounter counter, CashCounter[] counters) {
        int pullingRate = 100;

        while (true) {
            if (!counter.customerQueue.isEmpty()) {
                Customer customer = counter.customerQueue.poll();
                try {
                    Thread.sleep(customer.getProcessingTime() * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                counter.processCustomer(customer);
            } else {
                if (!processing && allQueuesEmpty(counters)) {
                    break;
                }

                try {
                    Thread.sleep(pullingRate);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean allQueuesEmpty(CashCounter[] counters) {
        for (CashCounter counter : counters) {
            if (!counter.customerQueue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void printExpressArrivals(CashCounter[] counters, List<Customer> expressCustomers, int customerArrivalRate) {
        long pauseDuration = customerArrivalRate * 1000;
    
        // Print express arrivals
        for (Customer expressCustomer : expressCustomers) {
            CashCounter expressCounter = counters[0];
            synchronized (expressCounter) {
                expressCounter.customerQueue.add(expressCustomer);
                customersArrived.incrementAndGet();
                System.out.println("\nExpress Customer " + expressCustomer.getCustomerID() + " arrived at Counter " + expressCounter.getCounterNumber());
            }

            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void printRegularArrivals(CashCounter[] counters, List<Customer> regularCustomers, int customerArrivalRate) {
        long pauseDuration = customerArrivalRate * 1000;
    
        for (Customer regularCustomer : regularCustomers) {
            CashCounter[] nonExpressCounters = Arrays.copyOfRange(counters, 1, counters.length);
            Arrays.sort(nonExpressCounters, Comparator.comparingInt(c -> c.customerQueue.size()));
            CashCounter shortestQueueCounter = nonExpressCounters[0];
             
            synchronized (shortestQueueCounter) {
                shortestQueueCounter.customerQueue.add(regularCustomer);
                customersArrived.incrementAndGet();
                System.out.println("\nRegular Customer " + regularCustomer.getCustomerID() + " arrived at Counter " + shortestQueueCounter.getCounterNumber());
            }

            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }           
}