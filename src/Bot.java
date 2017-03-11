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

    public int buyBond(String stream) {
        //int sellStart = replyStream.indexOf("SELL");
        int pos = stream.lastIndexOf("SELL");
        if(pos != -1) {
            String sellInfo = stream.substring(pos);
            int end = stream.indexOf(':', pos);

            String highestBuy = stream.substring(pos + 5, end);
            //System.out.println(lowestSell);
            int infoend = stream.indexOf(' ', end);
            String numtoSell = "";
            if (infoend == -1) {
                numtoSell = stream.substring(end + 1);
            } else {
                numtoSell = stream.substring(end + 1, infoend);
            }
            int position = Integer.parseInt(numtoSell);
            int cost = Integer.parseInt(highestBuy);
            int sellprice;
            String trans;

            /*
            int i = sellStart + 5;
            String costString = "";
            while (replyStream.charAt(i) != ':') {
                //System.out.println("Buy Cost Loop");
                costString += replyStream.charAt(i);
                i++;
            }
            int cost = Integer.parseInt(costString);
            String amountString = "";
            i++;
            while (replyStream.charAt(i) != ' ' && i < replyStream.length()) {
                //System.out.println("Buy Amount Loop");
                amountString += replyStream.charAt(i);
                i++;
            }
            int amount = Integer.parseInt(amountString);
            */

            if (cost < 1000) {
                String sendBuy = "ADD " + orderID + " BOND BUY " + cost + " " + position;
                System.out.println("Sending: " + sendBuy);
                to_exchange.println(sendBuy);
                orderID++;
                return cost;
            }
        }
        return -1;
    }

    public int sellBond(String stream, int buyPrice) {
        int pos = stream.lastIndexOf("BUY");
        if(pos != -1) {
            int end = stream.indexOf(':', pos);
            String lowestSell = stream.substring(pos + 4, end);
            //System.out.println(lowestSell);
            int infoend = stream.indexOf(' ', end);
            String numtoSell = stream.substring(end + 1, infoend);
            int position = Integer.parseInt(numtoSell);
            int lowprice = Integer.parseInt(lowestSell);
            int sellprice;
            String trans;
            if (lowprice >= buyPrice) {
                sellprice = lowprice - 1;
                trans = "ADD " + orderID + " BOND SELL " + sellprice + " " + position;
                System.out.println("Sending: " + trans);
                to_exchange.println(trans);
                orderID++;
                return sellprice;
            }
        }
        return -1;
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

                if(replyStream.contains("BOND") && replyStream.contains("BOOK")) {
                    System.err.printf("The exchange replied: %s\n", replyStream);
                    int buyPrice = b.buyBond(replyStream);
                    if(buyPrice != -1) {
                        String buyReply = from_exchange.readLine().trim();
                        System.err.printf("The exchange replied: %s\n", buyReply);
                    }
                    int sellPrice = b.sellBond(replyStream, buyPrice);
                    if(sellPrice != -1) {
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
