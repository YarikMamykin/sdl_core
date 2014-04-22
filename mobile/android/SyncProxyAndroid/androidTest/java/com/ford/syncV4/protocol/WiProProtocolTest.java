package com.ford.syncV4.protocol;

import android.test.InstrumentationTestCase;

import com.ford.syncV4.protocol.enums.FrameDataControlFrameType;
import com.ford.syncV4.protocol.enums.FrameType;
import com.ford.syncV4.protocol.enums.ServiceType;
import com.ford.syncV4.proxy.constants.ProtocolConstants;
import com.ford.syncV4.service.Service;
import com.ford.syncV4.session.Session;
import com.ford.syncV4.util.BitConverter;
import com.ford.syncV4.util.logger.Logger;

import junit.framework.Assert;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Andrew Batutin on 8/21/13
 */
public class WiProProtocolTest extends InstrumentationTestCase {

    public static final int MESSAGE_ID = 1;
    public static final byte SESSION_ID = (byte) 48;
    public static final byte FRAME_SEQUENCE_NUMBER = (byte) 1;
    public static final int FRAME_SIZE_SHIFT = 100;
    public static final IProtocolListener DUMMY_PROTOCOL_LISTENER =
            new IProtocolListener() {
                @Override
                public void onProtocolMessageBytesToSend(byte[] msgBytes,
                                                         int offset,
                                                         int length) {
                }

                @Override
                public void onProtocolMessageReceived(ProtocolMessage msg) {
                }

                @Override
                public void onProtocolSessionStarted(Session session,
                                                     byte version,
                                                     String correlationID) {
                }

                @Override
                public void onProtocolServiceEnded(ServiceType serviceType,
                                                   byte sessionID,
                                                   String correlationID) {
                }

                @Override
                public void onProtocolHeartbeatACK() {
                }

                @Override
                public void onResetHeartbeat() {

                }

                @Override
                public void onProtocolError(String info, Exception e) {
                }

                @Override
                public void onMobileNavAckReceived(int frameReceivedNumber) {

                }

                @Override
                public void onProtocolAppUnregistered() {

                }

                @Override
                public void onProtocolServiceStarted(ServiceType serviceType, byte sessionID, byte version, String correlationID) {

                }

                @Override
                public void onStartServiceNackReceived(ServiceType serviceType) {

                }
            };
    private static final String TAG = WiProProtocolTest.class.getSimpleName();
    Method currentCheckMethod;
    private WiProProtocol wiProProtocol;
    private ProtocolFrameHeader currentFrameHeader;
    private byte[] currentData;

    public WiProProtocolTest() {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        wiProProtocol = new WiProProtocol(mock(IProtocolListener.class)) {

            @Override
            public void SendMessage(ProtocolMessage protocolMsg) {
                prepareMockItems();
                super.SendMessage(protocolMsg);
            }

            private void prepareMockItems() {
                _messageLocks = mock(Hashtable.class);
                when(_messageLocks.get(anyByte())).thenReturn("mockLock");
                doThrow(new IllegalStateException("should not get protocol error")).when(_protocolListener).onProtocolError(anyString(), any(Exception.class));
            }

            @Override
            protected void handleProtocolFrameToSend(ProtocolFrameHeader header, byte[] data, int offset, int length) {
                super.handleProtocolFrameToSend(header, data, offset, length);
                callCheck(currentCheckMethod, data, header, offset, length);
            }
        };
        wiProProtocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
    }

    public void testSendMobileNavSmallFrameProtocolMessageSucceed() throws Exception {
        ProtocolMessage message = generateMobileNavProtocolMessage(8);
        currentData = generateByteArray(0, 8);
        currentFrameHeader =
                ProtocolFrameHeaderFactory.createSingleSendData(ServiceType.Mobile_Nav, SESSION_ID,
                        currentData.length, MESSAGE_ID, ProtocolConstants.PROTOCOL_VERSION_THREE);
        currentCheckMethod = generateCurrentCheckMethod("checkCurrentArgumentsSmallFrame");
        wiProProtocol.SendMessage(message);
    }

    public void testSendMobileNavFirstBigFrameProtocolMessageSucceed() throws Exception {
        ProtocolMessage message = generateMobileNavProtocolMessage(WiProProtocol.MAX_DATA_SIZE * 2);
        currentData = generateByteArray(0, WiProProtocol.MAX_DATA_SIZE * 2);
        currentFrameHeader =
                ProtocolFrameHeaderFactory.createMultiSendDataFirst(ServiceType.Mobile_Nav,
                        SESSION_ID, MESSAGE_ID, ProtocolConstants.PROTOCOL_VERSION_THREE);
        currentCheckMethod = generateCurrentCheckMethod("checkCurrentArgumentsFirstBigFrame");
        wiProProtocol.SendMessage(message);
    }

    public void testSendMobileNavConsecutiveBigFrameProtocolMessageSucceed() throws Exception {
        ProtocolMessage message = generateMobileNavProtocolMessage(WiProProtocol.MAX_DATA_SIZE * 3);
        currentData = generateByteArray(0, WiProProtocol.MAX_DATA_SIZE);
        currentFrameHeader =
                ProtocolFrameHeaderFactory.createMultiSendDataRest(ServiceType.Mobile_Nav,
                        SESSION_ID, currentData.length, FRAME_SEQUENCE_NUMBER, MESSAGE_ID,
                        ProtocolConstants.PROTOCOL_VERSION_THREE);
        currentCheckMethod = generateCurrentCheckMethod("checkCurrentArgumentsSecondBigFrame");
        wiProProtocol.SendMessage(message);
    }

    public void testSendMobileNavLastBigFrameProtocolMessageSucceed() throws Exception {
        ProtocolMessage message = generateMobileNavProtocolMessage(WiProProtocol.MAX_DATA_SIZE * 3);
        currentData = generateByteArray(WiProProtocol.MAX_DATA_SIZE * 2, WiProProtocol.MAX_DATA_SIZE);
        currentFrameHeader =
                ProtocolFrameHeaderFactory.createMultiSendDataRest(ServiceType.Mobile_Nav,
                        SESSION_ID, currentData.length, (byte) 0, MESSAGE_ID,
                        ProtocolConstants.PROTOCOL_VERSION_THREE);
        currentCheckMethod = generateCurrentCheckMethod("checkCurrentArgumentsLastBigFrame");
        wiProProtocol.SendMessage(message);
    }

    public void testSendMobileNabLastUnAlightedBigFrameProtocolMessageSucceed() throws Exception {
        ProtocolMessage message =
                generateMobileNavProtocolMessage(WiProProtocol.MAX_DATA_SIZE * 3 + FRAME_SIZE_SHIFT);
        currentData = generateByteArray(WiProProtocol.MAX_DATA_SIZE * 3, FRAME_SIZE_SHIFT);
        currentFrameHeader =
                ProtocolFrameHeaderFactory.createMultiSendDataRest(ServiceType.Mobile_Nav,
                        SESSION_ID, currentData.length, (byte) 0, MESSAGE_ID,
                        ProtocolConstants.PROTOCOL_VERSION_THREE);
        wiProProtocol.set_TEST_ProtocolMaxVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        wiProProtocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        currentCheckMethod = generateCurrentCheckMethod("checkCurrentArgumentsLastUnAlightedBigFrame");
        wiProProtocol.SendMessage(message);
    }

    private Method generateCurrentCheckMethod(String checkMethodName) throws NoSuchMethodException {
        Class[] parameterTypes = new Class[4];
        parameterTypes[0] = byte[].class;
        parameterTypes[1] = ProtocolFrameHeader.class;
        parameterTypes[2] = int.class;
        parameterTypes[3] = int.class;
        return WiProProtocolTest.class.getMethod(checkMethodName, parameterTypes);
    }

    private ProtocolMessage generateMobileNavProtocolMessage(int i) {
        byte[] frame = generateByteArray(0, i);
        ProtocolMessage message = new ProtocolMessage();
        message.setData(frame);
        message.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        message.setSessionID(SESSION_ID);
        message.setServiceType(ServiceType.Mobile_Nav);
        return message;
    }

    private ProtocolMessage generateRPCProtocolMessage(int dataSize, int bulkDataSize) {
        ProtocolMessage message = new ProtocolMessage();
        if (dataSize > 0) {
            message.setData(generateByteArray(0, dataSize));
        }
        if (bulkDataSize > 0) {
            message.setBulkData(generateByteArray(0, bulkDataSize));
        }
        message.setVersion(ProtocolConstants.PROTOCOL_VERSION_TWO);
        message.setSessionID(SESSION_ID);
        message.setServiceType(ServiceType.RPC);
        return message;
    }

    private byte[] generateByteArray(int offset, int size) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) (i + offset);
        }
        return b;
    }

    private void callCheck(Method method, byte[] data, ProtocolFrameHeader messageHeader, int offset, int length) {
        Object[] parameters = new Object[4];
        parameters[0] = data;
        parameters[1] = messageHeader;
        parameters[2] = offset;
        parameters[3] = length;
        try {
            method.invoke(WiProProtocolTest.this, parameters);
        } catch (IllegalAccessException e) {
            Logger.e("WiProProtocolTest", e.toString());
        } catch (InvocationTargetException e) {
            reconstructAssertionError(e);
        }
    }

    private void reconstructAssertionError(InvocationTargetException e) {
        String methodName = getMethodName(e);
        assertNull("Should not get here. " + e.getCause().getMessage() + " " + methodName, e);
    }

    private String getMethodName(InvocationTargetException e) {
        String methodName = "";
        if (e.getCause() != null && e.getCause().getStackTrace() != null && e.getCause().getStackTrace().length > 0) {
            for (int i = 0; i < e.getCause().getStackTrace().length; i++) {
                if (e.getCause().getStackTrace()[i].toString().contains(this.getClass().getName())) {
                    methodName = e.getCause().getStackTrace()[i].toString();
                    break;
                }
            }
        }
        return methodName;
    }

    public void checkCurrentArgumentsSmallFrame(byte[] data, ProtocolFrameHeader messageHeader, int offset, int length) throws Exception {
        assertTrue(Arrays.equals(currentData, data));
        assertEquals("ServiceType should be equal.", currentFrameHeader.getServiceType(), messageHeader.getServiceType());
        assertEquals("FrameType should be equal.", currentFrameHeader.getFrameType(), messageHeader.getFrameType());
        assertEquals("FrameData should be equal.", currentFrameHeader.getFrameData(), messageHeader.getFrameData());
        assertEquals("Version should be equal.", currentFrameHeader.getVersion(), messageHeader.getVersion());
        assertEquals("Compressed state should be equal.", currentFrameHeader.isCompressed(), messageHeader.isCompressed());
        assertEquals("Frame headers should be equal.", currentFrameHeader.getDataSize(), messageHeader.getDataSize());
        assertEquals("DataSize should be equal.", currentFrameHeader.getMessageID(), messageHeader.getMessageID());
        assertEquals("Frame headers should be equal.", currentFrameHeader.getSessionID(), messageHeader.getSessionID());
    }

    public void checkCurrentArgumentsFirstBigFrame(byte[] data, ProtocolFrameHeader messageHeader, int offset, int length) throws Exception {
        if (messageHeader.getFrameType() == FrameType.First) {
            byte[] firstFrameData = getFirstFrameData(currentData);
            assertTrue("Arrays should be equal.", Arrays.equals(firstFrameData, data));
            assertEquals("ServiceType should be equal.", currentFrameHeader.getServiceType(), messageHeader.getServiceType());
            assertEquals("FrameType should be equal.", currentFrameHeader.getFrameType(), messageHeader.getFrameType());
            assertEquals("FrameData should be equal.", currentFrameHeader.getFrameData(), messageHeader.getFrameData());
            assertEquals("Version should be equal.", currentFrameHeader.getVersion(), messageHeader.getVersion());
            assertEquals("Compressed state should be equal.", currentFrameHeader.isCompressed(), messageHeader.isCompressed());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getDataSize(), messageHeader.getDataSize());
            assertEquals("DataSize should be equal.", currentFrameHeader.getMessageID(), messageHeader.getMessageID());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getSessionID(), messageHeader.getSessionID());
        }
    }

    public void checkCurrentArgumentsSecondBigFrame(byte[] data, ProtocolFrameHeader messageHeader, int offset, int length) throws Exception {
        if (messageHeader.getFrameType() == FrameType.Consecutive && messageHeader.getFrameData() == (byte) 1) {
            assertTrue("Length of data should be less then WiProProtocol.MAX_DATA_SIZE", length <= WiProProtocol.MAX_DATA_SIZE);
            byte[] res = getDataToCheck(data, offset, length);
            assertTrue("Arrays should be equal.", Arrays.equals(currentData, res));
            assertTrue("Offset should be 0 for second frame", offset == 0);
            assertEquals("ServiceType should be equal.", currentFrameHeader.getServiceType(), messageHeader.getServiceType());
            assertEquals("FrameType should be equal.", currentFrameHeader.getFrameType(), messageHeader.getFrameType());
            assertEquals("FrameData should be equal.", currentFrameHeader.getFrameData(), messageHeader.getFrameData());
            assertEquals("Version should be equal.", currentFrameHeader.getVersion(), messageHeader.getVersion());
            assertEquals("Compressed state should be equal.", currentFrameHeader.isCompressed(), messageHeader.isCompressed());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getDataSize(), messageHeader.getDataSize());
            assertEquals("DataSize should be equal.", currentFrameHeader.getMessageID(), messageHeader.getMessageID());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getSessionID(), messageHeader.getSessionID());
        }
    }

    private byte[] getDataToCheck(byte[] data, int offset, int length) {
        byte[] res = new byte[length];
        System.arraycopy(data, offset, res, 0, length);
        return res;
    }

    public void checkCurrentArgumentsLastBigFrame(byte[] data, ProtocolFrameHeader messageHeader, int offset, int length) throws Exception {
        if (messageHeader.getFrameType() == FrameType.Consecutive && messageHeader.getFrameData() == (byte) 0) {
            assertTrue("Length of data should be less then WiProProtocol.MAX_DATA_SIZE", length <= WiProProtocol.MAX_DATA_SIZE);
            byte[] res = getDataToCheck(data, offset, length);
            assertTrue("Arrays should be equal.", Arrays.equals(currentData, res));
            assertTrue("Offset should be 2976 for last frame", offset == WiProProtocol.MAX_DATA_SIZE * 3 - length);
            assertEquals("ServiceType should be equal.", currentFrameHeader.getServiceType(), messageHeader.getServiceType());
            assertEquals("FrameType should be equal.", currentFrameHeader.getFrameType(), messageHeader.getFrameType());
            assertEquals("FrameData should be equal.", currentFrameHeader.getFrameData(), messageHeader.getFrameData());
            assertEquals("Version should be equal.", currentFrameHeader.getVersion(), messageHeader.getVersion());
            assertEquals("Compressed state should be equal.", currentFrameHeader.isCompressed(), messageHeader.isCompressed());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getDataSize(), messageHeader.getDataSize());
            assertEquals("DataSize should be equal.", currentFrameHeader.getMessageID(), messageHeader.getMessageID());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getSessionID(), messageHeader.getSessionID());
        }
    }

    public void checkCurrentArgumentsLastUnAlightedBigFrame(byte[] data, ProtocolFrameHeader messageHeader, int offset, int length) throws Exception {
        if (messageHeader.getFrameType() == FrameType.Consecutive && messageHeader.getFrameData() == (byte) 0) {
            assertTrue("Length of data should be == 100", length == FRAME_SIZE_SHIFT);
            assertTrue("Offset of data should be == 4464", offset == WiProProtocol.MAX_DATA_SIZE * 3);
            byte[] res = getDataToCheck(data, offset, length);
            assertTrue("Arrays should be equal.", Arrays.equals(currentData, res));
            assertEquals("ServiceType should be equal.", currentFrameHeader.getServiceType(), messageHeader.getServiceType());
            assertEquals("FrameType should be equal.", currentFrameHeader.getFrameType(), messageHeader.getFrameType());
            assertEquals("FrameData should be equal.", currentFrameHeader.getFrameData(), messageHeader.getFrameData());
            assertEquals("Version should be equal.", currentFrameHeader.getVersion(), messageHeader.getVersion());
            assertEquals("Compressed state should be equal.", currentFrameHeader.isCompressed(), messageHeader.isCompressed());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getDataSize(), messageHeader.getDataSize());
            assertEquals("DataSize should be equal.", currentFrameHeader.getMessageID(), messageHeader.getMessageID());
            assertEquals("Frame headers should be equal.", currentFrameHeader.getSessionID(), messageHeader.getSessionID());
        }
    }

    private byte[] getFirstFrameData(byte[] data) {
        int frameCount = data.length / WiProProtocol.MAX_DATA_SIZE;
        if (data.length % WiProProtocol.MAX_DATA_SIZE > 0) {
            frameCount++;
        }
        byte[] firstFrameData = new byte[8];
        // First four bytes are data size.
        System.arraycopy(BitConverter.intToByteArray(data.length), 0, firstFrameData, 0, 4);
        // Second four bytes are frame count.
        System.arraycopy(BitConverter.intToByteArray(frameCount), 0, firstFrameData, 4, 4);
        return firstFrameData;
    }

    public byte[] extractByteArrayPart(final byte[] src, int offset,
                                       int length) {
        final byte[] dst = new byte[length];
        System.arraycopy(src, offset, dst, 0, length);
        return dst;
    }

    public void testReadingHashIDFromStartSessionACK() throws Throwable {
        // null as a listener won't work
        final WiProProtocol protocol = new WiProProtocol(DUMMY_PROTOCOL_LISTENER);

        final ByteArrayOutputStream StartSessionACKMessageStream =
                new ByteArrayOutputStream(ProtocolConstants.PROTOCOL_FRAME_HEADER_SIZE_V_2);

        final byte[] msgFirstBytes = new byte[]{0x20, 0x07, 0x02, 0x00};
        StartSessionACKMessageStream.write(msgFirstBytes);
        final byte[] msgDataSize = new byte[]{0x00, 0x00, 0x00, 0x00};
        StartSessionACKMessageStream.write(msgDataSize);
        final byte[] msgHashID = new byte[]{0x12, 0x34, (byte) 0xCD, (byte) 0xEF};
        StartSessionACKMessageStream.write(msgHashID);

        final byte[] StartSessionACKMessage = StartSessionACKMessageStream.toByteArray();
        protocol.HandleReceivedBytes(StartSessionACKMessage, StartSessionACKMessage.length);
        Assert.assertEquals("HashID is incorrect", 0x1234CDEF, protocol.hashID);
    }

    public void testSendingHashIDWithEndSession() throws IOException {
        final IProtocolListener protocolListener = new IProtocolListener() {
            private int sendCount = 0;

            @Override
            public void onProtocolMessageBytesToSend(byte[] msgBytes,
                                                     int offset, int length) {
                // This method is called twice, sending header and then data
                final byte[] expectedMsgMessageID =
                        new byte[]{(byte) 0xCD, (byte) 0xEF, 0x12, 0x34};

                switch (sendCount) {
                    case 0:
                        final byte[] msgDataSize =
                                extractByteArrayPart(msgBytes, 4, 4);
                        final byte[] expectedMsgDataSize =
                                new byte[]{0x00, 0x00, 0x00, 0x04};
                        Assert.assertEquals("Data Size is incorrect",
                                BitConverter
                                        .intFromByteArray(expectedMsgDataSize,
                                                0),
                                BitConverter.intFromByteArray(msgDataSize, 0));

                        final byte[] msgMessageID =
                                extractByteArrayPart(msgBytes, 8, 4);
                        Assert.assertEquals("Message ID should be hash ID",
                                BitConverter
                                        .intFromByteArray(expectedMsgMessageID,
                                                0),
                                BitConverter.intFromByteArray(msgMessageID, 0));
                        break;

                    case 1:
                        final byte[] msgData =
                                extractByteArrayPart(msgBytes, 0, 4);
                        final byte[] expectedMsgData = expectedMsgMessageID;
                        assertEquals("Data should contain hash ID", BitConverter
                                .intFromByteArray(expectedMsgData, 0),
                                BitConverter.intFromByteArray(msgData, 0));
                        break;

                    default:
                        Assert.assertTrue(String.format(
                                "onProtocolMessageBytesToSend is called too many times: %d",
                                sendCount), false);
                }

                ++sendCount;
            }

            @Override
            public void onProtocolMessageReceived(ProtocolMessage msg) {
            }

            @Override
            public void onProtocolSessionStarted(Session session,
                                                 byte version,
                                                 String correlationID) {
            }

            @Override
            public void onProtocolServiceEnded(ServiceType sessionType,
                                               byte sessionID,
                                               String correlationID) {
            }

            @Override
            public void onProtocolHeartbeatACK() {
            }

            @Override
            public void onResetHeartbeat() {

            }

            @Override
            public void onProtocolError(String info, Exception e) {
            }

            @Override
            public void onMobileNavAckReceived(int frameReceivedNumber) {

            }

            @Override
            public void onProtocolAppUnregistered() {

            }

            @Override
            public void onProtocolServiceStarted(ServiceType serviceType, byte sessionID, byte version, String correlationID) {

            }

            @Override
            public void onStartServiceNackReceived(ServiceType serviceType) {

            }
        };

        final WiProProtocol protocol = new WiProProtocol(protocolListener);
        protocol.hashID = 0xCDEF1234;
        protocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        protocol.EndProtocolService(ServiceType.RPC, (byte) 0x01);
    }

    public void testEndSessionACKFrameReceived() throws Exception {
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.EndServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setServiceType(ServiceType.RPC);
        frameHeader.setDataSize(0);
        IProtocolListener mock = mock(IProtocolListener.class);
        WiProProtocol.MessageFrameAssembler messageFrameAssembler = new WiProProtocol(mock).new MessageFrameAssembler();
        ArgumentCaptor<ServiceType> sessionTypeCaptor = ArgumentCaptor.forClass(ServiceType.class);
        ArgumentCaptor<Byte> sessionIdCaptor = ArgumentCaptor.forClass(byte.class);
        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        messageFrameAssembler.handleFrame(frameHeader, new byte[0]);
        Mockito.verify(mock).onProtocolServiceEnded(sessionTypeCaptor.capture(), sessionIdCaptor.capture(), correlationIdCaptor.capture());
        assertEquals(ServiceType.RPC, sessionTypeCaptor.getValue());
        assertEquals(SESSION_ID, sessionIdCaptor.getValue().byteValue());
        assertEquals("", correlationIdCaptor.getValue());
    }

    public void testStartServiceWithSessionId() throws Exception {
        final byte id = 13;
        WiProProtocol protocol = new WiProProtocol(mock(IProtocolListener.class)) {
            @Override
            protected void handleProtocolFrameToSend(ProtocolFrameHeader header, byte[] data, int offset, int length) {
                super.handleProtocolFrameToSend(header, data, offset, length);
                assertEquals("Session ID should be same", id, header.getSessionID());
            }
        };
        Session session = new Session();
        session.setSessionId(id);
        protocol.StartProtocolService(ServiceType.Mobile_Nav, session);
    }

    public void testStartSessionWithSessionId() throws Exception {
        final byte id = 13;
        WiProProtocol protocol = new WiProProtocol(mock(IProtocolListener.class)) {
            @Override
            protected void handleProtocolFrameToSend(ProtocolFrameHeader header, byte[] data, int offset, int length) {
                super.handleProtocolFrameToSend(header, data, offset, length);
                assertEquals("Session ID should be same", id, header.getSessionID());
            }
        };
        protocol.StartProtocolSession(id);
    }

    public void testStartSessionNavigationWith0SessionIDThrowsExp() throws Exception {
        WiProProtocol protocol = new WiProProtocol(mock(IProtocolListener.class));
        try {
            Session session = new Session();
            session.setSessionId((byte) 0);
            protocol.StartProtocolService(ServiceType.Mobile_Nav, session);
            assertTrue("Should not get here", false);
        } catch (IllegalArgumentException exp) {
            assertNotNull("Should get and exception", exp);
        }
    }

    public void testHandleProtocolSessionStartedYieldsService() throws Exception {
        final boolean[] passed = {false};
        WiProProtocol protocol = new WiProProtocol(new IProtocolListener() {
            @Override
            public void onProtocolMessageBytesToSend(byte[] msgBytes, int offset, int length) {

            }

            @Override
            public void onProtocolMessageReceived(ProtocolMessage msg) {

            }

            @Override
            public void onProtocolSessionStarted(Session session, byte version, String correlationID) {
                assertEquals("currentSession id should be SESSION_ID", SESSION_ID, session.getSessionId());
                Service service = session.getServiceList().get(0);
                assertEquals("should be RPC service", ServiceType.RPC, service.getServiceType());
                assertEquals("service should belong to the currentSession", session, service.getSession());
                passed[0] = true;
            }

            @Override
            public void onProtocolServiceEnded(ServiceType serviceType, byte sessionID, String correlationID) {

            }

            @Override
            public void onProtocolHeartbeatACK() {

            }

            @Override
            public void onResetHeartbeat() {

            }

            @Override
            public void onProtocolError(String info, Exception e) {

            }

            @Override
            public void onMobileNavAckReceived(int frameReceivedNumber) {

            }

            @Override
            public void onProtocolAppUnregistered() {

            }

            @Override
            public void onProtocolServiceStarted(ServiceType serviceType, byte sessionID, byte version, String correlationID) {

            }

            @Override
            public void onStartServiceNackReceived(ServiceType serviceType) {

            }
        });
        protocol.handleProtocolSessionStarted(ServiceType.RPC, SESSION_ID,
                ProtocolConstants.PROTOCOL_VERSION_THREE, "");
        assertTrue("test should pass", passed[0]);
    }


    public void testStartServiceACK_RPC_FrameReceived() throws Exception {
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.StartServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID((byte) 0);
        frameHeader.setServiceType(ServiceType.RPC);
        frameHeader.setDataSize(0);
        IProtocolListener mock = mock(IProtocolListener.class);
        WiProProtocol.MessageFrameAssembler messageFrameAssembler = new WiProProtocol(mock).new MessageFrameAssembler();
        ArgumentCaptor<Session> sessionTypeCaptor = ArgumentCaptor.forClass(Session.class);
        ArgumentCaptor<Byte> versionCaptor = ArgumentCaptor.forClass(byte.class);
        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        messageFrameAssembler.handleFrame(frameHeader, new byte[0]);
        Mockito.verify(mock).onProtocolSessionStarted(sessionTypeCaptor.capture(), versionCaptor.capture(), correlationIdCaptor.capture());
        assertEquals(0, sessionTypeCaptor.getValue().getSessionId());
        assertEquals(ServiceType.RPC, sessionTypeCaptor.getValue().getServiceList().get(0).getServiceType());
    }

    public void testStartServiceACK_Mobile_Nav_FrameReceived() throws Exception {
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.StartServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Mobile_Nav);
        frameHeader.setDataSize(0);
        IProtocolListener mock = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(mock);
        protocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        WiProProtocol.MessageFrameAssembler messageFrameAssembler = protocol.new MessageFrameAssembler();
        ArgumentCaptor<ServiceType> serviceTypeCaptor = ArgumentCaptor.forClass(ServiceType.class);
        ArgumentCaptor<Byte> sessionIDCaptor = ArgumentCaptor.forClass(byte.class);
        ArgumentCaptor<Byte> versionCaptor = ArgumentCaptor.forClass(byte.class);
        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        messageFrameAssembler.handleFrame(frameHeader, new byte[0]);
        Mockito.verify(mock).onProtocolServiceStarted(serviceTypeCaptor.capture(), sessionIDCaptor.capture(), versionCaptor.capture(), correlationIdCaptor.capture());
        assertEquals(ServiceType.Mobile_Nav, serviceTypeCaptor.getValue());
        assertEquals(SESSION_ID, sessionIDCaptor.getValue().byteValue());
        assertEquals(ProtocolConstants.PROTOCOL_VERSION_THREE, versionCaptor.getValue().byteValue());
        assertEquals("", correlationIdCaptor.getValue());
    }

    public void testStartServiceACK_RPC_SessionID0_NotthorwExp() throws Exception {
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.StartServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.RPC);
        frameHeader.setDataSize(0);
        IProtocolListener mock = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(mock);
        protocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        WiProProtocol.MessageFrameAssembler messageFrameAssembler = protocol.new MessageFrameAssembler();
        try {
            messageFrameAssembler.handleFrame(frameHeader, new byte[0]);
        }catch (IllegalArgumentException exp){
            assertTrue(" should not get here",false);
        }
    }

    public void testStartServiceACK_Navi_SessionID0_thorwExp() throws Exception {
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.StartServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID((byte) 0);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Mobile_Nav);
        frameHeader.setDataSize(0);
        IProtocolListener mock = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(mock);
        protocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        WiProProtocol.MessageFrameAssembler messageFrameAssembler = protocol.new MessageFrameAssembler();
        try {
            messageFrameAssembler.handleFrame(frameHeader, new byte[0]);
            assertTrue(" should not get here",false);
        }catch (IllegalArgumentException exp){
            assertNotNull(exp);
        }
    }


    public void testStartServiceACK_AudioService_FrameReceived() throws Exception {
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.StartServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Audio_Service);
        frameHeader.setDataSize(0);
        IProtocolListener mock = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(mock);
        protocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        WiProProtocol.MessageFrameAssembler messageFrameAssembler = protocol.new MessageFrameAssembler();
        ArgumentCaptor<ServiceType> serviceTypeCaptor = ArgumentCaptor.forClass(ServiceType.class);
        ArgumentCaptor<Byte> sessionIDCaptor = ArgumentCaptor.forClass(byte.class);
        ArgumentCaptor<Byte> versionCaptor = ArgumentCaptor.forClass(byte.class);
        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        messageFrameAssembler.handleFrame(frameHeader, new byte[0]);
        Mockito.verify(mock).onProtocolServiceStarted(serviceTypeCaptor.capture(), sessionIDCaptor.capture(), versionCaptor.capture(), correlationIdCaptor.capture());
        assertEquals(ServiceType.Audio_Service, serviceTypeCaptor.getValue());
        assertEquals(SESSION_ID, sessionIDCaptor.getValue().byteValue());
        assertEquals(ProtocolConstants.PROTOCOL_VERSION_THREE, versionCaptor.getValue().byteValue());
        assertEquals("", correlationIdCaptor.getValue());
    }


    public void testHeartBeatMonitorResetOnMessageSent() throws Exception {
        IProtocolListener protocolListener = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(protocolListener);
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(FrameDataControlFrameType.StartServiceACK.getValue());
        frameHeader.setFrameType(FrameType.Control);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.RPC);
        frameHeader.setDataSize(0);
        protocol.handleProtocolFrameToSend(frameHeader, null,0,0 );
        verify(protocolListener).onResetHeartbeat();
    }

    public void testFrameHeaderAndDataSendWithOneChunk() throws Exception {
        IProtocolListener protocolListener = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(protocolListener);
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(ProtocolFrameHeader.FrameDataSingleFrame);
        frameHeader.setFrameType(FrameType.Single);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Mobile_Nav);
        frameHeader.setDataSize(0);
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i + 1);
        }
        protocol.handleProtocolFrameToSend(frameHeader, data, 0, data.length);
        byte[] frameHeaderArray = frameHeader.assembleHeaderBytes();
        byte[] expectedResult = new byte[frameHeaderArray.length + data.length];
        System.arraycopy(frameHeaderArray, 0, expectedResult, 0, frameHeaderArray.length);
        System.arraycopy(data, 0, expectedResult, frameHeaderArray.length, data.length);

        // Take in count that messages are going to be sent with ExecutorService
        Thread.sleep(20);

        verify(protocolListener, times(1)).onProtocolMessageBytesToSend(expectedResult, 0,
                expectedResult.length);
    }

    public void testFrameHeaderSendWithNoData() throws Exception {
        IProtocolListener protocolListener = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(protocolListener);
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(ProtocolFrameHeader.FrameDataSingleFrame);
        frameHeader.setFrameType(FrameType.Single);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Mobile_Nav);
        frameHeader.setDataSize(0);
        protocol.handleProtocolFrameToSend(frameHeader, null, 0, 0);
        byte[] frameHeaderArray = frameHeader.assembleHeaderBytes();

        // Take in count that messages are going to be sent with ExecutorService
        Thread.sleep(20);

        verify(protocolListener, times(1)).onProtocolMessageBytesToSend(frameHeaderArray, 0,
                frameHeaderArray.length);
    }

    public void testFrameHeaderAndDataSendWithPartialChunk() throws Exception {
        IProtocolListener protocolListener = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(protocolListener);
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(ProtocolFrameHeader.FrameDataFinalConsecutiveFrame);
        frameHeader.setFrameType(FrameType.Consecutive);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Mobile_Nav);
        frameHeader.setDataSize(0);
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i + 1);
        }
        protocol.handleProtocolFrameToSend(frameHeader, data, 20, 20);
        byte[] frameHeaderArray = frameHeader.assembleHeaderBytes();
        byte[] expectedResult = new byte[frameHeaderArray.length + 20];
        System.arraycopy(frameHeaderArray, 0, expectedResult, 0, frameHeaderArray.length);
        System.arraycopy(data, 20, expectedResult, frameHeaderArray.length, 20);

        // Take in count that messages are going to be sent with ExecutorService
        Thread.sleep(20);

        verify(protocolListener, times(1)).onProtocolMessageBytesToSend(expectedResult, 0,
                expectedResult.length);
    }

    public void testFrameHeaderAndDataSendWithPartialChunkWithLengthToBig() throws Exception {
        IProtocolListener protocolListener = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(protocolListener);
        ProtocolFrameHeader frameHeader = new ProtocolFrameHeader();
        frameHeader.setFrameData(ProtocolFrameHeader.FrameDataFinalConsecutiveFrame);
        frameHeader.setFrameType(FrameType.Consecutive);
        frameHeader.setSessionID(SESSION_ID);
        frameHeader.setVersion(ProtocolConstants.PROTOCOL_VERSION_THREE);
        frameHeader.setServiceType(ServiceType.Mobile_Nav);
        frameHeader.setDataSize(0);
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i + 1);
        }
        protocol.handleProtocolFrameToSend(frameHeader, data, 90, 20);
        byte[] frameHeaderArray = frameHeader.assembleHeaderBytes();
        byte[] expectedResult = new byte[frameHeaderArray.length + 10];
        System.arraycopy(frameHeaderArray, 0, expectedResult, 0, frameHeaderArray.length);
        System.arraycopy(data, 90, expectedResult, frameHeaderArray.length, 10);

        // Take in count that messages are going to be sent with ExecutorService
        Thread.sleep(20);

        verify(protocolListener, times(1)).onProtocolMessageBytesToSend(expectedResult, 0,
                expectedResult.length);
    }

    public void testPriorityBlockingQueueWithCorrectOutputOrderByCorrelationId()
            throws InterruptedException {
        PriorityBlockingQueue<Runnable> queue =
                new PriorityBlockingQueue<Runnable>(20, new CompareMessagesPriority());

        RunnableWithPriority runWithPriority;

        int i;
        for (i = 0; i < 50; i++) {
            runWithPriority = new RunnableWithPriority((byte) 0, i);
            queue.add(runWithPriority);
        }

        i = 0;
        while (queue.size() > 0) {
            assertEquals(i++, ((RunnableWithPriority)queue.take()).getCorrelationId());
        }
    }

    public void testPriorityInMessages() {
        ProtocolFrameHeader rpc = ProtocolFrameHeaderFactory.createStartSession(
                ServiceType.RPC, SESSION_ID, ProtocolConstants.PROTOCOL_VERSION_TWO);

        ProtocolFrameHeader bulkData = ProtocolFrameHeaderFactory.createStartSession(
                ServiceType.Bulk_Data, SESSION_ID, ProtocolConstants.PROTOCOL_VERSION_TWO);

        ProtocolFrameHeader mobileNaviService = ProtocolFrameHeaderFactory.createStartSession(
                ServiceType.Mobile_Nav, SESSION_ID, ProtocolConstants.PROTOCOL_VERSION_TWO);

        ProtocolFrameHeader audioService = ProtocolFrameHeaderFactory.createStartSession(
                ServiceType.Audio_Service, SESSION_ID, ProtocolConstants.PROTOCOL_VERSION_TWO);

        // RPC has a high priority
        assertTrue(rpc.getServiceType().getValue() < bulkData.getServiceType().getValue());
        assertTrue(rpc.getServiceType().getValue() < mobileNaviService.getServiceType().getValue());
        assertTrue(rpc.getServiceType().getValue() < audioService.getServiceType().getValue());

        // Bulk data has a lower priority
        assertTrue(bulkData.getServiceType().getValue() > rpc.getServiceType().getValue());
        assertTrue(bulkData.getServiceType().getValue() > mobileNaviService.getServiceType().getValue());
        assertTrue(bulkData.getServiceType().getValue() > audioService.getServiceType().getValue());

        // Mobile Navi prior to Bulk data
        assertTrue(mobileNaviService.getServiceType().getValue() > rpc.getServiceType().getValue());
        assertTrue(mobileNaviService.getServiceType().getValue() < bulkData.getServiceType().getValue());
        assertTrue(mobileNaviService.getServiceType().getValue() > audioService.getServiceType().getValue());

        // Audio prior to Mobile Navi
        assertTrue(audioService.getServiceType().getValue() < mobileNaviService.getServiceType().getValue());
        assertTrue(audioService.getServiceType().getValue() < bulkData.getServiceType().getValue());
        assertTrue(audioService.getServiceType().getValue() > rpc.getServiceType().getValue());
    }

    public void testRPCHasHigherPriorityToBulk() throws InterruptedException {
        int messagesNumber = 50;
        //CountDownLatch countDownLatch = new CountDownLatch();
        IProtocolListener protocolListener = mock(IProtocolListener.class);
        WiProProtocol protocol = new WiProProtocol(protocolListener) {

            @Override
            public void SendMessage(ProtocolMessage protocolMsg) {
                prepareMockItems();
                super.SendMessage(protocolMsg);
            }

            @Override
            protected void handleProtocolFrameToSend(ProtocolFrameHeader header, byte[] data,
                                                     int offset, int length) {
                super.handleProtocolFrameToSend(header, data, offset, length);

                Logger.d(TAG + " header:" + header.getFrameType() + " " + header.getDataSize());
            }

            private void prepareMockItems() {
                _messageLocks = mock(Hashtable.class);
                when(_messageLocks.get(anyByte())).thenReturn("mockLock");
                doThrow(new IllegalStateException("should not get protocol error"))
                        .when(_protocolListener).onProtocolError(anyString(), any(Exception.class));
            }
        };
        protocol.setProtocolVersion(ProtocolConstants.PROTOCOL_VERSION_TWO);

        ProtocolMessage protocolMessageWithData =
                generateRPCProtocolMessage(ProtocolConstants.PROTOCOL_FRAME_HEADER_SIZE_V_2, 1000);
        ProtocolMessage protocolMessage =
                generateRPCProtocolMessage(ProtocolConstants.PROTOCOL_FRAME_HEADER_SIZE_V_2, 0);

        for (int i = 0; i < messagesNumber; i++) {
            int result = randInt(0, 1);
            if (result == 0) {
                protocol.SendMessage(protocolMessageWithData);
            } else {
                protocol.SendMessage(protocolMessage);
            }
        }

        Thread.sleep(2000);
    }

    private int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}