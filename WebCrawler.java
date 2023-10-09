import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * Prog1 - WebCrawler
 * Professor Dimpsey
 * CSS 436 - Cloud Computing
 *
 * This program represents a web crawler application.
 * The user can provide a URL and the number of hops
 * as an argument for the web crawler.
 *
 * @author Krishna Langille
 * @version 1.0
 * January 18, 2023
 *
 */
public class WebCrawler
{
    /*
    These are global variables
    They are used to mostly store data that would be erased during recursion
    */
    static String[] visited = new String[1000]; //Used to store visited websites, max of 1000 entries
    static int numVisited = 0; //Used as a counter for the visited array to keep track of how much is populated
    static boolean finished = false; //Used to check if the recursion is finished or not
    static boolean error = false; //Used for 400 level status codes to properly re-enter recursion
    static String previous = ""; //Used to quickly access the previous URL for 400 level status codes

    /**
     * This is the main method that calls the recursive
     * method requestGET to start recursively web crawling.
     * @param args - This is the arguments in the command line.
     *             Should be a starting URL followed by the number of hops.
     * @throws IOException - thrown because we are opening an HTTP connection in the recursion.
     */
    public static void main(String[] args) throws IOException{
        //parse cli inputs into url string and number of hops(int)
        String stringUrl = args[0];

        //adds the current URL to the visited array and increments the size
        visited[numVisited++] = stringUrl;

        //calls method that invokes the GET TCP HTTP request
        requestGET(stringUrl, Integer.parseInt(args[1]));
    }

    /**
     * This method is the heart of the program that recursively web crawls.
     * @param stringUrl - This is the URL that is passed in. Initially it
     *                  is the first argument in the main, but it then changes
     *                  to whatever the next URL is.
     * @param jumps - This is the number of jumps the recursion has left until the end.
     * @throws IOException - thrown because we are opening an HTTP connection.
     */
    public static void requestGET(String stringUrl, int jumps) throws IOException{
        //make a URL object from the input String
        URL url = new URL(stringUrl);

        //make the connection to fetch resources
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        //set the HTTP request wanted. In this case GET
        connection.setRequestMethod("GET");

        //gets the status code from the GET request
        int statusCode = connection.getResponseCode();
        System.out.println("Status code: " + statusCode);

        //switch case that determines how the status code is handled
        switch(statusCode/100){
            case 1:
            case 2:
                //Nothing to be done here
                break;
            case 3:
                //this properly redirects the connection to the proper URL
                String redirectUrl = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(redirectUrl).openConnection();
                System.out.println("Redirecting: " + stringUrl + " to: " + redirectUrl);
                break;
            case 4:
                //this flips the error bit and returns since 400 level codes are bad requests on the server side
                System.out.println("Error visiting " + stringUrl + ", going back...");
                error = true;
                return;
            case 5:
                //we retry this instance since there is a temporary issue that may be with the client
                System.out.println("Retrying connection...");
                requestGET(stringUrl, jumps);
                break;
            default:
                break;
        }

        previous = stringUrl;

        //prints out URL currently visiting and the number of jumps left
        System.out.println("Currently visiting: " + stringUrl + "\nNumber of jumps left: " + jumps);

        //this while loop goes through the HTML retrieved and finds instances of a href to further web crawl
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        String newUrl = "";
        boolean found = false;
        while ((line = reader.readLine()) != null)
        {
            //first check if the current line of HTML contains an instance of a href"
            if(line.contains("a href=\"")){

                //this section isolates the part of the line that has our next URL
                int indexStart = line.indexOf("a href=\"");
                newUrl = line.substring(indexStart + 8);
                newUrl = newUrl.substring(0, newUrl.indexOf("\""));

                //checks if the URL is a valid http or https URL
                if(newUrl.startsWith("http")){
                    boolean visit = false;
                    //for loop finds out if the found URL is visited or not
                    for(int i = 0; i < numVisited; i++){
                        if(newUrl.charAt(newUrl.length() - 1) == '/'){
                            if(newUrl.equals(visited[i])){
                                visit = true;
                                break;
                            }
                            if(newUrl.substring(0, newUrl.length() - 1).equals(visited[i])){
                                visit = true;
                                break;
                            }
                        }
                        else{
                            if(newUrl.equalsIgnoreCase(visited[i])){
                                visit = true;
                                break;
                            }
                            if(newUrl.concat("/").equalsIgnoreCase(visited[i])){
                                visit = true;
                                break;
                            }
                        }
                    }
                    //if the URL is unique to the current crawl, add it to the visited array and flip found bit
                    if(!visit){
                        found = true;
                        visited[numVisited++] = newUrl;
                        break;
                    }
                }
            }
        }

        //if no valid links were found, we hit a dead end and exit
        if(!found){
            System.out.println("No valid links were found, exiting...");
            System.exit(0);
        }

        //if there are still hops, we will recurse
        while(jumps != 0 && !finished){
            if(error){
                error = false;
                requestGET(previous, jumps);
            }
            else{
                requestGET(newUrl, --jumps);
            }
        }
        finished = true;
    }
}