package i5.las2peer.services.swevaExecution;


import jdk.nashorn.api.scripting.JSObject;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import javax.script.*;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.Phaser;

public class JavaScriptEnvironment {

    private static final String POLYFILL_FILE = "./js/nashornPolyfill.js";
    private ScriptEngine engine;
    private CloseableHttpAsyncClient httpclient;
    private Phaser phaser;
    private Timer timer;
    private StringBuilder logger;
    private Invocable invocable;
    private boolean running = false;
    private String resultJSON = "";

    public JavaScriptEnvironment() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        httpclient = HttpAsyncClients.createDefault();
        phaser = new Phaser(1);
        timer = new Timer("jsEventLoop", false);
        logger = new StringBuilder();
        invocable = (Invocable) engine;
        httpclient.start();
        setBindings();
        loadPolyfill();

    }

    private void loadPolyfill() {
        try {
            engine.eval("load('" + POLYFILL_FILE + "')");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    public JavaScriptEnvironment(String[] libraries) throws ScriptException {
        this();
        load(libraries);
    }

    private void load(String[] libraries) throws ScriptException {
        for (int i = 0; i < libraries.length; i++) {

            engine.eval("load('" + libraries[i] + "')");

        }
    }

    private void setBindings() {
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        engineScope.put("window", engineScope);
        engineScope.put("phaser", phaser);
        engineScope.put("timer", timer);
        engineScope.put("httpclient", httpclient);
        engineScope.put("logger", logger);
        engineScope.put("resultJSON", resultJSON);
    }

    public String getAndClearLog(){
        String result = logger.toString();
        logger.setLength(0);
        return result;
    }

    public String getLog(){
        return logger.toString();
    }

    public String eval(String code) throws ScriptException {

        running = true;
        Object result = engine.eval(code);
        phaser.arriveAndAwaitAdvance();
        running = false;
        resultJSON = (String) engine.get("resultJSON");
        return resultJSON;
    }

    public boolean isRunning() {
        return running;
    }

    public String invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {

        running = true;
        invocable.invokeFunction(name, args);
        phaser.arriveAndAwaitAdvance();

        resultJSON = (String) engine.get("resultJSON");

        running = false;

        return resultJSON;
    }

    public void close() {
        timer.cancel();
        timer.purge();
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
