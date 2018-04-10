package net.floodlightcontroller.demomodule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.demomodule.bean.PortInfo;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.OFMessageUtils;

public class DemoModule implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected ITopologyService topology;
	protected static Logger logger;
	protected static final short APP_ID = 101;
	private final static int MAX_CHANGE_TIME = 10;
	static {
		AppCookie.registerApp(APP_ID, "DemoModule");
	}
	protected static final U64 FORWARD_ONLY_COOKIE = AppCookie.makeCookie(APP_ID, 0xaaaaaaL);
	protected static final int PACKET_MAX = 3;
	protected Map<String, PortInfo> portInfos;
	public static List<String> portKeyWithLinks = new ArrayList<String>();
	protected boolean DEBUG = true;

	@Override
	public String getName() {
		return DemoModule.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return ((type == OFType.PACKET_IN || type == OFType.FLOW_MOD) && name.equals("topology"));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		topology = context.getServiceImpl(ITopologyService.class);
		logger = LoggerFactory.getLogger(DemoModule.class);
		portInfos = new HashMap<String, PortInfo>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
		case PACKET_IN:
			IRoutingDecision decision = null;
			if (cntx == null) {
				logger.warn("DemoModule error: FloodlightContext is null.");
			} else {
				decision = IRoutingDecision.rtStore.get(cntx, IRoutingDecision.CONTEXT_DECISION);
				return this.processPacketInMessage(sw, (OFPacketIn) msg, decision, cntx);
			}
			break;
		default:
			break;
		}
		return Command.CONTINUE;
	}

	public Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, IRoutingDecision decision,
			FloodlightContext cntx) {
		boolean setDecisionFlag = false;
		OFPort inPort = OFMessageUtils.getInPort(pi);
		String portKey = sw.getId().toString() + "_" + inPort.toString();
		if (portKeyWithLinks.contains(portKey))
			return Command.CONTINUE;

		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (eth.getEtherType() == EthType.IPv4) {
			IPv4 ipv4 = (IPv4) eth.getPayload();
			IPv4Address srcIp = ipv4.getSourceAddress();
			MacAddress srcMac = eth.getSourceMACAddress();
			logger.info("##### DemoModule: new Packet-In fount on Switch {} Port {}", sw.getId().toString(),
					inPort.toString());

			if (DEBUG) {
				if (portInfos.containsKey(portKey)) {
					PortInfo portInfo = portInfos.get(portKey);
					logger.info(String.format("lastIp %s lastMac %s packetNum %d", portInfo.getLastIP().toString(),
							portInfo.getLastMac().toString(), portInfo.getPacketNum()));
				} else {
					logger.info("port info not exists");
				}
				logger.info(String.format("newIp %s newMac %s", srcIp.toString(), srcMac.toString()));
			}

			if (!portInfos.containsKey(portKey) || !portInfos.get(portKey).getLastMac().equals(srcMac)
					|| !portInfos.get(portKey).getLastIP().equals(srcIp)) {
				logger.info("port info is not same as before");
				if (!portInfos.containsKey(portKey)) {
					logger.info("port info empty, add new");
					PortInfo portInfo = new PortInfo();
					portInfo.setLastMac(srcMac);
					portInfo.setLastIP(srcIp);
					portInfo.setPacketNum(0);
					portInfo.setChangedCount(0);
					portInfos.put(portKey, portInfo);
				} else {
					portInfos.get(portKey).setPacketNum(0);
					portInfos.get(portKey).setChangedCount(portInfos.get(portKey).getChangedCount() + 1);
				}
				if (portInfos.get(portKey).getChangedCount() < MAX_CHANGE_TIME)
					return Command.CONTINUE;
				setDecisionFlag = new Random().nextBoolean();
			} else {
				logger.info("port info is same as before");
				logger.info("increase packetnum to " + (portInfos.get(portKey).getPacketNum() + 1));
				portInfos.get(portKey).setPacketNum(portInfos.get(portKey).getPacketNum() + 1);
				if (portInfos.get(portKey).getPacketNum() < PACKET_MAX && new Random().nextBoolean()) {
					logger.info("set decision flag true 1");
					setDecisionFlag = true;
				}
			}
		}
		if (setDecisionFlag) {
			logger.info("set decision");
			decision = new RoutingDecision(sw.getId(), inPort,
					IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE),
					IRoutingDecision.RoutingAction.FORWARD_ONLY);
			decision.setDescriptor(FORWARD_ONLY_COOKIE);
			decision.addToContext(cntx);
		}
		return Command.CONTINUE;
	}

}
