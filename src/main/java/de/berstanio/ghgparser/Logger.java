package de.berstanio.ghgparser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
//EInen kleinen Logger den ich geschrieben habe, ist aber nicht so relevant
public class Logger {

   static class LogPrintStream extends OutputStream{

       private PrintStream first;
       private PrintStream second;

       public LogPrintStream(PrintStream first, PrintStream second){
           setFirst(first);
           setSecond(second);
       }

       @Override
       public void close() {
           getFirst().close();
           getSecond().close();
       }

       @Override
       public void write(int b) {
            getFirst().write(b);
            getSecond().write(b);
       }

       public PrintStream getFirst() {
           return first;
       }

       public void setFirst(PrintStream first) {
           this.first = first;
       }

       public PrintStream getSecond() {
           return second;
       }

       public void setSecond(PrintStream second) {
           this.second = second;
       }
   }

    static {
        try {
            if (!new File("logs").exists()) new File("logs").mkdir();
            Files.deleteIfExists(Paths.get("logs/latest.log"));
            Files.createFile(Paths.get("logs/latest.log"));
            PrintStream outStream = new PrintStream(new LogPrintStream(new PrintStream(new FileOutputStream("logs/latest.log")), System.out),true);
            System.setOut(outStream);
            PrintStream errStream = new PrintStream(new LogPrintStream(new PrintStream(new FileOutputStream("logs/latest.log")), System.err),true);
            System.setErr(errStream);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                Date date = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy_HH:mm");
                String s = simpleDateFormat.format(date);
                s = "logs/log-" + s + ".log";
                try {
                    new File("logs/latest.log").renameTo(new File(s));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                outStream.close();
                errStream.close();
            },"Shutdown-thread"));

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
