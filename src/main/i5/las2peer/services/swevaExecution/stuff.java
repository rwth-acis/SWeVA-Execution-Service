package i5.las2peer.services.swevaExecution;


import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import javax.script.*;
import java.io.IOException;
import java.util.Timer;

import java.util.concurrent.Phaser;


/**
 * Created by Alexander on 18.11.2015.
 */
public class stuff {
    public static void main(String[] args) {
        doIt3();
        System.out.println("DONE!!!");
    }
    private static void doIt3() {
        System.out.println("-----");

        try {
            JavaScriptEnvironmentManager javaScriptEnvironmentManager = new JavaScriptEnvironmentManager(new String[]{"http://localhost:5001/core.build.js", "./js/test.js"});

            String result = javaScriptEnvironmentManager.invokeFunction("test");
            System.out.println(result);
            result = javaScriptEnvironmentManager.invokeFunction("test");
            System.out.println(result);
            result = javaScriptEnvironmentManager.invokeFunction("test");
            System.out.println(result);
            result = javaScriptEnvironmentManager.invokeFunction("test");
            System.out.println(result);
            javaScriptEnvironmentManager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void doIt2() {
        System.out.println("######");
        JavaScriptEnvironment javaScriptEnvironment = null;
        try {
            javaScriptEnvironment = new JavaScriptEnvironment(new String[]{"http://localhost:5001/core.build.js", "./js/test.js"});
            System.out.println("####1");
            String result = javaScriptEnvironment.invokeFunction("test");
            System.out.println(result);

            System.out.println();
            javaScriptEnvironment.close();
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static void doIt() {

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

        //create shared objects
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        httpclient.start();
        final Phaser phaser = new Phaser(1);
        final Timer timer = new Timer("jsEventLoop", false);
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        StringBuilder logger = new StringBuilder();
        engineScope.put("window", engineScope);
        engineScope.put("phaser", phaser);
        engineScope.put("timer", timer);
        engineScope.put("httpclient", httpclient);
        engineScope.put("logger", logger);


        try {

            engine.eval("load('./js/nashornPolyfill.js')");
            engine.eval("load('http://localhost:5001/core.build.js')");


            System.out.println("############################");
            //String code = "print(Object.keys(sweva).join(', '))";
            /*String  code="sweva.axios.get('http://localhost:5001/nashornPolyfill.js').then(function(response){" +
                    "console.log(response.status);" +
                    "})";*/

            String code = "var manager = new sweva.ExecutionManager();\n" +
                    "manager.setup('add');\n" +

                    "manager.execute({\n" +
                    "   \n" +
                    "        sum1: 10,\n" +
                    "        sum2: 50\n" +
                    "    \n" +
                    "},\n" +
                    "    {\n" +
                    "        offset: 0,\n" +
                    "        invert: 0\n" +
                    "    })\n" +
                    ".then(function (result) {    \n" +
                    "    console.log(result);\n" +
                    "\n" +
                    "});";
           /* String code="" +
                    "var promises=[];" +
                    "promises.push(sweva.axios.get('http://localhost:5001/nashornPolyfill.js'));" +
                    "promises.push(sweva.axios.get('http://localhost:5001/execution.js'));" +
                    "Promise.all(promises).then(function(response){" +
                    "console.log(123);" +
                    "})";*/
           /* String code="sweva.ComposableLoader.load('add').then(function(composable){" +
                    "console.log(composable.name);" +

                    "})";*/

            /*String code =" sweva.axios.get('http://localhost:5001/examplesJSON/add.json').then(function (response) {\n" +
                    "                console.log(response.status);\n" +
                    "            });";*/
            engine.eval(code);


        } catch (Exception e) {
            e.printStackTrace();
        }

        //cleanup
        phaser.arriveAndAwaitAdvance();
        timer.cancel();
        timer.purge();
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
