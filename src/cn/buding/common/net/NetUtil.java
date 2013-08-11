package cn.buding.common.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * utils for handle network operation.
 */
public class NetUtil {
	private static final String TAG = "NetUtil";
	private static final String CONTENT_CHARSET = "UTF-8";

	/**
	 * is at least one network connected
	 * 
	 * @return
	 */
	public static boolean isNetworkAvailable(Context ctx) {
		if (ctx == null)
			return false;
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		return (info != null && info.isConnected());
	}

	/**
	 * is current connected network is wifi
	 * 
	 * @return
	 */
	public static boolean isNetworkWifi(Context ctx) {
		if (ctx == null)
			return false;
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		return (info != null && info.getType() == ConnectivityManager.TYPE_WIFI);
	}

	public static boolean isNetWorkGPRS(Context ctx) {
		if (ctx == null)
			return false;
		TelephonyManager tele = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		int type = tele.getNetworkType();
		return type == TelephonyManager.NETWORK_TYPE_GPRS
				|| type == TelephonyManager.NETWORK_TYPE_EDGE;
	}

	/**
	 * is current connected network is cmwap
	 * 
	 * @return
	 */
	public static boolean isNetworkCmwap(Context ctx) {
		// 以下为支持cmwap部分，移动和联通的cmwap代理貌似都是10.0.0.172，有待测试
		String proxyHost = android.net.Proxy.getDefaultHost();
		// 判断网络状况，如果有其它通路的话就不要再使用cmwap的代理
		try {
			if (proxyHost != null && !isNetworkWifi(ctx)) {
				ConnectivityManager manager = (ConnectivityManager) ctx
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo info = manager.getActiveNetworkInfo();
				String extraInfo = null;
				if (info != null)
					extraInfo = info.getExtraInfo();
				if (extraInfo != null) {
					String extraTrim = extraInfo.trim().toLowerCase();
					if (extraTrim.equals("cmwap") || extraTrim.equals("uniwap")
							|| extraTrim.equals("ctnet")
							|| extraTrim.equals("ctwap"))
						return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * whether there is only one network connected.
	 * {@link ConnectivityManager#TYPE_WIFI}
	 * {@link ConnectivityManager#TYPE_MOBILE}
	 * 
	 * @param ctx
	 * @return
	 */
	public static boolean onlyOneNetWork(Context ctx) {
		if (ctx == null)
			return false;
		ConnectivityManager manager = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (manager == null)
			return false;
		NetworkInfo[] infos = manager.getAllNetworkInfo();
		int count = 0;
		if (infos == null)
			return false;
		for (NetworkInfo info : infos)
			if (info.isConnected())
				count++;
		return count == 1;
	}

	private final static int DEFAULT_TIMEOUT_TIME = 20000;

	/**
	 * create a connection from a url. and add proxy if network is cmwap
	 * 
	 * @param mUrl
	 *            the url to build connection
	 * @return the connection of mUrl
	 * @throws IOException
	 */
	public static synchronized HttpURLConnection getHttpUrlConnection(
			Context mContext, URL mUrl) throws IOException {
		HttpURLConnection conn = null;
		if (NetUtil.isNetworkCmwap(mContext)) {
			java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP,
					new InetSocketAddress(android.net.Proxy.getDefaultHost(),
							android.net.Proxy.getDefaultPort()));
			conn = (HttpURLConnection) mUrl.openConnection(p);
		} else
			conn = (HttpURLConnection) mUrl.openConnection();
		conn.setConnectTimeout(DEFAULT_TIMEOUT_TIME);
		conn.setReadTimeout(DEFAULT_TIMEOUT_TIME);
		return conn;
	}

	/**
	 * return current ip address
	 * 
	 * @return
	 */
	public static String getIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ipaddress = inetAddress.getHostAddress()
								.toString();
						return ipaddress;
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, "Socket exception in GetIP Address of Utilities", ex);
		}
		return null;
	}

	/**
	 * return the wifi address of the cellphone
	 * 
	 * @return
	 */
	public static String getWifiMacAddress(Context context) {
		WifiManager wifimanager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiinfo = wifimanager.getConnectionInfo();
		if (wifiinfo != null && wifiinfo.getMacAddress() != null
				&& wifiinfo.getMacAddress().length() > 0)
			return wifiinfo.getMacAddress();
		return null;
	}

	/**
	 * this part is can not be compiled in 1.5, so we just return null
	 * currently.
	 * 
	 * @return
	 */
	public static String getBluetoothMacAddress(Context context) {
		// BluetoothAdapter bluetoothDefaultAdapter =
		// BluetoothAdapter.getDefaultAdapter();
		// String mac = null;
		// if ((bluetoothDefaultAdapter != null) &&
		// (bluetoothDefaultAdapter.isEnabled()))
		// mac = BluetoothAdapter.getDefaultAdapter().getAddress();
		// if(mac != null && mac.length() > 0)
		// return mac;
		return null;
	}

	/**
	 * compress a byte array to a new byte array by {@link Deflater}
	 * 
	 * @param data
	 *            input data to compress
	 * @param len
	 *            the 0..length in data is for compress
	 * @return compressed byte array
	 */
	public static byte[] deflateCompress(byte[] data) {
		Deflater compresser = new Deflater();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		try {
			compresser.reset();
			compresser.setInput(data, 0, data.length);
			compresser.finish();
			byte[] buf = new byte[1024];
			while (!compresser.finished()) {
				int i = compresser.deflate(buf);
				bos.write(buf, 0, i);
			}
			return bos.toByteArray();
		} finally {
			compresser.end();
			try {
				bos.close();
			} catch (IOException e) {
			}
		}

	}

	/**
	 * decompress 0..len in data
	 * 
	 * @param data
	 * @param len
	 *            length of data for decompress in data[]
	 * @return
	 * @throws DataFormatException
	 */
	public static byte[] defalteDecompress(byte[] data)
			throws DataFormatException {
		ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
		Inflater decompresser = new Inflater();
		try {
			decompresser.reset();
			decompresser.setInput(data, 0, data.length);
			byte[] buf = new byte[1024];
			while (!decompresser.finished()) {
				int i = decompresser.inflate(buf);
				o.write(buf, 0, i);
			}
			return o.toByteArray();
		} finally {
			decompresser.end();
			try {
				o.close();
			} catch (IOException e) {
			}
		}
	}

	public static byte[] gzipCompress(byte[] data) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		GZIPOutputStream gos = null;
		try {
			gos = new GZIPOutputStream(os);
			gos.write(data);
			gos.finish();
			return os.toByteArray();
		} finally {
			os.close();
			if (gos != null)
				gos.close();
		}

	}

	public static byte[] gzipDecompress(byte[] compressed) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(compressed);
		GZIPInputStream gis = null;
		try {
			gis = new GZIPInputStream(is, 1024);
			byte[] data = new byte[1024];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			int len = 0;
			while ((len = gis.read(data)) != -1) {
				bos.write(data, 0, len);
			}
			return bos.toByteArray();
		} finally {
			is.close();
			if (gis != null)
				gis.close();
		}
	}

	/**
	 * copy of Entity.toString
	 * 
	 * @throws IOException
	 */
	public static String entityStreamToString(InputStream instream,
			String charset) throws IOException {
		if (instream == null) {
			return "";
		}
		if (charset == null) {
			charset = HTTP.DEFAULT_CONTENT_CHARSET;
		}
		Reader reader = new InputStreamReader(instream, charset);
		CharArrayBuffer buffer = new CharArrayBuffer(4096);
		try {
			char[] tmp = new char[1024];
			int l;
			while ((l = reader.read(tmp)) != -1) {
				buffer.append(tmp, 0, l);
			}
		} finally {
			reader.close();
		}
		return buffer.toString();
	}

}
