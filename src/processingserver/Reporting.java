package processingserver;

/**
 *
 * @author nicolaus
 */
public class Reporting {
    
    private static long timeStart;
    private static long timeStop;
    private static String prevTask;
    
    public static void startTask(String task) {
        System.out.println(task + "...");
        prevTask = task;
        timeStart = System.nanoTime();
    }
    
    public static void endTask() {
        timeStop = System.nanoTime();
        System.out.println(String.format("%1$-50s %2$6.2f sec", prevTask + ":", (timeStop - timeStart)/1e9));
    }
    
    public static void endTaskAndStartAnother(String task) {
        timeStop = System.nanoTime();
        System.out.println(String.format("%1$-50s %2$6.2f sec", prevTask + ":", (timeStop - timeStart)/1e9));
        System.out.println(task + "...");
        prevTask = task;
        timeStart = System.nanoTime();
    }
    
    public static void print(String str) {
        System.out.println(str);
    }
    
}
