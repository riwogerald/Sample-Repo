import java.util.*;
import java.io.*;

//class declaration
public class EveSim
{
    //declaration of simulation constants.
    final static int QUEUE_LIMIT = 100;
    final static int BUSY = 1;
    final static int IDLE = 0;
    final static double INFINITY = 1.0e30;
    final static double LARGE_TIME = 1.0e29;
    
    private static int next_event_type, num_custs_delayed, num_delays_required, num_events, num_in_q, server_status;
    private static double area_num_in_q, area_server_status, mean_interarrival, mean_service, time, time_last_event, total_of_delays;
    private static double[] time_arrival = new double[QUEUE_LIMIT + 1];
    private static double[] time_next_event = new double[3]; // 0 unused, 1=arrival, 2=departure
    private static PrintWriter output; 
    
    // Initialization function.
    private static void initialize()
    {
        // Initialize time
        time = 0.0;
        
        // initialize state variables  
        server_status = IDLE;
        num_in_q = 0;
        time_last_event = 0.0;

        // initialize statistical counters
        num_custs_delayed = 0;
        total_of_delays = 0.0;
        area_num_in_q = 0.0;
        area_server_status = 0.0;
        
        // Initialize event list. since no customers are present, the departure (service completion) 
        // event is eliminated from consideration.
        time_next_event[1] = time + expon(mean_interarrival);
        time_next_event[2] = INFINITY;
    }
    
    // Timing function 
    private static void timing()
    {
        int i;
        double min_time_next_event = LARGE_TIME;

        next_event_type = 0;

        // Determine the event type of the next event to occur.
        for (i = 1; i <= num_events; ++i) 
        {
            if (time_next_event[i] < min_time_next_event) 
            {
                min_time_next_event = time_next_event[i];
                next_event_type = i;
            }   
        }

        // Check to see whether the event list is empty
        if (next_event_type == 0) 
        {
            // The event list is empty, so stop the simulation.
            if (output != null) {
                output.printf("\n%s%f","Event list empty at time: ", time);
                output.close();
            }
            System.err.println("Event list empty at time: " + time);
            System.exit(1); 
        }

        // The event list is not empty, so advance the simulation clock.
        time = min_time_next_event;
    }

    // Arrival event function
    private static void arrive()
    {
        double delay;

        // Schedule next arrival.
        time_next_event[1] = time + expon(mean_interarrival);

        // Check to see whether server is busy.
        if (server_status == BUSY) 
        {
            // Server is busy, so increment number of customers in queue.
            ++num_in_q;

            // Check to see whether an overflow condition exists
            if (num_in_q > QUEUE_LIMIT) 
            {
                // The queue has overflowed, so stop the simulation
                if (output != null) {
                    output.printf("\n%s","Overflow of the array time_arrival at");
                    output.printf("%s%f", " time: ", time);
                    output.close();
                }
                System.err.println("Queue overflow at time: " + time);
                System.exit(2);
            }

            // There is still room in the queue, so store the time of arrival of the arriving customer at the new end of time_arrival.
            time_arrival[num_in_q] = time;
        }
        else 
        {
            //Server is idle, so arriving customer has a delay of zero.(The following two statements are 
            // for program clarity and do not affect the results of the simulation)
            delay = 0.0;
            total_of_delays += delay;

            // Increment the number of customers delayed, and make server busy.
            ++num_custs_delayed;
            server_status = BUSY;

            // Schedule a departure (service completion).
            time_next_event[2] = time + expon(mean_service);
        }
    }

    // Departure event function.
    private static void depart()
    {
        int i;
        double delay;

        // Check to see whether the queue is empty.
        if (num_in_q == 0) 
        {
            // The queue is empty so make the server idle and eliminate the departure (service completion) event from consideration.
            server_status = IDLE;
            time_next_event[2] = INFINITY;
        }
        else
        {
            // The queue is nonempty, so decrement the number of customers in queue.
            --num_in_q;

            // Compute the delay of the customer who is beginning service and update the total delay accumulator.
            delay = time - time_arrival[1];
            total_of_delays += delay;

            // Increment the number of customers delayed, and schedule departure
            ++num_custs_delayed;
            time_next_event[2] = time + expon(mean_service);

            // Move each customer in queue (if any) up one place.
            for (i = 1; i <= num_in_q; ++i) 
            {
                time_arrival[i] = time_arrival[i + 1];
            }
        }
    }

    // Update area accumulators for time-average statistics.
    private static void update_time_avg_stats()
    {
        double time_since_last_event;

        // compute time since last event, and update last-event-time marker.
        time_since_last_event = time - time_last_event;
        time_last_event = time;

        // Update area under number-in-queue function.
        area_num_in_q += num_in_q * time_since_last_event;

        // Update area under server-busy indicator function.
        area_server_status += server_status * time_since_last_event;
    }

    // Exponential variate generation function
    private static double expon(double mean)
    {
        double u;

        // Generate a U(0,1) random variate.
        u = Math.random();

        // Return an exponential random variate with mean "mean".
        return -mean * Math.log(u);
    }
    
    // Improved input parsing method
    private static void readInputParameters() throws IOException {
        FileReader in = null;
        Scanner read = null;
        
        try {
            in = new FileReader("infile.txt");
            read = new Scanner(in);
            
            if (read.hasNextDouble()) {
                mean_interarrival = read.nextDouble();
            } else {
                throw new IOException("Expected mean interarrival time");
            }
            
            if (read.hasNextDouble()) {
                mean_service = read.nextDouble();
            } else {
                throw new IOException("Expected mean service time");
            }
            
            if (read.hasNextInt()) {
                num_delays_required = read.nextInt();
            } else {
                throw new IOException("Expected number of customers");
            }
            
            // Validate input parameters
            if (mean_interarrival <= 0 || mean_service <= 0 || num_delays_required <= 0) {
                throw new IllegalArgumentException("All parameters must be positive");
            }
            
        } finally {
            if (read != null) {
                read.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
    
    // Main Method
    public static void main(String[] args)
    {
        try {
            // Read input parameters from input file
            readInputParameters();
            
            // specify the number of events for the timing function.
            num_events = 2;

            // creating the output file
            output = new PrintWriter("outfile.txt");

            // write report heading and input parameters into output files.
            output.printf("%s\n\n", "SINGLE-SERVER QUEUING SYSTEM");
            output.printf("%s%.1f%s\n", "Mean interarrival time: ", mean_interarrival, " minutes");
            output.printf("%s%.1f%s\n", "Mean service time: ", mean_service, " minutes");
            output.printf("%s%d\n", "Number of customers: ", num_delays_required);
            
            // Initialize the simulation.
            initialize();

            // Run the simulation while more delays are still needed.
            while (num_custs_delayed < num_delays_required)
            {
                // Determine the next event.
                timing();

                // Update time-average statistical accumulators.
                update_time_avg_stats();

                // Invoke the appropriate event function.
                switch (next_event_type)
                {
                    case 1:
                        arrive();
                        break;
                    case 2:
                        depart();
                        break;
                    default:
                        System.err.println("Unknown event type: " + next_event_type);
                        break;
                }
            }
            
            // generate reports
            output.printf("\n%s%.5f%s\n", "Average delay in queue: ", total_of_delays / num_custs_delayed, " minutes");
            output.printf("%s%.5f\n", "Average number in queue: ", area_num_in_q / time);
            output.printf("%s%.5f\n", "Server utilization: ", area_server_status / time);
            output.printf("%s%.5f", "Time simulation ended: ", time);

        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input parameters: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Ensure output file is closed
            if (output != null) {
                output.close();
            }
        }
    }
}