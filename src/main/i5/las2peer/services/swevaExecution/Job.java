package i5.las2peer.services.swevaExecution;


import i5.las2peer.restMapper.HttpResponse;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Job {
    private String name;
    private long interval;
    private String description;
    private String composable;
    private String data;
    private String input;

    private String lastError = "";
    private long created = 0;
    private long lastExecution = 0;
    private long nextExecution = 0;

    public Job(String name, long interval, String description, String composable, String data, String input) {
        this.name = name;

        if (interval < 1) {
            interval = 1;
        }
        this.interval = interval * 60 * 1000;//minutes to milliseconds


        this.description = description;
        this.composable = composable;
        this.data = data;
        this.input = input;
        Date date = new Date();

        this.created = date.getTime();
    }

    public void execute(JavaScriptEnvironmentManager jsem) {
        Date date = new Date();


        try {
            jsem.invokeFunction("executeComposable", composable, data, input);
            lastError = "";
        } catch (Exception e) {
            lastError = e.getMessage();
        }
        lastExecution = date.getTime();
        nextExecution = lastExecution + interval;
    }

    public String getName() {
        return name;
    }

    public long getInterval() {
        return interval;
    }

    public String getDescription() {
        return description;
    }

    public String getComposable() {
        return composable;
    }

    public String getData() {
        return data;
    }

    public String getInput() {
        return input;
    }

    public long getNextExecution() {
        return nextExecution;
    }

    public String getLastError() {
        return lastError;
    }

    public long getCreated() {
        return created;
    }

    public long getLastExecution() {
        return lastExecution;
    }

    @Override
    public String toString() {
        return "[" + name + "] " + description + "\n" +
                "  composable: " + composable + "\n" +
                "  created: " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(created) + "\n" +
                "  last: " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(lastExecution) + "\n" +
                "  next: " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(nextExecution) + "\n" +
                "  last error: " + lastError;
    }
}
