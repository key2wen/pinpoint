package com.nhn.pinpoint.common.util;

import org.apache.hadoop.hbase.util.Bytes;

import com.nhn.pinpoint.common.Histogram;
import com.nhn.pinpoint.common.HistogramSlot;
import com.nhn.pinpoint.common.ServiceType;
import com.nhn.pinpoint.common.hbase.HBaseTables;

/**
 * <pre>
 * columnName format = SERVICETYPE(2bytes) + SLOT(2bytes) + APPNAMELEN(2bytes) + APPLICATIONNAME(str) + HOST(str)
 * </pre>
 * 
 * @author netspider
 * 
 */
public class ApplicationMapStatisticsUtils {

	public static byte[] makeColumnName(short serviceType, String applicationName, String destHost, int elapsed, boolean isError) {
		if (applicationName == null) {
			throw new NullPointerException("applicationName must not be null");
		}
		if (destHost == null) {
			// throw new NullPointerException("destHost must not be null");
			destHost = "";
		}
		byte[] serviceTypeBytes = Bytes.toBytes(serviceType);
		byte[] slotNumber;
		if (isError) {
			slotNumber = HBaseTables.STATISTICS_CQ_ERROR_SLOT;
		} else {
			slotNumber = findResponseHistogramSlotNo(serviceType, elapsed);
		}
		byte[] applicationNameBytes = Bytes.toBytes(applicationName);
		byte[] applicationNameLenBytes = Bytes.toBytes((short) applicationNameBytes.length);
		byte[] destHostBytes = Bytes.toBytes(destHost);

		return BytesUtils.concat(serviceTypeBytes, slotNumber, applicationNameLenBytes, applicationNameBytes, destHostBytes);
	}
	
	public static byte[] makeColumnName(short serviceType, String agentId, int elapsed, boolean isError) {
		if (agentId == null) {
			agentId = "";
		}
		byte[] slotNumber;
		if (isError) {
			slotNumber = HBaseTables.STATISTICS_CQ_ERROR_SLOT;
		} else {
			slotNumber = findResponseHistogramSlotNo(serviceType, elapsed);
		}
		byte[] agentIdBytes = Bytes.toBytes(agentId);
		
		return BytesUtils.concat(slotNumber, agentIdBytes);
	}

	private static byte[] findResponseHistogramSlotNo(short serviceType, int elapsed) {
		Histogram histogram = ServiceType.findServiceType(serviceType).getHistogram();
		HistogramSlot histogramSlot = histogram.findHistogramSlot(elapsed);
		short slotTime = (short) histogramSlot.getSlotTime();
		return Bytes.toBytes(slotTime);
	}

	public static short getDestServiceTypeFromColumnName(byte[] bytes) {
		return BytesUtils.bytesToShort(bytes, 0);
	}

	/**
	 * 
	 * @param bytes
	 * @return <pre>
	 * 0 > : ms
	 * 0 : slow
	 * -1 : error
	 * </pre>
	 */
	public static short getHistogramSlotFromColumnName(byte[] bytes) {
		return BytesUtils.bytesToShort(bytes, 2);
	}

	public static String getDestApplicationNameFromColumnName(byte[] bytes) {
		return new String(bytes, 6, BytesUtils.bytesToShort(bytes, 4)).trim();
	}

	public static String getHost(byte[] bytes) {
		int offset = 6 + BytesUtils.bytesToShort(bytes, 4);

		if (offset == bytes.length) {
			return null;
		}

		return new String(bytes, offset, bytes.length - offset).trim();
	}

	/**
	 * <pre>
	 * rowkey format = "APPLICATIONNAME(max 24bytes)" + apptype(2byte) + "TIMESTAMP(8byte)"
	 * </pre>
	 * 
	 * @param applicationName
	 * @param timestamp
	 * @return
	 */
	public static byte[] makeRowKey(String applicationName, short applicationType, long timestamp) {
		if (applicationName == null) {
			throw new NullPointerException("applicationName must not be null");
		}

		byte[] applicationnameBytes = Bytes.toBytes(applicationName);
		byte[] applicationnameBytesLength = Bytes.toBytes((short) applicationnameBytes.length);
		// byte[] offset = new byte[HBaseTables.APPLICATION_NAME_MAX_LEN - applicationnameBytes.length];
		byte[] applicationtypeBytes = Bytes.toBytes(applicationType);
		byte[] slot = Bytes.toBytes(TimeUtils.reverseCurrentTimeMillis(timestamp));

		return BytesUtils.concat(applicationnameBytesLength, applicationnameBytes, applicationtypeBytes, slot);
	}

	public static String getApplicationNameFromRowKey(byte[] bytes) {
		short applicationNameLength = BytesUtils.bytesToShort(bytes, 0);
		byte[] temp = new byte[applicationNameLength];
		System.arraycopy(bytes, 2, temp, 0, applicationNameLength);
		return new String(temp); //.trim();
	}

	public static short getApplicationTypeFromRowKey(byte[] bytes) {
		short applicationNameLength = BytesUtils.bytesToShort(bytes, 0);
		return BytesUtils.bytesToShort(bytes, applicationNameLength + 2);
	}
	
	public static long getTimestampFromRowKey(byte[] bytes) {
		short applicationNameLength = BytesUtils.bytesToShort(bytes, 0);
		return TimeUtils.recoveryCurrentTimeMillis(BytesUtils.bytesToLong(bytes, applicationNameLength + 4));
	}
}
