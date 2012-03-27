package com.j256.simplejmx.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.j256.simplejmx.common.JmxAttribute;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;

public class CommandLineJmxClientTest {

	private static final int JMX_PORT = 8000;
	private static final String JMX_DOMAIN = "foo.com";

	private static JmxServer server;
	private static CommandLineJmxClient client;
	private static String objectName;

	@BeforeClass
	public static void beforeClass() throws Exception {
		server = new JmxServer(JMX_PORT);
		server.start();
		OurJmxClass obj = new OurJmxClass();
		server.register(obj);
		client = new CommandLineJmxClient("localhost", JMX_PORT);
		objectName = JMX_DOMAIN + ":name=" + OurJmxClass.class.getSimpleName();
	}

	@AfterClass
	public static void afterClass() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	@Test
	public void testCommandLine() throws Exception {
		CommandLineJmxClient client = new CommandLineJmxClient("localhost", JMX_PORT);
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		System.setOut(new PrintStream(array));
		System.setIn(new ByteArrayInputStream("help".getBytes()));
		client.runCommandLine();
		String output = new String(array.toByteArray());
		assertTrue(output, output.matches("(?s).*execute a script.*"));
	}

	@Test
	public void testListObjects() throws Exception {
		String output = getClientOutput(client, "objects");
		assertTrue(output, output.contains(objectName));
	}

	@Test
	public void testListObjectsPattern() throws Exception {
		String output = getClientOutput(client, "objects " + OurJmxClass.class.getSimpleName());
		assertTrue(output, output.contains(objectName));
	}

	@Test
	public void testShowAttributes() throws Exception {
		String output = getClientOutput(client, "attrs " + objectName);
		assertTrue(output.contains("x(get, set int)"));
	}

	@Test
	public void testShowAttributesNoObjectName() throws Exception {
		String output = getClientOutput(client, "attrs");
		assertTrue(output, output.matches("(?s).*Usage: attrs.*"));
	}

	@Test
	public void testShowAttributesBadObjectName() throws Exception {
		String output = getClientOutput(client, "attrs not-valid-object-name");
		assertTrue(output, output.matches("(?s).*Invalid object name:.*"));
	}

	@Test
	public void testGetAllAttributes() throws Exception {
		String output = getClientOutput(client, "get " + objectName);
		assertTrue(output, output.matches("(?s).*get 'x' in \\d+ms = 0.*"));
	}

	@Test
	public void testGetAllAttributesBadObject() throws Exception {
		String output = getClientOutput(client, "get bad-object-name");
		assertTrue(output, output.matches("(?s).*Invalid object name.*"));
	}

	@Test
	public void testGetAttribute() throws Exception {
		String output = getClientOutput(client, "get " + objectName + " x");
		assertTrue(output, output.matches("(?s).*get 'x' in \\d+ms = 0.*"));

	}

	@Test
	public void testGetAttributeBadObject() throws Exception {
		String output = getClientOutput(client, "get bad-object-name foo");
		assertTrue(output, output.matches("(?s).*Invalid object name.*"));
	}

	@Test
	public void testGetAllAttributesTooManyArgs() throws Exception {
		String output = getClientOutput(client, "get " + objectName + " too many args");
		assertTrue(output, output.matches("(?s).*Usage: get.*"));
	}

	@Test
	public void testSetAttribute() throws Exception {
		int val = 123;
		String output = getClientOutput(client, "set " + objectName + " x " + val);
		output = getClientOutput(client, "get " + objectName + " x");
		assertTrue(output, output.matches("(?s).*get 'x' in \\d+ms = " + val + ".*"));
	}

	@Test
	public void testSetAttributeBadObjectName() throws Exception {
		String output = getClientOutput(client, "set bad-object-name");
		assertTrue(output, output.matches("(?s).*Invalid object name.*"));
	}

	@Test
	public void testSetAttributeNotEnoughArgs() throws Exception {
		String output = getClientOutput(client, "set " + objectName + " x");
		assertTrue(output, output.matches("(?s).*Usage: set.*"));
	}

	@Test
	public void testSetAttributeWrongType() throws Exception {
		String output = getClientOutput(client, "set " + objectName + " x 1 2 3");
		assertTrue(output, output.matches("(?s).*Problems setting information.*"));
	}

	@Test
	public void testListOperations() throws Exception {
		String output = getClientOutput(client, "ops " + objectName);
		assertTrue(output, output.matches("(?s).*int times\\(int, int\\).*"));
	}

	@Test
	public void testListOperationsBadObjectName() throws Exception {
		String output = getClientOutput(client, "ops bad-object-name");
		assertTrue(output, output.matches("(?s).*Invalid object name.*"));
	}

	@Test
	public void testListOperationsTooManyArgs() throws Exception {
		String output = getClientOutput(client, "ops " + objectName + " 1 2 3");
		assertTrue(output, output.matches("(?s).*Usage: ops.*"));
	}

	@Test
	public void testDoOperation() throws Exception {
		int x1 = 12;
		int x2 = 654;
		String output = getClientOutput(client, "do " + objectName + " times " + x1 + " " + x2);
		int times = x1 * x2;
		assertTrue(output, output.matches("(?s).*do 'times' in \\d+ms = " + times + ".*"));
	}

	@Test
	public void testDoOperationBadObjectName() throws Exception {
		String output = getClientOutput(client, "do bad-object-name");
		assertTrue(output, output.matches("(?s).*Invalid object name.*"));
	}

	@Test
	public void testDoOperationTooFewArgs() throws Exception {
		String output = getClientOutput(client, "do " + objectName);
		assertTrue(output, output.matches("(?s).*Usage: do.*"));
	}

	@Test
	public void testDoOperationByteArrayReturn() throws Exception {
		byte x1 = 17;
		byte x2 = 102;
		String output = getClientOutput(client, "do " + objectName + " byteArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a byte[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationShortArrayReturn() throws Exception {
		short x1 = 12;
		short x2 = 44;
		String output = getClientOutput(client, "do " + objectName + " shortArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a short[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationIntArrayReturn() throws Exception {
		int x1 = 2312;
		int x2 = 6544;
		String output = getClientOutput(client, "do " + objectName + " intArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a int[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationLongArrayReturn() throws Exception {
		long x1 = 231138242;
		long x2 = 6572044;
		String output = getClientOutput(client, "do " + objectName + " longArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a long[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationBooleanArrayReturn() throws Exception {
		boolean x1 = true;
		boolean x2 = false;
		String output = getClientOutput(client, "do " + objectName + " booleanArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a boolean[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationCharArrayReturn() throws Exception {
		char x1 = '4';
		char x2 = '$';
		String output = getClientOutput(client, "do " + objectName + " charArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a char[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationFloatArrayReturn() throws Exception {
		float x1 = 23.12F;
		float x2 = 65.44F;
		String output = getClientOutput(client, "do " + objectName + " floatArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a float[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationDoubleArrayReturn() throws Exception {
		double x1 = 231.2;
		double x2 = 654.4;
		String output = getClientOutput(client, "do " + objectName + " doubleArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a double[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationObjectArrayReturn() throws Exception {
		int x1 = 2312;
		int x2 = 6544;
		String output = getClientOutput(client, "do " + objectName + " objectArray " + x1 + " " + x2);
		assertTrue(output, output.contains("is a Integer[] array:"));
		assertTrue(output, output.contains("[0] = " + x1));
		assertTrue(output, output.contains("[1] = " + x2));
	}

	@Test
	public void testDoOperationNull() throws Exception {
		String output = getClientOutput(client, "do " + objectName + " returnNull");
		assertTrue(output, output.matches("(?s).*do 'returnNull' in \\d+ms = null.*"));
	}

	@Test
	public void testDoOperationThrow() throws Exception {
		String output = getClientOutput(client, "do " + objectName + " doThrow");
		assertTrue(output, output.matches("(?s).*throw away.*"));
	}

	@Test
	public void testDoLinesOperation() throws Exception {
		int x1 = 12;
		int x2 = 654;
		String output =
				getClientOutput(client, "dolines " + objectName + " times", Integer.toString(x1), Integer.toString(x2));
		int times = x1 * x2;
		assertTrue(output, output.matches("(?s).*dolines 'times' in \\d+ms = " + times + ".*"));
	}

	@Test
	public void testDoLinesBadName() throws Exception {
		String output = getClientOutput(client, "dolines bad-object-name foo");
		assertTrue(output, output.matches("(?s).*Invalid object name.*"));
	}

	@Test
	public void testDoLinesNoArg() throws Exception {
		String output = getClientOutput(client, "dolines " + objectName);
		assertTrue(output, output.matches("(?s).*Usage: dolines.*"));
	}

	@Test
	public void testHelp() throws Exception {
		String output = getClientOutput(client, "help");
		assertTrue(output, output.matches("(?s).*execute a script.*"));
	}

	@Test
	public void testExamples() throws Exception {
		String output = getClientOutput(client, "examples");
		assertTrue(output, output.matches("(?s).*do something like the following.*"));
	}

	@Test
	public void testSleep() throws Exception {
		long before = System.currentTimeMillis();
		int ms = 434;
		getClientOutput(client, "sleep " + ms);
		assertTrue(System.currentTimeMillis() - before >= ms);
	}

	@Test
	public void testSleepNoArg() throws Exception {
		String output = getClientOutput(client, "sleep");
		assertTrue(output, output.matches("(?s).*Usage: sleep.*"));
	}

	@Test
	public void testSleepInvalidNumber() throws Exception {
		String output = getClientOutput(client, "sleep not-a-number");
		assertTrue(output, output.matches("(?s).*invalid millis number.*"));
	}

	@Test
	public void testInvalidCommand() throws Exception {
		String output = getClientOutput(client, "jopfewjfwepfjwe");
		assertTrue(output, output.matches("(?s).*Unknown command.*"));
	}

	@Test
	public void testBlankLine() throws Exception {
		String output = getClientOutput(client, "");
		assertEquals("", output);
	}

	@Test
	public void testComment() throws Exception {
		String output = getClientOutput(client, "#");
		assertEquals("", output);
	}

	@Test
	public void testRunNoArg() throws Exception {
		String output = getClientOutput(client, "run");
		assertTrue(output, output.matches("(?s).*Usage: run.*"));
	}

	@Test
	public void testRunNoFile() throws Exception {
		String output = getClientOutput(client, "run some-file-that-doesnt-exist");
		assertTrue(output, output.matches("(?s).*Script file is not found.*"));
	}

	/* ============================= */

	private String getClientOutput(CommandLineJmxClient client, String command) throws Exception {
		return getClientOutput(client, new String[] { command });
	}

	private String getClientOutput(CommandLineJmxClient client, String... commands) throws Exception {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		System.setOut(new PrintStream(array));
		client.runCommands(commands);
		return new String(array.toByteArray());
	}

	@JmxResource(domainName = JMX_DOMAIN)
	protected static class OurJmxClass {
		int x;
		@JmxAttribute
		public void setX(int x) {
			this.x = x;
		}
		@JmxAttribute
		public int getX() {
			return x;
		}
		@JmxOperation
		public int times(int x1, int x2) {
			return x1 * x2;
		}
		@JmxOperation
		public byte[] byteArray(byte x1, byte x2) {
			return new byte[] { x1, x2 };
		}
		@JmxOperation
		public short[] shortArray(short x1, short x2) {
			return new short[] { x1, x2 };
		}
		@JmxOperation
		public int[] intArray(int x1, int x2) {
			return new int[] { x1, x2 };
		}
		@JmxOperation
		public long[] longArray(long x1, long x2) {
			return new long[] { x1, x2 };
		}
		@JmxOperation
		public boolean[] booleanArray(boolean x1, boolean x2) {
			return new boolean[] { x1, x2 };
		}
		@JmxOperation
		public char[] charArray(char x1, char x2) {
			return new char[] { x1, x2 };
		}
		@JmxOperation
		public float[] floatArray(float x1, float x2) {
			return new float[] { x1, x2 };
		}
		@JmxOperation
		public double[] doubleArray(double x1, double x2) {
			return new double[] { x1, x2 };
		}
		@JmxOperation
		public Integer[] objectArray(int x1, int x2) {
			return new Integer[] { x1, x2 };
		}
		@JmxOperation
		public void doThrow() {
			throw new RuntimeException("throw away!");
		}
		@JmxOperation
		public Object returnNull() {
			return null;
		}
	}
}
