import java.util.*;

class CashCounter {
    protected Queue<Customer> customerQueue = new ArrayDeque<>();
    private int totalCustomersProcessed = 0;
    private int totalWaitingTime = 0;
    private int counterNumber;

    public CashCounter(int counterNumber) {
        this.counterNumber = counterNumber;
    }

    public synchronized void processCustomer(Customer customer) {
        try {
            Thread.sleep(customer.getProcessingTime() * 1000);
            totalCustomersProcessed++;
            totalWaitingTime += customer.getProcessingTime();
            System.out.println("\n" + (customer instanceof ExpressCustomer ? "Express" : "Regular") +
                    " Customer " + customer.getCustomerID() + " processed at Counter " + counterNumber);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getTotalCustomersProcessed() {
        return totalCustomersProcessed;
    }

    public int getTotalWaitingTime() {
        return totalWaitingTime;
    }

    public int getCounterNumber() {
        return counterNumber;
    }

    public synchronized int getQueueSize() {
        return customerQueue.size();
    }
}

class Customer {
    private static int customerCount = 0;
    private int customerID;
    private double arrivalTime;
    private int processingTime;
    private int itemsInCart;

    public Customer(double arrivalTime, int itemsInCart) {
        this.customerID = ++customerCount;
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

public class Simulation {
    private static volatile boolean processing = true;
    private static int totalCustomers;
    private static int customersArrived = 0;

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

            currentTime += customerArrivalRate * 1000; // Update the current time for the next customer
        }

        scanner.close();

        Thread arrivalThread = new Thread(() -> printArrivals(counters, expressCustomers, regularCustomers, customerArrivalRate));
        arrivalThread.start();

        Thread[] threads = new Thread[numCashCounters];
        for (int i = 0; i < numCashCounters; i++) {
            final int counterIndex = i;
            threads[i] = new Thread(() -> processCustomers(counters[counterIndex], counters));
            threads[i].start();
        }

        try {
            arrivalThread.join();
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
            double avgWaitingTime = counter.getTotalCustomersProcessed() != 0 ? counter.getTotalWaitingTime() / counter.getTotalCustomersProcessed() : 0;
            System.out.printf("Counter %d: (%d) Customers Processed, Average Waiting Time - %.1f seconds%n",
                    counter.getCounterNumber(), counter.getTotalCustomersProcessed(), avgWaitingTime);
        }
    }

    private static void processCustomers(CashCounter counter, CashCounter[] counters) {
        int pullingRate = 100;

        while (true) {
            synchronized (counter) {
                if (!counter.customerQueue.isEmpty()) {
                    Customer customer = counter.customerQueue.poll();
                    counter.processCustomer(customer);
                    counter.notify();  // Notify after processing a customer
                } else {
                    if (!processing && allQueuesEmpty(counters)) {
                        break;
                    }
                    try {
                        counter.wait(pullingRate);  // Wait for a short duration
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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


    private static void printArrivals(CashCounter[] counters, List<Customer> expressCustomers, List<Customer> regularCustomers, int customerArrivalRate) {
        long pauseDuration = customerArrivalRate * 1000;

        // Start express customer threads
        for (Customer expressCustomer : expressCustomers) {
            new Thread(() -> {
                CashCounter expressCounter = counters[0]; // Counter 0 is reserved for express customers
                synchronized (expressCounter) {
                    expressCounter.customerQueue.add(expressCustomer);
                    customersArrived++;
                    System.out.println("\nExpress Customer " + expressCustomer.getCustomerID() + " arrived at Counter " + expressCounter.getCounterNumber());
                }
            }).start();
        }

        // Regular customers
        for (Customer regularCustomer : regularCustomers) {
            // Exclude Counter 0 (express counter) from the sort
            CashCounter[] nonExpressCounters = Arrays.copyOfRange(counters, 1, counters.length);
            Arrays.sort(nonExpressCounters, Comparator.comparingInt(c -> c.customerQueue.size()));

            CashCounter shortestQueueCounter = nonExpressCounters[0]; // The first non-express counter is now the shortest

            // Synchronize only the selected counter for more fine-grained control
            synchronized (shortestQueueCounter) {
                shortestQueueCounter.customerQueue.add(regularCustomer);
                customersArrived++;
                System.out.println("\nRegular Customer " + regularCustomer.getCustomerID() + " arrived at Counter " + shortestQueueCounter.getCounterNumber());
            }

            // Pause for the specified arrival rate before the next regular customer
            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
