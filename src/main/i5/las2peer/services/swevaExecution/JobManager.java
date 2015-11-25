package i5.las2peer.services.swevaExecution;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class JobManager {
    private ArrayList<Job> jobs = new ArrayList<Job>();
    private final static int MAX_JOBS = 1000;

    public JobManager() {

    }

    public String listJobs() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Job job : jobs) {
            stringBuilder.append(job.toString() + "\n\n");
        }
        return stringBuilder.toString();
    }

    public void addJob(Job job) throws Exception {

        for (Job jobItem : jobs) {
            if(jobItem.getName().equals(job.getName())){
                throw new Exception("Job with the name \""+job.getName()+"\" already exists.");
            }
        }

        if (jobs.size() < MAX_JOBS) {
            jobs.add(job);
        } else {
            throw new Exception("Limit of " + MAX_JOBS + " jobs reached. Please remove some running jobs.");
        }
    }

    public boolean removeJob(String name) {
        Iterator<Job> i = jobs.iterator();
        boolean result = false;
        while (i.hasNext()) {
            Job job = i.next();
            if (job.getName().equals(name)) {
                result = true;
                i.remove();
            }
        }
        return result;
    }

    public void executeJobs(JavaScriptEnvironmentManager jsem) {

        Date date = new Date();
        long now = date.getTime();
        for (Job job : jobs) {
            if (job.getNextExecution() <= now) {
                job.execute(jsem);
            }
        }

    }

    public void clear() {
        jobs.clear();
    }
}
