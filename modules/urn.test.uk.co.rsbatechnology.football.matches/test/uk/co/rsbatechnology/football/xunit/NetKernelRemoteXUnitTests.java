package uk.co.rsbatechnology.football.xunit;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class NetKernelRemoteXUnitTests {

    @Rule
    public ErrorCollector collector = new ErrorCollector();
    
    /**
     * Example jUnit test that runs all NetKernel xUnit tests configured for a given Module.
     * 
     * The url passed to the runNetKernelXUnitTests method has to point to a running NetKernel instance accessible to the jUnit executable.
     * Should be of the form: <NK Backend fulcrum address>/test/exec/xml/test:<module uri>
     * 
     * @throws Exception
     * 
     */
    @Test
    public void runXUnitTests() throws Exception {
        // SETUP
        
        // EXECUTE
        // Code from: http://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples/client/ClientWithResponseHandler.java
        String response = runNetKernelXUnitTests("http://localhost:1060/test/exec/xml/test:urn:uk:co:rsbatechnology:football:matches", false)
                .orElseThrow(IllegalStateException::new);
        
        // VERIFY
        assertTestsHavePassed(response);
    }

    protected Optional<String> runNetKernelXUnitTests(String url, boolean suppressConsoleOutput) {
        String responseBody = null;
        
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(url);

            if (!suppressConsoleOutput) System.out.println("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            responseBody = httpclient.execute(httpget, responseHandler);
            
            if (!suppressConsoleOutput) {
                System.out.println("----------------------------------------");
                System.out.println(responseBody);                
            }
                        
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return Optional.of(responseBody);
        
    }
    
    protected void assertTestsHavePassed(String responseBody) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(responseBody)));
        
        //see: https://docs.oracle.com/javase/7/docs/api/javax/xml/xpath/package-summary.html
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/testlist/test";
        NodeList testNodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        
            for (int i = 0; i < testNodes.getLength(); i++) {
                Node testNode = testNodes.item(i);
                String testStatus = testNode.getAttributes().getNamedItem("testStatus").getNodeValue();
                Node testNameNode = testNode.getAttributes().getNamedItem("name");
                String failureMessage = "Test FAILED: " + ((testNameNode != null) ? testNameNode.getNodeValue() : "#" + (i+1));
                collector.checkThat(testStatus, equalTo("success"));
            }            
    }
}
