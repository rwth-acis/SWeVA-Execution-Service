package i5.las2peer.services.swevaExecution;


import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Iterator;

public class JavaScriptEnvironmentManager {
    private ArrayList<JavaScriptEnvironment> javaScriptEnvironments = new ArrayList<JavaScriptEnvironment>();
    private String[] libraries;
    private final static int TARGET_POOL_SIZE = 1000;
    private final static int MAX_POOL_SIZE = 2000;

    public JavaScriptEnvironmentManager() {

    }

    public JavaScriptEnvironmentManager(String[] libraries) throws Exception {
        this();
        this.libraries = libraries;
        addEngine();
    }

    public String invokeFunction(String name, Object... args) throws Exception {

        Iterator<JavaScriptEnvironment> i = javaScriptEnvironments.iterator();
        while (i.hasNext()) {
            JavaScriptEnvironment jse = i.next();
            if (!jse.isRunning()) {

                try {
                    return jse.invokeFunction(name, args);
                } catch (ScriptException e) {
                    throw new Exception(jse.getAndClearLog(), e);
                }

            }
        }

        JavaScriptEnvironment jse = addEngine();
        if (jse != null) {
            try {
                return jse.invokeFunction(name, args);
            } catch (ScriptException e) {
                throw new Exception(jse.getAndClearLog(), e);
            }
        }


        return null;
    }

    private JavaScriptEnvironment addEngine() throws Exception {
        if (javaScriptEnvironments.size() < MAX_POOL_SIZE) {
            JavaScriptEnvironment javaScriptEnvironment = new JavaScriptEnvironment(this.libraries);
            javaScriptEnvironments.add(javaScriptEnvironment);
            return javaScriptEnvironment;
        } else {
            throw new Exception("Could not create additional JavaScript engine. Wait for other engines to finish.");

        }
    }

    public void cleanup() {
        Iterator<JavaScriptEnvironment> i = javaScriptEnvironments.iterator();
        int freeCount = 0;
        while (i.hasNext()) {
            JavaScriptEnvironment jse = i.next();
            if (!jse.isRunning()) {
                freeCount++;
            }
            i.remove();
        }
        if (freeCount > TARGET_POOL_SIZE) {
            i = javaScriptEnvironments.iterator();
            int removeCount = freeCount - TARGET_POOL_SIZE;
            while (i.hasNext() && removeCount > 0) {
                JavaScriptEnvironment jse = i.next();
                if (!jse.isRunning()) {
                    removeCount--;
                    i.remove();
                }
            }
        }

    }

    public void close(){
        Iterator<JavaScriptEnvironment> i = javaScriptEnvironments.iterator();
        while (i.hasNext()) {
            JavaScriptEnvironment jse = i.next();
            jse.close();
        }
        javaScriptEnvironments.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            this.close();
        } catch (Throwable t) {
            throw t;
        } finally {
            super.finalize();
        }
    }
}
