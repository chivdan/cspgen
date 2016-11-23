package ru.ifmo.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class NxtStudioTester {
	private int testingPort = 7500;
	private int terminatePort = 7600;
	private Random random = new Random();
	private String testFileName;
	
	public void run(String[] args) throws IOException, InterruptedException {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getLocalizedMessage());
			System.err.println("Test generator for PnP system");
			System.err.println("Author: Daniil Chivilikhin (chivdan@rain.ifmo.ru)\n");
			System.err.print("Usage: ");
			parser.printSingleLineUsage(System.err);
			System.err.println();
			parser.printUsage(System.err);
			return;
		}
		
		testFileName = "test-" + length + "-" + id;
		
		TcpClient client = new TcpClient(testingPort);
		TcpClient terminate = new TcpClient(terminatePort); 
		PrintWriter test = new PrintWriter(new File(testFileName));

		for (int i = 0; i < length; i++) {
			int wp = 1 + random.nextInt(3);
			String result = wp + "";
			System.out.println("Sending " + result);
			client.write(result);
			test.println(wp);
			Thread.sleep(7000);
		}
		
		terminate.write("terminate");
		client.close();
		terminate.close();
		test.close();
	}
	

	@Option(name = "--id", aliases = {"-i"}, usage = "test id", metaVar = "<id>", required = true)
	private int id;
	
	@Option(name = "--length", aliases = {"-l"}, usage = "test length (in WPs)", metaVar = "<length>", required = true)
	private int length;
	
	public static void main(String[] args) throws Exception {
		new NxtStudioTester().run(args);
	}
}
