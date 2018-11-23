package com.websystique.springboot.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LambdaStatsCollector {
    public LambdaStats collect(Map<String,String> headers){

        LambdaStats stats = new LambdaStats();
        
        try {
            // This is for testing. Actual implementation needs to be agnostic as to the invocation method as this stats
            // collector should also be compatible for other lambda event sources than API Gateway
            if(headers != null){
                String keepwarmHeader = headers.get("X-Lambda-Keepwarm");
                if(keepwarmHeader != null){
                    int sleepMillis = Integer.parseInt(keepwarmHeader);
                    sleepMillis = sleepMillis < 2000 ? sleepMillis : 2000;
                    Thread.sleep(sleepMillis);
                }
            }
		} catch (InterruptedException | NumberFormatException e) {
			e.printStackTrace();
		}

        try {
            
            File scratchFile = new File(getTempDir(),"container_id.tmp");

            if (!scratchFile.exists())
            {
                String id = UUID.randomUUID().toString();

                scratchFile.createNewFile();
                FileWriter fileWriter = new FileWriter(scratchFile.getAbsolutePath());
                PrintWriter printWriter = new PrintWriter(fileWriter);
                printWriter.print(id);
                printWriter.close();

                stats.setContainerId(id);
                stats.setColdStart(true);
            }
            else
            {
                List<String> allLines = Files.readAllLines(Paths.get(scratchFile.getAbsolutePath()));
                
                stats.setContainerId(allLines.get(0));
                stats.setColdStart(false);
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }

        return stats;
    }

    private String getTempDir(){

        String os = System.getProperty("os.name").toLowerCase();

        if(os.indexOf("mac") >= 0 || os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0){
            // Lambda temp dir is allocated here
            return "/tmp";
        }
        
        return System.getProperty("java.io.tmpdir");
    }
}