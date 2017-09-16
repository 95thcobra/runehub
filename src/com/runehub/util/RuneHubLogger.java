package com.runehub.util;

import java.io.*;
import java.text.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/16/2017
 */
public class RuneHubLogger extends PrintStream {
    private final SimpleDateFormat date = new SimpleDateFormat("h:mm:ss");

    public RuneHubLogger(OutputStream out) {
        super(out);
    }

    @Override
    public void println(boolean var) {
        super.println(construct(var));
    }

    @Override
    public void println(char var) {
        super.println(construct(var));
    }

    @Override
    public void println(int var) {
        super.println(construct(var));
    }

    @Override
    public void println(long var) {
        super.println(construct(var));
    }

    @Override
    public void println(float var) {
        super.println(construct(var));
    }

    @Override
    public void println(double var) {
        super.println(construct(var));
    }

    @Override
    public void println(char[] var) {
        super.println(construct(var));
    }

    @Override
    public void println(String var) {
        super.println(construct(var));
    }

    @Override
    public void println(Object var) {
        super.println(construct(var));
    }

    private String construct(Object var) {
        StackTraceElement t = new Throwable().getStackTrace()[2];
        return "[" + date.format(System.currentTimeMillis()) + "] .(" + t.getFileName() + ":" + t.getLineNumber() + "): " + var;
    }
}
