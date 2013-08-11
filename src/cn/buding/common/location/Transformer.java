package cn.buding.common.location;

public class Transformer {
	private class CnXyPair {
		int x;
		int y;
	}

	private int wg_flag = 0;

	private double yj_sin2(double x) {
		double tt;
		double ss;
		int ff;
		double s2;
		int cc;
		ff = 0;
		if (x < 0) {
			x = -x;
			ff = 1;
		}
		cc = (int) (x / 6.28318530717959);
		tt = x - cc * 6.28318530717959;
		if (tt > 3.1415926535897932) {
			tt = tt - 3.1415926535897932;
			if (ff == 1)
				ff = 0;
			else if (ff == 0)
				ff = 1;
		}
		x = tt;
		ss = x;
		s2 = x;
		tt = tt * tt;
		s2 = s2 * tt;
		ss = ss - s2 * 0.166666666666667;
		s2 = s2 * tt;
		ss = ss + s2 * 8.33333333333333E-03;
		s2 = s2 * tt;
		ss = ss - s2 * 1.98412698412698E-04;
		s2 = s2 * tt;
		ss = ss + s2 * 2.75573192239859E-06;
		s2 = s2 * tt;
		ss = ss - s2 * 2.50521083854417E-08;
		if (ff == 1)
			ss = -ss;
		return ss;
	}

	private double Transform_yj5(double x, double y) {
		double tt;
		tt =
				300 + 1 * x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1
						* Math.sqrt(Math.sqrt(x * x));
		tt =
				tt
						+ (20 * yj_sin2(18.849555921538764 * x) + 20 * yj_sin2(6.283185307179588 * x))
						* 0.6667;
		tt =
				tt
						+ (20 * yj_sin2(3.141592653589794 * x) + 40 * yj_sin2(1.047197551196598 * x))
						* 0.6667;
		tt =
				tt
						+ (150 * yj_sin2(0.2617993877991495 * x) + 300 * yj_sin2(0.1047197551196598 * x))
						* 0.6667;
		return tt;
	}

	private double Transform_yjy5(double x, double y) {
		double tt;
		tt =
				-100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2
						* Math.sqrt(Math.sqrt(x * x));
		tt =
				tt
						+ (20 * yj_sin2(18.849555921538764 * x) + 20 * yj_sin2(6.283185307179588 * x))
						* 0.6667;
		tt =
				tt
						+ (20 * yj_sin2(3.141592653589794 * y) + 40 * yj_sin2(1.047197551196598 * y))
						* 0.6667;
		tt =
				tt
						+ (160 * yj_sin2(0.2617993877991495 * y) + 320 * yj_sin2(0.1047197551196598 * y))
						* 0.6667;
		return tt;
	}

	private double Transform_jy5(double x, double xx) {
		double n;
		double a;
		double e;
		a = 6378245;
		e = 0.00669342;
		n =
				Math.sqrt(1 - e * yj_sin2(x * 0.0174532925199433)
						* yj_sin2(x * 0.0174532925199433));
		n = (xx * 180) / (a / n * Math.cos(x * 0.0174532925199433) * 3.1415926);
		return n;
	}

	private double Transform_jyj5(double x, double yy) {
		double m;
		double a;
		double e;
		double mm;
		a = 6378245;
		e = 0.00669342;
		mm =
				1 - e * yj_sin2(x * 0.0174532925199433)
						* yj_sin2(x * 0.0174532925199433);
		m = (a * (1 - e)) / (mm * Math.sqrt(mm));
		return (yy * 180) / (m * 3.1415926);
	}

	private CnXyPair wgtochina_lb(int wg_flag, int wg_lng, int wg_lat) {
		double x_add;
		double y_add;
		double x_l;
		double y_l;
		CnXyPair res = new CnXyPair();
		res.x = 0;
		res.y = 0;

		x_l = wg_lng;
		x_l = x_l / 3686400.0;
		y_l = wg_lat;
		y_l = y_l / 3686400.0;
		if (x_l < 72.004) {
			return res;
		}
		if (x_l > 137.8347) {
			return res;
		}
		if (y_l < 0.8293) {
			return res;
		}
		if (y_l > 55.8271) {
			return res;
		}
		if (wg_flag == 0) {
			res.x = wg_lng;
			res.y = wg_lat;
			return res;
		}

		x_add = Transform_yj5(x_l - 105, y_l - 35);
		y_add = Transform_yjy5(x_l - 105, y_l - 35);
		res.x = (int) ((x_l + Transform_jy5(y_l, x_add)) * 3686400);
		res.y = (int) ((y_l + Transform_jyj5(y_l, y_add)) * 3686400);
		return res;
	}

	private int latlon_to_1_div_1024_second(double latlon) {
		return (int) (latlon * (double) 3600 * (double) 1024);
	}

	public Location transform(Location latlon) {
		wg_flag = 0;
		int wg_lng = latlon_to_1_div_1024_second(latlon.getLongitude());
		int wg_lat = latlon_to_1_div_1024_second(latlon.getLatitude());

		CnXyPair cPair;
		if (wg_flag == 0) {
			cPair = wgtochina_lb(wg_flag, wg_lng, wg_lat);
			wg_flag = 1;
		}
		cPair = wgtochina_lb(wg_flag, wg_lng, wg_lat);
		double longitude = ((double) cPair.x) / (double) (3600 * 1024);
		double latitude = ((double) cPair.y) / (double) (3600 * 1024);
		latlon.setLatitude(latitude);
		latlon.setLongitude(longitude);
		return latlon;
	}
}
