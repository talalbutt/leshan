package leshan.client.lwm2m.bootstrap;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.net.InetSocketAddress;
import java.util.UUID;

import leshan.client.lwm2m.bootstrap.BootstrapMessageDeliverer.InterfaceTypes;
import leshan.client.lwm2m.bootstrap.BootstrapMessageDeliverer.OperationTypes;
import leshan.client.lwm2m.response.OperationResponse;
import leshan.client.lwm2m.response.OperationResponseCode;
import leshan.client.lwm2m.util.ResponseCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapUplinkTest {
	private static final int SYNC_TIMEOUT_MS = 2000;
	private static final String ENDPOINT_NAME = UUID.randomUUID().toString();
	private byte[] actualPayload;
	private String actualRequest;
	private Code actualCode;
	private ResponseCallback callback;
	
	@Mock
	private CoAPEndpoint endpoint;
	@Mock
	private BootstrapDownlink downlink;
	private String expectedRequest;
	private InetSocketAddress serverAddress;
	
	@Before
	public void setUp() {
		callback = new ResponseCallback();
		expectedRequest = "coap://localhost/bs?ep=" + ENDPOINT_NAME;
		serverAddress = InetSocketAddress.createUnresolved("localhost", 1234);
	}

	private BootstrapUplink initializeServerResponse(final InterfaceTypes interfaceType, final OperationTypes operationType, final ResponseCode responseCode) {
		doAnswer(new Answer<Void>(){
			
			@Override
			public Void answer(final InvocationOnMock invocation) throws Throwable {
				final Request request = (Request) invocation.getArguments()[0];
				actualRequest = request.getURI();
				actualCode = request.getCode();
				
				final Response response = new Response(responseCode);
				response.setPayload(OperationResponseCode.generateReasonPhrase(OperationResponseCode.valueOf(response.getCode().value), interfaceType, operationType));
				
				request.setResponse(response);
				
				return null;
			}
		}).when(endpoint).sendRequest(any(Request.class));
		
		final BootstrapUplink uplink = new BootstrapUplink(serverAddress, endpoint, downlink);
		return uplink;
	}
	
	private void sendBootstrapAndGetAsyncResponse(final BootstrapUplink uplink) {
		uplink.bootstrap(ENDPOINT_NAME, callback);
		
		await().untilTrue(callback.isCalled());
		if(callback.isSuccess()){
			actualPayload = callback.getResponsePayload();
		}
	}
	
	private void sendBootstrapAndGetSyncResponse(final BootstrapUplink uplink) {
		final OperationResponse operationResponse = uplink.bootstrap(ENDPOINT_NAME, SYNC_TIMEOUT_MS);
		if(operationResponse.isSuccess()){
			actualPayload = operationResponse.getPayload();
		}
	}
	
	private void verifyResponse(final String expectedPayload) {
		assertEquals(expectedRequest, actualRequest);
		assertEquals(Code.POST, actualCode);
		if(expectedPayload != null){
			assertArrayEquals(expectedPayload.getBytes(), actualPayload);
		}
		else{
			assertTrue(actualPayload == null);
		}
	}
	
	@Test
	public void testGoodAsyncRequestPayload() {
		final BootstrapUplink uplink = initializeServerResponse(InterfaceTypes.BOOTSTRAP, OperationTypes.REQUEST, ResponseCode.CHANGED);
		
		sendBootstrapAndGetAsyncResponse(uplink);
		
		verifyResponse("Request Bootstrap is completed successfully");
	}
	
	
	@Test
	public void testBadAsyncPayload() {
		final BootstrapUplink uplink = initializeServerResponse(InterfaceTypes.BOOTSTRAP, OperationTypes.REQUEST, ResponseCode.BAD_REQUEST);
		
		sendBootstrapAndGetAsyncResponse(uplink);
		
		verifyResponse(null);
	}
	
	@Test
	public void testGoodSyncPayload(){
		final BootstrapUplink uplink = initializeServerResponse(InterfaceTypes.BOOTSTRAP, OperationTypes.REQUEST, ResponseCode.CHANGED);
		
		sendBootstrapAndGetSyncResponse(uplink);
		
		verifyResponse("Request Bootstrap is completed successfully");
	}
	
	@Test
	public void testBadSyncPayload() {
		final BootstrapUplink uplink = initializeServerResponse(InterfaceTypes.BOOTSTRAP, OperationTypes.REQUEST, ResponseCode.BAD_REQUEST);
		
		sendBootstrapAndGetSyncResponse(uplink);
		
		verifyResponse(null);
	}
}
