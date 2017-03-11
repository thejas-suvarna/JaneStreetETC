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

    public boolean buyBond(String replyStream) {
        int sellStart = replyStream.indexOf("SELL");
        if(sellStart != -1) {
            int i = sellStart + 5;
            String costString = "";
            while (replyStream.charAt(i) != ':') {
                System.out.println("Buy Cost Loop");
                costString += replyStream.charAt(i);
                i++;
            }
            int cost = Integer.parseInt(costString);
            String amountString = "";
            while (replyStream.charAt(i) != ' ') {
                System.out.println("Buy Amount Loop");
                amountString += replyStream.charAt(i);
            }
            int amount = Integer.parseInt(amountString);

            if (cost < 1000) {
                to_exchange.println("ADD " + orderID + " BOND BUY " + cost + " " + amount);
                orderID++;
                return true;
            }
        }
        return false;
    }

    public boolean sellBond(String stream) {
        int pos = stream.lastIndexOf("SELL");
        if(pos != -1) {
            int end = stream.indexOf(':', pos);
            String lowestSell = stream.substring(pos + 2, end);
            int infoend = stream.indexOf(' ', end);
            String numtoSell = stream.substring(end + 2, infoend);
            int position = Integer.parseInt(numtoSell);
            int lowprice = Integer.parseInt(lowestSell);
            int sellprice;
            String trans;
            if (lowprice <= 1001) {
                sellprice = lowprice - 1;
                trans = "ADD " + orderID + " BOND SELL " + sellprice + " " + position;
                to_exchange.println(trans);
                return true;
            }
        }
        return false;
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
                //System.err.printf("The exchange replied: %s\n", replyStream);

                if(replyStream.charAt(5) == 'B') {
                    System.err.printf("The exchange replied: %s\n", replyStream);
                    boolean bought = b.buyBond(replyStream);
                    if(bought) {
                        String buyReply = from_exchange.readLine().trim();
                        System.err.printf("The exchange replied: %s\n", buyReply);
                    }
                    boolean sold = b.sellBond(replyStream);
                    if(sold) {
                        String sellReply = from_exchange.readLine().trim();
                        System.err.printf("The exchange replied: %s\n", sellReply);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }
}
