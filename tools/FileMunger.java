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

    final static String UPPER_START_MARKER = "<(td|TD)\\s+[^>]*(rowspan|ROWSPAN)\\s*=\\s*['\"]?3['\"]?[^>]*>";
    final static String UPPER_END_MARKER = "</(td|TD)\\s*>";

    final static String LOWER_START_MARKER1 = "<[aA]\\s+(name|NAME)=['\"]navbar_bottom['\"]\\s*>";
    final static String LOWER_START_MARKER2 = "<(tr|TR)\\s*>";

    String _encoding = "UTF-8";

    int _filesChecked;
    int _filesModified;

    final Pattern _upperStartPattern;
    final Pattern _upperEndPattern;

    final Pattern _lowerStartPattern1;
    final Pattern _lowerStartPattern2;

    private FileMunger()
    {
        _upperStartPattern = Pattern.compile(UPPER_START_MARKER);
        _upperEndPattern = Pattern.compile(UPPER_END_MARKER);

        _lowerStartPattern1 = Pattern.compile(LOWER_START_MARKER1);
        _lowerStartPattern2 = Pattern.compile(LOWER_START_MARKER2);
    }

    public boolean munge(String[] args)
        throws Exception
    {
        _filesChecked = _filesModified = 0;
        if (args.length != 3) {
            System.err.println("Usage: java "+getClass()+" [input-dir] [inject-file1] [inject-file2");
            return false;
        }
        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.err.println("Argument #1 not a directory");
            return false;
        }
        File[] files = new File[2];
        for (int i = 0; i < files.length; ++i) {
            String fstr = args[1+i];
            File f = new File(fstr);
            files[i] = f;
            if (!f.isFile()) {
                System.err.println("Argument '"+fstr+"' is not a file");
                return false;
            }
        }
        String content1 = readFile(files[0]);
        // Need to surround second part within table cell marker
        String content2 = "<td rowspan='3'>"+readFile(files[1])+"</td>";

        mungeDir(dir, content1, content2);
        System.out.println("Complete: files checked: "+_filesChecked+"; modified: "+_filesModified);
        return true;
    }

    private void mungeDir(File dir, String content1, String content2)
        throws IOException
    {
        System.out.println("Checking directory '"+dir.getName()+"': ");

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                mungeDir(file, content1, content2);
                continue;
            }
            ++_filesChecked;
            mungeFile(file, content1, content2);
        }
        System.out.println("Finished directory '"+dir.getName()+"': ");
    }

    private void mungeFile(File file, String inclContent1, String inclContent2)
        throws IOException
    {
        System.out.print("Checking file '"+file.getName()+"': ");
        String origContent = readFile(file);
        Matcher m = _upperStartPattern.matcher(origContent);
        if (m.find()) {

            int startStart = m.start();
            int startEnd = m.end();

            Matcher m2 = _upperEndPattern.matcher(origContent);
            if (m2.find(startEnd)) {
                ++_filesModified;
                int endStart = m2.start();
                int endEnd = m2.end();

                String upper = origContent.substring(0, startEnd) + inclContent1;
                String lower = mungeBottom(origContent.substring(endStart), inclContent2);

                // Good: let's output!
                Writer w = new OutputStreamWriter(new FileOutputStream(file), _encoding);
                w.write(upper);
                w.write(lower);
                w.close();
                System.out.println("MATCH: replaced content");
                return;
            }
            System.out.println("WARN: start matched, end not");
            return;
        }
        System.out.println("(no match)");
    }

    private String mungeBottom(String bottomStr, String incl)
    {
        Matcher m = _lowerStartPattern1.matcher(bottomStr);
        if (m.find()) {
            Matcher m2 = _lowerStartPattern2.matcher(bottomStr);
            if (m2.find(m.end())) {
                StringBuilder sb = new StringBuilder(bottomStr);
                sb.insert(m2.end(), incl);
                return sb.toString();
            }
        }
        // nope, no match, return as is:
        System.out.println(" (warn: secondary part didn't match)");
        return bottomStr;
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
