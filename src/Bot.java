/**
 * Created by gregory on 3/11/2017.
 */

import java.lang.*;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;

public class Bot {

    public Bot() {

    }

    public void buyBond() {

    }

    public void sellBond() {

    }

    public static void main(String[] args) {
        try
        {
            Socket skt = new Socket("test-exch-abcde", 20000);
            BufferedReader from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            PrintWriter to_exchange = new PrintWriter(skt.getOutputStream(), true);

            to_exchange.println("HELLO ABCDE");
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);

            Bot b = new Bot();

            while(true) {
                String replyStream = from_exchange.readLine().trim();
                System.err.printf("The exchange replied: %s\n", replyStream);

                if(replyStream.charAt(5) == 'B') {

                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }
}
