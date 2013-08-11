/**
 * 
 */
package cn.buding.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import android.util.Log;

/**
 * <p>
 * RSA工具类。提供签名、验证、加密和解密方法。使用UTF-8编码实现从字符串到字节数组的转换。
 * 
 * @author li xiaofeng
 * 
 */
public class RSASignature {

	private static final String UTF_8 = "utf-8";
	private static final String TAG = "RSASignature";

	public static String encryptToString(String content, PublicKey pubkey) {
		try {
			return encryptToString(content.getBytes(UTF_8), pubkey);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	public static String encryptToString(byte[] cdata, PublicKey pubkey) {
		try {
			return Base64.encodeBase64String(encrypt(cdata, pubkey));
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}

		return null;
	}

	public static byte[] encrypt(byte[] cdata, String pubkey) {
		PublicKey key = getPublicKey(pubkey);
		if (key != null)
			return encrypt(cdata, key);
		return null;
	}

	public static byte[] encrypt(byte[] cdata, PublicKey pubkey) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, pubkey);

			InputStream ins = new ByteArrayInputStream(cdata);
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			// rsa加密的最大长度是117字节 = (keysize/8 - 11) [PKCS1填充]
			byte[] buf = new byte[117];
			int bufsize;
			while ((bufsize = ins.read(buf)) != -1) {
				byte[] block = null;
				if (buf.length == bufsize) {
					block = buf;
				} else {
					block = new byte[bufsize];
					System.arraycopy(buf, 0, block, 0, bufsize);
				}
				writer.write(cipher.doFinal(block));
			}

			return writer.toByteArray();
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}

		return null;
	}

	public static String decryptToString(String content, PrivateKey prikey) {

		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, prikey);

			InputStream ins = new ByteArrayInputStream(
					Base64.decodeBase64(content));
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			// rsa解密的字节大小最多是128，将需要解密的内容，按128位拆开解密
			byte[] buf = new byte[128];
			int bufl;
			while ((bufl = ins.read(buf)) != -1) {
				byte[] block = null;

				if (buf.length == bufl) {
					block = buf;
				} else {
					block = new byte[bufl];
					System.arraycopy(buf, 0, block, 0, bufl);
				}

				writer.write(cipher.doFinal(block));
			}

			return new String(writer.toByteArray(), UTF_8);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	/**
	 * 
	 * 解密
	 * 
	 * @param content
	 *            密文
	 * 
	 * @param key
	 *            商户私钥
	 * 
	 * @return 解密后的字符串
	 */
	public static String decryptToString(String content, String key) {
		PrivateKey prikey = getPrivateKey(key);
		return decryptToString(content, prikey);
	}

	/**
	 * 得到公钥
	 * 
	 */
	public static PublicKey getPublicKey(String key) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] encodedKey = Base64.decodeBase64(key.getBytes());

			PublicKey pubKey = keyFactory
					.generatePublic(new X509EncodedKeySpec(encodedKey));
			return pubKey;
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	/**
	 * 
	 * 得到私钥
	 * 
	 * @param key
	 *            密钥字符串（经过base64编码）
	 * 
	 */

	public static PrivateKey getPrivateKey(String key) {
		try {
			byte[] keyBytes;
			keyBytes = Base64.decodeBase64(key);

			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
			return privateKey;
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;

	}

	public static final String SIGN_ALGORITHMS = "SHA1WithRSA";

	/**
	 * RSA签名
	 * 
	 * @param content
	 *            待签名数据
	 * @param priKey
	 *            商户私钥
	 * @return 签名值
	 */
	public static String sign(String content, PrivateKey priKey) {
		try {
			String charset = UTF_8;

			java.security.Signature signature = java.security.Signature
					.getInstance(SIGN_ALGORITHMS);

			signature.initSign(priKey);
			signature.update(content.getBytes(charset));

			byte[] signed = signature.sign();

			return Base64.encodeBase64String(signed);
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return null;
	}

	/**
	 * RSA验签名检查
	 * 
	 * @param content
	 *            待签名数据
	 * @param sign
	 *            签名值
	 * @param pubKey
	 *            支付宝公钥
	 * @return 布尔值
	 */
	public static boolean doCheck(String content, String sign, PublicKey pubKey) {
		try {
			java.security.Signature signature = java.security.Signature
					.getInstance(SIGN_ALGORITHMS);

			signature.initVerify(pubKey);
			signature.update(content.getBytes(UTF_8));

			boolean bverify = signature.verify(Base64.decodeBase64(sign));
			return bverify;
		} catch (Exception e) {
			Log.e(TAG, "", e);
		}
		return false;
	}

}
