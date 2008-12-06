package tools;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Simple helper class that can be used to modify bunch of files (recursively)
 * by matching start and end regexps, and replacing content between
 * first instance of such a pair.
 */
public final class FileMunger
{
    final static int MAX_REGEX_LEN = 500;

    String _encoding = "UTF-8";

    int _filesChecked;
    int _filesModified;

    private FileMunger() { }

    public boolean munge(String[] args)
        throws Exception
    {
        _filesChecked = _filesModified = 0;
        if (args.length != 4) {
            System.err.println("Usage: java "+getClass()+" [input-dir] [start-marker-re-file] [end-marker-re-file] [inject-file]");
            return false;
        }
        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.err.println("Argument #1 not a directory");
            return false;
        }
        File[] files = new File[3];
        for (int i = 0; i < 3; ++i) {
            String fstr = args[1+i];
            File f = new File(fstr);
            files[i] = f;
            if (!f.isFile()) {
                System.err.println("Argument '"+fstr+"' is not a file");
                return false;
            }
        }
        Pattern startRE = getRegex(files[0]);
        Pattern endRE = getRegex(files[1]);
        String content = readFile(files[2]);

        mungeDir(dir, startRE, endRE, content);
        System.out.println("Complete: files checked: "+_filesChecked+"; modified: "+_filesModified);
        return true;
    }

    private void mungeDir(File dir, Pattern startRE, Pattern endRE, String inclContent)
        throws IOException
    {
        System.out.println("Checking directory '"+dir.getName()+"': ");

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                mungeDir(file, startRE, endRE, inclContent);
                continue;
            }
            ++_filesChecked;
            mungeFile(file, startRE, endRE, inclContent);
        }
        System.out.println("Finished directory '"+dir.getName()+"': ");
    }

    private void mungeFile(File file, Pattern startRE, Pattern endRE, String inclContent)
        throws IOException
    {
        System.out.print("Checking file '"+file.getName()+"': ");
        String origContent = readFile(file);
        Matcher m = startRE.matcher(origContent);
        if (m.find()) {
            int startStart = m.start();
            int startEnd = m.end();

            Matcher m2 = endRE.matcher(origContent);
            if (m2.find(startEnd)) {
                ++_filesModified;
                int endStart = m2.start();
                int endEnd = m2.end();

                // Good: let's replace!
                Writer w = new OutputStreamWriter(new FileOutputStream(file), _encoding);
                w.write(origContent, 0, startEnd);
                w.write(inclContent);
                w.write(origContent.substring(endStart));
                w.close();
                System.out.println("MATCH: replaced content");
                return;
            }
            System.out.println("WARN: start matched, end not");
            return;
        }
        System.out.println("(no match)");
    }

    private Pattern getRegex(File input)
        throws IOException
    {
        String reStr = readFile(input);
        // Let's allow comments in there tho:
        BufferedReader br = new BufferedReader(new StringReader(reStr));
        StringBuilder sb = new StringBuilder(reStr.length());
        String line;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            sb.append(line);
        }
        reStr = sb.toString();

        // for now, let's havity sanity check limit...
        if (reStr.length() > MAX_REGEX_LEN) {
            throw new IllegalArgumentException("File '"+input+"' contains regexp that is too long (max "+MAX_REGEX_LEN+"; got "+reStr.length()+")");
        }
        
System.err.println("RegExp from "+input+": '"+reStr+"'");

        try {
            return Pattern.compile(reStr);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Failed to parse regex from file '"+input+"': "+iae.getMessage());
        }
    }

    private String readFile(File input)
        throws IOException
    {
        Reader r = new InputStreamReader(new FileInputStream(input), _encoding);
        int len = (int) input.length();
        char[] buf = new char[Math.min(len, 2000)];
        StringBuilder sb = new StringBuilder(len);
        int count;

        while ((count = r.read(buf)) > 0) {
            sb.append(buf, 0, count);
        }
        r.close();
        return sb.toString();
    }

    public static void main(String[] args)
        throws Exception
    {
        if (!new FileMunger().munge(args)) {
            System.exit(1);
        }
    }
}
