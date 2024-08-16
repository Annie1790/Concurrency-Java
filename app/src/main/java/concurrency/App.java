package concurrency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;
import java.util.stream.LongStream;

public class App {
    public static void main(String[] args) throws InterruptedException, IOException {
        //part 1
        Thread thread = new Thread(printResult);
        thread.start();
        System.out.println("Thread has started");
        thread.join();
        System.out.println("Thread has been completed");

        MyExecutorService.run5ThreadSimultaneously();

        System.out.println(String.format("Count probable primes in parallel: %s", countProbableprimesParallel(10000)));
        System.out.println(String.format("Count probable primes in simultaneously: %s", countProbableprimesNotParallel(10000)));

        ExecutorService pool = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 10_000; i++) {
            Counter counterA = new Counter();

            CompletableFuture<Void> increment1 = CompletableFuture.runAsync(counterA::increment, pool);
            CompletableFuture<Void> increment2 = CompletableFuture.runAsync(counterA::increment, pool);

            CompletableFuture<Void> all = CompletableFuture.<Integer>allOf(increment1, increment2);
            all.thenApply((v) -> {
                if (counterA.get() != 2) {
                    System.out.println("Incorrect counter value: " + Integer.toString(counterA.get()));
                }
                return null;
            });
        }
        waitForThreadpoolShutdown(pool);
    }

    private static final Runnable printResult = () -> {
        try {
            String result = getWebpage("https://dummyjson.com/c/3029-d29f-4014-9fb4");
            System.out.println(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    private static void waitForThreadpoolShutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdownNow();
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("Pool did not complete within 10 seconds");
            pool.shutdownNow();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            }
        }
    }

    public static class Counter {
        private int val = 0;

        public synchronized void increment() {
            val += 1;
        }

        public int get() {
            return val;
        }
    }
    private static String getWebpage(String urlString) throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(1000);

        int response = connection.getResponseCode();
        System.out.println(response);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();

        return inputLine;
    }

    //part 3
    static long countProbableprimesParallel(long n) {
        return LongStream.rangeClosed(2, n)
                .mapToObj(BigInteger::valueOf)
                .parallel()
                .filter(i -> i.isProbablePrime(50))
                .count();
    }
    //part 3
    static long countProbableprimesNotParallel(long n) {
        return LongStream.rangeClosed(2, n)
                .mapToObj(BigInteger::valueOf)
                .filter(i -> i.isProbablePrime(50))
                .count();
    }

    private static class MyExecutorService {
        //Part 2
        static Executor executor = Executors.newFixedThreadPool(5);

        private static void run5ThreadSimultaneously() throws IOException, InterruptedException {
            for (int i = 1; i <= 5; i++) {
                executor.execute(printResult);
                System.out.println(String.format("Thread no %s started", i));
            }
            System.out.println("Thread started 5 times");
        }
    }
}
