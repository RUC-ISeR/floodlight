package net.floodlightcontroller.demomodule.bean;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;

public class PortInfo {

	public MacAddress getLastMac() {
		return lastMac;
	}

	public void setLastMac(MacAddress lastMac) {
		this.lastMac = lastMac;
	}

	public IPv4Address getLastIP() {
		return lastIP;
	}

	public void setLastIP(IPv4Address lastIP) {
		this.lastIP = lastIP;
	}

	public int getPacketNum() {
		return packetNum;
	}

	public void setPacketNum(int packetNum) {
		this.packetNum = packetNum;
	}

	public long getChangedCount() {
		return changedCount;
	}

	public void setChangedCount(long changedCount) {
		this.changedCount = changedCount;
	}

	private MacAddress lastMac;
	private IPv4Address lastIP;
	private int packetNum;
	private long changedCount;
}
