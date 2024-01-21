import java.util.Queue;
import java.util.Scanner;
import java.util.ArrayDeque;

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
            System.out.println("\nCustomer " + customer.getCustomerID() + " processed at Counter " + counterNumber);
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
}

class Customer {
    private static int customerCount = 0;
    private int customerID;
    private double arrivalTime;
    private int processingTime;

    public Customer(double arrivalTime, int processingTime) {
        this.customerID = ++customerCount;
        this.arrivalTime = arrivalTime;
        this.processingTime = processingTime;
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
}

public class Simulation {

    private static volatile boolean processing = true;
    private static int customersArrived = 0;
    private static int totalCustomers;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the number of cash counters: ");
        int numCashCounters = scanner.nextInt();

        System.out.print("Enter the customer arrival rate (seconds): ");
        int customerArrivalRate = scanner.nextInt();

        System.out.print("Enter the customer processing time (seconds): ");
        int customerProcessingTime = scanner.nextInt();

        System.out.print("Enter the total number of customers: ");
        totalCustomers = scanner.nextInt();

        scanner.close();

        CashCounter[] counters = new CashCounter[numCashCounters];
        for (int i = 0; i < numCashCounters; i++) {
            counters[i] = new CashCounter(i + 1);
        }

        Thread arrivalThread = new Thread(() -> printArrivals(counters, customerArrivalRate, customerProcessingTime));
        arrivalThread.start();

        Thread[] threads = new Thread[numCashCounters];
        for (int i = 0; i < numCashCounters; i++) {
            final int counterIndex = i;
            threads[i] = new Thread(() -> processCustomers(counters[counterIndex]));
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
            double avgWaitingTime = counter.getTotalWaitingTime() / counter.getTotalCustomersProcessed();
            System.out.printf("Counter %d: (%d) Customers Processed, Average Waiting Time - %.1f seconds%n",
                    counter.getCounterNumber(), counter.getTotalCustomersProcessed(), avgWaitingTime);
        }
    }

    private static void processCustomers(CashCounter counter) {
        int pullingRate = 1000;

        while (processing || !counter.customerQueue.isEmpty()) {
            synchronized (counter) {
                if (!counter.customerQueue.isEmpty()) {
                    Customer customer = counter.customerQueue.poll();
                    counter.processCustomer(customer);
                } else {
                    try {
                        Thread.sleep(pullingRate);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static void printArrivals(CashCounter[] counters, int customerArrivalRate, int customerProcessingTime) {
        long pauseDuration = customerArrivalRate * 1000;

        while (customersArrived < totalCustomers) {
            for (CashCounter counter : counters) {
                if (customersArrived < totalCustomers) {
                    Customer customer = new Customer(System.currentTimeMillis(), customerProcessingTime);
                    counter.customerQueue.add(customer);
                    customersArrived++;
                    System.out.println("\nCustomer " + customer.getCustomerID() + " arrived at Counter " + counter.getCounterNumber());
                }
            }

            try {
                Thread.sleep(pauseDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
