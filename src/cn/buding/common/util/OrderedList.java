package cn.buding.common.util;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * a List that always in order.
 */
public class OrderedList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 6042257577267157249L;
	private Comparator<T> comparetor;
	private boolean mPermitRepeat = false;

	public OrderedList(Comparator<T> comparetor) {
		this(comparetor, false);
	}

	/**
	 * @param comparetor the comparetor to keep the list in order
	 * @param permitRepeat whether the list permit same item exist.
	 */
	public OrderedList(Comparator<T> comparetor, boolean permitRepeat) {
		this.comparetor = comparetor;
		mPermitRepeat = permitRepeat;
	}

	@Override
	public boolean add(T t) {
		int count = size();
		int beg = 0, end = count - 1, mid;
		int flag;
		if (count == 0 || comparetor.compare(t, get(beg)) < 0) {
			add(0, t);
			return true;
		}
		if (comparetor.compare(t, get(end)) > 0) {
			add(count, t);
			return true;
		}
		if (comparetor.compare(t, get(beg)) == 0) {
			if (mPermitRepeat)
				add(beg, t);
			return false;
		} else if (comparetor.compare(t, get(end)) == 0) {
			if (mPermitRepeat)
				add(end, t);
			return false;
		}

		while (beg + 1 < end) {
			mid = (beg + end) / 2;
			flag = comparetor.compare(t, get(mid));
			if (flag == 0) {
				if (mPermitRepeat)
					add(mid, t);
				return false;
			} else if (flag > 0) {
				beg = mid;
			} else {
				end = mid;
			}
		}
		add(beg + 1, t);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object t) {
		if (mPermitRepeat)
			return super.remove(t);
		int count = size();
		int beg = 0, end = count - 1, mid, flag;
		while (beg < end) {
			mid = (beg + end) / 2;
			flag = comparetor.compare((T) t, get(mid));
			if (flag == 0) {
				super.remove(mid);
				return true;
			} else if (flag > 0) {
				beg = mid + 1;
			} else {
				end = mid - 1;
			}
		}
		if (beg < count && comparetor.compare((T) t, get(beg)) == 0) {
			super.remove(beg);
			return true;
		}
		return false;
	}
}