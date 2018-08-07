package opendoc.batch.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessMgr extends Thread
{
    InputStream is;
    public ProcessMgr(InputStream is) {
        this.is = is;
    }
    public void run()
    {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ( (line = br.readLine()) != null)
                LogMgr.log("Process Message == "+line);
        } catch (IOException ioe) {
        	LogMgr.log("ProcessMgr Error == "+ioe.getMessage());
        }
    }
}

