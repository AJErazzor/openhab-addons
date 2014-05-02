/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.xbmc.rpc;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.openhab.binding.xbmc.internal.XbmcHost;
import org.openhab.binding.xbmc.rpc.calls.FilesPrepareDownload;
import org.openhab.binding.xbmc.rpc.calls.GUIShowNotification;
import org.openhab.binding.xbmc.rpc.calls.JSONRPCPing;
import org.openhab.binding.xbmc.rpc.calls.PlayerGetActivePlayers;
import org.openhab.binding.xbmc.rpc.calls.PlayerGetItem;
import org.openhab.binding.xbmc.rpc.calls.PlayerPlayPause;
import org.openhab.binding.xbmc.rpc.calls.PlayerStop;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.library.types.StringType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

/**
 * Manages the web socket connection for a single XBMC instance.
 * 
 * @author tlan, Ben Jones
 * @since 1.5.0
 */
public class XbmcConnector {

	private static final Logger logger = LoggerFactory.getLogger(XbmcConnector.class);

	// request timeout (configurable?)
	private static final int REQUEST_TIMEOUT_MS = 60000;
	
	// the XBMC instance and openHAB event publisher handles
	private final XbmcHost xbmc;
	private final EventPublisher eventPublisher;

	private final String httpUri;
	private final String wsUri;
	private final AsyncHttpClient client;
	private final WebSocketUpgradeHandler handler;
	
	// stores which property is associated with each item
	private final Map<String, String> watches = new HashMap<String, String>();

	// the async connection to the XBMC instance
	private WebSocket webSocket;
	private boolean connected = false;

	// the current player state
	private State currentState = State.Stop;
	
	private enum State {
		Play,
		Pause,
		Stop
	}

	/**
	 * @param xbmc
	 *            The host to connect to. Give a reachable hostname or ip
	 *            address, without protocol or port
	 * @param eventPublisher
	 *            EventPublisher to push out state updates
	 */
	public XbmcConnector(XbmcHost xbmc, EventPublisher eventPublisher) {
		this.xbmc = xbmc;
		this.eventPublisher = eventPublisher;

		this.httpUri = String.format("http://%s:%d/jsonrpc", xbmc.getHostname(), xbmc.getPort());
		this.wsUri = String.format("ws://%s:%d/jsonrpc", xbmc.getHostname(), xbmc.getWSPort());
		
		this.client = new AsyncHttpClient(new NettyAsyncHttpProvider(createAsyncHttpClientConfig()));
		this.handler = createWebSocketHandler();
	}

	/***
	 * Check if the connection to the XBMC instance is active
	 * 
	 * @return true if an active connection to the XBMC instance exists, false otherwise
	 */
	public boolean isConnected() { 
		if (webSocket == null || !webSocket.isOpen())
			return false;
		
		return connected;
	}
	
	/**
	 * Attempts to create a connection to the XBMC host and begin listening
	 * for updates over the async http web socket
	 *  
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void open() throws IOException, InterruptedException, ExecutionException {
		// cleanup any existing web socket left over from previous attempts
		close();
		
		// attempt to open the web socket connection to the XBMC instance
		webSocket = client.prepareGet(wsUri).execute(handler).get();
	}

	/***
	 * Close this connection to the XBMC instance
	 */
	public void close() {
		// if there is an old web socket then clean up and destroy
		if (webSocket != null) { 
			webSocket.close();
			webSocket = null;
		}
	}
		
	private AsyncHttpClientConfig createAsyncHttpClientConfig() {
		Builder builder = new AsyncHttpClientConfig.Builder();
		builder.setRealm(createRealm());
		builder.setRequestTimeoutInMs(REQUEST_TIMEOUT_MS);
		return builder.build();
	}

	private Realm createRealm() {
		Realm.RealmBuilder builder = new Realm.RealmBuilder();
		builder.setPrincipal(xbmc.getUsername());
		builder.setPassword(xbmc.getPassword());
		builder.setUsePreemptiveAuth(true);
		builder.setScheme(AuthScheme.BASIC);
		return builder.build();
	}

	private WebSocketUpgradeHandler createWebSocketHandler() {
		WebSocketUpgradeHandler.Builder builder = new WebSocketUpgradeHandler.Builder();
		builder.addWebSocketListener(new XbmcWebSocketListener());
		return builder.build(); 
	}
	
	class XbmcWebSocketListener implements WebSocketTextListener {
		private final ObjectMapper mapper = new ObjectMapper();

		@Override
		public void onOpen(WebSocket webSocket) {
			logger.debug("[{}]: Websocket opened", xbmc.getHostname());
			connected = true;
		}
		
		@Override
		public void onError(Throwable e) {
			if (e instanceof ConnectException) {
				logger.debug("[{}]: Websocket connection error", xbmc.getHostname());
			} else if (e instanceof TimeoutException) {
				logger.debug("[{}]: Websocket timeout error", xbmc.getHostname());
			} else {
				logger.error("[{}]: Websocket error: {}", xbmc.getHostname(), e.getMessage());
			}
		}
		
		@Override
		public void onClose(WebSocket webSocket) {
			logger.warn("[{}]: Websocket closed", xbmc.getHostname());
			webSocket = null;
			connected = false;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void onMessage(String message) {
			logger.debug("[{}]: Message received: {}", xbmc.getHostname(), message);
			Map<String, Object> json;
			try {
				json = mapper.readValue(message, Map.class);
			} catch (JsonParseException e) {
				logger.error("Error parsing JSON", e);
				return;
			} catch (JsonMappingException e) {
				logger.error("Error mapping JSON", e);
				return;
			} catch (IOException e) {
				logger.error("An I/O error occured while decoding JSON", e);
				return;
			}

			// We only care about certain notifications on the websocket
			// feed, since all our actual data fetching is done via http
			try {
				if (json.containsKey("method")) {
					String method = (String)json.get("method");
					if (method.startsWith("Player.On")) {
						processPlayerStateChanged(method, json);
					}
				}
			} catch (Exception e) {
				logger.error("Error handling player state change message", e);
			}
		}
		
		@Override
		public void onFragment(String fragment, boolean last) {}
	}

	/**
	 * Send a ping to the XBMC host and wait for a 'pong'.
	 */
	public void ping() {
		final JSONRPCPing ping = new JSONRPCPing(client, httpUri);
		
		ping.execute(new Runnable() {
			@Override
			public void run() {
				connected = ping.isPong();
			}
		});
	}
	
	/**
	 * Create a mapping between an item and an xbmc property
	 * 
	 * @param itemName
	 *            The name of the item which should receive updates
	 * @param property
	 *            The property of this xbmc instance, which is to be 
	 *            watched for changes
	 * 
	 */
	public void addItem(String itemName, String property) {
		if (!watches.containsKey(itemName)) {
			watches.put(itemName, property);
		}
	}
	
	public void updatePlayerStatus() {
		final PlayerGetActivePlayers activePlayers = new PlayerGetActivePlayers(client, httpUri);
		
		activePlayers.execute(new Runnable() {
			@Override
			public void run() {
				if (activePlayers.isPlaying()) {
					updateState(State.Play);
					requestPlayerUpdate(activePlayers.getPlayerId());
				} else {
					updateState(State.Stop);
				}
			}
		});
	}

	public void playerPlayPause() {
		final PlayerGetActivePlayers activePlayers = new PlayerGetActivePlayers(client, httpUri);
		
		activePlayers.execute(new Runnable() {
			public void run() {
				PlayerPlayPause playPause = new PlayerPlayPause(client, httpUri);
				playPause.setPlayerId(activePlayers.getPlayerId());
				playPause.execute();
			}
		});
	}
	
	public void playerStop() {
		final PlayerGetActivePlayers activePlayers = new PlayerGetActivePlayers(client, httpUri);
		
		activePlayers.execute(new Runnable() {
			public void run() {
				PlayerStop stop = new PlayerStop(client, httpUri);
				stop.setPlayerId(activePlayers.getPlayerId());
				stop.execute();
			}
		});
	}
	
	public void showNotification(String title, String message) {
		final GUIShowNotification showNotification = new GUIShowNotification(client, httpUri);
		
		showNotification.setTitle(title);
		showNotification.setMessage(message);
		showNotification.execute();
	}
	
	private void processPlayerStateChanged(String method, Map<String, Object> json) {
		if ("Player.OnPlay".equals(method)) {
			// get the player id and make a new request for the media details
			Map<String, Object> params = RpcCall.getMap(json, "params");
			Map<String, Object> data = RpcCall.getMap(params, "data");
			Map<String, Object> player = RpcCall.getMap(data, "player");
			Integer playerId = (Integer)player.get("playerid");			

			updateState(State.Play);
			requestPlayerUpdate(playerId);
		}

		if ("Player.OnPause".equals(method)) {
			updateState(State.Pause);
		}

		if ("Player.OnStop".equals(method)) {
			updateState(State.Stop);
		}
	}

	private void updateState(State state) {
		// sometimes get a Pause immediately after a Stop - so just ignore
		if (currentState.equals(State.Stop) && state.equals(State.Pause))
			return;
		
		// set the player state watch values
		updateProperty("Player.State", state.toString());
		
		// if this is a Stop then clear everything else
		if (state == State.Stop) {
			for (String property : getPlayerProperties()) {				
				updateProperty(property, null);
			}
		}
		
		// keep track of our current state
		currentState = state;
	}
	
	private void requestPlayerUpdate(int playerId) {
		// CRIT: if a PVR recording is played in XBMC the playerId is reported as -1
		if (playerId == -1) {
			logger.warn("[{}]: Invalid playerId ({}) - assume this is a PVR recording playback and update playerId -> 1 (video player)", xbmc.getHostname(), playerId);
			playerId = 1;
		}
		
		if (playerId < 0 || playerId > 2) {
			logger.debug("[{}]: Invalid playerId ({}) - must be between 0 and 2 (inclusive)", xbmc.getHostname(), playerId);
			return;
		}
		
		// get the list of properties we are interested in
		final List<String> properties = getPlayerProperties();
		
		// make the request for the player item details
		final PlayerGetItem item = new PlayerGetItem(client, httpUri);
		item.setPlayerId(playerId);
		item.setProperties(properties);
		
		item.execute(new Runnable() {
			public void run() {
				// now update each of the openHAB items for each property
				for (String property : properties) {
					String value = item.getPropertyValue(property);			
					if (property.equals("Player.Fanart")) {
						updateFanartUrl(property, value);
					} else {
						updateProperty(property, value);				
					}
				}
			}
		});
	}

	private void updateFanartUrl(final String property, String imagePath) {
		if (StringUtils.isEmpty(imagePath))
			return;
		
		final FilesPrepareDownload fanart = new FilesPrepareDownload(client, httpUri);
		fanart.setImagePath(imagePath);
		
		fanart.execute(new Runnable() {
			public void run() {
				String url = String.format("http://%s:%d/%s", xbmc.getHostname(), xbmc.getPort(), fanart.getPath());
				updateProperty(property, url);
			}
		});
	}

	private void updateProperty(String property, String value) {
		StringType stringType = new StringType(value == null ? "" : value);
		
		for (Entry<String, String> e : watches.entrySet()) {
			if (property.equals(e.getValue())) {
				eventPublisher.postUpdate(e.getKey(), stringType);
			}
		}
	}
	
	private List<String> getPlayerProperties() {
		// get a distinct list of player properties we have items configured for
		List<String> properties = new ArrayList<String>();
		for (String property : watches.values()) {
			if (!property.startsWith("Player."))
				continue;
			if (property.equals("Player.State"))
				continue;
			if (properties.contains(property))
				continue;
			
			properties.add(property);
		}
		return properties;
	}
}
