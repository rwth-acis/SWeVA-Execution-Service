package i5.las2peer.services.swevaExecution;

import i5.las2peer.api.Service;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import javax.ws.rs.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * LAS2peer Service
 * <p>
 * This is a template for a very basic LAS2peer service
 * that uses the LAS2peer Web-Connector for RESTful access to it.
 * <p>
 * Note:
 * If you plan on using Swagger you should adapt the information below
 * in the ApiInfo annotation to suit your project.
 * If you do not intend to provide a Swagger documentation of your service API,
 * the entire ApiInfo annotation should be removed.
 */
@Path("/swevaExecution")
@Version("0.1") // this annotation is used by the XML mapper
@Api
@SwaggerDefinition(
        info = @Info(
                title = "SWeVA Execution Service",
                version = "0.1",
                description = "A service to execute SWeVA composables.",
                termsOfService = "",
                contact = @Contact(
                        name = "Alexander Ruppert",
                        url = "",
                        email = "alexander.ruppert@rwth-aachen.de"
                ),
                license = @License(
                        name = "BSD",
                        url = ""
                )
        ))
public class SwevaExecution extends Service {


    private JavaScriptEnvironmentManager javaScriptEnvironmentManager;
    private JobManager jobManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public SwevaExecution() {
        // read and set properties values
        // IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
        setFieldValues();
        // instantiate a database manager to handle database connection pooling and credentials
        try {
            javaScriptEnvironmentManager = new JavaScriptEnvironmentManager(
                    new String[]{"http://localhost:5001/core.build.js", "./js/scripts.js"});
            jobManager = new JobManager();
            scheduler.scheduleAtFixedRate(() -> jobManager.executeJobs(javaScriptEnvironmentManager), 0, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @POST
    @Path("execute/{composable}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Composable execution",
            notes = "Executes a composable with the given data and input values in the request body.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_SERVER_ERROR, message = "Something went wrong"),
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Successfully executed")
    })
    public HttpResponse executeComposable(@PathParam("composable") String composable, @ContentParam() String content) {

        JSONObject body = (JSONObject) JSONValue.parse(content);
        JSONObject data = (JSONObject) body.get("data");
        JSONObject input = (JSONObject) body.get("input");


        String result;


        try {
            result = javaScriptEnvironmentManager.invokeFunction("executeComposable", composable, data.toJSONString(), input.toJSONString());
        } catch (Exception e) {
            result = e.getMessage();
            return new HttpResponse(result, HttpURLConnection.HTTP_SERVER_ERROR);

        }

        return new HttpResponse(result, HttpURLConnection.HTTP_OK);
    }

    @GET
    @Path("jobs")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Lists all jobs",
            notes = "Lists all jobs currently registered.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Job listed")
    })
    public HttpResponse getJobs (){
        return new HttpResponse(jobManager.listJobs(), HttpURLConnection.HTTP_OK);
    }

    @DELETE
    @Path("jobs/{job}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Deletes a job",
            notes = "Deletes a job with the given name.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Job not found"),
            @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Job deleted")
    })
    public HttpResponse deleteJob (@PathParam("job") String jobName){
        boolean result = jobManager.removeJob(jobName);
        if(result){
            return new HttpResponse("Job deleted", HttpURLConnection.HTTP_OK);
        }
        return new HttpResponse("Could not delete job", HttpURLConnection.HTTP_NOT_FOUND);
    }

    @POST
    @Path("jobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Creates a job",
            notes = "Jobs are tasks perodically run. I.e. can be used for crawlers.")
    @ApiResponses(value = {
            @ApiResponse(code = HttpURLConnection.HTTP_SERVER_ERROR, message = "Something went wrong"),
            @ApiResponse(code = HttpURLConnection.HTTP_CREATED, message = "Job created")
    })
    public HttpResponse addJob(@ContentParam() String content) {
        JSONObject body = (JSONObject) JSONValue.parse(content);
        String name = (String) body.get("name");
        long interval = Long.valueOf((Integer) body.get("interval"));
        String description = (String) body.get("description");
        JSONObject execution = (JSONObject) body.get("execution");
        String composable = (String) execution.get("composable");
        JSONObject data = (JSONObject) execution.get("data");
        JSONObject input = (JSONObject) execution.get("input");

        try {
            jobManager.addJob(new Job(name, interval, description, composable, data.toJSONString(), input.toJSONString()));
        } catch (Exception e) {
            return new HttpResponse(e.getMessage(), HttpURLConnection.HTTP_SERVER_ERROR);
        }

        return new HttpResponse("Job created", HttpURLConnection.HTTP_CREATED);
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    // Methods required by the LAS2peer framework.
    // //////////////////////////////////////////////////////////////////////////////////////

    /**
     * Method for debugging purposes.
     * Here the concept of restMapping validation is shown.
     * It is important to check, if all annotations are correct and consistent.
     * Otherwise the service will not be accessible by the WebConnector.
     * Best to do it in the unit tests.
     * To avoid being overlooked/ignored the method is implemented here and not in the test section.
     *
     * @return true, if mapping correct
     */
    public boolean debugMapping() {
        String XML_LOCATION = "./restMapping.xml";
        String xml = getRESTMapping();

        try {
            RESTMapper.writeFile(XML_LOCATION, xml);
        } catch (IOException e) {
            e.printStackTrace();
        }

        XMLCheck validator = new XMLCheck();
        ValidationResult result = validator.validate(xml);

        if (result.isValid()) {
            return true;
        }
        return false;
    }

    /**
     * This method is needed for every RESTful application in LAS2peer. There is no need to change!
     *
     * @return the mapping
     */
    public String getRESTMapping() {
        String result = "";
        try {
            result = RESTMapper.getMethodsAsXML(this.getClass());
        } catch (Exception e) {

            e.printStackTrace();
        }
        return result;
    }

}
