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
    private String stream;

    public Bot() {
        initializeConnection();
    }

    public void initializeConnection() {
        try {
            Socket skt = new Socket("production", 20000);
            from_exchange = new BufferedReader(new InputStreamReader(skt.getInputStream()));
            to_exchange = new PrintWriter(skt.getOutputStream(), true);

            to_exchange.println("HELLO ABCDE");
            String reply = from_exchange.readLine().trim();
            System.err.printf("The exchange replied: %s\n", reply);
        } catch (Exception e) {
            //e.printStackTrace(System.out);
            initializeConnection();
        }
    }

    public void trade() {
        try {
            int buyPrice = 0;
            while (true) {
                String replyStream = from_exchange.readLine().trim();
                //System.err.printf("The exchange replied: %s\n", replyStream);
                if (replyStream.contains("BOND") && replyStream.contains("BOOK")) {
                    //System.err.printf("The exchange replied: %s\n", replyStream);
                    int buyPricetemp = buyBond(replyStream);
                    if (buyPricetemp != -1) {
                        if (buyPricetemp > buyPrice){
                            buyPrice = buyPricetemp;
                        }
                        String buyReply = from_exchange.readLine().trim();
                        System.err.printf("The exchange replied: %s\n", buyReply);
                    }
                    int sellPrice = sellBond(replyStream, buyPrice);
                    if (sellPrice != -1) {
                        String sellReply = from_exchange.readLine().trim();
                        System.err.printf("The exchange replied: %s\n", sellReply);
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace(System.out);
            initializeConnection();
        }
    }

        public int calcFairValue(String Stream, String Ticker){
        int maxBuy = 0, minSell = 0, buy, sell, buyPos, sellPos;
        int index1 = Stream.indexOf("BUY");
        int index2 = Stream.indexOf("SELL");
        String buyStream = Stream.substring(index1+4,index2-1);
        String sellStream = Stream.substring(index2+5);
        int indexcol = Stream.indexOf(':');
        while(indexcol != -1){
            buy = Integer.parseInt(buyStream.substring(0,indexcol-1));
            if(buy > maxBuy){
                maxBuy = buy;
            }
                    }

        return 0;
    }

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
            if(end != -1) {
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

                if (cost < 1000) {
                    String sendBuy = "ADD " + orderID + " BOND BUY " + cost + " " + position;
                    System.out.println("Sending: " + sendBuy);
                    to_exchange.println(sendBuy);
                    orderID++;
                    return cost;
                }
            }
        }
        return -1;
    }

    public int sellBond(String stream, int buyPrice) {
        int pos = stream.lastIndexOf("BUY");
        if(pos != -1 && !stream.contains("BUY SELL")) {
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
        Bot b = new Bot();
        while(true) {
            b.trade();
        }
    }
}
