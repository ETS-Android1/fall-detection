package things.android.essential;

import java.io.IOException;

interface SerialListener {
    void onSerialConnect() throws IOException;
    void onSerialConnectError(Exception e);
    void onSerialRead(byte[] data);
    void onSerialIoError(Exception e);
}
