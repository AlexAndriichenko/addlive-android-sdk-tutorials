package com.addlive.tutorials;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import com.addlive.platform.ADL;
import com.addlive.platform.InitProgressChangedEvent;
import com.addlive.platform.InitState;
import com.addlive.platform.InitStateChangedEvent;
import com.addlive.platform.PlatformInitListener;
import com.addlive.platform.PlatformInitOptions;
import com.addlive.service.AuthDetails;
import com.addlive.service.ConnectionDescriptor;
import com.addlive.service.Device;
import com.addlive.service.MediaConnection;
import com.addlive.service.MediaType;
import com.addlive.service.ResponderAdapter;
import com.addlive.service.UIThreadResponder;
import com.addlive.service.VideoCaptureDevice;
import com.addlive.service.listener.AddLiveServiceListener;
import com.addlive.service.listener.AddLiveServiceListenerAdapter;
import com.addlive.service.listener.ConnectionLostEvent;
import com.addlive.service.listener.SessionReconnectedEvent;
import com.addlive.service.listener.SpeechActivityEvent;
import com.addlive.service.listener.UserStateChangedEvent;
import com.addlive.service.listener.VideoFrameSizeChangedEvent;

public class Tutorial6 extends Activity {

    /**
     * ===========================================================================
     * Constants
     * ===========================================================================
     */

    private static final long ADL_APP_ID = 1; // TODO set your app ID here.
    private static final String ADL_API_KEY = "YOUR_API_KEY_HERE"; // TODO set you API key here.
    private static final String ADL_SCOPE_ID = "any_scope_id"; // TODO set you scope ID here.

    private static final String LOG_TAG = "AddLiveTutorial";

    /**
     * ===========================================================================
     * Properties
     * ===========================================================================
     */

    // Local userId.
    private long _userId;

    // Current remote sink id.
    private String _currentRemoteSinkId;

    // Remote videoSinkId.
    private String _remoteVideoSinkId;

    // Remote screenSinkId.
    private String _remoteScreenSinkId;

    // Video or screen flag
    boolean _videoFeed = false;

    // Toggle button
    Button _toggle;

    // Remote VideoView object.
    com.addlive.view.VideoView _view;

    // Remote VideoView width after calculating.
    float _videoWidth;

    // Remote VideoView height after calculating.
    float _videoHeight;

    // Remote VideoView max width.
    float _videoMaxWidth;

    // Remote VideoView max height.
    float _videoMaxHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial6);

        // Hiding keyboard.
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Setting the local video view.
        SurfaceView local = (SurfaceView) findViewById(R.id.local_video);
        local.setZOrderMediaOverlay(true);

        // Setting the local userId.
        Random rand = new Random();
        _userId = (1 + rand.nextInt(9999));

        // Setting the maximum constants according to the device screen.
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        _videoMaxWidth = (float) (size.x * 0.95);
        _videoMaxHeight = (float) (size.y * 0.55);

        // Setting the reference to VideoView object.
        _view = (com.addlive.view.VideoView) findViewById(R.id.remote_video);

        // Setting the reference to button object.
        _toggle = (Button) findViewById(R.id.toggle);

        // Setting view's listeners.
        initializeActions();

        // Framework initialization.
        initializeAddLive();
    }

    /**
     * ===========================================================================
     * UI Actions initialization and handling
     * ===========================================================================
     */

    private void initializeActions() {

        // Setting the click listener for the connect button.
        findViewById(R.id.connect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // Calling method to handle the connection.
                        onConnect();
                    }
                }
        );

        // Setting the click listener for the disconnect button.
        findViewById(R.id.disconnect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // Calling method to handle the disconnection.
                        onDisconnect();
                    }
                }
        );

        // Setting the click listener for the toggle video/screen button.
        findViewById(R.id.toggle).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // Calling method to handle the toggle.
                        onToggle();
                    }
                }
        );
    }

    //==========================================================================

    private void onConnect() {

        // Showing a message when connecting.
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Connecting...");

        // Disabling the connect button.
        Button connect = (Button) findViewById(R.id.connect);
        connect.setEnabled(false);

        Log.d(LOG_TAG, "Connecting to scope: '" + ADL_SCOPE_ID + "'");

        // Creating the descriptor to send with the connect call.
        ConnectionDescriptor desc = genConnDescriptor();

        // Responder to handle the connect response.
        UIThreadResponder<MediaConnection> connectResponder =
                new UIThreadResponder<MediaConnection>(this) {
                    @Override
                    protected void handleResult(MediaConnection result) {

                        Log.d(LOG_TAG, "Successfully connected to the scope");

                        // Removing the message when connecting.
                        TextView status = (TextView) findViewById(R.id.status);
                        status.setText("");

                        // Hiding the connect button.
                        Button connect = (Button) findViewById(R.id.connect);
                        connect.setVisibility(View.GONE);

                        // Showing and enabling the disconnect button.
                        Button disconnect = (Button) findViewById(R.id.disconnect);
                        disconnect.setVisibility(View.VISIBLE);
                        disconnect.setEnabled(true);

	        	    /* getService - Returns a reference to the implementation 
	        	     * of the AddLiveService interface.
	        		 * 
	        		 * monitorSpeechActivity - Controls monitoring of speech 
	        		 * activity within given scope.
	        		 */
                        ADL.getService().monitorSpeechActivity(
                                new ResponderAdapter<Void>(), ADL_SCOPE_ID, true);
                    }

                    @Override
                    protected void handleError(int errCode, String errMessage) {

                        // Setting and showing the error label.
                        Log.e(LOG_TAG, "ERROR: (" + errCode + ") " + errMessage);

                        TextView status = (TextView) findViewById(R.id.status);
                        status.setTextColor(Color.RED);
                        status.setText("ERROR: (" + errCode + ") " + errMessage);

                        // Enabling the connect button again.
                        Button connect = (Button) findViewById(R.id.connect);
                        connect.setEnabled(true);
                    }
                };
	        
		/* getService - Returns a reference to the implementation of the 
		 * AddLiveService interface.
		 * 
		 * connect - Establishes connection to the streaming server using given
		 * description. This is the most important method of all provided by 
		 * the AddLive Service API.
		 */
        ADL.getService().connect(connectResponder, desc);
    }

    //==========================================================================

    private void onDisconnect() {

        // Disabling the disconnect button.
        Button disconnect = (Button) findViewById(R.id.disconnect);
        disconnect.setEnabled(false);

        // Showing a message when disconnecting.
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Disconnecting ...");

        // Responder to handle the disconnect response.
        UIThreadResponder<Void> disconnectResponder =
                new UIThreadResponder<Void>(this) {
                    @Override
                    protected void handleResult(Void result) {

                        // Removing the message when disconnecting.
                        TextView status = (TextView) findViewById(R.id.status);
                        status.setText("");

                        // Showing the connect button.
                        Button connect = (Button) findViewById(R.id.connect);
                        connect.setVisibility(View.VISIBLE);
                        connect.setEnabled(true);

                        // Hiding the disconnect button.
                        Button disconnect = (Button) findViewById(R.id.disconnect);
                        disconnect.setVisibility(View.GONE);

                        // Hiding toggle button.
                        _toggle.setVisibility(View.INVISIBLE);

                        // Clearing the remote VideoView.
                        _view.stop();
                        _view.setSinkId("");
                    }

                    @Override
                    protected void handleError(int errCode, String errMessage) {

                        // Setting and showing the error label.
                        Log.e(LOG_TAG, "ERROR: (" + errCode + ") " + errMessage);

                        TextView status = (TextView) findViewById(R.id.status);
                        status.setTextColor(Color.RED);
                        status.setText("ERROR: (" + errCode + ") " + errMessage);

                        // Enabling the disconnect button again.
                        Button disconnect = (Button) findViewById(R.id.disconnect);
                        disconnect.setEnabled(true);
                    }
                };
        
		/* getService - Returns a reference to the implementation of the 
		 * AddLiveService interface.
		 * 
		 * disconnect - Disconnects previously established connection to the 
		 * streaming server.
		 */
        ADL.getService().disconnect(disconnectResponder, ADL_SCOPE_ID);
    }

    //==========================================================================

    private void onToggle() {

        if (!_videoFeed) {

            // Toggle feed from screen to video.
            onRemoteStoppedScreen();
        } else {

            // Toggle feed from video to screen.
            onRemoteStoppedVideo();
        }
    }

    /**
     * ===========================================================================
     * AddLive Platform initialization
     * ===========================================================================
     */

    private void initializeAddLive() {

        // ADL.init listener
        PlatformInitListener listener = new PlatformInitListener() {
            @Override
            public void onInitProgressChanged(InitProgressChangedEvent e) {
                // Actually not used by the platform for now. Just a placeholder.
            }

            @Override
            public void onInitStateChanged(InitStateChangedEvent e) {

                if (e.getState() == InitState.INITIALIZED) {

                    // Successfully initialized.
                    onAdlInitialized();
                } else {

                    // Unsuccessfully initialized.
                    onAdlInitError(e);
                }
            }
        };

        // Describes the options allowing platform initialization.
        PlatformInitOptions initOptions = new PlatformInitOptions();
        String storageDir =
                Environment.getExternalStorageDirectory().getAbsolutePath();

        // Sets the path where platform should look for the native components
        // of the AddLive SDK.
        initOptions.setStorageDir(storageDir);

        // Sets the value of the applicationId property.
        initOptions.setApplicationId(ADL_APP_ID);

        Log.d(LOG_TAG, "Initializing the AddLive SDK.");
	    
	    /* Initializes the AddLive ADL. The process is asynchronous and 
	     * initialization state changes and progress updates are reported via
	     * the listener provided.
	     */
        ADL.init(listener, initOptions, this);
    }

    //==========================================================================

    private void onAdlInitialized() {
        Log.d(LOG_TAG, "AddLive SDK initialized");

		/* getService - Returns a reference to the implementation of the 
		 * AddLiveService interface.
		 * 
		 * addServiceListener - Registers a AddLive Service listener. 
		 * Listener provided here should subclass the provided 
		 * AddLiveServiceListener stub and implement required methods.
		 */
        ADL.getService().addServiceListener(new ResponderAdapter<Void>(),
                getListener());

		/* 
		 * getVideoCaptureDeviceNames - Returns an id of currently selected
		 * video capture device.
		 */
        ADL.getService().getVideoCaptureDeviceNames(
                new UIThreadResponder<List<Device>>(this) {
                    @Override
                    protected void handleResult(List<Device> devices) {

                        // On successful process the devices.
                        onGetVideoCaptureDeviceNames(devices);
                    }

                    @Override
                    protected void handleError(int errCode, String errMessage) {
                        Log.e(LOG_TAG, "Failed to get video capture devices.");
                    }
                }
        );
    }

    //==========================================================================

    private void onAdlInitError(InitStateChangedEvent e) {

        // Setting and showing the error label.
        TextView status = (TextView) findViewById(R.id.status);
        status.setTextColor(Color.RED);

        String errMessage = "ERROR: (" + e.getErrCode() + ") " +
                e.getErrMessage();
        status.setText(errMessage);

        Log.e(LOG_TAG, errMessage);
    }

    //==========================================================================

    private void onGetVideoCaptureDeviceNames(List<Device> devicesAvailables) {

        // Setting the front camera as the local video feeding camera.
        SurfaceView view = (SurfaceView) findViewById(R.id.local_video);
        ADL.getService().setVideoCaptureDevice(new ResponderAdapter<Void>(),
                VideoCaptureDevice.FRONT_CAMERA.getId(), view);
	    
		/* 
		 * startLocalVideo - Starts previewing the local video feed.
		 */
        ADL.getService().startLocalVideo(new UIThreadResponder<String>(this) {
            @Override
            protected void handleResult(String videoSinkId) {
                Log.d(LOG_TAG, "Succesfully started local video.");
            }

            @Override
            protected void handleError(int errCode, String errMessage) {
                Log.e(LOG_TAG, "Failed to start local video.");
            }
        }, view);
    }

    /**
     * ===========================================================================
     * AddLive Service Events handling
     * Interface allowing implementation of global AddLive Service event handlers.
     * ===========================================================================
     */

    private AddLiveServiceListener getListener() {
        return new AddLiveServiceListenerAdapter() {

            /*
             * onVideoFrameSizeChanged - Called whenever the video resolution of
             * any video sink changes.
             */
            @Override
            public void onVideoFrameSizeChanged(final VideoFrameSizeChangedEvent e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.v(LOG_TAG, "videoFrameSizeChanged: " + e.getSinkId()
                                + " -> " + e.getWidth() + "x" + e.getHeight());

                        // If its the current sink feeding.
                        if (e.getSinkId().equals(_currentRemoteSinkId)) {

                            // Getting the new values to scale the VideoView.
                            fitDimensions(e.getWidth(), e.getHeight(),
                                    _videoMaxWidth, _videoMaxHeight);

                            // Get the layout.
                            RelativeLayout.LayoutParams _rootLayoutParams =
                                    (RelativeLayout.LayoutParams) _view.getLayoutParams();

                            // Setting new dimensions.
                            _rootLayoutParams.width = (int) _videoWidth;
                            _rootLayoutParams.height = (int) _videoHeight;
                            _view.setLayoutParams(_rootLayoutParams);
                        }

                        // Setting the new resolution to the remote VideoView.
                        _view.resolutionChanged(e.getWidth(), e.getHeight());
                    }
                });
            }

            /*
             * onConnectionLost - Called whenever local user lost a connection
             * to the AddLive Streaming Server.
             * (e.g. due to Internet connectivity issues)
             */
            @Override
            public void onConnectionLost(final ConnectionLostEvent e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "onConnectionLost Listener triggered");

                        // Removing the remoteVideoSink to the VideoView
                        _view.stop();
                        _view.setSinkId("");
                    }
                });
            }

            /*
             * onSessionReconnected - Called whenever the AddLive SDK
             * reestablishes a connection to the AddLive streaming server.
             */
            @Override
            public void onSessionReconnected(final SessionReconnectedEvent e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "onSessionReconnected Listener triggered");
                    }
                });
            }

            /*
             * onSpeechActivity - Reports speech activity within given scope.
             */
            @Override
            public void onSpeechActivity(final SpeechActivityEvent e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "onSpeechActivity");

                        Log.d(LOG_TAG, "Active Speakers: " +
                                e.getActiveSpeakers().toString());

                        Log.d(LOG_TAG, "Speech Activity: " +
                                e.getSpeechActivity().toString());
                    }
                });
            }

            /*
             * onMediaStreamEvent - Called whenever remote user publishes or
             * stops publishing media of particular type, inside a scope to
             * which local user is also connected.
             */
            @Override
            public void onMediaStreamEvent(final UserStateChangedEvent e) {
                super.onMediaStreamEvent(e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (e.getMediaType() == MediaType.VIDEO) {

                            // Updating the Id.
                            _remoteVideoSinkId = e.getVideoSinkId();

                            // If I was feeding video but now it's disable and
                            // I have available screen.
                            if (_videoFeed && !e.isVideoPublished() &&
                                    e.isScreenPublished()) {

                                // Toggle feed from video to screen.
                                onRemoteStoppedVideo();

                            } else if (e.isVideoPublished()) {

                                // Updating current Id.
                                _currentRemoteSinkId = _remoteVideoSinkId;

                                // Setting the remoteVideoSink to the VideoView.
                                _view.stop();
                                _view.setSinkId(_currentRemoteSinkId);
                                _view.start();
                            }
                        } else if (e.getMediaType() == MediaType.SCREEN) {

                            // Updating the Id.
                            _remoteScreenSinkId = e.getScreenSinkId();

                            // If I was feeding screen but now it's disable and
                            // I have available video.
                            if (!_videoFeed && !e.isScreenPublished() &&
                                    e.isVideoPublished()) {

                                // Toggle feed from screen to video.
                                onRemoteStoppedScreen();

                            } else if (e.isScreenPublished()) {

                                // Updating current Id.
                                _currentRemoteSinkId = _remoteScreenSinkId;

                                // Setting the remoteVideoSink to the VideoView.
                                _view.stop();
                                _view.setSinkId(_currentRemoteSinkId);
                                _view.start();
                            }
                        }

                        // Stop the feeding if there's none
                        if (!e.isScreenPublished() && !e.isVideoPublished()) {

                            // Removing the remoteVideoSink to the VideoView.
                            _view.stop();
                            _view.setSinkId("");
                        }

                        // Setting the toggle button visibility.
                        if (e.isScreenPublished() && e.isVideoPublished()) {
                            _toggle.setVisibility(View.VISIBLE);
                        } else if (e.isScreenPublished() || e.isVideoPublished()) {
                            _toggle.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }

            /*
             * onUserEvent - Called whenever new remote user joined or
             * existing participant left a scope to which local user is also
             * connected.
             */
            @Override
            public void onUserEvent(final UserStateChangedEvent e) {
                super.onUserEvent(e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "onAdlUserEvent: " + e.toString());

                        // If it's a new user connecting.
                        if (e.isConnected()) {
                            Log.i(LOG_TAG, "New user connected: " +
                                    e.getUserId());

                            // Getting the videoId.
                            if (e.isVideoPublished()) {
                                _currentRemoteSinkId = e.getVideoSinkId();
                                _remoteVideoSinkId = _currentRemoteSinkId;
                                _videoFeed = true;
                            }

                            // Getting the screenId. Giving priority to the
                            // screen feed if both are available.
                            if (e.isScreenPublished()) {
                                _currentRemoteSinkId = e.getScreenSinkId();
                                _remoteScreenSinkId = _currentRemoteSinkId;
                                _videoFeed = false;
                            }

                            // Showing the toggle button
                            if (e.isScreenPublished() && e.isVideoPublished()) {
                                _toggle.setVisibility(View.VISIBLE);
                            }

                            // Stopping the previous VideoView if any.
                            _view.stop();

                            // Setting the remoteVideoSink to the VideoView.
                            _view.setSinkId(_currentRemoteSinkId);
                            _view.start();
                        } else {

                            // Removing the remoteVideoSink to the VideoView.
                            _view.stop();
                            _view.setSinkId("");
                        }
                    }
                });
            }
        };
    }

    /**
     * ===========================================================================
     * Private helpers
     * ===========================================================================
     */

    // Toggle feed from video to screen.
    private void onRemoteStoppedVideo() {

        // Updating current Id.
        _currentRemoteSinkId = _remoteScreenSinkId;

        // Setting the remoteVideoSink to the VideoView.
        _view.stop();
        _view.setSinkId(_currentRemoteSinkId);
        _view.start();
        _videoFeed = false;
    }

    // Toggle feed from screen to video.
    private void onRemoteStoppedScreen() {

        // Updating current Id.
        _currentRemoteSinkId = _remoteVideoSinkId;

        // Setting the remoteVideoSink to the VideoView.
        _view.stop();
        _view.setSinkId(_currentRemoteSinkId);
        _view.start();
        _videoFeed = true;
    }

    // Generates the ConnectionDescriptor (authentication + video description)
    private ConnectionDescriptor genConnDescriptor() {

        ConnectionDescriptor desc = new ConnectionDescriptor();
        desc.setAutopublishAudio(true);
        desc.setAutopublishVideo(true);
        desc.setScopeId(ADL_SCOPE_ID);

        // Authentication
        String salt = "Some random string salt";
        long timeNow = System.currentTimeMillis() / 1000;
        long expires = timeNow + (5 * 60);

        AuthDetails authDetails = new AuthDetails();
        authDetails.setUserId(_userId);
        authDetails.setSalt(salt);
        authDetails.setExpires(expires);
        String signatureBody = String.valueOf(ADL_APP_ID) + ADL_SCOPE_ID +
                _userId + salt + expires + ADL_API_KEY;
        MessageDigest digest;
        String signature = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(signatureBody.getBytes());
            signature = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e1) {
            Log.e(LOG_TAG, "Failed to calculate authentication signature due to "
                    + "missing SHA-256 algorithm.");
        }
        authDetails.setSignature(signature);
        desc.setAuthDetails(authDetails);

        return desc;
    }

    private static String bytesToHexString(byte[] bytes) {

        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(0xFF & aByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private void fitDimensions(float srcW, float srcH, float targetW, float targetH) {
        float srcAR = srcW / srcH;
        float targetAR = targetW / targetH;

        if (srcW < targetW && srcH < targetH) {
            _videoWidth = srcW;
            _videoHeight = srcH;
        }
        if (srcAR < targetAR) {
            // Match height
            _videoWidth = srcW * targetH / srcH;
            _videoHeight = targetH;
        } else {
            // Match width
            _videoWidth = targetW;
            _videoHeight = targetW * srcH / srcW;
        }
    }
}
