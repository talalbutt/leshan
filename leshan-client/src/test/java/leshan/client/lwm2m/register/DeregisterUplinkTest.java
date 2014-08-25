package leshan.client.lwm2m.register;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.util.UUID;

import leshan.client.lwm2m.bootstrap.BootstrapMessageDeliverer.InterfaceTypes;
import leshan.client.lwm2m.bootstrap.BootstrapMessageDeliverer.OperationTypes;
import leshan.client.lwm2m.response.MockedCallback;
import leshan.client.lwm2m.response.OperationResponse;
import leshan.client.lwm2m.response.OperationResponseCode;

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
public class DeregisterUplinkTest {
	private static final String SERVER_HOST = "leshan.com";
	private static final int SERVER_PORT = 1234;

	private static final String ENDPOINT_LOCATION = UUID.randomUUID().toString();

	@Mock
	private CoAPEndpoint endpoint;
	
	private String expectedRequestLocation;
	private String actualRequestLocation;

	private RegisterUplink uplink;

	private MockedCallback callback;

	private byte[] actualResponsePayload;

	private InetSocketAddress serverAddress;
	
	@Before
	public void setUp(){
		callback = new MockedCallback();
		serverAddress = InetSocketAddress.createUnresolved(SERVER_HOST, SERVER_PORT);
		expectedRequestLocation = "coap://" + serverAddress.getHostString() + ":" + serverAddress.getPort() + "/rd/" + ENDPOINT_LOCATION;
		uplink = new RegisterUplink(serverAddress, endpoint);
		
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(final InvocationOnMock invocation) throws Throwable {
				final Request request = (Request) invocation.getArguments()[0];
				actualRequestLocation = request.getURI();
				
				final Response response = new Response(ResponseCode.DELETED);
				response.setPayload(OperationResponseCode.generateReasonPhrase(OperationResponseCode.valueOf(response.getCode().value), 
						InterfaceTypes.REGISTRATION, OperationTypes.DEREGISTER));

				request.setResponse(response);
				
				return null;
			}
		}).when(endpoint).sendRequest(any(Request.class));
	}

	@Test
	public void testGoodSyncDeregister() {
		
		final OperationResponse response = uplink.deregister(ENDPOINT_LOCATION);
		
		
		verify(endpoint).stop();
		verify(endpoint).sendRequest(any(Request.class));
		
		assertTrue(response.isSuccess());
		assertEquals(ResponseCode.DELETED, response.getResponseCode());
		assertEquals(expectedRequestLocation, actualRequestLocation);
	}
	
	@Test
	public void testGoodAsyncDeregister() {
		
		uplink.deregister(ENDPOINT_LOCATION, callback);
		
		await().untilTrue(callback.isCalled());
		actualResponsePayload = callback.getResponsePayload();
		
		verify(endpoint).stop();
		verify(endpoint).sendRequest(any(Request.class));
		
		assertTrue(callback.isSuccess());
		assertEquals(ResponseCode.DELETED, callback.getResponseCode());
		assertEquals(expectedRequestLocation, actualRequestLocation);
	}
	
	@Test
	public void testNullSyncDeregister() {
		final OperationResponse response = uplink.deregister(null);
		
		verify(endpoint, never()).stop();
		verify(endpoint, never()).sendRequest(any(Request.class));
		
		assertFalse(response.isSuccess());
		assertEquals(ResponseCode.NOT_FOUND, response.getResponseCode());
	}

}