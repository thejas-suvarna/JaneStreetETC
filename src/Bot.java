/**
 * Created by gregory on 3/11/2017.
 */

import java.lang.*;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;

public class Bot {

    private BufferedReader from_exchange;
    private PrintWriter to_exchange;
    private int orderID = 1;

    public Bot(BufferedReader from_exchange, PrintWriter to_exchange) {
        this.from_exchange = from_exchange;
        this.to_exchange = to_exchange;
    }

    public void buyBond(String replyStream) {
        int sellStart = replyStream.indexOf("SELL");
        int i = sellStart + 5;
        String costString = "";
        while(replyStream.charAt(i) != ':') {
            costString += replyStream.charAt(i);
            i++;
        }
        int cost = Integer.parseInt(costString);
        String amountString = "";
        while(replyStream.charAt(i) != ' ') {
            amountString += replyStream.charAt(i);
        }
        int amount = Integer.parseInt(amountString);

        
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

            Bot b = new Bot(from_exchange, to_exchange);

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
