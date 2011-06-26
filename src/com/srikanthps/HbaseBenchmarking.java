package com.srikanthps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * A simple HBase Benchmarking tool
 * 
 * @author Srikanth P Shreenivas (srikanthps@yahoo.com)
 * 
 *         It has been tested on standalone HBase 0.20.6.
 * 
 *         You need to have table, testtable, in your HBase. Create it in HBase
 *         shell using the command: create "testtable", "data"
 */
public class HbaseBenchmarking {

	private static final int LABEL_RIGHT_PAD = 12;
	private static final int VALUE_LEFT_PAD = 11;

	public static SynchronizedDescriptiveStatistics timingStats = new SynchronizedDescriptiveStatistics();

	private static HBaseConfiguration hbaseConfiguration;
	static {
		hbaseConfiguration = new HBaseConfiguration();
		hbaseConfiguration.set("hbase.rootdir", "file:/tmp/hbase-root/hbase");
		hbaseConfiguration.set("hbase.master.port", "60000");
		hbaseConfiguration.set("hbase.zookeeper.quorum", "192.168.1.3");
		hbaseConfiguration.set("hbase.client.write.buffer", "2097152");
	}

	public static void main(String[] args) throws InterruptedException, InstantiationException, IllegalAccessException {

		System.out.println("Simple HBase benchmarking application");

		long startingRowKey = 600000000000L;

		System.out.println(StringUtils.rightPad("Scenario,", 50) + StringUtils.leftPad("Num Threads", LABEL_RIGHT_PAD)
				+ StringUtils.leftPad("Puts", LABEL_RIGHT_PAD) + StringUtils.leftPad("Mean,", LABEL_RIGHT_PAD)
				+ StringUtils.leftPad("Stdev,", LABEL_RIGHT_PAD) + StringUtils.leftPad("Max,", LABEL_RIGHT_PAD)
				+ StringUtils.leftPad("Min,", LABEL_RIGHT_PAD) + StringUtils.leftPad("80 Pctl,", LABEL_RIGHT_PAD)
				+ StringUtils.leftPad("90 Pctl,", LABEL_RIGHT_PAD));

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 100, 1, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 100, 1, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 100, 1, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 1000, 1, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 1000, 1, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 1000, 1, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 10000, 1, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 10000, 1, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 10000, 1, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 100, 10, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 100, 10, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 100, 10, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 1000, 10, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 1000, 10, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 1000, 10, DATA);
		System.out.println();

		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 10000, 10, DATA);
		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 10000, 10, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 10000, 10, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 100, 100, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 100, 100, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 100, 100, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 1000, 100, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 1000, 100, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 1000, 100, DATA);
		System.out.println();

		runTest(HTablePoolBasedRowCreator.class, startingRowKey, 10000, 100, DATA);
		runTest(HTableWithReusedHBaseConfiguration.class, startingRowKey, 10000, 100, DATA);
		runTest(HTableWithNewHBaseConfiguration.class, startingRowKey, 10000, 100, DATA);
		System.out.println();

	}

	private static void runTest(Class rowCreatorClass, long startingRowKey, int numRecordsToInsert, long numThreadsToUse, String data)
			throws InterruptedException, InstantiationException, IllegalAccessException {

		timingStats.clear();

		int numRecordsPerThread = (int) Math.ceil(((double) numRecordsToInsert) / numThreadsToUse);

		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < numThreadsToUse; i++) {
			RowCreator c = (RowCreator) rowCreatorClass.newInstance();
			c.setData(data);
			c.setNextRowkey(startingRowKey);
			c.setNumRecordsToInsert(numRecordsPerThread);

			Thread t = new Thread(c);
			threads.add(t);
			t.start();
		}

		for (int i = 0; i < numThreadsToUse; i++) {
			threads.get(i).join();
		}

		System.out.println(StringUtils.rightPad(rowCreatorClass.newInstance().toString(), 50) + ","
				+ format(numThreadsToUse, VALUE_LEFT_PAD) + "," + format(timingStats.getN(), VALUE_LEFT_PAD) + ","
				+ format(timingStats.getMean(), VALUE_LEFT_PAD) + "," + format(timingStats.getStandardDeviation(), VALUE_LEFT_PAD) + ","
				+ format(timingStats.getMax(), VALUE_LEFT_PAD) + "," + format(timingStats.getMin(), VALUE_LEFT_PAD) + ","
				+ format(timingStats.getPercentile(80.0), VALUE_LEFT_PAD) + "," + format(timingStats.getPercentile(90.0), VALUE_LEFT_PAD));
	}

	private static String format(Long d, int pad) {
		return StringUtils.leftPad(String.format("%10d", d), pad);
	}

	private static String format(Double d, int pad) {
		return StringUtils.leftPad(String.format("%.2f", d), pad);
	}

	public static abstract class RowCreator implements Runnable {

		int numRecordsToInsert;
		String data;
		long nextRowkey;

		public RowCreator() {
		}

		public void setData(String data) {
			this.data = data;
		}

		public void setNextRowkey(long nextRowkey) {
			this.nextRowkey = nextRowkey;
		}

		public void setNumRecordsToInsert(int numRecordsToInsert) {
			this.numRecordsToInsert = numRecordsToInsert;
		}

		@Override
		public void run() {
			createRows();
		}

		private void createRows() {
			HTable table = null;
			try {
				table = getHTable("testtable");

				for (int i = 0; i < numRecordsToInsert; i++) {

					long before = System.currentTimeMillis();
					putRecord(table);
					long after = System.currentTimeMillis();

					long timeForThisEntry = after - before;

					timingStats.addValue(Double.valueOf(timeForThisEntry));
				}

			} catch (Exception e) {
				System.out.println("Exception during operation" + e);
				e.printStackTrace();
			} finally {
				returnHTable(table);
			}
		}

		protected abstract HTable getHTable(String tableName);

		protected abstract void returnHTable(HTable table);

		private void putRecord(HTable table) throws IOException {
			Put p = new Put(Bytes.toBytes(String.valueOf(nextRowkey++)));
			p.add("data".getBytes(), "col1".getBytes(), data.getBytes());
			table.put(p);
		}

	}

	public static class HTablePoolBasedRowCreator extends RowCreator {

		private final static int poolsize = 100;
		private static HTablePool hTablePool = new HTablePool(hbaseConfiguration, poolsize);
		
		public void uninitialize() {
			if (hTablePool != null) {
				hTablePool = null;
			}
		}
		
		public HTablePoolBasedRowCreator() {
			super();
		}

		public HTable getHTable(String tableName) {
			return hTablePool.getTable(tableName);
		}

		public void returnHTable(HTable table) {
			if (table != null) {
				hTablePool.putTable(table);
			}
		}

		@Override
		public String toString() {
			return "HTable from HTablePool (pool size: " + poolsize + ")";
		}

	}

	public static class HTableWithReusedHBaseConfiguration extends RowCreator {

		public HTable getHTable(String tableName) {
			try {
				return new HTable(hbaseConfiguration, tableName);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		public void returnHTable(HTable table) {
			if (table != null) {
				try {
					table.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public String toString() {
			return "HTable with same configuration instance";
		}
	}

	public static class HTableWithNewHBaseConfiguration extends RowCreator {

		public HTable getHTable(String tableName) {
			try {
				return new HTable(new HBaseConfiguration(hbaseConfiguration), tableName);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		public void returnHTable(HTable table) {
			if (table != null) {
				try {
					table.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public String toString() {
			return "HTable with new Hbase configuration instance";
		}
	}
	
	
	private static final String DATA = "A huge string : Licensed to the Apache Software Foundation (ASF) under one " +
		"or more contributor license agreements. See the NOTICE file distributed with this" +
		" work for additional information regarding copyright ownership. The ASF licenses " +
		"this file to you under the Apache License, Version 2.0 (the \"License\"); you may" +
		" not use this file except in compliance with the License. You may obtain a copy of " +
		"the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable" +
		" law or agreed to in writing, software distributed under the License is distributed " +
		"on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express " +
		"or implied. See the License for the specific language governing permissions and " +
		"limitations under the License. Licensed to the Apache Software Foundation (ASF) " +
		"under one or more contributor license agreements. See the NOTICE file distributed" +
		" with this work for additional information regarding copyright ownership. The ASF " +
		" not use this file except in compliance with the License. You may obtain a copy of " +
		"the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable" +
		" law or agreed to in writing, software distributed under the License is distributed " +
		"on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express " +
		"or implied. See the License for the specific language governing permissions and " +
		"limitations under the License. Licensed to the Apache Software Foundation (ASF) " +
		"under one or more contributor license agreements. See the NOTICE file distributed" +
		" with this work for additional information regarding copyright ownership. The ASF " +
		" not use this file except in compliance with the License. You may obtain a copy of " +
		"the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable" +
		" law or agreed to in writing, software distributed under the License is distributed " +
		"on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express " +
		"or implied. See the License for the specific language governing permissions and " +
		"limitations under the License. Licensed to the Apache Software Foundation (ASF) " +
		"under one or more contributor license agreements. See the NOTICE file distributed" +
		" with this work for additional information regarding copyright ownership. The ASF " +
		" not use this file except in compliance with the License. You may obtain a copy of " +
		"the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable" +
		" law or agreed to in writing, software distributed under the License is distributed " +
		"on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express " +
		"or implied. See the License for the specific language governing permissions and " +
		"limitations under the License. Licensed to the Apache Software Foundation (ASF) " +
		"under one or more contributor license agreements. See the NOTICE file distributed" +
		" with this work for additional information regarding copyright ownership. The ASF " +
		" not use this file except in compliance with the License. You may obtain a copy of " +
		"the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable" +
		" law or agreed to in writing, software distributed under the License is distributed " +
		"on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express " +
		"or implied. See the License for the specific language governing permissions and " +
		"limitations under the License. Licensed to the Apache Software Foundation (ASF) " +
		"under one or more contributor license agreements. See the NOTICE file distributed" +
		" with this work for additional information regarding copyright ownership. The ASF " +
		"licenses this file to you under the Apache License, Version Licensed to the Apache " +
		"Software Foundation (ASF) under one or more contributor license agreements. See the " +
		"NOTICE file distributed with this work for additional information regarding copyright" +
		" ownership. The ASF licenses this file to you under the Apache License, Version 2.0 " +
		"(the \"License\"); you may not use this file except in compliance with the License.";

	
}
