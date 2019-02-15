package std.uflo.blehidapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements BleStatusCallbackListener {
    private static final String TAG = "BleHidApp";
    private static final int SET_BLE_STATUS = 0;

    private static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SET_BLE_STATUS:
                    if (msg.arg1 == 1) {
                        mStatusTextView.setText(R.string.ble_connected);
                    }
                    else {
                        mStatusTextView.setText(R.string.ble_disconnect);
                    }
            }
        }
    };

    private Button mSendButton;
    private EditText mMessageEdit;
    private static TextView mStatusTextView;
    private BleServer mBleServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mSendButton = findViewById(R.id.btn_msg_send);
        mMessageEdit = findViewById(R.id.edit_message);
        mStatusTextView = findViewById(R.id.label_ble_state);

        mBleServer = new BleServer(getApplicationContext(), this);

        mSendButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mMessageEdit.getText().toString();
                if (message == null || message.equals(""))
                    message = "Hello World!";
                mBleServer.sendMessage(message);
            }
        });


        mBleServer.startServers();
        mBleServer.startAdvertising();

    }

/*    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        mBleServer.startServers();
        mBleServer.startAdvertising();

        super.onResume();
    }
*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

/*    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        mBleServer.stopAdvertising();
        mBleServer.stopServers();

        super.onPause();
    }
*/
    @Override
    public void onDestroy() {
        mBleServer.stopAdvertising();
        mBleServer.stopServers();
        super.onDestroy();
    }

    @Override
    public void onBleStatusCallback(boolean connect) {
        if (connect)
            mHandler.sendMessage(mHandler.obtainMessage(SET_BLE_STATUS, 1, 0));
        else
            mHandler.sendMessage(mHandler.obtainMessage(SET_BLE_STATUS, 0, 0));
    }
}
